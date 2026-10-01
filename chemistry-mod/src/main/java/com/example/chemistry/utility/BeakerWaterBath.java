package com.example.chemistry.utility;

import com.example.chemistry.*;
import com.example.chemistry.entity.PlacedVesselEntity;
import com.example.chemistry.item.LabVesselItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import java.util.List;

/** One separately owned inner vessel; only the outer vessel receives burner heat. */
public final class BeakerWaterBath {
    private static final String CHILD="chem_bath_vessel",DISPLACED="chem_bath_displacement_ml";
    public static ItemStack inner(Level level,ItemStack outer){var tag=outer.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var value=tag.get(CHILD);return value==null?ItemStack.EMPTY:ItemStack.CODEC.parse(level.registryAccess().createSerializationContext(NbtOps.INSTANCE),value).result().orElse(ItemStack.EMPTY);}
    public static void inner(Level level,ItemStack outer,ItemStack child){var tag=outer.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(child.isEmpty()){tag.remove(CHILD);tag.remove(DISPLACED);}else{var copy=child.copyWithCount(1);var ct=copy.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();ct.putBoolean("chem_bath_inner",true);copy.set(DataComponents.CUSTOM_DATA,CustomData.of(ct));tag.put(CHILD,ItemStack.CODEC.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE),copy).getOrThrow());tag.putDouble(DISPLACED,((LabVesselItem)child.getItem()).capacity()*.35);}outer.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));LabVesselItem.updateTint(outer);}
    public static double displacement(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getDoubleOr(DISPLACED,0);}
    public static boolean canInstall(Level l,ItemStack outer,ItemStack child){if(!com.example.chemistry.garden.ChemicalGarden.beaker(outer)||VesselHeating.isSealed(outer)||!(child.getItem() instanceof LabVesselItem small)||!(outer.getItem() instanceof LabVesselItem large)||!inner(l,outer).isEmpty())return false;String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(child.getItem()).getPath();return (id.startsWith("test_tube_")||id.startsWith("beaker_"))&&small.capacity()<=large.capacity()/2&&LabVesselItem.usedVolume(outer)+small.capacity()*.35<=large.capacity()+1e-8&&child.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().get(CHILD)==null;}
    public static double water(ItemStack s){return com.example.chemistry.garden.ChemicalGarden.mass(s,"liquid","water");}
    public static double exchange(ItemStack outer,ItemStack child){if(water(outer)<=1e-9)return 0;double a=TemperatureSystem.getTemp(outer),b=TemperatureSystem.getTemp(child),ca=ThermalSystem.capacity(outer),cb=ThermalSystem.capacity(child);double contact=Math.clamp(water(outer)/Math.max(1,displacement(outer)),0,1);String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(child.getItem()).getPath();double q=(a-b)*contact*(id.contains("dewar")?.08:.8);double equilibrium=(a-b)/(1/ca+1/cb);q=Math.copySign(Math.min(Math.abs(q),Math.abs(equilibrium)),q);ThermalSystem.addHeat(outer,-q,"water_bath");ThermalSystem.addHeat(child,q,"water_bath");return q;}
    public static void tick(Level l,net.minecraft.core.BlockPos pos,ItemStack outer){var child=inner(l,outer);if(child.isEmpty())return;exchange(outer,child);int holes=VesselHeating.getStopperHoles(child);VesselHeating.Outcome result=VesselHeating.tick(child,l,pos,ItemStack.EMPTY,false,false,null);if(result==VesselHeating.Outcome.POPPED){VesselHeating.unseal(child);if(l instanceof net.minecraft.server.level.ServerLevel sl)sl.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(sl,pos.getX()+.5,pos.getY()+.8,pos.getZ()+.5,new ItemStack(com.example.chemistry.registry.ModItems.stopperForHoles(holes))));}inner(l,outer,child);}
    public static ItemStack take(Level l,ItemStack outer){var child=inner(l,outer);inner(l,outer,ItemStack.EMPTY);if(!child.isEmpty()){var tag=child.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.remove("chem_bath_inner");child.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}return child;}
    private static boolean aim(PlacedVesselEntity e,Player p){double scale=e.getMountScale();return new AABB(e.getX()-.13*scale,e.getY()+.2*scale,e.getZ()-.13*scale,e.getX()+.13*scale,e.getY()+1.0*scale,e.getZ()+.13*scale).clip(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(6))).isPresent();}
    public static InteractionResult interact(PlacedVesselEntity e,Player p,InteractionHand hand){var outer=e.getVessel();if(!com.example.chemistry.garden.ChemicalGarden.beaker(outer))return InteractionResult.PASS;var held=p.getItemInHand(hand);var child=inner(e.level(),outer);if(!p.isShiftKeyDown()&&held.getItem() instanceof LabVesselItem&&canInstall(e.level(),outer,held)){if(!e.level().isClientSide()){inner(e.level(),outer,held);held.shrink(1);e.setVessel(outer);p.displayClientMessage(Component.literal("已装入水浴容器；潜行点内层容器取出"),true);}return InteractionResult.SUCCESS;}if(!child.isEmpty()&&aim(e,p)){if(held.isEmpty()&&p.isShiftKeyDown()){if(!e.level().isClientSide()){var removed=take(e.level(),outer);e.setVessel(outer);if(!p.getInventory().add(removed))p.drop(removed,false);}return InteractionResult.SUCCESS;}if(!held.isEmpty()&&!(held.getItem() instanceof LabVesselItem)&&!e.level().isClientSide()&&LabInteractions.interactPlacedVessel(held,child,ItemStack.EMPTY,ItemStack.EMPTY,p)){inner(e.level(),outer,child);e.setVessel(outer);return InteractionResult.SUCCESS;}}return InteractionResult.PASS;}
    public static void info(Level l,ItemStack outer,List<Component> lines){var child=inner(l,outer);if(child.isEmpty())return;lines.add(Component.literal(String.format(java.util.Locale.ROOT,"水浴：外层 %.1f°C；内层 %.1f°C%s",TemperatureSystem.getTemp(outer),TemperatureSystem.getTemp(child),water(outer)>0?"":"（外层缺水）")));lines.add(Component.literal("内层 "+child.getHoverName().getString()+"："));com.example.chemistry.api.goggles.ChemGoggleLines.appendContents(lines,child);}
    private BeakerWaterBath(){}
}
