package com.example.chemistry.client;
import com.example.chemistry.entity.ThermometerSleeveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class ThermometerSleeveRenderer extends EntityRenderer<ThermometerSleeveEntity,ThermometerSleeveRenderer.State> {
    public static class State extends EntityRenderState { float scale,yaw,tilt,width; boolean filled; }
    public ThermometerSleeveRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(ThermometerSleeveEntity e,State s,float tick) {
        super.extractRenderState(e,s,tick);s.width=e.owner() instanceof com.example.chemistry.entity.DistillationPartEntity part&&part.kind()==com.example.chemistry.entity.DistillationPartEntity.HEAD?2.025F:1F;s.scale=e.scale();s.yaw=e.yaw();s.tilt=e.tilt();s.filled=!e.thermometer().isEmpty();
    }
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera) {
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(s.yaw));pose.mulPose(Axis.ZP.rotationDegrees(s.tilt));
        pose.scale(s.scale,s.scale,s.scale);
        pose.pushPose();pose.scale(s.width,1,s.width);
        var model=ModStandaloneModels.thermometerSleeve();
        if(model!=null)out.submitBlockModel(pose,CabinetGlassLayer.TYPE,model,1,1,1,s.lightCoords,OverlayTexture.NO_OVERLAY,0);
        pose.popPose();
        if(s.filled) {var thermometer=ModStandaloneModels.attachedModel(7);
            if(thermometer!=null)out.submitBlockModel(pose,CabinetGlassLayer.TYPE,thermometer,1,1,1,s.lightCoords,OverlayTexture.NO_OVERLAY,0);
        }
        pose.popPose();
    }
}
