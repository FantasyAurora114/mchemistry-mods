package com.example.chemistry.item;

import com.example.chemistry.block.LaboratoryBenchBlock;
import net.minecraft.core.Direction;
import com.example.chemistry.entity.PlacedVesselEntity;
import com.example.chemistry.registry.ModEntities;
import com.example.chemistry.registry.ModBlocks;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** A vessel item that can be placed directly on the ground (in addition to
 *  being mounted on the iron stand / tripod). Contents are carried in the
 *  stack and copied into the placed block entity. */
public class PlacedVesselItem extends LabVesselItem {

    public PlacedVesselItem(Properties properties, int capacity) {
        super(properties, capacity);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        // Bench sides belong to drawers/doors; never place a vessel beside the bench at floor level.
        if (level.getBlockState(context.getClickedPos()).getBlock() instanceof LaboratoryBenchBlock
                && context.getClickedFace() != Direction.UP) {
            return InteractionResult.FAIL;
        }
        var pos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(pos).canBeReplaced()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        PlacedVesselEntity entity =
                new PlacedVesselEntity(ModEntities.PLACED_VESSEL.get(), level);
        var hit = context.getClickLocation();
        if (context.getClickedFace() == Direction.UP) {
            entity.setPos(hit.x, hit.y + 0.002, hit.z);
        } else {
            entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        }
        entity.setVessel(context.getItemInHand().copyWithCount(1));
        level.addFreshEntity(entity);
        if (context.getPlayer() == null || !context.getPlayer().isCreative()) context.getItemInHand().shrink(1);
        level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }
}
