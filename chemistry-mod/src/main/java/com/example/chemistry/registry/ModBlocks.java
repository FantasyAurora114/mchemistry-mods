package com.example.chemistry.registry;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.block.AlcoholLampBlock;
import com.example.chemistry.block.IronStandBlock;
import com.example.chemistry.block.PlacedTestTubeBlock;
import com.example.chemistry.block.RubberTubeLinkBlock;
import com.example.chemistry.block.WaterTroughBlock;
import com.example.chemistry.block.GasCollectingBottleBlock;
import com.example.chemistry.block.GasWashingBottleBlock;
import com.example.chemistry.block.SeparatoryFunnelBlock;
import com.example.chemistry.block.TripodBlock;
import com.example.chemistry.block.PlacedVesselBlock;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ChemistryMod.MODID);

    public static final DeferredBlock<LiquidBlock> CHEMICAL_WATER = BLOCKS.registerBlock("chemical_water",
            props -> new LiquidBlock(ModFluids.CHEMICAL_WATER.get(), props),
            props -> props.mapColor(MapColor.WATER)
                    .replaceable()
                    .noCollision()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable()
                    .liquid());

    public static final DeferredBlock<PlacedTestTubeBlock> PLACED_TEST_TUBE = BLOCKS.registerBlock("placed_test_tube",
            PlacedTestTubeBlock::new,
            props -> props.mapColor(MapColor.NONE)
                    .noOcclusion()
                    .strength(0.2F)
                    .noLootTable()
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<IronStandBlock> IRON_STAND = BLOCKS.registerBlock("iron_stand",
            IronStandBlock::new,
            props -> props.mapColor(MapColor.METAL)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .noLootTable()
                    .pushReaction(PushReaction.DESTROY));

    /** Decorative lab bench (实验台); black top, four sides, open bottom. */
    public static final DeferredBlock<Block> LAB_TABLE = BLOCKS.registerBlock("lab_table",
            Block::new,
            props -> props.mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    /** Alcohol lamp (酒精灯): unlit / lit / capped states, heats test tubes. */
    public static final DeferredBlock<AlcoholLampBlock> ALCOHOL_LAMP = BLOCKS.registerBlock("alcohol_lamp",
            AlcoholLampBlock::new,
            props -> props.mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.0F, 2.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> state.getValue(AlcoholLampBlock.LIT) ? 15 : 0)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    /** Water trough (水槽): empty / water-filled states. */
    public static final DeferredBlock<WaterTroughBlock> WATER_TROUGH = BLOCKS.registerBlock("water_trough",
            WaterTroughBlock::new,
            props -> props.mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.5F, 3.0F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    /** Placeable gas collecting bottle (集气瓶). */
    public static final DeferredBlock<GasCollectingBottleBlock> GAS_COLLECTING_BOTTLE =
            BLOCKS.registerBlock("gas_collecting_bottle", GasCollectingBottleBlock::new,
                    props -> props.mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(0.5F, 2.0F)
                            .sound(SoundType.GLASS)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY));

    /** Gas washing bottle (洗气瓶): plain or assembled with stopper + tubes. */
    public static final DeferredBlock<GasWashingBottleBlock> GAS_WASHING_BOTTLE =
            BLOCKS.registerBlock("gas_washing_bottle", GasWashingBottleBlock::new,
                    props -> props.mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(0.5F, 2.0F)
                            .sound(SoundType.GLASS)
                            .noOcclusion()
                            .noLootTable()
                            .pushReaction(PushReaction.DESTROY));

    /** Long-stem funnel (长颈漏斗): a plain placeable glass funnel. */
    public static final DeferredBlock<Block> LONG_STEM_FUNNEL =
            BLOCKS.registerBlock("long_stem_funnel", Block::new,
                    props -> props.mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(0.5F, 2.0F)
                            .sound(SoundType.GLASS)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY));

    /** Separatory funnel (分液漏斗): piston open/closed and stopper on/off states. */
    public static final DeferredBlock<SeparatoryFunnelBlock> SEPARATORY_FUNNEL =
            BLOCKS.registerBlock("separatory_funnel", SeparatoryFunnelBlock::new,
                    props -> props.mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(0.5F, 2.0F)
                            .sound(SoundType.GLASS)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY));

    /** Chemistry tripod (三脚架): invisible block drawn by {@code TripodRenderer}. */
    public static final DeferredBlock<TripodBlock> TRIPOD =
            BLOCKS.registerBlock("tripod", TripodBlock::new,
                    props -> props.mapColor(MapColor.METAL)
                            .strength(0.6F, 2.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY));

    /** Vessel placed directly on the ground (drawn by its block-entity renderer). */
    public static final DeferredBlock<PlacedVesselBlock> PLACED_VESSEL =
            BLOCKS.registerBlock("placed_vessel", PlacedVesselBlock::new,
                    props -> props.mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(0.4F, 1.0F)
                            .sound(SoundType.GLASS)
                            .noOcclusion()
                            .noLootTable()
                            .pushReaction(PushReaction.DESTROY));

    /** Invisible one-block segment of a rubber tube; drawn by a block-entity renderer. */
    public static final DeferredBlock<RubberTubeLinkBlock> RUBBER_TUBE_LINK = BLOCKS.registerBlock("rubber_tube_link",
            RubberTubeLinkBlock::new,
            props -> props.mapColor(MapColor.COLOR_YELLOW)
                    .noCollision()
                    .strength(0.2F)
                    .noLootTable()
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    private ModBlocks() {
    }
}
