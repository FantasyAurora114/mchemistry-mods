package com.example.chemistry.item;

import java.util.function.Supplier;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A sealed gas collecting bottle. Right-clicking removes the glass cover:
 * the bottle becomes its open variant and the player receives a glass sheet.
 */
public class GasCollectingBottleItem extends Item {

    private final Supplier<Item> openVariant;
    private final Supplier<Item> glassSheet;
    private final Supplier<Item> immediateOpenProduct;

    public GasCollectingBottleItem(Properties properties, Supplier<Item> openVariant, Supplier<Item> glassSheet) {
        this(properties, openVariant, glassSheet, null);
    }

    public GasCollectingBottleItem(Properties properties, Supplier<Item> openVariant, Supplier<Item> glassSheet,
            Supplier<Item> immediateOpenProduct) {
        super(properties);
        this.openVariant = openVariant;
        this.glassSheet = glassSheet;
        this.immediateOpenProduct = immediateOpenProduct;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ItemStack held = player.getItemInHand(hand);
            // Some gases (e.g. NO) oxidise the moment the bottle is opened.
            ItemStack open = new ItemStack(
                    immediateOpenProduct != null ? immediateOpenProduct.get() : openVariant.get());
            if (held.getCount() > 1) {
                held.shrink(1);
                if (!player.getInventory().add(open)) {
                    player.drop(open, false);
                }
            } else {
                player.setItemInHand(hand, open);
            }
            ItemStack sheet = new ItemStack(glassSheet.get());
            if (!player.getInventory().add(sheet)) {
                player.drop(sheet, false);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
