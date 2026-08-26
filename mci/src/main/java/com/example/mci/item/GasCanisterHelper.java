/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.example.chemistry.data.GasJars
 *  com.example.chemistry.data.GasJars$GasJar
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.component.CustomData
 *  net.minecraft.world.item.component.CustomModelData
 */
package com.example.mci.item;

import com.example.chemistry.data.GasJars;
import com.example.mci.item.GasCanisterItem;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

public final class GasCanisterHelper {
    public static final String KEY_GAS = "chem_gas";
    public static final String KEY_AMOUNT = "chem_amount";
    public static final String KEY_PURITY = "chem_purity";

    private GasCanisterHelper() {
    }

    public static boolean isCanister(ItemStack stack) {
        return stack.getItem() instanceof GasCanisterItem;
    }

    public static String getGasId(ItemStack stack) {
        CompoundTag tag = GasCanisterHelper.tag(stack);
        String gas = tag.getStringOr(KEY_GAS, "");
        return gas.isEmpty() ? null : gas;
    }

    public static long getAmount(ItemStack stack) {
        return GasCanisterHelper.tag(stack).getLongOr(KEY_AMOUNT, 0L);
    }

    public static double getPurity(ItemStack stack) {
        return GasCanisterHelper.tag(stack).getDoubleOr(KEY_PURITY, 1.0);
    }

    public static boolean isEmpty(ItemStack stack) {
        return GasCanisterHelper.getGasId(stack) == null || GasCanisterHelper.getAmount(stack) <= 0L;
    }

    public static long getSpace(ItemStack stack) {
        return Math.max(0L, 16000L - GasCanisterHelper.getAmount(stack));
    }

    public static long fill(ItemStack stack, String gasId, long amount, double purity) {
        if (gasId == null || gasId.isEmpty() || amount <= 0L) {
            return 0L;
        }
        String current = GasCanisterHelper.getGasId(stack);
        if (current != null && !current.equals(gasId)) {
            return 0L;
        }
        long accepted = Math.min(amount, GasCanisterHelper.getSpace(stack));
        if (accepted <= 0L) {
            return 0L;
        }
        long oldAmount = GasCanisterHelper.getAmount(stack);
        double oldPurity = GasCanisterHelper.getPurity(stack);
        double newPurity = oldAmount == 0L ? purity : (oldPurity * (double)oldAmount + purity * (double)accepted) / (double)(oldAmount + accepted);
        CompoundTag tag = GasCanisterHelper.tag(stack);
        tag.putString(KEY_GAS, gasId);
        tag.putLong(KEY_AMOUNT, oldAmount + accepted);
        tag.putDouble(KEY_PURITY, newPurity);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of((CompoundTag)tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of("filled"), List.of(Integer.valueOf(GasCanisterHelper.gasColor(gasId)))));
        return accepted;
    }

    public static long drain(ItemStack stack, long amount) {
        if (GasCanisterHelper.isEmpty(stack) || amount <= 0L) {
            return 0L;
        }
        long drained = Math.min(amount, GasCanisterHelper.getAmount(stack));
        CompoundTag tag = GasCanisterHelper.tag(stack);
        long remaining = GasCanisterHelper.getAmount(stack) - drained;
        if (remaining <= 0L) {
            tag.remove(KEY_GAS);
            tag.remove(KEY_AMOUNT);
            tag.remove(KEY_PURITY);
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        } else {
            tag.putLong(KEY_AMOUNT, remaining);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of((CompoundTag)tag));
        return drained;
    }

    public static void clear(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_DATA);
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);
    }

    private static CompoundTag tag(ItemStack stack) {
        return ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
    }

    private static int gasColor(String gasId) {
        for (GasJars.GasJar gas : GasJars.ALL) {
            if (!gas.id().equals(gasId)) continue;
            return gas.color();
        }
        return 0xFFFFFF;
    }
}
