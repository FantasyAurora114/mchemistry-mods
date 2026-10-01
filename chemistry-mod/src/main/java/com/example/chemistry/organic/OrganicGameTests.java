package com.example.chemistry.organic;

import com.example.chemistry.*;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.solution.*;
import com.example.chemistry.titration.LiquidTransfer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class OrganicGameTests {
    private static void check(GameTestHelper h,boolean ok,String msg){h.assertTrue(ok,Component.literal(msg));}
    private static void near(GameTestHelper h,double actual,double expected,double eps,String msg){check(h,Math.abs(actual-expected)<eps,msg+": "+actual+" != "+expected);}
    private static ItemStack vessel(){return new ItemStack(ModItems.ERLENMEYER_FLASK.get());}
    private static ItemStack mixture(String organic){var s=vessel();LabVesselItem.addMass(s,"liquid","water",100);LabVesselItem.addMass(s,"liquid",organic,20);return s;}
    private static double mass(ItemStack s,String id){return LabVesselItem.getContents(s).stream().filter(e->e.id().equals(id)).mapToDouble(LabVesselItem.Entry::amount).sum();}
    public static void phases(GameTestHelper h){h.runAtTickTime(1,()->{
        for(String id:new String[]{"toluene","carbon_tetrachloride"}){
            var s=mixture(id);var before=SolutionSpecies.analyticalSnapshot(s);var state=LiquidPhases.read(s);
            check(h,state.separated(),"binary did not separate");check(h,(id.equals("toluene")?state.top():state.bottom()).name().equals(id),"density ordering wrong");
            var aq=state.layers().stream().filter(l->l.name().equals("aqueous")).findFirst().orElseThrow();
            near(h,aq.entries().stream().filter(e->e.id().equals(id)).mapToDouble(LabVesselItem.Entry::amount).sum(),id.equals("toluene")?.07:.05,1e-9,"finite water solubility");
            near(h,state.layers().stream().mapToDouble(LiquidPhases.Layer::ml).sum(),LabVesselItem.usedVolume(s),1e-8,"phase volume mismatch");
            check(h,Conservation.compare(before,SolutionSpecies.analyticalSnapshot(s)).conserved(),"phase view mutated material");
            var dilute=vessel();LabVesselItem.addMass(dilute,"liquid","water",100);LabVesselItem.addMass(dilute,"liquid",id,.01);check(h,LiquidPhases.read(dilute).layers().size()==1,"trace solvent falsely separate");
        }
        for(String id:new String[]{"ethanol","methanol","acetone","glycerol"}){var s=mixture(id);check(h,LiquidPhases.read(s).modelled()&&LiquidPhases.read(s).layers().size()==1,"miscible liquid separated");}
        var ternary=mixture("toluene");LabVesselItem.addMass(ternary,"liquid","ethanol",1);check(h,!LiquidPhases.read(ternary).modelled(),"ternary cosolvent guessed");
        check(h,LiquidTransfer.sample(ternary,vessel(),1,false)==0,"unmodelled sample claimed layer");h.succeed();
    });}
    public static void transfer(GameTestHelper h){h.runAtTickTime(1,()->{
        var s=mixture("toluene");LabVesselItem.addMass(s,"liquid","sodium_chloride_solution",1);LabVesselItem.addMass(s,"solid","copper",2);
        TemperatureSystem.setRawTemp(s,60);var top=vessel();var before=SolutionSpecies.analyticalSnapshot(s);
        double initialEnergy=ThermalSystem.capacity(s)*60+ThermalSystem.capacity(top)*TemperatureSystem.getTemp(top);
        near(h,LiquidTransfer.sample(s,top,5,false),5,1e-7,"top aliquot volume");near(h,mass(top,"water"),0,1e-9,"top carried water");near(h,mass(top,"sodium_chloride_solution"),0,1e-9,"top carried salt");
        check(h,Conservation.compare(before,SolutionSpecies.analyticalSnapshot(s).plus(SolutionSpecies.analyticalSnapshot(top))).conserved(),"top material transaction lost matter");
        near(h,ThermalSystem.capacity(s)*TemperatureSystem.getTemp(s)+ThermalSystem.capacity(top)*TemperatureSystem.getTemp(top),initialEnergy,1e-5,"phase aliquot heat lost");
        var bottom=vessel();double salt=mass(s,"sodium_chloride_solution"),water=mass(s,"water");LiquidTransfer.sample(s,bottom,10,true);
        near(h,mass(bottom,"sodium_chloride_solution")/mass(bottom,"water"),salt/water,1e-9,"aqueous solute proportion wrong");near(h,mass(s,"copper"),2,1e-9,"pipette took sediment");
        check(h,mass(bottom,"toluene")>0,"dissolved organic trace vanished");
        var allTop=vessel();double upper=LiquidPhases.read(s).top().ml();near(h,LiquidTransfer.sample(s,allTop,100,false),upper,1e-7,"sample crossed interface");near(h,mass(allTop,"water"),0,1e-9,"boundary overdraw water");
        var full=vessel();LabVesselItem.addMass(full,"liquid","water",250);double m=mass(s,"water");check(h,LiquidTransfer.sample(s,full,1,true)==0,"full receiver accepted sample");near(h,mass(s,"water"),m,1e-9,"failed sample lost water");
        VesselHeating.seal(s,1);check(h,LiquidTransfer.sample(s,vessel(),1,true)==0,"sealed vessel sampled");
        var boiling=mixture("toluene");TemperatureSystem.setRawTemp(boiling,100);ThermalSystem.addHeat(boiling,500,"test_heater");
        var organicOnly=vessel();LiquidTransfer.sample(boiling,organicOnly,5,false);
        near(h,ThermalSystem.boilingEnergy(boiling),500,1e-7,"organic aliquot stole water latent budget");
        near(h,ThermalSystem.boilingEnergy(organicOnly),0,1e-7,"nonwater sample gained water latent budget");
        h.succeed();
    });}
    public static void mixingPersistence(GameTestHelper h){h.runAtTickTime(1,()->{
        var s=mixture("carbon_tetrachloride");LabVesselItem.updateTint(s);check(h,s.get(DataComponents.CUSTOM_MODEL_DATA).strings().get(0).startsWith("layers_"),"dual layer inventory state absent");
        check(h,s.get(DataComponents.CUSTOM_MODEL_DATA).colors().size()==2,"inventory layer tints absent");
        var initial=SolutionSpecies.analyticalSnapshot(s);PhaseSystem.dissolveAndCrystallize(s,true);check(h,LiquidPhases.read(s).dispersed(),"stirring did not disperse");
        var restored=vessel();restored.applyComponents(s.getComponents());check(h,LiquidPhases.read(restored).dispersed(),"restoration lost mixing timer");
        var sample=vessel();LiquidTransfer.sample(s,sample,5,false);check(h,mass(sample,"water")>0&&mass(sample,"carbon_tetrachloride")>0,"dispersed sample falsely selected pure phase");
        check(h,Conservation.compare(initial,SolutionSpecies.analyticalSnapshot(s).plus(SolutionSpecies.analyticalSnapshot(sample))).conserved(),"dispersed transfer lost matter");
        for(int i=0;i<55;i++)PhaseSystem.dissolveAndCrystallize(restored,false);
        check(h,LiquidPhases.read(restored).separated(),"standing did not separate again");
        check(h,Conservation.compare(initial,SolutionSpecies.analyticalSnapshot(restored)).conserved(),"settling altered mass");
        near(h,LiquidPhases.read(restored).bottom().ml(),LiquidPhases.read(mixture("carbon_tetrachloride")).bottom().ml(),1e-8,"restored interface wrong");h.succeed();
    });}
    public static void interaction(GameTestHelper h){h.runAtTickTime(1,()->{
        var player=h.makeMockPlayer(GameType.SURVIVAL);var source=mixture("toluene");var pipette=new ItemStack(ModItems.PHASE_PIPETTE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,pipette);player.setItemInHand(InteractionHand.OFF_HAND,source);
        var event=new PlayerInteractEvent.RightClickItem(player,InteractionHand.MAIN_HAND);LabInteractions.onRightClickItem(event);
        check(h,event.isCanceled()&&event.getCancellationResult()==InteractionResult.SUCCESS,"pipette packet not consumed");check(h,mass(pipette,"toluene")>0&&mass(pipette,"water")==0,"top interaction sampled wrong phase");
        double before=com.example.chemistry.filtration.Filtration.liquidVolume(pipette);var receiving=vessel();player.setItemInHand(InteractionHand.OFF_HAND,receiving);player.setShiftKeyDown(true);
        event=new PlayerInteractEvent.RightClickItem(player,InteractionHand.MAIN_HAND);LabInteractions.onRightClickItem(event);
        near(h,before-com.example.chemistry.filtration.Filtration.liquidVolume(pipette),1,1e-7,"fine interaction duplicate debit");
        var lower=new ItemStack(ModItems.PHASE_PIPETTE.get());check(h,LabInteractions.interactPlacedVessel(lower,source,ItemStack.EMPTY,ItemStack.EMPTY,player),"placed vessel did not accept pipette");check(h,mass(lower,"water")>0,"sneak did not sample lower aqueous layer");
        var rod=new ItemStack(ModItems.GLASS_ROD.get());check(h,GlassRodSampling.dip(player,rod,mixture("toluene")),"top rod sample failed");
        var tag=rod.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();check(h,!tag.getBooleanOr("rod_sample_aqueous",true),"organic rod sample called aqueous");
        var paper=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry","ph_test_paper")));check(h,GlassRodSampling.test(player,rod,paper),"organic sample paper test failed");
        boolean found=false;for(int i=0;i<player.getInventory().getContainerSize();i++){var item=player.getInventory().getItem(i);if(item.is(ModItems.USED_PH_PAPER.get())){found=true;check(h,!item.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("ph_applicable",true),"nonwater sample displayed numerical pH");}}
        check(h,found,"paper not returned");
        var stand=new com.example.chemistry.entity.IronStandEntity(com.example.chemistry.registry.ModEntities.IRON_STAND.get(),h.getLevel());stand.setAttachment(1);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.PHASE_PIPETTE.get()));stand.interact(player,InteractionHand.MAIN_HAND);
        check(h,!player.getMainHandItem().isEmpty()&&stand.findMountedVessel()==null,"pipette accidentally installed as flask");
        h.succeed();
    });}
    private OrganicGameTests(){}
}
