package com.example.chemistry.client;

import com.example.chemistry.blockentity.AssemblyFrameBlockEntity;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** 机架块 BER：按槽位位置画出每个子件的占位玻璃块。 */
public class AssemblyFrameRenderer
        implements BlockEntityRenderer<AssemblyFrameBlockEntity, AssemblyFrameRenderState> {

    /** 2×3 槽位中心（方块本地 16 分之一格）：x 两列，y 三行。 */
    private static final double[][] SLOT_POS = {
            {4.0, 12.0, 8.0}, {12.0, 12.0, 8.0},
            {4.0, 7.0, 8.0}, {12.0, 7.0, 8.0},
            {4.0, 2.0, 8.0}, {12.0, 2.0, 8.0}};

    private final BlockStateModel placeholder;

    public AssemblyFrameRenderer(BlockEntityRendererProvider.Context context) {
        this.placeholder = context.blockRenderDispatcher().getBlockModel(
                net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState());
    }

    @Override
    public AssemblyFrameRenderState createRenderState() {
        return new AssemblyFrameRenderState();
    }

    @Override
    public void extractRenderState(AssemblyFrameBlockEntity blockEntity,
            AssemblyFrameRenderState renderState, float partialTick,
            net.minecraft.world.phys.Vec3 cameraPosition,
            @org.jetbrains.annotations.Nullable
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick,
                cameraPosition, breakProgress);
        for (int i = 0; i < AssemblyFrameBlockEntity.SLOTS; i++) {
            renderState.hasPart[i] = !blockEntity.getPart(i).isEmpty();
        }
    }

    @Override
    public void submit(AssemblyFrameRenderState renderState, com.mojang.blaze3d.vertex.PoseStack poseStack,
            SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        for (int i = 0; i < AssemblyFrameBlockEntity.SLOTS; i++) {
            if (!renderState.hasPart[i]) {
                continue;
            }
            double[] p = SLOT_POS[i];
            poseStack.pushPose();
            poseStack.translate((p[0] - 2.0) / 16.0, (p[1] - 2.0) / 16.0, (p[2] - 2.0) / 16.0);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            nodeCollector.submitBlockModel(poseStack, RenderType.cutout(), placeholder,
                    1.0F, 1.0F, 1.0F, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}
