package com.example.mci.registry;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.example.mci.ChemistryMod;
import com.example.mci.data.Ores;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ChemistryMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ORES =
            tab("mci_ores", "itemGroup.mci.ores",
                    () -> ModBlocks.ORE_BLOCKS.get(0).get().asItem().getDefaultInstance(),
                    output -> {
                        for (Ores.OreDef ore : Ores.ALL) {
                            output.accept(BuiltInRegistries.ITEM.getValue(
                                    ResourceLocation.fromNamespaceAndPath("mchemistry", "ore_" + ore.id())));
                            output.accept(BuiltInRegistries.ITEM.getValue(
                                    ResourceLocation.fromNamespaceAndPath("mchemistry", "deepslate_ore_" + ore.id())));
                            output.accept(BuiltInRegistries.ITEM.getValue(
                                    ResourceLocation.fromNamespaceAndPath("mchemistry", "loose_" + ore.id())));
                        }
                    });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> INDUSTRIAL =
            tab("mci_industrial", "itemGroup.mci.industrial",
                    () -> MciItems.SYNTHESIS_TOWER_ITEM.get().getDefaultInstance(),
                    output -> {
                        output.accept(MciItems.SYNTHESIS_TOWER_ITEM.get());
                        output.accept(MciItems.GAS_CANISTER.get());
                    });

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
