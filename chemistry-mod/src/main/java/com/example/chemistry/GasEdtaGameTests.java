package com.example.chemistry;

import com.example.chemistry.block.GasApplianceBlock;
import com.example.chemistry.blockentity.GasApplianceBlockEntity;
import com.example.chemistry.entity.RubberTubeEntity;
import com.example.chemistry.item.GasCylinderItem;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.*;
import com.example.chemistry.solution.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import java.util.Map;

public final class GasEdtaGameTests {
    private static void check(GameTestHelper h, boolean ok, String message) { h.assertTrue(ok, Component.literal(message)); }
    private static GasApplianceBlockEntity place(GameTestHelper h, BlockPos relative, GasApplianceBlock block) {
        BlockPos p = h.absolutePos(relative);
        h.getLevel().setBlock(p, block.defaultBlockState(), 3);
        return GasApplianceBlock.device(h.getLevel(), p);
    }
    public static void supply(GameTestHelper h) { h.runAtTickTime(1, () -> {
        var source = place(h, new BlockPos(1,1,1), ModBlocks.GAS_CYLINDER_SMALL.get());
        var target = place(h, new BlockPos(3,1,1), ModBlocks.GAS_CYLINDER_SMALL.get());
        source.loadCylinder("methane", 100); source.toggleValve();
        target.loadCylinder("methane", 0);
        var tube = RubberTubeEntity.create(h.getLevel(), source.port(), target.port(), source.portPosition());
        tube.setSupplyLine(true); tube.initAir(); h.getLevel().addFreshEntity(tube);
        for (int i=0;i<20;i++) source.tickServer();
        check(h,target.remaining()>0,"automatic delivery failed");
        check(h,source.remaining()+target.remaining()+tube.getTransitMl()==100,"supply creates/loses gas");
        target.loadCylinder("methane",target.capacity());
        for (int i=0;i<20;i++) source.tickServer();
        int left=source.remaining(),transit=tube.getTransitMl();
        source.tickServer(); check(h,source.remaining()==left&&tube.getTransitMl()==transit,"blocked receiver consumes gas");
        check(h,tube.dropItem().is(ModItems.GAS_SUPPLY_TUBE.get()),"supply line loses type");
        source.toggleValve(); source.tickServer();check(h,source.remaining()==left,"closed valve supplies gas");
        tube.discard();h.succeed();
    }); }
    public static void burner(GameTestHelper h) { h.runAtTickTime(1, () -> {
        var b=place(h,new BlockPos(1,1,1),ModBlocks.BUNSEN_BURNER.get());
        b.toggleValve();check(h,b.receive("hydrogen",10)==0,"burner accepts wrong fuel");
        check(h,b.receive("methane",20)==20&&b.ignite(),"methane ignition failed");
        var vessel=new ItemStack(ModItems.ERLENMEYER_FLASK.get());LabVesselItem.addMass(vessel,"liquid","water",100);
        double before=TemperatureSystem.getTemp(vessel);b.tickServer();
        check(h,b.remaining()==18,"burning does not consume 2 mL");
        check(h,GasBurners.heat(h.getLevel(),b.getBlockPos().above(),vessel)&&TemperatureSystem.getTemp(vessel)>before,"burner does not heat vessel");
        double once=TemperatureSystem.getTemp(vessel);check(h,!b.heat(vessel)&&TemperatureSystem.getTemp(vessel)==once,"heat budget reused");
        for(int i=0;i<10;i++)b.tickServer();check(h,!b.burning()&&b.remaining()==0,"empty burner keeps burning");
        b.receive("methane",20);b.ignite();b.toggleValve();b.tickServer();check(h,!b.burning()&&b.remaining()==20,"closed burner still consumes gas");
        b.loadCylinder("methane",0); b.toggleValve();
        var source=place(h,new BlockPos(2,1,1),ModBlocks.GAS_CYLINDER_SMALL.get());source.loadCylinder("methane",1000);source.toggleValve();
        var tube=RubberTubeEntity.create(h.getLevel(),source.port(),b.port(),source.portPosition());tube.initAir();h.getLevel().addFreshEntity(tube);
        for(int i=0;i<4;i++){source.tickServer();b.tickServer();}
        check(h,b.ignite(),"short hose cannot ignite burner");
        for(int i=0;i<30;i++){source.tickServer();b.tickServer();check(h,b.burning(),"short hose starves burner");}
        check(h,source.remaining()+tube.getTransitMl()+b.remaining()<1000,"connected burner consumes no methane");tube.discard();h.succeed();
    }); }
    public static void lifecycle(GameTestHelper h) { h.runAtTickTime(1, () -> {
        check(h,ModItems.GAS_CYLINDERS.size()==com.example.chemistry.data.GasJars.ALL.size()*2,"missing gas variants");
        for(var gas:com.example.chemistry.data.GasJars.ALL)for(boolean tall:new boolean[]{false,true}){
            var stack=GasCylinderItem.filled(tall,gas.id(),1234);check(h,stack.getItem() instanceof GasCylinderItem,"missing cylinder "+gas.id());
            var item=(GasCylinderItem)stack.getItem();check(h,item.remaining(stack)==1234&&item.gas(stack).equals(gas.id()),"remaining reset");
        }
        var b=place(h,new BlockPos(1,1,1),ModBlocks.GAS_CYLINDER_TALL.get());
        h.getLevel().setBlock(b.getBlockPos().above(),b.getBlockState().setValue(GasApplianceBlock.HALF,DoubleBlockHalf.UPPER),3);
        b.loadCylinder("oxygen",321);check(h,GasApplianceBlock.device(h.getLevel(),b.getBlockPos().above())==b,"upper half proxy failed");
        var saved=b.saveCustomOnly(h.getLevel().registryAccess());
        var input=net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved);
        var copy=new GasApplianceBlockEntity(b.getBlockPos(),b.getBlockState());copy.loadWithComponents(input);
        check(h,copy.remaining()==321&&copy.gas().equals("oxygen"),"cylinder save/load resets contents");
        h.getLevel().removeBlock(b.getBlockPos().above(),false);
        check(h,h.getLevel().getBlockState(b.getBlockPos()).isAir(),"breaking upper leaves lower");
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(b.getBlockPos()).inflate(2));
        check(h,drops.stream().filter(e->e.getItem().getItem() instanceof GasCylinderItem).count()==1,"double or missing cylinder drop");
        check(h,drops.stream().filter(e->e.getItem().getItem() instanceof GasCylinderItem).allMatch(e->((GasCylinderItem)e.getItem().getItem()).remaining(e.getItem())==321),"break resets gas");h.succeed();
    }); }
    public static void competition(GameTestHelper h) { h.runAtTickTime(1, () -> {
        var totals=new SpeciesInventory(Map.of("water",5.5,"edta_h2",.001,"sodium",.002,"calcium",.001,"magnesium",.001,"chloride",.004),Map.of());
        var r=EdtaEquilibrium.solve(totals,.1);check(h,r!=null&&Conservation.compare(totals,r.species()).conserved(),"EDTA conservation");
        check(h,r.species().amount("calcium_edta")>r.species().amount("magnesium_edta"),"metal competition wrong");
        var repeat=EdtaEquilibrium.solve(r.species(),.1);check(h,Math.abs(repeat.ph()-r.ph())<1e-7,"EDTA not idempotent");
        var acid=totals.plus(new SpeciesInventory(Map.of("aqueous_hcl",.02),Map.of()));
        var acidic=EdtaEquilibrium.solve(acid,.1);check(h,acidic.ph()<r.ph()&&acidic.species().amount("calcium_edta")<r.species().amount("calcium_edta"),"acid does not reduce binding");
        var iron=new SpeciesInventory(Map.of("water",5.5,"iron_iii",.001,"thiocyanate",.001,"chloride",.003,"potassium",.001),Map.of());
        var old=CoordinationEquilibrium.solve(iron,.1);
        var chelate=EdtaEquilibrium.solve(iron.plus(new SpeciesInventory(Map.of("edta_h2",.001,"sodium",.002),Map.of())),.1);
        check(h,chelate.species().amount("iron_thiocyanate")<old.amount("iron_thiocyanate")&&chelate.species().amount("iron_iii_edta")>.0009,"EDTA fails Fe-SCN competition");
        check(h,EdtaEquilibrium.solve(totals.plus(new SpeciesInventory(Map.of("carbonate",.001),Map.of())),.1)==null,"unsupported acid-base system gives exact EDTA pH");h.succeed();
    }); }
    public static void salts(GameTestHelper h) { h.runAtTickTime(1, () -> {
        for(var compound:com.example.chemistry.data.EdtaCompounds.ALL){
            var source=new ItemStack(ModItems.ERLENMEYER_FLASK.get());LabVesselItem.addMass(source,"liquid","water",100);
            LabVesselItem.addMass(source,"solid",compound.id(),.01);PhaseSystem.dissolveAndCrystallize(source,false);
            var before=SolutionSpecies.analyticalSnapshot(source);var derived=SolutionSpecies.snapshot(source);
            check(h,before.fullyModelled()&&Conservation.compare(before,derived).conserved(),"unmapped/unbalanced salt "+compound.id());
            check(h,EdtaEquilibrium.read(source)!=null,"salt fails to dissolve "+compound.id());
            var target=new ItemStack(ModItems.ERLENMEYER_FLASK.get());LabVesselItem.addMass(target,"liquid","water",25);
            var sum=derived.plus(SolutionSpecies.snapshot(target));LabVesselItem.transferLiquids(source,target);
            check(h,Conservation.compare(sum,SolutionSpecies.snapshot(source).plus(SolutionSpecies.snapshot(target))).conserved(),"EDTA transfer loses matter "+compound.id());
        }h.succeed();
    }); }
    private GasEdtaGameTests() { }
}
