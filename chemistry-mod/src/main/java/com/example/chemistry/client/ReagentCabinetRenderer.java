package com.example.chemistry.client;

import com.example.chemistry.block.ReagentCabinetBlock;
import com.example.chemistry.blockentity.ReagentCabinetBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.*;

public class ReagentCabinetRenderer implements BlockEntityRenderer<ReagentCabinetBlockEntity,ReagentCabinetRenderer.State> {
    private static final java.util.List<StandaloneModelKey<BlockStateModel>> MODELS=new java.util.ArrayList<>();
    static {for(String type:new String[]{"base","tall"})for(String part:new String[]{"body_opaque","left_door_opaque","right_door_opaque","body_glass","left_door_glass","right_door_glass"}) {
        String id="mchemistry:block/"+type+"_reagent_cabinet_"+part;MODELS.add(new StandaloneModelKey<>(()->id));
    }}
    public static void registerModels(ModelEvent.RegisterStandalone e){
        int i=0;for(String type:new String[]{"base","tall"})for(String part:new String[]{"body_opaque","left_door_opaque","right_door_opaque","body_glass","left_door_glass","right_door_glass"})
            e.register(MODELS.get(i++),SimpleUnbakedStandaloneModel.blockStateModel(ResourceLocation.fromNamespaceAndPath("mchemistry","block/"+type+"_reagent_cabinet_"+part)));
    }
    /** Opt-in development smoke check against the actual baked resource models. */
    public static void verifyLightingModels() {
        int checked=0;
        for(boolean tall:new boolean[]{false,true}) {
            var state=(tall?com.example.chemistry.registry.ModBlocks.TALL_REAGENT_CABINET.get()
                    :com.example.chemistry.registry.ModBlocks.BASE_REAGENT_CABINET.get()).defaultBlockState();
            for(int part=0;part<3;part++)for(int facing=0;facing<4;facing++) {
                var model=Minecraft.getInstance().getModelManager().getStandaloneModel(MODELS.get((tall?6:0)+part));
                if(model==null)throw new IllegalStateException("Cabinet model missing");
                var matrix=new org.joml.Matrix4f().translate(.5f,0,.5f).rotateY((float)Math.toRadians(facing*90)).translate(-.5f,0,-.5f);
                if(part>0)matrix.translate(.5f,0,0).rotateY((float)Math.toRadians(part==1?45:-45)).translate(-.5f,0,0);
                var mesh=CabinetLighting.bake(net.minecraft.world.level.EmptyBlockAndTintGetter.INSTANCE,
                        net.minecraft.core.BlockPos.ZERO,state,model,matrix);
                if(mesh.isEmpty() || mesh.size()%4!=0)throw new IllegalStateException("Invalid cabinet quad mesh");
                for(var v:mesh)if(!Float.isFinite(v.x+v.y+v.z+v.u+v.v))throw new IllegalStateException("Invalid cabinet vertex");
                checked++;
            }
        }
        for(int part:new int[]{4,5}){
            var model=Minecraft.getInstance().getModelManager().getStandaloneModel(MODELS.get(6+part));
            var mesh=CabinetLighting.bake(net.minecraft.world.level.EmptyBlockAndTintGetter.INSTANCE,net.minecraft.core.BlockPos.ZERO,
                    com.example.chemistry.registry.ModBlocks.TALL_REAGENT_CABINET.get().defaultBlockState(),model,new org.joml.Matrix4f());
            if(mesh.isEmpty())throw new IllegalStateException("Cabinet glass pass missing");
        }
        com.example.chemistry.ChemistryMod.LOGGER.info("Cabinet lighting smoke check passed: {} baked models/orientations",checked);
        verifyContents();
    }
    private static void verifyContents() {
        int checked=0;
        for(var item:java.util.List.of(com.example.chemistry.registry.ModItems.LIQUID_BOTTLE.get(),
                com.example.chemistry.registry.ModItems.SOLID_JAR.get(),com.example.chemistry.registry.ModItems.ERLENMEYER_FLASK.get(),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath("mchemistry","beaker_100ml")))) {
            var stack=new net.minecraft.world.item.ItemStack(item);
            if(stack.getItem() instanceof com.example.chemistry.item.LiquidBottleItem)
                com.example.chemistry.transfer.BottleCodes.setLiquid(stack,"water",true,100);
            else if(stack.getItem() instanceof com.example.chemistry.item.SolidBottleItem)
                com.example.chemistry.transfer.BottleCodes.setSolid(stack,"sodium_chloride",true);
            else com.example.chemistry.item.LabVesselItem.addMass(stack,"liquid","water",100);
            var model=new ItemStackRenderState();
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(model,stack,ItemDisplayContext.NONE,null,null,0);
            for(boolean tall:new boolean[]{false,true}) {
                double[] shelves=tall?new double[]{25.77,19.84,13.91,7.98,2.05}:new double[]{10.85,6.45,2.05};
                for(int slot=0;slot<shelves.length*3;slot++) {
                    var pose=new PoseStack();fitShelfItem(pose,model,tall,slot,shelves[slot/3]);
                    var mesh=CabinetLighting.bakeItem(model,pose,15728880);
                    if(mesh.isEmpty())throw new IllegalStateException("Cabinet shelf item did not submit geometry");
                    for(var v:mesh) {
                        if(!Float.isFinite(v.x+v.y+v.z+v.u+v.v)||v.y<shelves[slot/3]/16-.001
                                ||v.y>(shelves[slot/3]+(tall?4.8:3.45))/16+.004
                                ||v.z<.20||v.z>.78||v.x<.05||v.x>.95)
                            throw new IllegalStateException("Cabinet shelf mesh is outside cabinet: "+stack+" / "+slot);
                    }
                    checked++;
                }
            }
        }
        com.example.chemistry.ChemistryMod.LOGGER.info("Cabinet contents check passed: {} real item meshes across all shelves",checked);
    }
    public ReagentCabinetRenderer(BlockEntityRendererProvider.Context c){}
    public static class State extends BlockEntityRenderState {
        boolean upper,tall;float angle,rotation;
        java.util.List<CabinetLighting.Vertex> geometry=java.util.List.of(),glass=java.util.List.of();
        java.util.List<CabinetLighting.Vertex> contents=java.util.List.of();
        final ItemStackRenderState[] items=new ItemStackRenderState[15];
        State(){for(int i=0;i<15;i++)items[i]=new ItemStackRenderState();}
    }
    @Override public State createRenderState(){return new State();}
    @Override public boolean shouldRenderOffScreen(){return true;}
    @Override public void extractRenderState(ReagentCabinetBlockEntity be,State s,float partial,Vec3 camera,
            @org.jetbrains.annotations.Nullable net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderer.super.extractRenderState(be,s,partial,camera,overlay);
        s.upper=ReagentCabinetBlock.isUpper(be.getBlockState());
        s.tall=be.rows()==5;
        s.angle=net.minecraft.util.Mth.lerp(partial,be.previousOpen,be.openProgress)*90;
        s.rotation=switch(be.getBlockState().getValue(ReagentCabinetBlock.FACING)){case SOUTH->180;case EAST->-90;case WEST->90;default->0;};
        java.util.List<CabinetLighting.Vertex> vertices=new java.util.ArrayList<>(),glass=new java.util.ArrayList<>();
        if(!s.upper && be.getLevel()!=null) {
            var manager=Minecraft.getInstance().getModelManager();
            var facing=new org.joml.Matrix4f().translate(.5f,0,.5f).rotateY((float)Math.toRadians(s.rotation)).translate(-.5f,0,-.5f);
            for(int part=0;part<6;part++) {
                var transform=new org.joml.Matrix4f(facing);
                int door=part%3;
                if(door>0) {
                    float x=(door==1?.75f:15.25f)/16,z=.9f/16;
                    transform.translate(x,0,z).rotateY((float)Math.toRadians(s.angle*(door==1?1:-1))).translate(-x,0,-z);
                }
                (part<3?vertices:glass).addAll(CabinetLighting.bake(be.getLevel(),be.getBlockPos(),be.getBlockState(),
                        manager.getStandaloneModel(MODELS.get((s.tall?6:0)+part)),transform));
            }
        }
        s.geometry=java.util.List.copyOf(vertices);s.glass=java.util.List.copyOf(glass);
        for(int i=0;i<15;i++){
            s.items[i].clear();
            if(i<be.getContainerSize()){
                var stack=be.getItem(i).copy();
                if(stack.getItem() instanceof com.example.chemistry.item.LiquidBottleItem
                        ||stack.getItem() instanceof com.example.chemistry.item.SolidBottleItem
                        ||stack.getItem() instanceof com.example.chemistry.item.DropperBottleItem)
                    com.example.chemistry.transfer.BottleCodes.refreshModel(stack);
                Minecraft.getInstance().getItemModelResolver().updateForTopItem(s.items[i],stack,ItemDisplayContext.NONE,be.getLevel(),null,i);
            }
        }
        var contents = new java.util.ArrayList<CabinetLighting.Vertex>();
        PoseStack itemPose = new PoseStack();
        itemPose.translate(.5,0,.5);itemPose.mulPose(Axis.YP.rotationDegrees(s.rotation));itemPose.translate(-.5,0,-.5);
        double[] shelves=s.tall?new double[]{25.77,19.84,13.91,7.98,2.05}:new double[]{10.85,6.45,2.05};
        for(int i=0;i<shelves.length*3;i++) {
            var item=s.items[i];if(item.isEmpty())continue;
            itemPose.pushPose();
            fitShelfItem(itemPose,item,s.tall,i,shelves[i/3]);
            int light=s.lightCoords;
            if(be.getLevel()!=null) {
                var front=be.getBlockPos().relative(be.getBlockState().getValue(ReagentCabinetBlock.FACING))
                        .above((int)(shelves[i/3]/16));
                light=net.minecraft.client.renderer.LevelRenderer.getLightColor(be.getLevel(),front);
            }
            contents.addAll(CabinetLighting.bakeItem(item,itemPose,light));itemPose.popPose();
        }
        s.contents=java.util.List.copyOf(contents);
    }
    static void fitShelfItem(PoseStack pose,ItemStackRenderState item,boolean tall,int slot,double shelf) {
        AABB bounds=item.getModelBoundingBox();
        double size=Math.min(3.6/16/Math.max(.01,bounds.getXsize()),Math.min((tall?4.8:3.45)/16/Math.max(.01,bounds.getYsize()),.55/Math.max(.01,bounds.getZsize())));
        if(!Double.isFinite(size))throw new IllegalStateException("Invalid cabinet item bounds");
        pose.translate((12.6-(slot%3)*4.6)/16,shelf/16+.002,.49);
        pose.scale((float)size,(float)size,(float)size);
        pose.translate(-(bounds.minX+bounds.maxX)/2,-bounds.minY,-(bounds.minZ+bounds.maxZ)/2);
    }
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        if(s.upper)return;
        CabinetLighting.submit(s.geometry,pose,collector.order(-1),RenderType.cutout());
        CabinetLighting.submit(s.contents,pose,collector.order(1),CabinetGlassLayer.TYPE);
        CabinetLighting.submit(s.glass,pose,collector.order(2),CabinetGlassLayer.TYPE);
    }
}
