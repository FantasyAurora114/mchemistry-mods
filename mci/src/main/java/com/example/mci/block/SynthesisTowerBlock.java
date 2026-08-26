/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.EntityBlock
 *  net.minecraft.world.level.block.HorizontalDirectionalBlock
 *  net.minecraft.world.level.block.Mirror
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityTicker
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.EnumProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.BlockHitResult
 *  org.jetbrains.annotations.Nullable
 */
package com.example.mci.block;

import com.example.mci.blockentity.SynthesisTowerBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class SynthesisTowerBlock
extends HorizontalDirectionalBlock
implements EntityBlock {
    public static final MapCodec<SynthesisTowerBlock> CODEC = SynthesisTowerBlock.simpleCodec(SynthesisTowerBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public SynthesisTowerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public MapCodec<SynthesisTowerBlock> codec() {
        return CODEC;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    protected BlockState rotate(BlockState state, Rotation rotation) {
        return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
    }

    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation((Direction)state.getValue(FACING)));
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SynthesisTowerBlockEntity(pos, state);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : (lvl, pos, st, be) -> {
            if (be instanceof SynthesisTowerBlockEntity) {
                SynthesisTowerBlockEntity tower = (SynthesisTowerBlockEntity)be;
                tower.tickServer();
            }
        };
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity blockEntity;
        if (!level.isClientSide() && (blockEntity = level.getBlockEntity(pos)) instanceof SynthesisTowerBlockEntity) {
            SynthesisTowerBlockEntity tower = (SynthesisTowerBlockEntity)blockEntity;
            player.openMenu((MenuProvider)tower);
        }
        return InteractionResult.SUCCESS;
    }
}
