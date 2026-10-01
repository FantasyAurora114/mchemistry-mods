package com.example.chemistry.client;
import com.example.chemistry.electrical.SaltBridgeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
public final class SaltBridgeRenderer extends EntityRenderer<SaltBridgeEntity,SaltBridgeRenderer.State>{
    public static class State extends EntityRenderState {Vec3[] points;boolean filled;}
    public SaltBridgeRenderer(EntityRendererProvider.Context c){super(c);}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(SaltBridgeEntity e,State s,float p){super.extractRenderState(e,s,p);s.points=e.path();s.filled=e.grams()>0;}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){
        var unit=ElectricalModels.get("electrical_unit");if(unit==null||s.points==null)return;
        for(int i=0;i<s.points.length-1;i++){Vec3 a=s.points[i].subtract(new Vec3(s.x,s.y,s.z)),d=s.points[i+1].subtract(s.points[i]);double len=d.length();if(len<1e-6)continue;
            pose.pushPose();pose.translate(a.x,a.y,a.z);pose.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees(Math.atan2(d.x,d.z))));pose.mulPose(Axis.XP.rotationDegrees((float)Math.toDegrees(Math.acos(d.y/len))));
            pose.pushPose();pose.scale(.065F,(float)len,.065F);out.submitBlockModel(pose,CabinetGlassLayer.TYPE,unit,.7F,.85F,.9F,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
            if(s.filled){pose.scale(.025F,(float)len,.025F);out.submitBlockModel(pose,RenderType.cutout(),unit,.88F,.9F,.78F,s.lightCoords,OverlayTexture.NO_OVERLAY,0);}pose.popPose();
        }
    }
}
