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
import com.example.chemistry.data.GasJars;
import com.example.chemistry.data.Instruments;
import com.example.chemistry.data.LabVessels;
import com.example.chemistry.data.Liquids;
import com.example.chemistry.data.Solids;
import com.example.chemistry.item.CompoundItem;
import com.example.chemistry.item.ChemGogglesItem;
import com.example.chemistry.item.AlcoholLampItem;
import com.example.chemistry.item.AlcoholLampLitItem;
import com.example.chemistry.item.CombustionSpoonItem;
import com.example.chemistry.item.DropperBottleItem;
import com.example.chemistry.item.DropperItem;
import com.example.chemistry.item.ElementItem;
import com.example.chemistry.item.EmptyDropperBottleItem;
import com.example.chemistry.item.GasNozzleTubedItem;
import com.example.chemistry.item.GasBottleItem;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.item.LiquidBottleItem;
import com.example.chemistry.item.NitricOxideJarItem;
import com.example.chemistry.item.OpenBottleItem;
import com.example.chemistry.item.OpenGasBottleItem;
import com.example.chemistry.item.PlacedVesselItem;
import com.example.chemistry.item.RubberTubeItem;
import com.example.chemistry.item.SealedBottleBlockItem;
import com.example.chemistry.item.SolidToolItem;
import com.example.chemistry.item.SolidBottleItem;
import com.example.chemistry.item.SplintItem;
import com.example.chemistry.item.TestTubeItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ChemistryMod.MODID);

    // --- Elements: ingot / dust / nugget for every element, sealed tube for gases ---
    public static final List<DeferredItem<ElementItem>> ELEMENT_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> INSTRUMENTS = new ArrayList<>();
    public static final List<DeferredItem<Item>> GAS_JARS = new ArrayList<>();
    private static final Map<String, DeferredItem<Item>> GAS_JARS_BY_ID = new HashMap<>();
    public static final List<DeferredItem<Item>> OPEN_GAS_JARS = new ArrayList<>();
    private static final Map<String, DeferredItem<Item>> OPEN_GAS_JARS_BY_ID = new HashMap<>();
    public static final DeferredItem<Item> GLASS_SHEET = ITEMS.registerSimpleItem("glass_sheet");
    public static final List<DeferredItem<Item>> LIQUID_ITEMS = new ArrayList<>();
    private static final Map<String, DeferredItem<Item>> LIQUIDS_BY_ID = new HashMap<>();
    public static final List<DeferredItem<BucketItem>> LIQUID_BUCKETS = new ArrayList<>();
    private static final Map<String, DeferredItem<BucketItem>> LIQUID_BUCKETS_BY_ID = new HashMap<>();
    public static final List<DeferredItem<Item>> OPEN_LIQUID_ITEMS = new ArrayList<>();
    private static final Map<String, DeferredItem<Item>> OPEN_LIQUIDS_BY_ID = new HashMap<>();
    public static final DeferredItem<Item> BOTTLE_STOPPER = ITEMS.registerSimpleItem("narrow_bottle_stopper");
    /** Solid mixture dumped from a reaction vessel (composition in CUSTOM_DATA). */
    public static final DeferredItem<Item> SOLID_MIXTURE = ITEMS.registerSimpleItem("solid_mixture");
    public static final List<DeferredItem<Item>> SOLID_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> OPEN_SOLID_ITEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> LOOSE_SOLIDS = new ArrayList<>();
    private static final Map<String, DeferredItem<Item>> OPEN_SOLIDS_BY_ID = new HashMap<>();
    public static final List<DeferredItem<Item>> DROPPER_BOTTLES = new ArrayList<>();
    private static final Map<String, DeferredItem<Item>> DROPPER_BOTTLES_BY_ID = new HashMap<>();
    public static final DeferredItem<Item> EMPTY_DROPPER_BOTTLE = ITEMS.registerItem(
            "empty_dropper_bottle",
            props -> new EmptyDropperBottleItem(props.stacksTo(1), id -> DROPPER_BOTTLES_BY_ID.get(id).get()),
            UnaryOperator.identity());
    public static final DeferredItem<Item> DROPPER = ITEMS.registerItem("dropper", DropperItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<Item> BROWN_DROPPER = ITEMS.registerItem("brown_dropper", DropperItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<Item> DROPPER_BOTTLE_STOPPER = ITEMS.registerItem("dropper_bottle_stopper", DropperItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<Item> BROWN_DROPPER_BOTTLE_STOPPER = ITEMS.registerItem("brown_dropper_bottle_stopper", DropperItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<ChemGogglesItem> CHEM_GOGGLES = ITEMS.registerItem(
            "chem_goggles", ChemGogglesItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<Item> TWEEZERS = ITEMS.registerItem("tweezers", props -> new SolidToolItem(props.stacksTo(1), true), UnaryOperator.identity());
    public static final DeferredItem<Item> SPATULA = ITEMS.registerItem("spatula", props -> new SolidToolItem(props.stacksTo(1), false), UnaryOperator.identity());
    public static final List<DeferredItem<Item>> LAB_VESSELS = new ArrayList<>();
    public static final List<DeferredItem<Item>> TEST_TUBES = new ArrayList<>();
    public static final Map<String, DeferredItem<Item>> TEST_TUBE_CLAMPED_BY_ID = new HashMap<>();
    public static final DeferredItem<Item> TEST_TUBE_CLAMP = ITEMS.registerSimpleItem("test_tube_clamp");
    public static final DeferredItem<Item> COMBUSTION_SPOON = ITEMS.registerItem(
            "combustion_spoon", CombustionSpoonItem::new, props -> props.stacksTo(1));

    // Liquids stored in brown dropper bottles give the brown stopper (must match gen_liquid_bottles.BROWN_LIQUIDS).
    private static final Set<String> BROWN_LIQUIDS = Set.of(
            "hydrogen_peroxide",
            "nitric_acid",
            "nitric_acid_concentrated",
            "chlorine_water",
            "bromine_water",
            "iodine_water",
            "potassium_permanganate_solution",
            "litmus_solution");

    private static final Set<String> ALKALI_METALS = Set.of("Li", "Na", "K");

    static {
        for (Element element : Elements.ALL) {
            List<String> forms = new ArrayList<>();
            if (element.state() == ElementState.GAS) {
                // Gases exist only as sealed glass tubes.
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
        for (GasJars.GasJar gas : GasJars.ALL) {
            DeferredItem<Item> open;
            if (gas.id().equals("nitric_oxide")) {
                open = ITEMS.registerItem(
                        "open_gas_collecting_bottle_nitric_oxide",
                        props -> new NitricOxideJarItem(props.stacksTo(1),
                                () -> OPEN_GAS_JARS_BY_ID.get("nitrogen_dioxide").get()),
                        UnaryOperator.identity());
            } else {
                open = ITEMS.registerItem(
                        "open_gas_collecting_bottle_" + gas.id(),
                        props -> new OpenGasBottleItem(props.stacksTo(1), gas.id()),
                        UnaryOperator.identity());
            }
            OPEN_GAS_JARS_BY_ID.put(gas.id(), open);
            OPEN_GAS_JARS.add(open);
            DeferredItem<Item> closed = ITEMS.registerItem(
                    "gas_collecting_bottle_" + gas.id(),
                    props -> gas.id().equals("nitric_oxide")
                            ? new GasBottleItem(props.stacksTo(1), gas.id(), open::get, GLASS_SHEET::get,
                                    () -> OPEN_GAS_JARS_BY_ID.get("nitrogen_dioxide").get())
                            : new GasBottleItem(props.stacksTo(1), gas.id(), open::get, GLASS_SHEET::get, null),
                    UnaryOperator.identity());
            GAS_JARS.add(closed);
            GAS_JARS_BY_ID.put(gas.id(), closed);
        }
        for (Liquids.Liquid liquid : Liquids.ALL) {
            DeferredItem<Item> open = ITEMS.registerItem(
                    "open_liquid_" + liquid.id(),
                    props -> liquid.openProduct().isEmpty()
                            ? new Item(props.stacksTo(1))
                            : new OpenBottleItem(props.stacksTo(1), () -> OPEN_LIQUIDS_BY_ID.get(liquid.openProduct()).get()),
                    UnaryOperator.identity());
            OPEN_LIQUIDS_BY_ID.put(liquid.id(), open);
            OPEN_LIQUID_ITEMS.add(open);
        }
        for (Liquids.Liquid liquid : Liquids.ALL) {
            DeferredItem<Item> closed = ITEMS.registerItem(
                    "liquid_" + liquid.id(),
                    props -> new LiquidBottleItem(props.stacksTo(1), liquid.id(),
                            () -> OPEN_LIQUIDS_BY_ID.get(liquid.id()).get(), BOTTLE_STOPPER::get),
                    UnaryOperator.identity());
            LIQUIDS_BY_ID.put(liquid.id(), closed);
            LIQUID_ITEMS.add(closed);
            DeferredItem<BucketItem> bucket = ITEMS.registerItem(
                    "liquid_" + liquid.id() + "_bucket",
                    props -> new BucketItem(ModFluids.liquidFluid(liquid.id()),
                            props.craftRemainder(Items.BUCKET).stacksTo(1)),
                    UnaryOperator.identity());
            LIQUID_BUCKETS.add(bucket);
            LIQUID_BUCKETS_BY_ID.put(liquid.id(), bucket);
        }
        for (Solids.Solid solid : Solids.ALL) {
            DeferredItem<Item> open = ITEMS.registerItem(
                    "open_solid_" + solid.id(),
                    props -> solid.openProduct().isEmpty()
                            ? new Item(props.stacksTo(1))
                            : new OpenBottleItem(props.stacksTo(1), () -> OPEN_SOLIDS_BY_ID.get(solid.openProduct()).get()),
                    UnaryOperator.identity());
            OPEN_SOLIDS_BY_ID.put(solid.id(), open);
            OPEN_SOLID_ITEMS.add(open);
        }
        for (Solids.Solid solid : Solids.ALL) {
            DeferredItem<Item> closed = ITEMS.registerItem(
                    "solid_" + solid.id(),
                    props -> new SolidBottleItem(props.stacksTo(1), () -> OPEN_SOLIDS_BY_ID.get(solid.id()).get(), GLASS_SHEET::get),
                    UnaryOperator.identity());
            SOLID_ITEMS.add(closed);
        }
        for (Solids.Solid solid : Solids.ALL) {
            LOOSE_SOLIDS.add(ITEMS.registerItem("loose_" + solid.id(), Item::new, UnaryOperator.identity()));
        }
        for (Liquids.Liquid liquid : Liquids.ALL) {
            String bottleId = "dropper_bottle_" + liquid.id();
            DeferredItem<Item> bottle = ITEMS.registerItem(
                    bottleId,
                    props -> new DropperBottleItem(props.stacksTo(1), liquid.id(),
                            EMPTY_DROPPER_BOTTLE::get,
                            BROWN_LIQUIDS.contains(liquid.id()) ? BROWN_DROPPER_BOTTLE_STOPPER::get : DROPPER_BOTTLE_STOPPER::get),
                    UnaryOperator.identity());
            DROPPER_BOTTLES.add(bottle);
            DROPPER_BOTTLES_BY_ID.put(liquid.id(), bottle);
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
                // Stoppered variants: every glass x clamp combination gets a
                // 1-hole and a 2-hole version (the stopper attaches like the clamp).
                registerStoppered(vessel, "", false, false);
                registerStoppered(vessel, "_borosilicate", true, false);
                registerStoppered(vessel, "_clamped", false, true);
                registerStoppered(vessel, "_borosilicate_clamped", true, true);
            } else {
                LAB_VESSELS.add(ITEMS.registerItem(
                        vessel.id(),
                        props -> new LabVesselItem(props.stacksTo(1), vessel.capacity()),
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

    public static Item openGasJar(String gasId) {
        DeferredItem<Item> open = OPEN_GAS_JARS_BY_ID.get(gasId);
        return open != null ? open.get() : net.minecraft.world.item.Items.AIR;
    }

    public static Item gasJarItem(String gasId) {
        DeferredItem<Item> jar = GAS_JARS_BY_ID.get(gasId);
        return jar != null ? jar.get() : net.minecraft.world.item.Items.AIR;
    }

    /** The loose (散装) item for a solid substance id, e.g. "iron_powder" -> loose_iron_powder. */
    public static Item looseSolid(String solidId) {
        return BuiltInRegistries.ITEM.getValue(
                ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "loose_" + solidId));
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
    public static final DeferredItem<BlockItem> GAS_WASHING_BOTTLE =
            ITEMS.registerSimpleBlockItem("gas_washing_bottle", ModBlocks.GAS_WASHING_BOTTLE);
    public static final DeferredItem<BlockItem> GAS_WASHING_BOTTLE_ASSEMBLED =
            ITEMS.registerSimpleBlockItem("gas_washing_bottle_assembled", ModBlocks.GAS_WASHING_BOTTLE);
    public static final DeferredItem<Item> CLAY_TRIANGLE =
            ITEMS.registerItem("clay_triangle", Item::new, props -> props.stacksTo(16));
    // NB-lab vessels (hold reagents, filled via the existing tool mechanics).
    public static final DeferredItem<LabVesselItem> ROUND_BOTTOM_FLASK =
            ITEMS.registerItem("round_bottom_flask",
                    props -> new LabVesselItem(props.stacksTo(1), 250), UnaryOperator.identity());
    public static final DeferredItem<LabVesselItem> ERLENMEYER_FLASK =
            ITEMS.registerItem("erlenmeyer_flask",
                    props -> new PlacedVesselItem(props.stacksTo(1), 250), UnaryOperator.identity());
    public static final DeferredItem<LabVesselItem> CRUCIBLE =
            ITEMS.registerItem("crucible",
                    props -> new LabVesselItem(props.stacksTo(1), 50), UnaryOperator.identity());
    public static final DeferredItem<LabVesselItem> EVAPORATING_DISH =
            ITEMS.registerItem("evaporating_dish",
                    props -> new LabVesselItem(props.stacksTo(1), 50), UnaryOperator.identity());
    // Distillation / ignition parts.
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
    // Glass tubing connectors.
    public static final DeferredItem<Item> STRAIGHT_GLASS_TUBE =
            ITEMS.registerItem("straight_glass_tube", Item::new, props -> props.stacksTo(16));
    public static final DeferredItem<Item> RIGHT_ANGLE_GLASS_TUBE =
            ITEMS.registerItem("right_angle_glass_tube", Item::new, props -> props.stacksTo(16));
    public static final DeferredItem<Item> RIGHT_ANGLE_GLASS_TUBE_LONG =
            ITEMS.registerItem("right_angle_glass_tube_long", Item::new, props -> props.stacksTo(16));
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
    public static final DeferredItem<Item> ALCOHOL_LAMP_CAP = ITEMS.registerSimpleItem("alcohol_lamp_cap");
    public static final DeferredItem<RubberTubeItem> RUBBER_TUBE =
            ITEMS.registerItem("rubber_tube", RubberTubeItem::new, props -> props.stacksTo(16));
    public static final DeferredItem<Item> GAS_NOZZLE = ITEMS.registerSimpleItem("gas_nozzle");
    public static final DeferredItem<Item> GAS_NOZZLE_TUBED =
            ITEMS.registerItem("gas_nozzle_tubed", GasNozzleTubedItem::new, props -> props.stacksTo(1));
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
    public static final DeferredItem<Item> IRON_CATALYST = ITEMS.registerSimpleItem("iron_catalyst");
    public static final DeferredItem<Item> VANADIUM_PENTOXIDE_CATALYST =
            ITEMS.registerSimpleItem("vanadium_pentoxide_catalyst");
    public static final DeferredItem<Item> PLATINUM_RHODIUM_CATALYST =
            ITEMS.registerSimpleItem("platinum_rhodium_catalyst");
    public static final DeferredItem<BlockItem> EMPTY_GAS_JAR =
            ITEMS.registerItem("empty_gas_collecting_bottle",
                    props -> new SealedBottleBlockItem(ModBlocks.GAS_COLLECTING_BOTTLE.get(), props),
                    UnaryOperator.identity());
    public static final DeferredItem<Item> EMPTY_NARROW_BOTTLE =
            ITEMS.registerSimpleItem("empty_narrow_bottle");

    /** Stack a chemical item with creative-mode purity (99.99999%). */
    public static ItemStack pure(ItemStack stack) {
        return com.example.chemistry.PurityHelper.pure(stack);
    }

    public static Item liquidItem(String liquidId) {
        DeferredItem<Item> item = LIQUIDS_BY_ID.get(liquidId);
        return item == null ? net.minecraft.world.item.Items.AIR : item.get();
    }

    public static Item openLiquidItem(String liquidId) {
        DeferredItem<Item> item = OPEN_LIQUIDS_BY_ID.get(liquidId);
        return item == null ? net.minecraft.world.item.Items.AIR : item.get();
    }

    public static Item liquidBucket(String liquidId) {
        DeferredItem<BucketItem> item = LIQUID_BUCKETS_BY_ID.get(liquidId);
        return item == null ? net.minecraft.world.item.Items.AIR : item.get();
    }

    public static Item dropperBottle(String liquidId) {
        DeferredItem<Item> item = DROPPER_BOTTLES_BY_ID.get(liquidId);
        return item == null ? net.minecraft.world.item.Items.AIR : item.get();
    }

    private ModItems() {
    }
}
