package com.example.chemistry;

import com.example.chemistry.item.DropperHelper;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.solution.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AcidBaseGameTests {
    private static void check(GameTestHelper h,boolean ok,String message) { h.assertTrue(ok,Component.literal(message)); }
    private static void near(GameTestHelper h,double a,double b,double tolerance,String message) {
        check(h,Math.abs(a-b)<tolerance,message+": "+a+" != "+b);
    }
    private static ItemStack vessel() {
        var result=new ItemStack(ModItems.ERLENMEYER_FLASK.get());LabVesselItem.addMass(result,"liquid","water",100);return result;
    }
    private static double mm(String id) { return SpeciesCatalog.get(id).molarMass(); }
    private static void phase(ItemStack sample) { for(int i=0;i<20;i++)PhaseSystem.tick(sample,25); }
    public static void solver(GameTestHelper h) { h.runAtTickTime(1,()->{
        near(h,AcidBaseEquilibrium.solve(.1,0,0,0,0).ph(),7,1e-10,"water is not neutral");
        near(h,AcidBaseEquilibrium.solve(.1,.01,0,0,0).ph(),1,1e-8,"strong acid pH");
        near(h,AcidBaseEquilibrium.solve(.1,0,.01,0,0).ph(),13,1e-8,"strong base pH");
        near(h,AcidBaseEquilibrium.solve(.1,.001,.001,0,0).ph(),7,1e-8,"strong equivalence pH");
        double dilute=AcidBaseEquilibrium.solve(1,1e-8,0,0,0).ph();
        check(h,dilute>6.9&&dilute<7,"dilute acid became alkaline or lost water autoionization");
        near(h,AcidBaseEquilibrium.solve(1,0,0,.1,0).ph(),2.8753,.0002,"weak acid pH");
        near(h,AcidBaseEquilibrium.solve(1,0,0,.05,.05).ph(),-Math.log10(AcidBaseEquilibrium.ACETIC_KA),.001,"buffer half equivalence");
        double salt=AcidBaseEquilibrium.solve(1,0,0,0,.1).ph();
        check(h,salt>8.8&&salt<8.9,"acetate hydrolysis missing");
        for(double invalid:new double[]{Double.NaN,-1,Double.POSITIVE_INFINITY,0}) {
            boolean rejected=false;
            try { AcidBaseEquilibrium.solve(invalid,0,0,0,0); } catch(IllegalArgumentException expected){rejected=true;}
            check(h,rejected,"invalid volume accepted");
        }
        h.succeed();
    }); }
    public static void vessels(GameTestHelper h) { h.runAtTickTime(1,()->{
        for(String acid:List.of("hydrochloric_acid","hydrochloric_acid_concentrated","nitric_acid","nitric_acid_concentrated","acetic_acid")) {
            for(String metal:List.of("sodium","potassium")) {
                var sample=vessel();String acidSpecies=acid.startsWith("hydrochloric")?"aqueous_hcl":acid.startsWith("nitric")?"aqueous_hno3":"acetic_acid";
                LabVesselItem.addMass(sample,"liquid",acid,.004*mm(acidSpecies));
                LabVesselItem.addMass(sample,"solid",metal+"_hydroxide",.004*mm("solid:"+metal+"_hydroxide"));
                var before=SolutionSpecies.analyticalSnapshot(sample);
                check(h,ReactionEngine.checkAndStart(sample,null),"wet neutralization not acknowledged");
                phase(sample);var after=SolutionSpecies.analyticalSnapshot(sample);
                check(h,Conservation.compare(before,after).conserved(),"neutralization lost matter");
                double ph=AcidBaseEquilibrium.read(sample).ph();
                if(acid.equals("acetic_acid"))check(h,ph>8,"weak acid equivalence falsely neutral");
                else near(h,ph,7,.00001,"strong acid equivalence");
                check(h,Conservation.compare(after,SolutionSpecies.snapshot(sample)).conserved(),"derived pH ions changed elements, charge or mass");
                check(h,!ReactionEngine.checkAndStart(sample,null),"legacy neutralization double-counted product");
                var restored=new ItemStack(ModItems.ERLENMEYER_FLASK.get());restored.applyComponents(sample.getComponents());
                near(h,AcidBaseEquilibrium.read(restored).ph(),ph,1e-10,"pH changed on component restoration");
            }
        }
        var unsupported=vessel();LabVesselItem.addMass(unsupported,"liquid","sodium_carbonate_solution",1);
        check(h,AcidBaseEquilibrium.read(unsupported)==null,"unsupported carbonate given precise pH");
        var small=vessel();double extent=1e-6;
        LabVesselItem.addMass(small,"liquid","hydrochloric_acid",extent*mm("aqueous_hcl"));
        LabVesselItem.addMass(small,"liquid","sodium_hydroxide_solution",extent*mm("solid:sodium_hydroxide"));
        var before=SolutionSpecies.analyticalSnapshot(small);phase(small);
        near(h,AcidBaseEquilibrium.read(small).ph(),7,.00001,"small addition below legacy threshold did not neutralize");
        check(h,Conservation.compare(before,SolutionSpecies.snapshot(small)).conserved(),"small addition lost matter");
        h.succeed();
    }); }
    public static void titration(GameTestHelper h) { h.runAtTickTime(1,()->{
        var sample=vessel();LabVesselItem.addMass(sample,"liquid","acetic_acid",.001*mm("acetic_acid"));
        double initial=AcidBaseEquilibrium.read(sample).ph();
        LabVesselItem.addMass(sample,"liquid","sodium_hydroxide_solution",.0005*mm("solid:sodium_hydroxide"));phase(sample);
        double half=AcidBaseEquilibrium.read(sample).ph();
        near(h,half,-Math.log10(AcidBaseEquilibrium.ACETIC_KA),.01,"half equivalence did not form buffer");
        LabVesselItem.addMass(sample,"liquid","sodium_hydroxide_solution",.0005*mm("solid:sodium_hydroxide"));phase(sample);
        double equivalent=AcidBaseEquilibrium.read(sample).ph();
        check(h,equivalent>8&&equivalent<9,"weak-acid equivalence wrong");
        LabVesselItem.addMass(sample,"liquid","sodium_hydroxide_solution",.0001*mm("solid:sodium_hydroxide"));phase(sample);
        check(h,AcidBaseEquilibrium.read(sample).ph()>10&&initial<half&&half<equivalent,"titration curve did not cross equivalence");
        var buffer=vessel();LabVesselItem.addMass(buffer,"liquid","acetic_acid",.001*mm("acetic_acid"));
        LabVesselItem.addMass(buffer,"solid","sodium_acetate",.001*mm("solid:sodium_acetate"));phase(buffer);
        double previous=AcidBaseEquilibrium.read(buffer).ph();
        LabVesselItem.addMass(buffer,"liquid","hydrochloric_acid",.00001*mm("aqueous_hcl"));
        var before=SolutionSpecies.analyticalSnapshot(buffer);phase(buffer);
        check(h,Math.abs(AcidBaseEquilibrium.read(buffer).ph()-previous)<.1,"buffer did not resist added acid");
        check(h,Conservation.compare(before,SolutionSpecies.snapshot(buffer)).conserved(),"buffer protonation lost mass");
        LabVesselItem.addMass(buffer,"liquid","water",100);phase(buffer);
        near(h,AcidBaseEquilibrium.read(buffer).ph(),previous,.03,"buffer dilution behaved as strong acid");
        var dropper=new ItemStack(ModItems.DROPPER.get());DropperHelper.fill(dropper,"hydrochloric_acid",2);
        var receiver=vessel();double volume=LabVesselItem.usedVolume(receiver);
        check(h,DropperHelper.pour(dropper,receiver,true),"fine addition failed");
        check(h,DropperHelper.getMl(dropper)==1,"fine addition did not deduct 1mL");
        near(h,LabVesselItem.usedVolume(receiver)-volume,1,1e-7,"fine addition volume");
        check(h,DropperHelper.pour(dropper,receiver,false)&&DropperHelper.isEmpty(dropper),"last partial addition failed");
        near(h,LabVesselItem.usedVolume(receiver)-volume,2,1e-7,"partial dropper duplicated liquid");
        var full=new ItemStack(ModItems.ERLENMEYER_FLASK.get());LabVesselItem.addMass(full,"liquid","water",250);
        DropperHelper.fill(dropper,"hydrochloric_acid",2);
        check(h,!DropperHelper.pour(dropper,full,true)&&DropperHelper.getMl(dropper)==2,"full receiver consumed titrant");
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var heldDropper=new ItemStack(ModItems.DROPPER.get());DropperHelper.fill(heldDropper,"hydrochloric_acid",10);
        var heldVessel=vessel();player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,heldDropper);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,heldVessel);player.setShiftKeyDown(true);
        var event=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem(
                player,net.minecraft.world.InteractionHand.MAIN_HAND);
        LabInteractions.onRightClickItem(event);
        check(h,event.isCanceled(),"fine addition did not cancel subsequent dropper use");
        check(h,DropperHelper.getMl(heldDropper)==9,"actual interaction did not deduct one mL");
        near(h,LabVesselItem.usedVolume(heldVessel),101,1e-7,"actual held-vessel fine addition volume");
        h.succeed();
    }); }
    public static void sampling(GameTestHelper h) { h.runAtTickTime(1,()->{
        var sample=vessel();LabVesselItem.addMass(sample,"liquid","acetic_acid",.001*mm("acetic_acid"));
        LabVesselItem.addMass(sample,"liquid","sodium_acetate_solution",.001*mm("solid:sodium_acetate"));phase(sample);
        double ph=AcidBaseEquilibrium.read(sample).ph();
        var lines=new ArrayList<Component>();AcidBaseEquilibrium.appendInfo(lines,sample,true);
        check(h,lines.stream().anyMatch(c->c.getString().contains("pH")),"goggle pH missing");
        var player=h.makeMockPlayer(GameType.SURVIVAL);var rod=new ItemStack(ModItems.GLASS_ROD.get());
        var before=SolutionSpecies.analyticalSnapshot(sample);
        check(h,GlassRodSampling.dip(player,rod,sample),"buffer sampling failed");
        var stored=rod.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        near(h,stored.getDoubleOr("rod_sample_ph",0),ph,1e-7,"paper and goggles disagree");
        check(h,stored.getBooleanOr("rod_sample_equilibrium",false),"supported sample marked unmodelled");
        var entries=new ArrayList<LabVesselItem.Entry>();
        for(var tag:stored.getListOrEmpty("rod_sample"))if(tag instanceof net.minecraft.nbt.CompoundTag t)
            entries.add(new LabVesselItem.Entry("liquid",t.getStringOr("id",""),t.getDoubleOr("grams",0)));
        var aliquot=new ItemStack(ModItems.ERLENMEYER_FLASK.get());
        for(var e:entries)LabVesselItem.addMass(aliquot,e.type(),e.id(),e.amount());
        check(h,Conservation.compare(before,SolutionSpecies.analyticalSnapshot(sample).plus(SolutionSpecies.analyticalSnapshot(aliquot))).conserved(),"sample removal lost matter");
        var paper=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry","ph_test_paper")),2);
        check(h,GlassRodSampling.test(player,rod,paper)&&paper.getCount()==1,"sample or paper not consumed once");
        check(h,!GlassRodSampling.test(player,rod,paper),"sample reused");
        var unknown=vessel();LabVesselItem.addMass(unknown,"liquid","sodium_carbonate_solution",1);
        check(h,GlassRodSampling.dip(player,rod,unknown),"unmodelled sample unavailable");
        check(h,!rod.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("rod_sample_equilibrium",true),"unmodelled paper reading mislabeled");
        h.succeed();
    }); }
    private AcidBaseGameTests() {}
}
