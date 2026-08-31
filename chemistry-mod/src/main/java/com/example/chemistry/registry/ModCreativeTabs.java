package com.example.chemistry.registry;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.data.GasJars;
import com.example.chemistry.data.Liquids;
import com.example.chemistry.data.Solids;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Creative-mode tabs, grouped by category. Chemical substances are given in
 * creative-mode purity (99.99999%). The unified bottles appear once per
 * substance as NBT-filled stacks of the single bottle item.
 */
public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ChemistryMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ELEMENTS =
            tab("mchemistry_elements", "itemGroup.mchemistry.elements",
                    () -> ModItems.ELEMENT_ITEMS.get(0).get().getDefaultInstance(),
                    output -> {
                        ModItems.ELEMENT_ITEMS.forEach(item -> pure(output, item.get()));
                        pure(output, ModItems.COMPOUND_WATER.get());
                        pure(output, ModItems.COMPOUND_SALT.get());
                        pure(output, ModItems.RED_PHOSPHORUS.get());
                        pure(output, ModItems.WHITE_PHOSPHORUS.get());
                        pure(output, ModItems.GRAPHITE.get());
                        pure(output, ModItems.OZONE.get());
                    });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> INSTRUMENTS =
            tab("mchemistry_instruments", "itemGroup.mchemistry.instruments",
                    () -> ModItems.INSTRUMENTS.get(0).get().getDefaultInstance(),
                    output -> {
                        output.accept(ModItems.HANDBOOK.get());
                        ModItems.INSTRUMENTS.forEach(item -> output.accept(item.get()));
                        output.accept(ModItems.GLASS_SHEET.get());
                        output.accept(ModItems.BOTTLE_STOPPER.get());
                        output.accept(ModItems.DROPPER.get());
                        output.accept(ModItems.BROWN_DROPPER.get());
                        output.accept(ModItems.DROPPER_BOTTLE_STOPPER.get());
                        output.accept(ModItems.BROWN_DROPPER_BOTTLE_STOPPER.get());
                        output.accept(ModItems.TWEEZERS.get());
                        output.accept(ModItems.SPATULA.get());
                        output.accept(ModItems.CHEM_GOGGLES.get());
                        output.accept(ModItems.COMBUSTION_SPOON.get());
                        ModItems.LAB_VESSELS.forEach(item -> output.accept(item.get()));
                        ModItems.TEST_TUBES.forEach(item -> output.accept(item.get()));
                        output.accept(ModItems.TEST_TUBE_CLAMP.get());
                        output.accept(ModItems.IRON_STAND_ITEM.get());
                        output.accept(ModItems.LAB_TABLE_ITEM.get());
                        output.accept(ModItems.WATER_TROUGH_ITEM.get());
                        output.accept(ModItems.LONG_STEM_FUNNEL.get());
                        output.accept(ModItems.SEPARATORY_FUNNEL.get());
                        output.accept(ModItems.TRIPOD.get());
                        output.accept(ModItems.HEATING_MANTLE.get());
                        output.accept(ModItems.TEST_TUBE_RACK.get());
                        output.accept(ModItems.ASSEMBLY_FRAME.get());
                        output.accept(ModItems.GRADUATED_CYLINDER.get());
                        output.accept(ModItems.MAGNETIC_STIRRER.get());
                        output.accept(ModItems.STIR_BAR.get());
                        output.accept(ModItems.DISTILLATION_HEAD.get());
                        output.accept(ModItems.GAS_WASHING_BOTTLE.get());
                        output.accept(ModItems.GAS_WASHING_BOTTLE_ASSEMBLED.get());
                        output.accept(ModItems.CLAY_TRIANGLE.get());
                        output.accept(ModItems.STRAIGHT_GLASS_TUBE.get());
                        output.accept(ModItems.GLASS_ROD.get());
                        output.accept(ModItems.RIGHT_ANGLE_GLASS_TUBE.get());
                        output.accept(ModItems.RIGHT_ANGLE_GLASS_TUBE_LONG.get());
                        output.accept(ModItems.STRAIGHT_GLASS_TUBE_TUBED.get());
                        output.accept(ModItems.RIGHT_ANGLE_GLASS_TUBE_TUBED.get());
                        output.accept(ModItems.RIGHT_ANGLE_GLASS_TUBE_LONG_TUBED.get());
                        output.accept(ModItems.RUBBER_TUBE.get());
                        output.accept(ModItems.RUBBER_STOPPER_1_HOLE.get());
                        output.accept(ModItems.RUBBER_STOPPER_2_HOLE.get());
                        output.accept(ModItems.RUBBER_STOPPER_3_HOLE.get());
                        output.accept(ModItems.IRON_CATALYST.get());
                        output.accept(ModItems.VANADIUM_PENTOXIDE_CATALYST.get());
                        output.accept(ModItems.PLATINUM_RHODIUM_CATALYST.get());
                        output.accept(ModItems.ALCOHOL_LAMP.get());
                        output.accept(ModItems.ALCOHOL_LAMP_LIT.get());
                        output.accept(ModItems.ALCOHOL_LAMP_CAPPED.get());
                        output.accept(ModItems.ALCOHOL_LAMP_CAP.get());
                        output.accept(ModItems.ALCOHOL_BLOWTORCH.get());
                        output.accept(ModItems.ALCOHOL_BLOWTORCH_LIT.get());
                        output.accept(ModItems.IRON_RING.get());
                        output.accept(ModItems.ASBESTOS_GAUZE.get());
                        output.accept(ModItems.CLAY_GAUZE.get());
                        output.accept(ModItems.ROUND_BOTTOM_FLASK.get());
                        output.accept(ModItems.ERLENMEYER_FLASK.get());
                        output.accept(ModItems.FLAT_BOTTOM_FLASK.get());
                        output.accept(ModItems.GROUND_GLASS_FLASK.get());
                        output.accept(ModItems.GROUND_GLASS_ERLENMEYER.get());
                        output.accept(ModItems.GROUND_GLASS_FLAT_BOTTOM_FLASK.get());
                        output.accept(ModItems.THREE_NECK_FLASK.get());
                        output.accept(ModItems.GLASS_STOPPER.get());
                        output.accept(ModItems.CRUCIBLE.get());
                        output.accept(ModItems.EVAPORATING_DISH.get());
                        output.accept(ModItems.STRAIGHT_CONDENSER.get());
                        output.accept(ModItems.RECEIVER_ADAPTER_STRAIGHT.get());
                        output.accept(ModItems.RECEIVER_ADAPTER_BENT.get());
                        output.accept(ModItems.THERMOMETER.get());
                        output.accept(ModItems.CRUCIBLE_TONGS.get());
                        output.accept(ModItems.SPLINT.get());
                        output.accept(ModItems.GLOWING_SPLINT.get());
                        output.accept(ModItems.LABEL.get());
                    });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GASES =
            tab("mchemistry_gases", "itemGroup.mchemistry.gases",
                    () -> ModItems.gasBottle(GasJars.ALL.get(0).id(), true),
                    output -> {
                        for (GasJars.GasJar gas : GasJars.ALL) {
                            pure(output, ModItems.gasBottle(gas.id(), true));
                            pure(output, ModItems.gasBottle(gas.id(), false));
                        }
                        output.accept(ModItems.emptyGasJar());
                        output.accept(ModItems.gasBottleWater());
                    });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> LIQUIDS =
            tab("mchemistry_liquids", "itemGroup.mchemistry.liquids",
                    () -> ModItems.liquidBottle(Liquids.ALL.get(0).id(), true),
                    output -> {
                        for (Liquids.Liquid liquid : Liquids.ALL) {
                            pure(output, ModItems.liquidBottle(liquid.id(), true));
                            pure(output, ModItems.liquidBottle(liquid.id(), false));
                            pure(output, ModItems.dropperBottle(liquid.id()));
                        }
                        ModItems.LIQUID_BUCKETS.forEach(item -> pure(output, item.get()));
                        output.accept(ModItems.emptyNarrowBottle());
                        output.accept(ModItems.emptyDropperBottle());
                    });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SOLIDS =
            tab("mchemistry_solids", "itemGroup.mchemistry.solids",
                    () -> ModItems.solidJar(Solids.ALL.get(0).id(), true),
                    output -> {
                        for (Solids.Solid solid : Solids.ALL) {
                            pure(output, ModItems.solidJar(solid.id(), true));
                            pure(output, ModItems.solidJar(solid.id(), false));
                        }
                        ModItems.LOOSE_SOLIDS.forEach(item -> pure(output, item.get()));
                    });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> OTHER =
            tab("mchemistry_other", "itemGroup.mchemistry.other",
                    () -> ModItems.CHEMICAL_WATER_BUCKET.get().getDefaultInstance(),
                    output -> {
                        output.accept(ModItems.CHEMICAL_WATER_BUCKET.get());
                    });

    private static void pure(CreativeModeTab.Output output, ItemLike item) {
        output.accept(ModItems.pure(new ItemStack(item.asItem())));
    }

    private static void pure(CreativeModeTab.Output output, ItemStack stack) {
        output.accept(ModItems.pure(stack));
    }

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> tab(String id, String titleKey,
            Supplier<ItemStack> icon, Consumer<CreativeModeTab.Output> items) {
        return CREATIVE_MODE_TABS.register(id, () -> CreativeModeTab.builder()
                .title(Component.translatable(titleKey))
                .withTabsBefore(CreativeModeTabs.COMBAT)
                .icon(icon)
                .displayItems((parameters, output) -> items.accept(output))
                .build());
    }

    private ModCreativeTabs() {
    }
}
