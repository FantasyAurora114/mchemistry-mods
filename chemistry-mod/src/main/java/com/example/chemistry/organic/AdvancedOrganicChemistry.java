package com.example.chemistry.organic;
import java.util.*;
import com.example.chemistry.*;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.solution.BatchChemistry;
import net.minecraft.world.item.ItemStack;
/** Stoichiometry is chemical; tick speeds/temperature gates are game pacing, not laboratory kinetics. */
public final class AdvancedOrganicChemistry {
 public record Rule(Map<String,Integer> lhs,Map<String,Integer> rhs,String equation,boolean acid,boolean warm){}
 public static final List<Rule> RULES=List.of(
  new Rule(Map.of("solid:salicylic_acid",1,"liquid:acetic_anhydride",1),Map.of("solid:acetylsalicylic_acid",1,"liquid:acetic_acid",1),"C₇H₆O₃ + (CH₃CO)₂O → C₉H₈O₄ + CH₃COOH",true,true),
  new Rule(Map.of("liquid:aniline",1,"liquid:acetic_anhydride",1),Map.of("solid:acetanilide",1,"liquid:acetic_acid",1),"C₆H₅NH₂ + (CH₃CO)₂O → C₆H₅NHCOCH₃ + CH₃COOH",false,false),
  new Rule(Map.of("liquid:acetic_anhydride",1,"liquid:water",1),Map.of("liquid:acetic_acid",2),"(CH₃CO)₂O + H₂O → 2 CH₃COOH",false,false),
  new Rule(Map.of("solid:acetylsalicylic_acid",1,"liquid:water",1),Map.of("solid:salicylic_acid",1,"liquid:acetic_acid",1),"C₉H₈O₄ + H₂O → C₇H₆O₃ + CH₃COOH",true,true),
  new Rule(Map.of("solid:acetanilide",1,"liquid:water",1),Map.of("liquid:aniline",1,"liquid:acetic_acid",1),"C₆H₅NHCOCH₃ + H₂O → C₆H₅NH₂ + CH₃COOH",true,true),
  new Rule(Map.of("liquid:ethyl_benzoate",1,"liquid:sodium_hydroxide_solution",1),Map.of("solid:sodium_benzoate",1,"liquid:ethanol",1),"C₆H₅COOC₂H₅ + NaOH → C₆H₅COONa + C₂H₅OH",false,true));
 public static void tick(ItemStack s){
  boolean acid=LabVesselItem.getContents(s).stream().anyMatch(e->e.type().equals("liquid")&&e.amount()>0&&(e.id().startsWith("hydrochloric_acid")||e.id().startsWith("sulfuric_acid")));
  var present=new java.util.HashSet<String>();LabVesselItem.getContents(s).forEach(e->{if(e.amount()>0)present.add(e.type()+":"+e.id());});
  for(var r:RULES)if(present.containsAll(r.lhs().keySet())&&(!r.acid()||acid)&&(!r.warm()||TemperatureSystem.getTemp(s)>=45)&&BatchChemistry.transact(s,r.lhs(),r.rhs(),.00001))BatchChemistry.recordCompleted(s,r.equation());
  hydrolyzeWF6(s);
 }
 public static boolean hydrolyzeWF6(ItemStack s){
  double ml=VesselGasPhase.read(s).stream().filter(p->p.id().equals("tungsten_hexafluoride")).mapToDouble(VesselGasPhase.Part::ml).sum();
  double water=BatchChemistry.mass(s,"liquid","water");double extent=Math.min(.00001,Math.min(ml/24465,water/(3*18.015)));
  if(extent<=1e-12)return false;
  VesselGasPhase.remove(s,"tungsten_hexafluoride",extent*24465);
  LabVesselItem.consumeMass(s,"liquid","water",extent*3*18.015);
  LabVesselItem.addMass(s,"solid","tungsten_trioxide",extent*231.837);
  // Preserve product mass in the reaction ledger before the existing gas-phase step transfers/vents it.
  VesselGasPhase.pour(s,"hydrogen_fluoride",extent*6*24465);
  BatchChemistry.recordCompleted(s,"WF₆ + 3 H₂O → WO₃ + 6 HF");return true;
 }
 private AdvancedOrganicChemistry(){}
}
