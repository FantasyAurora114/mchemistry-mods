package com.example.chemistry.client;

import com.example.chemistry.blockentity.MagneticStirrerBlockEntity;
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

/** 磁力搅拌机：外壳 + 烧瓶 + 搅拌子。 */
public class MagneticStirrerRenderer
        implements BlockEntityRenderer<MagneticStirrerBlockEntity, MagneticStirrerRenderState> {

    public MagneticStirrerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public MagneticStirrerRenderState createRenderState() {
        return new MagneticStirrerRenderState();
    }

    @Override
    public void extractRenderState(MagneticStirrerBlockEntity blockEntity,
            MagneticStirrerRenderState renderState, float partialTick,
            net.minecraft.world.phys.Vec3 cameraPosition,
            @org.jetbrains.annotations.Nullable
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick,
                cameraPosition, breakProgress);
        renderState.facing = blockEntity.getBlockState()
                .getValue(com.example.chemistry.block.MagneticStirrerBlock.FACING);
        ItemStack flask = blockEntity.getFlask();
        renderState.vesselType = vesselType(flask);
        renderState.color = LabVesselItem.contentsColor(flask);
        renderState.vesselVisual = VesselVisualState.of(flask);
        renderState.vesselStoppers = VesselHeating.neckStopperMask(flask);
        renderState.hasBar = blockEntity.hasStirBar();
    }

    @Override
    public void submit(MagneticStirrerRenderState renderState, PoseStack poseStack,
            SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        BlockStateModel stirrer = ModStandaloneModels.magneticStirrer();
        if (stirrer != null) {
            // 模型默认面板朝南，按朝向绕方块中心旋转（与 blockstate 的 y 旋转一致）。
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(com.mojang.math.Axis.YP
                    .rotationDegrees(180.0F - renderState.facing.toYRot()));
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            nodeCollector.submitBlockModel(poseStack, RenderType.cutout(), stirrer,
                    1.0F, 1.0F, 1.0F, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        if (renderState.vesselType != 0) {
            poseStack.pushPose();
            double s = 0.5;
            poseStack.translate((8.5 - 8.5 * s) / 16.0, 5.8 / 16.0,
                    (8.5 - 8.5 * s) / 16.0);
            poseStack.scale((float) s, (float) s, (float) s);
            if (renderState.vesselType == 1) {
                SingleNeckRenderer.draw(poseStack, nodeCollector, renderState.vesselVisual, renderState.lightCoords);
            } else if (renderState.vesselType == 2) {
                ErlenmeyerRenderer.draw(poseStack, nodeCollector,
                        renderState.vesselVisual, renderState.lightCoords);
            } else {
                int vesselType = renderState.vesselType;
                BlockStateModel model = ModStandaloneModels.vessel(vesselType);
                if (model != null) {
                    nodeCollector.submitBlockModel(poseStack, RenderType.cutout(), model,
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
        // 搅拌子：画在底盘上方。
        if (renderState.hasBar) {
            BlockStateModel bar = ModStandaloneModels.stirBar();
            if (bar != null) {
                poseStack.pushPose();
                poseStack.translate((8.5 - 4.0) / 16.0, 6.1 / 16.0, 0.0);
                poseStack.scale(0.5F, 0.5F, 0.5F);
                nodeCollector.submitBlockModel(poseStack, RenderType.cutout(), bar,
                        1.0F, 1.0F, 1.0F, renderState.lightCoords,
                        OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
        }
    }

    private static int vesselType(ItemStack stack) {
        return VesselHeating.vesselType(stack);
    }
}
