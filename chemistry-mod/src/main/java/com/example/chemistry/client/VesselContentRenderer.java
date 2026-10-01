package com.example.chemistry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Shared fill and settled-solid rendering for the other vessel models. */
public final class VesselContentRenderer {
    private static final double[][] BOUNDS = {
            {0, 0}, {0.18, 6.3}, {0, 0}, {0.2, 5.2}, {0.2, 1.5},
            {.78, 7.45}, {com.example.chemistry.ThreeNeckGeometry.LIQUID_BOTTOM, com.example.chemistry.ThreeNeckGeometry.LIQUID_TOP}, {0.6, 6.4}, {.78,10.15}, {.78,13.65}, {.4,7.0}};

    public static void draw(PoseStack pose, SubmitNodeCollector collector,
            int vesselType, VesselVisualState visual, int light) {
        if (vesselType < 1 || vesselType >= BOUNDS.length || vesselType == 2) return;
        BlockStateModel model = ModStandaloneModels.vesselContents(vesselType);
        if (model == null) return;
        if (visual.liquidFill() > visual.sedimentFill()) {
            if(visual.phaseBoundary()>visual.sedimentFill())layer(pose,collector,ModStandaloneModels.vesselLiquid(vesselType),vesselType,
                    visual.sedimentFill(),visual.phaseBoundary(),visual.bottomLiquidColor(),light,true);
            layer(pose, collector, ModStandaloneModels.vesselLiquid(vesselType), vesselType,
                    Math.max(visual.sedimentFill(),visual.phaseBoundary()), visual.liquidFill(),
                    visual.liquidColor(), light, true);
        }
        if (visual.sedimentFill() > 0) {
            layer(pose, collector, model, vesselType, 0, visual.sedimentFill(),
                    visual.sedimentColor(), light, false);
        }
    }

    private static void layer(PoseStack pose, SubmitNodeCollector collector,
            BlockStateModel model, int type, float bottom, float top,
            int color, int light, boolean liquid) {
        float height = top - bottom;
        double base = BOUNDS[type][0];
        double fullHeight = BOUNDS[type][1] - base;
        pose.pushPose();
        pose.translate(0, (base + fullHeight * bottom - base * height) / 16.0, 0);
        pose.scale(1, height, 1);
        collector.submitBlockModel(pose, liquid?CabinetGlassLayer.TYPE:RenderType.cutout(), model,
                ((color >> 16) & 255) / 255.0f,
                ((color >> 8) & 255) / 255.0f,
                (color & 255) / 255.0f,
                light, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    private VesselContentRenderer() {}
}
