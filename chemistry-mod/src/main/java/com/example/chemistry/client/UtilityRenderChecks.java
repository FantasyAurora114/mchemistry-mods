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
public final class UtilityRenderChecks {
 private static boolean done;
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){if(done||!Boolean.getBoolean("mchemistry.verifyUtilities")||Minecraft.getInstance().getOverlay()!=null)return;done=true;int count=0;
  var models=new java.util.ArrayList<net.minecraft.client.renderer.block.model.BlockStateModel>();
  models.add(ModStandaloneModels.attachedModel(1));models.add(ModStandaloneModels.attachedModel(2));models.add(ModStandaloneModels.attachedModel(4));models.add(ModStandaloneModels.attachedModel(8));
  for(String id:new String[]{"temperature_controlled_circulator","circulating_water_vacuum_pump"})for(String part:new String[]{"body","lid","main"})models.add(ElectricalModels.get(id+"_"+part));
  for(var model:models){if(model==null)throw new IllegalStateException("Utility model missing");int faces=0;for(var part:model.collectParts(EmptyBlockAndTintGetter.INSTANCE,BlockPos.ZERO,Blocks.GLASS.defaultBlockState(),RandomSource.create(1))){var quads=new java.util.ArrayList<>(part.getQuads(null));for(Direction d:Direction.values())quads.addAll(part.getQuads(d));for(var quad:quads){if(quad.sprite().contents().name().getPath().contains("missingno"))throw new IllegalStateException("Missing tube/machine texture");faces++;}}if(faces==0)throw new IllegalStateException("Utility model empty");count++;}
  LaboratoryBenchRenderer.verifySinkGeometry();
  try{
   var source=Minecraft.getInstance().renderBuffers().bufferSource();var field=net.minecraft.client.renderer.MultiBufferSource.BufferSource.class.getDeclaredField("fixedBuffers");field.setAccessible(true);
   var buffers=(java.util.Map<?,?>)field.get(source);var keys=new java.util.ArrayList<>(buffers.keySet());
   if(!buffers.containsKey(CabinetGlassLayer.TYPE)||!buffers.containsKey(CabinetGlassLayer.ITEM)||keys.indexOf(CabinetGlassLayer.TYPE)<keys.indexOf(net.minecraft.client.renderer.Sheets.cutoutBlockSheet()))throw new IllegalStateException("Glass buffer is not deferred after opaque geometry");
  }catch(ReflectiveOperationException ex){throw new IllegalStateException("Cannot check deferred glass buffers",ex);}
  ChemistryMod.LOGGER.info("Bench render checks passed: four facing faucet meshes match collision; glass uses deferred buffers after opaque geometry");
  ChemistryMod.LOGGER.info("Utility render check passed: {} authored machine/tube models have textured faces",count);
 }
 private UtilityRenderChecks(){}
}
