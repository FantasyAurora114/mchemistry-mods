package com.example.chemistry.client;
import com.example.chemistry.ChemistryMod;
import com.example.chemistry.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid=ChemistryMod.MODID,value=Dist.CLIENT)
public final class RadiationRenderChecks {
 private static boolean done;
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){if(done||!Boolean.getBoolean("mchemistry.verifyRadiation")||Minecraft.getInstance().getOverlay()!=null)return;done=true;var mc=Minecraft.getInstance();int count=0;
  for(var item:java.util.List.of(ModItems.GEIGER_COUNTER.get(),ModItems.RADIATION_HOOD.get(),ModItems.RADIATION_SUIT.get(),ModItems.RADIATION_LEGGINGS.get(),ModItems.RADIATION_BOOTS.get(),ModItems.RADIATION_SHIELD_BOX.get(),ModItems.LEAD_LINED_CABINET.get(),ModItems.RADIOACTIVE_WASTE_BOTTLE.get(),ModItems.STRAIGHT_GLASS_TUBE.get(),ModItems.STRAIGHT_GLASS_TUBE_LONG.get(),ModItems.RIGHT_ANGLE_GLASS_TUBE.get(),ModItems.RIGHT_ANGLE_GLASS_TUBE_LONG.get()))for(var context:java.util.List.of(ItemDisplayContext.GUI,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)){
   var state=new ItemStackRenderState();mc.getItemModelResolver().updateForTopItem(state,new ItemStack(item),context,mc.level,null,0);if(state.isEmpty()||state.getModelBoundingBox().getYsize()<=0)throw new IllegalStateException("Radiation/tube empty model "+item);count++;
  }
  for(var e:com.example.chemistry.data.RadioElements.ALL)for(String form:java.util.List.of("ingot","dust","nugget")){
   var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry","element_"+e.id()+"_"+form));var state=new ItemStackRenderState();mc.getItemModelResolver().updateForTopItem(state,new ItemStack(item),ItemDisplayContext.GUI,mc.level,null,0);if(state.isEmpty())throw new IllegalStateException("Radio element model missing");count++;
  }
  for(var shape:java.util.List.of("straight_glass_tube","straight_glass_tube_long","right_angle_glass_tube","right_angle_glass_tube_long")){
   var tex=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry","textures/item/"+shape+"_gui.png");if(mc.getResourceManager().getResource(tex).isEmpty())throw new IllegalStateException("Tube GUI texture missing");
  }
  LaboratoryBenchRenderer.verifyDoorGeometry();
  ChemistryMod.LOGGER.info("Radiation render checks passed: {} equipment/storage/tube/element model states",count);
 }
 private RadiationRenderChecks(){}
}
