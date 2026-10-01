package com.example.chemistry.api.goggles;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.chemistry.data.ChemicalInfoProvider;
import com.example.chemistry.data.GasJars;
import com.example.chemistry.data.Liquids;
import com.example.chemistry.data.Solids;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.item.LabVesselItem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Shared line formatting for the chemist's goggles overlay. */
public final class ChemGoggleLines {

    private static final Map<String, String> LIQUID_NAMES = Liquids.ALL.stream()
            .collect(Collectors.toMap(Liquids.Liquid::id, Liquids.Liquid::chinese));
    private static final Map<String, String> SOLID_NAMES = Solids.ALL.stream()
            .collect(Collectors.toMap(Solids.Solid::id, Solids.Solid::chinese));
    private static final Map<String, GasJars.GasJar> GAS_BY_ID = GasJars.ALL.stream()
            .collect(Collectors.toMap(GasJars.GasJar::id, g -> g));

    private ChemGoggleLines() {
    }

    public static String liquidName(String id) {
        if(com.example.chemistry.organic.Extraction.iodine(id)&&!id.equals("iodine_water"))return "碘（"+liquidName(id.substring("iodine_".length()))+"相）";
        if (id.startsWith("molten_")) {
            return "熔融" + solidName(id.substring("molten_".length()));
        }
        return LIQUID_NAMES.getOrDefault(id, id);
    }

    public static String solidName(String id) {
        return SOLID_NAMES.getOrDefault(id, id);
    }

    public static String gasName(String id) {
        if(id.equals("water_vapor"))return "水蒸气";
        GasJars.GasJar gas = GAS_BY_ID.get(id);
        return gas != null ? gas.chinese() : id;
    }

    public static String gasFormula(String id) {
        if(id.equals("water_vapor"))return "H₂O";
        GasJars.GasJar gas = GAS_BY_ID.get(id);
        return gas != null ? gas.formula() : "";
    }

    public static Component temp(double temp) {
        return Component.literal("温度：" + String.format("%.0f", temp) + "°C");
    }

    /** Temperature line with the I-key pin state appended. */
    public static Component temp(double temp, boolean locked) {
        return Component.literal("温度：" + String.format("%.0f", temp) + "°C"
                + (locked ? "（已锁定）" : ""));
    }

    /** Sealed vessels build up pressure when heated; show it like a gauge. */
    public static void appendPressure(List<Component> tooltip, ItemStack stack) {
        if(com.example.chemistry.utility.VacuumState.enabled(stack)){tooltip.add(Component.literal(String.format(java.util.Locale.ROOT,"绝对气压：%.2f kPa；水沸点约 %.1f°C",VesselHeating.pressureKpa(stack),com.example.chemistry.utility.VacuumState.boilingPoint(stack,"water"))));return;}
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int pressure = tag.getIntOr("chem_pressure", 0);
        boolean sealed = VesselHeating.isSealed(stack);
        if (!sealed && pressure <= 0) {
            return;
        }
        tooltip.add(Component.literal("压力：" + pressure + "/" + VesselHeating.POP_PRESSURE
                + (sealed ? "（密封）" : "（已泄压）")));
    }

    /** Detailed mole view is shown only while inspecting a vessel while sneaking. */
    public static void appendSpecies(List<Component> tooltip, ItemStack stack) {
        com.example.chemistry.solution.AcidBaseEquilibrium.appendInfo(tooltip,stack,true);
        com.example.chemistry.solution.PrecipitationEquilibrium.appendInfo(tooltip,stack,true);
        com.example.chemistry.solution.CoordinationEquilibrium.appendInfo(tooltip,stack,true);
        com.example.chemistry.solution.BatchCoordination.appendInfo(tooltip,stack);
        com.example.chemistry.garden.ChemicalGarden.appendInfo(tooltip,stack);
        com.example.chemistry.solution.EdtaEquilibrium.appendInfo(tooltip,stack);
        try {
            var inventory = com.example.chemistry.solution.SolutionSpecies.snapshot(stack);
            var entries = inventory.moles().entrySet().stream()
                    .filter(e -> e.getValue() > 0)
                    .filter(e -> {
                        var phase = com.example.chemistry.solution.SpeciesCatalog.get(e.getKey()).phase();
                        return phase == com.example.chemistry.solution.ChemicalSpecies.Phase.AQUEOUS
                                || phase == com.example.chemistry.solution.ChemicalSpecies.Phase.SOLVENT;
                    }).sorted(java.util.Map.Entry.comparingByKey()).toList();
            if (entries.isEmpty() && inventory.fullyModelled()) return;
            tooltip.add(Component.literal("溶液物种："));
            if (inventory.amount("iron_thiocyanate") > 0) {
                tooltip.add(Component.literal("  Fe³⁺ + SCN⁻ ⇌ [FeSCN]²⁺（室温近似）"));
            }
            for (var entry : entries.stream().limit(8).toList()) {
                var species = com.example.chemistry.solution.SpeciesCatalog.get(entry.getKey());
                tooltip.add(Component.literal("  " + species.label() + " ×"
                        + String.format(java.util.Locale.ROOT, "%.3g", entry.getValue() * 1000) + " mmol"));
            }
            if (entries.size() > 8) tooltip.add(Component.literal("  另有 " + (entries.size() - 8) + " 种"));
            if (!inventory.fullyModelled()) tooltip.add(Component.literal("  部分成分尚未解析，原质量已保留"));
        } catch (IllegalArgumentException invalidContents) {
            tooltip.add(Component.literal("溶液物种：内容物数据异常"));
        }
    }

    /** Appends the vessel's contents summary (mass + per-substance lines). */
    public static void appendContents(List<Component> tooltip, ItemStack stack) {
        com.example.chemistry.organic.LiquidPhases.appendInfo(tooltip,stack);
        com.example.chemistry.solution.AcidBaseEquilibrium.appendInfo(tooltip,stack,false);
        com.example.chemistry.ReactionEngine.appendFeedback(tooltip,stack);
        com.example.chemistry.solution.PrecipitationEquilibrium.appendInfo(tooltip,stack,false);
        com.example.chemistry.solution.CoordinationEquilibrium.appendInfo(tooltip,stack,false);
        com.example.chemistry.solution.BatchCoordination.appendInfo(tooltip,stack);
        com.example.chemistry.garden.ChemicalGarden.appendInfo(tooltip,stack);
        com.example.chemistry.solution.EdtaEquilibrium.appendInfo(tooltip,stack);
        List<LabVesselItem.Entry> entries = LabVesselItem.getContents(stack);
        if (entries.isEmpty()) {
            tooltip.add(Component.literal("内容物：空"));
        } else {
            double used = entries.stream().mapToDouble(LabVesselItem.Entry::amount).sum();
            tooltip.add(Component.literal("内容物：" + String.format("%.1f", used) + "g"));
            for (LabVesselItem.Entry e : entries) {
                String name;
                if (e.type().equals("liquid")) {
                    if(com.example.chemistry.organic.Extraction.iodine(e.id())){tooltip.add(Component.literal("  "+liquidName(e.id())+" ×"+String.format(java.util.Locale.ROOT,"%.3f",e.amount()*1000)+" mg"));continue;}
                    if (com.example.chemistry.data.Solutions.soluteOf(e.id()) != null
                            && LabVesselItem.isExplicitSolute(stack, e.id())) {
                        tooltip.add(Component.literal("  " + solidName(com.example.chemistry.data.Solutions.soluteOf(e.id()))
                                + "（已溶） ×" + String.format("%.1f", e.amount()) + "g"));
                        continue;
                    }
                    name = liquidName(e.id());
                    double density = ChemicalInfoProvider.densityOfLiquid(e.id());
                    if (density > 0) {
                        name += " ×" + String.format("%.1f", e.amount() / density) + "mL";
                    } else {
                        name += " ×" + String.format("%.1f", e.amount()) + "g";
                    }
                } else {
                    name = solidName(e.id()) + " ×" + String.format("%.1f", e.amount()) + "g";
                }
                tooltip.add(Component.literal("  " + name));
            }
        }
        List<com.example.chemistry.VesselGasPhase.Part> gas =
                com.example.chemistry.VesselGasPhase.read(stack);
        if (!gas.isEmpty()) {
            String gasLine = gas.stream()
                    .map(p -> gasName(p.id()) + " "
                            + com.example.chemistry.VesselGasPhase.formatMl(p.ml()) + "mL")
                    .collect(java.util.stream.Collectors.joining(", "));
            tooltip.add(Component.literal("气相：" + gasLine));
        }
    }
}
