package com.example.chemistry.data;
import java.util.Set;
/** Qualitative gameplay labels; unknown information remains explicitly pending. */
public final class HazardProfiles {
 public static ChemicalInfoProvider.ChemicalInfo enrich(String key,ChemicalInfoProvider.ChemicalInfo i){
  String toxicity=i.toxicity(),corr=i.corrosiveness(),expl=i.explosiveness();
  if(key.contains("phosgene")||key.contains("polonium")||key.contains("plutonium"))toxicity="extreme";
  else if(RadioChemicals.ROOTS.keySet().stream().anyMatch(id->key.equals("element_"+id)||key.endsWith("_"+id)||key.endsWith("_"+id+"_solution"))||key.contains("radon"))toxicity="high";
  if(key.contains("hydrogen_fluoride")||key.contains("boron_trifluoride")){toxicity="high";corr="strong";expl="none";}
  if(key.contains("hydrogen_bromide")||key.contains("hydrogen_iodide")){toxicity="high";corr="strong";expl="none";}
  if(key.contains("ozone")){toxicity="high";corr="strong";expl="conditional";}
  if(key.contains("dinitrogen_tetroxide")){toxicity="high";corr="strong";expl="conditional";}
  if(key.contains("silane")){toxicity="moderate";corr="none";expl="high";}
  if(unknown(toxicity)&&Set.of("glucose","fructose","sucrose","maltose","starch","cellulose","glycine","alanine").stream().anyMatch(key::contains))toxicity="low";
  if(unknown(toxicity)&&(key.contains("n_hexane")||key.contains("acetonitrile")||key.contains("aniline")||key.contains("phenol")))toxicity="high";
  if(unknown(toxicity)&&(key.contains("nickel")||key.contains("cobalt")||key.contains("lead_")||key.contains("chromate")))toxicity="high";
  if(key.contains("propionic_acid")||key.contains("butyric_acid")||key.contains("lactic_acid")||key.contains("gluconic_acid")){if(unknown(corr))corr="weak";}
  if(Set.of("isopropanol","n_propanol","n_butanol","n_hexane","cyclohexane","acetonitrile","styrene","ethyl_lactate","isoamyl_acetate","diethyl_oxalate").stream().anyMatch(key::contains)){if(unknown(expl))expl="flammable_vapor";if(unknown(corr))corr="none";}
  if(key.equals("element_yttrium")){toxicity="harmful_dust";corr="none";expl="dust";}
  if(java.util.Set.of("element_rubidium","element_caesium","solid_rubidium","solid_caesium","loose_rubidium","loose_caesium").contains(key)){toxicity="moderate";corr="strong";expl="water_reactive";}
  return new ChemicalInfoProvider.ChemicalInfo(i.formula(),i.molarMass(),toxicity,corr,expl,i.ph(),i.density(),i.appearance(),i.odour(),i.melting(),i.boiling());
 }
 public static boolean unknown(String s){return s.equals("未标定")||s.equals("unknown")||s.isBlank();}
 private HazardProfiles(){}
}
