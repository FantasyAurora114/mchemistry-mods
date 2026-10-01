package com.example.chemistry.client;
import com.example.chemistry.entity.PlacedReagentBottleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
public class PlacedReagentBottleRenderer extends EntityRenderer<PlacedReagentBottleEntity,PlacedReagentBottleRenderer.State>{
    public static class State extends EntityRenderState{float yaw;final ItemStackRenderState item=new ItemStackRenderState();}
    public PlacedReagentBottleRenderer(EntityRendererProvider.Context c){super(c);}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(PlacedReagentBottleEntity e,State s,float p){super.extractRenderState(e,s,p);s.yaw=e.getYRot();var bottle=e.toStack();com.example.chemistry.transfer.BottleCodes.refreshModel(bottle);net.minecraft.client.Minecraft.getInstance().getItemModelResolver().updateForTopItem(s.item,bottle,ItemDisplayContext.NONE,e.level(),null,0);}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){if(s.item.isEmpty())return;var b=s.item.getModelBoundingBox();float scale=(float)Math.min(.23/Math.max(b.getXsize(),b.getZsize()),.4/b.getYsize());pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(-s.yaw));pose.scale(scale,scale,scale);pose.translate(-(b.minX+b.maxX)/2,-b.minY,-(b.minZ+b.maxZ)/2);s.item.submit(pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
}
