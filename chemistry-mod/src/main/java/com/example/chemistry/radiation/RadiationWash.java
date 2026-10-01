package com.example.chemistry.radiation;
import com.example.chemistry.ChemistryMod;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.*;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
/** Rinsing transfers real residue into a separate waste bottle; it never deletes radionuclides. */
@EventBusSubscriber(modid=ChemistryMod.MODID)
public final class RadiationWash {
 public static boolean collect(ItemStack source,ItemStack waste,int waterMl){
  if(RadioLedger.tag(source).contains("radio_surface")){var sample=new ItemStack(ModItems.RADIOACTIVE_WASTE_BOTTLE.get());sample.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(RadioLedger.tag(source).getCompoundOrEmpty("radio_surface")));if(!collect(sample,waste,waterMl))return false;var clean=RadioLedger.tag(source);clean.remove("radio_surface");clean.remove("radio_nuclides");source.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(clean));return true;}
  if(!(source.getItem() instanceof LabVesselItem)||!waste.is(ModItems.RADIOACTIVE_WASTE_BOTTLE.get())||source==waste||RadioLedger.carriers(source).isEmpty())return false;
  var trial=waste.copy();for(var e:LabVesselItem.getContents(source))LabVesselItem.addMass(trial,e.type(),e.id(),e.amount());
  if(waterMl>0){LabVesselItem.addMass(trial,"liquid","water",waterMl*.9995);LabVesselItem.addMass(trial,"liquid","calcium_chloride_solution",waterMl*.0002);LabVesselItem.addMass(trial,"liquid","magnesium_chloride_solution",waterMl*.0002);LabVesselItem.addMass(trial,"liquid","sodium_chloride_solution",waterMl*.0001);}
  if(LabVesselItem.usedVolume(trial)>10000+1e-6)return false;RadioLedger.inherit(source,waste,trial);com.example.chemistry.filtration.Filtration.commit(waste,trial);LabVesselItem.clearContents(source);return true;
 }
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void wash(PlayerInteractEvent.RightClickBlock e){
  if(e.getLevel().isClientSide()||e.getHand()!=InteractionHand.MAIN_HAND||!e.getEntity().isShiftKeyDown())return;
  Player p=e.getEntity();var source=p.getMainHandItem();var waste=p.getOffhandItem();if(!waste.is(ModItems.RADIOACTIVE_WASTE_BOTTLE.get()))return;
  if(e.getLevel().getBlockEntity(e.getPos()) instanceof com.example.chemistry.blockentity.LaboratoryBenchBlockEntity bench&&((com.example.chemistry.block.LaboratoryBenchBlock)bench.getBlockState().getBlock()).variant()==2&&(source.getItem() instanceof LabVesselItem||RadiationGear.isSuit(source)||source.isEmpty())){
   String message;
   if(!bench.flowing()||bench.waterMl()<25)message="请开启水龙头，并保证水箱至少有25 mL水";
   else if(com.example.chemistry.VesselHeating.isSealed(source))message="请先打开待清洗的容器";
   else if(source.isEmpty()&&p.getPersistentData().contains("chem_radio_skin")){
    var skin=new ItemStack(ModItems.RADIOACTIVE_WASTE_BOTTLE.get());skin.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(p.getPersistentData().getCompoundOrEmpty("chem_radio_skin")));
    if(collect(skin,waste,25)){bench.consumeWater(25);p.getPersistentData().remove("chem_radio_skin");message="已清洗皮肤表面污染；累计辐射剂量仍保留";}else message="废液瓶已满";
   }
   else if(collect(source,waste,25)){bench.consumeWater(25);message="已将残留及25 mL冲洗水转入放射性废液瓶";e.getLevel().playSound(null,e.getPos(),net.minecraft.sounds.SoundEvents.BOTTLE_FILL,net.minecraft.sounds.SoundSource.PLAYERS,.5F,1);}
   else message="没有放射性残留，或废液瓶已满";
   p.displayClientMessage(net.minecraft.network.chat.Component.literal(message),true);e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
  }else if(source.is(net.minecraft.world.item.Items.WATER_BUCKET)&&e.getLevel() instanceof ServerLevel level){
   var state=RadiationContamination.get(level);var dirty=state.at(e.getPos());if(dirty.isEmpty())return;
   if(collect(dirty,waste,1000)){state.remove(e.getPos());if(!p.isCreative())p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(net.minecraft.world.item.Items.BUCKET));p.displayClientMessage(net.minecraft.network.chat.Component.literal("污染物与冲洗水已收集；废液瓶仍具有放射性"),true);}else p.displayClientMessage(net.minecraft.network.chat.Component.literal("废液瓶容量不足"),true);
   e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
  }
 }
 private RadiationWash(){}
}
