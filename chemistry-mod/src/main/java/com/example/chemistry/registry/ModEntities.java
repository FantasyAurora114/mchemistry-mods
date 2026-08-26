package com.example.chemistry.registry;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.entity.RubberTubeEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ChemistryMod.MODID);

    /** Sagging rubber tube connecting two anchors (blocks or entities). */
    public static final DeferredHolder<EntityType<?>, EntityType<RubberTubeEntity>> RUBBER_TUBE =
            ENTITY_TYPES.register("rubber_tube", () -> EntityType.Builder.of(RubberTubeEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "rubber_tube"))));

    private ModEntities() {
    }
}
