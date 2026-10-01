package com.example.chemistry.client;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.blockentity.IronStandBlockEntity;
import com.example.chemistry.client.goggle.ChemGoggleOverlayRenderer;
import com.example.chemistry.data.Liquids;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModBlockEntities;
import com.example.chemistry.registry.ModBlocks;
import com.example.chemistry.registry.ModEntities;
import com.example.chemistry.registry.ModFluidTypes;
import com.example.chemistry.registry.ModFluids;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.resources.ResourceLocation;

@EventBusSubscriber(modid = ChemistryMod.MODID, value = Dist.CLIENT)
public class ModClientEvents {
    @SubscribeEvent
    public static void glassBuffers(net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent event) {
        // Deferred transparent batches flush after opaque benches/items have written their depth.
        event.registerRenderBuffer(CabinetGlassLayer.TYPE);
        event.registerRenderBuffer(CabinetGlassLayer.ITEM);
    }
    @SubscribeEvent
    public static void glassTypes(net.neoforged.neoforge.client.event.RegisterNamedRenderTypesEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath("mchemistry","lab_glass"),
                ChunkSectionLayer.TRANSLUCENT,CabinetGlassLayer.ITEM);
    }


    @SubscribeEvent
    public static void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
        ModStandaloneModels.register(event);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SALT_BRIDGE.get(),SaltBridgeRenderer::new);
        event.registerEntityRenderer(ModEntities.WATER_MACHINE.get(),WaterMachineRenderer::new);
        event.registerEntityRenderer(ModEntities.UTILITY_LINE.get(),ElectricWireRenderer::new);
        event.registerEntityRenderer(ModEntities.IEC_PLUG.get(),IECPlugRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GAS_APPLIANCE.get(), GasApplianceRenderer::new);
        event.registerEntityRenderer(ModEntities.PLACED_REAGENT_BOTTLE.get(), PlacedReagentBottleRenderer::new);
        event.registerEntityRenderer(ModEntities.FILTER_FUNNEL.get(), FilterFunnelRenderer::new);
        event.registerEntityRenderer(ModEntities.BURETTE.get(),BuretteRenderer::new);
        event.registerEntityRenderer(ModEntities.SEPARATORY_FUNNEL.get(),SeparatoryFunnelRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.REAGENT_CABINET.get(), ReagentCabinetRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LABORATORY_BENCH.get(),LaboratoryBenchRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RUBBER_TUBE_LINK.get(), RubberTubeLinkRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.IRON_STAND.get(), IronStandRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TRIPOD.get(), TripodRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PLACED_VESSEL.get(), PlacedVesselRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WATER_TROUGH.get(), WaterTroughRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GAS_COLLECTING_BOTTLE.get(),
                GasCollectingBottleRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HEATING_MANTLE.get(),
                HeatingMantleRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TEST_TUBE_RACK.get(),
                TestTubeRackRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ASSEMBLY_FRAME.get(),
                AssemblyFrameRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PLACED_GRADUATED_CYLINDER.get(),
                PlacedGraduatedCylinderRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MAGNETIC_STIRRER.get(),
                MagneticStirrerRenderer::new);
        event.registerEntityRenderer(ModEntities.RUBBER_TUBE.get(), RubberTubeRenderer::new);
        event.registerEntityRenderer(ModEntities.GRADUATED_CYLINDER.get(),
                GraduatedCylinderEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MAGNETIC_STIRRER.get(),
                MagneticStirrerEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.PLACED_VESSEL.get(),
                PlacedVesselEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.GAS_COLLECTING_BOTTLE.get(),
                GasCollectingBottleEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.IRON_STAND.get(),
                IronStandEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ELECTRO_DEVICE.get(), ElectroDeviceRenderer::new);
        event.registerEntityRenderer(ModEntities.ELECTRIC_WIRE.get(), ElectricWireRenderer::new);
        event.registerEntityRenderer(ModEntities.THERMOMETER_SLEEVE.get(), ThermometerSleeveRenderer::new);
        event.registerEntityRenderer(ModEntities.DISTILLATION_PART.get(),
                DistillationPartEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ALCOHOL_LAMP.get(),
                AlcoholLampEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(com.example.chemistry.registry.ModMenus.TALL_CABINET.get(),ReagentCabinetScreen::new);
        event.register(com.example.chemistry.registry.ModMenus.BASE_CABINET.get(),ReagentCabinetScreen::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        ChemGoggleOverlayRenderer.registerGuiLayers(event);
    }

    @SubscribeEvent
    public static void registerFluidTypeExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new ChemistryFluidTypeExtensions(), ModFluidTypes.CHEMICAL_WATER_TYPE.get());
        for (Liquids.Liquid liquid : Liquids.ALL) {
            event.registerFluidType(new LiquidFluidTypeExtensions(liquid.color()),
                    ModFluidTypes.liquidType(liquid.id()).get());
        }
    }

    @SubscribeEvent
    public static void registerBlockExtensions(RegisterClientExtensionsEvent event) {
        event.registerBlock(new InvisibleBlockClientExtensions(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/iron_stand")),
                ModBlocks.IRON_STAND.get());
        event.registerBlock(new InvisibleBlockClientExtensions(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/rubber_tube_side")),
                ModBlocks.RUBBER_TUBE_LINK.get());
        event.registerBlock(new InvisibleBlockClientExtensions(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/tripod")),
                ModBlocks.TRIPOD.get());
        event.registerBlock(new InvisibleBlockClientExtensions(
                        ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/funnel")),
                ModBlocks.PLACED_VESSEL.get());
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0 || level == null || pos == null) {
                return 0xFFFFFF;
            }
            if (level.getBlockEntity(pos) instanceof IronStandBlockEntity be) {
                return LabVesselItem.contentsColor(be.getTube());
            }
            return 0xFFFFFF;
        }, ModBlocks.IRON_STAND.get());
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModFluids.CHEMICAL_WATER.get(), ChunkSectionLayer.TRANSLUCENT);
            ItemBlockRenderTypes.setRenderLayer(ModFluids.CHEMICAL_WATER_FLOWING.get(), ChunkSectionLayer.TRANSLUCENT);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.IRON_STAND.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LAB_TABLE.get(), ChunkSectionLayer.SOLID);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ALCOHOL_LAMP.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ALCOHOL_BLOWTORCH.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WATER_TROUGH.get(), ChunkSectionLayer.TRANSLUCENT);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GAS_COLLECTING_BOTTLE.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GAS_WASHING_BOTTLE.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LONG_STEM_FUNNEL.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SEPARATORY_FUNNEL.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.TRIPOD.get(), ChunkSectionLayer.CUTOUT);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PLACED_VESSEL.get(), ChunkSectionLayer.CUTOUT);
        });
    }
}
