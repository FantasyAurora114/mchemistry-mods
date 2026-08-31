package com.example.chemistry.item;

import java.util.ArrayList;
import java.util.List;

import com.example.chemistry.data.ChemicalInfoProvider;
import com.example.chemistry.data.Liquids;
import com.example.chemistry.data.Solids;
import com.example.chemistry.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

/**
 * A reaction vessel (试管 / 烧杯) that holds reagents. Contents are stored as
 * custom data; the item texture shows a liquid-coloured fill when non-empty.
 */
public class LabVesselItem extends Item {

    private final int capacity;

    public LabVesselItem(Properties properties, int capacity) {
        super(properties);
        this.capacity = capacity;
    }

    /** Let the client send the use packet when the other hand holds a transfer
     *  tool / bottle, so the vessel can be filled from either hand. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack other = hand == InteractionHand.MAIN_HAND
                ? player.getOffhandItem() : player.getMainHandItem();
        if (com.example.chemistry.LabInteractions.isTransferTool(other)) {
            return InteractionResult.SUCCESS;
        }
        // 左手反应容器 + 右手空手：倒出容器内全部物质（之后容器为空）。
        if (hand == InteractionHand.OFF_HAND && other.isEmpty()) {
            if (!level.isClientSide()) {
                pourOutAll(player, player.getOffhandItem());
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public int capacity() {
        return capacity;
    }

    public record Entry(String type, String id, double amount) {
    }

    /** Hydrated salts -> anhydrous product (mass preserved by molar ratio). */
    private static final java.util.Map<String, String> HYDROUS_TO_ANHYDROUS =
            java.util.Map.of(
                    "iron_sulfate_heptahydrate", "iron_sulfate",
                    "copper_sulfate_pentahydrate", "copper_sulfate_anhydrous");

    /** Solid phase change inside a vessel: hydrated salts lose their water of
     *  crystallisation (绿矾/胆矾 -> anhydrous sulfate) instead of vanishing,
     *  and only a much hotter anhydrous solid evaporates slowly. Shared by the
     *  held-vessel ticker and the placed-vessel thermodynamics. */
    public static boolean phaseChangeSolid(ItemStack stack, Entry entry, double temp, double rate) {
        if (temp <= ChemicalInfoProvider.boilingPointOf("solid_" + entry.id())) {
            return false;
        }
        String anhydrous = HYDROUS_TO_ANHYDROUS.get(entry.id());
        if (anhydrous != null) {
            double anhydrousMolar = ChemicalInfoProvider.molarMassOf("solid_" + anhydrous);
            double hydrousMolar = ChemicalInfoProvider.molarMassOf("solid_" + entry.id());
            if (anhydrousMolar > 0 && hydrousMolar > 0) {
                double converted = Math.min(entry.amount(), rate);
                consumeMass(stack, "solid", entry.id(), converted);
                addMass(stack, "solid", anhydrous, converted * anhydrousMolar / hydrousMolar);
            } else {
                consumeMass(stack, "solid", entry.id(), entry.amount());
            }
            return true;
        }
        if (ChemicalInfoProvider.boilingPointOf("solid_" + entry.id()) > 400) {
            // True boiling point: evaporate slowly. Below that, the "boiling
            // point" is a dehydration temperature, not an evaporation one.
            consumeMass(stack, "solid", entry.id(), Math.min(entry.amount(), rate));
        } else {
            consumeMass(stack, "solid", entry.id(), entry.amount());
        }
        return true;
    }

    public static List<Entry> getContents(ItemStack stack) {
        List<Entry> out = new ArrayList<>();
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = tag.getListOrEmpty("chem_contents");
        for (Tag t : list) {
            CompoundTag c = (CompoundTag) t;
            out.add(new Entry(c.getStringOr("type", ""), c.getStringOr("id", ""), c.getDoubleOr("amount", 0.0)));
        }
        return out;
    }

    /** Colour of the first content entry (liquid or solid), for placed rendering. */
    public static int contentsColor(ItemStack stack) {
        for (Entry e : getContents(stack)) {
            if (e.type().equals("liquid")) {
                for (Liquids.Liquid l : Liquids.ALL) {
                    if (l.id().equals(e.id())) {
                        return l.color();
                    }
                }
            } else {
                for (Solids.Solid s : Solids.ALL) {
                    if (s.id().equals(e.id())) {
                        return s.color();
                    }
                }
            }
        }
        return 0xFFFFFF;
    }

    public static double totalMass(ItemStack stack) {
        return getContents(stack).stream().mapToDouble(Entry::amount).sum();
    }

    public static boolean canAdd(ItemStack stack, double mass) {
        if (!(stack.getItem() instanceof LabVesselItem vessel)) {
            return false;
        }
        return totalMass(stack) + mass <= vessel.capacity;
    }

    public static boolean addLiquid(ItemStack stack, String liquidId, int ml) {
        double grams = ml * ChemicalInfoProvider.densityOfLiquid(liquidId);
        return add(stack, "liquid", liquidId, grams);
    }

    public static boolean addSolid(ItemStack stack, String solidId) {
        return add(stack, "solid", solidId, 5.0);
    }

    /** Add a raw mass (grams), used when reaction products are produced. */
    public static boolean addMass(ItemStack stack, String type, String id, double grams) {
        return add(stack, type, id, grams, false);
    }

    /** Remove a raw mass (grams) from a reactant entry; drops the entry at zero. */
    public static void consumeMass(ItemStack stack, String type, String id, double grams) {
        ListTag list = new ListTag();
        for (Entry e : getContents(stack)) {
            if (e.type().equals(type) && e.id().equals(id)) {
                double remaining = e.amount() - grams;
                if (remaining > 0.001) {
                    CompoundTag c = new CompoundTag();
                    c.putString("type", type);
                    c.putString("id", id);
                    c.putDouble("amount", remaining);
                    list.add(c);
                }
            } else {
                CompoundTag c = new CompoundTag();
                c.putString("type", e.type());
                c.putString("id", e.id());
                c.putDouble("amount", e.amount());
                list.add(c);
            }
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove(com.example.chemistry.ReactionEngine.KEY_EQUILIBRIUM);
        tag.put("chem_contents", list);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        com.example.chemistry.VesselGasPhase.normalize(stack);
        updateTint(stack);
    }

    /** Empties the vessel (e.g. a fully inverted test tube spills its contents). */
    public static void clearContents(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove("chem_contents");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        com.example.chemistry.VesselGasPhase.normalize(stack);
    }

    /**
     * 左手反应容器 + 右手空手：倒出全部物质。腐蚀性液体会造成伤害；
     * 固体以物品形式归还（单一固体给散装物品并标注克数，多种固体给混合物）。
     */
    public static void pourOutAll(Player player, ItemStack vessel) {
        if (player == null || vessel.isEmpty() || !(vessel.getItem() instanceof LabVesselItem)) {
            return;
        }
        List<Entry> contents = getContents(vessel);
        if (contents.isEmpty()) {
            return;
        }
        // 腐蚀性液体伤害
        float dmg = 0.0F;
        for (Entry e : contents) {
            if (!e.type().equals("liquid")) {
                continue;
            }
            ChemicalInfoProvider.ChemicalInfo info =
                    ChemicalInfoProvider.forItem("liquid_" + e.id());
            String corr = info == null ? "none" : info.corrosiveness();
            if ("strong".equals(corr)) {
                dmg = Math.max(dmg, 8.0F);
            } else if ("moderate".equals(corr)) {
                dmg = Math.max(dmg, 4.0F);
            }
        }
        if (dmg > 0.0F) {
            player.hurt(player.damageSources().generic(), dmg);
        }
        // 固体 → 物品形式
        List<Entry> solids = contents.stream().filter(e -> e.type().equals("solid")).toList();
        if (!solids.isEmpty()) {
            ItemStack dump;
            if (solids.size() == 1) {
                Entry only = solids.get(0);
                dump = new ItemStack(ModItems.looseSolid(only.id()));
                CompoundTag tag = dump.getOrDefault(
                        DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                tag.putDouble("chem_grams", only.amount());
                dump.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            } else {
                dump = new ItemStack(ModItems.SOLID_MIXTURE.get());
                CompoundTag tag = new CompoundTag();
                ListTag list = new ListTag();
                for (Entry e : solids) {
                    CompoundTag c = new CompoundTag();
                    c.putString("type", e.type());
                    c.putString("id", e.id());
                    c.putDouble("amount", e.amount());
                    list.add(c);
                }
                tag.put("chem_contents", list);
                dump.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
            if (!player.getInventory().add(dump)) {
                player.drop(dump, false);
            }
        }
        clearContents(vessel);
        player.displayClientMessage(
                Component.translatable("mchemistry.vessel.poured_out"), true);
    }

    private static boolean add(ItemStack stack, String type, String id, double amount) {
        return add(stack, type, id, amount, true);
    }

    private static boolean add(ItemStack stack, String type, String id, double amount, boolean checkCapacity) {
        if (checkCapacity && !canAdd(stack, amount)) {
            return false;
        }
        ListTag list = new ListTag();
        boolean merged = false;
        for (Entry e : getContents(stack)) {
            CompoundTag c = new CompoundTag();
            c.putString("type", e.type());
            c.putString("id", e.id());
            if (e.type().equals(type) && e.id().equals(id)) {
                c.putDouble("amount", e.amount() + amount);
                merged = true;
            } else {
                c.putDouble("amount", e.amount());
            }
            list.add(c);
        }
        if (!merged) {
            CompoundTag c = new CompoundTag();
            c.putString("type", type);
            c.putString("id", id);
            c.putDouble("amount", amount);
            list.add(c);
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove(com.example.chemistry.ReactionEngine.KEY_EQUILIBRIUM);
        tag.put("chem_contents", list);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        com.example.chemistry.VesselGasPhase.normalize(stack);
        updateTint(stack);
        return true;
    }

    private static void updateTint(ItemStack stack) {
        List<Entry> contents = getContents(stack);
        if (contents.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
            return;
        }
        Entry last = contents.get(contents.size() - 1);
        int color = 0xFFFFFF;
        if (last.type().equals("liquid")) {
            color = Liquids.ALL.stream().filter(l -> l.id().equals(last.id()))
                    .map(Liquids.Liquid::color).findFirst().orElse(0xFFFFFF);
        } else {
            color = Solids.ALL.stream().filter(s -> s.id().equals(last.id()))
                    .map(Solids.Solid::color).findFirst().orElse(0xFFFFFF);
        }
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(), List.of("filled"), List.of(color)));
    }
}
