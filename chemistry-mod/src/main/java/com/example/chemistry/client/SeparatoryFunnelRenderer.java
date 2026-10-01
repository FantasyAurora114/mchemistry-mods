package com.example.chemistry.client;

import com.example.chemistry.organic.*;
import com.example.chemistry.filtration.Filtration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

public final class SeparatoryFunnelRenderer extends EntityRenderer<SeparatoryFunnelEntity,SeparatoryFunnelRenderer.State> {
    public static final class State extends EntityRenderState {float yaw;boolean open,capped;double volume,lower;int lowerColor,upperColor;final ItemStackRenderState receiver=new ItemStackRenderState();}
    public SeparatoryFunnelRenderer(EntityRendererProvider.Context c){super(c);}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(SeparatoryFunnelEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.yaw=e.getYRot();s.open=e.open();s.capped=e.capped();s.volume=Filtration.liquidVolume(e.device());
        var phases=LiquidPhases.read(e.device());var visual=VesselVisualState.of(e.device());s.upperColor=visual.liquidColor();s.lowerColor=visual.bottomLiquidColor();s.lower=phases.separated()?phases.bottom().ml():0;
        s.receiver.clear();net.minecraft.client.Minecraft.getInstance().getItemModelResolver().updateForTopItem(s.receiver,e.receiver(),ItemDisplayContext.NONE,e.level(),null,0);
    }
    @Override public void submit(State s,PoseStack p,SubmitNodeCollector out,CameraRenderState camera){
        p.pushPose();p.mulPose(Axis.YP.rotationDegrees(-s.yaw));
        if(!s.receiver.isEmpty()){
            var b=s.receiver.getModelBoundingBox();float fit=(float)Math.min(.35/Math.max(.01,b.getXsize()),Math.min(.30/Math.max(.01,b.getYsize()),.35/Math.max(.01,b.getZsize())));
            p.pushPose();p.translate(.03125,.09,-.19);p.scale(fit,fit,fit);p.translate(-(b.minX+b.maxX)/2,-b.minY,-(b.minZ+b.maxZ)/2);s.receiver.submit(p,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);p.popPose();
        }
        p.translate(.03125,.46,-.19);p.scale(SeparatoryFunnelEntity.SCALE,SeparatoryFunnelEntity.SCALE,SeparatoryFunnelEntity.SCALE);p.translate(-.5,0,-.5);
        draw(p,out,s,"separatory_clamp",false);
        if(s.volume>0){int top=Math.max(1,Math.min(20,(int)Math.ceil(s.volume/250*20)));
            if(s.lower>0&&top>1){int lower=Math.max(1,Math.min(top-1,(int)Math.ceil(s.lower/250*20)));liquid(p,out,s,String.format(java.util.Locale.ROOT,"separatory_liquid_%02d",lower),s.lowerColor);liquid(p,out,s,String.format(java.util.Locale.ROOT,"separatory_band_%02d_%02d",lower,top),s.upperColor);}
            else liquid(p,out,s,String.format(java.util.Locale.ROOT,"separatory_liquid_%02d",top),s.upperColor);
        }
        draw(p,out,s,"separatory_glass",true);if(s.capped)draw(p,out,s,"separatory_stopper",true);
        p.pushPose();if(s.open){p.translate(.5,3.35/16,7.8/16);p.mulPose(Axis.ZP.rotationDegrees(90));p.translate(-.5,-3.35/16,-7.8/16);}draw(p,out,s,"separatory_valve",true);p.popPose();p.popPose();
    }
    private static void draw(PoseStack p,SubmitNodeCollector out,State s,String id,boolean glass){var m=ElectricalModels.get(id);if(m!=null)out.submitBlockModel(p,glass?CabinetGlassLayer.TYPE:RenderType.cutout(),m,1,1,1,s.lightCoords,OverlayTexture.NO_OVERLAY,0);}
    private static void liquid(PoseStack p,SubmitNodeCollector out,State s,String id,int color){var m=ElectricalModels.get(id);if(m!=null)out.submitBlockModel(p,CabinetGlassLayer.TYPE,m,((color>>16)&255)/255F,((color>>8)&255)/255F,(color&255)/255F,s.lightCoords,OverlayTexture.NO_OVERLAY,0);}
}
