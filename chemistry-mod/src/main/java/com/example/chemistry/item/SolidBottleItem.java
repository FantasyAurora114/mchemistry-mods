package com.example.chemistry.item;

import java.util.function.Supplier;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * A sealed wide-mouth bottle of a solid. Right-clicking removes the cover:
 * the bottle becomes its open variant and the player receives a glass sheet.
 */
public class SolidBottleItem extends Item {

    private final Supplier<Item> openVariant;
    private final Supplier<Item> cover;

    public SolidBottleItem(Properties properties, Supplier<Item> openVariant, Supplier<Item> cover) {
        super(properties);
        this.openVariant = openVariant;
        this.cover = cover;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ItemStack held = player.getItemInHand(hand);
            ItemStack open = new ItemStack(openVariant.get());
            CustomData data = open.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = data.copyTag();
            tag.putLong("chem_opened", level.getGameTime());
            open.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            if (held.getCount() > 1) {
                held.shrink(1);
                if (!player.getInventory().add(open)) {
                    player.drop(open, false);
                }
            } else {
                player.setItemInHand(hand, open);
            }
            ItemStack coverStack = new ItemStack(cover.get());
            if (!player.getInventory().add(coverStack)) {
                player.drop(coverStack, false);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
