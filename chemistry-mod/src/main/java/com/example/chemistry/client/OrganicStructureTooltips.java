package com.example.chemistry.client;
import com.example.chemistry.ChemistryMod;
import com.example.chemistry.organic.OrganicStructures;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
/** Scientific diagrams are the archived PubChem depictions, not generative molecular drawings. */
@EventBusSubscriber(modid=ChemistryMod.MODID,value=Dist.CLIENT)
public final class OrganicStructureTooltips {
 public record StructureCard(OrganicStructures.Structure structure) implements TooltipComponent {}
 @SubscribeEvent public static void gather(RenderTooltipEvent.GatherComponents e){var s=OrganicStructures.of(e.getItemStack());if(s!=null)e.getTooltipElements().add(Math.min(1,e.getTooltipElements().size()),Either.right(new StructureCard(s)));}
 @SubscribeEvent public static void register(RegisterClientTooltipComponentFactoriesEvent e){e.register(StructureCard.class,Card::new);}
 public static final class Card implements ClientTooltipComponent {
  private final OrganicStructures.Structure structure;
  public Card(StructureCard card){structure=card.structure();}
  public int getHeight(Font font){return 72;}
  public int getWidth(Font font){return Math.max(100,font.width("PubChem CID "+structure.cid()));}
  public void renderImage(Font font,int x,int y,int width,int height,GuiGraphics gui){
   gui.blit(ResourceLocation.fromNamespaceAndPath("mchemistry","textures/structures/"+structure.id()+".png"),x,y,x+90,y+60,0F,1F,0F,1F);
   gui.drawString(font,"PubChem CID "+structure.cid(),x,y+61,0xffa0baca,false);
  }
 }
 @SubscribeEvent public static void hazard(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent e){if(!com.mojang.blaze3d.platform.InputConstants.isKeyDown(net.minecraft.client.Minecraft.getInstance().getWindow(),org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)&&!com.mojang.blaze3d.platform.InputConstants.isKeyDown(net.minecraft.client.Minecraft.getInstance().getWindow(),org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT))return;var structure=OrganicStructures.of(e.getItemStack());if(structure!=null){String codes=com.example.chemistry.organic.OrganicHazardSources.CODES.get(structure.id());if(codes!=null){e.getToolTip().add(net.minecraft.network.chat.Component.literal("PubChem GHS（供应商汇总，详见来源记录）"));for(int start=0;start<codes.length();start+=44)e.getToolTip().add(net.minecraft.network.chat.Component.literal(codes.substring(start,Math.min(codes.length(),start+44))));}}}
 private OrganicStructureTooltips(){}
}
