package com.example.chemistry.transfer;

import com.example.chemistry.storage.ChemUnits;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

/**
 * Unified bottle content model. The four bottle families (细口瓶 / 广口瓶 /
 * 集气瓶 / 滴瓶) are each a single item whose contents live in CUSTOM_DATA
 * instead of one item per reagent.
 *
 * <p>NBT keys:
 * <ul>
 *   <li>{@code chem_liquid} — liquid id (细口瓶 / 滴瓶)</li>
 *   <li>{@code chem_solid} — solid id (广口瓶)</li>
 *   <li>{@code chem_gas} — dominant gas id (集气瓶; mixture lives in
 *       {@code chem_gas_mix})</li>
 *   <li>{@code chem_sealed} — stopper / glass plate present</li>
 *   <li>{@code chem_ml} — amount in mB (partial fill; default = capacity)</li>
 *   <li>{@code chem_brown} — brown glass (light protection)</li>
 *   <li>{@code chem_water} — 集气瓶装满水 (排水法)</li>
 * </ul>
 */
public final class BottleCodes {

    public static final String KEY_LIQUID = "chem_liquid";
    public static final String KEY_SOLID = "chem_solid";
    public static final String KEY_GAS = "chem_gas";
    public static final String KEY_SEALED = "chem_sealed";
    public static final String KEY_ML = "chem_ml";
    public static final String KEY_BROWN = "chem_brown";
    public static final String KEY_WATER = "chem_water";
    public static final String KEY_OPENED = "chem_opened";

    public static final String ID_LIQUID_BOTTLE = "liquid_bottle";
    public static final String ID_SOLID_JAR = "solid_jar";
    public static final String ID_GAS_BOTTLE = "gas_collecting_bottle";
    public static final String ID_DROPPER_BOTTLE = "dropper_bottle";

    /** Two in-game days after which opened reactive bottles degrade. */
    public static final long TWO_DAYS_TICKS = 48000L;

    private BottleCodes() {
    }

    public static String pathOf(ItemStack stack) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(stack.getItem()).getPath();
    }

    // --- Item type predicates (unified items only) ---

    public static boolean isLiquidBottle(ItemStack stack) {
        return pathOf(stack).equals(ID_LIQUID_BOTTLE);
    }

    public static boolean isSolidJar(ItemStack stack) {
        return pathOf(stack).equals(ID_SOLID_JAR);
    }

    public static boolean isGasBottle(ItemStack stack) {
        return pathOf(stack).equals(ID_GAS_BOTTLE);
    }

    public static boolean isDropperBottle(ItemStack stack) {
        return pathOf(stack).equals(ID_DROPPER_BOTTLE);
    }

    // --- Content reads ---

    /** Liquid id held by a narrow bottle or dropper bottle, or null. */
    public static String liquidIdOf(ItemStack stack) {
        if (!isLiquidBottle(stack) && !isDropperBottle(stack)) {
            return null;
        }
        String id = tag(stack).getStringOr(KEY_LIQUID, "");
        return id.isEmpty() ? null : id;
    }

    /** Solid id held by a wide-mouth jar, or null. */
    public static String solidIdOf(ItemStack stack) {
        if (!isSolidJar(stack)) {
            return null;
        }
        String id = tag(stack).getStringOr(KEY_SOLID, "");
        return id.isEmpty() ? null : id;
    }

    /** Dominant gas id held by a gas collecting bottle, or null. */
    public static String gasIdOf(ItemStack stack) {
        if (!isGasBottle(stack)) {
            return null;
        }
        String id = tag(stack).getStringOr(KEY_GAS, "");
        return id.isEmpty() ? null : id;
    }

    public static boolean isSealed(ItemStack stack) {
        return tag(stack).getBooleanOr(KEY_SEALED, true);
    }

    public static boolean isBrown(ItemStack stack) {
        return tag(stack).getBooleanOr(KEY_BROWN, false);
    }

    public static boolean isWater(ItemStack stack) {
        return tag(stack).getBooleanOr(KEY_WATER, false);
    }

    public static boolean isEmpty(ItemStack stack) {
        return liquidIdOf(stack) == null && solidIdOf(stack) == null
                && gasIdOf(stack) == null && !isWater(stack);
    }

    /** The substance key ({@code liquid_<id>} / {@code solid_<id>} / {@code gas_<id>})
     *  for a reagent stack, or the item path for non-bottle chemicals. This is
     *  what {@code ChemicalInfoProvider.forItem} expects. */
    public static String substanceKeyOf(ItemStack stack) {
        if (isLiquidBottle(stack)) {
            String id = liquidIdOf(stack);
            return id == null ? ID_LIQUID_BOTTLE : "liquid_" + id;
        }
        if (isDropperBottle(stack)) {
            String id = liquidIdOf(stack);
            return id == null ? ID_DROPPER_BOTTLE : "liquid_" + id;
        }
        if (isSolidJar(stack)) {
            String id = solidIdOf(stack);
            return id == null ? ID_SOLID_JAR : "solid_" + id;
        }
        if (isGasBottle(stack)) {
            String id = gasIdOf(stack);
            return id == null ? ID_GAS_BOTTLE : "gas_collecting_bottle_" + id;
        }
        return pathOf(stack);
    }

    /** The content-specific display name (e.g. "稀硫酸"), or null when empty. */
    public static Component displayName(ItemStack stack) {
        String liquid = liquidIdOf(stack);
        if (liquid != null) {
            for (com.example.chemistry.data.Liquids.Liquid l : com.example.chemistry.data.Liquids.ALL) {
                if (l.id().equals(liquid)) {
                    return Component.literal(l.chinese());
                }
            }
        }
        String solid = solidIdOf(stack);
        if (solid != null) {
            for (com.example.chemistry.data.Solids.Solid s : com.example.chemistry.data.Solids.ALL) {
                if (s.id().equals(solid)) {
                    return Component.literal(s.chinese());
                }
            }
        }
        String gas = gasIdOf(stack);
        if (gas != null) {
            for (com.example.chemistry.data.GasJars.GasJar g : com.example.chemistry.data.GasJars.ALL) {
                if (g.id().equals(gas)) {
                    return Component.literal(g.chinese());
                }
            }
        }
        if (isWater(stack)) {
            return Component.translatable("item.mchemistry.gas_collecting_bottle_water");
        }
        return null;
    }

    // --- Item model (select on custom_model_data string, reusing old assets) ---

    /** Update CUSTOM_MODEL_DATA so the select model picks the right texture. */
    public static void refreshModel(ItemStack stack) {
        String key = modelKey(stack);
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(java.util.List.of(), java.util.List.of(),
                        key.isEmpty() ? java.util.List.of() : java.util.List.of(key),
                        java.util.List.of()));
    }

    private static String modelKey(ItemStack stack) {
        if (isLiquidBottle(stack)) {
            String id = liquidIdOf(stack);
            return id == null ? "" : (isSealed(stack) ? "liquid_" : "open_liquid_") + id;
        }
        if (isSolidJar(stack)) {
            String id = solidIdOf(stack);
            return id == null ? "" : (isSealed(stack) ? "solid_" : "open_solid_") + id;
        }
        if (isGasBottle(stack)) {
            if (isWater(stack)) {
                return "gas_collecting_bottle_water";
            }
            String id = gasIdOf(stack);
            if (id == null) {
                return "empty_gas_collecting_bottle";
            }
            return (isSealed(stack) ? "gas_collecting_bottle_" : "open_gas_collecting_bottle_") + id;
        }
        if (isDropperBottle(stack)) {
            String id = liquidIdOf(stack);
            return id == null ? "empty_dropper_bottle" : "dropper_bottle_" + id;
        }
        return "";
    }

    // --- Volume ---

    public static int volumeOf(ItemStack stack) {
        int fallback = bottleCapacityOf(stack);
        return (int) tag(stack).getLongOr(KEY_ML, fallback);
    }

    /** Liquid volume in mB for fluid-pipe transfer: buckets, narrow bottles and
     *  dropper bottles (0 for gas jars / non-liquid items). */
    public static int liquidVolumeOf(ItemStack stack) {
        if (bucketIdOf(stack) != null) {
            return ChemUnits.BUCKET_VOLUME;
        }
        if (isLiquidBottle(stack)) {
            return (int) tag(stack).getLongOr(KEY_ML, ChemUnits.LIQUID_BOTTLE_VOLUME);
        }
        if (isDropperBottle(stack)) {
            return (int) tag(stack).getLongOr(KEY_ML, ChemUnits.DROPPER_BOTTLE_VOLUME);
        }
        return 0;
    }

    public static void setVolume(ItemStack stack, int volume) {
        CompoundTag t = tag(stack);
        t.putLong(KEY_ML, volume);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    /** Fixed capacity of a bottle item in mB (0 for non-bottles). */
    public static int bottleCapacityOf(ItemStack stack) {
        if (isLiquidBottle(stack)) {
            return ChemUnits.LIQUID_BOTTLE_VOLUME;
        }
        if (isDropperBottle(stack)) {
            return ChemUnits.DROPPER_BOTTLE_VOLUME;
        }
        if (isGasBottle(stack)) {
            return ChemUnits.GAS_JAR_VOLUME;
        }
        return 0;
    }

    // --- Content writes (mutate an existing stack) ---

    public static void setLiquid(ItemStack stack, String liquidId, boolean sealed, int ml) {
        CompoundTag t = tag(stack);
        if (liquidId == null || liquidId.isEmpty()) {
            t.remove(KEY_LIQUID);
        } else {
            t.putString(KEY_LIQUID, liquidId);
        }
        t.putBoolean(KEY_SEALED, sealed);
        t.putLong(KEY_ML, ml);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    public static void setSolid(ItemStack stack, String solidId, boolean sealed) {
        CompoundTag t = tag(stack);
        if (solidId == null || solidId.isEmpty()) {
            t.remove(KEY_SOLID);
        } else {
            t.putString(KEY_SOLID, solidId);
        }
        t.putBoolean(KEY_SEALED, sealed);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    public static void setGas(ItemStack stack, String gasId, boolean sealed) {
        CompoundTag t = tag(stack);
        if (gasId == null || gasId.isEmpty()) {
            t.remove(KEY_GAS);
        } else {
            t.putString(KEY_GAS, gasId);
        }
        t.putBoolean(KEY_SEALED, sealed);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    public static void setSealed(ItemStack stack, boolean sealed) {
        CompoundTag t = tag(stack);
        t.putBoolean(KEY_SEALED, sealed);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    public static void setWater(ItemStack stack, boolean water) {
        CompoundTag t = tag(stack);
        t.putBoolean(KEY_WATER, water);
        if (water) {
            t.remove(KEY_GAS);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    public static void setBrown(ItemStack stack, boolean brown) {
        CompoundTag t = tag(stack);
        t.putBoolean(KEY_BROWN, brown);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    // --- Fluid-transfer legacy helpers (bucket / water, kept for pipes) ---

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

    public static boolean isEmptyBucket(ItemStack stack) {
        return pathOf(stack).equals("bucket");
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
