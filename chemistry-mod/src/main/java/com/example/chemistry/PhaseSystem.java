package com.example.chemistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.chemistry.data.ChemicalInfoProvider;
import com.example.chemistry.item.LabVesselItem;

import net.minecraft.world.item.ItemStack;

/**
 * 物态变化引擎：融化/凝固、蒸发/凝结、升华/凝华、溶解/结晶。
 * 每 tick 按温度处理容器内容物：
 *  - 固体温度 ≥ 熔点 → 融化为 "molten_<id>" 液体；反之凝固回固体；
 *  - 液体温度 ≥ 沸点 → 蒸发（进入气相，或直接逸出）；气体低于沸点凝结为液体；
 *  - 少数物质（碘等）在较低温度升华，气体凝华为固体；
 *  - 常见盐在水存在时按溶解度溶解/结晶。
 */
public final class PhaseSystem {

    /** 每 tick 相变速率（克/秒 约值，按 tick 折算）。 */
    private static final double RATE = 0.2 / 20.0;
    /** 升华物质：碘等（固体 id -> 气体 id，升华温度）。 */
    private static final Map<String, String> SUBLIMATION = Map.of(
            "iodine", "iodine_vapor");
    private static final double IODINE_SUBLIME_TEMP = 50.0;
    /** 溶解度（g 溶质 / 100g 水），简化为常温近似值；超过则结晶。 */
    private static final Map<String, Double> SOLUBILITY = Map.ofEntries(
            Map.entry("sodium_chloride", 36.0),
            Map.entry("potassium_chloride", 34.0),
            Map.entry("potassium_nitrate", 32.0),
            Map.entry("sodium_nitrate", 88.0),
            Map.entry("ammonium_chloride", 37.0),
            Map.entry("sodium_carbonate", 21.5),
            Map.entry("copper_sulfate", 32.0),
            Map.entry("iron_sulfate", 29.0),
            Map.entry("silver_nitrate", 216.0));

    /** 溶解热（°C / g 溶质）：正数放热、负数吸热。 */
    private static final Map<String, Double> DISSOLUTION_HEAT = Map.of(
            "sodium_hydroxide", 0.08,
            "potassium_hydroxide", 0.06,
            "calcium_oxide", 0.10,
            "ammonium_nitrate", -0.05);

    private PhaseSystem() {
    }

    /** 该固体是否溶于水（用于入水溶解）。 */
    public static boolean isSoluble(String solidId) {
        return SOLUBILITY.containsKey(solidId);
    }

    /** 对容器内容物执行物态变化。 */
    public static void tick(ItemStack stack, double temp) {
        if (stack.isEmpty() || !(stack.getItem() instanceof LabVesselItem)) {
            return;
        }
        List<LabVesselItem.Entry> contents = LabVesselItem.getContents(stack);
        for (LabVesselItem.Entry e : new ArrayList<>(contents)) {
            if (e.type().equals("solid")) {
                tickSolid(stack, e, temp);
            } else if (e.type().equals("liquid")) {
                tickLiquid(stack, e, temp);
            }
        }
        tickGas(stack, temp);
        dissolveAndCrystallize(stack);
    }

    private static void tickSolid(ItemStack stack, LabVesselItem.Entry e, double temp) {
        double melt = meltingPoint(e.id());
        // 融化：固体 ≥ 熔点 → molten_<id> 液体。
        if (melt > 0 && temp >= melt) {
            double convert = Math.min(RATE, e.amount());
            LabVesselItem.consumeMass(stack, "solid", e.id(), convert);
            LabVesselItem.addMass(stack, "liquid", "molten_" + e.id(), convert);
            return;
        }
        // 升华：碘等。
        String gas = SUBLIMATION.get(e.id());
        if (gas != null && temp >= IODINE_SUBLIME_TEMP) {
            double convert = Math.min(RATE, e.amount());
            LabVesselItem.consumeMass(stack, "solid", e.id(), convert);
            VesselGasPhase.pour(stack, gas, convert * 10.0);
        }
    }

    private static void tickLiquid(ItemStack stack, LabVesselItem.Entry e, double temp) {
        // 熔融物凝固回固体。
        if (e.id().startsWith("molten_")) {
            String solid = e.id().substring("molten_".length());
            double melt = meltingPoint(solid);
            if (melt > 0 && temp < melt) {
                double convert = Math.min(RATE, e.amount());
                LabVesselItem.consumeMass(stack, "liquid", e.id(), convert);
                LabVesselItem.addMass(stack, "solid", solid, convert);
            }
            return;
        }
        // 蒸发：液体 ≥ 沸点 → 逸出（若有气体形式则进入气相）。
        double boil = ChemicalInfoProvider.boilingPointOf("liquid_" + e.id());
        if (temp >= boil) {
            double evaporate = Math.min(RATE, e.amount());
            LabVesselItem.consumeMass(stack, "liquid", e.id(), evaporate);
            String gas = liquidToGas(e.id());
            if (gas != null) {
                VesselGasPhase.pour(stack, gas, evaporate * 10.0);
            }
        }
    }

    private static void tickGas(ItemStack stack, double temp) {
        // 凝结/凝华：把气相中沸点高于当前温度的气体凝回液体/固体。
        for (VesselGasPhase.Part p : VesselGasPhase.read(stack)) {
            if (p.id().equals(VesselGasPhase.AIR)) {
                continue;
            }
            String liquid = gasToLiquid(p.id());
            double boil = liquid != null
                    ? ChemicalInfoProvider.boilingPointOf("liquid_" + liquid)
                    : 100.0;
            if (temp < boil - 1.0) {
                double condense = Math.min(p.ml() / 10.0, RATE);
                if (condense > 0.001) {
                    // 从气相扣减并生成液体；若该气体是升华气体则凝华为固体。
                    VesselGasPhase.remove(stack, p.id(), condense * 10.0);
                    String solid = gasToSublimableSolid(p.id());
                    if (solid != null && temp < IODINE_SUBLIME_TEMP - 10.0) {
                        LabVesselItem.addMass(stack, "solid", solid, condense);
                    } else if (liquid != null) {
                        LabVesselItem.addMass(stack, "liquid", liquid, condense);
                    }
                }
            }
        }
    }

    /** 溶解/结晶：水存在时按溶解度溶解固体；过饱和时结晶析出。 */
    private static void dissolveAndCrystallize(ItemStack stack) {
        dissolveAndCrystallize(stack, false);
    }

    /** 溶解/结晶（stirred=true 时溶解加速）。 */
    public static void dissolveAndCrystallize(ItemStack stack, boolean stirred) {
        LabVesselItem.Entry water = null;
        for (LabVesselItem.Entry e : LabVesselItem.getContents(stack)) {
            if (e.type().equals("liquid") && e.id().equals("water")) {
                water = e;
                break;
            }
        }
        if (water == null) {
            return;
        }
        double waterGrams = water.amount();
        for (LabVesselItem.Entry e : new ArrayList<>(LabVesselItem.getContents(stack))) {
            if (!e.type().equals("solid")) {
                continue;
            }
            Double sol = SOLUBILITY.get(e.id());
            if (sol == null) {
                continue;
            }
            // 100g 水可溶 sol g；waterGrams 水可溶 sol*waterGrams/100 g。
            double cap = sol * waterGrams / 100.0;
            // 渐进溶解（搅拌加速 4 倍）。
            double rate = stirred ? 2.0 : 0.5;
            double dissolve = Math.min(rate, Math.min(e.amount(), cap));
            if (dissolve > 0.001) {
                LabVesselItem.consumeMass(stack, "solid", e.id(), dissolve);
                LabVesselItem.addMass(stack, "liquid", e.id() + "_solution", dissolve);
                // 溶解热：放热升温 / 吸热降温。
                Double heat = DISSOLUTION_HEAT.get(e.id());
                if (heat != null) {
                    double temp = TemperatureSystem.getTemp(stack);
                    TemperatureSystem.setTemp(stack, temp + heat * dissolve);
                }
            }
        }
    }

    private static double meltingPoint(String solidId) {
        var info = ChemicalInfoProvider.forItem("solid_" + solidId);
        if (info == null) {
            return 0;
        }
        String m = info.melting();
        StringBuilder num = new StringBuilder();
        for (int i = 0; i < m.length(); i++) {
            char c = m.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                num.append(c);
            } else if (num.length() > 0) {
                break;
            }
        }
        try {
            return num.length() > 0 ? Double.parseDouble(num.toString()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String liquidToGas(String liquidId) {
        return switch (liquidId) {
            case "water" -> "water_vapor";
            case "hydrochloric_acid" -> "hydrogen_chloride";
            case "hydrochloric_acid_concentrated" -> "hydrogen_chloride";
            case "nitric_acid" -> "nitrogen_dioxide";
            case "ammonia_water" -> "ammonia";
            case "ammonia_water_concentrated" -> "ammonia";
            default -> null;
        };
    }

    private static String gasToLiquid(String gasId) {
        return switch (gasId) {
            case "water_vapor" -> "water";
            case "hydrogen_chloride" -> "hydrochloric_acid";
            case "ammonia" -> "ammonia_water";
            default -> null;
        };
    }

    private static String gasToSublimableSolid(String gasId) {
        for (Map.Entry<String, String> e : SUBLIMATION.entrySet()) {
            if (e.getValue().equals(gasId)) {
                return e.getKey();
            }
        }
        return null;
    }
}
