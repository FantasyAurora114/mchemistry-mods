package com.example.mci;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.example.mci.data.MciSubstances;
import com.example.mci.registry.ModBlockEntities;
import com.example.mci.registry.ModBlocks;
import com.example.mci.registry.ModCapabilities;
import com.example.mci.registry.ModCreativeTabs;
import com.example.mci.registry.MciItems;
import com.example.mci.registry.ModMenus;
import com.example.mci.network.ChemistryNetworking;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * MCI（化学时代·化工）— industrial/ore addon for MChemistry.
 * Depends on the mchemistry mod (required, loaded AFTER it), registers its
 * ores + synthesis tower through the ChemistryAPI and its own registries.
 */
@Mod(ChemistryMod.MODID)
public class ChemistryMod {

    public static final String MODID = "mci";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ChemistryMod(IEventBus modEventBus, ModContainer modContainer) {
        // Ores/solids/reactions go through the ChemistryAPI; the core mod's
        // DynamicItemRegistrar creates the solid item families afterwards.
        MciSubstances.registerAll();

        ModBlocks.BLOCKS.register(modEventBus);
        MciItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(ModCapabilities::registerCapabilities);
        modEventBus.addListener(ChemistryNetworking::register);
        LOGGER.info("[MCI] registered {} ores and {} reactions",
                MciSubstances.SOLIDS.size(), MciSubstances.REACTIONS.size());
    }
}
