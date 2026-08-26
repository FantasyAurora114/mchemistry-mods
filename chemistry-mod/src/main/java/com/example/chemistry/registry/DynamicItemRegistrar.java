package com.example.chemistry.registry;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.api.ChemistryAPI;
import com.example.chemistry.api.Substances.ElementSubstance;
import com.example.chemistry.api.Substances.GasSubstance;
import com.example.chemistry.api.Substances.LiquidSubstance;
import com.example.chemistry.api.Substances.SolidSubstance;
import com.example.chemistry.data.ElementCategory;
import com.example.chemistry.data.ElementState;
import com.example.chemistry.item.ElementItem;
import com.example.chemistry.item.GasCollectingBottleItem;
import com.example.chemistry.item.LiquidBottleItem;
import com.example.chemistry.item.SolidBottleItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.function.Supplier;

/**
 * Creates item families for substances registered by addon mods through
 * {@link ChemistryAPI}. Runs on the ITEMS RegisterEvent, which fires after all
 * mod constructors, so addon registrations are always visible here. Addons must
 * ship the matching assets (items JSON / models / textures) at the standard
 * paths (e.g. {@code liquid_<id>}).
 */
@EventBusSubscriber(modid = ChemistryMod.MODID)
public final class DynamicItemRegistrar {

    private DynamicItemRegistrar() {
    }

    @SubscribeEvent
    public static void onRegisterItems(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.ITEM)) {
            return;
        }
        for (LiquidSubstance s : ChemistryAPI.addonLiquids()) {
            String id = s.id();
            Supplier<Item> open = () -> item("open_liquid_" + id);
            event.register(Registries.ITEM, rl("liquid_" + id),
                    () -> new LiquidBottleItem(props("liquid_" + id).stacksTo(1), id, open,
                            () -> item("narrow_bottle_stopper")));
            event.register(Registries.ITEM, rl("open_liquid_" + id),
                    () -> new Item(props("open_liquid_" + id).stacksTo(1)));
            event.register(Registries.ITEM, rl("dropper_bottle_" + id),
                    () -> new Item(props("dropper_bottle_" + id).stacksTo(1)));
        }
        for (SolidSubstance s : ChemistryAPI.addonSolids()) {
            String id = s.id();
            Supplier<Item> open = () -> item("open_solid_" + id);
            event.register(Registries.ITEM, rl("solid_" + id),
                    () -> new SolidBottleItem(props("solid_" + id).stacksTo(1), open,
                            () -> item("glass_sheet")));
            event.register(Registries.ITEM, rl("open_solid_" + id),
                    () -> new Item(props("open_solid_" + id).stacksTo(1)));
            event.register(Registries.ITEM, rl("loose_" + id),
                    () -> new Item(props("loose_" + id)));
        }
        for (GasSubstance s : ChemistryAPI.addonGases()) {
            String id = s.id();
            Supplier<Item> open = () -> item("open_gas_collecting_bottle_" + id);
            event.register(Registries.ITEM, rl("gas_collecting_bottle_" + id),
                    () -> new GasCollectingBottleItem(props("gas_collecting_bottle_" + id).stacksTo(1), open,
                            () -> item("glass_sheet")));
            event.register(Registries.ITEM, rl("open_gas_collecting_bottle_" + id),
                    () -> new Item(props("open_gas_collecting_bottle_" + id).stacksTo(1)));
        }
        for (ElementSubstance s : ChemistryAPI.addonElements()) {
            registerElementForms(event, s);
        }
    }

    private static void registerElementForms(RegisterEvent event, ElementSubstance s) {
        if (s.state() == ElementState.GAS) {
            event.register(Registries.ITEM, rl("element_" + s.id() + "_tube"),
                    () -> new ElementItem(props("element_" + s.id() + "_tube")));
            return;
        }
        if (s.category() != ElementCategory.NONMETAL) {
            event.register(Registries.ITEM, rl("element_" + s.id() + "_ingot"),
                    () -> new ElementItem(props("element_" + s.id() + "_ingot")));
        }
        event.register(Registries.ITEM, rl("element_" + s.id() + "_dust"),
                () -> new ElementItem(props("element_" + s.id() + "_dust")));
        event.register(Registries.ITEM, rl("element_" + s.id() + "_nugget"),
                () -> new ElementItem(props("element_" + s.id() + "_nugget")));
    }

    private static Item.Properties props(String path) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, rl(path)));
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, path));
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, path);
    }
}
