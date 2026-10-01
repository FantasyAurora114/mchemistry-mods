package com.example.chemistry.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
public final class SingleNeckRenderer {
    private SingleNeckRenderer() { }
    public static void draw(PoseStack pose,SubmitNodeCollector collector,VesselVisualState visual,int light) {
        var model=visual.groundJoint()?ModStandaloneModels.singleGround():ModStandaloneModels.vessel(1);
        if(model!=null)collector.submitBlockModel(pose,CabinetGlassLayer.TYPE,model,1,1,1,light,OverlayTexture.NO_OVERLAY,0);
        VesselContentRenderer.draw(pose,collector,1,visual,light);
    }
}
