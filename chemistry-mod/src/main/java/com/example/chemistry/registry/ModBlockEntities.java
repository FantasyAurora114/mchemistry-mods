package com.example.chemistry.registry;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.blockentity.IronStandBlockEntity;
import com.example.chemistry.blockentity.PlacedTestTubeBlockEntity;
import com.example.chemistry.blockentity.RubberTubeLinkBlockEntity;
import com.example.chemistry.blockentity.GasCollectingBottleBlockEntity;
import com.example.chemistry.blockentity.GasWashingBottleBlockEntity;
import com.example.chemistry.blockentity.TripodBlockEntity;
import com.example.chemistry.blockentity.PlacedVesselBlockEntity;
import com.example.chemistry.blockentity.WaterTroughBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ChemistryMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlacedTestTubeBlockEntity>> PLACED_TEST_TUBE =
            BLOCK_ENTITY_TYPES.register("placed_test_tube",
                    () -> new BlockEntityType<>(PlacedTestTubeBlockEntity::new, ModBlocks.PLACED_TEST_TUBE.get()));

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

    private ModBlockEntities() {
    }
}
