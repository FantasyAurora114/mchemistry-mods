package com.example.chemistry.client;

import com.example.chemistry.VesselHeating;
import com.example.chemistry.blockentity.WaterTroughBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Draws the inverted gas bottle inside the water trough (排水法集气) plus a
 *  water level that drops as gas displaces it. The trough base/water/nozzle
 *  are still rendered from the blockstate model. */
public class WaterTroughRenderer
        implements BlockEntityRenderer<WaterTroughBlockEntity, WaterTroughRenderState> {

    private static final float MOUTH_Y = 3.2F / 16.0F;

    public WaterTroughRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public WaterTroughRenderState createRenderState() {
        return new WaterTroughRenderState();
    }

    @Override
    public void extractRenderState(WaterTroughBlockEntity blockEntity, WaterTroughRenderState renderState,
            float partialTick, net.minecraft.world.phys.Vec3 cameraPosition,
            @org.jetbrains.annotations.Nullable
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick,
                cameraPosition, breakProgress);
        renderState.hasBottle = blockEntity.hasBottle();
        renderState.waterFill = blockEntity.hasBottle()
                ? blockEntity.getWaterMl() / (float) WaterTroughBlockEntity.CAPACITY_ML : 0.0F;
        net.minecraft.world.item.ItemStack flask = blockEntity.getFlask();
        renderState.vesselType = vesselType(flask);
        renderState.color = com.example.chemistry.item.LabVesselItem.contentsColor(flask);
    }

    @Override
    public void submit(WaterTroughRenderState renderState, PoseStack poseStack,
            SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (!renderState.hasBottle) {
            if (renderState.vesselType == 0) {
                return;
            }
        } else {
            BlockStateModel bottle = ModStandaloneModels.gasBottleInverted();
            if (bottle != null) {
                poseStack.pushPose();
                poseStack.translate(0.0F, MOUTH_Y, 0.0F);
                nodeCollector.submitBlockModel(poseStack, RenderType.cutout(), bottle,
                        1.0F, 1.0F, 1.0F, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                if (renderState.waterFill > 0.001F) {
                    BlockStateModel water = ModStandaloneModels.bottleWater();
                    if (water != null) {
                        poseStack.pushPose();
                        poseStack.scale(1.0F, renderState.waterFill, 1.0F);
                        nodeCollector.submitBlockModel(poseStack, RenderType.translucentMovingBlock(), water,
                                0.55F, 0.78F, 1.0F, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                        poseStack.popPose();
                    }
                }
                poseStack.popPose();
            }
        }
        // 冰浴中浸泡的烧瓶/锥形瓶：用半透明渲染层，避免被半透明冰块盖住。
        if (renderState.vesselType != 0) {
            poseStack.pushPose();
            double s = 0.7;
            poseStack.translate((8.5 - 8.5 * s) / 16.0, 1.0 / 16.0,
                    (8.5 - 8.5 * s) / 16.0);
            poseStack.scale((float) s, (float) s, (float) s);
            if (renderState.vesselType == 2) {
                ErlenmeyerRenderer.draw(poseStack, nodeCollector,
                        ModStandaloneModels.vessel(2),
                        ModStandaloneModels.erlenmeyerBodyUnit(),
                        ModStandaloneModels.erlenmeyerLiquidUnit(),
                        renderState.color, renderState.lightCoords,
                        RenderType.translucentMovingBlock());
            } else {
                BlockStateModel flask = ModStandaloneModels.vessel(renderState.vesselType);
                if (flask != null) {
                    nodeCollector.submitBlockModel(poseStack,
                            RenderType.translucentMovingBlock(), flask,
                            1.0F, 1.0F, 1.0F, renderState.lightCoords,
                            OverlayTexture.NO_OVERLAY, 0);
                }
                int c = renderState.color;
                if (c != 0xFFFFFF) {
                    BlockStateModel contents = ModStandaloneModels.vesselContents(renderState.vesselType);
                    if (contents != null) {
                        nodeCollector.submitBlockModel(poseStack,
                                RenderType.translucentMovingBlock(), contents,
                                ((c >> 16) & 0xFF) / 255.0F, ((c >> 8) & 0xFF) / 255.0F,
                                (c & 0xFF) / 255.0F, renderState.lightCoords,
                                OverlayTexture.NO_OVERLAY, 0);
                    }
                }
            }
            poseStack.popPose();
        }
    }

    private static int vesselType(net.minecraft.world.item.ItemStack stack) {
        return VesselHeating.vesselType(stack);
    }
}
