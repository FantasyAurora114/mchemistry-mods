package com.example.chemistry.client;

import com.example.chemistry.blockentity.HeatingMantleBlockEntity;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.VesselHeating;
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

/** 在加热套外壳里绘制放入的烧瓶（圆底/锥形瓶，带内容物颜色）。 */
public class HeatingMantleRenderer
        implements BlockEntityRenderer<HeatingMantleBlockEntity, HeatingMantleRenderState> {

    public HeatingMantleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public HeatingMantleRenderState createRenderState() {
        return new HeatingMantleRenderState();
    }

    @Override
    public void extractRenderState(HeatingMantleBlockEntity blockEntity,
            HeatingMantleRenderState renderState, float partialTick,
            net.minecraft.world.phys.Vec3 cameraPosition,
            @org.jetbrains.annotations.Nullable
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick,
                cameraPosition, breakProgress);
        ItemStack flask = blockEntity.getFlask();
        renderState.vesselType = vesselType(flask);
        renderState.color = LabVesselItem.contentsColor(flask);
        renderState.vesselVisual = VesselVisualState.of(flask);
        renderState.vesselStoppers = VesselHeating.neckStopperMask(flask);
    }

    @Override
    public void submit(HeatingMantleRenderState renderState, PoseStack poseStack,
            SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        int type = renderState.vesselType;
        if (type == 0) {
            return;
        }
        poseStack.pushPose();
        // 烧瓶坐在加热套内胆里（内胆底部约 y=2），缩小 0.7 倍。
        double s = 0.7;
        poseStack.translate((8.5 - 8.5 * s) / 16.0, 2.5 / 16.0,
                (8.5 - 8.5 * s) / 16.0);
        poseStack.scale((float) s, (float) s, (float) s);
        if (type == 1) {
            SingleNeckRenderer.draw(poseStack,nodeCollector,renderState.vesselVisual,renderState.lightCoords);
        } else if (type == 2) {
            ErlenmeyerRenderer.draw(poseStack, nodeCollector,
                    renderState.vesselVisual, renderState.lightCoords);
        } else {
            // 圆底烧瓶(1)/三颈烧瓶(6)/平底烧瓶(7)各自用对应模型。
            int vesselType = type;
            BlockStateModel model = ModStandaloneModels.vessel(vesselType);
            if (model != null) {
                nodeCollector.submitBlockModel(poseStack, (vesselType==5||vesselType>=8||vesselType==6)?CabinetGlassLayer.TYPE:RenderType.cutout(), model,
                        1.0F, 1.0F, 1.0F, renderState.lightCoords,
                        OverlayTexture.NO_OVERLAY, 0);
            }
            VesselContentRenderer.draw(poseStack, nodeCollector, vesselType,
                    renderState.vesselVisual, renderState.lightCoords);
            if (renderState.vesselStoppers != 0) {
                PlacedVesselRenderer.drawNeckStoppers(renderState.vesselStoppers,
                        poseStack, nodeCollector, renderState.lightCoords);
            }
        }
        poseStack.popPose();
    }

    private static int vesselType(ItemStack stack) {
        return VesselHeating.vesselType(stack);
    }
}
