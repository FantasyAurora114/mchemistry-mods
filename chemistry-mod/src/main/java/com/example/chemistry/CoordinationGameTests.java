package com.example.chemistry;

import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.*;
import com.example.chemistry.solution.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.Map;

public final class CoordinationGameTests {
    private static void check(GameTestHelper h,boolean ok,String message){h.assertTrue(ok,Component.literal(message));}
    private static ItemStack mixture(){
        ItemStack s=new ItemStack(ModItems.ERLENMEYER_FLASK.get());
        LabVesselItem.addMass(s,"liquid","water",100);
        LabVesselItem.addMass(s,"solid","iron_iii_chloride",.0162205);
        LabVesselItem.addMass(s,"solid","potassium_thiocyanate",.00971763);
        PhaseSystem.dissolveAndCrystallize(s,false);
        return s;
    }
    public static void equilibrium(GameTestHelper h){h.runAtTickTime(1,()->{
        ItemStack s=mixture(),copy=s.copy();
        var totals=SolutionSpecies.analyticalSnapshot(s);var bound=SolutionSpecies.snapshot(s);
        check(h,totals.fullyModelled(),"new salts must be fully mapped");
        check(h,bound.amount("iron_thiocyanate")>0 && bound.amount("iron_iii")>0 && bound.amount("thiocyanate")>0,"partial binding required");
        check(h,Conservation.compare(totals,bound).conserved(),"binding must conserve elements, charge and mass");
        double v=SolutionSpecies.solutionLitres(s);
        double q=bound.amount("iron_thiocyanate")*v/(bound.amount("iron_iii")*bound.amount("thiocyanate"));
        check(h,Math.abs(q-CoordinationEquilibrium.FORMATION_CONSTANT)<1e-8,"equilibrium quotient");
        var repeat=CoordinationEquilibrium.solve(bound,v);
        check(h,Math.abs(repeat.amount("iron_thiocyanate")-bound.amount("iron_thiocyanate"))<1e-12,"solver must be idempotent");
        check(h,ItemStack.matches(s,copy),"reads mutated inventory");
        var spectator=totals.plus(new SpeciesInventory(Map.of(),Map.of(new SpeciesCatalog.ContentKey("liquid","unknown"),.5)));
        check(h,Conservation.compare(spectator,CoordinationEquilibrium.solve(spectator,v)).conserved(),"unmapped material changed");
        for(double invalid:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY}){
            boolean rejected=false;try{CoordinationEquilibrium.solve(totals,invalid);}catch(IllegalArgumentException expected){rejected=true;}
            check(h,rejected,"invalid volume accepted");
        }
        h.succeed();
    });}
    public static void shifts(GameTestHelper h){h.runAtTickTime(1,()->{
        ItemStack base=mixture();double initial=SolutionSpecies.snapshot(base).amount("iron_thiocyanate");
        int originalColor=LabVesselItem.contentsColor(base);
        ItemStack dilute=base.copy();LabVesselItem.addMass(dilute,"liquid","water",100);
        check(h,SolutionSpecies.snapshot(dilute).amount("iron_thiocyanate")<initial,"dilution must dissociate complex");
        check(h,LabVesselItem.contentsColor(dilute)!=originalColor,"dilution must change visible tint");
        LabVesselItem.consumeMass(dilute,"liquid","water",100);
        check(h,Math.abs(SolutionSpecies.snapshot(dilute).amount("iron_thiocyanate")-initial)<1e-12,"removing added water must restore equilibrium");
        for(String solute:new String[]{"iron_chloride_solution","potassium_thiocyanate_solution"}){
            var more=base.copy();LabVesselItem.addMass(more,"liquid",solute,.02);
            check(h,SolutionSpecies.snapshot(more).amount("iron_thiocyanate")>initial,"adding reagent must shift binding");
        }
        var dry=base.copy();LabVesselItem.consumeMass(dry,"liquid","water",100);PhaseSystem.dissolveAndCrystallize(dry,false);
        check(h,SolutionSpecies.snapshot(dry).amount("iron_thiocyanate")==0,"dry residue cannot be aqueous complex");
        check(h,SolutionSpecies.snapshot(dry).amount("solid:potassium_thiocyanate")>0,"drying must keep ligand residue");
        LabVesselItem.addMass(dry,"liquid","water",100);PhaseSystem.dissolveAndCrystallize(dry,false);
        check(h,Math.abs(SolutionSpecies.snapshot(dry).amount("iron_thiocyanate")-initial)<1e-12,"redissolution must restore binding");
        check(h,com.example.chemistry.data.Reactions.ALL.stream().noneMatch(r->r.display().contains("FeCl₃ + 3KSCN")),"old irreversible recipe still active");
        h.succeed();
    });}
    public static void transfer(GameTestHelper h){h.runAtTickTime(1,()->{
        ItemStack source=mixture(),target=new ItemStack(ModItems.ERLENMEYER_FLASK.get());
        LabVesselItem.addMass(target,"liquid","water",25);
        var before=SolutionSpecies.snapshot(source).plus(SolutionSpecies.snapshot(target));
        check(h,LabVesselItem.transferLiquids(source,target),"complex solution transfer failed");
        check(h,Conservation.compare(before,SolutionSpecies.snapshot(source).plus(SolutionSpecies.snapshot(target))).conserved(),"transfer loses matter");
        check(h,SolutionSpecies.snapshot(source).massGrams()==0 && SolutionSpecies.snapshot(target).amount("iron_thiocyanate")>0,"transfer leaves ghost complex");
        var entity=new com.example.chemistry.entity.PlacedVesselEntity(ModEntities.PLACED_VESSEL.get(),h.getLevel());entity.setVessel(target);
        var loaded=new com.example.chemistry.entity.PlacedVesselEntity(ModEntities.PLACED_VESSEL.get(),h.getLevel());
        var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess());
        entity.saveWithoutId(output);
        loaded.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),output.buildResult()));
        check(h,SolutionSpecies.snapshot(target).equals(SolutionSpecies.snapshot(loaded.getVessel())),"save/load changes binding");
        ItemStack hot=mixture(),receiver=new ItemStack(ModItems.ERLENMEYER_FLASK.get());
        var initial=SolutionSpecies.snapshot(hot);
        for(int i=0;i<35;i++){TemperatureSystem.setTemp(hot,110);VesselHeating.tick(hot,h.getLevel(),h.absolutePos(net.minecraft.core.BlockPos.ZERO),receiver,true,true,null);}
        check(h,Conservation.compare(initial,SolutionSpecies.snapshot(hot).plus(SolutionSpecies.snapshot(receiver))).conserved(),"distillation loses metal or ligand");
        check(h,SolutionSpecies.snapshot(receiver).amount("iron_thiocyanate")==0,"nonvolatile complex carried into receiver");
        LabVesselItem.clearContents(target);check(h,SolutionSpecies.snapshot(target).massGrams()==0,"clearing leaves complex");
        h.succeed();
    });}
}
