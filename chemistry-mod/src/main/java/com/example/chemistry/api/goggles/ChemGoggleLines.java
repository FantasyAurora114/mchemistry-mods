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
        return LIQUID_NAMES.getOrDefault(id, id);
    }

    public static String solidName(String id) {
        return SOLID_NAMES.getOrDefault(id, id);
    }

    public static String gasName(String id) {
        GasJars.GasJar gas = GAS_BY_ID.get(id);
        return gas != null ? gas.chinese() : id;
    }

    public static String gasFormula(String id) {
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
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int pressure = tag.getIntOr("chem_pressure", 0);
        boolean sealed = VesselHeating.isSealed(stack);
        if (!sealed && pressure <= 0) {
            return;
        }
        tooltip.add(Component.literal("压力：" + pressure + "/" + VesselHeating.POP_PRESSURE
                + (sealed ? "（密封）" : "（已泄压）")));
    }

    /** Appends the vessel's contents summary (mass + per-substance lines). */
    public static void appendContents(List<Component> tooltip, ItemStack stack) {
        List<LabVesselItem.Entry> entries = LabVesselItem.getContents(stack);
        if (entries.isEmpty()) {
            tooltip.add(Component.literal("内容物：空"));
        } else {
            double used = entries.stream().mapToDouble(LabVesselItem.Entry::amount).sum();
            tooltip.add(Component.literal("内容物：" + String.format("%.1f", used) + "g"));
            for (LabVesselItem.Entry e : entries) {
                String name;
                if (e.type().equals("liquid")) {
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
