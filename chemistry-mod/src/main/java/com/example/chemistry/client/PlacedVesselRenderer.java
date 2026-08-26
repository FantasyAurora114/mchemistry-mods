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
        renderState.vesselType = vesselType(vessel);
        renderState.color = LabVesselItem.contentsColor(vessel);
        renderState.vesselSealed = VesselHeating.isSealed(vessel);
        renderState.attached1 = attachedType(blockEntity.getAttached1());
        renderState.attached2 = attachedType(blockEntity.getAttached2());
    }

    @Override
    public void submit(PlacedVesselRenderState renderState, PoseStack poseStack,
            SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (renderState.vesselType == 0) {
            return;
        }
        BlockStateModel model = ModStandaloneModels.vessel(renderState.vesselType);
        if (model != null) {
            nodeCollector.submitBlockModel(poseStack, RenderType.cutout(), model,
                    1.0F, 1.0F, 1.0F, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        int c = renderState.color;
        if (c != 0xFFFFFF) {
            BlockStateModel contents = ModStandaloneModels.vesselContents(renderState.vesselType);
            if (contents != null) {
                nodeCollector.submitBlockModel(poseStack, RenderType.cutout(), contents,
                        ((c >> 16) & 0xFF) / 255.0F, ((c >> 8) & 0xFF) / 255.0F, (c & 0xFF) / 255.0F,
                        renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            }
        }
        if (renderState.vesselSealed) {
            poseStack.pushPose();
            poseStack.translate(8.5F / 16.0F, 9.0F / 16.0F, 8.5F / 16.0F);
            BlockStateModel stopper = ModStandaloneModels.vesselStopper();
            if (stopper != null) {
                nodeCollector.submitBlockModel(poseStack, RenderType.cutout(), stopper,
                        1.0F, 1.0F, 1.0F, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            }
            if (renderState.attached1 != 0) {
                BlockStateModel m = ModStandaloneModels.attachedModel(renderState.attached1);
                if (m != null) {
                    poseStack.pushPose();
                    poseStack.translate(0.0F, 0.0F,
                            (renderState.attached2 != 0 ? -0.55F : 0.0F) / 16.0F);
                    nodeCollector.submitBlockModel(poseStack, RenderType.cutout(), m,
                            1.0F, 1.0F, 1.0F, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                    poseStack.popPose();
                }
            }
            if (renderState.attached2 != 0) {
                poseStack.pushPose();
                poseStack.translate(0.0F, 0.0F, 0.55F / 16.0F);
                BlockStateModel m = ModStandaloneModels.attachedModel(renderState.attached2);
                if (m != null) {
                    nodeCollector.submitBlockModel(poseStack, RenderType.cutout(), m,
                            1.0F, 1.0F, 1.0F, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                }
                poseStack.popPose();
            }
            poseStack.popPose();
        }
    }

    private static int vesselType(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return switch (path) {
            case "round_bottom_flask" -> 1;
            case "erlenmeyer_flask" -> 2;
            case "crucible" -> 3;
            case "evaporating_dish" -> 4;
            default -> 0;
        };
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
            case "right_angle_glass_tube" -> 2;
            case "right_angle_glass_tube_long" -> 4;
            case "long_stem_funnel" -> 5;
            case "separatory_funnel" -> 6;
            default -> 0;
        };
    }
}
