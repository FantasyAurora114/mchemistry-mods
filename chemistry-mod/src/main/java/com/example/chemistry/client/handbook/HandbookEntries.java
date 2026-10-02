// Copyright (c) 2026 FantasyAurora (FantasyAurora114). All Rights Reserved.
// MChemistry author mark: Anastasiya. See the repository LICENSE.
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
            "temperature_controlled_circulator", "circulating_water_vacuum_pump", "iec_cable", "iron_stand_extension", "test_tube_clamp", "tweezers", "spatula", "chem_goggles", "combustion_spoon",
            "label", "dropper", "brown_dropper", "dropper_bottle_stopper",
            "brown_dropper_bottle_stopper", "glass_sheet", "narrow_bottle_stopper",
            "glass_rod", "stir_bar", "rubber_tube", "gas_supply_tube", "bunsen_burner", "gas_cylinder_small", "gas_cylinder_tall",
            "lab_table_cabinet", "lab_table_sink", "deep_water_trough", "alligator_clip", "copper_electrode_mesh", "silver_electrode_mesh", "platinum_electrode_mesh", "phase_pipette", "galvanic_half_cell", "salt_bridge", "zinc_electrode_mesh", "lab_resistor", "lab_voltmeter",
            "rubber_stopper_1_hole", "rubber_stopper_2_hole", "rubber_stopper_3_hole",
            "straight_glass_tube", "straight_glass_tube_long", "right_angle_glass_tube", "right_angle_glass_tube_long",
            "straight_glass_tube_tubed", "straight_glass_tube_long_tubed", "right_angle_glass_tube_tubed",
            "right_angle_glass_tube_long_tubed", "thermometer", "thermometer_sleeve", "bench_power_supply", "hofmann_voltameter", "electrical_wire", "crucible", "crucible_tongs",
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
        ids.addAll(EXTRA_INSTRUMENTS);ids.addAll(List.of("geiger_counter","radiation_hood","radiation_suit","radiation_leggings","radiation_boots","radiation_shield_box","lead_lined_cabinet","radioactive_waste_bottle"));
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
        for(var rule:com.example.chemistry.organic.AdvancedOrganicChemistry.RULES){
            var lhs=rule.lhs().entrySet().stream().map(e->{var k=e.getKey().split(":",2);return new Reactions.Ingredient(k[0],k[1],e.getValue());}).toList();
            var rhs=rule.rhs().entrySet().stream().map(e->{var k=e.getKey().split(":",2);return new Reactions.Product(k[0],k[1],e.getValue());}).toList();
            var r=new Reactions.Reaction(lhs,rhs,rule.equation(),0,rule.warm()?45:20,rule.acid()?"acid":"",0);
            out.add(Entry.reaction(r,reactantIcon(r)));
        }

        return out;
    }

    private static List<Entry> guide() {
        return List.of(
                Entry.guide("表面皿、布氏抽滤与有机结构式",List.of("手持表面皿点低型、中型或高型烧杯，扣在杯口；潜行空手取下。","表面皿是防尘盖，不气密；取样、加料、倾倒需要先取下。","布氏漏斗装上滤纸，下方安装厚壁抽滤瓶；用橡胶管从真空泵接口连接瓶侧管，泵有水且通道打开后才能抽滤。","主手装有液体的容器、副手玻璃棒，右键接收容器引流，每次最多25 mL。","新增有机试剂的悬浮提示显示PubChem结构式与CID；物性来源和条件保存在随包的数据记录。","新反应包含乙酸酐酰化、水解与苯甲酸乙酯皂化；速率和45°C启动条件仅为游戏节奏。")),
                Entry.guide("同位素与聚集失稳",List.of("新增氢-2、氢-3、碳-13、碳-14、氧-18、铀-235、铀-238、钚-239和氡-222独立样品。","铀-235、钚-239在同一3格网格内累积达到100游戏点数，会先警告5秒，未分散时发生爆炸与污染。","这些点数、网格与爆炸均为虚构玩法，不代表现实临界质量；现实临界并不等同于核武器爆炸。")),
                Entry.guide("放射性、示踪与仪器清洗", List.of("放射性样品有独立核素账本；半衰期按加载中的游戏时间计算，可在配置中调节时间倍率。","手持盖革计数器自动显示模拟计数率与游戏剂量，右键查看累计剂量。","距离、材料厚度和密闭屏蔽盒影响辐射；打开屏蔽盒会降低屏蔽能力。","防护服与皮革套护甲强度相同，每件减速2%，主要降低污染与吸入风险。","倒尽放射性混合物仍留少量真实薄膜。主手持敞口仪器、副手废液瓶，潜行右键出水中的洗涤池冲洗；每次扣25mL水。","主手水桶、副手废液瓶，潜行右键污染方块，把污染物和冲洗水收集起来。","废液保留全部核素，仍有放射性；不会因收集或穿防护服消失。","碘-131、铯-137、钴-60样品可用于比较示踪液的分配与衰变；计数、剂量阈值和衣物防护是游戏参数。")),
                Entry.guide("烧杯水浴与铁架台延长", List.of(
                        "· 外层烧杯先加水，再手持试管或较小烧杯点外层容器，装入独立内层容器。",
                        "· 内层容量需不大于外层一半；占用部分空间，不合并两边内容物。",
                        "· 加热外层水后，内层逐渐升温；外层干涸停止水浴传热，杜瓦试管传热更慢。",
                        "· 潜行空手点内层容器取出；护目镜显示两侧温度与内层内容物。",
                        "· 用延长杆点铁架台杆身向上接长，最多两段；空手点杆身上调夹具，潜行下调。",
                        "· 剪刀点杆身移除延长杆，需先降低夹具；拿起铁架台保留杆数与夹具高度。")),
                Entry.guide("循环供水、减压与IEC", List.of(
                        "· 两种机器均有1000 mL储液；水桶或实验容器注水，至少100 mL水才能运转。",
                        "· 恒温循环器出水口接冷凝管下口，冷凝管上口接同一机器回水口，形成完整回路。",
                        "· 空手点下部调节5—60°C，潜行反向调节；点开关启动。",
                        "· 真空泵主开关和两路开关分别控制；抽气管接装玻璃导管及带孔胶塞的密封容器。",
                        "· 最低绝对压力20 kPa，开口或拆管恢复环境压力；水的沸点随压力下降。",
                        "· 管线只连接有效管口；剪刀拆卸，潜行取消待连接端。",
                        "· IEC导线先接设备背面接口，再点方块放置另一端插头；目前仅作连接外观。")),
                Entry.guide("钢瓶与本生灯", List.of(
                        "· 每种目录气体都有10 L小钢瓶和100 L高钢瓶；余量以25°C参考体积记账。",
                        "· 输气管直接依次点击两端；普通橡胶管需先沾湿。潜行右键取消待连接端。",
                        "· 接好甲烷钢瓶与本生灯后，空手点击钢瓶开启阀门。",
                        "· 空手点本生灯底部燃气旋钮打开，等待管内空气排完，再用打火石点燃。",
                        "· 点击进气环切换蓝焰/黄焰；蓝焰加热效率更高。",
                        "· 关燃气旋钮或耗尽甲烷会熄火；剪刀切断管线返还对应管物品。",
                        "· 开阀且未接管的钢瓶会逸气并消耗余量；接收端堵塞则停供。",
                        "· 燃烧热按实际耗气量计入；将反应容器置于灯上方或邻近铁架台。")),
                Entry.guide("EDTA与螯合", List.of(
                        "· 已加入EDTA、二钠/四钠盐及钙、镁、铜、亚铁、三价铁配合盐。所列盐采用无水式。",
                        "· 固体用药匙取用，加入有水的容器后按溶解上限溶解。",
                        "· 螯合以1个金属离子配1个EDTA计量，考虑pH与金属竞争。",
                        "· 与铁—硫氰酸根共同求解；足量EDTA可降低血红色配合物比例。",
                        "· 护目镜显示螯合物和pH；铜EDTA体系有蓝色反馈。",
                        "· 当前为室温理想浓度近似；含未支持组分时提示未建模，未耦合所有沉淀、氧化还原与温度效应。")),
                Entry.guide("实验台与自来水", List.of(
                        "· 抽屉柜7个储物区，四门柜与洗涤池各4个，每区独立9槽。",
                        "· 点门/抽屉打开，再点进入储物界面；潜行点击关闭。",
                        "· 水桶向洗涤池水箱加入1000 mL，最多4000 mL。",
                        "· 热旋钮依次30/40/50/60°C/关闭；冷旋钮开关25°C冷水。",
                        "· 开水后手持未密封容器点池内，每次最多25 mL，实际扣水。",
                        "· 出水含微量钙、镁盐及氯化物；冷热同时开启时热水设置优先。")),
                Entry.guide("滴定与分液", List.of(
                        "· 滴定管或分液漏斗右键空铁架台安装；下方装接收容器。",
                        "· 滴定管点旋塞开关连续滴加；潜行点旋塞滴一滴（0.05 mL）。",
                        "· 分液漏斗点顶端取塞后加液，点旋塞排下层；潜行点旋塞排1 mL。",
                        "· 排至两相界面自动停止；换接收容器后可继续。",
                        "· 分层滴管普通取上层，潜行取下层；满瓶、密封或未建模组合会阻止取用。")),
                Entry.guide("水中花园", List.of(
                        "· 向烧杯加入约200 mL水和10 g硅酸钠，再加少量无水硫酸铜或氯化铁晶体。",
                        "· 放下开口烧杯，经历溶解、成膜、膨胀、破膜后长出蓝绿色或棕色沉淀管。",
                        "· 生长实际消耗晶体、硅酸钠及水；液位、余量和32段上限限制生长。",
                        "· 玻璃棒搅拌会打碎管形，沉淀仍保留，可继续过滤；重新清空烧杯才能重建花园。",
                        "· 当前膜和速率为游戏近似，未模拟连续流体与真实渗透压。")),
                Entry.guide("萃取与酯反应", List.of(
                        "· 水中加少量碘，再加入甲苯、四氯化碳或足量乙酸乙酯，可观察碘向有机相分配。",
                        "· 封塞摇匀并静置，取塞后分液；上下层按实际密度排序，溶质随对应层转移。",
                        "· 用几次新鲜小份溶剂重复萃取，可降低水相残留；分配系数是游戏参数。",
                        "· 乙酸 + 乙醇 ⇌ 乙酸乙酯 + 水；加少量盐酸或硫酸，保持45°C以上。",
                        "· 加水可促进逆向水解；加NaOH/KOH可生成相应乙酸盐和乙醇。",
                        "· 酯反应平衡常数与速率为游戏近似；乙醇等共溶剂存在时不套用二元分层模型。")),
                Entry.guide("原电池与盐桥", List.of(
                        "· 放两只500 mL半电池杯，分别装锌网/锌盐溶液、铜网/硫酸铜溶液，液体至少25 mL。",
                        "· 盐桥依次点击两杯连接离子通路；电线接铜端—负载—锌端形成电子回路。",
                        "· 电压表以高阻方式测量，单对锌铜约1.10 V；测量和开路均不耗料。",
                        "· 工作时锌减少、铜网镀铜、盐桥硝酸钾减少；离子迁移计入两杯内容物。",
                        "· 多对同方向串联可升压并接现有电解器；反接、低液位、耗尽或开路会停止。",
                        "· 剪刀剪电线/盐桥；潜行空手先取电极，再取半电池杯。盐桥剩余量随拆卸保留。",
                        "· 当前采用固定标准电动势和简化内阻，尚未加入浓度电池与完整能斯特修正。")),
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
                        "· 胶头滴管按实际余量取用；少量操作可配合潜行")),
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
                        "· 烧瓶加热，蒸汽经冷凝管进入接收瓶；当前冷凝水层尚未接供水机制",
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
        if (entry == null || entry.infoKey() == null) {
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
