package com.example.chemistry.client;

import com.example.chemistry.blockentity.TestTubeRackBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;

/** 试管架渲染：先画木架，再在第一个槽位画试管（正置/倒置，带内容物颜色）。 */
public class TestTubeRackRenderer
        implements BlockEntityRenderer<TestTubeRackBlockEntity, TestTubeRackRenderState> {

    public TestTubeRackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public TestTubeRackRenderState createRenderState() {
        return new TestTubeRackRenderState();
    }

    @Override
    public void extractRenderState(TestTubeRackBlockEntity blockEntity,
            TestTubeRackRenderState renderState, float partialTick,
            net.minecraft.world.phys.Vec3 cameraPosition,
            @org.jetbrains.annotations.Nullable
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick,
                cameraPosition, breakProgress);
        for (int i = 0; i < 5; i++) {
            ItemStack tube = blockEntity.getTube(i);
            renderState.hasTube[i] = !tube.isEmpty();
            renderState.dewar[i]=tube.getItem() instanceof com.example.chemistry.item.TestTubeItem t&&t.isDewar();
            renderState.inverted[i] = blockEntity.isInverted(i);
            renderState.color[i] = com.example.chemistry.item.LabVesselItem.contentsColor(tube);
        }
    }

    @Override
    public void submit(TestTubeRackRenderState renderState, PoseStack poseStack,
            SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        BlockStateModel rack = ModStandaloneModels.testTubeRack();
        if (rack != null) {
            nodeCollector.submitBlockModel(poseStack, RenderType.cutout(), rack,
                    1.0F, 1.0F, 1.0F, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        // 与铁架台共用同一套试管模型（带玻璃口沿的正式试管）与内容物模型。
        BlockStateModel tube = ModStandaloneModels.part(0, ModStandaloneModels.PART_TUBE);
        BlockStateModel contents = ModStandaloneModels.part(0, ModStandaloneModels.PART_CONTENTS);
        if (tube == null) {
            return;
        }
        double[] xs = {3.0, 5.5, 8.0, 10.5, 13.0};
        for (int i = 0; i < 5; i++) {
            if (!renderState.hasTube[i]) {
                continue;
            }
            poseStack.pushPose();
            if (renderState.inverted[i]) {
                // 倒置：瓶口朝下套在前排凸起柱上。绕 X 转 180° 会把 z 镜像，
                // 所以枢轴 z 要取「目标 z + 模型中心 z」，镜像后才落在柱子上。
                poseStack.translate((xs[i] - 8.8333) / 16.0, 0.24, (6.25 + 8.8333) / 16.0);
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
                poseStack.translate(0.0F, -12.0F / 16.0F, 0.0F);
            } else {
                // 正立：底部坐在底座上，管身穿过顶部孔洞（孔中心 z≈9.5）。
                poseStack.translate((xs[i] - 8.8333) / 16.0, (1.0 - 7.0) / 16.0,
                        (9.5 - 8.8333) / 16.0);
            }
            nodeCollector.submitBlockModel(poseStack, CabinetGlassLayer.TYPE, ModStandaloneModels.testTube(0,renderState.dewar[i]),
                    1.0F, 1.0F, 1.0F, renderState.lightCoords,
                    OverlayTexture.NO_OVERLAY, 0);
            int c = renderState.color[i];
            // 空试管不画内容物（contentsColor 为 0xFFFFFF 表示无内容），
            // 否则会显示一块白色/灰色的填充把试管下半截盖住。
            if (contents != null && c != 0xFFFFFF) {
                nodeCollector.submitBlockModel(poseStack,
                        CabinetGlassLayer.TYPE, contents,
                        ((c >> 16) & 0xFF) / 255.0F, ((c >> 8) & 0xFF) / 255.0F,
                        (c & 0xFF) / 255.0F, renderState.lightCoords,
                        OverlayTexture.NO_OVERLAY, 0);
            }
            poseStack.popPose();
        }
    }
}
