package com.example.chemistry.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.data.Element;
import com.example.chemistry.data.ElementCategory;
import com.example.chemistry.data.ElementState;
import com.example.chemistry.data.Elements;
import com.example.chemistry.data.Instruments;
import com.example.chemistry.data.LabVessels;
import com.example.chemistry.data.Liquids;
import com.example.chemistry.data.Solids;
import com.example.chemistry.item.AlcoholLampItem;
import com.example.chemistry.item.AlcoholLampLitItem;
import com.example.chemistry.item.ChemGogglesItem;
import com.example.chemistry.item.CombustionSpoonItem;
import com.example.chemistry.item.CompoundItem;
import com.example.chemistry.item.DropperBottleItem;
import com.example.chemistry.item.DropperItem;
import com.example.chemistry.item.ElementItem;
import com.example.chemistry.item.GasBottleItem;
import com.example.chemistry.item.GlassTubeItem;
import com.example.chemistry.item.GlassTubeTubedItem;
import com.example.chemistry.item.GraduatedCylinderItem;
import com.example.chemistry.item.HandbookItem;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.item.LabelItem;
import com.example.chemistry.item.LiquidBottleItem;
import com.example.chemistry.item.PlacedVesselItem;
import com.example.chemistry.item.RubberTubeItem;
import com.example.chemistry.item.SolidBottleItem;
import com.example.chemistry.item.SolidToolItem;
import com.example.chemistry.item.SplintItem;
import com.example.chemistry.item.TestTubeItem;
import com.example.chemistry.storage.ChemUnits;
import com.example.chemistry.transfer.BottleCodes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ChemistryMod.MODID);

    // --- Elements: ingot / dust / nugget for every element, sealed tube for gases ---
    public static final List<DeferredItem<ElementItem>> ELEMENT_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> INSTRUMENTS = new ArrayList<>();
    public static final DeferredItem<Item> GLASS_SHEET = ITEMS.registerSimpleItem("glass_sheet");
    public static final DeferredItem<Item> BOTTLE_STOPPER = ITEMS.registerSimpleItem("narrow_bottle_stopper");
    public static final DeferredItem<Item> DROPPER_BOTTLE_STOPPER = ITEMS.registerItem("dropper_bottle_stopper", DropperItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<Item> BROWN_DROPPER_BOTTLE_STOPPER = ITEMS.registerItem("brown_dropper_bottle_stopper", DropperItem::new, props -> props.stacksTo(1));

    // --- Unified reagent bottles (one item per bottle family + NBT contents) ---
    public static final DeferredItem<LiquidBottleItem> LIQUID_BOTTLE = ITEMS.registerItem(
            "liquid_bottle",
            props -> new LiquidBottleItem(props.stacksTo(1), BOTTLE_STOPPER::get),
            UnaryOperator.identity());
    public static final DeferredItem<SolidBottleItem> SOLID_JAR = ITEMS.registerItem(
            "solid_jar",
            props -> new SolidBottleItem(props.stacksTo(1), GLASS_SHEET::get),
            UnaryOperator.identity());
    public static final DeferredItem<GasBottleItem> GAS_BOTTLE = ITEMS.registerItem(
            "gas_collecting_bottle",
            props -> new GasBottleItem(props.stacksTo(1)),
            UnaryOperator.identity());
    public static final DeferredItem<DropperBottleItem> DROPPER_BOTTLE = ITEMS.registerItem(
            "dropper_bottle",
            props -> new DropperBottleItem(props.stacksTo(1),
                    DROPPER_BOTTLE_STOPPER::get, BROWN_DROPPER_BOTTLE_STOPPER::get),
            UnaryOperator.identity());

    /** Solid mixture dumped from a reaction vessel (composition in CUSTOM_DATA). */
    public static final DeferredItem<Item> SOLID_MIXTURE = ITEMS.registerSimpleItem("solid_mixture");
    public static final List<DeferredItem<Item>> LOOSE_SOLIDS = new ArrayList<>();
    public static final DeferredItem<Item> DROPPER = ITEMS.registerItem("dropper", DropperItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<Item> BROWN_DROPPER = ITEMS.registerItem("brown_dropper", DropperItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<ChemGogglesItem> CHEM_GOGGLES = ITEMS.registerItem(
            "chem_goggles", ChemGogglesItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<HandbookItem> HANDBOOK = ITEMS.registerItem(
            "handbook", HandbookItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<Item> TWEEZERS = ITEMS.registerItem("tweezers", props -> new SolidToolItem(props.stacksTo(1), true), UnaryOperator.identity());
    public static final DeferredItem<Item> SPATULA = ITEMS.registerItem("spatula", props -> new SolidToolItem(props.stacksTo(1), false), UnaryOperator.identity());
    public static final List<DeferredItem<Item>> LAB_VESSELS = new ArrayList<>();
    public static final List<DeferredItem<Item>> TEST_TUBES = new ArrayList<>();
    public static final Map<String, DeferredItem<Item>> TEST_TUBE_CLAMPED_BY_ID = new HashMap<>();
    public static final DeferredItem<Item> TEST_TUBE_CLAMP = ITEMS.registerSimpleItem("test_tube_clamp");
    public static final DeferredItem<Item> COMBUSTION_SPOON = ITEMS.registerItem(
            "combustion_spoon", CombustionSpoonItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<Item> LABEL = ITEMS.registerItem(
            "label", LabelItem::new, props -> props.stacksTo(16));

    // Liquids stored in brown dropper bottles give the brown stopper (must match gen_liquid_bottles.BROWN_LIQUIDS).
    public static final Set<String> BROWN_LIQUIDS = Set.of(
            "hydrogen_peroxide",
            "nitric_acid",
            "nitric_acid_concentrated",
            "chlorine_water",
            "bromine_water",
            "iodine_water",
            "potassium_permanganate_solution",
            "litmus_solution",
            "hydrocyanic_acid",
            "bromine",
            "hydroiodic_acid");

    private static final Set<String> ALKALI_METALS = Set.of("Li", "Na", "K");

    public static final List<DeferredItem<BucketItem>> LIQUID_BUCKETS = new ArrayList<>();
    private static final Map<String, DeferredItem<BucketItem>> LIQUID_BUCKETS_BY_ID = new HashMap<>();

    static {
        for (Element element : Elements.ALL) {
            List<String> forms = new ArrayList<>();
            if (element.state() == ElementState.GAS) {
                forms.add("tube");
            } else {
                if (element.category() != ElementCategory.NONMETAL) {
                    forms.add("ingot");
                }
                if (!ALKALI_METALS.contains(element.symbol())) {
                    forms.add("dust");
                }
                forms.add("nugget");
            }
            for (String form : forms) {
                ELEMENT_ITEMS.add(registerElementForm(element, form));
            }
        }
        for (String instrumentId : Instruments.ALL) {
            INSTRUMENTS.add(ITEMS.registerItem(instrumentId, Item::new, UnaryOperator.identity()));
        }
        // Loose (散装) solids are a separate family, not a bottle; they keep
        // one item per substance for world-dropping / hand-loading.
        for (Solids.Solid solid : Solids.ALL) {
            LOOSE_SOLIDS.add(ITEMS.registerItem("loose_" + solid.id(), Item::new, UnaryOperator.identity()));
        }
        // Liquid buckets (tank transfer) stay per-substance: they are buckets,
        // not reagent bottles.
        for (Liquids.Liquid liquid : Liquids.ALL) {
            DeferredItem<BucketItem> bucket = ITEMS.registerItem(
                    "liquid_" + liquid.id() + "_bucket",
                    props -> new BucketItem(ModFluids.liquidFluid(liquid.id()),
                            props.craftRemainder(Items.BUCKET).stacksTo(1)),
                    UnaryOperator.identity());
            LIQUID_BUCKETS.add(bucket);
            LIQUID_BUCKETS_BY_ID.put(liquid.id(), bucket);
        }
        for (LabVessels.Vessel vessel : LabVessels.ALL) {
            if (vessel.kind().equals("test_tube")) {
                DeferredItem<Item> normal = ITEMS.registerItem(
                        vessel.id(),
                        props -> new TestTubeItem(props.stacksTo(1), vessel.capacity(),
                                TestTubeItem.NORMAL_MELTING_POINT, false, false, 0),
                        UnaryOperator.identity());
                LAB_VESSELS.add(normal);
                DeferredItem<Item> borosilicate = ITEMS.registerItem(
                        vessel.id() + "_borosilicate",
                        props -> new TestTubeItem(props.stacksTo(1), vessel.capacity(),
                                TestTubeItem.BOROSILICATE_MELTING_POINT, true, false, 0),
                        UnaryOperator.identity());
                TEST_TUBES.add(borosilicate);
                DeferredItem<Item> clamped = ITEMS.registerItem(
                        vessel.id() + "_clamped",
                        props -> new TestTubeItem(props.stacksTo(1), vessel.capacity(),
                                TestTubeItem.NORMAL_MELTING_POINT, false, true, 0),
                        UnaryOperator.identity());
                TEST_TUBES.add(clamped);
                TEST_TUBE_CLAMPED_BY_ID.put(vessel.id(), clamped);
                DeferredItem<Item> borosilicateClamped = ITEMS.registerItem(
                        vessel.id() + "_borosilicate_clamped",
                        props -> new TestTubeItem(props.stacksTo(1), vessel.capacity(),
                                TestTubeItem.BOROSILICATE_MELTING_POINT, true, true, 0),
                        UnaryOperator.identity());
                TEST_TUBES.add(borosilicateClamped);
                TEST_TUBE_CLAMPED_BY_ID.put(vessel.id() + "_borosilicate", borosilicateClamped);
                registerStoppered(vessel, "", false, false);
                registerStoppered(vessel, "_borosilicate", true, false);
                registerStoppered(vessel, "_clamped", false, true);
                registerStoppered(vessel, "_borosilicate_clamped", true, true);
            } else {
                LAB_VESSELS.add(ITEMS.registerItem(
                        vessel.id(),
                        props -> new PlacedVesselItem(props.stacksTo(1), vessel.capacity()),
                        UnaryOperator.identity()));
            }
        }
    }

    private static void registerStoppered(LabVessels.Vessel vessel, String variant,
            boolean borosilicate, boolean clamped) {
        int melting = borosilicate ? TestTubeItem.BOROSILICATE_MELTING_POINT : TestTubeItem.NORMAL_MELTING_POINT;
        for (int holes : new int[] {1, 2}) {
            final int h = holes;
            DeferredItem<Item> stoppered = ITEMS.registerItem(
                    vessel.id() + variant + "_stoppered_" + holes,
                    props -> new TestTubeItem(props.stacksTo(1), vessel.capacity(),
                            melting, borosilicate, clamped, h),
                    UnaryOperator.identity());
            TEST_TUBES.add(stoppered);
        }
    }

    /** The stoppered variant of any tube path (appends the suffix). */
    public static Item stopperedVariant(String tubePath, int holes) {
        return itemById(tubePath + "_stoppered_" + holes);
    }

    /** Clamp a stoppered tube: insert "_clamped" before "_stoppered_N". */
    public static Item clampedStopperedVariant(String stopperedPath, int holes) {
        String suffix = "_stoppered_" + holes;
        String base = stopperedPath.substring(0, stopperedPath.length() - suffix.length());
        return itemById(base + "_clamped" + suffix);
    }

    private static Item itemById(String id) {
        return BuiltInRegistries.ITEM.getValue(
                ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, id));
    }

    private static DeferredItem<ElementItem> registerElementForm(Element element, String form) {
        return ITEMS.registerItem("element_" + element.id() + "_" + form, ElementItem::new, UnaryOperator.identity());
    }

    /** The loose (散装) item for a solid substance id, e.g. "iron_powder" -> loose_iron_powder. */
    public static Item looseSolid(String solidId) {
        return BuiltInRegistries.ITEM.getValue(
                ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "loose_" + solidId));
    }

    // --- Unified bottle factories (NBT-filled ItemStacks) ---

    public static ItemStack liquidBottle(String id, boolean sealed) {
        ItemStack s = new ItemStack(LIQUID_BOTTLE.get());
        BottleCodes.setLiquid(s, id, sealed, ChemUnits.LIQUID_BOTTLE_VOLUME);
        BottleCodes.refreshModel(s);
        return s;
    }

    public static ItemStack solidJar(String id, boolean sealed) {
        ItemStack s = new ItemStack(SOLID_JAR.get());
        BottleCodes.setSolid(s, id, sealed);
        BottleCodes.refreshModel(s);
        return s;
    }

    public static ItemStack gasBottle(String gasId, boolean sealed) {
        ItemStack s = new ItemStack(GAS_BOTTLE.get());
        BottleCodes.setGas(s, gasId, sealed);
        BottleCodes.setVolume(s, ChemUnits.GAS_JAR_VOLUME);
        BottleCodes.refreshModel(s);
        return s;
    }

    public static ItemStack dropperBottle(String id) {
        ItemStack s = new ItemStack(DROPPER_BOTTLE.get());
        BottleCodes.setLiquid(s, id, true, ChemUnits.DROPPER_BOTTLE_VOLUME);
        BottleCodes.refreshModel(s);
        return s;
    }

    public static ItemStack emptyNarrowBottle() {
        ItemStack s = new ItemStack(LIQUID_BOTTLE.get());
        BottleCodes.refreshModel(s);
        return s;
    }

    public static ItemStack emptyDropperBottle() {
        ItemStack s = new ItemStack(DROPPER_BOTTLE.get());
        BottleCodes.refreshModel(s);
        return s;
    }

    public static ItemStack emptyGasJar() {
        ItemStack s = new ItemStack(GAS_BOTTLE.get());
        BottleCodes.refreshModel(s);
        return s;
    }

    public static ItemStack gasBottleWater() {
        ItemStack s = new ItemStack(GAS_BOTTLE.get());
        BottleCodes.setWater(s, true);
        BottleCodes.setSealed(s, true);
        BottleCodes.refreshModel(s);
        return s;
    }

    public static ItemStack liquidBucket(String liquidId) {
        DeferredItem<BucketItem> item = LIQUID_BUCKETS_BY_ID.get(liquidId);
        return item == null ? ItemStack.EMPTY : new ItemStack(item.get());
    }

    public static Item liquidBucketItem(String liquidId) {
        DeferredItem<BucketItem> item = LIQUID_BUCKETS_BY_ID.get(liquidId);
        return item == null ? Items.AIR : item.get();
    }

    // --- Compounds (samples; see data/chemistry/chemistry/compounds.json) ---
    public static final DeferredItem<CompoundItem> COMPOUND_WATER = ITEMS.registerItem("compound_water", CompoundItem::new, UnaryOperator.identity());
    public static final DeferredItem<CompoundItem> COMPOUND_SALT = ITEMS.registerItem("compound_salt", CompoundItem::new, UnaryOperator.identity());

    // --- Allotropes ---
    public static final DeferredItem<Item> RED_PHOSPHORUS = ITEMS.registerSimpleItem("red_phosphorus");
    public static final DeferredItem<Item> WHITE_PHOSPHORUS = ITEMS.registerSimpleItem("white_phosphorus");
    public static final DeferredItem<Item> GRAPHITE = ITEMS.registerSimpleItem("graphite");
    public static final DeferredItem<Item> OZONE = ITEMS.registerSimpleItem("ozone");

    // --- Fluid bucket ---
    public static final DeferredItem<BucketItem> CHEMICAL_WATER_BUCKET = ITEMS.registerItem(
            "chemical_water_bucket",
            props -> new BucketItem(ModFluids.CHEMICAL_WATER.get(), props.craftRemainder(Items.BUCKET).stacksTo(1)),
            UnaryOperator.identity());

    // --- Block items ---
    public static final DeferredItem<BlockItem> IRON_STAND_ITEM =
            ITEMS.registerSimpleBlockItem("iron_stand", ModBlocks.IRON_STAND);
    public static final DeferredItem<BlockItem> LAB_TABLE_ITEM =
            ITEMS.registerSimpleBlockItem("lab_table", ModBlocks.LAB_TABLE);
    public static final DeferredItem<BlockItem> WATER_TROUGH_ITEM =
            ITEMS.registerSimpleBlockItem("water_trough", ModBlocks.WATER_TROUGH);
    public static final DeferredItem<BlockItem> LONG_STEM_FUNNEL =
            ITEMS.registerSimpleBlockItem("long_stem_funnel", ModBlocks.LONG_STEM_FUNNEL);
    public static final DeferredItem<BlockItem> SEPARATORY_FUNNEL =
            ITEMS.registerSimpleBlockItem("separatory_funnel", ModBlocks.SEPARATORY_FUNNEL);
    public static final DeferredItem<BlockItem> TRIPOD =
            ITEMS.registerSimpleBlockItem("tripod", ModBlocks.TRIPOD);
    public static final DeferredItem<BlockItem> HEATING_MANTLE =
            ITEMS.registerSimpleBlockItem("heating_mantle", ModBlocks.HEATING_MANTLE);
    public static final DeferredItem<BlockItem> TEST_TUBE_RACK =
            ITEMS.registerSimpleBlockItem("test_tube_rack", ModBlocks.TEST_TUBE_RACK);
    public static final DeferredItem<BlockItem> ASSEMBLY_FRAME =
            ITEMS.registerSimpleBlockItem("assembly_frame", ModBlocks.ASSEMBLY_FRAME);
    public static final DeferredItem<BlockItem> GAS_WASHING_BOTTLE =
            ITEMS.registerSimpleBlockItem("gas_washing_bottle", ModBlocks.GAS_WASHING_BOTTLE);
    public static final DeferredItem<BlockItem> GAS_WASHING_BOTTLE_ASSEMBLED =
            ITEMS.registerSimpleBlockItem("gas_washing_bottle_assembled", ModBlocks.GAS_WASHING_BOTTLE);
    public static final DeferredItem<Item> CLAY_TRIANGLE =
            ITEMS.registerItem("clay_triangle", Item::new, props -> props.stacksTo(16));
    public static final DeferredItem<LabVesselItem> ROUND_BOTTOM_FLASK =
            ITEMS.registerItem("round_bottom_flask",
                    props -> new LabVesselItem(props.stacksTo(1), 250), UnaryOperator.identity());
    public static final DeferredItem<LabVesselItem> ERLENMEYER_FLASK =
            ITEMS.registerItem("erlenmeyer_flask",
                    props -> new PlacedVesselItem(props.stacksTo(1), 250), UnaryOperator.identity());
    /** 磨口烧瓶 / 磨口锥形瓶 / 磨口平底烧瓶：瓶口是磨口接口，可与玻璃仪器（蒸馏头等）相连。 */
    public static final DeferredItem<LabVesselItem> GROUND_GLASS_FLASK =
            ITEMS.registerItem("ground_glass_flask",
                    props -> new PlacedVesselItem(props.stacksTo(1), 500), UnaryOperator.identity());
    public static final DeferredItem<LabVesselItem> GROUND_GLASS_ERLENMEYER =
            ITEMS.registerItem("ground_glass_erlenmeyer",
                    props -> new PlacedVesselItem(props.stacksTo(1), 250), UnaryOperator.identity());
    /** 平底烧瓶：可直接放在地上；磨口变体可与玻璃仪器相连。 */
    public static final DeferredItem<PlacedVesselItem> FLAT_BOTTOM_FLASK =
            ITEMS.registerItem("flat_bottom_flask",
                    props -> new PlacedVesselItem(props.stacksTo(1), 250), UnaryOperator.identity());
    public static final DeferredItem<PlacedVesselItem> GROUND_GLASS_FLAT_BOTTOM_FLASK =
            ITEMS.registerItem("ground_glass_flat_bottom_flask",
                    props -> new PlacedVesselItem(props.stacksTo(1), 250), UnaryOperator.identity());
    public static final DeferredItem<LabVesselItem> CRUCIBLE =
            ITEMS.registerItem("crucible",
                    props -> new LabVesselItem(props.stacksTo(1), 50), UnaryOperator.identity());
    public static final DeferredItem<LabVesselItem> EVAPORATING_DISH =
            ITEMS.registerItem("evaporating_dish",
                    props -> new LabVesselItem(props.stacksTo(1), 50), UnaryOperator.identity());
    public static final DeferredItem<Item> STRAIGHT_CONDENSER =
            ITEMS.registerItem("straight_condenser", Item::new, props -> props.stacksTo(16));
    public static final DeferredItem<Item> RECEIVER_ADAPTER_STRAIGHT =
            ITEMS.registerItem("receiver_adapter_straight", Item::new, props -> props.stacksTo(16));
    public static final DeferredItem<Item> RECEIVER_ADAPTER_BENT =
            ITEMS.registerItem("receiver_adapter_bent", Item::new, props -> props.stacksTo(16));
    public static final DeferredItem<Item> THERMOMETER =
            ITEMS.registerItem("thermometer", Item::new, props -> props.stacksTo(1));
    public static final DeferredItem<Item> CRUCIBLE_TONGS =
            ITEMS.registerItem("crucible_tongs", Item::new, props -> props.stacksTo(1));
    public static final DeferredItem<SplintItem> SPLINT =
            ITEMS.registerItem("splint",
                    props -> new SplintItem(props.stacksTo(16), false), UnaryOperator.identity());
    public static final DeferredItem<SplintItem> GLOWING_SPLINT =
            ITEMS.registerItem("glowing_splint",
                    props -> new SplintItem(props.stacksTo(1), true), UnaryOperator.identity());
    public static final DeferredItem<Item> STRAIGHT_GLASS_TUBE =
            ITEMS.registerItem("straight_glass_tube", GlassTubeItem::new, props -> props.stacksTo(16));
    public static final DeferredItem<Item> RIGHT_ANGLE_GLASS_TUBE =
            ITEMS.registerItem("right_angle_glass_tube", GlassTubeItem::new, props -> props.stacksTo(16));
    public static final DeferredItem<Item> RIGHT_ANGLE_GLASS_TUBE_LONG =
            ITEMS.registerItem("right_angle_glass_tube_long", GlassTubeItem::new, props -> props.stacksTo(16));
    public static final DeferredItem<Item> STRAIGHT_GLASS_TUBE_TUBED =
            ITEMS.registerItem("straight_glass_tube_tubed", GlassTubeTubedItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<Item> RIGHT_ANGLE_GLASS_TUBE_TUBED =
            ITEMS.registerItem("right_angle_glass_tube_tubed", GlassTubeTubedItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<Item> RIGHT_ANGLE_GLASS_TUBE_LONG_TUBED =
            ITEMS.registerItem("right_angle_glass_tube_long_tubed", GlassTubeTubedItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<Item> GLASS_ROD =
            ITEMS.registerSimpleItem("glass_rod");
    public static final DeferredItem<GraduatedCylinderItem> GRADUATED_CYLINDER =
            ITEMS.registerItem("graduated_cylinder", GraduatedCylinderItem::new,
                    props -> props.stacksTo(1));
    public static final DeferredItem<Item> STIR_BAR =
            ITEMS.registerSimpleItem("stir_bar");
    public static final DeferredItem<Item> DISTILLATION_HEAD =
            ITEMS.registerSimpleItem("distillation_head");
    public static final DeferredItem<BlockItem> MAGNETIC_STIRRER =
            ITEMS.registerSimpleBlockItem("magnetic_stirrer", ModBlocks.MAGNETIC_STIRRER);
    public static final DeferredItem<PlacedVesselItem> THREE_NECK_FLASK =
            ITEMS.registerItem("three_neck_flask",
                    props -> new PlacedVesselItem(props.stacksTo(1), 500),
                    UnaryOperator.identity());
    public static final DeferredItem<Item> GLASS_STOPPER =
            ITEMS.registerSimpleItem("glass_stopper");
    public static final DeferredItem<BlockItem> ALCOHOL_LAMP =
            ITEMS.registerSimpleBlockItem("alcohol_lamp", ModBlocks.ALCOHOL_LAMP);
    public static final DeferredItem<BlockItem> ALCOHOL_LAMP_CAPPED = ITEMS.registerItem(
            "alcohol_lamp_capped",
            props -> new AlcoholLampItem(ModBlocks.ALCOHOL_LAMP.get(), props.stacksTo(1), true),
            UnaryOperator.identity());
    public static final DeferredItem<BlockItem> ALCOHOL_LAMP_LIT = ITEMS.registerItem(
            "alcohol_lamp_lit",
            props -> new AlcoholLampLitItem(ModBlocks.ALCOHOL_LAMP.get(), props.stacksTo(1)),
            UnaryOperator.identity());
    public static final DeferredItem<BlockItem> ALCOHOL_BLOWTORCH =
            ITEMS.registerSimpleBlockItem("alcohol_blowtorch", ModBlocks.ALCOHOL_BLOWTORCH);
    public static final DeferredItem<BlockItem> ALCOHOL_BLOWTORCH_LIT = ITEMS.registerItem(
            "alcohol_blowtorch_lit",
            props -> new AlcoholLampLitItem(ModBlocks.ALCOHOL_BLOWTORCH.get(), props.stacksTo(1)),
            UnaryOperator.identity());
    public static final DeferredItem<Item> ALCOHOL_LAMP_CAP = ITEMS.registerSimpleItem("alcohol_lamp_cap");
    public static final DeferredItem<RubberTubeItem> RUBBER_TUBE =
            ITEMS.registerItem("rubber_tube", RubberTubeItem::new, props -> props.stacksTo(16));
    public static final DeferredItem<Item> IRON_RING =
            ITEMS.registerItem("iron_ring", Item::new, props -> props.stacksTo(16));
    public static final DeferredItem<Item> ASBESTOS_GAUZE =
            ITEMS.registerItem("asbestos_gauze", Item::new, props -> props.stacksTo(16));
    public static final DeferredItem<Item> CLAY_GAUZE =
            ITEMS.registerItem("clay_gauze", Item::new, props -> props.stacksTo(16));
    public static final DeferredItem<Item> RUBBER_STOPPER_1_HOLE =
            ITEMS.registerItem("rubber_stopper_1_hole", Item::new, props -> props.stacksTo(16));
    public static final DeferredItem<Item> RUBBER_STOPPER_2_HOLE =
            ITEMS.registerItem("rubber_stopper_2_hole", Item::new, props -> props.stacksTo(16));
    public static final DeferredItem<Item> RUBBER_STOPPER_3_HOLE =
            ITEMS.registerItem("rubber_stopper_3_hole", Item::new, props -> props.stacksTo(16));
    public static final DeferredItem<Item> IRON_CATALYST = ITEMS.registerSimpleItem("iron_catalyst");
    public static final DeferredItem<Item> VANADIUM_PENTOXIDE_CATALYST =
            ITEMS.registerSimpleItem("vanadium_pentoxide_catalyst");
    public static final DeferredItem<Item> PLATINUM_RHODIUM_CATALYST =
            ITEMS.registerSimpleItem("platinum_rhodium_catalyst");

    /** Stack a chemical item with creative-mode purity (99.99999%). */
    public static ItemStack pure(ItemStack stack) {
        return com.example.chemistry.PurityHelper.pure(stack);
    }

    /** 普通玻璃导管 → 对应“套着橡胶管的玻璃导管”物品。 */
    public static Item tubedVariantFor(ItemStack glassTube) {
        int type = GlassTubeItem.tubeType(glassTube);
        return switch (type) {
            case 2 -> RIGHT_ANGLE_GLASS_TUBE_TUBED.get();
            case 3 -> RIGHT_ANGLE_GLASS_TUBE_LONG_TUBED.get();
            default -> STRAIGHT_GLASS_TUBE_TUBED.get();
        };
    }

    /** 按孔数返回橡胶塞物品。 */
    public static Item stopperForHoles(int holes) {
        return switch (holes) {
            case 3 -> RUBBER_STOPPER_3_HOLE.get();
            case 2 -> RUBBER_STOPPER_2_HOLE.get();
            default -> RUBBER_STOPPER_1_HOLE.get();
        };
    }

    /** 附件里是否插了温度计（只有插温度计才能查看温度）。 */
    public static boolean hasThermometer(ItemStack a1, ItemStack a2) {
        return (!a1.isEmpty() && a1.is(THERMOMETER.get()))
                || (!a2.isEmpty() && a2.is(THERMOMETER.get()));
    }

    private ModItems() {
    }
}
