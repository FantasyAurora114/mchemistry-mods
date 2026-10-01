package com.example.chemistry.client;

import com.example.chemistry.entity.GraduatedCylinderEntity;
import com.example.chemistry.item.DropperHelper;
import com.example.chemistry.item.GraduatedCylinderItem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** 放下的量筒（实体版）：画量筒外壳 + 半透明液体（按填充量缩放）。 */
public class GraduatedCylinderEntityRenderer
        extends EntityRenderer<GraduatedCylinderEntity, GraduatedCylinderEntityRenderState> {

    public GraduatedCylinderEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public GraduatedCylinderEntityRenderState createRenderState() {
        return new GraduatedCylinderEntityRenderState();
    }

    @Override
    public void extractRenderState(GraduatedCylinderEntity entity,
            GraduatedCylinderEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        String liquid = entity.getLiquid();
        state.fill = liquid != null
                ? (float) (entity.getMl() / GraduatedCylinderItem.CAPACITY)
                : 0.0F;
        state.color = liquid != null ? DropperHelper.liquidColor(liquid) : 0xFFFFFF;
    }

    @Override
    public void submit(GraduatedCylinderEntityRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState cameraState) {
        // 实体位于方块底面中心，模型是 0..16 方块空间 → 平移到方块角。
        poseStack.pushPose();
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        BlockStateModel cyl = ModStandaloneModels.graduatedCylinder();
        if (cyl != null) {
            collector.submitBlockModel(poseStack, RenderType.cutout(), cyl,
                    1.0F, 1.0F, 1.0F, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        if (state.fill <= 0.01F) {
            poseStack.popPose();
            return;
        }
        BlockStateModel liquid = ModStandaloneModels.graduatedCylinderLiquid();
        if (liquid != null) {
            int c = state.color;
            poseStack.pushPose();
            poseStack.translate(0.0F, 1.3F / 16.0F, 0.0F);
            poseStack.scale(1.0F, Math.max(0.02F, state.fill * (13.2F / 16.0F)), 1.0F);
            collector.submitBlockModel(poseStack, CabinetGlassLayer.TYPE, liquid,
                    ((c >> 16) & 0xFF) / 255.0F, ((c >> 8) & 0xFF) / 255.0F,
                    (c & 0xFF) / 255.0F, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
