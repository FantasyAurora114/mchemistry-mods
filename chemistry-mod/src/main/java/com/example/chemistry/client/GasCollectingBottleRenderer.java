package com.example.chemistry.client;

import com.example.chemistry.block.GasCollectingBottleBlock;
import com.example.chemistry.blockentity.GasCollectingBottleBlockEntity;
import com.example.chemistry.data.GasJars;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Draws the collected-gas fill inside a placed gas collecting bottle. The
 *  glass bottle itself stays a blockstate model; this renderer only adds the
 *  tinted fill, growing from the bottom (upright / 向上排气法) or from the top
 *  of the bottle (inverted / 向下排气法) as gas flows in. */
public class GasCollectingBottleRenderer
        implements BlockEntityRenderer<GasCollectingBottleBlockEntity, GasCollectingBottleRenderState> {

    public GasCollectingBottleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public GasCollectingBottleRenderState createRenderState() {
        return new GasCollectingBottleRenderState();
    }

    @Override
    public void extractRenderState(GasCollectingBottleBlockEntity blockEntity,
            GasCollectingBottleRenderState renderState, float partialTick,
            net.minecraft.world.phys.Vec3 cameraPosition,
            @org.jetbrains.annotations.Nullable
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick,
                cameraPosition, breakProgress);
        int fillMl = blockEntity.getFillMl();
        renderState.fill = fillMl > 0
                ? fillMl / (float) GasCollectingBottleBlockEntity.CAPACITY_ML : 0.0F;
        renderState.gasColor = 0xFFFFFF;
        String gasId = blockEntity.getGasId();
        for (GasJars.GasJar gas : GasJars.ALL) {
            if (gas.id().equals(gasId)) {
                renderState.gasColor = gas.color();
                break;
            }
        }
        renderState.inverted = blockEntity.getBlockState()
                .getValue(GasCollectingBottleBlock.INVERTED);
    }

    @Override
    public void submit(GasCollectingBottleRenderState renderState, PoseStack poseStack,
            SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        float fill = renderState.fill;
        if (fill <= 0.001F) {
            return;
        }
        BlockStateModel fillModel = ModStandaloneModels.gasBottleFill();
        if (fillModel == null) {
            return;
        }
        int c = renderState.gasColor;
        float r = ((c >> 16) & 0xFF) / 255.0F;
        float g = ((c >> 8) & 0xFF) / 255.0F;
        float b = (c & 0xFF) / 255.0F;
        poseStack.pushPose();
        if (renderState.inverted) {
            // Bottle body spans y 2..7; gas enters at the mouth (bottom) and
            // rises, so the fill clings to the top of the body and grows down.
            poseStack.translate(0.0F, (6.8F - 5.0F * fill) / 16.0F, 0.0F);
        } else {
            poseStack.translate(0.0F, 0.2F / 16.0F, 0.0F);
        }
        poseStack.scale(1.0F, fill, 1.0F);
        nodeCollector.submitBlockModel(poseStack, RenderType.translucentMovingBlock(),
                fillModel, r, g, b, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
