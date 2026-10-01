package com.example.chemistry.client;

import com.example.chemistry.blockentity.PlacedVesselBlockEntity;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.item.DropperItem;
import com.example.chemistry.item.LabVesselItem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Draws a vessel placed on the ground with its liquid-coloured contents. */
public class PlacedVesselRenderer
        implements BlockEntityRenderer<PlacedVesselBlockEntity, PlacedVesselRenderState> {

    public PlacedVesselRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public PlacedVesselRenderState createRenderState() {
        return new PlacedVesselRenderState();
    }

    @Override
    public void extractRenderState(PlacedVesselBlockEntity blockEntity, PlacedVesselRenderState renderState,
            float partialTick, net.minecraft.world.phys.Vec3 cameraPosition,
            @org.jetbrains.annotations.Nullable
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick,
                cameraPosition, breakProgress);
        ItemStack vessel = blockEntity.getVessel();
        renderState.vesselType = vesselType(vessel);renderState.watchGlass=com.example.chemistry.organic.OrganicApparatus.covered(vessel);
        renderState.color = LabVesselItem.contentsColor(vessel);
        renderState.vesselVisual = VesselVisualState.of(vessel);
        renderState.garden=com.example.chemistry.garden.ChemicalGarden.stems(vessel);
        renderState.vesselSealed = VesselHeating.isSealed(vessel);
        renderState.vesselStoppers = VesselHeating.neckStopperMask(vessel);
        for (int i = 0; i < 3; i++) {
            renderState.rubberHoles[i] = VesselHeating.rubberHoles(vessel, i);
        }
        renderState.attached1 = attachedType(blockEntity.getAttached1());
        renderState.attached2 = attachedType(blockEntity.getAttached2());
    }

    @Override
    public void submit(PlacedVesselRenderState renderState, PoseStack poseStack,
            SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (renderState.vesselType == 0) {
            return;
        }
        if (renderState.vesselType == 1) {
                SingleNeckRenderer.draw(poseStack, nodeCollector, renderState.vesselVisual, renderState.lightCoords);
            } else if (renderState.vesselType == 2) {
            ErlenmeyerRenderer.draw(poseStack, nodeCollector,
                    renderState.vesselVisual, renderState.lightCoords);
        } else {
            BlockStateModel model = ModStandaloneModels.vessel(renderState.vesselType);
            if (model != null) {
                nodeCollector.submitBlockModel(poseStack, (renderState.vesselType == 6 || renderState.vesselType == 5 || renderState.vesselType >= 8) ? CabinetGlassLayer.TYPE : RenderType.cutout(), model,
                        1.0F, 1.0F, 1.0F, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            }
            VesselContentRenderer.draw(poseStack, nodeCollector, renderState.vesselType,
                    renderState.vesselVisual, renderState.lightCoords);
        }
        WatchGlassRenderer.draw(poseStack,nodeCollector,renderState.vesselType,renderState.watchGlass,renderState.lightCoords);
        GardenRenderer.draw(poseStack,nodeCollector,renderState.garden,renderState.vesselType,renderState.lightCoords,renderState.vesselVisual.liquidFill());
        if (renderState.vesselSealed) {
            if (renderState.vesselType == 6) {
                int instrumentNeck = -1;
                for (int n = 0; n < 3; n++) {
                    if (renderState.rubberHoles[n] > 0) {
                        instrumentNeck = n;
                        break;
                    }
                }
                for (int n = 0; n < 3; n++) {
                    if (renderState.rubberHoles[n] == 0) {
                        continue;
                    }
                    poseStack.pushPose();
                    double[] neck = VesselHeating.THREE_NECK[n];
                    poseStack.translate(neck[0] / 16.0F, neck[1] / 16.0F, 8.5F / 16.0F);
                    if (n == 0) {
                        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(com.example.chemistry.ThreeNeckGeometry.SIDE_ANGLE));
                    } else if (n == 2) {
                        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-com.example.chemistry.ThreeNeckGeometry.SIDE_ANGLE));
                    }
                    submitStopper(renderState, poseStack, nodeCollector, n == instrumentNeck);
                    poseStack.popPose();
                }
            } else {
                poseStack.pushPose();
                poseStack.translate(8.5F / 16.0F, (float) VesselHeating.mouthTopY(renderState.vesselType) / 16.0F, 8.5F / 16.0F);
                submitStopper(renderState, poseStack, nodeCollector, true);
                poseStack.popPose();
            }
        }
        drawNeckStoppers(renderState.vesselStoppers, poseStack, nodeCollector,
                renderState.lightCoords);
    }

    private void submitStopper(PlacedVesselRenderState rs, PoseStack poseStack,
            SubmitNodeCollector collector, boolean withInstruments) {
        BlockStateModel stopper = ModStandaloneModels.vesselStopper();
        if (stopper != null) {
            collector.submitBlockModel(poseStack, RenderType.cutout(), stopper,
                    1.0F, 1.0F, 1.0F, rs.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        if (withInstruments && rs.attached1 != 0) {
            BlockStateModel m = ModStandaloneModels.attachedModel(rs.attached1);
            if (m != null) {
                poseStack.pushPose();
                poseStack.translate(0.0F, 0.0F,
                        (rs.attached2 != 0 ? -0.55F : 0.0F) / 16.0F);
                collector.submitBlockModel(poseStack, RenderType.cutout(), m,
                        1.0F, 1.0F, 1.0F, rs.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
        }
        if (withInstruments && rs.attached2 != 0) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, 0.55F / 16.0F);
            BlockStateModel m = ModStandaloneModels.attachedModel(rs.attached2);
            if (m != null) {
                collector.submitBlockModel(poseStack, RenderType.cutout(), m,
                        1.0F, 1.0F, 1.0F, rs.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            }
            poseStack.popPose();
        }
    }

    /** 在“当前 pose”（已按挂载点缩放/平移）里画出三颈瓶每个已塞瓶口的玻璃塞。 */
    public static void drawNeckStoppers(int mask, PoseStack poseStack,
            SubmitNodeCollector nodeCollector, int lightCoords) {
        if (mask == 0) {
            return;
        }
        BlockStateModel plug = ModStandaloneModels.glassStopperPlug();
        if (plug == null) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            if ((mask & (1 << i)) == 0) {
                continue;
            }
            double[] neck = VesselHeating.THREE_NECK[i];
            poseStack.pushPose();
            poseStack.translate(neck[0] / 16.0, neck[1] / 16.0, 8.5 / 16.0);
            // 两侧颈向外张开 35°，瓶塞跟着倾斜（中间颈不动）。
            if (i == 0) {
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(com.example.chemistry.ThreeNeckGeometry.SIDE_ANGLE));
            } else if (i == 2) {
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-com.example.chemistry.ThreeNeckGeometry.SIDE_ANGLE));
            }
            nodeCollector.submitBlockModel(poseStack, RenderType.cutout(), plug,
                    1.0F, 1.0F, 1.0F, lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    private static int vesselType(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.equals("thermometer")) {
            return 7;
        }
        return VesselHeating.vesselType(stack);
    }

    private static int attachedType(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.getItem() instanceof DropperItem) {
            return 3;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return switch (path) {
            case "straight_glass_tube" -> 1;
            case "straight_glass_tube_long" -> 8;
            case "right_angle_glass_tube" -> 2;
            case "right_angle_glass_tube_long" -> 4;
            case "long_stem_funnel" -> 5;
            case "separatory_funnel" -> 6;
            default -> 0;
        };
    }
}
