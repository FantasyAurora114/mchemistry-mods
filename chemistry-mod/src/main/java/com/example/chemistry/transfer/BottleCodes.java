package com.example.chemistry.transfer;

import com.example.chemistry.storage.ChemUnits;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Maps between container items (gas jars, liquid bottles, dropper bottles)
 * and their tank units (gas id / liquid id + mB volume).
 */
public final class BottleCodes {

    private static final String KEY_AMOUNT = "chem_amount";

    private BottleCodes() {
    }

    public static String pathOf(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    /** Gas id held by a sealed gas collecting bottle, or null. */
    public static String gasIdOf(ItemStack stack) {
        String path = pathOf(stack);
        if (path.startsWith("gas_collecting_bottle_") && !path.startsWith("open_")) {
            return path.substring("gas_collecting_bottle_".length());
        }
        return null;
    }

    /** Gas id held by a sealed or open gas collecting bottle, or null. */
    public static String gasIdOfAny(ItemStack stack) {
        String path = pathOf(stack);
        if (path.startsWith("gas_collecting_bottle_")) {
            return path.startsWith("open_")
                    ? path.substring("open_gas_collecting_bottle_".length())
                    : path.substring("gas_collecting_bottle_".length());
        }
        return null;
    }

    public static int gasVolumeOf(ItemStack stack) {
        return gasIdOf(stack) == null ? 0 : ChemUnits.GAS_JAR_VOLUME;
    }

    public static boolean isEmptyGasJar(ItemStack stack) {
        return pathOf(stack).equals("empty_gas_collecting_bottle");
    }

    /** Liquid id held by a narrow bottle / dropper bottle, or null. */
    public static String liquidIdOf(ItemStack stack) {
        String path = pathOf(stack);
        if (path.startsWith("liquid_") && path.endsWith("_bucket")) {
            return path.substring("liquid_".length(), path.length() - "_bucket".length());
        }
        if (path.startsWith("liquid_")) {
            return path.substring("liquid_".length());
        }
        if (path.startsWith("open_liquid_")) {
            return path.substring("open_liquid_".length());
        }
        if (path.startsWith("dropper_bottle_")) {
            return path.substring("dropper_bottle_".length());
        }
        return null;
    }

    /** Liquid id held by a filled liquid bucket, or null. */
    public static String bucketIdOf(ItemStack stack) {
        String path = pathOf(stack);
        if (path.startsWith("liquid_") && path.endsWith("_bucket")) {
            return path.substring("liquid_".length(), path.length() - "_bucket".length());
        }
        if (path.equals("water_bucket")) {
            return "water";
        }
        return null;
    }

    public static int liquidVolumeOf(ItemStack stack) {
        String path = pathOf(stack);
        int defaultVolume;
        if (path.startsWith("liquid_") && path.endsWith("_bucket")) {
            defaultVolume = ChemUnits.BUCKET_VOLUME;
        } else if (path.startsWith("liquid_") || path.startsWith("open_liquid_")) {
            defaultVolume = ChemUnits.LIQUID_BOTTLE_VOLUME;
        } else if (path.startsWith("dropper_bottle_")) {
            defaultVolume = ChemUnits.DROPPER_BOTTLE_VOLUME;
        } else {
            return 0;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return (int) tag.getLongOr(KEY_AMOUNT, defaultVolume);
    }

    /** Fixed capacity of a bottle item, in mB (0 for non-bottles). */
    public static int bottleCapacityOf(ItemStack stack) {
        String path = pathOf(stack);
        if (path.equals("empty_narrow_bottle") || path.startsWith("liquid_") || path.startsWith("open_liquid_")) {
            return ChemUnits.LIQUID_BOTTLE_VOLUME;
        }
        if (path.equals("empty_dropper_bottle") || path.startsWith("dropper_bottle_")) {
            return ChemUnits.DROPPER_BOTTLE_VOLUME;
        }
        if (path.equals("bucket") || (path.startsWith("liquid_") && path.endsWith("_bucket"))
                || path.equals("water_bucket")) {
            return ChemUnits.BUCKET_VOLUME;
        }
        return 0;
    }

    public static boolean isEmptyBucket(ItemStack stack) {
        return pathOf(stack).equals("bucket");
    }

    /** Stores a partial volume on a filled bottle item. */
    public static void setVolume(ItemStack stack, int volume) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putLong(KEY_AMOUNT, volume);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean isEmptyLiquidBottle(ItemStack stack) {
        String path = pathOf(stack);
        return path.equals("empty_narrow_bottle") || path.equals("empty_dropper_bottle");
    }
}
