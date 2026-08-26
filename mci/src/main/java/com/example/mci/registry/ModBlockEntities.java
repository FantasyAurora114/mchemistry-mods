package com.example.mci.registry;

import com.example.mci.ChemistryMod;
import com.example.mci.blockentity.SynthesisTowerBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ChemistryMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SynthesisTowerBlockEntity>> SYNTHESIS_TOWER =
            BLOCK_ENTITY_TYPES.register("synthesis_tower",
                    () -> new BlockEntityType<>(SynthesisTowerBlockEntity::new,
                            ModBlocks.SYNTHESIS_TOWER.get()));

    private ModBlockEntities() {
    }
}
