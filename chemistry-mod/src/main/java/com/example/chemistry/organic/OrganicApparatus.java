package com.example.chemistry.organic;
import com.example.chemistry.*;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.filtration.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
/** A loose watch glass is a vented cover, never a pressure-tight stopper. */
@EventBusSubscriber(modid=ChemistryMod.MODID)
public final class OrganicApparatus {
 public static boolean is(ItemStack s,String id){return com.example.chemistry.transfer.BottleCodes.pathOf(s).equals(id);}
 public static boolean covered(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("watch_glass",false);}
 public static boolean beaker(ItemStack s){int t=VesselHeating.vesselType(s);return t==5||t==8||t==9;}
 public static boolean cover(ItemStack s,boolean cover){if(!beaker(s)||covered(s)==cover)return false;var t=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();t.putBoolean("watch_glass",cover);s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));LabVesselItem.updateTint(s);return true;}
 public static boolean guidedPour(ItemStack from,ItemStack to){return !covered(from)&&!covered(to)&&Filtration.pour(from,to,25);}
 public static boolean interact(Player p,ItemStack vessel,java.util.function.Consumer<ItemStack> save,Vec3 at){
  var held=p.getMainHandItem();if(is(held,"watch_glass")&&beaker(vessel)){if(cover(vessel,true)){if(!p.isCreative())held.shrink(1);save.accept(vessel);}return true;}
  if(covered(vessel)){
   if(held.isEmpty()&&p.isShiftKeyDown()){cover(vessel,false);save.accept(vessel);var dish=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry","watch_glass")));if(!p.getInventory().add(dish))p.drop(dish,false);}
   else p.displayClientMessage(net.minecraft.network.chat.Component.literal("潜行空手取下表面皿；盖子防尘但不气密"),true);return true;
  }
  if(held.getItem() instanceof LabVesselItem&&p.getOffhandItem().is(ModItems.GLASS_ROD.get())){
   if(guidedPour(held,vessel)){save.accept(vessel);if(p.level() instanceof ServerLevel level){for(int i=0;i<8;i++){var point=p.getEyePosition().lerp(at,i/8.0);level.sendParticles(net.minecraft.core.particles.ParticleTypes.FALLING_WATER,point.x,point.y,point.z,1,0,0,0,0);}level.playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.BOTTLE_EMPTY,net.minecraft.sounds.SoundSource.PLAYERS,.5F,1);}p.displayClientMessage(net.minecraft.network.chat.Component.literal("沿玻璃棒引流，最多转移25 mL"),true);}else p.displayClientMessage(net.minecraft.network.chat.Component.literal("检查盖子、接收容量和内容物"),true);return true;
  }return false;
 }
 public static InteractionResult placed(com.example.chemistry.entity.PlacedVesselEntity v,Player p,InteractionHand hand){
  if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
  var stack=v.getVessel();var held=p.getItemInHand(hand);
  boolean relevant=covered(stack)||(beaker(stack)&&is(held,"watch_glass"))
    ||(held.getItem() instanceof LabVesselItem&&p.getOffhandItem().is(ModItems.GLASS_ROD.get()));
  if(!relevant)return InteractionResult.PASS;
  if(v.level().isClientSide())return InteractionResult.SUCCESS;
  return interact(p,stack,v::setVessel,v.position().add(0,.5,0))?InteractionResult.SUCCESS:InteractionResult.PASS;
 }
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void block(PlayerInteractEvent.RightClickBlock e){if(e.getHand()!=InteractionHand.MAIN_HAND||e.getLevel().isClientSide())return;boolean handled=false;
  if(e.getLevel().getBlockEntity(e.getPos()) instanceof com.example.chemistry.blockentity.PlacedVesselBlockEntity v)handled=interact(e.getEntity(),v.getVessel(),v::setVessel,Vec3.atCenterOf(e.getPos()).add(0,.4,0));
  if(handled){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}
 }
 @SubscribeEvent public static void tooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent e){if(covered(e.getItemStack()))e.getToolTip().add(net.minecraft.network.chat.Component.literal("已盖表面皿；放置后潜行空手取下（非气密）"));}
 private OrganicApparatus(){}
}
