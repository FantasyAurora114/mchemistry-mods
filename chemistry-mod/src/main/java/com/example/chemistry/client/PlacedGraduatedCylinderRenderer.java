package com.example.chemistry.client;

import com.example.chemistry.blockentity.PlacedGraduatedCylinderBlockEntity;
import com.example.chemistry.item.GraduatedCylinderItem;
import com.example.chemistry.item.DropperHelper;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;

/** 放下的量筒：画量筒外壳 + 半透明液体（按填充量缩放）。 */
public class PlacedGraduatedCylinderRenderer
        implements BlockEntityRenderer<PlacedGraduatedCylinderBlockEntity,
                PlacedGraduatedCylinderRenderState> {

    public PlacedGraduatedCylinderRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public PlacedGraduatedCylinderRenderState createRenderState() {
        return new PlacedGraduatedCylinderRenderState();
    }

    @Override
    public void extractRenderState(PlacedGraduatedCylinderBlockEntity blockEntity,
            PlacedGraduatedCylinderRenderState renderState, float partialTick,
            net.minecraft.world.phys.Vec3 cameraPosition,
            @org.jetbrains.annotations.Nullable
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick,
                cameraPosition, breakProgress);
        ItemStack cyl = blockEntity.getCylinder();
        String liquid = GraduatedCylinderItem.getLiquid(cyl);
        renderState.fill = liquid != null
                ? (float) (GraduatedCylinderItem.getMl(cyl) / GraduatedCylinderItem.CAPACITY)
                : 0.0F;
        renderState.color = liquid != null
                ? DropperHelper.liquidColor(liquid)
                : 0xFFFFFF;
    }

    @Override
    public void submit(PlacedGraduatedCylinderRenderState renderState, PoseStack poseStack,
            SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        BlockStateModel cyl = ModStandaloneModels.graduatedCylinder();
        if (cyl != null) {
            nodeCollector.submitBlockModel(poseStack, RenderType.cutout(), cyl,
                    1.0F, 1.0F, 1.0F, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        if (renderState.fill <= 0.01F) {
            return;
        }
        BlockStateModel liquid = ModStandaloneModels.graduatedCylinderLiquid();
        if (liquid == null) {
            return;
        }
        int c = renderState.color;
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.3F / 16.0F, 0.0F);
        poseStack.scale(1.0F, Math.max(0.02F, renderState.fill * (13.2F / 16.0F)), 1.0F);
        nodeCollector.submitBlockModel(poseStack, RenderType.translucentMovingBlock(), liquid,
                ((c >> 16) & 0xFF) / 255.0F, ((c >> 8) & 0xFF) / 255.0F,
                (c & 0xFF) / 255.0F, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
