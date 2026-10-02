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

/**
 * MChemistry（化学时代）—— NeoForge 1.21.10 化学模组本体。
 *
 * Copyright (c) 2026 FantasyAurora (FantasyAurora114). All Rights Reserved.
 * Personal marks: FantasyAurora / Observer / Anastasiya. See the repository LICENSE.
 * 作者标识（非密码学签名）：
 *  - 作者：FantasyAurora114
 *  - 签名：MCH-AUTH::FantasyAurora114::0.1.0::2026
 *  - 运行时可用 /chemistry about 校验；打包后的 jar 清单含
 *    X-Chemistry-Author / X-Chemistry-Signature 属性。
 */
@Mod(ChemistryMod.MODID)
public class ChemistryMod {

    public static final String MODID = "mchemistry";
    /** 模组作者（归属标识）。 */
    public static final String AUTHOR = "FantasyAurora114";
    /** 构建署名（非密码学签名）。 */
    public static final String SIGNATURE = "MCH-AUTH::FantasyAurora114::0.1.0::2026";
    /** 构建版本号（由 mod_version 注入，运行时可查）。 */
    public static final String VERSION = "0.1.0";
    /** 全部署名：用于归属标识及运行时署名摘要。 */
    private static final java.util.List<String> ALL_SIGNATURES = java.util.List.of(
            "FantasyAurora", "Fantasy_Aurora", "FantasyAurora114", "Observer",
            "Anastasiya", "Valer1ya", "FantasyForward");
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 由全部署名生成署名摘要（非密码学校验）（/chemistry about 显示）。 */
    public static String signatureChecksum() {
        String joined = String.join("|", ALL_SIGNATURES);
        return Integer.toHexString(joined.hashCode());
    }

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
        com.example.chemistry.registry.ModMenus.MENUS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ReactionGameTests.TEST_FUNCTIONS.register(modEventBus);
        modEventBus.addListener(ModCapabilities::registerCapabilities);
        modEventBus.addListener(ChemistryNetworking::register);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
