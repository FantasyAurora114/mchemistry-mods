package com.example.chemistry.block;

import com.example.chemistry.blockentity.TestTubeRackBlockEntity;
import com.example.chemistry.item.TestTubeItem;
import com.example.chemistry.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 试管架：右键放正置试管，潜行+右键放倒置试管，空手取出。 */
public class TestTubeRackBlock extends Block implements EntityBlock {

    public static final MapCodec<TestTubeRackBlock> CODEC = simpleCodec(TestTubeRackBlock::new);
    private static final VoxelShape SHAPE = box(0.0, 0.0, 4.0, 16.0, 5.0, 12.0);

    public TestTubeRackBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<TestTubeRackBlock> codec() {
        return CODEC;
    }

    /** 由方块实体渲染器绘制。 */
    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    /** 插在架孔里的试管上半截悬空在碰撞体(至 y=5)之上；交互命中框覆盖到试管口高度。 */
    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return box(0.0, 0.0, 4.0, 16.0, 16.0, 12.0);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TestTubeRackBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof TestTubeRackBlockEntity be)) {
            return InteractionResult.PASS;
        }
        // 放试管：潜行=倒置，普通=正置；带试管夹的试管不能放（夹子碍事）。
        if (stack.getItem() instanceof TestTubeItem tt && !tt.isClamped()) {
            // 精确拾取：先看视线对准哪个孔，对准的孔已占则提示。
            int slot = be.pickSlot(player, pos, false);
            if (slot >= 0 && !be.getTube(slot).isEmpty()) {
                if (!level.isClientSide()) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.rack.slot_occupied"), true);
                }
                return InteractionResult.FAIL;
            }
            if (slot < 0) {
                slot = be.firstEmpty();
            }
            if (slot >= 0) {
                if (!level.isClientSide()) {
                    be.setTube(slot, stack.copy(), player.isShiftKeyDown());
                    stack.shrink(1);
                    level.playSound(null, pos, SoundEvents.GLASS_PLACE,
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // 空手右键：交给 useWithoutItem 取试管。
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof TestTubeRackBlockEntity be) {
            // 精确取出：视线对准哪支试管就取哪支。
            int slot = be.pickSlot(player, pos, true);
            if (slot >= 0) {
                if (!level.isClientSide()) {
                    ItemStack tube = be.getTube(slot);
                    be.setTube(slot, ItemStack.EMPTY, false);
                    if (!player.getInventory().add(tube)) {
                        player.drop(tube, false);
                    }
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP,
                            SoundSource.BLOCKS, 0.8F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }
            if (be.firstEmpty() >= 0) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative()
                && level.getBlockEntity(pos) instanceof TestTubeRackBlockEntity be) {
            for (int i = 0; i < TestTubeRackBlockEntity.SLOTS; i++) {
                ItemStack tube = be.getTube(i);
                if (!tube.isEmpty()) {
                    net.minecraft.world.entity.item.ItemEntity item =
                            new net.minecraft.world.entity.item.ItemEntity(
                                    level, pos.getX() + 0.5, pos.getY() + 0.4,
                                    pos.getZ() + 0.5, tube);
                    item.setDefaultPickUpDelay();
                    level.addFreshEntity(item);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
