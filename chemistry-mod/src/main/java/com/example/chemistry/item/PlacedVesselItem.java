package com.example.chemistry.item;

import com.example.chemistry.blockentity.PlacedVesselBlockEntity;
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
        var pos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(pos).canBeReplaced()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, ModBlocks.PLACED_VESSEL.get().defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof PlacedVesselBlockEntity be) {
                be.setVessel(context.getItemInHand().copy());
            }
            context.getItemInHand().shrink(1);
            level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }
}
