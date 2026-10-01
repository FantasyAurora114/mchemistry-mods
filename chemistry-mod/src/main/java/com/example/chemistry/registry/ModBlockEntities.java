package com.example.chemistry.registry;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.blockentity.IronStandBlockEntity;
import com.example.chemistry.blockentity.RubberTubeLinkBlockEntity;
import com.example.chemistry.blockentity.GasCollectingBottleBlockEntity;
import com.example.chemistry.blockentity.GasWashingBottleBlockEntity;
import com.example.chemistry.blockentity.TripodBlockEntity;
import com.example.chemistry.blockentity.HeatingMantleBlockEntity;
import com.example.chemistry.blockentity.TestTubeRackBlockEntity;
import com.example.chemistry.blockentity.AssemblyFrameBlockEntity;
import com.example.chemistry.blockentity.PlacedGraduatedCylinderBlockEntity;
import com.example.chemistry.blockentity.MagneticStirrerBlockEntity;
import com.example.chemistry.blockentity.PlacedVesselBlockEntity;
import com.example.chemistry.blockentity.WaterTroughBlockEntity;
import com.example.chemistry.blockentity.AlcoholLampBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ChemistryMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.example.chemistry.blockentity.GasApplianceBlockEntity>> GAS_APPLIANCE = BLOCK_ENTITY_TYPES.register("gas_appliance", () -> new BlockEntityType<>(com.example.chemistry.blockentity.GasApplianceBlockEntity::new, ModBlocks.BUNSEN_BURNER.get(), ModBlocks.GAS_CYLINDER_SMALL.get(), ModBlocks.GAS_CYLINDER_TALL.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.example.chemistry.blockentity.ReagentCabinetBlockEntity>> REAGENT_CABINET = BLOCK_ENTITY_TYPES.register("reagent_cabinet", () -> new BlockEntityType<>(com.example.chemistry.blockentity.ReagentCabinetBlockEntity::new,ModBlocks.TALL_REAGENT_CABINET.get(),ModBlocks.BASE_REAGENT_CABINET.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.example.chemistry.blockentity.LaboratoryBenchBlockEntity>> LABORATORY_BENCH = BLOCK_ENTITY_TYPES.register("laboratory_bench", () -> new BlockEntityType<>(com.example.chemistry.blockentity.LaboratoryBenchBlockEntity::new,ModBlocks.LAB_TABLE.get(),ModBlocks.LAB_TABLE_CABINET.get(),ModBlocks.LAB_TABLE_SINK.get()));

    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.example.chemistry.radiation.ShieldedStorageBlockEntity>> SHIELDED_STORAGE=BLOCK_ENTITY_TYPES.register("shielded_storage",()->new BlockEntityType<>(com.example.chemistry.radiation.ShieldedStorageBlockEntity::new,ModBlocks.RADIATION_SHIELD_BOX.get(),ModBlocks.LEAD_LINED_CABINET.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IronStandBlockEntity>> IRON_STAND =
            BLOCK_ENTITY_TYPES.register("iron_stand",
                    () -> new BlockEntityType<>(IronStandBlockEntity::new, ModBlocks.IRON_STAND.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RubberTubeLinkBlockEntity>> RUBBER_TUBE_LINK =
            BLOCK_ENTITY_TYPES.register("rubber_tube_link",
                    () -> new BlockEntityType<>(RubberTubeLinkBlockEntity::new, ModBlocks.RUBBER_TUBE_LINK.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasCollectingBottleBlockEntity>> GAS_COLLECTING_BOTTLE =
            BLOCK_ENTITY_TYPES.register("gas_collecting_bottle",
                    () -> new BlockEntityType<>(GasCollectingBottleBlockEntity::new,
                            ModBlocks.GAS_COLLECTING_BOTTLE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TripodBlockEntity>> TRIPOD =
            BLOCK_ENTITY_TYPES.register("tripod",
                    () -> new BlockEntityType<>(TripodBlockEntity::new, ModBlocks.TRIPOD.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatingMantleBlockEntity>> HEATING_MANTLE =
            BLOCK_ENTITY_TYPES.register("heating_mantle",
                    () -> new BlockEntityType<>(HeatingMantleBlockEntity::new,
                            ModBlocks.HEATING_MANTLE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestTubeRackBlockEntity>> TEST_TUBE_RACK =
            BLOCK_ENTITY_TYPES.register("test_tube_rack",
                    () -> new BlockEntityType<>(TestTubeRackBlockEntity::new,
                            ModBlocks.TEST_TUBE_RACK.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AssemblyFrameBlockEntity>> ASSEMBLY_FRAME =
            BLOCK_ENTITY_TYPES.register("assembly_frame",
                    () -> new BlockEntityType<>(AssemblyFrameBlockEntity::new,
                            ModBlocks.ASSEMBLY_FRAME.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlacedGraduatedCylinderBlockEntity>> PLACED_GRADUATED_CYLINDER =
            BLOCK_ENTITY_TYPES.register("placed_graduated_cylinder",
                    () -> new BlockEntityType<>(PlacedGraduatedCylinderBlockEntity::new,
                            ModBlocks.PLACED_GRADUATED_CYLINDER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MagneticStirrerBlockEntity>> MAGNETIC_STIRRER =
            BLOCK_ENTITY_TYPES.register("magnetic_stirrer",
                    () -> new BlockEntityType<>(MagneticStirrerBlockEntity::new,
                            ModBlocks.MAGNETIC_STIRRER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlacedVesselBlockEntity>> PLACED_VESSEL =
            BLOCK_ENTITY_TYPES.register("placed_vessel",
                    () -> new BlockEntityType<>(PlacedVesselBlockEntity::new,
                            ModBlocks.PLACED_VESSEL.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasWashingBottleBlockEntity>> GAS_WASHING_BOTTLE =
            BLOCK_ENTITY_TYPES.register("gas_washing_bottle",
                    () -> new BlockEntityType<>(GasWashingBottleBlockEntity::new,
                            ModBlocks.GAS_WASHING_BOTTLE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaterTroughBlockEntity>> WATER_TROUGH =
            BLOCK_ENTITY_TYPES.register("water_trough",
                    () -> new BlockEntityType<>(WaterTroughBlockEntity::new,
                            ModBlocks.WATER_TROUGH.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlcoholLampBlockEntity>> ALCOHOL_LAMP =
            BLOCK_ENTITY_TYPES.register("alcohol_lamp",
                    () -> new BlockEntityType<>(AlcoholLampBlockEntity::new,
                            ModBlocks.ALCOHOL_LAMP.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlcoholLampBlockEntity>> ALCOHOL_BLOWTORCH =
            BLOCK_ENTITY_TYPES.register("alcohol_blowtorch",
                    () -> new BlockEntityType<>(AlcoholLampBlockEntity::new,
                            ModBlocks.ALCOHOL_BLOWTORCH.get()));

    private ModBlockEntities() {
    }
}
