package com.example.chemistry.organic;
import com.example.chemistry.*;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.item.LabVesselItem;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
public final class WatchGlassGameTests {
 private static ItemStack stack(String id){return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry",id)));}
 private static void check(GameTestHelper h,boolean ok,String msg){h.assertTrue(ok,Component.literal(msg));}
 public static void cover(GameTestHelper h){h.runAtTickTime(1,()->{
  for(String id:java.util.List.of("beaker_100ml","beaker_medium","beaker_tall")){
   var s=stack(id);LabVesselItem.addMass(s,"liquid","water",100);
   check(h,OrganicApparatus.cover(s,true),"beaker refused watch glass");
   check(h,!VesselHeating.isSealed(s),"loose watch glass became pressure seal");
   var copy=s.copy();check(h,OrganicApparatus.covered(copy),"pickup lost cover");
   check(h,!com.example.chemistry.filtration.Filtration.pour(copy,new ItemStack(ModItems.ERLENMEYER_FLASK.get()),25),"covered beaker poured");
   check(h,com.example.chemistry.titration.LiquidTransfer.pour(copy,new ItemStack(ModItems.ERLENMEYER_FLASK.get()),25)==0,"covered beaker sampled");
   check(h,OrganicApparatus.cover(copy,false),"cover did not detach");
   check(h,OrganicApparatus.guidedPour(copy,new ItemStack(ModItems.ERLENMEYER_FLASK.get())),"uncovered pour failed");
  }h.succeed();
 });}
 public static void interaction(GameTestHelper h){h.runAtTickTime(1,()->{
  var p=h.makeMockPlayer(GameType.SURVIVAL);var s=stack("beaker_medium");
  var dish=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry","watch_glass")));
  p.setItemInHand(InteractionHand.MAIN_HAND,dish);final ItemStack[] saved={s};
  check(h,OrganicApparatus.interact(p,s,v->saved[0]=v,p.position()),"cover click missed");
  check(h,dish.isEmpty()&&OrganicApparatus.covered(saved[0]),"cover consumption failed");
  p.setShiftKeyDown(true);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
  check(h,OrganicApparatus.interact(p,saved[0],v->saved[0]=v,p.position()),"uncover click missed");
  check(h,!OrganicApparatus.covered(saved[0]),"cover flag retained");
  check(h,p.getInventory().contains(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry","watch_glass")))),"watch glass not returned");h.succeed();
 });}
 private WatchGlassGameTests(){}
}
