package com.example.chemistry.client;

import java.util.ArrayList;
import java.util.List;

import com.example.chemistry.ChemistryMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/**
 * Standalone (unbound) block models used by the iron stand's block-entity
 * renderer. Each is a normal model file under models/block/.
 */
public class ModStandaloneModels {

    public static final int PART_BASE = 0;
    public static final int PART_TUBE = 1;
    public static final int PART_CONTENTS = 2;
    public static final int PART_STOPPER = 3;
    private static final String[] PARTS = {"", "_tube_only", "_contents", "_stopper", "_dewar"};
    /** Iron-stand heating attachments: 1 = ring, 2 = asbestos gauze, 3 = clay gauze. */
    private static final String[] ATTACHMENTS = {"ring", "asbestos", "clay"};

    private static final List<StandaloneModelKey<BlockStateModel>> KEYS = new ArrayList<>();
    private static final List<StandaloneModelKey<BlockStateModel>> KEYS_ATTACHMENT = new ArrayList<>();
    private static final StandaloneModelKey<BlockStateModel> KEY_ERLENMEYER_GROUND =
            new StandaloneModelKey<>(() -> "mchemistry:block/erlenmeyer_flask_ground_joint");
    private static final List<StandaloneModelKey<BlockStateModel>> ERLENMEYER_LEVELS =
            new ArrayList<>();
    private static final StandaloneModelKey<BlockStateModel> KEY_GLASS_STRAIGHT =
            new StandaloneModelKey<>(() -> "mchemistry:block/glass_tube_straight");
    private static final StandaloneModelKey<BlockStateModel> KEY_GLASS_STRAIGHT_LONG =
            new StandaloneModelKey<>(() -> "mchemistry:block/glass_tube_straight_long");
    private static final StandaloneModelKey<BlockStateModel> KEY_GLASS_RIGHT_ANGLE =
            new StandaloneModelKey<>(() -> "mchemistry:block/glass_tube_right_angle");
    private static final StandaloneModelKey<BlockStateModel> KEY_GLASS_RIGHT_ANGLE_LONG =
            new StandaloneModelKey<>(() -> "mchemistry:block/glass_tube_right_angle_long");
    private static final StandaloneModelKey<BlockStateModel> KEY_GLASS_DROPPER =
            new StandaloneModelKey<>(() -> "mchemistry:block/glass_tube_dropper");
    private static final StandaloneModelKey<BlockStateModel> KEY_SINGLE_GROUND =
            new StandaloneModelKey<>(() -> "mchemistry:block/ground_glass_flask");
    public static BlockStateModel singleGround() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_SINGLE_GROUND);
    }
    private static final StandaloneModelKey<BlockStateModel> KEY_THERMOMETER_SLEEVE =
            new StandaloneModelKey<>(() -> "mchemistry:block/thermometer_sleeve");
    public static BlockStateModel thermometerSleeve() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_THERMOMETER_SLEEVE);
    }
    private static final StandaloneModelKey<BlockStateModel> KEY_THERMOMETER_IN_STOPPER =
            new StandaloneModelKey<>(() -> "mchemistry:block/thermometer_in_stopper");
    private static final StandaloneModelKey<BlockStateModel> KEY_TRIPOD_BAR =
            new StandaloneModelKey<>(() -> "mchemistry:block/tripod_bar");
    private static final StandaloneModelKey<BlockStateModel> KEY_TRIPOD_LEG =
            new StandaloneModelKey<>(() -> "mchemistry:block/tripod_leg");
    private static final StandaloneModelKey<BlockStateModel> KEY_CLAY_TRIANGLE_UNIT =
            new StandaloneModelKey<>(() -> "mchemistry:block/clay_triangle_unit");
    private static final StandaloneModelKey<BlockStateModel> KEY_TEST_TUBE_RACK =
            new StandaloneModelKey<>(() -> "mchemistry:block/test_tube_rack");
    private static final StandaloneModelKey<BlockStateModel> KEY_TEST_TUBE_RACK_TUBE =
            new StandaloneModelKey<>(() -> "mchemistry:block/test_tube_rack_tube");
    private static final StandaloneModelKey<BlockStateModel> KEY_TEST_TUBE_RACK_TUBE_CONTENTS =
            new StandaloneModelKey<>(() -> "mchemistry:block/test_tube_rack_tube_contents");
    private static final StandaloneModelKey<BlockStateModel> KEY_GRADUATED_CYLINDER =
            new StandaloneModelKey<>(() -> "mchemistry:block/graduated_cylinder");
    private static final StandaloneModelKey<BlockStateModel> KEY_GRADUATED_CYLINDER_LIQUID =
            new StandaloneModelKey<>(() -> "mchemistry:block/graduated_cylinder_liquid");
    private static final StandaloneModelKey<BlockStateModel> KEY_MAGNETIC_STIRRER =
            new StandaloneModelKey<>(() -> "mchemistry:block/magnetic_stirrer");
    private static final StandaloneModelKey<BlockStateModel> KEY_STIR_BAR =
            new StandaloneModelKey<>(() -> "mchemistry:block/stir_bar");
    private static final StandaloneModelKey<BlockStateModel> KEY_GLASS_STOPPER_PLUG =
            new StandaloneModelKey<>(() -> "mchemistry:block/glass_stopper_plug");
    private static final StandaloneModelKey<BlockStateModel> KEY_FUNNEL_IN_STOPPER =
            new StandaloneModelKey<>(() -> "mchemistry:block/funnel_in_stopper");
    private static final StandaloneModelKey<BlockStateModel> KEY_SEPARATORY_IN_STOPPER =
            new StandaloneModelKey<>(() -> "mchemistry:block/separatory_funnel_in_stopper");
    private static final String[] VESSELS = {
            "round_bottom_flask", "erlenmeyer_flask", "crucible", "evaporating_dish", "beaker",
            "three_neck_flask", "flat_bottom_flask", "beaker_medium", "beaker_tall", "suction_flask"};
    private static final List<StandaloneModelKey<BlockStateModel>> KEYS_VESSELS = new ArrayList<>();
    private static final List<StandaloneModelKey<BlockStateModel>> KEYS_VESSEL_CONTENTS = new ArrayList<>();
    private static final List<StandaloneModelKey<BlockStateModel>> KEYS_VESSEL_LIQUID = new ArrayList<>();
    private static final StandaloneModelKey<BlockStateModel> KEY_CONDENSER =
            new StandaloneModelKey<>(() -> "mchemistry:block/straight_condenser");
    private static final StandaloneModelKey<BlockStateModel> KEY_DISTILLATION_HEAD =
            new StandaloneModelKey<>(() -> "mchemistry:block/distillation_head");
    private static final StandaloneModelKey<BlockStateModel> KEY_DISTILLATION_STEAM =
            new StandaloneModelKey<>(() -> "mchemistry:block/distillation_head_steam");
    private static final StandaloneModelKey<BlockStateModel> KEY_RECEIVER_ADAPTER_BENT =
            new StandaloneModelKey<>(() -> "mchemistry:block/receiver_adapter_bent");
    private static final StandaloneModelKey<BlockStateModel> KEY_RECEIVER_ADAPTER_STRAIGHT =
            new StandaloneModelKey<>(() -> "mchemistry:block/receiver_adapter_straight");
    private static final List<StandaloneModelKey<BlockStateModel>> COW_HORN_PARTS = new ArrayList<>();
    private static final StandaloneModelKey<BlockStateModel> KEY_VESSEL_STOPPER =
            new StandaloneModelKey<>(() -> "mchemistry:block/vessel_stopper");
    private static final StandaloneModelKey<BlockStateModel> KEY_RUBBER_TUBE_LINE =
            new StandaloneModelKey<>(() -> "mchemistry:block/rubber_tube_line");
    private static final StandaloneModelKey<BlockStateModel> KEY_GAS_BOTTLE_INVERTED =
            new StandaloneModelKey<>(() -> "mchemistry:block/gas_bottle_inverted");
    private static final StandaloneModelKey<BlockStateModel> KEY_GAS_BOTTLE =
            new StandaloneModelKey<>(() -> "mchemistry:block/gas_bottle");
    private static final StandaloneModelKey<BlockStateModel> KEY_GAS_BOTTLE_OPENING =
            new StandaloneModelKey<>(() -> "mchemistry:block/gas_bottle_opening");
    private static final StandaloneModelKey<BlockStateModel> KEY_GAS_BOTTLE_PLATE =
            new StandaloneModelKey<>(() -> "mchemistry:block/gas_bottle_plate");
    private static final StandaloneModelKey<BlockStateModel> KEY_GAS_BOTTLE_OPENING_INVERTED =
            new StandaloneModelKey<>(() -> "mchemistry:block/gas_bottle_opening_inverted");
    private static final StandaloneModelKey<BlockStateModel> KEY_GAS_BOTTLE_PLATE_INVERTED =
            new StandaloneModelKey<>(() -> "mchemistry:block/gas_bottle_plate_inverted");
    private static final StandaloneModelKey<BlockStateModel> KEY_BOTTLE_WATER =
            new StandaloneModelKey<>(() -> "mchemistry:block/bottle_water");
    private static final StandaloneModelKey<BlockStateModel> KEY_GAS_BOTTLE_WATER_FILL =
            new StandaloneModelKey<>(() -> "mchemistry:block/gas_bottle_water_fill");
    public static BlockStateModel gasBottleWaterFill(){return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_GAS_BOTTLE_WATER_FILL);}
    private static final StandaloneModelKey<BlockStateModel> KEY_GAS_BOTTLE_FILL =
            new StandaloneModelKey<>(() -> "mchemistry:block/gas_bottle_fill");

    public static void register(ModelEvent.RegisterStandalone event) {
        ReagentCabinetRenderer.registerModels(event);
        LaboratoryBenchRenderer.registerModels(event);
        GasApplianceRenderer.registerModels(event);
        KEYS.clear();
        for (int r = 0; r < 8; r++) {
            for (String part : PARTS) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                        ChemistryMod.MODID, "block/iron_stand_" + r + part);
                StandaloneModelKey<BlockStateModel> key =
                        new StandaloneModelKey<>(() -> id.toString());
                event.register(key, SimpleUnbakedStandaloneModel.blockStateModel(id));
                KEYS.add(key);
            }
        }
        for (String suffix : ATTACHMENTS) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    ChemistryMod.MODID, "block/iron_stand_attachment_" + suffix);
            StandaloneModelKey<BlockStateModel> key =
                    new StandaloneModelKey<>(() -> id.toString());
            event.register(key, SimpleUnbakedStandaloneModel.blockStateModel(id));
            KEYS_ATTACHMENT.add(key);
        }
        event.register(KEY_ERLENMEYER_GROUND,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,
                                "block/erlenmeyer_flask_ground_joint")));
        ERLENMEYER_LEVELS.clear();
        for (int level = 5; level <= 100; level += 5) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,
                    "block/erlenmeyer_liquid_" + String.format(java.util.Locale.ROOT, "%03d", level));
            StandaloneModelKey<BlockStateModel> key =
                    new StandaloneModelKey<>(() -> id.toString());
            event.register(key, SimpleUnbakedStandaloneModel.blockStateModel(id));
            ERLENMEYER_LEVELS.add(key);
        }
        for (String vessel : VESSELS) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    ChemistryMod.MODID, "block/" + vessel);
            StandaloneModelKey<BlockStateModel> key =
                    new StandaloneModelKey<>(() -> id.toString());
            event.register(key, SimpleUnbakedStandaloneModel.blockStateModel(id));
            KEYS_VESSELS.add(key);
            ResourceLocation cid = ResourceLocation.fromNamespaceAndPath(
                    ChemistryMod.MODID, "block/" + vessel + "_contents");
            StandaloneModelKey<BlockStateModel> ckey =
                    new StandaloneModelKey<>(() -> cid.toString());
            event.register(ckey, SimpleUnbakedStandaloneModel.blockStateModel(cid));
            KEYS_VESSEL_CONTENTS.add(ckey);
            var liquidId=ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,"block/"+vessel+"_water_contents");
            var liquidKey=new StandaloneModelKey<BlockStateModel>(() -> liquidId.toString());
            event.register(liquidKey,SimpleUnbakedStandaloneModel.blockStateModel(liquidId));
            KEYS_VESSEL_LIQUID.add(liquidKey);
        }
        event.register(KEY_CONDENSER,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/straight_condenser")));
        event.register(KEY_DISTILLATION_HEAD,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,
                                "block/distillation_head")));
        event.register(KEY_DISTILLATION_STEAM,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,
                                "block/distillation_head_steam")));
        event.register(KEY_RECEIVER_ADAPTER_BENT,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,
                                "block/receiver_adapter_bent")));
        event.register(KEY_RECEIVER_ADAPTER_STRAIGHT,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,
                                "block/receiver_adapter_straight")));
        COW_HORN_PARTS.clear();
        for (int index = 0; index < CowHornParts.PARTS.length; index++) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,
                    "block/cow_horn_parts_135/part_"
                            + String.format(java.util.Locale.ROOT, "%02d", index));
            StandaloneModelKey<BlockStateModel> key =
                    new StandaloneModelKey<>(() -> id.toString());
            event.register(key, SimpleUnbakedStandaloneModel.blockStateModel(id));
            COW_HORN_PARTS.add(key);
        }
        event.register(KEY_VESSEL_STOPPER,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/vessel_stopper")));
        event.register(KEY_RUBBER_TUBE_LINE,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/rubber_tube_line")));
        event.register(KEY_GAS_BOTTLE_INVERTED,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/gas_bottle_inverted")));
        event.register(KEY_GAS_BOTTLE,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/gas_bottle")));
        event.register(KEY_GAS_BOTTLE_OPENING,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/gas_bottle_opening")));
        event.register(KEY_GAS_BOTTLE_PLATE,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/gas_bottle_plate")));
        event.register(KEY_GAS_BOTTLE_OPENING_INVERTED,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/gas_bottle_opening_inverted")));
        event.register(KEY_GAS_BOTTLE_PLATE_INVERTED,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/gas_bottle_plate_inverted")));
        event.register(KEY_BOTTLE_WATER,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/bottle_water")));
        event.register(KEY_GAS_BOTTLE_WATER_FILL,SimpleUnbakedStandaloneModel.blockStateModel(
                ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,"block/gas_bottle_water_fill")));
        event.register(KEY_GAS_BOTTLE_FILL,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/gas_bottle_fill")));
        event.register(KEY_GLASS_STRAIGHT,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/glass_tube_straight")));
        event.register(KEY_GLASS_STRAIGHT_LONG,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/glass_tube_straight_long")));
        event.register(KEY_GLASS_RIGHT_ANGLE,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/glass_tube_right_angle")));
        event.register(KEY_GLASS_RIGHT_ANGLE_LONG,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/glass_tube_right_angle_long")));
        event.register(KEY_GLASS_DROPPER,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/glass_tube_dropper")));
        event.register(KEY_SINGLE_GROUND, SimpleUnbakedStandaloneModel.blockStateModel(
                ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,"block/ground_glass_flask")));
        event.register(KEY_THERMOMETER_SLEEVE, SimpleUnbakedStandaloneModel.blockStateModel(
                ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,"block/thermometer_sleeve")));
        event.register(KEY_THERMOMETER_IN_STOPPER,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,
                                "block/thermometer_in_stopper")));
        event.register(KEY_TRIPOD_BAR,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/tripod_bar")));
        event.register(KEY_TRIPOD_LEG,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/tripod_leg")));
        event.register(KEY_CLAY_TRIANGLE_UNIT,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/clay_triangle_unit")));
        event.register(KEY_TEST_TUBE_RACK,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/test_tube_rack")));
        event.register(KEY_TEST_TUBE_RACK_TUBE,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,
                                "block/test_tube_rack_tube")));
        event.register(KEY_TEST_TUBE_RACK_TUBE_CONTENTS,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,
                                "block/test_tube_rack_tube_contents")));
        event.register(KEY_GRADUATED_CYLINDER,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,
                                "block/graduated_cylinder")));
        event.register(KEY_GRADUATED_CYLINDER_LIQUID,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,
                                "block/graduated_cylinder_liquid")));
        event.register(KEY_MAGNETIC_STIRRER,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,
                                "block/magnetic_stirrer")));
        event.register(KEY_STIR_BAR,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,
                                "block/stir_bar")));
        event.register(KEY_GLASS_STOPPER_PLUG,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,
                                "block/glass_stopper_plug")));
        event.register(KEY_FUNNEL_IN_STOPPER,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/funnel_in_stopper")));
        event.register(KEY_SEPARATORY_IN_STOPPER,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/separatory_funnel_in_stopper")));
    }

    public static BlockStateModel part(int rotation, int part) {
        return Minecraft.getInstance().getModelManager()
                .getStandaloneModel(KEYS.get(rotation * PARTS.length + part));
    }

    /** Heating attachment model for type 1-3 (ring / asbestos / clay). */
    public static BlockStateModel attachment(int type) {
        return Minecraft.getInstance().getModelManager()
                .getStandaloneModel(KEYS_ATTACHMENT.get(type - 1));
    }

    /** 1 = straight, 2 = right-angle, 3 = dropper, 4 = long right-angle,
     *  5 = long-stem funnel, 6 = separatory funnel, 7 = thermometer. */
    public static BlockStateModel attachedModel(int type) {
        StandaloneModelKey<BlockStateModel> key = switch (type) {
            case 7 -> KEY_THERMOMETER_IN_STOPPER;
            case 6 -> KEY_SEPARATORY_IN_STOPPER;
            case 5 -> KEY_FUNNEL_IN_STOPPER;
            case 4 -> KEY_GLASS_RIGHT_ANGLE_LONG;
            case 8 -> KEY_GLASS_STRAIGHT_LONG;
            case 2 -> KEY_GLASS_RIGHT_ANGLE;
            case 3 -> KEY_GLASS_DROPPER;
            default -> KEY_GLASS_STRAIGHT;
        };
        return Minecraft.getInstance().getModelManager().getStandaloneModel(key);
    }

    public static BlockStateModel tripodBar() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_TRIPOD_BAR);
    }

    public static BlockStateModel tripodLeg() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_TRIPOD_LEG);
    }

    public static BlockStateModel clayTriangleUnit() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_CLAY_TRIANGLE_UNIT);
    }

    public static BlockStateModel testTubeRack() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_TEST_TUBE_RACK);
    }

    public static BlockStateModel testTubeRackTube() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_TEST_TUBE_RACK_TUBE);
    }

    public static BlockStateModel testTubeRackTubeContents() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(
                KEY_TEST_TUBE_RACK_TUBE_CONTENTS);
    }

    public static BlockStateModel graduatedCylinder() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_GRADUATED_CYLINDER);
    }

    public static BlockStateModel graduatedCylinderLiquid() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_GRADUATED_CYLINDER_LIQUID);
    }

    public static BlockStateModel magneticStirrer() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_MAGNETIC_STIRRER);
    }

    public static BlockStateModel stirBar() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_STIR_BAR);
    }

    public static BlockStateModel glassStopperPlug() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_GLASS_STOPPER_PLUG);
    }

    /** Vessel model: 1 = flask, 2 = erlenmeyer, 3 = crucible, 4 = evaporating dish. */
    public static BlockStateModel vessel(int type) {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEYS_VESSELS.get(type - 1));
    }

    public static BlockStateModel erlenmeyerGroundJoint() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_ERLENMEYER_GROUND);
    }

    public static BlockStateModel erlenmeyerLiquidLevel(float fill) {
        int index = Math.max(0, Math.min(ERLENMEYER_LEVELS.size() - 1,
                (int) Math.ceil(Math.max(0, fill) * 20.0) - 1));
        return Minecraft.getInstance().getModelManager().getStandaloneModel(
                ERLENMEYER_LEVELS.get(index));
    }

    public static BlockStateModel vesselLiquid(int type) {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEYS_VESSEL_LIQUID.get(type-1));
    }
    public static BlockStateModel testTube(int rotation,boolean dewar){return part(rotation,dewar?4:PART_TUBE);}
    public static BlockStateModel vesselContents(int type) {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEYS_VESSEL_CONTENTS.get(type - 1));
    }

    public static BlockStateModel condenser() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_CONDENSER);
    }

    public static BlockStateModel distillationHead() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_DISTILLATION_HEAD);
    }

    public static BlockStateModel distillationSteam() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_DISTILLATION_STEAM);
    }

    public static BlockStateModel receiverAdapter(boolean bent) {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(
                bent ? KEY_RECEIVER_ADAPTER_BENT : KEY_RECEIVER_ADAPTER_STRAIGHT);
    }

    public static BlockStateModel cowHornPart(int index) {
        return Minecraft.getInstance().getModelManager()
                .getStandaloneModel(COW_HORN_PARTS.get(index));
    }

    public static BlockStateModel vesselStopper() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_VESSEL_STOPPER);
    }

    public static BlockStateModel rubberTubeLine() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_RUBBER_TUBE_LINE);
    }

    /** Inverted gas bottle standing in a water trough (排水法集气). */
    public static BlockStateModel gasBottleInverted() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_GAS_BOTTLE_INVERTED);
    }

    public static BlockStateModel gasBottle(boolean inverted, boolean hasPlate, boolean hasNozzle) {
        StandaloneModelKey<BlockStateModel> key;
        if (inverted) {
            key = hasNozzle ? KEY_GAS_BOTTLE_OPENING_INVERTED
                    : (hasPlate ? KEY_GAS_BOTTLE_PLATE_INVERTED : KEY_GAS_BOTTLE_INVERTED);
        } else {
            key = hasNozzle ? KEY_GAS_BOTTLE_OPENING
                    : (hasPlate ? KEY_GAS_BOTTLE_PLATE : KEY_GAS_BOTTLE);
        }
        return Minecraft.getInstance().getModelManager().getStandaloneModel(key);
    }

    /** Translucent water column inside the inverted bottle. */
    public static BlockStateModel bottleWater() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_BOTTLE_WATER);
    }

    public static BlockStateModel gasBottleFill() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_GAS_BOTTLE_FILL);
    }
}
