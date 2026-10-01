package com.example.chemistry.registry;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.block.AlcoholLampBlock;
import com.example.chemistry.block.IronStandBlock;
import com.example.chemistry.block.RubberTubeLinkBlock;
import com.example.chemistry.block.WaterTroughBlock;
import com.example.chemistry.block.GasCollectingBottleBlock;
import com.example.chemistry.block.GasWashingBottleBlock;
import com.example.chemistry.block.SeparatoryFunnelBlock;
import com.example.chemistry.block.TripodBlock;
import com.example.chemistry.block.HeatingMantleBlock;
import com.example.chemistry.block.TestTubeRackBlock;
import com.example.chemistry.block.AssemblyFrameBlock;
import com.example.chemistry.block.PlacedGraduatedCylinderBlock;
import com.example.chemistry.block.MagneticStirrerBlock;
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

    public static final DeferredBlock<com.example.chemistry.block.GasApplianceBlock> BUNSEN_BURNER = BLOCKS.registerBlock("bunsen_burner", p -> new com.example.chemistry.block.GasApplianceBlock(p, 0), p -> p.strength(1.5F).sound(SoundType.METAL).noOcclusion().noLootTable());
    public static final DeferredBlock<com.example.chemistry.block.GasApplianceBlock> GAS_CYLINDER_SMALL = BLOCKS.registerBlock("gas_cylinder_small", p -> new com.example.chemistry.block.GasApplianceBlock(p, 1), p -> p.strength(2.5F).sound(SoundType.METAL).noOcclusion().noLootTable());
    public static final DeferredBlock<com.example.chemistry.block.GasApplianceBlock> GAS_CYLINDER_TALL = BLOCKS.registerBlock("gas_cylinder_tall", p -> new com.example.chemistry.block.GasApplianceBlock(p, 2), p -> p.strength(2.5F).sound(SoundType.METAL).noOcclusion().noLootTable());

    public static final DeferredBlock<com.example.chemistry.block.ReagentCabinetBlock> TALL_REAGENT_CABINET = BLOCKS.registerBlock("tall_reagent_cabinet", p -> new com.example.chemistry.block.ReagentCabinetBlock(p,true), p -> p.strength(2.5f).sound(SoundType.METAL).noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<com.example.chemistry.block.ReagentCabinetBlock> BASE_REAGENT_CABINET = BLOCKS.registerBlock("base_reagent_cabinet", p -> new com.example.chemistry.block.ReagentCabinetBlock(p,false), p -> p.strength(2.5f).sound(SoundType.METAL).noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK));

    public static final DeferredBlock<com.example.chemistry.radiation.ShieldedStorageBlock> RADIATION_SHIELD_BOX=BLOCKS.registerBlock("radiation_shield_box",com.example.chemistry.radiation.ShieldedStorageBlock::new,p->p.strength(3F).sound(SoundType.METAL).noLootTable());
    public static final DeferredBlock<com.example.chemistry.radiation.ShieldedStorageBlock> LEAD_LINED_CABINET=BLOCKS.registerBlock("lead_lined_cabinet",com.example.chemistry.radiation.ShieldedStorageBlock::new,p->p.strength(3F).sound(SoundType.METAL).noLootTable());
    public static final DeferredBlock<Block> LAB_CEILING_TILE=BLOCKS.registerBlock("lab_ceiling_tile",Block::new,p->p.strength(1.5F,6F).sound(SoundType.STONE));
    public static final DeferredBlock<com.example.chemistry.organic.LabCeilingLightBlock> LAB_CEILING_LIGHT=BLOCKS.registerBlock("lab_ceiling_light",com.example.chemistry.organic.LabCeilingLightBlock::new,p->p.strength(1F).sound(SoundType.GLASS).noOcclusion().lightLevel(s->s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)?15:0));
    public static final DeferredBlock<Block> LAB_FLOOR_TILE=BLOCKS.registerBlock("lab_floor_tile",Block::new,p->p.strength(1.5F,6F).sound(SoundType.STONE));
    public static final DeferredBlock<Block> LAB_WALL_TILE=BLOCKS.registerBlock("lab_wall_tile",Block::new,p->p.strength(1.5F,6F).sound(SoundType.STONE));

    public static final DeferredBlock<LiquidBlock> CHEMICAL_WATER = BLOCKS.registerBlock("chemical_water",
            props -> new LiquidBlock(ModFluids.CHEMICAL_WATER.get(), props),
            props -> props.mapColor(MapColor.WATER)
                    .replaceable()
                    .noCollision()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable()
                    .liquid());

    public static final DeferredBlock<IronStandBlock> IRON_STAND = BLOCKS.registerBlock("iron_stand",
            IronStandBlock::new,
            props -> props.mapColor(MapColor.METAL)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .noLootTable()
                    .pushReaction(PushReaction.DESTROY));

    /** Preserve the old registry ID so placed benches become the new A variant. */
    public static final DeferredBlock<com.example.chemistry.block.LaboratoryBenchBlock> LAB_TABLE = BLOCKS.registerBlock("lab_table",
            p -> new com.example.chemistry.block.LaboratoryBenchBlock(p,0),
            p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(2.0F,6.0F).sound(SoundType.METAL).noOcclusion().noLootTable());
    public static final DeferredBlock<com.example.chemistry.block.LaboratoryBenchBlock> LAB_TABLE_CABINET = BLOCKS.registerBlock("lab_table_cabinet",
            p -> new com.example.chemistry.block.LaboratoryBenchBlock(p,1),
            p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(2.0F,6.0F).sound(SoundType.METAL).noOcclusion().noLootTable());
    public static final DeferredBlock<com.example.chemistry.block.LaboratoryBenchBlock> LAB_TABLE_SINK = BLOCKS.registerBlock("lab_table_sink",
            p -> new com.example.chemistry.block.LaboratoryBenchBlock(p,2),
            p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(2.0F,6.0F).sound(SoundType.METAL).noOcclusion().noLootTable());

    /** Alcohol lamp (酒精灯): unlit / lit / capped states, heats test tubes. */
    public static final DeferredBlock<AlcoholLampBlock> ALCOHOL_LAMP = BLOCKS.registerBlock("alcohol_lamp",
            AlcoholLampBlock::new,
            props -> props.mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.0F, 2.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> state.getValue(AlcoholLampBlock.LIT) ? 15 : 0)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    /** Alcohol blowtorch (酒精喷灯): reaches 1200 C, heats twice as fast. */
    public static final DeferredBlock<AlcoholLampBlock> ALCOHOL_BLOWTORCH = BLOCKS.registerBlock(
            "alcohol_blowtorch",
            props -> new AlcoholLampBlock(props, AlcoholLampBlock.Kind.BLOWTORCH),
            props -> props.mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.0F, 2.0F)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(AlcoholLampBlock.LIT) ? 15 : 0)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<com.example.chemistry.block.DeepWaterTroughBlock> DEEP_WATER_TROUGH = BLOCKS.registerBlock("deep_water_trough",com.example.chemistry.block.DeepWaterTroughBlock::new,p->p.strength(1.5F,3F).sound(SoundType.GLASS).noOcclusion().noLootTable());

    /** Water trough (水槽): empty / water-filled states. */
    public static final DeferredBlock<WaterTroughBlock> WATER_TROUGH = BLOCKS.registerBlock("water_trough",
            WaterTroughBlock::new,
            props -> props.mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.5F, 3.0F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .dynamicShape()
                    .forceSolidOn()
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

    /** Heating mantle (加热套): holds a flask, heats toward a set temperature. */
    public static final DeferredBlock<HeatingMantleBlock> HEATING_MANTLE =
            BLOCKS.registerBlock("heating_mantle", HeatingMantleBlock::new,
                    props -> props.mapColor(MapColor.COLOR_LIGHT_GRAY)
                            .strength(0.8F, 3.0F)
                            .sound(SoundType.STONE)
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

    /** Test tube rack (试管架). */
    public static final DeferredBlock<TestTubeRackBlock> TEST_TUBE_RACK =
            BLOCKS.registerBlock("test_tube_rack", TestTubeRackBlock::new,
                    props -> props.mapColor(MapColor.WOOD)
                            .strength(0.6F, 2.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion().dynamicShape()
                            .noLootTable()
                            .pushReaction(PushReaction.DESTROY));

    /** 机架块：2×3 通用框架（第 3 步）。 */
    public static final DeferredBlock<AssemblyFrameBlock> ASSEMBLY_FRAME =
            BLOCKS.registerBlock("assembly_frame", AssemblyFrameBlock::new,
                    props -> props.mapColor(MapColor.METAL)
                            .strength(1.0F, 3.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .noLootTable()
                            .pushReaction(PushReaction.DESTROY));

    /** Placed graduated cylinder (量筒). */
    public static final DeferredBlock<PlacedGraduatedCylinderBlock> PLACED_GRADUATED_CYLINDER =
            BLOCKS.registerBlock("placed_graduated_cylinder", PlacedGraduatedCylinderBlock::new,
                    props -> props.mapColor(MapColor.NONE)
                            .strength(0.3F, 1.0F)
                            .noOcclusion()
                            .noLootTable()
                            .pushReaction(PushReaction.DESTROY));

    /** Magnetic stirrer (磁力搅拌机). */
    public static final DeferredBlock<MagneticStirrerBlock> MAGNETIC_STIRRER =
            BLOCKS.registerBlock("magnetic_stirrer", MagneticStirrerBlock::new,
                    props -> props.mapColor(MapColor.COLOR_LIGHT_GRAY)
                            .strength(0.8F, 3.0F)
                            .sound(SoundType.METAL)
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
