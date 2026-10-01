package com.example.chemistry.client;
import com.example.chemistry.utility.*;
import com.example.chemistry.item.LabVesselItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
/** Authored machine shell with movable covers, switches and a finite reservoir. */
public class WaterMachineRenderer extends EntityRenderer<WaterMachineEntity,WaterMachineRenderer.State> {
    public static class State extends EntityRenderState {boolean vacuum,lid,on,left,right;float yaw;double fill,temp,pressureLeft,pressureRight;}
    public WaterMachineRenderer(EntityRendererProvider.Context c){super(c);}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(WaterMachineEntity e,State s,float p){super.extractRenderState(e,s,p);s.vacuum=e.vacuum();s.lid=e.flag("lid");s.on=e.flag("on");s.left=e.flag("channel_0");s.right=e.flag("channel_1");s.yaw=e.getYRot();s.fill=Math.clamp(LabVesselItem.usedVolume(e.stack())/1000,0,1);s.temp=com.example.chemistry.TemperatureSystem.getTemp(e.stack());s.pressureLeft=e.number("pressure_0",101.325);s.pressureRight=e.number("pressure_1",101.325);}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(-s.yaw));pose.scale(WaterMachineEntity.SCALE,WaterMachineEntity.SCALE,WaterMachineEntity.SCALE);pose.translate(-.5,0,-7.0/16);
        String id=s.vacuum?"circulating_water_vacuum_pump":"temperature_controlled_circulator";
        draw(pose,out,s,id+"_body");pose.pushPose();if(s.lid)pose.translate(0,.42,.35);draw(pose,out,s,id+"_lid");pose.popPose();
        switchPart(pose,out,s,id+"_main",s.vacuum?8:12.7,s.on);if(s.vacuum){switchPart(pose,out,s,id+"_left",4.5,s.left);switchPart(pose,out,s,id+"_right",11.5,s.right);}
        if(s.fill>0){double height=(s.vacuum?4.48:6.48)*s.fill;var water=ModStandaloneModels.vesselLiquid(5);if(water!=null){pose.pushPose();pose.translate(2.15/16,9.12/16,3.15/16);pose.scale(11.7F/12,(float)height/10,7.7F/12);pose.translate(-2.0/16,-.5/16,-2.0/16);out.submitBlockModel(pose,CabinetGlassLayer.TYPE,water,.56F,.77F,.90F,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}}
        pose.popPose();}
    private static void switchPart(PoseStack p,SubmitNodeCollector out,State s,String id,double x,boolean on){p.pushPose();p.translate(x/16,11.7/16,.68/16);p.mulPose(Axis.XP.rotationDegrees(on?-12:12));p.translate(-x/16,-11.7/16,-.68/16);draw(p,out,s,id);p.popPose();}
    private static void draw(PoseStack p,SubmitNodeCollector out,State s,String id){var m=ElectricalModels.get(id);if(m!=null)out.submitBlockModel(p,RenderType.cutout(),m,1,1,1,s.lightCoords,OverlayTexture.NO_OVERLAY,0);}
}
