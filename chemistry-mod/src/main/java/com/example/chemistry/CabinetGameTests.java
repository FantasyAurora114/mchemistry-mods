package com.example.chemistry;
import com.example.chemistry.block.ReagentCabinetBlock;
import com.example.chemistry.blockentity.ReagentCabinetBlockEntity;
import com.example.chemistry.menu.ReagentCabinetMenu;
import com.example.chemistry.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
public final class CabinetGameTests {
    private static void check(GameTestHelper h,boolean b,String s){h.assertTrue(b,Component.literal(s));}
    public static void inventory(GameTestHelper h){h.runAtTickTime(1,()->{
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        for(int rows:new int[]{3,5}){
            var menu=new ReagentCabinetMenu(1,player.getInventory(),rows);
            check(h,menu.slots.size()==rows*3+36,"slot count");
            for(var item:new Item[]{ModItems.LIQUID_BOTTLE.get(),ModItems.SOLID_JAR.get(),ModItems.DROPPER_BOTTLE.get(),ModItems.GRADUATED_CYLINDER.get(),ModItems.ERLENMEYER_FLASK.get(),ModItems.GROUND_GLASS_ERLENMEYER.get()})
                check(h,menu.getSlot(0).mayPlace(new ItemStack(item)),"allowed item rejected");
            check(h,!menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)),"stone accepted");
            check(h,!menu.getSlot(0).mayPlace(new ItemStack(ModItems.DROPPER.get())),"loose dropper accepted");
            check(h,menu.getSlot(0).getMaxStackSize()==1,"stack limit");
            ItemStack bottle=new ItemStack(ModItems.LIQUID_BOTTLE.get());bottle.set(DataComponents.CUSTOM_NAME,Component.literal("cabinet sample"));
            player.getInventory().setItem(9,bottle);
            check(h,!menu.quickMoveStack(player,rows*3).isEmpty(),"shift insert failed");
            check(h,menu.getSlot(0).getItem().getHoverName().getString().equals("cabinet sample"),"name lost");
            check(h,!menu.quickMoveStack(player,0).isEmpty() && !menu.getSlot(0).hasItem(),"shift remove failed");
            player.getInventory().clearContent();
        }
        h.succeed();
    });}
    public static void persistence(GameTestHelper h){h.runAtTickTime(1,()->{
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(1,1,1));
        var state=ModBlocks.BASE_REAGENT_CABINET.get().defaultBlockState().setValue(ReagentCabinetBlock.OPEN,true).setValue(ReagentCabinetBlock.FACING,Direction.WEST);
        level.setBlock(pos,state,3);var be=(ReagentCabinetBlockEntity)level.getBlockEntity(pos);
        ItemStack flask=new ItemStack(ModItems.ERLENMEYER_FLASK.get());
        com.example.chemistry.item.LabVesselItem.addLiquid(flask,"water",25);TemperatureSystem.setTemp(flask,41);
        flask.set(DataComponents.CUSTOM_NAME,Component.literal("saved flask"));be.setItem(8,flask.copy());
        var loaded=(ReagentCabinetBlockEntity)BlockEntity.loadStatic(pos,state,be.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
        check(h,loaded!=null && ItemStack.matches(flask,loaded.getItem(8)),"inventory metadata did not round trip");
        check(h,loaded.getBlockState().getValue(ReagentCabinetBlock.OPEN),"open state lost");
        check(h,loaded.getBlockState().getValue(ReagentCabinetBlock.FACING)==Direction.WEST,"facing lost");
        // Exercise the actual chunk/update packet path, not only disk serialization.
        var clientCopy=new ReagentCabinetBlockEntity(pos,state);
        clientCopy.handleUpdateTag(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),be.getUpdateTag(level.registryAccess())));
        check(h,ItemStack.matches(flask,clientCopy.getItem(8)),"client update lost shelf item");
        be.removeItem(8,1);
        clientCopy.handleUpdateTag(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),be.getUpdatePacket().getTag()));
        check(h,clientCopy.getItem(8).isEmpty(),"client update left a ghost shelf item");
        ReagentCabinetBlock.setOpen(level,pos,false);
        check(h,!be.getBlockState().getValue(ReagentCabinetBlock.OPEN),"close failed");
        h.succeed();
    });}
    public static void placement(GameTestHelper h){h.runAtTickTime(1,()->{
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(1,1,1));
        var player=h.makeMockPlayer(GameType.CREATIVE);
        var block=ModBlocks.TALL_REAGENT_CABINET.get();
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        level.setBlock(pos.above(),Blocks.STONE.defaultBlockState(),3);
        var context=new net.minecraft.world.item.context.BlockPlaceContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(block),new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.UP,pos,false));
        check(h,block.getStateForPlacement(context)==null,"tall cabinet overwrites ceiling");
        level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),3);
        for(float yaw:new float[]{0,90,180,270}) {
            player.setYRot(yaw);
            var state=block.getStateForPlacement(context);
            check(h,state!=null && state.getValue(ReagentCabinetBlock.FACING)==player.getDirection().getOpposite(),"placement facing");
        }
        var state=block.defaultBlockState();
        level.setBlock(pos,state,3);block.setPlacedBy(level,pos,state,player,new ItemStack(block));
        ReagentCabinetBlock.inventory(level,pos).setItem(0,new ItemStack(ModItems.LIQUID_BOTTLE.get()));
        var area=new AABB(pos).inflate(4);level.getEntitiesOfClass(ItemEntity.class,area).forEach(e->e.discard());
        block.playerWillDestroy(level,pos.above(),level.getBlockState(pos.above()),player);
        level.destroyBlock(pos.above(),false);
        int cabinet=0,bottles=0;
        for(var e:level.getEntitiesOfClass(ItemEntity.class,area)) {if(e.getItem().is(ModItems.TALL_REAGENT_CABINET.get()))cabinet+=e.getItem().getCount();if(e.getItem().is(ModItems.LIQUID_BOTTLE.get()))bottles+=e.getItem().getCount();}
        check(h,cabinet==0 && bottles==1,"creative removal duplicates cabinet or loses contents");
        check(h,level.getBlockState(pos).isAir(),"creative removal leaves lower half");
        // Replacing either half (also the removal path used by explosions) must release inventory once.
        level.getEntitiesOfClass(ItemEntity.class,area).forEach(e->e.discard());
        level.setBlock(pos,state,3);block.setPlacedBy(level,pos,state,player,new ItemStack(block));
        ReagentCabinetBlock.inventory(level,pos).setItem(0,new ItemStack(ModItems.LIQUID_BOTTLE.get()));
        level.setBlock(pos.above(),Blocks.STONE.defaultBlockState(),3);
        check(h,level.getBlockState(pos).isAir(),"replacement leaves lower half");
        check(h,level.getEntitiesOfClass(ItemEntity.class,area).stream().mapToInt(e->e.getItem().getCount()).sum()==2,"replacement drop count");
        h.succeed();
    });}
    public static void halves(GameTestHelper h){h.runAtTickTime(1,()->{
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(1,1,1));
        for(boolean upper:new boolean[]{false,true}){
            var area=new AABB(pos).inflate(4);level.getEntitiesOfClass(ItemEntity.class,area).forEach(e->e.discard());
            var block=ModBlocks.TALL_REAGENT_CABINET.get();var state=block.defaultBlockState();
            level.setBlock(pos,state,3);block.setPlacedBy(level,pos,state,null,new ItemStack(block));
            var be=ReagentCabinetBlock.inventory(level,pos.above());check(h,be!=null && be.getContainerSize()==15,"upper inventory owner");
            be.setItem(0,new ItemStack(ModItems.SOLID_JAR.get()));
            ReagentCabinetBlock.setOpen(level,pos.above(),true);
            check(h,level.getBlockState(pos).getValue(ReagentCabinetBlock.OPEN) && level.getBlockState(pos.above()).getValue(ReagentCabinetBlock.OPEN),"halves open mismatch");
            level.destroyBlock(upper?pos.above():pos,true);
            check(h,level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir(),"orphan half");
            int cabinet=0,jars=0;
            for(var e:level.getEntitiesOfClass(ItemEntity.class,area)) {if(e.getItem().is(ModItems.TALL_REAGENT_CABINET.get()))cabinet+=e.getItem().getCount();if(e.getItem().is(ModItems.SOLID_JAR.get()))jars+=e.getItem().getCount();}
            check(h,cabinet==1 && jars==1,"missing or duplicate drops: cabinet="+cabinet+", jars="+jars);
        }
        h.succeed();
    });}
}
