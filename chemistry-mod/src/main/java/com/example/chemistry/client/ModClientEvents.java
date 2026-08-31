package com.example.chemistry.client;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.blockentity.IronStandBlockEntity;
import com.example.chemistry.blockentity.PlacedTestTubeBlockEntity;
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
    public static void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
        ModStandaloneModels.register(event);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
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
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
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
            if (level.getBlockEntity(pos) instanceof PlacedTestTubeBlockEntity be) {
                return LabVesselItem.contentsColor(be.getTube());
            }
            return 0xFFFFFF;
        }, ModBlocks.PLACED_TEST_TUBE.get());
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
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PLACED_TEST_TUBE.get(), ChunkSectionLayer.CUTOUT);
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
