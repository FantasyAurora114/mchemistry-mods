package com.example.chemistry.registry;

import com.example.chemistry.transfer.BottleFluidResourceHandler;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Capability registration independent of Mekanism: reagent bottles, dropper
 * bottles and droppers are fillable/drainable
 *   container items ({@code Capabilities.Fluid.ITEM}).
 */
public final class ModCapabilities {

    private ModCapabilities() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        List<Item> fluidItems = new ArrayList<>();
        for (DeferredItem<Item> item : ModItems.LIQUID_ITEMS) {
            fluidItems.add(item.get());
        }
        for (DeferredItem<Item> item : ModItems.OPEN_LIQUID_ITEMS) {
            fluidItems.add(item.get());
        }
        for (DeferredItem<Item> item : ModItems.DROPPER_BOTTLES) {
            fluidItems.add(item.get());
        }
        for (DeferredItem<? extends Item> item : ModItems.LIQUID_BUCKETS) {
            fluidItems.add(item.get());
        }
        fluidItems.add(net.minecraft.world.item.Items.BUCKET);
        fluidItems.add(ModItems.EMPTY_NARROW_BOTTLE.get());
        fluidItems.add(ModItems.EMPTY_DROPPER_BOTTLE.get());
        fluidItems.add(ModItems.DROPPER.get());
        fluidItems.add(ModItems.BROWN_DROPPER.get());
        fluidItems.add(ModItems.DROPPER_BOTTLE_STOPPER.get());
        fluidItems.add(ModItems.BROWN_DROPPER_BOTTLE_STOPPER.get());

        event.registerItem(Capabilities.Fluid.ITEM,
                (stack, access) -> new BottleFluidResourceHandler(access),
                fluidItems.toArray(Item[]::new));
    }
}
