package com.example.chemistry.organic;
import com.example.chemistry.*;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.solution.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
public final class AdvancedGameTests {
 private static void check(GameTestHelper h,boolean ok,String msg){h.assertTrue(ok,Component.literal(msg));}
 public static void reactions(GameTestHelper h){h.runAtTickTime(1,()->{
  for(var rule:AdvancedOrganicChemistry.RULES){var s=new ItemStack(ModItems.ERLENMEYER_FLASK.get());
   for(var e:rule.lhs().entrySet()){var key=e.getKey().split(":",2);LabVesselItem.addMass(s,key[0],key[1],.001*e.getValue()*BatchChemistry.molar(key[0],key[1]));}
   var before=SolutionSpecies.analyticalSnapshot(s);check(h,BatchChemistry.transact(s,rule.lhs(),rule.rhs(),.0001),"new organic recipe did not transact "+rule.equation());
   check(h,Conservation.compare(before,SolutionSpecies.analyticalSnapshot(s)).conserved(),"new organic recipe lost atoms");
  }h.succeed();
 });}
 public static void wf6(GameTestHelper h){h.runAtTickTime(1,()->{
  var s=new ItemStack(ModItems.ERLENMEYER_FLASK.get());LabVesselItem.addMass(s,"liquid","water",50);VesselGasPhase.pour(s,"tungsten_hexafluoride",20);
  check(h,AdvancedOrganicChemistry.hydrolyzeWF6(s),"wet WF6 did not hydrolyze");
  double solid=BatchChemistry.mass(s,"solid","tungsten_trioxide");check(h,Math.abs(solid-.00001*231.837)<1e-8,"WF6 solid product extent");
  check(h,VesselGasPhase.read(s).stream().filter(p->p.id().equals("tungsten_hexafluoride")).mapToDouble(VesselGasPhase.Part::ml).sum()<20,"WF6 gas not debited");
  var dry=new ItemStack(ModItems.ERLENMEYER_FLASK.get());VesselGasPhase.pour(dry,"tungsten_hexafluoride",20);check(h,!AdvancedOrganicChemistry.hydrolyzeWF6(dry),"dry WF6 generated products");
  PhaseSystem.tick(dry,25);check(h,VesselGasPhase.read(dry).stream().anyMatch(p->p.id().equals("tungsten_hexafluoride")),"noncondensable WF6 vanished");h.succeed();
 });}
 public static void isotopes(GameTestHelper h){h.runAtTickTime(1,()->{
  var u=ModItems.ISOTOPES.stream().map(ref->new ItemStack(ref.get())).filter(s->((com.example.chemistry.radiation.IsotopeSampleItem)s.getItem()).nuclide().equals("U235")).findFirst().orElseThrow();
  check(h,com.example.chemistry.radiation.NuclearInstability.points(u)>0,"fissile sample missing game points");
  var stable=ModItems.ISOTOPES.stream().map(ref->new ItemStack(ref.get())).filter(s->((com.example.chemistry.radiation.IsotopeSampleItem)s.getItem()).nuclide().equals("C13")).findFirst().orElseThrow();
  check(h,com.example.chemistry.radiation.NuclearInstability.points(stable)==0,"stable sample caused nuclear event");
  check(h,com.example.chemistry.radiation.RadioLedger.carriers(stable).containsKey("C13"),"stable isotope identity lost");h.succeed();
 });}
 private AdvancedGameTests(){}
}
