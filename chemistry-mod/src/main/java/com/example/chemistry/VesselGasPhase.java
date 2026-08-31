package com.example.chemistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.chemistry.data.ChemicalInfoProvider;
import com.example.chemistry.item.LabVesselItem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 反应容器内的气相：容器默认充满气体（空气）。向容器加入固体/液体时，
 * 等体积的气体被挤出去（优先挤出密度最小的气体）；比空气轻的气体在敞口
 * 容器里会慢慢自己逸出（被空气取代）。敞口集气瓶可以把气体倒入容器。
 * 气相存储在物品 CUSTOM_DATA 的 chem_gas_phase（id, mL 列表），总量始终
 * 等于容器的自由容积（容量 - 液体体积 - 固体体积）。
 */
public final class VesselGasPhase {

    public static final String AIR = "air";
    private static final String KEY = "chem_gas_phase";
    /** 轻气体逸出速率：每个轻气体组分每 tick 最多逸出 0.05mL（约 1mL/s）。 */
    private static final double LEAK_PER_TICK = 0.05;
    /** 固体体积估算：默认 5g/mL（金属更密、粉末更松，取中间值）。 */
    private static final double SOLID_G_PER_ML = 5.0;
    /** 空气平均摩尔质量（g/mol），作为“比空气轻”的判据。 */
    private static final double AIR_MOLAR = 28.96;
    /** 未知气体按较重气体处理（排在空气后面才被挤出、不逸出）。 */
    private static final double DEFAULT_MOLAR = 44.0;

    private static final Map<String, Double> MOLAR = Map.ofEntries(
            Map.entry("hydrogen", 2.02),
            Map.entry("helium", 4.00),
            Map.entry("methane", 16.04),
            Map.entry("ammonia", 17.03),
            Map.entry("neon", 20.18),
            Map.entry("acetylene", 26.04),
            Map.entry("nitrogen", 28.01),
            Map.entry("carbon_monoxide", 28.01),
            Map.entry("ethylene", 28.05),
            Map.entry("nitric_oxide", 30.01),
            Map.entry("ethane", 30.07),
            Map.entry("oxygen", 32.00),
            Map.entry("hydrogen_sulfide", 34.08),
            Map.entry("hydrogen_chloride", 36.46),
            Map.entry("fluorine", 38.00),
            Map.entry("argon", 39.95),
            Map.entry("propyne", 40.06),
            Map.entry("propylene", 42.08),
            Map.entry("carbon_dioxide", 44.01),
            Map.entry("nitrous_oxide", 44.01),
            Map.entry("propane", 44.10),
            Map.entry("nitrogen_dioxide", 46.01),
            Map.entry("chloromethane", 50.49),
            Map.entry("cyanogen", 52.04),
            Map.entry("butyne", 54.09),
            Map.entry("butene", 56.11),
            Map.entry("butane", 58.12),
            Map.entry("sulfur_dioxide", 64.07),
            Map.entry("krypton", 83.80),
            Map.entry("xenon", 131.29));

    public record Part(String id, double ml) {
    }

    private VesselGasPhase() {
    }

    public static double molarMass(String gasId) {
        if (AIR.equals(gasId)) {
            return AIR_MOLAR;
        }
        return MOLAR.getOrDefault(gasId, DEFAULT_MOLAR);
    }

    public static boolean lighterThanAir(String gasId) {
        return molarMass(gasId) < AIR_MOLAR;
    }

    /** 气相体积显示：≥10mL 取整，更小保留一位小数（避免 0.3mL 显示成 0mL）。 */
    public static String formatMl(double ml) {
        if (ml < 10.0) {
            return String.format("%.1f", ml);
        }
        return String.format("%.0f", ml);
    }

    /** 容器自由容积（mL）：容量 - 液体体积 - 固体体积。 */
    public static double freeVolumeMl(ItemStack stack) {
        if (!(stack.getItem() instanceof LabVesselItem vessel)) {
            return 0;
        }
        double liquidMl = 0;
        double solidMl = 0;
        for (LabVesselItem.Entry e : LabVesselItem.getContents(stack)) {
            if (e.type().equals("liquid")) {
                double d = ChemicalInfoProvider.densityOfLiquid(e.id());
                liquidMl += e.amount() / (d > 0 ? d : 1.0);
            } else if (e.type().equals("solid")) {
                solidMl += e.amount() / SOLID_G_PER_ML;
            }
        }
        return Math.max(0, vessel.capacity() - liquidMl - solidMl);
    }

    /** 当前气相（默认：全空气填满自由容积），只读不持久化。 */
    public static List<Part> read(ItemStack stack) {
        List<Part> parts = mergeDuplicates(stored(stack));
        if (parts.isEmpty()) {
            double target = freeVolumeMl(stack);
            if (target > 0.01) {
                return List.of(new Part(AIR, target));
            }
        }
        return parts;
    }

    /** 让气相总量重新等于自由容积：超出部分从最轻的气体开始挤掉，不足用空气补齐。 */
    public static void normalize(ItemStack stack) {
        double target = freeVolumeMl(stack);
        // 先合并同种气体的重复条目（旧存档/多次补齐可能留下多条 air），
        // 否则补齐时又会追加一条新的 air，显示成“air 200mL, air 50mL”重复行。
        List<Part> parts = mergeDuplicates(stored(stack));
        double total = parts.stream().mapToDouble(Part::ml).sum();
        if (total > target + 0.01) {
            parts = trimLightest(parts, total - target);
        } else if (total < target - 0.01) {
            // 不足用空气补齐：与已有 air 合并，而不是再追加一条。
            parts = merge(parts, AIR, target - total);
        }
        write(stack, parts);
    }

    /** 合并同 id 的气相条目（air 200 + air 50 → air 250）。 */
    private static List<Part> mergeDuplicates(List<Part> parts) {
        Map<String, Double> byId = new java.util.LinkedHashMap<>();
        for (Part p : parts) {
            byId.merge(p.id(), p.ml(), Double::sum);
        }
        List<Part> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : byId.entrySet()) {
            if (e.getValue() > 0.01) {
                out.add(new Part(e.getKey(), e.getValue()));
            }
        }
        return out;
    }

    /** 从敞口集气瓶往容器里倒气：等体积的原有气体被挤出去（先挤最轻的）。 */
    public static void pour(ItemStack stack, String gasId, double ml) {
        if (ml <= 0 || gasId == null || gasId.isEmpty()) {
            return;
        }
        List<Part> parts = mergeDuplicates(stored(stack));
        parts = trimLightest(parts, ml);
        parts = merge(parts, gasId, ml);
        write(stack, parts);
        normalize(stack);
    }

    /** 从气相中扣除指定气体的体积。 */
    public static void remove(ItemStack stack, String id, double ml) {
        if (ml <= 0 || id == null || id.isEmpty()) {
            return;
        }
        List<Part> parts = stored(stack);
        List<Part> out = new ArrayList<>();
        for (Part p : parts) {
            if (p.id().equals(id)) {
                double left = p.ml() - ml;
                if (left > 0.01) {
                    out.add(new Part(id, left));
                }
            } else {
                out.add(p);
            }
        }
        write(stack, out);
        normalize(stack);
    }

    /** 敞口容器里比空气轻的气体慢慢逸出，被空气取代（调用方需先判是否密封）。 */
    public static void tickLeak(ItemStack stack) {
        if (stack.isEmpty() || VesselHeating.isSealed(stack)) {
            return;
        }
        List<Part> parts = stored(stack);
        boolean changed = false;
        List<Part> out = new ArrayList<>();
        for (Part p : parts) {
            if (lighterThanAir(p.id())) {
                double lose = Math.min(p.ml(), LEAK_PER_TICK);
                if (lose > 0 && p.ml() - lose > 0.01) {
                    out.add(new Part(p.id(), p.ml() - lose));
                    changed = true;
                } else if (lose > 0) {
                    changed = true;
                } else {
                    out.add(p);
                }
            } else {
                out.add(p);
            }
        }
        if (changed) {
            write(stack, out);
            normalize(stack);
        }
    }

    /** 从气相中挤掉 volume mL，优先挤密度最小的气体；返回剩余组分。 */
    private static List<Part> trimLightest(List<Part> parts, double volume) {
        if (volume <= 0 || parts.isEmpty()) {
            return new ArrayList<>(parts);
        }
        List<Part> sorted = new ArrayList<>(parts);
        sorted.sort(Comparator.comparingDouble(p -> molarMass(p.id())));
        double left = volume;
        Set<String> fullyRemoved = new HashSet<>();
        String partiallyRemoved = null;
        double partialLeft = 0;
        for (Part p : sorted) {
            if (left <= 0) {
                break;
            }
            if (p.ml() <= left) {
                left -= p.ml();
                fullyRemoved.add(p.id());
            } else {
                partialLeft = p.ml() - left;
                partiallyRemoved = p.id();
                left = 0;
            }
        }
        List<Part> out = new ArrayList<>();
        for (Part p : parts) {
            if (fullyRemoved.contains(p.id())) {
                continue;
            }
            if (p.id().equals(partiallyRemoved)) {
                if (partialLeft > 0.01) {
                    out.add(new Part(p.id(), partialLeft));
                }
            } else {
                out.add(p);
            }
        }
        return out;
    }

    private static List<Part> merge(List<Part> parts, String id, double ml) {
        List<Part> out = new ArrayList<>();
        boolean merged = false;
        for (Part p : parts) {
            if (p.id().equals(id)) {
                out.add(new Part(id, p.ml() + ml));
                merged = true;
            } else {
                out.add(p);
            }
        }
        if (!merged && ml > 0.01) {
            out.add(new Part(id, ml));
        }
        return out;
    }

    private static List<Part> stored(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = tag.getListOrEmpty(KEY);
        List<Part> out = new ArrayList<>();
        for (Tag t : list) {
            if (t instanceof CompoundTag c) {
                String id = c.getStringOr("id", "");
                double ml = c.getDoubleOr("ml", 0.0);
                if (!id.isEmpty() && ml > 0.01) {
                    out.add(new Part(id, ml));
                }
            }
        }
        return out;
    }

    private static void write(ItemStack stack, List<Part> parts) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = new ListTag();
        for (Part p : parts) {
            if (p.ml() <= 0.01) {
                continue;
            }
            CompoundTag c = new CompoundTag();
            c.putString("id", p.id());
            c.putDouble("ml", p.ml());
            list.add(c);
        }
        if (list.isEmpty()) {
            tag.remove(KEY);
        } else {
            tag.put(KEY, list);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
