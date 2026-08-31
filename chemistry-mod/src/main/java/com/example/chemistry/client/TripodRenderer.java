package com.example.chemistry.client;

import com.example.chemistry.block.TripodBlock;
import com.example.chemistry.blockentity.TripodBlockEntity;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the tripod at 3/4 scale: a perfect equilateral triangle (three 6-unit
 * bars) and three legs at 75 degrees to the ground, plus an optional clay
 * triangle on top and an alcohol lamp underneath. Minecraft block models
 * cannot rotate by 60/15 degrees, so every bar is drawn with arbitrary
 * pose-stack rotations.
 */
public class TripodRenderer implements BlockEntityRenderer<TripodBlockEntity, TripodRenderState> {

    private static final double R = 2.0 * Math.sqrt(3.0); // circumradius of side-6 triangle

    // Side midpoints (x, z) and Y-rotation so the +X bar points along the side.
    private static final double[][] SIDES = {
            {6.5, 8.0 + R / 4.0, 120.0},          // A-B
            {8.0, 8.0 - R / 2.0, 0.0},            // B-C
            {9.5, 8.0 + R / 4.0, -120.0},         // C-A
    };

    // Triangle vertices (x, z) and outward radial direction (dx, dz).
    private static final double[][] VERTICES = {
            {8.0, 8.0 + R, 0.0, 1.0},
            {8.0 - 3.0, 8.0 - R / 2.0, -0.8660254, -0.5},
            {8.0 + 3.0, 8.0 - R / 2.0, 0.8660254, -0.5},
    };

    private static final double SCALE = 0.75;

    private final BlockRenderDispatcher blockRenderer;

    public TripodRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.blockRenderDispatcher();
    }

    @Override
    public TripodRenderState createRenderState() {
        return new TripodRenderState();
    }

    @Override
    public void extractRenderState(TripodBlockEntity blockEntity, TripodRenderState renderState,
            float partialTick, net.minecraft.world.phys.Vec3 cameraPosition,
            @org.jetbrains.annotations.Nullable
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick,
                cameraPosition, breakProgress);
        var state = blockEntity.getBlockState();
        renderState.hasClayTriangle = state.getValue(TripodBlock.HAS_CLAY_TRIANGLE);
        renderState.hasLamp = state.getValue(TripodBlock.HAS_LAMP);
        renderState.lampLit = state.getValue(TripodBlock.LAMP_LIT);
        renderState.vesselType = vesselType(blockEntity.getVessel());
        renderState.vesselColor = LabVesselItem.contentsColor(blockEntity.getVessel());
    }

    @Override
    public void submit(TripodRenderState renderState, PoseStack poseStack,
            SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        BlockStateModel bar = ModStandaloneModels.tripodBar();
        BlockStateModel leg = ModStandaloneModels.tripodLeg();
        // Scale the tripod (and the clay triangle on it) around the ground centre.
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale((float) SCALE, (float) SCALE, (float) SCALE);
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        // Top triangle: three bars around the block centre at y 10.67.
        for (double[] side : SIDES) {
            poseStack.pushPose();
            poseStack.translate((float) (side[0] / 16.0), 10.67F / 16.0F, (float) (side[1] / 16.0));
            poseStack.mulPose(Axis.YP.rotationDegrees((float) side[2]));
            render(bar, poseStack, nodeCollector, renderState);
            poseStack.popPose();
        }
        // Legs: hang down from the vertices, tilted 15 degrees outward.
        for (double[] v : VERTICES) {
            double dx = v[2];
            double dz = v[3];
            double phi = Math.toDegrees(Math.atan2(-dz, dx));
            poseStack.pushPose();
            poseStack.translate((float) (v[0] / 16.0), 10.42F / 16.0F, (float) (v[1] / 16.0));
            poseStack.mulPose(Axis.YP.rotationDegrees((float) phi));
            poseStack.mulPose(Axis.ZP.rotationDegrees(15.0F));
            render(leg, poseStack, nodeCollector, renderState);
            poseStack.popPose();
        }
        // Clay triangle: one unit on each side of the top triangle.
        if (renderState.hasClayTriangle) {
            BlockStateModel clay = ModStandaloneModels.clayTriangleUnit();
            for (double[] side : SIDES) {
                poseStack.pushPose();
                poseStack.translate((float) (side[0] / 16.0), 10.67F / 16.0F, (float) (side[1] / 16.0));
                poseStack.mulPose(Axis.YP.rotationDegrees((float) side[2]));
                render(clay, poseStack, nodeCollector, renderState);
                poseStack.popPose();
            }
        }
        // Crucible / evaporating dish on the clay triangle (centre x/z 7.5/8.5).
        if (renderState.vesselType != 0) {
            poseStack.pushPose();
            poseStack.translate((8.0 - 7.5) / 16.0, 11.17F / 16.0F, (8.0 - 8.5) / 16.0);
            if (renderState.vesselType == 2) {
                ErlenmeyerRenderer.draw(poseStack, nodeCollector,
                        ModStandaloneModels.vessel(2),
                        ModStandaloneModels.erlenmeyerBodyUnit(),
                        ModStandaloneModels.erlenmeyerLiquidUnit(),
                        renderState.vesselColor, renderState.lightCoords);
            } else {
                render(ModStandaloneModels.vessel(renderState.vesselType),
                        poseStack, nodeCollector, renderState, 1.0F, 1.0F, 1.0F);
                int c = renderState.vesselColor;
                if (c != 0xFFFFFF) {
                    render(ModStandaloneModels.vesselContents(renderState.vesselType),
                            poseStack, nodeCollector, renderState,
                            ((c >> 16) & 0xFF) / 255.0F, ((c >> 8) & 0xFF) / 255.0F,
                            (c & 0xFF) / 255.0F);
                }
            }
            poseStack.popPose();
        }
        poseStack.popPose();

        // Alcohol lamp underneath (not scaled).
        if (renderState.hasLamp) {
            BlockStateModel lamp = blockRenderer.getBlockModel(
                    ModBlocks.ALCOHOL_LAMP.get().defaultBlockState()
                            .setValue(com.example.chemistry.block.AlcoholLampBlock.LIT, renderState.lampLit));
            render(lamp, poseStack, nodeCollector, renderState);
        }
    }

    private static int vesselType(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return switch (path) {
            case "crucible" -> 3;
            case "evaporating_dish" -> 4;
            default -> 0;
        };
    }

    private static void render(BlockStateModel model, PoseStack poseStack,
            SubmitNodeCollector nodeCollector, TripodRenderState renderState) {
        if (model == null) {
            return;
        }
        nodeCollector.submitBlockModel(poseStack, RenderType.cutout(), model,
                1.0F, 1.0F, 1.0F, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
    }

    private static void render(BlockStateModel model, PoseStack poseStack,
            SubmitNodeCollector nodeCollector, TripodRenderState renderState,
            float r, float g, float b) {
        if (model == null) {
            return;
        }
        nodeCollector.submitBlockModel(poseStack, RenderType.cutout(), model,
                r, g, b, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
    }
}
