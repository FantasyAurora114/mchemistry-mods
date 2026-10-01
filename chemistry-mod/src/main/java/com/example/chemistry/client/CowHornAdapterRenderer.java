package com.example.chemistry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Submits the 25 glass and optional 17 liquid pieces at their authored angles. */
public final class CowHornAdapterRenderer {
    public static void draw(PoseStack pose, SubmitNodeCollector collector,
            int light, boolean flowing) {
        for (int index = 0; index < CowHornParts.PARTS.length; index++) {
            CowHornParts.Part part = CowHornParts.PARTS[index];
            if (part.liquid() && !flowing) continue;
            BlockStateModel model = ModStandaloneModels.cowHornPart(index);
            if (model == null) continue;
            pose.pushPose();
            pose.translate(part.x() / 16.0, part.y() / 16.0, part.z() / 16.0);
            pose.mulPose(Axis.ZP.rotationDegrees(part.angle()));
            pose.translate(-0.5, -0.5, -0.5);
            collector.submitBlockModel(pose, CabinetGlassLayer.TYPE, model,
                    part.liquid() ? 0.56F : 1.0F,
                    part.liquid() ? 0.79F : 1.0F,
                    1.0F, light, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    private CowHornAdapterRenderer() {}
}
