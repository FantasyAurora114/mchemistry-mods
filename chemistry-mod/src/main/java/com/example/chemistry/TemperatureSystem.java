package com.example.chemistry;

import com.example.chemistry.item.TestTubeItem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Test tube temperature: heats up near fire/lava, cools at room temperature.
 * Above 80 C the tube burns the holder and is dropped (clamped tubes are
 * insulated); above the melting point it disappears; a >150 C jump in one tick
 * shatters normal glass but not borosilicate.
 */
public final class TemperatureSystem {

    public static final double ROOM_TEMP = 20.0;
    public static final double HOT_THRESHOLD = 80.0;
    public static final double SHOCK_DELTA = 150.0;

    private static final String KEY_TEMP = "chem_temp";
    private static final String KEY_HEATMODE = "chem_heatmode";

    public static double getTemp(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getDoubleOr(KEY_TEMP, ROOM_TEMP);
    }

    public static void tick(ItemStack stack, Player player, int slot) {
        if (!(stack.getItem() instanceof TestTubeItem tube)) {
            return;
        }
        double current = getTemp(stack);
        int heatmode = getHeatmode(stack);
        // No environmental heating: temperature only changes via the sustained
        // heating mode (sneak + fire) or explicit right-click interactions.
        double next = current;
        if (heatmode > 0) {
            next = current + (heatmode - current) * 0.1;
            if (Math.abs(next - current) < 0.5) {
                next = heatmode;
            }
        }
        if (Math.abs(next - current) > SHOCK_DELTA && !tube.isBorosilicate()) {
            player.getInventory().setItem(slot, ItemStack.EMPTY);
            player.displayClientMessage(Component.translatable("mchemistry.tube.shatter"), true);
            return;
        }
        if (next > tube.meltingPoint()) {
            player.getInventory().setItem(slot, ItemStack.EMPTY);
            player.displayClientMessage(Component.translatable("mchemistry.tube.melt"), true);
            return;
        }
        if (next != current) {
            setTemp(stack, next);
        }
        if (next > HOT_THRESHOLD && !tube.isClamped()) {
            player.hurt(player.damageSources().onFire(), 4.0F);
            player.displayClientMessage(Component.translatable("mchemistry.tube.hot"), true);
            ItemStack dropped = stack.copy();
            player.getInventory().setItem(slot, ItemStack.EMPTY);
            player.drop(dropped, false);
        }
    }

    public static void addTemp(ItemStack stack, double delta) {
        if (stack.getItem() instanceof TestTubeItem) {
            setTemp(stack, Math.max(0, getTemp(stack) + delta));
        }
    }

    public static void addTempCapped(ItemStack stack, double delta, double max) {
        if (stack.getItem() instanceof TestTubeItem) {
            setTemp(stack, Math.min(max, getTemp(stack) + delta));
        }
    }

    public static void addTempFloored(ItemStack stack, double delta, double min) {
        if (stack.getItem() instanceof TestTubeItem) {
            setTemp(stack, Math.max(min, getTemp(stack) + delta));
        }
    }

    /** Sneak + fire toggles a sustained heating mode at the given target. */
    public static void toggleHeatmode(ItemStack stack, int target) {
        if (!(stack.getItem() instanceof TestTubeItem)) {
            return;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.getIntOr(KEY_HEATMODE, 0) == target) {
            tag.remove(KEY_HEATMODE);
        } else {
            tag.putInt(KEY_HEATMODE, target);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int getHeatmode(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getIntOr(KEY_HEATMODE, 0);
    }

    public static void setTemp(ItemStack stack, double temp) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putDouble(KEY_TEMP, temp);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private TemperatureSystem() {
    }
}
