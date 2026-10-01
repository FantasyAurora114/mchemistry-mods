package com.example.chemistry.organic;

import com.example.chemistry.*;
import com.example.chemistry.entity.IronStandEntity;
import com.example.chemistry.filtration.Filtration;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.*;
import com.example.chemistry.solution.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.*;

public final class SeparatoryGameTests {
    private static void check(GameTestHelper h,boolean ok,String msg){h.assertTrue(ok,Component.literal(msg));}
    private static void near(GameTestHelper h,double a,double b,double eps,String msg){check(h,Math.abs(a-b)<eps,msg+": "+a+" != "+b);}
    private static ItemStack cup(){return new ItemStack(ModItems.ERLENMEYER_FLASK.get());}
    private static ItemStack mixture(String id){var s=cup();LabVesselItem.addMass(s,"liquid","water",100);LabVesselItem.addMass(s,"liquid",id,20);return s;}
    private static double mass(ItemStack s,String id){return LabVesselItem.getContents(s).stream().filter(e->e.id().equals(id)).mapToDouble(LabVesselItem.Entry::amount).sum();}
    private static SeparatoryFunnelEntity funnel(GameTestHelper h){var f=new SeparatoryFunnelEntity(ModEntities.SEPARATORY_FUNNEL.get(),h.getLevel());f.unpack(new ItemStack(ModItems.SEPARATORY_FUNNEL.get()));return f;}
    public static void flow(GameTestHelper h){h.runAtTickTime(1,()->{
        for(String id:new String[]{"toluene","carbon_tetrachloride"}){
            var f=funnel(h);var source=mixture(id);f.capped(false);while(Filtration.liquidVolume(source)>1e-8)f.fillFrom(source);
            var initial=SolutionSpecies.analyticalSnapshot(f.device());var bottom=LiquidPhases.read(f.device()).bottom();f.receiver(cup());
            check(h,f.openValve(),"valve failed to open");double total=0;for(int i=0;i<300&&f.open();i++)total+=f.drain();
            check(h,!f.open(),"interface did not stop valve");near(h,total,bottom.ml(),1e-6,"drain crossed phase boundary");
            near(h,Filtration.liquidVolume(f.receiver()),bottom.ml(),1e-6,"received phase volume");
            check(h,Filtration.liquidVolume(f.device())>1,"upper phase also drained");
            if(id.equals("toluene")){near(h,mass(f.receiver(),"water"),100,1e-6,"lower aqueous mass wrong");near(h,mass(f.receiver(),id),.07,1e-7,"dissolved organic trace wrong");}
            else {near(h,mass(f.receiver(),"water"),0,1e-8,"organic lower phase contaminated water");near(h,mass(f.device(),id),.05,1e-7,"dissolved lower solvent lost");}
            var received=f.receiver();f.receiver(cup());check(h,f.openValve(),"remaining phase could not be drained");for(int i=0;i<300&&f.open();i++)f.drain();
            near(h,Filtration.liquidVolume(f.device()),0,1e-6,"remaining single phase did not drain");
            check(h,Conservation.compare(initial,SolutionSpecies.analyticalSnapshot(received).plus(SolutionSpecies.analyticalSnapshot(f.receiver())).plus(SolutionSpecies.analyticalSnapshot(f.device()))).conserved(),"complete separation lost elements, mass or charge");
        }h.succeed();
    });}
    public static void guards(GameTestHelper h){h.runAtTickTime(1,()->{
        var f=funnel(h);var source=mixture("toluene");double initial=Filtration.liquidVolume(source);
        check(h,f.fillFrom(source)==0&&f.capped(),"plugged funnel accepted liquid");near(h,Filtration.liquidVolume(source),initial,1e-9,"failed fill consumed liquid");
        f.capped(false);f.fillFrom(source);f.receiver(cup());f.capped(true);check(h,!f.openValve(),"plugged funnel drained");f.capped(false);
        var full=cup();LabVesselItem.addMass(full,"liquid","water",250);f.receiver(full);var before=SolutionSpecies.analyticalSnapshot(f.device());f.openValve();check(h,f.drain()==0&&!f.open(),"full receiver did not stop valve");check(h,Conservation.compare(before,SolutionSpecies.analyticalSnapshot(f.device())).conserved(),"full receiver lost source");
        f.receiver(ItemStack.EMPTY);f.openValve();check(h,f.drain()==0&&!f.open(),"missing receiver drained");
        var sealed=cup();VesselHeating.seal(sealed,1);f.receiver(sealed);f.openValve();check(h,f.drain()==0&&!f.open(),"sealed receiver drained");
        var contents=f.device();PhaseSystem.dissolveAndCrystallize(contents,true);f.device(contents);check(h,!f.openValve(),"dispersed mixture falsely separated");
        for(int i=0;i<55;i++)LiquidPhases.tick(contents,false);f.device(contents);LabVesselItem.addMass(contents,"solid","copper",1);f.device(contents);check(h,!f.openValve(),"solid mixture did not require filtration");
        var unknown=new ItemStack(ModItems.SEPARATORY_FUNNEL.get());LabVesselItem.addMass(unknown,"liquid","water",100);LabVesselItem.addMass(unknown,"liquid","benzene",20);f.device(unknown);check(h,!f.openValve(),"unmodelled solvent claimed phase selection");
        h.succeed();
    });}
    public static void lifecycle(GameTestHelper h){h.runAtTickTime(1,()->{
        var stand=new IronStandEntity(ModEntities.IRON_STAND.get(),h.getLevel());var pos=h.absolutePos(new BlockPos(1,1,1));stand.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);h.getLevel().addFreshEntity(stand);
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.SEPARATORY_FUNNEL.get()));stand.interact(player,InteractionHand.MAIN_HAND);
        var f=SeparatoryConnections.attached(stand);check(h,f!=null&&player.getMainHandItem().isEmpty(),"stand installation failed");f.capped(false);
        var source=mixture("carbon_tetrachloride");while(Filtration.liquidVolume(source)>1e-8)f.fillFrom(source);f.receiver(cup());f.openValve();f.drain();
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());f.saveWithoutId(out);var loaded=funnel(h);loaded.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
        check(h,loaded.open()&&!loaded.capped()&&loaded.ownerId().equals(stand.getUUID().toString()),"save lost valve, plug or owner");
        var original=SolutionSpecies.analyticalSnapshot(f.device()).plus(SolutionSpecies.analyticalSnapshot(f.receiver()));check(h,Conservation.compare(original,SolutionSpecies.analyticalSnapshot(loaded.device()).plus(SolutionSpecies.analyticalSnapshot(loaded.receiver()))).conserved(),"save lost contents");
        for(int i=0;i<100&&loaded.open();i++)loaded.drain();near(h,mass(loaded.receiver(),"water"),0,1e-8,"saved phase budget crossed interface");check(h,!loaded.open()&&mass(loaded.device(),"water")>99,"reload did not stop lower phase");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setPos(f.control().x,f.control().y-player.getEyeHeight(),f.control().z-2);player.setYRot(0);player.setXRot(0);player.setShiftKeyDown(true);
        double before=f.delivered();f.interact(player,InteractionHand.MAIN_HAND);near(h,f.delivered()-before,1,1e-6,"sneak valve did not drain 1mL");check(h,!f.open()&&!f.isRemoved(),"single drain took funnel away");
        player.setShiftKeyDown(false);f.interact(player,InteractionHand.MAIN_HAND);check(h,f.open(),"valve click did not open");for(int i=0;i<4;i++)f.tick();check(h,f.delivered()>before+1.9,"continuous valve did not drain");
        var area=stand.getBoundingBox().inflate(3);stand.discard();check(h,f.isRemoved(),"stand break orphaned separator");check(h,h.getLevel().getEntitiesOfClass(ItemEntity.class,area).stream().filter(e->e.getItem().is(ModItems.SEPARATORY_FUNNEL.get())).count()==1,"separator duplicated on break");
        h.succeed();
    });}
    public static void geometry(GameTestHelper h){h.runAtTickTime(1,()->{
        near(h,SeparatoryGeometry.height(0),4.8,1e-9,"liquid bottom wrong");near(h,SeparatoryGeometry.height(1),13.3,1e-9,"liquid reaches stopper");
        for(int step=1;step<=20;step++){double height=SeparatoryGeometry.height(step/20.);double volume=0;for(var tier:SeparatoryGeometry.TIERS)volume+=Math.max(0,Math.min(tier[1],height)-tier[0])*(tier[3]-tier[2])*(tier[5]-tier[4]);near(h,volume/SeparatoryGeometry.VOLUME,step/20.,1e-9,"pear-shaped fill not calibrated to volume");}
        var f=funnel(h);check(h,VesselHeating.isSealed(f.device()),"fresh plug not sealed");f.capped(false);check(h,f.device().get(DataComponents.CUSTOM_MODEL_DATA).strings().contains("uncapped_empty"),"empty unstoppered model missing");
        var source=mixture("toluene");while(Filtration.liquidVolume(source)>1e-8)f.fillFrom(source);check(h,f.device().get(DataComponents.CUSTOM_MODEL_DATA).strings().get(0).startsWith("uncapped_layers_"),"dual phase icon missing");
        f.capped(true);var player=h.makeMockPlayer(GameType.SURVIVAL);player.setItemInHand(InteractionHand.MAIN_HAND,f.toStack());player.setShiftKeyDown(true);ModItems.SEPARATORY_FUNNEL.get().use(h.getLevel(),player,InteractionHand.MAIN_HAND);check(h,LiquidPhases.read(player.getMainHandItem()).dispersed(),"held shaking not persisted");
        for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL){var stand=new IronStandEntity(ModEntities.IRON_STAND.get(),h.getLevel());stand.setFacing(direction);f.owner(stand);check(h,f.virtualHitbox().contains(f.control())&&f.virtualHitbox().contains(f.mouth()),"rotated controls outside selection");check(h,f.outlet().y>stand.getY()+.39,"outlet overlaps receiver");}
        h.succeed();
    });}
    private SeparatoryGameTests(){}
}
