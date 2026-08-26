package com.example.chemistry.item;

import com.example.chemistry.CombustionEngine;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A combustion spoon: a 50 mL reaction vessel that can be ignited. While lit it
 * burns its contents with oxygen from the air, or inside an oxygen jar.
 */
public class CombustionSpoonItem extends LabVesselItem {

    public static final int CAPACITY = 50;

    public CombustionSpoonItem(Properties properties) {
        super(properties, CAPACITY);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ItemStack stack = player.getItemInHand(hand);
            if (CombustionEngine.isLit(stack)) {
                CombustionEngine.setLit(stack, false);
                player.displayClientMessage(Component.translatable("mchemistry.spoon.extinguish"), true);
            } else if (!LabVesselItem.getContents(stack).isEmpty()) {
                CombustionEngine.setLit(stack, true);
                player.displayClientMessage(Component.translatable("mchemistry.spoon.ignite"), true);
            } else {
                player.displayClientMessage(Component.translatable("mchemistry.spoon.no_contents"), true);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
