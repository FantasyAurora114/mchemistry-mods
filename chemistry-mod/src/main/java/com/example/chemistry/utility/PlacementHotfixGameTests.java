package com.example.chemistry.utility;

import com.example.chemistry.block.LaboratoryBenchBlock;
import com.example.chemistry.entity.*;
import com.example.chemistry.item.*;
import com.example.chemistry.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class PlacementHotfixGameTests {
    private static void check(GameTestHelper h,boolean ok,String message){h.assertTrue(ok,Component.literal(message));}
    public static void placement(GameTestHelper h){h.runAtTickTime(1,()->{
        var pos=h.absolutePos(new BlockPos(2,1,2));
        var state=ModBlocks.LAB_TABLE.get().defaultBlockState();h.getLevel().setBlock(pos,state,3);
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var stack=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry","beaker_medium")),2);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var point=new Vec3(pos.getX()+.3,pos.getY()+1,pos.getZ()+.65);
        var hit=new BlockHitResult(point,Direction.UP,pos,false);
        state.useItemOn(stack,h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        var placed=h.getLevel().getEntitiesOfClass(PlacedVesselEntity.class,new AABB(pos).inflate(2));
        check(h,placed.size()==1,"bench swallowed beaker placement");var vessel=placed.getFirst();
        check(h,vessel.position().distanceTo(point.add(0,.002,0))<1e-6,"beaker did not land at clicked tabletop");
        check(h,stack.getCount()==1&&vessel.getVessel().getCount()==1,"placement duplicates stacked beakers");
        var side=new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false);
        stack.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,side));
        check(h,h.getLevel().getEntitiesOfClass(PlacedVesselEntity.class,new AABB(pos).inflate(2)).size()==1,"side click places beaker below bench");h.succeed();
    });}
    public static void ports(GameTestHelper h){h.runAtTickTime(1,()->{
        var pos=h.absolutePos(new BlockPos(2,1,2));h.getLevel().setBlock(pos,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var stack=new ItemStack(ModItems.RUBBER_TUBE.get(),2);RubberTubeItem.setWet(stack);
        var nozzle=RubberTubeEntity.Port.nozzle(pos,Direction.EAST);
        RubberTubeItem.setPending(stack,nozzle);check(h,nozzle.equals(RubberTubeItem.readPending(stack)),"nozzle loses endpoint type during two-click connection");RubberTubeItem.clearPending(stack);
        var invalid=RubberTubeEntity.Port.block(pos,Direction.UP);
        check(h,!RubberTubeItem.validPort(h.getLevel(),invalid),"ordinary block accepted as hose port");
        check(h,!RubberTubeItem.validPort(h.getLevel(),RubberTubeEntity.Port.entity(player.getUUID())),"player accepted as hose port");
        RubberTubeItem.startPending(h.getLevel(),player,stack,invalid);check(h,RubberTubeItem.readPending(stack)==null,"invalid port creates floating preview");
        RubberTubeItem.createTube(h.getLevel(),player,stack,invalid,RubberTubeEntity.Port.entity(player.getUUID()));
        check(h,stack.getCount()==2,"invalid connection consumes hose");
        var stand=new IronStandEntity(ModEntities.IRON_STAND.get(),h.getLevel());stand.setPos(Vec3.atBottomCenterOf(pos.above()));h.getLevel().addFreshEntity(stand);
        var vessel=new PlacedVesselEntity(ModEntities.PLACED_VESSEL.get(),h.getLevel());vessel.setPos(stand.position().add(0,.59375,0));vessel.setMount(.6F,.2,.59375,.2,0);vessel.setMountedStandId(stand.getUUID().toString());vessel.setVessel(new ItemStack(ModItems.ROUND_BOTTOM_FLASK.get()));vessel.setAttached1(new ItemStack(ModItems.STRAIGHT_GLASS_TUBE.get()));h.getLevel().addFreshEntity(vessel);
        var port=RubberTubeEntity.Port.stand(stand.blockPosition(),1);check(h,RubberTubeItem.validPort(h.getLevel(),port),"installed glass port rejected");
        vessel.setAttached1(ItemStack.EMPTY);check(h,!RubberTubeItem.validPort(h.getLevel(),port),"removed glass tube remains valid port");h.succeed();
    });}
    public static void sink(GameTestHelper h){h.runAtTickTime(1,()->{
        var pos=h.absolutePos(new BlockPos(2,1,2));
        for(var direction:new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}){
            var state=ModBlocks.LAB_TABLE_SINK.get().defaultBlockState().setValue(LaboratoryBenchBlock.FACING,direction);
            var shape=state.getCollisionShape(h.getLevel(),pos,CollisionContext.empty());
            var point=com.example.chemistry.block.BenchGeometry.world(state,pos,new Vec3(8,20,9.85));
            var inverse=com.example.chemistry.block.BenchGeometry.local(state,pos,point);
            check(h,inverse.distanceTo(new Vec3(8,20,9.85))<1e-8,"sink click transform is not inverse of model transform");
            check(h,shape.clip(point.add(0,0,-.1),point.add(0,0,.1),pos)!=null,"faucet spout missing collision for "+direction);
            check(h,shape.max(Direction.Axis.Y)>=22/16.0,"faucet height truncated");
            final boolean[] basin={false};shape.forAllBoxes((a,b,c,d,e,f)->{if(.5>a&&.5<d&&.5>c&&.5<f&&.9>b&&.9<e)basin[0]=true;});
            check(h,!basin[0],"sink basin blocked by collision");
        }h.succeed();
    });}
    public static void waterTank(GameTestHelper h){h.runAtTickTime(1,()->{
        var pos=h.absolutePos(new BlockPos(2,1,2));var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for(var facing:new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}){
            var state=ModBlocks.LAB_TABLE_SINK.get().defaultBlockState().setValue(LaboratoryBenchBlock.FACING,facing);h.getLevel().setBlock(pos,state,3);
            var tank=(com.example.chemistry.blockentity.LaboratoryBenchBlockEntity)h.getLevel().getBlockEntity(pos);tank.consumeWater(tank.waterMl());
            for(int i=0;i<4;i++){
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
                var point=com.example.chemistry.block.BenchGeometry.world(state,pos,new Vec3(8,8,.1));
                state.useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,new BlockHitResult(point,facing,pos,false));
                check(h,player.getMainHandItem().is(Items.BUCKET),"fill fails to return empty bucket");
            }
            check(h,tank.waterMl()==4000,"tank does not accept four buckets");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
            LaboratoryBenchBlock.fillFromBucket(player.getMainHandItem(),state,h.getLevel(),pos,player,InteractionHand.MAIN_HAND);
            check(h,tank.waterMl()==4000&&player.getMainHandItem().is(Items.WATER_BUCKET),"overflow destroys water bucket");
            if(tank.hotSetting()==0)tank.nextHot();
            var beaker=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry","beaker_medium"));
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(beaker));
            var point=com.example.chemistry.block.BenchGeometry.world(state,pos,new Vec3(8,11.5,8));
            state.useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,new BlockHitResult(point,Direction.UP,pos,false));
            check(h,tank.waterMl()==3975,"collecting 25 mL fails to debit tank");
            check(h,com.example.chemistry.item.LabVesselItem.getContents(player.getMainHandItem()).stream().anyMatch(c->c.id().equals("calcium_chloride_solution")),"tap water incorrectly pure");
            tank.consumeWater(tank.waterMl());check(h,tank.waterMl()==0,"tank cannot empty");
        }h.succeed();
    });}
    private PlacementHotfixGameTests(){}
}
