package com.example.chemistry;
import com.example.chemistry.entity.*;
import com.example.chemistry.item.*;
import com.example.chemistry.registry.*;
import com.example.chemistry.transfer.BottleCodes;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.*;

public final class LabVisualGameTests {
    private static void check(GameTestHelper h,boolean ok,String msg){h.assertTrue(ok,Component.literal(msg));}
    private static ItemStack tube(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath("mchemistry",id)));}
    public static void gasPlacement(GameTestHelper h){h.runAtTickTime(1,()->{
        var local=new BlockPos(2,0,2);h.setBlock(local,net.minecraft.world.level.block.Blocks.STONE);
        var block=h.absolutePos(local);var player=h.makeMockPlayer(GameType.SURVIVAL);player.setYRot(37);
        for(double offset:new double[]{.23,.75}){
            var stack=ModItems.gasBottle("hydrogen",true);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var hit=new BlockHitResult(new Vec3(block.getX()+offset,block.getY()+1,block.getZ()+.5),Direction.UP,block,false);
            stack.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
            check(h,stack.isEmpty(),"placed gas bottle not consumed");
        }
        var bottles=h.getLevel().getEntitiesOfClass(GasCollectingBottleEntity.class,new AABB(block.above()));
        check(h,bottles.size()==2,"multiple gas bottles in one block");
        for(var b:bottles){
            check(h,b.isInverted()&&b.hasPlate()&&b.getFillMl()==250,"light gas placement state");
            check(h,b.getYRot()==37&&b.virtualHitbox().getXsize()<.24,"yaw and individual selection");
        }
        var third=ModItems.emptyGasJar();player.setItemInHand(InteractionHand.MAIN_HAND,third);
        var hit=new BlockHitResult(bottles.getFirst().position(),Direction.UP,block,false);
        third.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
        check(h,third.getCount()==1,"overlap consumed bottle");
        for(var b:bottles)b.discard();h.succeed();
    });}
    public static void gasPorts(GameTestHelper h){h.runAtTickTime(1,()->{
        var origin=h.absolutePos(new BlockPos(2,1,2));
        var a=new GasCollectingBottleEntity(ModEntities.GAS_COLLECTING_BOTTLE.get(),h.getLevel());
        var b=new GasCollectingBottleEntity(ModEntities.GAS_COLLECTING_BOTTLE.get(),h.getLevel());
        a.setPos(origin.getX()+.2,origin.getY(),origin.getZ()+.5);b.setPos(origin.getX()+.75,origin.getY(),origin.getZ()+.5);
        for(var bottle:java.util.List.of(a,b)){bottle.setHasNozzle(true);bottle.setInverted(true);h.getLevel().addFreshEntity(bottle);}
        check(h,!a.nozzlePort().equals(b.nozzlePort()),"shared nozzle identity");
        check(h,GasFlowEngine.deliverTo(h.getLevel(),a.nozzlePort(),"hydrogen",23,.9)==23,"UUID gas delivery");
        check(h,a.getFillMl()==23&&b.getFillMl()==0,"gas delivered to wrong bottle");
        check(h,BottleCodes.volumeOf(a.toStack())==23,"pickup refilled partial gas");
        var water=ModItems.gasBottleWater();b.readFromItem(water);b.setInverted(true);
        check(h,b.waterMl()==250,"placed water disappeared");
        check(h,GasFlowEngine.deliverTo(h.getLevel(),b.nozzlePort(),"hydrogen",20,1)==20&&b.waterMl()==230,"water displacement accounting");
        check(h,GasFlowEngine.deliverTo(h.getLevel(),b.nozzlePort(),"oxygen",7,1)==7&&b.waterMl()==223,"water collection rejected denser gas");
        var saved=b.toStack();var restored=new GasCollectingBottleEntity(ModEntities.GAS_COLLECTING_BOTTLE.get(),h.getLevel());
        restored.setGasId(BottleCodes.gasIdOf(saved));restored.setFill(27,1);restored.readFromItem(saved);
        check(h,restored.waterMl()==223&&restored.getFillMl()==27,"partial water/gas pickup state");
        check(h,GasFlowEngine.deliverTo(h.getLevel(),a.nozzlePort(),"oxygen",5,1)==0,"density orientation ignored");
        var wire=RubberTubeEntity.create(h.getLevel(),a.nozzlePort(),b.nozzlePort(),a.nozzleTip());h.getLevel().addFreshEntity(wire);
        check(h,RubberTubeItem.hasTubeAt(h.getLevel(),a.nozzlePort()),"UUID occupation not found");
        check(h,a.nozzlePort().worldPos(h.getLevel()).distanceTo(a.nozzleTip())<1e-8,"nozzle drawing coordinate differs");
        wire.discard();a.discard();b.discard();h.succeed();
    });}
    public static void dewar(GameTestHelper h){h.runAtTickTime(1,()->{
        for(String size:new String[]{"5","10","50"}){
            var ordinary=tube("test_tube_"+size+"ml");var insulated=tube("test_tube_"+size+"ml_dewar");
            check(h,insulated.getItem() instanceof TestTubeItem t&&t.isDewar(),"Dewar item missing");
            check(h,((LabVesselItem)ordinary.getItem()).capacity()==((LabVesselItem)insulated.getItem()).capacity(),"Dewar capacity drift");
            for(var s:java.util.List.of(ordinary,insulated)){LabVesselItem.addLiquid(s,"water",10);TemperatureSystem.setTemp(s,100);}
            VesselHeating.coolGradual(ordinary);VesselHeating.coolGradual(insulated);
            double normalLoss=100-TemperatureSystem.getTemp(ordinary),dewarLoss=100-TemperatureSystem.getTemp(insulated);
            check(h,Math.abs(normalLoss/dewarLoss-8)<1e-6,"Dewar cooling ratio");
            TemperatureSystem.setTemp(insulated,0);VesselHeating.coolGradual(insulated);
            check(h,TemperatureSystem.getTemp(insulated)>0&&TemperatureSystem.getTemp(insulated)<.025,"cold Dewar did not approach room slowly");
            check(h,insulated.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA).strings().getFirst().startsWith("filled_"),"tube dynamic fill");
            check(h,tube("test_tube_"+size+"ml_dewar_clamped_stoppered_2").getItem() instanceof TestTubeItem t&&t.isClamped()&&t.stopperHoles()==2,"Dewar accessories missing");
        }
        var mounted=tube("test_tube_5ml_dewar");TemperatureSystem.setTemp(mounted,100);
        var stand=new IronStandEntity(ModEntities.IRON_STAND.get(),h.getLevel());
        stand.setPos(h.absolutePos(new BlockPos(4,1,4)).getCenter());stand.setTube(mounted);h.getLevel().addFreshEntity(stand);
        stand.tick();
        check(h,Math.abs(TemperatureSystem.getTemp(stand.getTube())-(100-.025/8))<1e-6,"mounted Dewar instantly cooled");
        var rackPos=new BlockPos(8,1,8);h.setBlock(rackPos,ModBlocks.TEST_TUBE_RACK.get());
        var rack=(com.example.chemistry.blockentity.TestTubeRackBlockEntity)h.getBlockEntity(rackPos,com.example.chemistry.blockentity.TestTubeRackBlockEntity.class);
        rack.setTube(0,mounted,false);rack.tickContents();
        check(h,Math.abs(TemperatureSystem.getTemp(rack.getTube(0))-(100-.025/8))<1e-6,"rack Dewar instantly cooled");
        stand.discard();h.succeed();
    });}
}
