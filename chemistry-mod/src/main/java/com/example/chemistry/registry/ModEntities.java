package com.example.chemistry.registry;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.entity.AlcoholLampEntity;
import com.example.chemistry.entity.DistillationPartEntity;
import com.example.chemistry.entity.GasCollectingBottleEntity;
import com.example.chemistry.entity.GraduatedCylinderEntity;
import com.example.chemistry.entity.IronStandEntity;
import com.example.chemistry.entity.MagneticStirrerEntity;
import com.example.chemistry.entity.PlacedVesselEntity;
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

    /** 放下的量筒（技术性实体：无物理、可点选、有虚拟命中框）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<GraduatedCylinderEntity>> GRADUATED_CYLINDER =
            ENTITY_TYPES.register("graduated_cylinder_entity", () -> EntityType.Builder.of(
                            GraduatedCylinderEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.94F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "graduated_cylinder_entity"))));

    /** 磁力搅拌机（技术性实体：无物理、可点选、有虚拟命中框）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<MagneticStirrerEntity>> MAGNETIC_STIRRER =
            ENTITY_TYPES.register("magnetic_stirrer_entity", () -> EntityType.Builder.of(
                            MagneticStirrerEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "magnetic_stirrer_entity"))));

    /** 落地容器（锥形瓶 / 烧瓶 / 三颈瓶 / 烧杯，技术性实体）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<PlacedVesselEntity>> PLACED_VESSEL =
            ENTITY_TYPES.register("placed_vessel_entity", () -> EntityType.Builder.of(
                            PlacedVesselEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.0F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "placed_vessel_entity"))));

    /** 集气瓶（技术性实体）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<GasCollectingBottleEntity>> GAS_COLLECTING_BOTTLE =
            ENTITY_TYPES.register("gas_collecting_bottle_entity", () -> EntityType.Builder.of(
                            GasCollectingBottleEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "gas_collecting_bottle_entity"))));

    /** 铁架台（技术性实体）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<IronStandEntity>> IRON_STAND =
            ENTITY_TYPES.register("iron_stand_entity", () -> EntityType.Builder.of(
                            IronStandEntity::new, MobCategory.MISC)
                    .sized(0.42F, 1.0F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "iron_stand_entity"))));

    /** Individually selectable distillation head, condenser, adapter and thermometer. */
    public static final DeferredHolder<EntityType<?>, EntityType<DistillationPartEntity>> DISTILLATION_PART =
            ENTITY_TYPES.register("distillation_part_entity", () -> EntityType.Builder.of(
                            DistillationPartEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.7F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "distillation_part_entity"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.example.chemistry.entity.ThermometerSleeveEntity>> THERMOMETER_SLEEVE =
            ENTITY_TYPES.register("thermometer_sleeve", () -> EntityType.Builder.of(
                    com.example.chemistry.entity.ThermometerSleeveEntity::new, MobCategory.MISC)
                    .sized(.15F,.35F).clientTrackingRange(10).updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,"thermometer_sleeve"))));

    /** 酒精灯 / 酒精喷灯（技术性实体）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<AlcoholLampEntity>> ALCOHOL_LAMP =
            ENTITY_TYPES.register("alcohol_lamp_entity", () -> EntityType.Builder.of(
                            AlcoholLampEntity::new, MobCategory.MISC)
                    .sized(0.32F, 0.56F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "alcohol_lamp_entity"))));

    public static final DeferredHolder<EntityType<?>,EntityType<com.example.chemistry.electrical.ElectroDeviceEntity>> ELECTRO_DEVICE =
            ENTITY_TYPES.register("electro_device", () -> EntityType.Builder.of(com.example.chemistry.electrical.ElectroDeviceEntity::new,MobCategory.MISC)
                    .sized(1F,1.74F).clientTrackingRange(10).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,"electro_device"))));
    public static final DeferredHolder<EntityType<?>,EntityType<com.example.chemistry.electrical.SaltBridgeEntity>> SALT_BRIDGE =
            ENTITY_TYPES.register("salt_bridge", () -> EntityType.Builder.of(com.example.chemistry.electrical.SaltBridgeEntity::new,MobCategory.MISC)
            .sized(.1F,.1F).clientTrackingRange(10).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,"salt_bridge"))));
    public static final DeferredHolder<EntityType<?>,EntityType<com.example.chemistry.electrical.ElectricWireEntity>> ELECTRIC_WIRE =
            ENTITY_TYPES.register("electric_wire", () -> EntityType.Builder.of(com.example.chemistry.electrical.ElectricWireEntity::new,MobCategory.MISC)
                    .sized(.1F,.1F).clientTrackingRange(12).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,"electric_wire"))));
    public static final DeferredHolder<EntityType<?>,EntityType<com.example.chemistry.filtration.FilterFunnelEntity>> FILTER_FUNNEL =
            ENTITY_TYPES.register("filter_funnel", () -> EntityType.Builder.of(com.example.chemistry.filtration.FilterFunnelEntity::new,MobCategory.MISC)
                    .sized(.5F,1.15F).clientTrackingRange(10).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,"filter_funnel"))));
    public static final DeferredHolder<EntityType<?>,EntityType<com.example.chemistry.entity.PlacedReagentBottleEntity>> PLACED_REAGENT_BOTTLE =
            ENTITY_TYPES.register("placed_reagent_bottle", () -> EntityType.Builder.of(com.example.chemistry.entity.PlacedReagentBottleEntity::new,MobCategory.MISC)
                    .sized(.23F,.4F).clientTrackingRange(10).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,"placed_reagent_bottle"))));
    public static final DeferredHolder<EntityType<?>,EntityType<com.example.chemistry.titration.BuretteEntity>> BURETTE =
            ENTITY_TYPES.register("burette", () -> EntityType.Builder.of(com.example.chemistry.titration.BuretteEntity::new,MobCategory.MISC)
                    .sized(.4F,1.53F).clientTrackingRange(10).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,"burette"))));
    public static final DeferredHolder<EntityType<?>,EntityType<com.example.chemistry.organic.SeparatoryFunnelEntity>> SEPARATORY_FUNNEL =
            ENTITY_TYPES.register("separatory_funnel", () -> EntityType.Builder.of(com.example.chemistry.organic.SeparatoryFunnelEntity::new,MobCategory.MISC)
                    .sized(.44F,1.25F).clientTrackingRange(10).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,"separatory_funnel"))));
    public static final DeferredHolder<EntityType<?>,EntityType<com.example.chemistry.utility.WaterMachineEntity>> WATER_MACHINE =
            ENTITY_TYPES.register("water_machine", () -> EntityType.Builder.of(com.example.chemistry.utility.WaterMachineEntity::new,MobCategory.MISC)
            .sized(.1F,.1F).clientTrackingRange(12).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,"water_machine"))));
    public static final DeferredHolder<EntityType<?>,EntityType<com.example.chemistry.utility.UtilityLineEntity>> UTILITY_LINE =
            ENTITY_TYPES.register("utility_line", () -> EntityType.Builder.of(com.example.chemistry.utility.UtilityLineEntity::new,MobCategory.MISC)
            .sized(.1F,.1F).clientTrackingRange(12).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,"utility_line"))));
    public static final DeferredHolder<EntityType<?>,EntityType<com.example.chemistry.utility.IECPlugEntity>> IEC_PLUG =
            ENTITY_TYPES.register("iec_plug", () -> EntityType.Builder.of(com.example.chemistry.utility.IECPlugEntity::new,MobCategory.MISC)
            .sized(.1F,.1F).clientTrackingRange(12).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,"iec_plug"))));
    private ModEntities() {
    }
}
