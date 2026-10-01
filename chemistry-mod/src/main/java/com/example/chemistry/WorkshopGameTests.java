package com.example.chemistry;

import com.example.chemistry.block.WaterTroughBlock;
import com.example.chemistry.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class WorkshopGameTests {
    private static void check(GameTestHelper h,boolean ok,String msg){h.assertTrue(ok,Component.literal(msg));}
    public static void troughCollision(GameTestHelper h){h.runAtTickTime(1,()->{
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(1,1,1));
        for(var fill:WaterTroughBlock.Fill.values()) {
            level.setBlock(pos,ModBlocks.WATER_TROUGH.get().defaultBlockState().setValue(WaterTroughBlock.FILLED,fill),3);
            ArmorStand entity=new ArmorStand(level,pos.getX()+.53,pos.getY()+1.5,pos.getZ()+.53);
            level.addFreshEntity(entity);entity.move(MoverType.SELF,new Vec3(0,-2,0));
            check(h,Math.abs(entity.getY()-(pos.getY()+.25))<1e-6,"entity fell through trough: "+fill);
            check(h,!level.getBlockState(pos).getBlockSupportShape(level,pos).isEmpty(),"missing support shape");
            entity.discard();
        }
        h.succeed();
    });}
    public static void scraping(GameTestHelper h){h.runAtTickTime(1,()->{
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(1,1,1));var area=new AABB(pos).inflate(3);
        var player=h.makeMockPlayer(GameType.CREATIVE);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_AXE));
        var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        var tick=new ServerTickEvent.Post(()->true,level.getServer());
        int successes=0,misses=0;
        for(int seed=0;seed<40;seed++) {
            level.getEntitiesOfClass(ItemEntity.class,area).forEach(e->e.discard());
            level.setBlock(pos,Blocks.OXIDIZED_COPPER.defaultBlockState(),3);
            player.getMainHandItem().useOn(context);
            check(h,level.getBlockState(pos).is(Blocks.WEATHERED_COPPER),"axe failed to scrape");
            boolean expected=net.minecraft.util.RandomSource.create(seed).nextInt(5)==0;
            level.random.setSeed(seed);CopperScrapingDrops.afterTick(tick);
            int amount=level.getEntitiesOfClass(ItemEntity.class,area).stream().filter(e->e.getItem().is(ModItems.looseSolid("basic_copper_carbonate"))).mapToInt(e->e.getItem().getCount()).sum();
            check(h,amount==(expected?1:0),"incorrect 1-in-5 roll or duplicate drop");
            if(expected)successes++;else misses++;
        }
        check(h,successes>0 && misses>0,"both random outcomes must be covered");
        for(var block:new Block[]{Blocks.COPPER_BLOCK,Blocks.WAXED_OXIDIZED_COPPER,Blocks.OAK_LOG}) {
            level.getEntitiesOfClass(ItemEntity.class,area).forEach(e->e.discard());level.setBlock(pos,block.defaultBlockState(),3);
            player.getMainHandItem().useOn(context);level.random.setSeed(0);CopperScrapingDrops.afterTick(tick);
            check(h,level.getEntitiesOfClass(ItemEntity.class,area).isEmpty(),"wax/clean copper/wood produced copper powder");
        }
        level.setBlock(pos,Blocks.OXIDIZED_COPPER.defaultBlockState(),3);
        var simulated=new BlockEvent.BlockToolModificationEvent(level.getBlockState(pos),context,ItemAbilities.AXE_SCRAPE,true);
        CopperScrapingDrops.onTool(simulated);
        var canceled=new BlockEvent.BlockToolModificationEvent(level.getBlockState(pos),context,ItemAbilities.AXE_SCRAPE,false);
        canceled.setCanceled(true);CopperScrapingDrops.onTool(canceled);
        level.setBlock(pos,Blocks.WEATHERED_COPPER.defaultBlockState(),3);CopperScrapingDrops.afterTick(tick);
        check(h,level.getEntitiesOfClass(ItemEntity.class,area).isEmpty(),"simulated or canceled scrape produced drops");
        check(h,CopperScrapingDrops.isScrape(Blocks.OXIDIZED_CUT_COPPER_STAIRS.defaultBlockState(),Blocks.WEATHERED_CUT_COPPER_STAIRS.defaultBlockState()),"vanilla copper shapes unsupported");
        h.succeed();
    });}
}
