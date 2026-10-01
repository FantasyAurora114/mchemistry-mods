package com.example.chemistry.client;

import com.example.chemistry.entity.AlcoholLampEntity;
import com.example.chemistry.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** 酒精灯 / 酒精喷灯（实体版）：用 blockstate 模型 + 灯帽/点燃状态。 */
public class AlcoholLampEntityRenderer
        extends EntityRenderer<AlcoholLampEntity, AlcoholLampEntityRenderState> {

    private final BlockRenderDispatcher blockRenderer;

    public AlcoholLampEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public AlcoholLampEntityRenderState createRenderState() {
        return new AlcoholLampEntityRenderState();
    }

    @Override
    public void extractRenderState(AlcoholLampEntity entity,
            AlcoholLampEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.lit = entity.isLit();
        state.capped = entity.isCapped();
        state.blowtorch = entity.isBlowtorch();
    }

    @Override
    public void submit(AlcoholLampEntityRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState cameraState) {
        BlockStateModel lamp = blockRenderer.getBlockModel(
                (state.blowtorch ? ModBlocks.ALCOHOL_BLOWTORCH.get() : ModBlocks.ALCOHOL_LAMP.get())
                        .defaultBlockState()
                        .setValue(com.example.chemistry.block.AlcoholLampBlock.LIT, state.lit)
                        .setValue(com.example.chemistry.block.AlcoholLampBlock.CAPPED, state.capped)
                        .setValue(com.example.chemistry.block.AlcoholLampBlock.STUCK, false));
        poseStack.pushPose();
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        collector.submitBlockModel(poseStack, RenderType.cutout(), lamp,
                1.0F, 1.0F, 1.0F, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
