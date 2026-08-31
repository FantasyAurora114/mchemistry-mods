package com.example.chemistry.block;

import com.example.chemistry.blockentity.AssemblyFrameBlockEntity;
import com.example.chemistry.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 机架块：2×3 通用框架，槽位 Map 存子件，BER 画多个子件。 */
public class AssemblyFrameBlock extends Block implements EntityBlock {

    public static final MapCodec<AssemblyFrameBlock> CODEC = simpleCodec(AssemblyFrameBlock::new);
    private static final VoxelShape SHAPE = box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    public AssemblyFrameBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<AssemblyFrameBlock> codec() {
        return CODEC;
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AssemblyFrameBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, net.minecraft.world.InteractionHand hand,
            BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof AssemblyFrameBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (!stack.isEmpty()) {
            // 命中子件端口：交给 IAssemblyPart 的端口行为。
            int portSlot = be.pickPartPort(level, pos, player);
            if (portSlot >= 0
                    && be.getPart(portSlot).getItem() instanceof com.example.chemistry.api.IAssemblyPart part) {
                InteractionResult r = part.interactPort(level, player, pos, portSlot, stack, hand);
                if (r != InteractionResult.PASS) {
                    return r;
                }
            }
            // 命中已占用的槽位：交给子件的 IAssemblyPart 行为。
            int hit = be.pickSlot(player, pos, true);
            if (hit >= 0 && be.getPart(hit).getItem() instanceof com.example.chemistry.api.IAssemblyPart part) {
                InteractionResult r = part.interactPart(level, player, pos, hit, stack, hand);
                if (r != InteractionResult.PASS) {
                    return r;
                }
            }
            // 放进命中的空槽（没命中任何槽时退回第一个空槽）。
            int slot = be.firstEmpty();
            int hitEmpty = be.pickSlot(player, pos, false);
            if (hitEmpty >= 0 && be.getPart(hitEmpty).isEmpty()) {
                slot = hitEmpty;
            }
            if (slot >= 0) {
                if (!level.isClientSide()) {
                    be.setPart(slot, stack.copy());
                    stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof AssemblyFrameBlockEntity be) {
            // 射线精确拾取：对准哪个子件取哪个。
            int take = be.pickSlot(player, pos, true);
            if (take >= 0) {
                ItemStack part = be.getPart(take);
                if (part.getItem() instanceof com.example.chemistry.api.IAssemblyPart p) {
                    part = p.takeOff(level, player, pos, take, part);
                }
                if (!level.isClientSide()) {
                    be.setPart(take, ItemStack.EMPTY);
                    if (!player.getInventory().add(part)) {
                        player.drop(part, false);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
