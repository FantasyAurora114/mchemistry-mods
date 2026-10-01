package com.example.chemistry.client;

import com.example.chemistry.electrical.*;
import com.example.chemistry.item.LabVesselItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;

public final class TroughRenderer {
    public static void draw(ElectroDeviceRenderer.State s,PoseStack p,SubmitNodeCollector out) {
        p.pushPose();p.mulPose(Axis.YP.rotationDegrees(-s.yaw));p.translate(-.5,0,-.5);
        var tank=ElectricalModels.get("deep_pneumatic_trough");if(tank!=null)out.submitBlockModel(p,CabinetGlassLayer.TYPE,tank,1,1,1,s.lightCoords,OverlayTexture.NO_OVERLAY,0);
        water(p,out,s.lightCoords,8,.82,8,13.3,7.7*Math.clamp((s.liquid-s.jarWater[0]-s.jarWater[1])/1000,0,1),8.3,s.color);
        for(int side=0;side<2;side++) {
            double x=side==0?4.25:11.75;p.pushPose();p.translate(x/16,1.0/16,8.0/16);p.mulPose(Axis.YP.rotationDegrees(90));p.scale(.6F,.6F,.6F);p.translate(-.5,0,-.5);
            String color=s.clips[side].is(com.example.chemistry.registry.ModItems.ALLIGATOR_CLIP_RED.get())?"red":"black";
            String metal=ElectrodePartItem.material(s.meshes[side]);
            if(!s.clips[side].isEmpty())part(p,out,s.lightCoords,"clip_mesh_silver_"+color+"_clip");
            if(!s.meshes[side].isEmpty()) {
                String surface=ElectrodePartItem.coating(s.meshes[side],"copper")>1e-9?"copper":ElectrodePartItem.coating(s.meshes[side],"silver")>1e-9?"silver":metal.equals("copper")?"copper":"silver";
                part(p,out,s.lightCoords,"clip_mesh_"+surface+"_"+color+"_mesh");
            }
            p.popPose();
            if(!s.jars[side].isEmpty()) {
                p.pushPose();p.translate((x-8.5)/16,.04,(8-8.5)/16);p.scale(.9F,1.7F,.9F);
                var jar=ModStandaloneModels.gasBottleInverted();if(jar!=null)out.submitBlockModel(p,CabinetGlassLayer.TYPE,jar,1,1,1,s.lightCoords,OverlayTexture.NO_OVERLAY,0);
                p.popPose();
                double fraction=Math.clamp(s.jarWater[side]/250,0,1),target=fraction*(4*2.1+25*9.0);
                double neck=Math.min(2.1,target/4),body=Math.max(0,(target-4*2.1)/25);
                water(p,out,s.lightCoords,x,.65,8,2,neck,2,s.color);
                water(p,out,s.lightCoords,x,2.75,8,5,body,5,s.color);
            }
        }
        p.popPose();
    }
    private static void part(PoseStack p,SubmitNodeCollector out,int light,String name) {
        var model=ElectricalModels.get(name);if(model!=null)out.submitBlockModel(p,RenderType.cutout(),model,1,1,1,light,OverlayTexture.NO_OVERLAY,0);
    }
    public static void water(PoseStack p,SubmitNodeCollector out,int light,double x,double y,double z,double w,double h,double d,int color) {
        if(h<=1e-8)return;var m=ElectricalModels.get("trough_liquid_unit");if(m==null)return;p.pushPose();p.translate(x/16,y/16,z/16);p.scale((float)w/16,(float)h/16,(float)d/16);
        out.submitBlockModel(p,CabinetGlassLayer.TYPE,m,((color>>16)&255)/255F,((color>>8)&255)/255F,(color&255)/255F,light,OverlayTexture.NO_OVERLAY,0);p.popPose();
    }
}
