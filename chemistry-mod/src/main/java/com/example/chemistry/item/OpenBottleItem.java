package com.example.chemistry.item;

import java.util.function.Supplier;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * An open reagent bottle. Reactive contents (NaOH/KOH/Ca(OH)2 solutions or
 * powders, clear limewater) convert to their carbonate / cloudy forms after
 * two in-game days.
 */
public class OpenBottleItem extends Item {

    public static final long TWO_DAYS_TICKS = 48000L;

    private final Supplier<Item> product;

    public OpenBottleItem(Properties properties, Supplier<Item> product) {
        super(properties);
        this.product = product;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof Player player)) {
            return;
        }
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (!tag.contains("chem_opened")) {
            return;
        }
        long opened = tag.getLongOr("chem_opened", 0L);
        if (level.getGameTime() - opened < TWO_DAYS_TICKS) {
            return;
        }
        // These bottles are non-stackable, so reference equality finds the exact slot.
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i) == stack) {
                player.getInventory().setItem(i, new ItemStack(product.get()));
                break;
            }
        }
    }
}
