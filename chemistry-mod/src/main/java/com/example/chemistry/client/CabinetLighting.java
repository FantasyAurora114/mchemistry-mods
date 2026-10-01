package com.example.chemistry.client;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.*;

/** Snapshot vanilla per-vertex AO during extraction; submission never reads the world. */
public final class CabinetLighting {
    public static final class Vertex {
        float x,y,z,u,v,nx,ny,nz;
        int r=255,g=255,b=255,a=255,light,overlay;
        void submit(PoseStack.Pose pose,VertexConsumer out) {
            out.addVertex(pose,x,y,z).setColor(r,g,b,a).setUv(u,v)
                    .setOverlay(overlay).setLight(light).setNormal(pose,nx,ny,nz);
        }
    }
    private static final class Capture implements VertexConsumer {
        final List<Vertex> vertices=new ArrayList<>();
        Vertex current;
        public VertexConsumer addVertex(float x,float y,float z) {current=new Vertex();current.x=x;current.y=y;current.z=z;vertices.add(current);return this;}
        public VertexConsumer setColor(int r,int g,int b,int a) {current.r=r;current.g=g;current.b=b;current.a=a;return this;}
        public VertexConsumer setUv(float u,float v) {current.u=u;current.v=v;return this;}
        public VertexConsumer setUv1(int u,int v) {current.overlay=u|(v<<16);return this;}
        public VertexConsumer setUv2(int u,int v) {current.light=u|(v<<16);return this;}
        public VertexConsumer setNormal(float x,float y,float z) {current.nx=x;current.ny=y;current.nz=z;return this;}
    }
    public static List<Vertex> bake(net.minecraft.world.level.BlockAndTintGetter level,BlockPos pos,BlockState state,BlockStateModel model,
            Matrix4f transform) {
        if(model==null)return List.of();
        Map<BlockPos,List<BakedQuad>> groups=new HashMap<>();
        var parts=model.collectParts(level,pos,state,RandomSource.create(42));
        for(var part:parts) {
            List<BakedQuad> quads=new ArrayList<>(part.getQuads(null));
            for(Direction d:Direction.values())quads.addAll(part.getQuads(d));
            for(BakedQuad q:quads) {
                int[] data=q.vertices().clone();int stride=data.length/4;
                Vector3f mean=new Vector3f();
                for(int i=0;i<4;i++) {
                    int at=i*stride;
                    Vector3f point=new Vector3f(Float.intBitsToFloat(data[at]),Float.intBitsToFloat(data[at+1]),Float.intBitsToFloat(data[at+2]));
                    transform.transformPosition(point);mean.add(point);
                    data[at]=Float.floatToRawIntBits(point.x);data[at+1]=Float.floatToRawIntBits(point.y);data[at+2]=Float.floatToRawIntBits(point.z);
                    // Packed vertex normals are transformed alongside the geometry.
                    int packed=data[at+7];
                    if((packed&0xffffff)!=0) {
                        Vector3f normal=new Vector3f((byte)packed/127f,(byte)(packed>>8)/127f,(byte)(packed>>16)/127f);
                        transform.transformDirection(normal).normalize();
                        data[at+7]=(packed&0xff000000)|((int)(normal.x*127)&255)|(((int)(normal.y*127)&255)<<8)|(((int)(normal.z*127)&255)<<16);
                    }
                }
                mean.mul(.25f);
                BlockPos offset=BlockPos.containing(mean.x,mean.y,mean.z);
                for(int i=0;i<4;i++)for(int axis=0;axis<3;axis++) {
                    int at=i*stride+axis;int shift=axis==0?offset.getX():axis==1?offset.getY():offset.getZ();
                    data[at]=Float.floatToRawIntBits(Float.intBitsToFloat(data[at])-shift);
                }
                Vector3f normal=new Vector3f(q.direction().getStepX(),q.direction().getStepY(),q.direction().getStepZ());
                transform.transformDirection(normal);
                Direction face=Direction.getApproximateNearest(normal.x,normal.y,normal.z);
                groups.computeIfAbsent(offset,k->new ArrayList<>()).add(new BakedQuad(data,q.tintIndex(),face,q.sprite(),q.shade(),q.lightEmission(),true));
            }
        }
        Capture capture=new Capture();
        var renderer=Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        for(var group:groups.entrySet()) {
            var quads=List.copyOf(group.getValue());
            BlockModelPart part=new BlockModelPart() {
                public List<BakedQuad> getQuads(Direction direction){return direction==null?quads:List.of();}
                public boolean useAmbientOcclusion(){return true;}
                public TextureAtlasSprite particleIcon(){return quads.getFirst().sprite();}
            };
            var offset=group.getKey();PoseStack pose=new PoseStack();pose.translate(offset.getX(),offset.getY(),offset.getZ());
            renderer.tesselateBlock(level,List.of(part),state,pos.offset(offset),pose,capture,false,OverlayTexture.NO_OVERLAY);
        }
        return List.copyOf(capture.vertices);
    }
    public static void submit(List<Vertex> vertices,PoseStack pose,net.minecraft.client.renderer.OrderedSubmitNodeCollector collector,RenderType type) {
        if(!vertices.isEmpty())collector.submitCustomGeometry(pose,type,(matrix,out)->vertices.forEach(v->v.submit(matrix,out)));
    }
    /** Keep shelf items in the same atlas, vertex format and target as the cabinet.
     * Item feature batches otherwise flush separately from the cabinet glass. */
    public static List<Vertex> bakeItem(net.minecraft.client.renderer.item.ItemStackRenderState item,
            PoseStack pose, int light) {
        var storage = new net.minecraft.client.renderer.SubmitNodeStorage();
        item.submit(pose, storage, light, OverlayTexture.NO_OVERLAY, 0);
        Capture capture = new Capture();
        for (var submission : storage.order(0).getItemSubmits()) {
            PoseStack itemPose = new PoseStack();
            itemPose.last().set(submission.pose());
            net.minecraft.client.renderer.entity.ItemRenderer.renderItem(submission.displayContext(), itemPose,
                    type -> capture, submission.lightCoords(), submission.overlayCoords(), submission.tintLayers(),
                    submission.quads(), submission.renderType(),
                    net.minecraft.client.renderer.item.ItemStackRenderState.FoilType.NONE);
        }
        return List.copyOf(capture.vertices);
    }
    private CabinetLighting() {}
}
