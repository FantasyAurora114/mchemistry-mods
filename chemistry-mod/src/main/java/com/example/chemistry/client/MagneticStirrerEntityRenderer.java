package com.example.chemistry.client;

import com.example.chemistry.VesselHeating;
import com.example.chemistry.entity.MagneticStirrerEntity;
import com.example.chemistry.item.LabVesselItem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;

/** 磁力搅拌机（实体版）：外壳 + 烧瓶 + 搅拌子。 */
public class MagneticStirrerEntityRenderer
        extends EntityRenderer<MagneticStirrerEntity, MagneticStirrerEntityRenderState> {

    public MagneticStirrerEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public MagneticStirrerEntityRenderState createRenderState() {
        return new MagneticStirrerEntityRenderState();
    }

    @Override
    public void extractRenderState(MagneticStirrerEntity entity,
            MagneticStirrerEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.facing = entity.getFacing();
        ItemStack flask = entity.getFlask();
        state.vesselType = VesselHeating.vesselType(flask);
        state.color = LabVesselItem.contentsColor(flask);
        state.vesselVisual = VesselVisualState.of(flask);
        state.vesselStoppers = VesselHeating.neckStopperMask(flask);
        state.hasBar = entity.hasStirBar();
    }

    @Override
    public void submit(MagneticStirrerEntityRenderState state, PoseStack poseStack,
            SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        // 实体位于方块底面中心，模型是 0..16 方块空间 → 平移到方块角。
        poseStack.pushPose();
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        BlockStateModel stirrer = ModStandaloneModels.magneticStirrer();
        if (stirrer != null) {
            // 模型默认面板朝南，按朝向绕方块中心旋转（与 blockstate 的 y 旋转一致）。
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(com.mojang.math.Axis.YP
                    .rotationDegrees(180.0F - state.facing.toYRot()));
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            nodeCollector.submitBlockModel(poseStack, RenderType.cutout(), stirrer,
                    1.0F, 1.0F, 1.0F, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        if (state.vesselType != 0) {
            poseStack.pushPose();
            double s = 0.5;
            poseStack.translate((8.5 - 8.5 * s) / 16.0, 5.8 / 16.0,
                    (8.5 - 8.5 * s) / 16.0);
            poseStack.scale((float) s, (float) s, (float) s);
            if (state.vesselType == 1) {
                SingleNeckRenderer.draw(poseStack, nodeCollector, state.vesselVisual, state.lightCoords);
            } else if (state.vesselType == 2) {
                ErlenmeyerRenderer.draw(poseStack, nodeCollector,
                        state.vesselVisual, state.lightCoords);
            } else {
                int vesselType = state.vesselType;
                BlockStateModel model = ModStandaloneModels.vessel(vesselType);
                if (model != null) {
                    nodeCollector.submitBlockModel(poseStack, (vesselType==5||vesselType>=8||vesselType==6)?CabinetGlassLayer.TYPE:RenderType.cutout(), model,
                            1.0F, 1.0F, 1.0F, state.lightCoords,
                            OverlayTexture.NO_OVERLAY, 0);
                }
                VesselContentRenderer.draw(poseStack, nodeCollector, vesselType,
                        state.vesselVisual, state.lightCoords);
                if (state.vesselStoppers != 0) {
                    PlacedVesselRenderer.drawNeckStoppers(state.vesselStoppers,
                            poseStack, nodeCollector, state.lightCoords);
                }
            }
            poseStack.popPose();
        }
        // 搅拌子：画在底盘上方。
        if (state.hasBar) {
            BlockStateModel bar = ModStandaloneModels.stirBar();
            if (bar != null) {
                poseStack.pushPose();
                poseStack.translate((8.5 - 4.0) / 16.0, 6.1 / 16.0, 0.0);
                poseStack.scale(0.5F, 0.5F, 0.5F);
                nodeCollector.submitBlockModel(poseStack, RenderType.cutout(), bar,
                        1.0F, 1.0F, 1.0F, state.lightCoords,
                        OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
    }
}
