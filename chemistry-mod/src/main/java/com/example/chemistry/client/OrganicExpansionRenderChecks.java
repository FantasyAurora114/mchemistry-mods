package com.example.chemistry.client;
import com.example.chemistry.ChemistryMod;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.organic.*;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.transfer.BottleCodes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid=ChemistryMod.MODID,value=Dist.CLIENT)
public final class OrganicExpansionRenderChecks {
 private static int stage,ticks;private static Screen previous;
 private static void verify(ItemStack s){var mc=Minecraft.getInstance();var state=new ItemStackRenderState();mc.getItemModelResolver().updateForTopItem(state,s,ItemDisplayContext.GUI,mc.level,null,0);if(state.isEmpty())throw new IllegalStateException("Expansion empty model "+s);}
 @SubscribeEvent public static void tick(ClientTickEvent.Post e){var mc=Minecraft.getInstance();if(!Boolean.getBoolean("mchemistry.verifyOrganicExpansion")||mc.getOverlay()!=null||stage==3||++ticks<240)return;
  if(stage==0){
   for(String id:new String[]{"watch_glass","buchner_funnel","suction_flask","glass_rod","lab_ceiling_tile","lab_ceiling_light","lab_voltmeter","lab_resistor"})verify(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath("mchemistry",id))));
   for(var ref:ModItems.ISOTOPES)verify(new ItemStack(ref.get()));
   for(String id:new String[]{"watch_glass","buchner_funnel","glass_rod"})if(ElectricalModels.get(id)==null)throw new IllegalStateException("Expansion standalone model absent");
   for(var s:OrganicStructures.ALL.values())if(mc.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("mchemistry","textures/structures/"+s.id()+".png")).isEmpty())throw new IllegalStateException("Missing PubChem structure "+s.id());
   var beaker=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath("mchemistry","beaker_medium")));LabVesselItem.addMass(beaker,"liquid","water",250);OrganicApparatus.cover(beaker,true);verify(beaker);
   previous=mc.screen;mc.setScreen(new StructureScreen());stage=1;ticks=0;
  }else if(stage==1&&ticks>=30){Screenshot.grab(mc.gameDirectory,"organic-structure-tooltip.png",mc.getMainRenderTarget(),1,message->ChemistryMod.LOGGER.info("Organic tooltip screenshot: {}",message.getString()));stage=2;ticks=0;
  }else if(stage==2&&ticks>=20){mc.setScreen(previous);ChemistryMod.LOGGER.info("Organic expansion client checks passed: apparatus, isotopes, covered beaker, structure resources and rendered tooltip");stage=3;}
 }
 private static final class StructureScreen extends Screen {
  private final ItemStack jar=new ItemStack(ModItems.SOLID_JAR.get());
  StructureScreen(){super(Component.literal("PubChem structure check"));BottleCodes.setSolid(jar,"caffeine",true);BottleCodes.refreshModel(jar);}
  @Override public void render(GuiGraphics g,int x,int y,float delta){g.drawString(font,"MChemistry / authoritative structure tooltip",20,15,0xffffffff,false);g.renderItem(jar,40,45);g.setTooltipForNextFrame(font,jar,65,45);}
 }
 private OrganicExpansionRenderChecks(){}
}
