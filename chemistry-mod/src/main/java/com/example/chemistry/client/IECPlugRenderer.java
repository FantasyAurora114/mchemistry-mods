package com.example.chemistry.client;
import com.example.chemistry.utility.IECPlugEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
public class IECPlugRenderer extends EntityRenderer<IECPlugEntity,EntityRenderState>{
 public IECPlugRenderer(EntityRendererProvider.Context c){super(c);}
 @Override public EntityRenderState createRenderState(){return new EntityRenderState();}
 @Override public void submit(EntityRenderState s,PoseStack p,SubmitNodeCollector out,CameraRenderState c){ElectroDeviceRenderer.box(p,out,s.lightCoords,0,0,0,1.6,1,2.6,0x31383F,false);for(int i:new int[]{-1,1})ElectroDeviceRenderer.box(p,out,s.lightCoords,i*.4,.25,-1.65,.2,.45,.8,0xB7AF99,false);}
}
