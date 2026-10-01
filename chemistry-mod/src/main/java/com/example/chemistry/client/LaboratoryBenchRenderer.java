package com.example.chemistry.client;

import com.example.chemistry.block.LaboratoryBenchBlock;
import com.example.chemistry.blockentity.LaboratoryBenchBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.*;
import org.joml.Matrix4f;
import java.util.*;

public class LaboratoryBenchRenderer implements BlockEntityRenderer<LaboratoryBenchBlockEntity,LaboratoryBenchRenderer.State> {
    private static final String[][] PARTS={
            {"body","left_outer_door","left_inner_door","drawer_bottom","drawer_middle","drawer_top","right_inner_door","right_outer_door"},
            {"body","door_left_outer","door_left_inner","door_right_inner","door_right_outer"},
            {"body","sink_left_door","sink_right_door","side_left_door","side_right_door","hot_handle","cold_handle"}
    };
    private static final String[] IDS={"lab_table","lab_table_cabinet","lab_table_sink"};
    private static final List<List<StandaloneModelKey<BlockStateModel>>> MODELS=new ArrayList<>();
    static {for(int v=0;v<3;v++){List<StandaloneModelKey<BlockStateModel>> row=new ArrayList<>();for(String part:PARTS[v]){String name="mchemistry:block/"+IDS[v]+"_"+part;row.add(new StandaloneModelKey<>(()->name));}MODELS.add(row);}}
    public static void registerModels(ModelEvent.RegisterStandalone event){for(int v=0;v<3;v++)for(int i=0;i<PARTS[v].length;i++)event.register(MODELS.get(v).get(i),SimpleUnbakedStandaloneModel.blockStateModel(ResourceLocation.fromNamespaceAndPath("mchemistry","block/"+IDS[v]+"_"+PARTS[v][i])));}
    public static void verifySinkGeometry(){
        var model=Minecraft.getInstance().getModelManager().getStandaloneModel(MODELS.get(2).get(0));
        for(var direction:new net.minecraft.core.Direction[]{net.minecraft.core.Direction.NORTH,net.minecraft.core.Direction.EAST,net.minecraft.core.Direction.SOUTH,net.minecraft.core.Direction.WEST}){
            var state=com.example.chemistry.registry.ModBlocks.LAB_TABLE_SINK.get().defaultBlockState().setValue(LaboratoryBenchBlock.FACING,direction);
            var shape=state.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,net.minecraft.core.BlockPos.ZERO);
            Matrix4f rotation=new Matrix4f().translate(.5f,0,.5f).rotateY((float)Math.toRadians(com.example.chemistry.block.BenchGeometry.rotation(state))).translate(-.5f,0,-.5f);
            int checked=0;
            for(var part:model.collectParts(net.minecraft.world.level.EmptyBlockAndTintGetter.INSTANCE,net.minecraft.core.BlockPos.ZERO,state,net.minecraft.util.RandomSource.create(1))){
                var quads=new ArrayList<>(part.getQuads(null));for(var face:net.minecraft.core.Direction.values())quads.addAll(part.getQuads(face));
                for(var quad:quads){int[] data=quad.vertices();int stride=data.length/4;var center=new org.joml.Vector3f();
                    for(int v=0;v<4;v++)center.add(Float.intBitsToFloat(data[v*stride]),Float.intBitsToFloat(data[v*stride+1]),Float.intBitsToFloat(data[v*stride+2]));center.mul(.25F);
                    if(center.y<=18F/16)continue;rotation.transformPosition(center);final boolean[] hit={false};
                    shape.forAllBoxes((a,b,c,d,e,f)->{if(center.x>=a-.001&&center.x<=d+.001&&center.y>=b-.001&&center.y<=e+.001&&center.z>=c-.001&&center.z<=f+.001)hit[0]=true;});
                    if(!hit[0])throw new IllegalStateException("Rendered faucet outside collision: "+direction+" "+center);checked++;
                }
            }if(checked==0)throw new IllegalStateException("Faucet model has no checked faces");
        }
    }
    public static void verifyDoorGeometry(){
        var manager=Minecraft.getInstance().getModelManager();
        var blocks=List.of(com.example.chemistry.registry.ModBlocks.LAB_TABLE.get(),com.example.chemistry.registry.ModBlocks.LAB_TABLE_CABINET.get(),com.example.chemistry.registry.ModBlocks.LAB_TABLE_SINK.get());
        for(int variant=0;variant<3;variant++)for(int part=1;part<PARTS[variant].length;part++){
            String name=PARTS[variant][part];if(name.startsWith("drawer_")||name.endsWith("handle"))continue;
            var state=blocks.get(variant).defaultBlockState();var model=manager.getStandaloneModel(MODELS.get(variant).get(part));
            double minX=Double.POSITIVE_INFINITY,maxX=Double.NEGATIVE_INFINITY,minZ=Double.POSITIVE_INFINITY,maxZ=Double.NEGATIVE_INFINITY;
            for(var mesh:model.collectParts(net.minecraft.world.level.EmptyBlockAndTintGetter.INSTANCE,net.minecraft.core.BlockPos.ZERO,state,net.minecraft.util.RandomSource.create(1))){
                var quads=new ArrayList<>(mesh.getQuads(null));for(var face:net.minecraft.core.Direction.values())quads.addAll(mesh.getQuads(face));
                for(var q:quads){var data=q.vertices();int stride=data.length/4;for(int v=0;v<4;v++){double x=Float.intBitsToFloat(data[v*stride]),z=Float.intBitsToFloat(data[v*stride+2]);minX=Math.min(minX,x);maxX=Math.max(maxX,x);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);}}
            }
            var center=new org.joml.Vector3f((float)((minX+maxX)/2),.5F,(float)((minZ+maxZ)/2));var opened=new org.joml.Vector3f(center);
            float hinge=pivot(name)/16F,z=.8F/16F;new Matrix4f().translate(hinge,0,z).rotateY((float)Math.toRadians(95*com.example.chemistry.block.BenchDoorGeometry.sign(name))).translate(-hinge,0,-z).transformPosition(opened);
            if(!Float.isFinite(center.x)||opened.z>=center.z-.01F)throw new IllegalStateException("Door swings into cabinet: "+name);
            for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL){var facing=state.setValue(LaboratoryBenchBlock.FACING,direction);
                var closedWorld=com.example.chemistry.block.BenchGeometry.world(facing,net.minecraft.core.BlockPos.ZERO,new Vec3(center.x*16,8,center.z*16));var openWorld=com.example.chemistry.block.BenchGeometry.world(facing,net.minecraft.core.BlockPos.ZERO,new Vec3(opened.x*16,8,opened.z*16));
                var origin=com.example.chemistry.block.BenchGeometry.world(facing,net.minecraft.core.BlockPos.ZERO,new Vec3(8,8,8));var front=com.example.chemistry.block.BenchGeometry.world(facing,net.minecraft.core.BlockPos.ZERO,new Vec3(8,8,-8)).subtract(origin);
                if(openWorld.subtract(closedWorld).dot(front)<=0)throw new IllegalStateException("Door orientation reverses: "+direction+" "+name);
            }
        }
    }
    public LaboratoryBenchRenderer(BlockEntityRendererProvider.Context ctx){}
    public static class State extends BlockEntityRenderState {List<CabinetLighting.Vertex> geometry=List.of();int water;boolean flowing,sink;float rotation;}
    @Override public State createRenderState(){return new State();}
    @Override public boolean shouldRenderOffScreen(){return true;}
    @Override public void extractRenderState(LaboratoryBenchBlockEntity be,State state,float partial,Vec3 camera,@org.jetbrains.annotations.Nullable net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderer.super.extractRenderState(be,state,partial,camera,overlay);
        int variant=((LaboratoryBenchBlock)be.getBlockState().getBlock()).variant();
        state.rotation=com.example.chemistry.block.BenchGeometry.rotation(be.getBlockState());state.sink=variant==2;
        state.water=be.waterMl();state.flowing=be.flowing();
        Matrix4f facing=new Matrix4f().translate(.5f,0,.5f).rotateY((float)Math.toRadians(state.rotation)).translate(-.5f,0,-.5f);
        var models=Minecraft.getInstance().getModelManager();List<CabinetLighting.Vertex> vertices=new ArrayList<>();
        for(int part=0;part<PARTS[variant].length;part++){
            Matrix4f transform=new Matrix4f(facing);if(part>0){String name=PARTS[variant][part];
                if(name.startsWith("drawer_")){float progress=net.minecraft.util.Mth.lerp(partial,be.previousOpen[part-1],be.openProgress[part-1]);transform.translate(0,0,-.30f*progress);}
                else if(name.endsWith("handle")){float angle=name.equals("hot_handle")?(be.hotSetting()==0?0:be.hotSetting()-20):be.coldOn()?45:0;float x=name.equals("hot_handle")?5.5f/16:10.5f/16;transform.translate(x,0,13.9f/16).rotateY((float)Math.toRadians(angle)).translate(-x,0,-13.9f/16);}
                else{int bay=part-1;float progress=net.minecraft.util.Mth.lerp(partial,be.previousOpen[bay],be.openProgress[bay]);float x=pivot(name)/16f,z=.8f/16f,sign=com.example.chemistry.block.BenchDoorGeometry.sign(name);transform.translate(x,0,z).rotateY((float)Math.toRadians(95*progress*sign)).translate(-x,0,-z);}
            }
            vertices.addAll(CabinetLighting.bake(be.getLevel(),be.getBlockPos(),be.getBlockState(),models.getStandaloneModel(MODELS.get(variant).get(part)),transform));
        }
        state.geometry=List.copyOf(vertices);
    }
    private static float pivot(String name){return switch(name){case "left_outer_door","door_left_outer","side_left_door"->.65f;case "left_inner_door"->5.1f;case "right_inner_door"->10.9f;case "right_outer_door","door_right_outer","side_right_door"->15.35f;case "door_left_inner"->7.9f;case "door_right_inner"->8.1f;case "sink_left_door"->3.3f;case "sink_right_door"->12.7f;default->8;};}
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        CabinetLighting.submit(state.geometry,pose,collector.order(-1),RenderType.cutout());
        if(state.sink){
            pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(state.rotation));pose.translate(-.5,0,-.5);
            // Compact internal tank, fill neck and a front level gauge, visible through open doors.
            ElectroDeviceRenderer.box(pose,collector,state.lightCoords,8,1.1,8,7.2,8.1,7.4,0xADB4BB,false);
            ElectroDeviceRenderer.box(pose,collector,state.lightCoords,8,9.2,8,1.5,.7,1.5,0x6B9AB0,false);
            ElectroDeviceRenderer.box(pose,collector,state.lightCoords,8,2,4.26,1.25,6,.06,0x25343D,false);
            ElectroDeviceRenderer.box(pose,collector,state.lightCoords,8,2.1,4.21,1.05,5.8*state.water/4000.0,.04,0x65B6D2,false);
            for(int i=0;i<=4;i++)ElectroDeviceRenderer.box(pose,collector,state.lightCoords,8.73,2.1+i*1.45,4.21,.35,.055,.04,0xE3E8E8,false);
            if(state.flowing&&state.water>0){
                ElectroDeviceRenderer.box(pose,collector,state.lightCoords,8,11.55,9.85,.3,8.2,.3,0x87BDE0,true);
                ElectroDeviceRenderer.box(pose,collector,state.lightCoords,8,11.55,9.85,1.2,.04,1.2,0x87BDE0,true);
            }
            pose.popPose();
        }
    }
}
