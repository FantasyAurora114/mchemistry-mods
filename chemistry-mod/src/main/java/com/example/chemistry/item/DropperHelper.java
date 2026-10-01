package com.example.chemistry.item;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.chemistry.storage.ChemUnits;
import com.example.chemistry.data.Liquids;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

/**
 * Shared logic for the functional dropper tools (胶头滴管 / 滴瓶瓶塞).
 * Contents are stored as custom data (liquid id + mL); the item model switches
 * to a liquid-tinted layer via CUSTOM_MODEL_DATA when filled.
 */
public final class DropperHelper {

    public static final int CAPACITY = ChemUnits.DROPPER_VOLUME;

    private static final String KEY_LIQUID = "chem_liquid";
    private static final String KEY_ML = "chem_ml";

    private static final Map<String, Integer> LIQUID_COLORS = Liquids.ALL.stream()
            .collect(Collectors.toMap(Liquids.Liquid::id, Liquids.Liquid::color));

    public static boolean isDropper(ItemStack stack) {
        return stack.getItem() instanceof DropperItem;
    }

    public static boolean isStopper(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.equals("dropper_bottle_stopper") || path.equals("brown_dropper_bottle_stopper");
    }

    public static boolean isEmpty(ItemStack stack) {
        return getLiquid(stack) == null;
    }

    /** 液体显示颜色。 */
    public static int liquidColor(String liquidId) {
        return LIQUID_COLORS.getOrDefault(liquidId, 0xFFFFFF);
    }

    public static String getLiquid(ItemStack stack) {
        CompoundTag tag = tag(stack);
        String liquid = tag.getStringOr(KEY_LIQUID, "");
        return liquid.isEmpty() ? null : liquid;
    }

    public static int getMl(ItemStack stack) {
        return (int) tag(stack).getLongOr(KEY_ML, 0L);
    }

    public static void fill(ItemStack stack, String liquidId, int ml) {
        CompoundTag tag = tag(stack);
        tag.putString(KEY_LIQUID, liquidId);
        tag.putLong(KEY_ML, ml);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        int color = LIQUID_COLORS.getOrDefault(liquidId, 0xFFFFFF);
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(), List.of("filled"), List.of(color)));
    }

    public static void setMl(ItemStack stack, int ml) {
        CompoundTag tag = tag(stack);
        tag.putLong(KEY_ML, ml);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (ml <= 0) {
            clear(stack);
        }
    }

    /** Fine addition is 1 mL; normal addition is 5 mL, bounded by actual remainder. */
    public static boolean pour(ItemStack dropper,ItemStack vessel,boolean fine) {
        if(com.example.chemistry.organic.OrganicApparatus.covered(vessel))return false;
        int amount=Math.min(fine?1:5,getMl(dropper));
        String liquid=getLiquid(dropper);
        if (liquid==null || amount<=0) return false;
        var before=vessel.copy();
        if(!LabVesselItem.addLiquid(vessel,liquid,amount))return false;
        com.example.chemistry.radiation.RadioLedger.inherit(dropper,before,vessel);
        setMl(dropper,getMl(dropper)-amount);
        return true;
    }

    public static void clear(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_DATA);
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private DropperHelper() {
    }
}
