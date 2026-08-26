package com.example.chemistry;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Purity stored on chemical items. Creative-mode items are 99.99999% pure;
 * tower products inherit the average purity of their inputs.
 */
public final class PurityHelper {

    public static final double CREATIVE_PURITY = 0.9999999;
    private static final String KEY = "chem_purity";

    public static double getPurity(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getDoubleOr(KEY, CREATIVE_PURITY);
    }

    public static void setPurity(ItemStack stack, double purity) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putDouble(KEY, Math.min(1.0, Math.max(0.0, purity)));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static ItemStack pure(ItemStack stack) {
        ItemStack copy = stack.copy();
        setPurity(copy, CREATIVE_PURITY);
        return copy;
    }

    private PurityHelper() {
    }
}
