package com.example.chemistry;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.example.chemistry.registry.ModBlockEntities;
import com.example.chemistry.registry.ModBlocks;
import com.example.chemistry.registry.ModCreativeTabs;
import com.example.chemistry.registry.ModEntities;
import com.example.chemistry.registry.ModFluidTypes;
import com.example.chemistry.registry.ModFluids;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.registry.ModCapabilities;
import com.example.chemistry.network.ChemistryNetworking;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

import com.example.chemistry.api.CoreSubstances;

@Mod(ChemistryMod.MODID)
public class ChemistryMod {

    public static final String MODID = "mchemistry";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ChemistryMod(IEventBus modEventBus, ModContainer modContainer) {
        // Seed the framework registry (substances + built-in reactions) first:
        // item registration, the reaction engine and addon queries all rely on it.
        CoreSubstances.init();
        // Register everything in dependency-safe order:
        // fluids and fluid types first, then blocks/items that reference them.
        ModFluidTypes.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(ModCapabilities::registerCapabilities);
        modEventBus.addListener(ChemistryNetworking::register);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
