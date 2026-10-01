package com.example.chemistry.solution;
import java.util.Map;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.data.FutureChemicals;
import com.example.chemistry.data.FutureReactions;
import com.example.chemistry.TemperatureSystem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.gametest.framework.GameTestHelper;
public final class FutureGameTests {
 private static ItemStack vessel(){return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry","beaker_medium")));}
 private static void add(ItemStack s,String id,double grams){LabVesselItem.addMass(s,"liquid",id,grams);}
 private static void check(boolean v,String msg){if(!v)throw new IllegalStateException(msg);}
 public static void formulas(GameTestHelper h){h.runAtTickTime(1,()->{
  for(var r:FutureChemicals.ALL) check(Math.abs(BatchChemistry.molar(r.phase().equals("SOLID")?"solid":"liquid",r.id())-r.mass())<1e-7,"formula "+r.id());
  var ids=new java.util.HashSet<String>();for(var e:com.example.chemistry.data.Elements.ALL)check(ids.add(e.symbol()),"duplicate element "+e.symbol());
  check(ids.size()>=77,"missing elements");
  for(String gas:java.util.List.of("phosgene","ozone","dinitrogen_tetroxide","hydrogen_fluoride","hydrogen_bromide","hydrogen_iodide","boron_trifluoride","silane")){
   for(boolean tall:new boolean[]{false,true}){var cylinder=com.example.chemistry.item.GasCylinderItem.filled(tall,gas,tall?100000:10000);check(cylinder.getItem() instanceof com.example.chemistry.item.GasCylinderItem,"missing cylinder "+gas);var item=(com.example.chemistry.item.GasCylinderItem)cylinder.getItem();check(item.remaining(cylinder)==(tall?100000:10000)&&item.gas(cylinder).equals(gas),"cylinder quantity "+gas);}
  }h.succeed();
 });}
 public static void reactions(GameTestHelper h){h.runAtTickTime(1,()->{
  for(var reaction:FutureReactions.ALL){var s=vessel();add(s,"water",100);var lhs=new java.util.HashMap<String,Integer>();var rhs=new java.util.HashMap<String,Integer>();
   reaction.reactants().forEach(e->{lhs.put(e.type()+":"+e.id(),e.coefficient());LabVesselItem.addMass(s,e.type(),e.id(),e.coefficient()*BatchChemistry.molar(e.type(),e.id())*.001);});
   reaction.products().forEach(e->rhs.put(e.type()+":"+e.id(),e.coefficient()));var before=SolutionSpecies.analyticalSnapshot(s);
   check(before.fullyModelled(),"unknown reactant "+reaction.display());check(BatchChemistry.transact(s,lhs,rhs,.0005),"unbalanced "+reaction.display());
   check(Conservation.compare(before,SolutionSpecies.analyticalSnapshot(s)).conserved(),"mass/atom loss "+reaction.display());
  }h.succeed();
 });}
 public static void sugars(GameTestHelper h){h.runAtTickTime(1,()->{
  var s=vessel();add(s,"water",100);add(s,"sucrose_solution",1);TemperatureSystem.setTemp(s,80);
  FutureChemistry.tick(s);check(BatchChemistry.mass(s,"liquid","glucose_solution")==0,"hydrolysis without catalyst");
  add(s,"hydrochloric_acid",.1);var before=SolutionSpecies.analyticalSnapshot(s);FutureChemistry.tick(s);
  check(BatchChemistry.mass(s,"liquid","glucose_solution")>0&&BatchChemistry.mass(s,"liquid","fructose_solution")>0,"hydrolysis inactive");
  check(Math.abs(BatchChemistry.mass(s,"liquid","hydrochloric_acid")-.1)<1e-9,"catalyst consumed");check(Conservation.compare(before,SolutionSpecies.analyticalSnapshot(s)).conserved(),"hydrolysis mass loss");
  var f=vessel();add(f,"water",100);add(f,"fructose_solution",1);LabVesselItem.addMass(f,"solid","copper_ii_hydroxide",1);TemperatureSystem.setTemp(f,80);FutureChemistry.tick(f);
  check(BatchChemistry.mass(f,"solid","copper_i_oxide")==0,"fructose requires alkaline isomerization");add(f,"sodium_hydroxide_solution",.01);FutureChemistry.tick(f);check(BatchChemistry.mass(f,"solid","copper_i_oxide")>0,"alkaline reducing sugar inactive");
  var t=vessel();add(t,"water",100);LabVesselItem.addMass(t,"solid","starch",1);int plain=LabVesselItem.contentsColor(t);add(t,"iodine_water",.001);check(plain!=LabVesselItem.contentsColor(t),"starch colour missing");h.succeed();
 });}
 public static void ester(GameTestHelper h){h.runAtTickTime(1,()->{
  var s=vessel();add(s,"water",1);add(s,"lactic_acid",9);add(s,"ethanol",5);add(s,"hydrochloric_acid",.001);TemperatureSystem.setTemp(s,60);var before=SolutionSpecies.analyticalSnapshot(s);
  FutureChemistry.tick(s);check(BatchChemistry.mass(s,"liquid","ethyl_lactate")>0,"ester formation missing");check(Conservation.compare(before,SolutionSpecies.analyticalSnapshot(s)).conserved(),"ester mass loss");
  var t=vessel();add(t,"water",100);add(t,"ethyl_lactate",1);add(t,"sodium_hydroxide_solution",1);TemperatureSystem.setTemp(t,60);before=SolutionSpecies.analyticalSnapshot(t);FutureChemistry.tick(t);
  check(BatchChemistry.mass(t,"liquid","sodium_lactate_solution")>0,"ester hydrolysis missing");check(Conservation.compare(before,SolutionSpecies.analyticalSnapshot(t)).conserved(),"hydrolysis mass loss");h.succeed();
 });}
 private FutureGameTests(){}
}
