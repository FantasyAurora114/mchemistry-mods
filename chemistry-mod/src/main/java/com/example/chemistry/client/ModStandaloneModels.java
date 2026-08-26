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
    private static final String[] PARTS = {"", "_tube_only", "_contents", "_stopper"};
    /** Iron-stand heating attachments: 1 = ring, 2 = asbestos gauze, 3 = clay gauze. */
    private static final String[] ATTACHMENTS = {"ring", "asbestos", "clay"};

    private static final List<StandaloneModelKey<BlockStateModel>> KEYS = new ArrayList<>();
    private static final List<StandaloneModelKey<BlockStateModel>> KEYS_ATTACHMENT = new ArrayList<>();
    private static final StandaloneModelKey<BlockStateModel> KEY_GLASS_STRAIGHT =
            new StandaloneModelKey<>(() -> "mchemistry:block/glass_tube_straight");
    private static final StandaloneModelKey<BlockStateModel> KEY_GLASS_RIGHT_ANGLE =
            new StandaloneModelKey<>(() -> "mchemistry:block/glass_tube_right_angle");
    private static final StandaloneModelKey<BlockStateModel> KEY_GLASS_RIGHT_ANGLE_LONG =
            new StandaloneModelKey<>(() -> "mchemistry:block/glass_tube_right_angle_long");
    private static final StandaloneModelKey<BlockStateModel> KEY_GLASS_DROPPER =
            new StandaloneModelKey<>(() -> "mchemistry:block/glass_tube_dropper");
    private static final StandaloneModelKey<BlockStateModel> KEY_TRIPOD_BAR =
            new StandaloneModelKey<>(() -> "mchemistry:block/tripod_bar");
    private static final StandaloneModelKey<BlockStateModel> KEY_TRIPOD_LEG =
            new StandaloneModelKey<>(() -> "mchemistry:block/tripod_leg");
    private static final StandaloneModelKey<BlockStateModel> KEY_CLAY_TRIANGLE_UNIT =
            new StandaloneModelKey<>(() -> "mchemistry:block/clay_triangle_unit");
    private static final StandaloneModelKey<BlockStateModel> KEY_FUNNEL_IN_STOPPER =
            new StandaloneModelKey<>(() -> "mchemistry:block/funnel_in_stopper");
    private static final StandaloneModelKey<BlockStateModel> KEY_SEPARATORY_IN_STOPPER =
            new StandaloneModelKey<>(() -> "mchemistry:block/separatory_funnel_in_stopper");
    private static final String[] VESSELS = {
            "round_bottom_flask", "erlenmeyer_flask", "crucible", "evaporating_dish"};
    private static final List<StandaloneModelKey<BlockStateModel>> KEYS_VESSELS = new ArrayList<>();
    private static final List<StandaloneModelKey<BlockStateModel>> KEYS_VESSEL_CONTENTS = new ArrayList<>();
    private static final StandaloneModelKey<BlockStateModel> KEY_CONDENSER =
            new StandaloneModelKey<>(() -> "mchemistry:block/straight_condenser");
    private static final StandaloneModelKey<BlockStateModel> KEY_VESSEL_STOPPER =
            new StandaloneModelKey<>(() -> "mchemistry:block/vessel_stopper");
    private static final StandaloneModelKey<BlockStateModel> KEY_RUBBER_TUBE_LINE =
            new StandaloneModelKey<>(() -> "mchemistry:block/rubber_tube_line");
    private static final StandaloneModelKey<BlockStateModel> KEY_GAS_BOTTLE_INVERTED =
            new StandaloneModelKey<>(() -> "mchemistry:block/gas_bottle_inverted");
    private static final StandaloneModelKey<BlockStateModel> KEY_BOTTLE_WATER =
            new StandaloneModelKey<>(() -> "mchemistry:block/bottle_water");
    private static final StandaloneModelKey<BlockStateModel> KEY_GAS_BOTTLE_FILL =
            new StandaloneModelKey<>(() -> "mchemistry:block/gas_bottle_fill");

    public static void register(ModelEvent.RegisterStandalone event) {
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
        }
        event.register(KEY_CONDENSER,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/straight_condenser")));
        event.register(KEY_VESSEL_STOPPER,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/vessel_stopper")));
        event.register(KEY_RUBBER_TUBE_LINE,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/rubber_tube_line")));
        event.register(KEY_GAS_BOTTLE_INVERTED,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/gas_bottle_inverted")));
        event.register(KEY_BOTTLE_WATER,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/bottle_water")));
        event.register(KEY_GAS_BOTTLE_FILL,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/gas_bottle_fill")));
        event.register(KEY_GLASS_STRAIGHT,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/glass_tube_straight")));
        event.register(KEY_GLASS_RIGHT_ANGLE,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/glass_tube_right_angle")));
        event.register(KEY_GLASS_RIGHT_ANGLE_LONG,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/glass_tube_right_angle_long")));
        event.register(KEY_GLASS_DROPPER,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/glass_tube_dropper")));
        event.register(KEY_TRIPOD_BAR,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/tripod_bar")));
        event.register(KEY_TRIPOD_LEG,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/tripod_leg")));
        event.register(KEY_CLAY_TRIANGLE_UNIT,
                SimpleUnbakedStandaloneModel.blockStateModel(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/clay_triangle_unit")));
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
     *  5 = long-stem funnel, 6 = separatory funnel. */
    public static BlockStateModel attachedModel(int type) {
        StandaloneModelKey<BlockStateModel> key = switch (type) {
            case 6 -> KEY_SEPARATORY_IN_STOPPER;
            case 5 -> KEY_FUNNEL_IN_STOPPER;
            case 4 -> KEY_GLASS_RIGHT_ANGLE_LONG;
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

    /** Vessel model: 1 = flask, 2 = erlenmeyer, 3 = crucible, 4 = evaporating dish. */
    public static BlockStateModel vessel(int type) {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEYS_VESSELS.get(type - 1));
    }

    public static BlockStateModel vesselContents(int type) {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEYS_VESSEL_CONTENTS.get(type - 1));
    }

    public static BlockStateModel condenser() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_CONDENSER);
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

    /** Translucent water column inside the inverted bottle. */
    public static BlockStateModel bottleWater() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_BOTTLE_WATER);
    }

    public static BlockStateModel gasBottleFill() {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY_GAS_BOTTLE_FILL);
    }
}
