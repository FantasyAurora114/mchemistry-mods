package com.example.chemistry.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A rubber-bulb dropper with a 25 mL capacity. Right-clicking squeezes out
 * 5 mL at a time; when empty the item returns to its empty texture.
 */
public class DropperItem extends Item {

    public DropperItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!DropperHelper.isEmpty(stack)) {
                DropperHelper.setMl(stack, DropperHelper.getMl(stack) - 5);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
