package com.example.chemistry.client;
import com.example.chemistry.titration.*;
import com.example.chemistry.filtration.Filtration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
public final class BuretteRenderer extends EntityRenderer<BuretteEntity,BuretteRenderer.State>{
    public static final class State extends EntityRenderState{float yaw;boolean open;String id;double volume;int color;final ItemStackRenderState receiver=new ItemStackRenderState();}
    public BuretteRenderer(EntityRendererProvider.Context c){super(c);}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(BuretteEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.yaw=e.getYRot();s.open=e.open();s.id=BuiltInRegistries.ITEM.getKey(e.device().getItem()).getPath();s.volume=Filtration.liquidVolume(e.device());s.color=VesselVisualState.of(e.device()).liquidColor();s.receiver.clear();net.minecraft.client.Minecraft.getInstance().getItemModelResolver().updateForTopItem(s.receiver,e.receiver(),ItemDisplayContext.NONE,e.level(),null,0);}
    @Override public void submit(State s,PoseStack p,SubmitNodeCollector out,CameraRenderState camera){
        p.pushPose();p.mulPose(Axis.YP.rotationDegrees(-s.yaw));
        if(!s.receiver.isEmpty()){
            var b=s.receiver.getModelBoundingBox();float fit=(float)Math.min(.35/Math.max(.01,b.getXsize()),Math.min(.30/Math.max(.01,b.getYsize()),.35/Math.max(.01,b.getZsize())));
            p.pushPose();p.translate(.03125,.09,-.19);p.scale(fit,fit,fit);p.translate(-(b.minX+b.maxX)/2,-b.minY,-(b.minZ+b.maxZ)/2);s.receiver.submit(p,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);p.popPose();
        }
        p.translate(.03125,.42,-.19);p.scale(BuretteEntity.SCALE,BuretteEntity.SCALE,BuretteEntity.SCALE);p.translate(-.5,0,-.5);
        draw(p,out,s,"burette_clamp",false);
        if(s.volume>0){
            p.pushPose();p.translate(.5,8.25/16,.5);p.scale(1.6F/16,(float)(21.45*Math.min(1,s.volume/50)/16),1.6F/16);
            var liquid=ElectricalModels.get("burette_liquid_unit");if(liquid!=null)out.submitBlockModel(p,CabinetGlassLayer.TYPE,liquid,((s.color>>16)&255)/255F,((s.color>>8)&255)/255F,(s.color&255)/255F,s.lightCoords,OverlayTexture.NO_OVERLAY,0);p.popPose();
        }
        draw(p,out,s,s.id+"_glass",true);
        p.pushPose();if(s.open){p.translate(8.15/16,5.4/16,.5);p.mulPose(Axis.XP.rotationDegrees(90));p.translate(-8.15/16,-5.4/16,-.5);}draw(p,out,s,s.id+"_valve",true);p.popPose();p.popPose();
    }
    private static void draw(PoseStack p,SubmitNodeCollector out,State s,String id,boolean glass){var m=ElectricalModels.get(id);if(m!=null)out.submitBlockModel(p,glass?CabinetGlassLayer.TYPE:RenderType.cutout(),m,1,1,1,s.lightCoords,OverlayTexture.NO_OVERLAY,0);}
}
