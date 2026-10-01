package com.example.chemistry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Renders the user's glass geometry with calibrated, volume-based fill steps. */
public final class ErlenmeyerRenderer {
    private ErlenmeyerRenderer() {}

    public static void draw(PoseStack pose, SubmitNodeCollector collector,
            VesselVisualState visual, int light) {
        draw(pose, collector, visual, light, CabinetGlassLayer.TYPE);
    }

    public static void draw(PoseStack pose, SubmitNodeCollector collector,
            VesselVisualState visual, int light, RenderType shellLayer) {
        BlockStateModel glass = visual.groundJoint()
                ? ModStandaloneModels.erlenmeyerGroundJoint()
                : ModStandaloneModels.vessel(2);
        if (glass != null) {
            collector.submitBlockModel(pose, shellLayer, glass,
                    1, 1, 1, light, OverlayTexture.NO_OVERLAY, 0);
        }
        if (visual.liquidFill() > 0.001f) {
            if(visual.phaseBoundary()>0){
                int top=Math.max(2,Math.min(20,(int)Math.ceil(visual.liquidFill()*20)));
                int bottom=Math.max(1,Math.min(top-1,(int)Math.ceil(visual.phaseBoundary()*20)));
                drawLayer(pose,collector,bottom/20F,visual.bottomLiquidColor(),light);
                drawModel(pose,collector,ElectricalModels.get(String.format(java.util.Locale.ROOT,"erlenmeyer_band_%02d_%02d",bottom,top)),visual.liquidColor(),light);
            }else drawLayer(pose, collector, visual.liquidFill(), visual.liquidColor(), light);
        }
        if (visual.sedimentFill() > 0.001f) {
            drawLayer(pose, collector, visual.sedimentFill(), visual.sedimentColor(), light);
        }
    }

    private static void drawLayer(PoseStack pose, SubmitNodeCollector collector,
            float fill, int color, int light) {
        BlockStateModel model = ModStandaloneModels.erlenmeyerLiquidLevel(fill);
        drawModel(pose,collector,model,color,light);
    }
    private static void drawModel(PoseStack pose,SubmitNodeCollector collector,BlockStateModel model,int color,int light){
        if(model==null)return;
        collector.submitBlockModel(pose, CabinetGlassLayer.TYPE, model,
                ((color >> 16) & 255) / 255.0f,
                ((color >> 8) & 255) / 255.0f,
                (color & 255) / 255.0f,
                light, OverlayTexture.NO_OVERLAY, 0);
    }
}
