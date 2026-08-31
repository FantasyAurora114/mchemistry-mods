package com.example.chemistry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Draws the Erlenmeyer flask (锥形瓶) with a perfectly smooth truncated-cone
 * body. MC block models only allow boxes, so the taper is rendered as many
 * thin scaled unit-box slices (visually smooth). The glass body is drawn
 * between the base top and the neck bottom; the liquid fill (tinted) between
 * the base top and the liquid level, with flat top/bottom caps.
 */
public final class ErlenmeyerRenderer {

    private static final int SEGMENTS = 24;

    // Model-space geometry (0..16 units, same convention as the vessel models).
    private static final double BODY_BOTTOM = 0.857;   // top of the base box
    private static final double BODY_TOP = 4.714;      // bottom of the neck box
    private static final double BODY_W_BOTTOM = 5.0;
    private static final double BODY_W_TOP = 2.0;
    private static final double LIQUID_TOP = 5.571;    // scaled liquid level
    private static final double LIQUID_W_BOTTOM = 4.5;
    private static final double LIQUID_W_TOP = 1.5;
    private static final double EDGE_W = 0.18;

    private ErlenmeyerRenderer() {
    }

    /** Draw the whole flask (base/neck boxes + smooth taper + tinted liquid)
     *  in the current pose. The pose must already be in the vessel's model
     *  space (0..16 units). */
    public static void draw(PoseStack poseStack, SubmitNodeCollector collector,
            BlockStateModel flask, BlockStateModel bodyUnit,
            BlockStateModel liquidUnit, int color, int lightCoords) {
        draw(poseStack, collector, flask, bodyUnit, liquidUnit, color, lightCoords,
                RenderType.cutout());
    }

    /** 同上，但允许指定渲染层（如放在半透明水槽/冰块里时用 translucent，
     *  否则会被半透明方块盖住）。 */
    public static void draw(PoseStack poseStack, SubmitNodeCollector collector,
            BlockStateModel flask, BlockStateModel bodyUnit,
            BlockStateModel liquidUnit, int color, int lightCoords,
            RenderType renderType) {
        if (flask != null) {
            collector.submitBlockModel(poseStack, renderType, flask,
                    1.0F, 1.0F, 1.0F, lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        // Four smooth corner lines along the taper (no stepped slice seams).
        for (double sx : new double[]{-1.0, 1.0}) {
            for (double sz : new double[]{-1.0, 1.0}) {
                drawCornerLine(poseStack, collector, bodyUnit,
                        8.5 + sx * BODY_W_BOTTOM / 2.0, BODY_BOTTOM,
                        8.5 + sz * BODY_W_BOTTOM / 2.0,
                        8.5 + sx * BODY_W_TOP / 2.0, BODY_TOP,
                        8.5 + sz * BODY_W_TOP / 2.0, lightCoords, renderType);
            }
        }
        if (color != 0xFFFFFF) {
            float r = ((color >> 16) & 0xFF) / 255.0F;
            float g = ((color >> 8) & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            drawFrustum(poseStack, collector, liquidUnit,
                    BODY_BOTTOM, LIQUID_TOP, LIQUID_W_BOTTOM, LIQUID_W_TOP,
                    r, g, b, lightCoords, true, RenderType.translucentMovingBlock());
            // 底座盒区域也要有液体（锥形瓶底部）。
            drawBaseLiquid(poseStack, collector, liquidUnit,
                    r, g, b, lightCoords, RenderType.translucentMovingBlock());
        }
    }

    /** 锥形瓶底部底座盒里的液体（y 0.15..0.8，宽约 4.2）。 */
    private static void drawBaseLiquid(PoseStack poseStack, SubmitNodeCollector collector,
            BlockStateModel unit, float r, float g, float b,
            int lightCoords, RenderType renderType) {
        double w = 4.2;
        poseStack.pushPose();
        poseStack.translate((8.5 - w / 2.0) / 16.0, 0.15 / 16.0,
                (8.5 - w / 2.0) / 16.0);
        poseStack.scale((float) (w / 16.0), (float) (0.65 / 16.0),
                (float) (w / 16.0));
        collector.submitBlockModel(poseStack, renderType, unit,
                r, g, b, lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    /** A thin box aligned along a corner of the frustum (tube-style rotation),
     *  giving one continuous slanted edge line. */
    private static void drawCornerLine(PoseStack poseStack, SubmitNodeCollector collector,
            BlockStateModel unit, double x0, double y0, double z0,
            double x1, double y1, double z1, int lightCoords, RenderType renderType) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double dz = z1 - z0;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-6) {
            return;
        }
        poseStack.pushPose();
        // 从起点出发沿线段方向铺一根细长条（与橡胶管渲染一致）。用中点+
        // 固定下移 0.5 格会把短棱线整体推到瓶底下方，必须用起点。
        poseStack.translate(x0 / 16.0, y0 / 16.0, z0 / 16.0);
        double ndx = dx / len;
        double ndy = dy / len;
        double ndz = dz / len;
        poseStack.mulPose(Axis.YP.rotationDegrees(
                (float) Math.toDegrees(Math.atan2(ndx, ndz))));
        poseStack.mulPose(Axis.XP.rotationDegrees(
                (float) Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, ndy))))));
        poseStack.scale((float) (EDGE_W / 16.0), (float) (len / 16.0),
                (float) (EDGE_W / 16.0));
        collector.submitBlockModel(poseStack, renderType, unit,
                1.0F, 1.0F, 1.0F, lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    /** One thin slice per step: the 0..16 unit box is translated to the
     *  slice's bottom-left corner and scaled to the frustum width/height —
     *  the same pattern as the rubber-tube renderer. Optional flat top/bottom
     *  caps for the liquid surface. */
    private static void drawFrustum(PoseStack poseStack, SubmitNodeCollector collector,
            BlockStateModel unit, double yBottom, double yTop,
            double wBottom, double wTop, float r, float g, float b,
            int lightCoords, boolean caps, RenderType renderType) {
        for (int i = 0; i < SEGMENTS; i++) {
            double t0 = (double) i / SEGMENTS;
            double t1 = (double) (i + 1) / SEGMENTS;
            double y0 = yBottom + (yTop - yBottom) * t0;
            double y1 = yBottom + (yTop - yBottom) * t1;
            double w = wBottom + (wTop - wBottom) * ((t0 + t1) / 2.0);
            poseStack.pushPose();
            poseStack.translate((8.5 - w / 2.0) / 16.0, y0 / 16.0,
                    (8.5 - w / 2.0) / 16.0);
            poseStack.scale((float) (w / 16.0), (float) ((y1 - y0) / 16.0),
                    (float) (w / 16.0));
            collector.submitBlockModel(poseStack, renderType, unit,
                    r, g, b, lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        if (caps) {
            cap(poseStack, collector, unit, yBottom, wBottom, r, g, b, lightCoords, renderType);
            cap(poseStack, collector, unit, yTop, wTop, r, g, b, lightCoords, renderType);
        }
    }

    private static void cap(PoseStack poseStack, SubmitNodeCollector collector,
            BlockStateModel unit, double y, double w, float r, float g, float b,
            int lightCoords, RenderType renderType) {
        poseStack.pushPose();
        poseStack.translate((8.5 - w / 2.0) / 16.0, y / 16.0,
                (8.5 - w / 2.0) / 16.0);
        poseStack.scale((float) (w / 16.0), 0.012F, (float) (w / 16.0));
        collector.submitBlockModel(poseStack, renderType, unit,
                r, g, b, lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
