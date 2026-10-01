package com.example.chemistry.radiation;
import java.util.*;
import com.example.chemistry.data.*;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.solution.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
public final class RadiationGameTests {
 private static ItemStack vessel(){return new ItemStack(ModItems.LAB_VESSELS.stream().filter(i->i.getId().getPath().equals("beaker_medium")).findFirst().orElseThrow().get());}
 private static void check(boolean b,String m){if(!b)throw new IllegalStateException(m);}
 public static void registry(GameTestHelper h){h.runAtTickTime(1,()->{
  for(var r:RadioChemicals.ALL){check(Math.abs(BatchChemistry.molar("solid",r.id())-r.mass())<1e-7,"radio formula "+r.id());var jar=ModItems.solidJar(r.id(),true);check(!RadioLedger.carriers(jar).isEmpty()&&RadioLedger.activity(jar)>0,"inactive radio jar "+r.id());}
  check(!Instruments.ALL.contains("beaker"),"old placeholder retained");check(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry","beaker"))==net.minecraft.world.item.Items.AIR,"placeholder registration remains");
  var co=ModItems.solidJar("cobalt_chloride",true);check(RadioLedger.carriers(co).isEmpty(),"stable cobalt becomes radioactive");check(RadioLedger.activity(ModItems.gasBottle("radon",true))>0,"radon inactive");h.succeed();
 });}
 public static void decay(GameTestHelper h){h.runAtTickTime(1,()->{
  for(String root:java.util.Set.copyOf(RadioChemicals.ROOTS.values())){var n=RadioLedger.NUCLIDES.get(root);var a=RadioLedger.advance(Map.of(root,1.0),n.halfSeconds());check(Math.abs(a.fractions().get(root)-.5)<1e-8,"half life "+root);check(Math.abs(RadioLedger.nucleons(a.fractions())-n.a())<1e-7,"nucleon loss "+root);check(a.mev()>0,"decay heat absent "+root);}
  var gasSample=ModItems.gasBottle("radon",true);RadioLedger.tick(gasSample,0,1);RadioLedger.tick(gasSample,1000,100);
  var gas=new com.example.chemistry.entity.GasCollectingBottleEntity(com.example.chemistry.registry.ModEntities.GAS_COLLECTING_BOTTLE.get(),h.getLevel());gas.setGasId("radon");gas.readFromItem(gasSample);var state=gas.toStack();check(RadioLedger.fractions(state,"Rn222").equals(RadioLedger.fractions(gasSample,"Rn222")),"placed gas resets nuclear state");
  var stable=RadioLedger.advance(Map.of("Pb206",1.0),1e9);check(stable.fractions().equals(Map.of("Pb206",1.0))&&stable.mev()==0,"stable daughter decays");h.succeed();
 });}
 public static void transfer(GameTestHelper h){h.runAtTickTime(1,()->{
  var from=vessel();LabVesselItem.addMass(from,"liquid","water",100);LabVesselItem.addMass(from,"liquid","sodium_iodide_131_solution",1);
  RadioLedger.tick(from,0,1);RadioLedger.tick(from,(long)(RadioLedger.NUCLIDES.get("I131").halfSeconds()*20),1);var before=SolutionSpecies.analyticalSnapshot(from);var to=vessel();
  check(com.example.chemistry.filtration.Filtration.pour(from,to,50),"radio pour failed");check(Math.abs(RadioLedger.fractions(to,"I131").get("I131")-.5)<1e-6,"transfer resets isotope age");
  double moles=RadioLedger.carriers(from).get("I131")+RadioLedger.carriers(to).get("I131");check(Math.abs(moles-1/BatchChemistry.molar("liquid","sodium_iodide_131_solution"))<1e-10,"transfer duplicates tracer");
  check(Conservation.compare(before,SolutionSpecies.analyticalSnapshot(from).plus(SolutionSpecies.analyticalSnapshot(to))).conserved(),"chemical carrier loss");
  var copy=from.copy();check(RadioLedger.fractions(copy,"I131").equals(RadioLedger.fractions(from,"I131")),"NBT copy loses nuclear state");
  var stock=ModItems.liquidBottle("sodium_iodide_131_solution",false);RadioLedger.tick(stock,0,1);RadioLedger.tick(stock,(long)(RadioLedger.NUCLIDES.get("I131").halfSeconds()*20),1);
  var dropper=new ItemStack(ModItems.DROPPER.get());check(com.example.chemistry.transfer.BottleQuantities.fillDropper(stock,dropper),"radio dropper filling failed");check(Math.abs(RadioLedger.fractions(dropper,"I131").get("I131")-.5)<1e-6,"dropper resets tracer age");
  var sampled=vessel();check(com.example.chemistry.item.DropperHelper.pour(dropper,sampled,true),"radio drop failed");check(Math.abs(RadioLedger.fractions(sampled,"I131").get("I131")-.5)<1e-6,"drop resets tracer age");h.succeed();
 });}
 public static void washing(GameTestHelper h){h.runAtTickTime(1,()->{
  var dirty=vessel();LabVesselItem.addMass(dirty,"liquid","water",10);LabVesselItem.addMass(dirty,"liquid","uranyl_nitrate_solution",.1);var receiver=vessel();check(com.example.chemistry.filtration.Filtration.pour(dirty,receiver,Double.MAX_VALUE),"pour fails");check(LabVesselItem.getContents(dirty).size()>0,"no actual film");
  var waste=new ItemStack(ModItems.RADIOACTIVE_WASTE_BOTTLE.get());double before=RadioLedger.carriers(dirty).get("U238");check(RadiationWash.collect(dirty,waste,25),"wash fails");check(RadioLedger.carriers(dirty).isEmpty(),"vessel remains dirty");check(Math.abs(RadioLedger.carriers(waste).get("U238")-before)<1e-12,"washing deletes isotope");check(BatchChemistry.mass(waste,"liquid","water")>=24.98,"rinse water missing");
  var full=new ItemStack(ModItems.RADIOACTIVE_WASTE_BOTTLE.get());LabVesselItem.addMass(full,"liquid","water",9999);var s=receiver.copy();double n=RadioLedger.activity(s);check(!RadiationWash.collect(s,full,25)&&Math.abs(RadioLedger.activity(s)-n)<1e-8,"full waste destroys sample");h.succeed();
 });}
 public static void shielding(GameTestHelper h){h.runAtTickTime(1,()->{
  check(RadiationSystem.transmission(5,1,1,false,false,0)<RadiationSystem.transmission(1,1,1,false,false,0),"distance has no effect");check(RadiationSystem.transmission(1,.02,1,false,false,0)<RadiationSystem.transmission(1,1,1,false,false,0),"shield ineffective");
  check(RadiationSystem.transmission(1,1,0,false,true,4)<RadiationSystem.transmission(1,1,0,false,true,0),"suit fails contamination");check(RadiationSystem.transmission(1,1,1,false,true,4)==RadiationSystem.transmission(1,1,1,false,true,0),"cloth blocks gamma");
  var p=new net.minecraft.core.BlockPos(1,1,1);h.setBlock(p,com.example.chemistry.registry.ModBlocks.RADIATION_SHIELD_BOX.get());var be=h.getBlockEntity(p,ShieldedStorageBlockEntity.class);be.setItem(0,ModItems.solidJar("cobalt_chloride_60",true));var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
  check(be.closed(),"new storage not sealed");be.startOpen(player);check(!be.closed(),"menu does not open shielding");be.stopOpen(player);check(be.closed(),"menu does not restore shielding");check(!be.getItem(0).isEmpty(),"storage loses sample");h.succeed();
 });}
 public static void armor(GameTestHelper h){h.runAtTickTime(1,()->{
  var pairs=List.of(new ItemStack[]{new ItemStack(ModItems.RADIATION_HOOD.get()),new ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET)},new ItemStack[]{new ItemStack(ModItems.RADIATION_SUIT.get()),new ItemStack(net.minecraft.world.item.Items.LEATHER_CHESTPLATE)},new ItemStack[]{new ItemStack(ModItems.RADIATION_LEGGINGS.get()),new ItemStack(net.minecraft.world.item.Items.LEATHER_LEGGINGS)},new ItemStack[]{new ItemStack(ModItems.RADIATION_BOOTS.get()),new ItemStack(net.minecraft.world.item.Items.LEATHER_BOOTS)});
  for(var pair:pairs){check(pair[0].getMaxDamage()==pair[1].getMaxDamage(),"durability differs from leather");var a=pair[0].get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);var b=pair[1].get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);double armor=a.modifiers().stream().filter(e->e.attribute().equals(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)).mapToDouble(e->e.modifier().amount()).sum(),base=b.modifiers().stream().filter(e->e.attribute().equals(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)).mapToDouble(e->e.modifier().amount()).sum();check(armor==base,"armor stronger than leather");check(a.modifiers().stream().anyMatch(e->e.attribute().equals(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)&&e.modifier().amount()<0),"no movement penalty");}
  h.succeed();
 });}
 public static void contamination(GameTestHelper h){h.runAtTickTime(1,()->{
  var state=new RadiationContamination(Map.of());var s=vessel();LabVesselItem.addMass(s,"liquid","water",10);LabVesselItem.addMass(s,"liquid","sodium_iodide_131_solution",.1);var p=new net.minecraft.core.BlockPos(7,2,8);state.deposit(p,s);
  double before=RadioLedger.activity(state.at(p));var codec=RadiationContamination.CODEC;var ops=net.minecraft.nbt.NbtOps.INSTANCE;var encoded=codec.encodeStart(ops,state).getOrThrow();var restored=codec.parse(ops,encoded).getOrThrow();check(Math.abs(RadioLedger.activity(restored.at(p))-before)<1e-6,"saved pollution differs");
  var waste=new ItemStack(ModItems.RADIOACTIVE_WASTE_BOTTLE.get());check(RadiationWash.collect(restored.at(p),waste,1000),"ground washing failed");restored.remove(p);check(restored.at(p).isEmpty()&&RadioLedger.activity(waste)>0,"cleanup deletes radioactivity");h.succeed();
 });}
 private RadiationGameTests(){}
}
