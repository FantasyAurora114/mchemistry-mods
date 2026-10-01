package com.example.chemistry.client;
import com.example.chemistry.ChemistryMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid=ChemistryMod.MODID,value=Dist.CLIENT)
public final class FilterRenderChecks {
    private static boolean done;
    @SubscribeEvent public static void check(ClientTickEvent.Post event){
        if(done||!Boolean.getBoolean("mchemistry.verifyFiltration")||Minecraft.getInstance().getOverlay()!=null)return;done=true;
        int checked=0;
        for(int type:new int[]{5,8,9}){verify(ModStandaloneModels.vessel(type),"block/"+(type==5?"beaker":type==8?"beaker_medium":"beaker_tall"));checked++;}
        for(String part:new String[]{"glass","paper","clamp"}){verify(ElectricalModels.get("filter_funnel_"+part),"block/filter_funnel");checked++;}
        int bottles=0;
        var mc=Minecraft.getInstance();
        for(var item:java.util.List.of(com.example.chemistry.registry.ModItems.DROPPER_BOTTLE.get(),com.example.chemistry.registry.ModItems.THERMOMETER.get(),com.example.chemistry.registry.ModItems.GAS_BOTTLE.get(),com.example.chemistry.registry.ModItems.LIQUID_BOTTLE.get(),com.example.chemistry.registry.ModItems.SOLID_JAR.get(),com.example.chemistry.registry.ModItems.LAB_FLOOR_TILE.get(),com.example.chemistry.registry.ModItems.LAB_WALL_TILE.get())){
            var stack=new net.minecraft.world.item.ItemStack(item);
            if(stack.is(com.example.chemistry.registry.ModItems.LIQUID_BOTTLE.get()))com.example.chemistry.transfer.BottleCodes.setLiquid(stack,"crude_saltwater",true,100);
            if(stack.is(com.example.chemistry.registry.ModItems.DROPPER_BOTTLE.get()))com.example.chemistry.transfer.BottleCodes.setLiquid(stack,"water",true,40);
            if(stack.is(com.example.chemistry.registry.ModItems.SOLID_JAR.get()))com.example.chemistry.transfer.BottleCodes.setSolid(stack,"crude_salt",true);
            com.example.chemistry.transfer.BottleCodes.refreshModel(stack);
            var state=new net.minecraft.client.renderer.item.ItemStackRenderState();mc.getItemModelResolver().updateForTopItem(state,stack,net.minecraft.world.item.ItemDisplayContext.NONE,mc.level,null,0);
            if(state.isEmpty()||state.getModelBoundingBox().getYsize()<=0)throw new IllegalStateException("Empty laboratory item model "+stack);
            bottles++;
        }
        ChemistryMod.LOGGER.info("New bottle and tile item checks passed: {} models",bottles);
        verifyWaterItems();
        for(int rotation=0;rotation<8;rotation++){
            verify(ModStandaloneModels.testTube(rotation,false),"block/test_tube_new");
            verify(ModStandaloneModels.testTube(rotation,true),"block/dewar_test_tube_new");
        }
        for(int type:new int[]{1,3,4,5,6,7,8,9})verify(ModStandaloneModels.vesselLiquid(type),"block/lab_water");
        verify(ModStandaloneModels.gasBottle(false,false,false),"block/gas_bottle_new");
        for(String id:new String[]{"test_tube_5ml","test_tube_5ml_dewar","test_tube_5ml_dewar_clamped_stoppered_2"}){
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry",id));
            var stack=new net.minecraft.world.item.ItemStack(item);com.example.chemistry.item.LabVesselItem.addLiquid(stack,"water",20);
            var state=new net.minecraft.client.renderer.item.ItemStackRenderState();
            mc.getItemModelResolver().updateForTopItem(state,stack,net.minecraft.world.item.ItemDisplayContext.GUI,null,null,0);
            if(state.isEmpty()||state.getModelBoundingBox().getYsize()<=0)throw new IllegalStateException("Missing new tube item "+id);
        }
        if(!com.mojang.blaze3d.systems.RenderSystem.getDevice().precompilePipeline(CabinetGlassLayer.ITEM.pipeline()).isValid())throw new IllegalStateException("Glass item pipeline failed");
        ChemistryMod.LOGGER.info("September28 gas/tube/water checks passed: 25 block models, 3 tube items, item glass pipeline");
        for(var stack:java.util.List.of(
                com.example.chemistry.registry.ModItems.emptyGasJar(),
                com.example.chemistry.registry.ModItems.gasBottle("hydrogen",true),
                com.example.chemistry.registry.ModItems.gasBottle("oxygen",false),
                com.example.chemistry.registry.ModItems.gasBottleWater())){
            var state=new net.minecraft.client.renderer.item.ItemStackRenderState();
            mc.getItemModelResolver().updateForTopItem(state,stack,net.minecraft.world.item.ItemDisplayContext.NONE,null,null,0);
            if(state.isEmpty()||state.getModelBoundingBox().getYsize()>.6)throw new IllegalStateException("Gas item model missing or too large");
            var count=new int[]{0};
            var recorder=(net.minecraft.client.renderer.SubmitNodeCollector)java.lang.reflect.Proxy.newProxyInstance(
                    FilterRenderChecks.class.getClassLoader(),new Class[]{net.minecraft.client.renderer.SubmitNodeCollector.class},
                    (proxy,method,args)->{
                        if(method.getName().equals("order"))return proxy;
                        if(method.getName().equals("submitItem")){
                            count[0]++;
                            if(args[7]!=CabinetGlassLayer.ITEM)throw new IllegalStateException("Gas item has wrong glass layer "+args[7]);
                            for(var q:(java.util.List<net.minecraft.client.renderer.block.model.BakedQuad>)args[6])
                                if(q.sprite().contents().name().getPath().contains("missing"))throw new IllegalStateException("Gas item missing texture");
                        }
                        return null;
                    });
            state.submit(new com.mojang.blaze3d.vertex.PoseStack(),recorder,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,0);
            if(count[0]==0)throw new IllegalStateException("Gas item did not draw");
        }
        verify(ModStandaloneModels.gasBottleWaterFill(),"block/lab_water");
        ChemistryMod.LOGGER.info("Gas item checks passed: 4 states with actual glass layer and no missing sprites");
        ReagentCabinetRenderer.verifyLightingModels();
        if(!com.mojang.blaze3d.systems.RenderSystem.getDevice().precompilePipeline(CabinetGlassLayer.TYPE.pipeline()).isValid())throw new IllegalStateException("Cabinet glass pipeline failed to compile");
        ChemistryMod.LOGGER.info("Filter/beaker render checks passed: {} models with authored textures",checked);
    }
    private static void verify(net.minecraft.client.renderer.block.model.BlockStateModel model,String texture){
        if(model==null)throw new IllegalStateException("Missing "+texture);int count=0;
        for(var part:model.collectParts(EmptyBlockAndTintGetter.INSTANCE,BlockPos.ZERO,Blocks.GLASS.defaultBlockState(),RandomSource.create(1))){var quads=new java.util.ArrayList<>(part.getQuads(null));for(Direction direction:Direction.values())quads.addAll(part.getQuads(direction));for(var q:quads){String path=q.sprite().contents().name().getPath();if(!path.equals(texture)&&!(texture.equals("block/lab_water")&&path.equals("block/lab_water_body")))throw new IllegalStateException("Unexpected texture "+texture);count++;}}
        if(count==0)throw new IllegalStateException("Empty "+texture);
    }
    private static void verifyWaterItems() {
        int count=0;var mc=Minecraft.getInstance();
        for(String id:new String[]{"beaker_100ml","beaker_medium","beaker_tall","beaker_500ml"}) {
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry",id));
            for(double fill:new double[]{25,250,475})for(var context:new net.minecraft.world.item.ItemDisplayContext[]{net.minecraft.world.item.ItemDisplayContext.GUI,net.minecraft.world.item.ItemDisplayContext.NONE}) {
                var stack=new net.minecraft.world.item.ItemStack(item);com.example.chemistry.item.LabVesselItem.addMass(stack,"liquid","water",fill);
                var state=new net.minecraft.client.renderer.item.ItemStackRenderState();
                mc.getItemModelResolver().updateForTopItem(state,stack,context,null,null,0);
                if(!state.isAnimated())throw new IllegalStateException("Water item does not animate: "+id);
                var submits=new net.minecraft.client.renderer.SubmitNodeStorage();
                state.submit(new com.mojang.blaze3d.vertex.PoseStack(),submits,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,0);
                boolean top=false,body=false;
                for(var submission:submits.order(0).getItemSubmits())for(var quad:submission.quads()) {
                    String path=quad.sprite().contents().name().getPath();
                    if(path.equals("block/lab_water")) {
                        if(!quad.sprite().contents().isAnimated()||quad.direction()!=Direction.UP)throw new IllegalStateException("Water waves assigned to side walls");
                        top=true;
                    }
                    if(path.equals("block/lab_water_body"))body=true;
                }
                if(!top||!body)throw new IllegalStateException("Water surface/body missing: "+id);
                count++;
            }
        }
        ChemistryMod.LOGGER.info("Animated beaker water checks passed: {} GUI/world fill states",count);
    }
}
