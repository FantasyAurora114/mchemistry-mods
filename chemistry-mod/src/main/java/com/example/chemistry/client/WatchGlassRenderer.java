package com.example.chemistry.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
public final class WatchGlassRenderer {
 public static void draw(PoseStack pose,SubmitNodeCollector out,int type,boolean cover,int light){if(!cover)return;var model=ElectricalModels.get("watch_glass");if(model==null)return;double width=type==5?11.14:type==8?9.64:7.94;float sc=(float)(width/10.7);pose.pushPose();pose.translate(8.5/16,com.example.chemistry.VesselHeating.mouthTopY(type)/16,8.5/16);pose.scale(sc,1,sc);pose.translate(-8.05/16,0,-8.85/16);out.submitBlockModel(pose,CabinetGlassLayer.TYPE,model,1,1,1,light,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
 private WatchGlassRenderer(){}
}
