package com.example.chemistry.block;

import com.example.chemistry.blockentity.RubberTubeLinkBlockEntity;
import com.example.chemistry.client.InvisibleBlockClientExtensions;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Invisible one-block segment of a rubber tube. A block entity stores the two
 * line endpoints; the block-entity renderer draws the part of the tube that
 * passes through this cell, so consecutive cells form a continuous straight
 * rubber tube between the two anchors.
 */
public class RubberTubeLinkBlock extends Block implements EntityBlock {

    public static final MapCodec<RubberTubeLinkBlock> CODEC = simpleCodec(RubberTubeLinkBlock::new);

    public RubberTubeLinkBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<RubberTubeLinkBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RubberTubeLinkBlockEntity(pos, state);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity.tickCount % 3 == 0) {
            InvisibleBlockClientExtensions.spawnStepParticles(level, pos, entity,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                            com.example.chemistry.ChemistryMod.MODID, "block/rubber_tube_side"));
        }
        super.stepOn(level, pos, state, entity);
    }
}
