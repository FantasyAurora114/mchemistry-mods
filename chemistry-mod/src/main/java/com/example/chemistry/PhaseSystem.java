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
 *  - 晶体在熔点吸收/释放热量，熔化或凝固期间温度保持不变；
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
    private PhaseSystem() {
    }

    /** 该固体是否溶于水（用于入水溶解）。 */
    public static boolean isSoluble(String solidId) {
        return AqueousSolubility.hasCurve(solidId);
    }

    /** Apply heat while holding temperature at a crystal's melting point. */
    public static double applyHeatJoules(ItemStack stack, double current, double joules) {
        if (!(stack.getItem() instanceof LabVesselItem)
                || !Double.isFinite(current) || !Double.isFinite(joules)
                || joules == 0) {
            return current;
        }
        boolean heating = joules > 0;
        double temperature = current;
        double budget = Math.abs(joules);
        // A vessel may contain several crystals with different melting points.
        for (int step = 0; step < 32 && budget > 1.0e-9; step++) {
            LabVesselItem.Entry phase = null;
            double transition = heating ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            for (LabVesselItem.Entry entry : LabVesselItem.getContents(stack)) {
                String solid = heating && entry.type().equals("solid") ? entry.id()
                        : !heating && entry.type().equals("liquid")
                                && entry.id().startsWith("molten_")
                                ? entry.id().substring("molten_".length()) : null;
                if (solid == null || entry.amount() <= 0) {
                    continue;
                }
                double point = com.example.chemistry.solution.HydrateChemistry.hydrate(solid) && heating ? com.example.chemistry.solution.HydrateChemistry.transition(solid) : meltingPoint(solid);
                if (point <= 0) {
                    continue;
                }
                if (heating && point >= temperature && point <= temperature + budget / ThermalSystem.capacity(stack)
                        && point < transition) {
                    phase = entry;
                    transition = point;
                } else if (!heating && point <= temperature && point >= temperature - budget / ThermalSystem.capacity(stack)
                        && point > transition) {
                    phase = entry;
                    transition = point;
                }
            }
            if (phase == null) {
                return temperature + (heating ? budget : -budget)/ThermalSystem.capacity(stack);
            }
            budget -= Math.abs(transition - temperature)*ThermalSystem.capacity(stack);
            temperature = transition;
            double converted = Math.min(phase.amount(),
                    budget / ThermalSystem.fusionJPerGram(heating ? phase.id() : phase.id().substring("molten_".length())));
            if (converted <= 1.0e-9) {
                return temperature;
            }
            if (heating && com.example.chemistry.solution.HydrateChemistry.hydrate(phase.id())) {
                com.example.chemistry.solution.HydrateChemistry.convert(stack,phase.id(),converted);
            } else if (heating) {
                LabVesselItem.consumeMass(stack, "solid", phase.id(), converted);
                LabVesselItem.addMass(stack, "liquid", "molten_" + phase.id(), converted);
            } else {
                LabVesselItem.consumeMass(stack, "liquid", phase.id(), converted);
                LabVesselItem.addMass(stack, "solid",
                        phase.id().substring("molten_".length()), converted);
            }
            budget -= converted * ThermalSystem.fusionJPerGram(heating ? phase.id() : phase.id().substring("molten_".length()));
            if (converted + 1.0e-6 < phase.amount()) {
                return temperature;
            }
        }
        return temperature + (heating ? budget : -budget)/ThermalSystem.capacity(stack);
    }

    /** 对容器内容物执行物态变化。 */
    public static void tick(ItemStack stack, double temp) {
        tick(stack, temp, false);
    }

    /** A condenser collects the boiling fraction in VesselHeating instead of venting it here. */
    public static void tick(ItemStack stack, double temp, boolean condenserCollecting) {
        if (stack.isEmpty() || !(stack.getItem() instanceof LabVesselItem)) {
            return;
        }
        LabVesselItem.normalizeSolutions(stack);
        com.example.chemistry.solution.CoordinationEquilibrium.tick(stack);
        List<LabVesselItem.Entry> contents = LabVesselItem.getContents(stack);
        for (LabVesselItem.Entry e : new ArrayList<>(contents)) {
            if (e.type().equals("solid")) {
                tickSolid(stack, e, temp);
            } else if (e.type().equals("liquid") && !condenserCollecting) {
                tickLiquid(stack, e, temp);
            }
        }
        tickGas(stack, temp);
        com.example.chemistry.solution.FutureChemistry.tick(stack);
        com.example.chemistry.organic.AdvancedOrganicChemistry.tick(stack);
        com.example.chemistry.solution.BatchChemistry.tick(stack);
        com.example.chemistry.solution.BatchCoordination.tick(stack);
        dissolveAndCrystallize(stack);
        com.example.chemistry.solution.AcidBaseEquilibrium.tick(stack);
        com.example.chemistry.solution.PrecipitationEquilibrium.tick(stack);
    }

    private static void tickSolid(ItemStack stack, LabVesselItem.Entry e, double temp) {
        // 升华：碘等。
        String gas = SUBLIMATION.get(e.id());
        if (gas != null && temp >= IODINE_SUBLIME_TEMP) {
            double convert = Math.min(RATE, e.amount());
            LabVesselItem.consumeMass(stack, "solid", e.id(), convert);
            VesselGasPhase.pour(stack, gas, convert * 10.0);
        }
    }

    private static void tickLiquid(ItemStack stack, LabVesselItem.Entry e, double temp) {
        // Melting/freezing is driven by temperature changes and latent heat.
        if (e.id().startsWith("molten_") || e.id().endsWith("_solution")
                || com.example.chemistry.data.Solutions.soluteOf(e.id()) != null) {
            return;
        }
        // 蒸发：液体 ≥ 沸点 → 逸出（若有气体形式则进入气相）。
        double boil = com.example.chemistry.utility.VacuumState.boilingPoint(stack,e.id());
        if (temp >= boil) {
            double evaporate = ThermalSystem.vaporizationLimit(stack,e.id(),Math.min(RATE,e.amount()),boil);
            if(evaporate<=1e-12)return;
            LabVesselItem.consumeMass(stack, "liquid", e.id(), evaporate);
            ThermalSystem.vaporized(stack,e.id(),evaporate,boil);
            String gas = liquidToGas(e.id());
            if (gas != null) {
                VesselGasPhase.pour(stack,gas,evaporate*(gas.equals("water_vapor")?com.example.chemistry.electrical.WaterElectrolysis.GAS_ML_PER_MOLE/com.example.chemistry.electrical.WaterElectrolysis.WATER_MOLAR:10));
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
            if(liquid==null&&gasToSublimableSolid(p.id())==null)continue;
            double boil = liquid != null
                    ? com.example.chemistry.utility.VacuumState.boilingPoint(stack,liquid)
                    : 100.0;
            if (temp < boil - 1.0) {
                double mlPerGram=p.id().equals("water_vapor")?com.example.chemistry.electrical.WaterElectrolysis.GAS_ML_PER_MOLE/com.example.chemistry.electrical.WaterElectrolysis.WATER_MOLAR:10;
                double condense=Math.min(p.ml()/mlPerGram,RATE);
                if(condense>1e-9){
                    // 从气相扣减并生成液体；若该气体是升华气体则凝华为固体。
                    VesselGasPhase.remove(stack,p.id(),condense*mlPerGram);
                    String solid = gasToSublimableSolid(p.id());
                    if (solid != null && temp < IODINE_SUBLIME_TEMP - 10.0) {
                        LabVesselItem.addMass(stack, "solid", solid, condense);
                    } else if (liquid != null) {
                        LabVesselItem.addMass(stack,"liquid",liquid,condense);
                        if(liquid.equals("water"))ThermalSystem.addHeat(stack,condense*ThermalSystem.WATER_VAPORIZATION,"condensation");
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
        LabVesselItem.normalizeSolutions(stack);
        if(stirred) com.example.chemistry.garden.ChemicalGarden.stir(stack);
        LabVesselItem.tickSuspension(stack, stirred);
        com.example.chemistry.organic.Extraction.tick(stack);
        com.example.chemistry.organic.LiquidPhases.tick(stack,stirred);
        LabVesselItem.Entry water = null;
        for (LabVesselItem.Entry e : LabVesselItem.getContents(stack)) {
            if (e.type().equals("liquid") && e.id().equals("water")) {
                water = e;
                break;
            }
        }
        if (water == null) {
            // No solvent remains: retain every last gram as residue at any temperature.
            for (LabVesselItem.Entry entry : LabVesselItem.getContents(stack)) {
                String solute = com.example.chemistry.data.Solutions.soluteOf(entry.id());
                if (entry.type().equals("liquid") && solute != null) {
                    LabVesselItem.consumeMass(stack, "liquid", entry.id(), entry.amount());
                    LabVesselItem.addMass(stack, "solid", solute, entry.amount());
                }
            }
            return;
        }
        // Cloudy limewater is a suspension, not dissolved calcium carbonate.
        for (LabVesselItem.Entry entry : LabVesselItem.getContents(stack)) {
            if (entry.type().equals("liquid") && entry.id().equals("limewater_cloudy")) {
                LabVesselItem.consumeMass(stack, "liquid", entry.id(), entry.amount());
                LabVesselItem.addMass(stack, "solid", "calcium_carbonate", entry.amount());
            }
        }
        double waterGrams = water.amount();
        // Evaluate both solid and dissolved entries: cooling can crystallise a
        // solution even after its last visible solid grain has dissolved.
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (LabVesselItem.Entry e : LabVesselItem.getContents(stack)) {
            if (e.type().equals("solid") && AqueousSolubility.hasCurve(e.id())) ids.add(e.id());
            if (e.type().equals("liquid") && e.id().endsWith("_solution")) {
                String id = AqueousSolubility.solidIdForSolution(e.id());
                if (id != null) ids.add(id);
            }
        }
        for (String id : ids) {
            if(com.example.chemistry.solution.BatchPrecipitation.manages(id))continue;
            if(com.example.chemistry.garden.ChemicalGarden.reserve(stack,id)) continue;
            if(id.equals("iodine")&&com.example.chemistry.organic.LiquidPhases.read(stack).layers().size()==2)continue;
            double solid = mass(stack, "solid", id);
            String solutionId = AqueousSolubility.solutionId(id);
            double dissolved = mass(stack, "liquid", solutionId);
            double cap = waterGrams * AqueousSolubility.gramsPer100gWater(
                    id, TemperatureSystem.getTemp(stack)) / 100.0;
            double delta = Math.max(-dissolved, Math.min(solid, cap - dissolved));
            double rate = stirred ? 2.0 : 0.5;
            delta = Math.max(-rate, Math.min(rate, delta));
            if (Math.abs(delta) <= 0.001) continue;
            if (delta > 0) {
                LabVesselItem.consumeMass(stack, "solid", id, delta);
                LabVesselItem.addMass(stack, "liquid", solutionId, delta);
            } else {
                if(com.example.chemistry.solution.HydrateChemistry.ownsCrystallization(id,TemperatureSystem.getTemp(stack)))continue;
                LabVesselItem.consumeMass(stack, "liquid", solutionId, -delta);
                LabVesselItem.addMass(stack, "solid", id, -delta);
            }
            // Q = -ΔH * Δn, then ΔT = Q / heat capacity of the contents.
            double molarMass = ChemicalInfoProvider.molarMassOf("solid_" + id);
            if (molarMass > 0 && AqueousSolubility.enthalpyJPerMol(id) != 0) {
                double heatJ = -AqueousSolubility.enthalpyJPerMol(id) * delta / molarMass;
                ThermalSystem.addHeat(stack,heatJ,"dissolution");
            }
        }
    }

    private static double mass(ItemStack stack, String type, String id) {
        return LabVesselItem.getContents(stack).stream()
                .filter(e -> e.type().equals(type) && e.id().equals(id))
                .mapToDouble(LabVesselItem.Entry::amount).sum();
    }

    private static double meltingPoint(String solidId) {
        // The catalogue's 400 C for CaC2O4 is decomposition, not a liquid phase.
        if(solidId.equals("calcium_oxalate"))return 0;
        var info = ChemicalInfoProvider.forItem("solid_" + solidId);
        if (info == null) {
            return 0;
        }
        String m = info.melting().trim();
        // A decomposition or sublimation temperature is not a melting point.
        if (!m.matches("[0-9]+(?:\\.[0-9]+)?°C")) {
            return 0;
        }
        try {
            return Double.parseDouble(m.substring(0, m.length() - 2));
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
