package com.example.chemistry.registry;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.example.chemistry.ChemistryMod;

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
 * creative-mode purity (99.99999%).
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
                        output.accept(ModItems.GAS_WASHING_BOTTLE.get());
                        output.accept(ModItems.GAS_WASHING_BOTTLE_ASSEMBLED.get());
                        output.accept(ModItems.CLAY_TRIANGLE.get());
                        output.accept(ModItems.STRAIGHT_GLASS_TUBE.get());
                        output.accept(ModItems.RIGHT_ANGLE_GLASS_TUBE.get());
                        output.accept(ModItems.RIGHT_ANGLE_GLASS_TUBE_LONG.get());
                        output.accept(ModItems.RUBBER_TUBE.get());
                        output.accept(ModItems.RUBBER_STOPPER_1_HOLE.get());
                        output.accept(ModItems.RUBBER_STOPPER_2_HOLE.get());
                        output.accept(ModItems.IRON_CATALYST.get());
                        output.accept(ModItems.VANADIUM_PENTOXIDE_CATALYST.get());
                        output.accept(ModItems.PLATINUM_RHODIUM_CATALYST.get());
                        output.accept(ModItems.EMPTY_GAS_JAR.get());
                        output.accept(ModItems.EMPTY_NARROW_BOTTLE.get());
                        output.accept(ModItems.ALCOHOL_LAMP.get());
                        output.accept(ModItems.ALCOHOL_LAMP_LIT.get());
                        output.accept(ModItems.ALCOHOL_LAMP_CAPPED.get());
                        output.accept(ModItems.ALCOHOL_LAMP_CAP.get());
                        output.accept(ModItems.GAS_NOZZLE.get());
                        output.accept(ModItems.GAS_NOZZLE_TUBED.get());
                        output.accept(ModItems.IRON_RING.get());
                        output.accept(ModItems.ASBESTOS_GAUZE.get());
                        output.accept(ModItems.CLAY_GAUZE.get());
                        output.accept(ModItems.ROUND_BOTTOM_FLASK.get());
                        output.accept(ModItems.ERLENMEYER_FLASK.get());
                        output.accept(ModItems.CRUCIBLE.get());
                        output.accept(ModItems.EVAPORATING_DISH.get());
                        output.accept(ModItems.STRAIGHT_CONDENSER.get());
                        output.accept(ModItems.RECEIVER_ADAPTER_STRAIGHT.get());
                        output.accept(ModItems.RECEIVER_ADAPTER_BENT.get());
                        output.accept(ModItems.THERMOMETER.get());
                        output.accept(ModItems.CRUCIBLE_TONGS.get());
                        output.accept(ModItems.SPLINT.get());
                        output.accept(ModItems.GLOWING_SPLINT.get());
                    });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GASES =
            tab("mchemistry_gases", "itemGroup.mchemistry.gases",
                    () -> ModItems.GAS_JARS.get(0).get().getDefaultInstance(),
                    output -> {
                        ModItems.GAS_JARS.forEach(item -> pure(output, item.get()));
                        ModItems.OPEN_GAS_JARS.forEach(item -> {
                            if (!item.getId().getPath().equals("open_gas_collecting_bottle_nitric_oxide")) {
                                pure(output, item.get());
                            }
                        });
                    });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> LIQUIDS =
            tab("mchemistry_liquids", "itemGroup.mchemistry.liquids",
                    () -> ModItems.LIQUID_ITEMS.get(0).get().getDefaultInstance(),
                    output -> {
                        ModItems.LIQUID_ITEMS.forEach(item -> pure(output, item.get()));
                        ModItems.OPEN_LIQUID_ITEMS.forEach(item -> pure(output, item.get()));
                        ModItems.DROPPER_BOTTLES.forEach(item -> pure(output, item.get()));
                        ModItems.LIQUID_BUCKETS.forEach(item -> pure(output, item.get()));
                        output.accept(ModItems.EMPTY_DROPPER_BOTTLE.get());
                    });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SOLIDS =
            tab("mchemistry_solids", "itemGroup.mchemistry.solids",
                    () -> ModItems.SOLID_ITEMS.get(0).get().getDefaultInstance(),
                    output -> {
                        ModItems.SOLID_ITEMS.forEach(item -> pure(output, item.get()));
                        ModItems.OPEN_SOLID_ITEMS.forEach(item -> pure(output, item.get()));
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
