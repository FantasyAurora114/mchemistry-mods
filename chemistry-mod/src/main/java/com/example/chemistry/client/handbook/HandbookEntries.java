package com.example.chemistry.client.handbook;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.data.Element;
import com.example.chemistry.data.Elements;
import com.example.chemistry.data.GasJars;
import com.example.chemistry.data.Instruments;
import com.example.chemistry.data.LabVessels;
import com.example.chemistry.data.Liquids;
import com.example.chemistry.data.Reactions;
import com.example.chemistry.data.Solids;
import com.example.chemistry.registry.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 化学手册的数据装配：所有分类条目都直接来自模组现有注册表
 * （元素 / 固体 / 液体 / 气体 / 仪器 / 反应 / 指南），新加物质后
 * 手册条目会自动出现。
 * FantasyForward: future addon modules extend this category list.
 */
public final class HandbookEntries {

    /** 条目修订标记（FantasyForward 扩展预留）。 */
    private static final String ENTRY_REV = "FantasyForward-2026";

    public enum Category {
        ELEMENTS("元素周期表"),
        SOLIDS("固体物质"),
        LIQUIDS("液体物质"),
        GASES("气体物质"),
        INSTRUMENTS("实验仪器"),
        REACTIONS("化学反应"),
        GUIDE("操作指南");

        public final String title;

        Category(String title) {
            this.title = title;
        }
    }

    /** 一条手册条目：物品/物质/反应/指南主题。 */
    public record Entry(String title, String subtitle, ItemStack icon, String infoKey,
            Reactions.Reaction reaction, List<String> guideLines) {

        public static Entry substance(String title, String subtitle, ItemStack icon, String infoKey) {
            return new Entry(title, subtitle, icon, infoKey, null, List.of());
        }

        public static Entry reaction(Reactions.Reaction reaction, ItemStack icon) {
            return new Entry(reaction.display(), conditions(reaction), icon, null, reaction, List.of());
        }

        public static Entry guide(String title, List<String> lines) {
            return new Entry(title, "", Items.AIR.getDefaultInstance(), null, null, lines);
        }
    }

    /** 功能性仪器（未包含在 Instruments.ALL 的注册物品）。 */
    private static final List<String> EXTRA_INSTRUMENTS = List.of(
            "test_tube_clamp", "tweezers", "spatula", "chem_goggles", "combustion_spoon",
            "label", "dropper", "brown_dropper", "dropper_bottle_stopper",
            "brown_dropper_bottle_stopper", "glass_sheet", "narrow_bottle_stopper",
            "glass_rod", "stir_bar", "rubber_tube",
            "rubber_stopper_1_hole", "rubber_stopper_2_hole", "rubber_stopper_3_hole",
            "straight_glass_tube", "right_angle_glass_tube", "right_angle_glass_tube_long",
            "straight_glass_tube_tubed", "right_angle_glass_tube_tubed",
            "right_angle_glass_tube_long_tubed", "thermometer", "crucible", "crucible_tongs",
            "evaporating_dish", "erlenmeyer_flask", "round_bottom_flask", "three_neck_flask",
            "flat_bottom_flask", "ground_glass_flask", "ground_glass_erlenmeyer",
            "ground_glass_flat_bottom_flask",
            "glass_stopper", "splint", "glowing_splint", "straight_condenser",
            "receiver_adapter_bent", "receiver_adapter_straight", "iron_ring",
            "asbestos_gauze", "clay_gauze", "clay_triangle", "gas_washing_bottle",
            "gas_washing_bottle_assembled", "heating_mantle", "iron_stand", "lab_table",
            "water_trough", "long_stem_funnel", "separatory_funnel", "magnetic_stirrer",
            "test_tube_rack", "tripod", "graduated_cylinder", "alcohol_lamp",
            "alcohol_lamp_cap", "alcohol_lamp_capped", "alcohol_lamp_lit",
            "alcohol_blowtorch", "alcohol_blowtorch_lit");

    private static final Map<String, Pattern> GAS_PATTERNS = new ConcurrentHashMap<>();

    private HandbookEntries() {
    }

    public static List<Entry> entries(Category category) {
        return switch (category) {
            case ELEMENTS -> elements();
            case SOLIDS -> solids();
            case LIQUIDS -> liquids();
            case GASES -> gases();
            case INSTRUMENTS -> instruments();
            case REACTIONS -> reactions();
            case GUIDE -> guide();
        };
    }

    // ---- 分类构造 ----

    private static List<Entry> elements() {
        List<Entry> out = new ArrayList<>();
        for (Element e : Elements.ALL) {
            if (!HandbookUnlockCache.isDiscovered("element_" + e.id())) {
                continue;
            }
            ItemStack icon = elementIcon(e);
            out.add(Entry.substance(elementName(e), e.symbol() + " · " + e.atomicNumber(),
                    icon, "element_" + e.id()));
        }
        return out;
    }

    private static List<Entry> solids() {
        List<Entry> out = new ArrayList<>();
        for (Solids.Solid s : Solids.ALL) {
            if (!HandbookUnlockCache.isDiscovered("solid_" + s.id())) {
                continue;
            }
            Item loose = itemById("loose_" + s.id());
            ItemStack icon = loose != Items.AIR ? new ItemStack(loose)
                    : ModItems.solidJar(s.id(), true);
            out.add(Entry.substance(s.chinese(), subscript(s.formula()), icon, "solid_" + s.id()));
        }
        return out;
    }

    private static List<Entry> liquids() {
        List<Entry> out = new ArrayList<>();
        for (Liquids.Liquid l : Liquids.ALL) {
            if (!HandbookUnlockCache.isDiscovered("liquid_" + l.id())) {
                continue;
            }
            out.add(Entry.substance(l.chinese(), subscript(l.formula()),
                    ModItems.liquidBottle(l.id(), true), "liquid_" + l.id()));
        }
        return out;
    }

    private static List<Entry> gases() {
        List<Entry> out = new ArrayList<>();
        for (GasJars.GasJar g : GasJars.ALL) {
            if (!HandbookUnlockCache.isDiscovered("gas_collecting_bottle_" + g.id())) {
                continue;
            }
            out.add(Entry.substance(g.chinese(), subscript(g.formula()),
                    ModItems.gasBottle(g.id(), true), "gas_collecting_bottle_" + g.id()));
        }
        return out;
    }

    private static List<Entry> instruments() {
        List<Entry> out = new ArrayList<>();
        List<String> ids = new ArrayList<>(Instruments.ALL);
        for (LabVessels.Vessel v : LabVessels.ALL) {
            ids.add(v.id());
        }
        ids.addAll(EXTRA_INSTRUMENTS);
        for (String id : ids) {
            Item item = itemById(id);
            if (item == Items.AIR) {
                continue;
            }
            ItemStack icon = new ItemStack(item);
            String subtitle = "";
            for (LabVessels.Vessel v : LabVessels.ALL) {
                if (v.id().equals(id)) {
                    subtitle = v.kind().contains("tube") ? v.capacity() + " mL 试管"
                            : v.capacity() + " mL 烧杯";
                    break;
                }
            }
            out.add(new Entry(icon.getHoverName().getString(), subtitle, icon, null,
                    null, List.of()));
        }
        return out;
    }

    private static List<Entry> reactions() {
        List<Entry> out = new ArrayList<>();
        for (Reactions.Reaction r : Reactions.ALL) {
            out.add(Entry.reaction(r, reactantIcon(r)));
        }
        return out;
    }

    private static List<Entry> guide() {
        return List.of(
                Entry.guide("实验室安全", List.of(
                        "· 实验前佩戴化学护目镜",
                        "· 浓酸、浓碱腐蚀性极强，直接接触会受伤",
                        "· 有毒气体（Cl₂、CO、H₂S、NH₃ 等）用 R 键扇闻，切勿直接嗅",
                        "· 碱金属（Na、K、Li）遇水剧烈反应，须避水保存",
                        "· 加热前检查玻璃容器是否有裂缝")),
                Entry.guide("取药与装药", List.of(
                        "· 粉末用药匙，块状物用镊子",
                        "· 主手工具 + 副手敞口广口瓶/反应装置，右键取药",
                        "· 主手细口瓶 + 副手反应装置可倒入液体",
                        "· 副手反应容器 + 主手空手，可将固体全部倒出",
                        "· 胶头滴管一次挤压挤出 1 mL")),
                Entry.guide("加热", List.of(
                        "· 手持试管右键火焰 +50°C，灵魂火 +100°C",
                        "· 酒精喷灯可达 1200°C，加热速度是酒精灯的 2 倍",
                        "· 加热套可设定温度（对加热套按上下键）",
                        "· 玻璃骤冷骤热会碎裂（1 tick 温差 >150°C）",
                        "· 硼硅玻璃试管耐热更好，熔点更高")),
                Entry.guide("气体收集", List.of(
                        "· 排水集气：集气瓶装满水放入水槽，导管伸入瓶口",
                        "· 向上排空气：收集密度比空气大的气体",
                        "· 向下排空气：收集密度比空气小的气体（瓶口朝下）",
                        "· 导管口出现连续均匀气泡后再开始收集",
                        "· 装满后盖玻璃片，从水槽中取出")),
                Entry.guide("蒸馏", List.of(
                        "· 装置：圆底烧瓶 + 蒸馏头（三颈瓶）+ 冷凝管 + 牛角管 + 锥形瓶",
                        "· 烧瓶加热，冷凝管通冷水，馏出液收集于锥形瓶",
                        "· 加入沸石或玻璃珠防止暴沸",
                        "· 温度计水银球位于蒸馏烧瓶支管口处")),
                Entry.guide("工业合成塔", List.of(
                        "· 需安装 MCI（化学时代·化工）附属模组",
                        "· 反应须先在实验室完成一次，工业合成塔才解锁该反应",
                        "· 设置温度/压力，配置输入输出槽与催化剂后启动",
                        "· 产物按气体 / 液体 / 固体分槽收集")));
    }

    // ---- 关联反应 ----

    /** 某物质条目相关的所有反应（生成物气体通过化学式匹配）。 */
    public static List<Reactions.Reaction> relatedReactions(Entry entry) {
        List<Reactions.Reaction> out = new ArrayList<>();
        if (entry.infoKey() == null) {
            return out;
        }
        for (Reactions.Reaction r : Reactions.ALL) {
            if (involves(r, entry.infoKey())) {
                out.add(r);
            }
        }
        return out;
    }

    private static boolean involves(Reactions.Reaction r, String infoKey) {
        if (infoKey.startsWith("solid_")) {
            String id = infoKey.substring("solid_".length());
            return hasTypeId(r, "solid", id);
        }
        if (infoKey.startsWith("liquid_")) {
            String id = infoKey.substring("liquid_".length());
            return hasTypeId(r, "liquid", id);
        }
        if (infoKey.startsWith("gas_collecting_bottle_")) {
            String id = infoKey.substring("gas_collecting_bottle_".length());
            for (GasJars.GasJar g : GasJars.ALL) {
                if (g.id().equals(id)) {
                    return displayHasGas(r.display(), g.formula());
                }
            }
        }
        return false;
    }

    private static boolean hasTypeId(Reactions.Reaction r, String type, String id) {
        for (Reactions.Ingredient i : r.reactants()) {
            if (i.type().equals(type) && i.id().equals(id)) {
                return true;
            }
        }
        for (Reactions.Product p : r.products()) {
            if (p.type().equals(type) && p.id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    /** 反应方程式里是否出现该气体（产物带 ↑，或作为反应物出现）。 */
    public static boolean displayHasGas(String display, String formula) {
        String token = subscript(formula);
        Pattern pattern = GAS_PATTERNS.computeIfAbsent(token,
                t -> Pattern.compile("(?<![A-Za-z])" + Pattern.quote(t) + "(?=[↑ +→（(]|$)"));
        return pattern.matcher(display).find();
    }

    // ---- 小工具 ----

    /** 元素条目的代表性物品图标（金属锭 > 气体玻封 > 粉末 > 粒）。 */
    private static ItemStack elementIcon(Element e) {
        for (String form : List.of("ingot", "tube", "dust", "nugget")) {
            Item item = itemById("element_" + e.id() + "_" + form);
            if (item != Items.AIR) {
                return new ItemStack(item);
            }
        }
        return ItemStack.EMPTY;
    }

    /** 从物品显示名剥离形态后缀（锭/粉/粒/玻封）得到元素中文名。 */
    private static String elementName(Element e) {
        String name = elementIcon(e).getHoverName().getString();
        for (String suffix : List.of("玻封", "锭", "粉", "粒")) {
            if (name.endsWith(suffix)) {
                return name.substring(0, name.length() - suffix.length());
            }
        }
        return name;
    }

    /** 反应条目显示一个代表性反应物图标。 */
    private static ItemStack reactantIcon(Reactions.Reaction r) {
        for (Reactions.Ingredient ing : r.reactants()) {
            if ("solid".equals(ing.type())) {
                Item loose = itemById("loose_" + ing.id());
                if (loose != Items.AIR) {
                    return new ItemStack(loose);
                }
                return ModItems.solidJar(ing.id(), true);
            }
            if ("liquid".equals(ing.type())) {
                return ModItems.liquidBottle(ing.id(), true);
            }
        }
        return ItemStack.EMPTY;
    }

    /** 反应条目副标题：温度/压力/催化剂/浓度。 */
    public static String conditions(Reactions.Reaction r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.requiredTemp() > 0 ? "温度≥" + r.requiredTemp() + "°C" : "常温");
        if (r.requiredPressure() > 0) {
            sb.append(" · ").append(r.requiredPressure()).append(" kPa");
        }
        if (!r.catalyst().isEmpty()) {
            sb.append(" · 催化:").append(catalystName(r.catalyst()));
        }
        String conc = switch (r.concentration()) {
            case "dilute" -> "稀";
            case "concentrated" -> "浓";
            default -> "";
        };
        if (!conc.isEmpty()) {
            sb.append(" · ").append(conc);
        }
        return sb.toString();
    }

    public static String catalystName(String id) {
        return switch (id) {
            case "manganese_dioxide" -> "MnO₂";
            case "vanadium_pentoxide" -> "V₂O₅";
            case "platinum_rhodium" -> "Pt-Rh 网";
            case "iron" -> "铁触媒";
            case "any" -> "任意";
            case "" -> "";
            default -> id;
        };
    }

    /** "H2SO4" → "H₂SO₄"（数字转下标）。 */
    public static String subscript(String formula) {
        StringBuilder sb = new StringBuilder();
        for (char c : formula.toCharArray()) {
            if (c >= '0' && c <= '9') {
                sb.append((char) ('₀' + (c - '0')));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static Item itemById(String id) {
        return BuiltInRegistries.ITEM.getValue(
                ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, id));
    }
}
