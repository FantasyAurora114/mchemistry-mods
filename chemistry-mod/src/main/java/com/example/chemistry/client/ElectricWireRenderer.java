package com.example.chemistry.client;
import com.example.chemistry.electrical.ElectricWireEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
public class ElectricWireRenderer extends EntityRenderer<ElectricWireEntity,ElectricWireRenderer.State> {
    public static class State extends EntityRenderState {Vec3[] points;int color;}
    public ElectricWireRenderer(EntityRendererProvider.Context c){super(c);}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(ElectricWireEntity e,State s,float p){super.extractRenderState(e,s,p);s.points=e.path();s.color=e.wireColor();}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){if(s.points==null||s.points.length<2)return;var model=ElectricalModels.get("electrical_unit");if(model==null)return;Vec3 origin=new Vec3(s.x,s.y,s.z);
        for(int i=0;i<s.points.length-1;i++){Vec3 a=s.points[i],b=s.points[i+1],d=b.subtract(a);double length=d.length();if(length<1e-6)continue;pose.pushPose();Vec3 local=a.subtract(origin);pose.translate(local.x,local.y,local.z);pose.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees(Math.atan2(d.x,d.z))));pose.mulPose(Axis.XP.rotationDegrees((float)Math.toDegrees(Math.acos(Math.clamp(d.y/length,-1,1)))));pose.scale(.025F,(float)length,.025F);out.submitBlockModel(pose,RenderType.cutout(),model,((s.color>>16)&255)/255F,((s.color>>8)&255)/255F,(s.color&255)/255F,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
    }
}
