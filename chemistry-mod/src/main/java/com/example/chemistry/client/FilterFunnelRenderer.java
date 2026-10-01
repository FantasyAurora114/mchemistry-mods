package com.example.chemistry.client;
import com.example.chemistry.filtration.*;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.item.LabVesselItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
public class FilterFunnelRenderer extends EntityRenderer<FilterFunnelEntity,FilterFunnelRenderer.State>{
    public static class State extends EntityRenderState{float yaw;boolean paper,buchner;double pending,residue;int liquidColor,solidColor;final ItemStackRenderState receiver=new ItemStackRenderState();}
    public FilterFunnelRenderer(EntityRendererProvider.Context c){super(c);}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(FilterFunnelEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.yaw=e.getYRot();s.buchner=e.buchner();s.paper=!e.paper().isEmpty();s.pending=Filtration.liquidVolume(e.contents());s.residue=Filtration.solids(e.paper());var visual=VesselVisualState.of(e.contents());s.liquidColor=visual.liquidColor();s.solidColor=VesselVisualState.of(e.paper()).sedimentColor();s.receiver.clear();net.minecraft.client.Minecraft.getInstance().getItemModelResolver().updateForTopItem(s.receiver,e.receiver(),ItemDisplayContext.NONE,e.level(),null,0);}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(-s.yaw));
        if(!s.receiver.isEmpty()){
            var b=s.receiver.getModelBoundingBox();double fit=Math.min(.36/Math.max(.01,b.getXsize()),Math.min(.30/Math.max(.01,b.getYsize()),.36/Math.max(.01,b.getZsize())));
            pose.pushPose();pose.translate(.03125,.12,-.14);pose.scale((float)fit,(float)fit,(float)fit);pose.translate(-(b.minX+b.maxX)/2,-b.minY,-(b.minZ+b.maxZ)/2);s.receiver.submit(pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
        }
        pose.translate(.03125,.46,-.14);pose.scale(FilterFunnelEntity.SCALE,FilterFunnelEntity.SCALE,FilterFunnelEntity.SCALE);pose.translate(-.5,0,-.5);
        draw(pose,out,s,"clamp",false);if(s.paper){if(s.buchner)ElectroDeviceRenderer.box(pose,out,s.lightCoords,8,5.05,8,6,.12,6,0xE8E5DC,false);else draw(pose,out,s,"paper",false);}
        if(s.residue>0)ElectroDeviceRenderer.box(pose,out,s.lightCoords,8,s.buchner?5.2:8.05,8,s.buchner?5:1.1,Math.min(1.5,s.residue/20*1.5),s.buchner?5:1.1,s.solidColor,false);
        if(s.pending>0&&s.buchner)ElectroDeviceRenderer.box(pose,out,s.lightCoords,8,5.3,8,5,Math.min(3,s.pending/100*3),5,s.liquidColor,true);
        if(s.pending>0&&!s.buchner){double top=8.0+6.5*Math.cbrt(Math.min(1,s.pending/100));for(double y=8;y<top;y+=.4){double width=Math.max(.5,(y-7.3)*1.05);ElectroDeviceRenderer.box(pose,out,s.lightCoords,8,y,8,width,Math.min(.4,top-y),width,s.liquidColor,true);}}
        if(s.buchner){var funnel=ElectricalModels.get("buchner_funnel");if(funnel!=null)out.submitBlockModel(pose,RenderType.cutout(),funnel,1,1,1,s.lightCoords,OverlayTexture.NO_OVERLAY,0);}else draw(pose,out,s,"glass",true);pose.popPose();
    }
    private static void draw(PoseStack pose,SubmitNodeCollector out,State s,String part,boolean transparent){var m=ElectricalModels.get("filter_funnel_"+part);if(m!=null)out.submitBlockModel(pose,transparent?CabinetGlassLayer.TYPE:RenderType.cutout(),m,1,1,1,s.lightCoords,OverlayTexture.NO_OVERLAY,0);}
}
