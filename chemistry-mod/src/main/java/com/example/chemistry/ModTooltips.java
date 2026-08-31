package com.example.chemistry;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.chemistry.api.ChemistryAPI;
import com.example.chemistry.api.Substances.LiquidSubstance;
import com.example.chemistry.data.ChemicalInfoProvider;
import com.example.chemistry.data.GasJars;
import com.example.chemistry.data.Solids;
import com.example.chemistry.data.Liquids;
import com.example.chemistry.api.goggles.ChemGoggleLines;
import com.example.chemistry.PurityHelper;
import com.example.chemistry.CombustionEngine;
import com.example.chemistry.item.CombustionSpoonItem;
import com.example.chemistry.item.DropperHelper;
import com.example.chemistry.item.DropperItem;
import com.example.chemistry.item.GasBottleItem;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.item.RubberTubeItem;
import com.example.chemistry.entity.RubberTubeEntity.Port;
import com.example.chemistry.item.SolidToolItem;
import com.example.chemistry.item.TestTubeItem;
import com.example.chemistry.TemperatureSystem;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.storage.ChemUnits;
import com.example.chemistry.transfer.BottleCodes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Appends chemistry information (formula, molar mass, toxicity, corrosiveness,
 * explosiveness, pH) to every chemical item's tooltip.
 */
@EventBusSubscriber(modid = ChemistryMod.MODID)
public class ModTooltips {

    private static final Map<String, String> LIQUID_NAMES = Liquids.ALL.stream()
            .collect(Collectors.toMap(Liquids.Liquid::id, Liquids.Liquid::chinese));

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        // 倒出的散装固体标注克数。
        if (stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag()
                .contains("chem_grams")) {
            double grams = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag()
                    .getDoubleOr("chem_grams", 0.0);
            event.getToolTip().add(Component.translatable(
                    "tooltip.mchemistry.solid_grams", String.format("%.1f", grams)));
        }
        if (stack.getItem() instanceof DropperItem && !DropperHelper.isEmpty(stack)) {
            String liquid = DropperHelper.getLiquid(stack);
            int ml = DropperHelper.getMl(stack);
            event.getToolTip().add(Component.translatable("tooltip.mchemistry.dropper_contents",
                    Component.literal(LIQUID_NAMES.getOrDefault(liquid, liquid)), ml, DropperHelper.CAPACITY));
        }
        if (stack.getItem() instanceof GasBottleItem) {
            event.getToolTip().add(Component.translatable("tooltip.mchemistry.gas_bottle_place"));
            net.minecraft.nbt.ListTag mix = stack.getOrDefault(
                    net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag()
                    .getListOrEmpty("chem_gas_mix");
            if (mix.size() > 1) {
                int total = 0;
                for (net.minecraft.nbt.Tag t : mix) {
                    if (t instanceof net.minecraft.nbt.CompoundTag c) {
                        total += c.getIntOr("ml", 0);
                    }
                }
                if (total > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mix.size(); i++) {
                        if (mix.get(i) instanceof net.minecraft.nbt.CompoundTag c) {
                            String id = c.getStringOr("id", "");
                            int ml = c.getIntOr("ml", 0);
                            int pct = (int) Math.round(100.0 * ml / total);
                            String name = GasJars.ALL.stream()
                                    .filter(g -> g.id().equals(id))
                                    .map(GasJars.GasJar::chinese)
                                    .findFirst().orElse(id);
                            if (i > 0) {
                                sb.append(" + ");
                            }
                            sb.append(name).append(' ').append(pct).append('%');
                        }
                    }
                    event.getToolTip().add(Component.translatable(
                            "tooltip.mchemistry.gas_mixture", Component.literal(sb.toString())));
                }
            }
        }
        if (stack.is(ModItems.SOLID_MIXTURE.get())) {
            List<LabVesselItem.Entry> contents = LabVesselItem.getContents(stack);
            if (!contents.isEmpty()) {
                String joined = contents.stream().map(e -> {
                    String n = e.type().equals("liquid")
                            ? LIQUID_NAMES.getOrDefault(e.id(), e.id())
                            : Solids.ALL.stream().filter(s -> s.id().equals(e.id()))
                                    .map(Solids.Solid::chinese).findFirst().orElse(e.id());
                    return n + "×" + String.format("%.1f", e.amount()) + "g";
                }).collect(Collectors.joining("、"));
                event.getToolTip().add(Component.translatable(
                        "tooltip.mchemistry.mixture_contents", Component.literal(joined)));
            }
        }
        if (stack.getItem() instanceof SolidToolItem tool) {
            String solid = SolidToolItem.getHeldSolid(stack);
            if (solid != null) {
                String name = Solids.ALL.stream().filter(s -> s.id().equals(solid))
                        .map(Solids.Solid::chinese).findFirst().orElse(solid);
                event.getToolTip().add(Component.translatable(
                        tool.holdsLumps() ? "tooltip.mchemistry.tweezers_holding" : "tooltip.mchemistry.spatula_holding",
                        Component.literal(name)));
            }
        }
        if (stack.getItem() instanceof LabVesselItem vessel) {
            List<LabVesselItem.Entry> contents = LabVesselItem.getContents(stack);
            double used = contents.stream().mapToDouble(LabVesselItem.Entry::amount).sum();
            event.getToolTip().add(Component.translatable("tooltip.mchemistry.vessel_capacity",
                    String.format("%.1f", used), vessel.capacity()));
            if (!contents.isEmpty()) {
                String joined = contents.stream().map(e -> {
                    String n = e.type().equals("liquid")
                            ? LIQUID_NAMES.getOrDefault(e.id(), e.id())
                            : Solids.ALL.stream().filter(s -> s.id().equals(e.id()))
                                    .map(Solids.Solid::chinese).findFirst().orElse(e.id());
                    return n + "×" + String.format("%.1f", e.amount()) + "g";
                }).collect(Collectors.joining(", "));
                event.getToolTip().add(Component.translatable("tooltip.mchemistry.vessel_contents", Component.literal(joined)));
            }
            List<VesselGasPhase.Part> gas = VesselGasPhase.read(stack);
            if (!gas.isEmpty()) {
                String gasJoined = gas.stream()
                        .map(p -> ChemGoggleLines.gasName(p.id())
                                + "×" + VesselGasPhase.formatMl(p.ml()) + "mL")
                        .collect(Collectors.joining(", "));
                event.getToolTip().add(Component.translatable(
                        "tooltip.mchemistry.vessel_gas", Component.literal(gasJoined)));
            }
        }
        if (stack.getItem() instanceof TestTubeItem) {
            double temp = TemperatureSystem.getTemp(stack);
            net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(
                    net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            // 只有插过温度计的试管才能查看温度。
            if (tag.getBooleanOr("chem_thermometer", false)) {
                event.getToolTip().add(Component.translatable("tooltip.mchemistry.tube_temp",
                        String.format("%.0f", temp)));
            }
        }
        if (stack.getItem() instanceof CombustionSpoonItem) {
            event.getToolTip().add(Component.translatable("tooltip.mchemistry.spoon_state",
                    Component.literal(CombustionEngine.isLit(stack) ? "燃烧中" : "未点燃")));
        }
        if (stack.is(ModItems.ALCOHOL_LAMP.get()) || stack.is(ModItems.ALCOHOL_LAMP_LIT.get())
                || stack.is(ModItems.ALCOHOL_LAMP_CAPPED.get())
                || stack.is(ModItems.ALCOHOL_BLOWTORCH.get())
                || stack.is(ModItems.ALCOHOL_BLOWTORCH_LIT.get())) {
            net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(
                    net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            double fuel = tag.getDoubleOr("chem_fuel_ml", 0.0);
            event.getToolTip().add(Component.translatable("tooltip.mchemistry.lamp_fuel",
                    String.format("%.0f", fuel)));
        }
        if (stack.getItem() instanceof RubberTubeItem) {
            event.getToolTip().add(Component.translatable(RubberTubeItem.isWet(stack)
                    ? "tooltip.mchemistry.rubber_tube_wet" : "tooltip.mchemistry.rubber_tube_dry"));
            Port start = RubberTubeItem.readPending(stack);
            if (start != null) {
                if (start.kind() == Port.KIND_ENTITY) {
                    event.getToolTip().add(Component.translatable("tooltip.mchemistry.rubber_tube_start_entity"));
                } else if (start.kind() == Port.KIND_STAND) {
                    event.getToolTip().add(Component.translatable("tooltip.mchemistry.rubber_tube_start_stand"));
                } else {
                    event.getToolTip().add(Component.translatable("tooltip.mchemistry.rubber_tube_start",
                            start.pos().getX(), start.pos().getY(), start.pos().getZ()));
                }
            }
        }
        String path = BottleCodes.substanceKeyOf(stack);
        appendAddonSubstanceInfo(event, path);
        ChemicalInfoProvider.ChemicalInfo info = ChemicalInfoProvider.forItem(path);
        if (info == null) {
            return;
        }
        List<Component> tip = event.getToolTip();
        tip.add(Component.empty());
        tip.add(Component.translatable("tooltip.mchemistry.formula", Component.literal(info.formula())));
        tip.add(Component.translatable("tooltip.mchemistry.molar_mass", Component.literal(info.molarMass())));
        tip.add(Component.translatable("tooltip.mchemistry.toxicity", Component.translatable("toxicity.mchemistry." + info.toxicity())));
        tip.add(Component.translatable("tooltip.mchemistry.corrosiveness", Component.translatable("corr.mchemistry." + info.corrosiveness())));
        tip.add(Component.translatable("tooltip.mchemistry.explosiveness", Component.translatable("expl.mchemistry." + info.explosiveness())));
        if (info.ph().equals("n/a")) {
            tip.add(Component.translatable("tooltip.mchemistry.ph_na", Component.translatable("ph.mchemistry.n_a")));
        } else {
            String[] parts = info.ph().split("\\|", 2);
            tip.add(Component.translatable("tooltip.mchemistry.ph",
                    Component.literal(parts[0]),
                    Component.translatable("ph.mchemistry." + parts[1])));
        }
        tip.add(Component.translatable("tooltip.mchemistry.density", Component.literal(info.density())));
        tip.add(Component.translatable("tooltip.mchemistry.appearance", Component.literal(info.appearance())));
        tip.add(Component.translatable("tooltip.mchemistry.odour", Component.literal(info.odour())));
        tip.add(Component.translatable("tooltip.mchemistry.melting", Component.literal(info.melting())));
        tip.add(Component.translatable("tooltip.mchemistry.boiling", Component.literal(info.boiling())));
        tip.add(Component.translatable("tooltip.mchemistry.purity",
                Component.literal(String.format("%.5f%%", PurityHelper.getPurity(stack) * 100))));
    }

    /**
     * Framework fallback: addon substances registered through
     * {@link ChemistryAPI} get a basic formula/concentration line here, since
     * the generated chemical-info provider only covers core substances.
     */
    private static void appendAddonSubstanceInfo(ItemTooltipEvent event, String path) {
        if (!path.startsWith("liquid_") || path.startsWith("open_") || path.endsWith("_bucket")) {
            return;
        }
        String id = path.substring("liquid_".length());
        for (LiquidSubstance liquid : ChemistryAPI.addonLiquids()) {
            if (liquid.id().equals(id)) {
                event.getToolTip().add(Component.literal("化学式 " + liquid.formula()));
                event.getToolTip().add(Component.literal("浓度 " + liquid.concentration().displayName()
                        + "（×" + liquid.concentration().speed() + "）"));
                return;
            }
        }
    }
}
