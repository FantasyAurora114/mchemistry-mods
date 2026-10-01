package com.example.chemistry.client;

import com.example.chemistry.data.GasJars;
import com.example.chemistry.entity.GasCollectingBottleEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** 集气瓶（实体版）：按状态选瓶子模型 + 染色的气体填充。 */
public class GasCollectingBottleEntityRenderer
        extends EntityRenderer<GasCollectingBottleEntity, GasCollectingBottleEntityRenderState> {

    public GasCollectingBottleEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public GasCollectingBottleEntityRenderState createRenderState() {
        return new GasCollectingBottleEntityRenderState();
    }

    @Override
    public void extractRenderState(GasCollectingBottleEntity entity,
            GasCollectingBottleEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.lift=entity.nozzleLift();
        state.yaw=entity.getYRot();
        state.inverted = entity.isInverted();
        state.hasPlate = entity.hasPlate();
        state.hasNozzle = entity.hasNozzle();
        state.tubeType = entity.getTubeType();
        int fillMl = entity.getFillMl();
        state.water=entity.waterMl()/(float)GasCollectingBottleEntity.CAPACITY_ML;
        state.fill = fillMl > 0 ? fillMl / (float) GasCollectingBottleEntity.CAPACITY_ML : 0.0F;
        state.gasColor = 0xFFFFFF;
        String gasId = entity.getGasId();
        for (GasJars.GasJar gas : GasJars.ALL) {
            if (gas.id().equals(gasId)) {
                state.gasColor = gas.color();
                break;
            }
        }
    }

    @Override
    public void submit(GasCollectingBottleEntityRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.translate(0,state.lift,0);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-state.yaw));
        poseStack.scale(.58F,.58F,.58F);
        poseStack.translate(-8.5/16.0, 0, -8.5/16.0);
        BlockStateModel bottle = ModStandaloneModels.gasBottle(
                state.inverted, state.hasPlate, state.hasNozzle);
        if (bottle != null) {
            collector.submitBlockModel(poseStack, CabinetGlassLayer.TYPE, bottle,
                    1.0F, 1.0F, 1.0F, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        if(state.hasNozzle)GasCollectingBottleRenderer.drawTube(poseStack,collector,state.lightCoords,state.tubeType,state.inverted);
        if(state.water>.001F){
            var waterModel=ModStandaloneModels.gasBottleWaterFill();
            poseStack.pushPose();
            poseStack.translate(0,.65/16,0);
            poseStack.scale(1,state.water,1);
            collector.submitBlockModel(poseStack,CabinetGlassLayer.TYPE,waterModel,.56F,.79F,1F,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
            poseStack.popPose();
        }
        float fill = state.fill;
        if (fill > 0.001F) {
            BlockStateModel fillModel = ModStandaloneModels.gasBottleFill();
            if (fillModel != null) {
                int c = state.gasColor;
                float r = ((c >> 16) & 0xFF) / 255.0F;
                float g = ((c >> 8) & 0xFF) / 255.0F;
                float b = (c & 0xFF) / 255.0F;
                poseStack.pushPose();
                if (state.inverted) {
                    poseStack.translate(0.0F, (5.65F - 5.0F * fill) / 16.0F, 0.0F);
                } else {
                    poseStack.translate(0.0F, 0.65F / 16.0F, 0.0F);
                }
                poseStack.scale(1.0F, fill, 1.0F);
                collector.submitBlockModel(poseStack, CabinetGlassLayer.TYPE,
                        fillModel, r, g, b, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
    }
}
