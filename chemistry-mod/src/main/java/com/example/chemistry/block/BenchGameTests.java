package com.example.chemistry.block;

import com.example.chemistry.blockentity.LaboratoryBenchBlockEntity;
import com.example.chemistry.item.GlassTubeItem;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.TemperatureSystem;
import com.example.chemistry.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;

public final class BenchGameTests {
    private static void check(GameTestHelper h,boolean ok,String reason){h.assertTrue(ok,Component.literal(reason));}
    public static void storageAndTubes(GameTestHelper h){h.runAtTickTime(1,()->{
        int[] expected={7,4,4};var blocks=new LaboratoryBenchBlock[]{ModBlocks.LAB_TABLE.get(),ModBlocks.LAB_TABLE_CABINET.get(),ModBlocks.LAB_TABLE_SINK.get()};
        for(int v=0;v<3;v++){var p=h.absolutePos(new BlockPos(v+1,1,1));h.getLevel().setBlock(p,blocks[v].defaultBlockState(),3);var b=(LaboratoryBenchBlockEntity)h.getLevel().getBlockEntity(p);check(h,b!=null&&b.bays()==expected[v],"bench bays "+v);b.item(0,0,new ItemStack(Items.GLASS_BOTTLE));check(h,b.bay(0).getItem(0).is(Items.GLASS_BOTTLE)&&b.bay(1).getItem(0).isEmpty(),"bays share inventory");b.toggle(0);check(h,b.open(0)&&!b.open(1),"bay doors linked");}
        check(h,GlassTubeItem.tubeType(new ItemStack(ModItems.STRAIGHT_GLASS_TUBE_LONG.get()))==4,"long straight type");
        check(h,ModItems.tubedVariantFor(new ItemStack(ModItems.STRAIGHT_GLASS_TUBE_LONG.get()))==ModItems.STRAIGHT_GLASS_TUBE_LONG_TUBED.get(),"long straight rubber variant");
        var broken=h.absolutePos(new BlockPos(1,1,1));
        h.getLevel().removeBlock(broken,false);
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(broken).inflate(1));
        check(h,drops.stream().filter(e->e.getItem().is(Items.GLASS_BOTTLE)).mapToInt(e->e.getItem().getCount()).sum()==1,"stored item lost or duplicated on break");
        check(h,drops.stream().filter(e->e.getItem().is(ModBlocks.LAB_TABLE.get().asItem())).mapToInt(e->e.getItem().getCount()).sum()==1,"bench item lost or duplicated on break");
        h.succeed();
    });}
    public static void tapWater(GameTestHelper h){h.runAtTickTime(1,()->{
        var p=h.absolutePos(new BlockPos(2,1,2));var block=ModBlocks.LAB_TABLE_SINK.get();var state=block.defaultBlockState();h.getLevel().setBlock(p,state,3);var b=(LaboratoryBenchBlockEntity)h.getLevel().getBlockEntity(p);
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(p.getX()+.5,p.getY()+.5,p.getZ()-1.5);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
        var center=new BlockHitResult(new Vec3(p.getX()+.5,p.getY()+12.0/16,p.getZ()+8.0/16),Direction.UP,p,false);
        block.useItemOn(player.getMainHandItem(),state,h.getLevel(),p,player,InteractionHand.MAIN_HAND,center);
        check(h,b.waterMl()==1000&&player.getMainHandItem().is(Items.BUCKET),"water bucket failed to fill tank");
        var hot=new BlockHitResult(new Vec3(p.getX()+5.5/16,p.getY()+17/16,p.getZ()+13.9/16),Direction.UP,p,false);
        for(int i=0;i<4;i++)block.useWithoutItem(state,h.getLevel(),p,player,hot);
        check(h,b.hotSetting()==60,"hot knob did not reach 60 C");
        var beaker=new ItemStack(BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath("mchemistry","beaker_500ml")));player.setItemInHand(InteractionHand.MAIN_HAND,beaker);
        var sink=new BlockHitResult(new Vec3(p.getX()+.5,p.getY()+12.0/16,p.getZ()+8.0/16),Direction.UP,p,false);
        block.useItemOn(player.getMainHandItem(),state,h.getLevel(),p,player,InteractionHand.MAIN_HAND,sink);
        var filled=player.getMainHandItem();check(h,b.waterMl()==975,"tap did not debit 25 mL: remaining="+b.waterMl()+", item="+filled.getItem()+", volume="+LabVesselItem.usedVolume(filled)+", flowing="+b.flowing());
        check(h,LabVesselItem.usedVolume(filled)>24.9&&LabVesselItem.usedVolume(filled)<25.1,"tap volume wrong");
        for(String id:new String[]{"water","calcium_chloride_solution","magnesium_chloride_solution","sodium_chloride_solution"})check(h,LabVesselItem.getContents(filled).stream().anyMatch(e->e.id().equals(id)&&e.amount()>0),"tap impurity missing: "+id);
        check(h,TemperatureSystem.getTemp(filled)>25&&TemperatureSystem.getTemp(filled)<60,"hot tap mixing temperature wrong");
        player.discard();h.succeed();
    });}
}
