package com.example.chemistry;

import com.example.chemistry.entity.*;
import com.example.chemistry.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.*;

public final class ThermometerSleeveGameTests {
    private static void check(GameTestHelper h, boolean ok, String message) { h.assertTrue(ok,Component.literal(message)); }
    private static PlacedVesselEntity vessel(GameTestHelper h) {
        var v=new PlacedVesselEntity(ModEntities.PLACED_VESSEL.get(),h.getLevel());
        var pos=h.absolutePos(new BlockPos(2,1,2));v.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        v.setVessel(new ItemStack(ModItems.THREE_NECK_FLASK.get()));h.getLevel().addFreshEntity(v);return v;
    }
    private static ItemStack sleeve() { return new ItemStack(ModItems.THERMOMETER_SLEEVE.get()); }
    public static void sockets(GameTestHelper h) { h.runAtTickTime(1,()->{
        var v=vessel(h);
        for(float yaw:new float[]{0,90,180,270}) {
            v.setMount(.6F,.2,.5,.1,yaw);
            for(int port=0;port<3;port++) {
                var s=ThermometerSleeves.install(v,port,sleeve());check(h,s!=null,"free neck rejected");
                check(h,s.position().distanceTo(ThermometerSleeves.mount(v,port).position())<1e-6,"mount shifted");
                check(h,s.yaw()==yaw && s.tilt()==(port==0?35:port==2?-35:0),"side neck orientation wrong");
                check(h,ThermometerSleeves.install(v,port,sleeve())==null,"duplicate socket occupancy");
            }
            check(h,ThermometerSleeves.attached(v).size()==3,"necks not independent");
            ThermometerSleeves.attached(v).forEach(Entity::discard);
        }
        var stack=v.getVessel();VesselHeating.setNeckStopper(stack,0,true);v.setVessel(stack);
        check(h,ThermometerSleeves.install(v,0,sleeve())==null,"glass stopper overlap");
        VesselHeating.sealNeck(stack,2,1);v.setVessel(stack);
        check(h,ThermometerSleeves.install(v,2,sleeve())==null,"rubber stopper overlap");
        v.setVessel(new ItemStack(ModItems.ERLENMEYER_FLASK.get()));
        check(h,ThermometerSleeves.install(v,1,sleeve())==null,"plain mouth accepted as ground joint");
        v.setVessel(new ItemStack(ModItems.GROUND_GLASS_ERLENMEYER.get()));
        check(h,ThermometerSleeves.install(v,1,sleeve())!=null,"ground erlenmeyer rejected");
        v.discard();h.succeed();
    }); }
    public static void inventory(GameTestHelper h) { h.runAtTickTime(1,()->{
        var v=vessel(h);var s=ThermometerSleeves.install(v,1,sleeve());
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var thermometer=new ItemStack(ModItems.THERMOMETER.get());thermometer.set(DataComponents.CUSTOM_NAME,Component.literal("保存温度计"));
        player.setItemInHand(InteractionHand.MAIN_HAND,thermometer);
        s.interact(player,InteractionHand.MAIN_HAND);
        check(h,player.getMainHandItem().isEmpty() && ThermometerSleeves.hasThermometer(v),"thermometer not consumed/stored");
        check(h,s.virtualHitbox().contains(s.position().add(0,.4,0)),"thermometer outside pick bounds");
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());s.saveWithoutId(output);
        var copy=new ThermometerSleeveEntity(ModEntities.THERMOMETER_SLEEVE.get(),h.getLevel());
        copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),output.buildResult()));
        check(h,copy.ownerId().equals(v.getUUID().toString()) && copy.port()==1,"saved owner lost");
        check(h,ItemStack.isSameItemSameComponents(copy.thermometer(),s.thermometer()),"thermometer metadata lost");
        check(h,copy.contents().size()==2,"saved drops incomplete");
        s.interact(player,InteractionHand.MAIN_HAND);
        check(h,s.thermometer().isEmpty() && !s.isRemoved(),"empty-hand removal removed adapter");
        check(h,player.getInventory().countItem(ModItems.THERMOMETER.get())==1,"thermometer duplicated/lost");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        player.setShiftKeyDown(true);s.interact(player,InteractionHand.MAIN_HAND);
        check(h,s.isRemoved() && player.getInventory().countItem(ModItems.THERMOMETER_SLEEVE.get())==1,"adapter pickup failed");
        v.discard();h.succeed();
    }); }
    public static void headInteraction(GameTestHelper h) { h.runAtTickTime(1,()->{
        var source=vessel(h);source.setMount(.6F,.2,.5,.1,0);
        var stand=new IronStandEntity(ModEntities.IRON_STAND.get(),h.getLevel());
        stand.setPos(source.position());h.getLevel().addFreshEntity(stand);
        var head=new DistillationPartEntity(ModEntities.DISTILLATION_PART.get(),h.getLevel());
        head.setStand(stand);head.setKind(DistillationPartEntity.HEAD);
        head.setPos(DistillationAssembly.mouth(stand).add(0,-.3,0));h.getLevel().addFreshEntity(head);
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var top=DistillationAssembly.headTop(stand);
        player.setPos(top.x,top.y-player.getEyeHeight(),top.z+2);
        player.setYRot(180);player.setXRot(0);
        for(Entity hit:new Entity[]{source,head,stand}) {
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.THERMOMETER_SLEEVE.get(),2));
            hit.interact(player,InteractionHand.MAIN_HAND);
            check(h,ThermometerSleeves.occupied(head,0),"head-top click blocked by "+hit.getType());
            check(h,!ThermometerSleeves.occupied(source,1),"head click attached to flask");
            check(h,player.getMainHandItem().getCount()==1,"installation item count wrong");
            hit.interact(player,InteractionHand.MAIN_HAND);
            check(h,ThermometerSleeves.attached(head).size()==1 && player.getMainHandItem().getCount()==1,
                    "repeated head click consumed/duplicated adapter");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.THERMOMETER.get()));
            source.interact(player,InteractionHand.MAIN_HAND);
            check(h,ThermometerSleeves.hasThermometer(head) && player.getMainHandItem().isEmpty(),
                    "source hitbox blocked thermometer insertion");
            ThermometerSleeves.attached(head).forEach(Entity::discard);
        }
        // The visible top rim should be selectable without an exact center pixel.
        player.setPos(top.x+.11,top.y-player.getEyeHeight(),top.z+2);
        player.setItemInHand(InteractionHand.MAIN_HAND,sleeve());
        source.interact(player,InteractionHand.MAIN_HAND);
        check(h,ThermometerSleeves.occupied(head,0),"head rim target too small");
        ThermometerSleeves.attached(head).forEach(Entity::discard);
        var legacy=new DistillationPartEntity(ModEntities.DISTILLATION_PART.get(),h.getLevel());
        legacy.setStand(stand);legacy.setKind(DistillationPartEntity.THERMOMETER);legacy.setPos(top);h.getLevel().addFreshEntity(legacy);
        player.setItemInHand(InteractionHand.MAIN_HAND,sleeve());
        head.interact(player,InteractionHand.MAIN_HAND);
        check(h,!ThermometerSleeves.occupied(head,0) && player.getMainHandItem().getCount()==1,
                "existing direct thermometer must reserve the top port");
        legacy.discard();head.discard();source.discard();stand.discard();h.succeed();
    }); }
    public static void lifecycle(GameTestHelper h) { h.runAtTickTime(1,()->{
        var v=vessel(h);var s=ThermometerSleeves.install(v,1,sleeve());s.setThermometer(new ItemStack(ModItems.THERMOMETER.get()));
        var area=v.getBoundingBox().inflate(2);
        h.getLevel().getEntitiesOfClass(ItemEntity.class,area).forEach(Entity::discard);
        v.discard();
        check(h,s.isRemoved(),"orphan after host pickup");
        int count=h.getLevel().getEntitiesOfClass(ItemEntity.class,area).stream().mapToInt(e->e.getItem().getCount()).sum();
        check(h,count==2,"parent removal lost or duplicated parts");
        var other=vessel(h);var kept=ThermometerSleeves.install(other,0,sleeve());
        other.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        check(h,!kept.isRemoved(),"chunk unload destroyed sleeve");kept.discard();
        var stand=new IronStandEntity(ModEntities.IRON_STAND.get(),h.getLevel());stand.setPos(v.getX(),v.getY(),v.getZ());h.getLevel().addFreshEntity(stand);
        var flask=vessel(h);flask.setMount(.6F,.2,.5,.1,0); // head attachment also needs a valid mounted source
        var head=new DistillationPartEntity(ModEntities.DISTILLATION_PART.get(),h.getLevel());head.setStand(stand);head.setKind(DistillationPartEntity.HEAD);head.setPos(stand.position());h.getLevel().addFreshEntity(head);
        var mount=ThermometerSleeves.mount(head,0);
        if(mount!=null && mount.position()!=null) {
            var hs=ThermometerSleeves.install(head,0,sleeve());check(h,hs!=null,"head sleeve rejected");
            head.discard();check(h,hs.isRemoved(),"head removal left adapter");
        } else { check(h,false,"test mounted source unavailable"); }
        flask.discard();stand.discard();h.succeed();
    }); }
}
