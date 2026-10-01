package com.example.chemistry.data;
import java.util.*;
import com.example.chemistry.data.FutureChemicals.Reagent;
public final class RadioChemicals {
 public static final List<Reagent> ALL=List.of(
  new Reagent("thorium","Th","钍","Thorium","SOLID",10201764,232.0377,0,1,9999,-9999,Map.of("Th",1)),
  new Reagent("radium","Ra","镭","Radium","SOLID",14147791,226,0,1,9999,-9999,Map.of("Ra",1)),
  new Reagent("plutonium","Pu","钚","Plutonium","SOLID",8292497,244,0,1,9999,-9999,Map.of("Pu",1)),
  new Reagent("polonium","Po","钋","Polonium","SOLID",11385278,209,0,1,9999,-9999,Map.of("Po",1)),
  new Reagent("actinium","Ac","锕","Actinium","SOLID",11976643,227,0,1,9999,-9999,Map.of("Ac",1)),
  new Reagent("americium","Am","镅","Americium","SOLID",10857392,243,0,1,9999,-9999,Map.of("Am",1)),
  new Reagent("technetium","Tc","锝","Technetium","SOLID",11383477,98,0,1,9999,-9999,Map.of("Tc",1)),
  new Reagent("uranium","U","铀","Uranium","SOLID",11057312,238.02891,0,1,9999,-9999,Map.of("U",1)),
  new Reagent("uranium_dioxide","UO2","二氧化铀","Uranium Dioxide","SOLID",13422009,270.02691,0,1,9999,-9999,Map.of("U",1,"O",2)),
  new Reagent("uranium_trioxide","UO3","三氧化铀","Uranium Trioxide","SOLID",13422009,286.02591,0,1,9999,-9999,Map.of("U",1,"O",3)),
  new Reagent("uranium_octoxide","U3O8","八氧化三铀","Uranium Octoxide","SOLID",13422009,842.07873,0,1,9999,-9999,Map.of("U",3,"O",8)),
  new Reagent("uranyl_nitrate","UO2N2O6","硝酸铀酰","Uranyl Nitrate","SOLID",13422009,394.03491,20,1,9999,-9999,Map.of("U",1,"O",8,"N",2)),
  new Reagent("uranyl_sulfate","UO2SO4","硫酸铀酰","Uranyl Sulfate","SOLID",13422009,366.08290999999997,10,1,9999,-9999,Map.of("U",1,"O",6,"S",1)),
  new Reagent("uranyl_chloride","UO2Cl2","氯化铀酰","Uranyl Chloride","SOLID",13422009,340.92691,10,1,9999,-9999,Map.of("U",1,"O",2,"Cl",2)),
  new Reagent("thorium_dioxide","ThO2","二氧化钍","Thorium Dioxide","SOLID",13422009,264.0357,0,1,9999,-9999,Map.of("Th",1,"O",2)),
  new Reagent("thorium_nitrate","ThN4O12","硝酸钍","Thorium Nitrate","SOLID",13422009,480.0537,10,1,9999,-9999,Map.of("Th",1,"N",4,"O",12)),
  new Reagent("thorium_chloride","ThCl4","氯化钍","Thorium Chloride","SOLID",13422009,373.83770000000004,10,1,9999,-9999,Map.of("Th",1,"Cl",4)),
  new Reagent("radium_chloride","RaCl2","氯化镭","Radium Chloride","SOLID",13422009,296.9,5,1,9999,-9999,Map.of("Ra",1,"Cl",2)),
  new Reagent("radium_sulfate","RaSO4","硫酸镭","Radium Sulfate","SOLID",13422009,322.056,0,1,9999,-9999,Map.of("Ra",1,"S",1,"O",4)),
  new Reagent("radium_carbonate","RaCO3","碳酸镭","Radium Carbonate","SOLID",13422009,286.008,0,1,9999,-9999,Map.of("Ra",1,"C",1,"O",3)),
  new Reagent("plutonium_dioxide","PuO2","二氧化钚","Plutonium Dioxide","SOLID",13422009,275.998,0,1,9999,-9999,Map.of("Pu",1,"O",2)),
  new Reagent("plutonium_trichloride","PuCl3","三氯化钚","Plutonium Trichloride","SOLID",13422009,350.35,5,1,9999,-9999,Map.of("Pu",1,"Cl",3)),
  new Reagent("plutonium_nitrate","PuN4O12","硝酸钚(IV)","Plutonium Nitrate","SOLID",13422009,492.016,5,1,9999,-9999,Map.of("Pu",1,"N",4,"O",12)),
  new Reagent("americium_dioxide","AmO2","二氧化镅","Americium Dioxide","SOLID",13422009,274.998,0,1,9999,-9999,Map.of("Am",1,"O",2)),
  new Reagent("americium_chloride","AmCl3","氯化镅","Americium Chloride","SOLID",13422009,349.35,5,1,9999,-9999,Map.of("Am",1,"Cl",3)),
  new Reagent("actinium_chloride","AcCl3","氯化锕","Actinium Chloride","SOLID",13422009,333.35,5,1,9999,-9999,Map.of("Ac",1,"Cl",3)),
  new Reagent("technetium_dioxide","TcO2","二氧化锝","Technetium Dioxide","SOLID",13422009,129.998,0,1,9999,-9999,Map.of("Tc",1,"O",2)),
  new Reagent("sodium_pertechnetate","NaTcO4","高锝酸钠","Sodium Pertechnetate","SOLID",13422009,184.986,5,1,9999,-9999,Map.of("Na",1,"Tc",1,"O",4)),
  new Reagent("polonium_dioxide","PoO2","二氧化钋","Polonium Dioxide","SOLID",13422009,240.998,0,1,9999,-9999,Map.of("Po",1,"O",2)),
  new Reagent("sodium_iodide_131","NaI","碘-131示踪碘化钠","Sodium Iodide 131","SOLID",13422009,149.89447,5,1,9999,-9999,Map.of("Na",1,"I",1)),
  new Reagent("cesium_chloride_137","CsCl","铯-137示踪氯化铯","Cesium Chloride 137","SOLID",13422009,168.355452,5,1,9999,-9999,Map.of("Cs",1,"Cl",1)),
  new Reagent("cobalt_chloride_60","CoCl2","钴-60示踪氯化钴","Cobalt Chloride 60","SOLID",13422009,129.833194,5,1,9999,-9999,Map.of("Co",1,"Cl",2))
 );
 public static final Map<String,String> ROOTS=Map.ofEntries(Map.entry("thorium","Th232"),Map.entry("radium","Ra226"),Map.entry("plutonium","Pu239"),Map.entry("polonium","Po210"),Map.entry("actinium","Ac227"),Map.entry("americium","Am241"),Map.entry("technetium","Tc99"),Map.entry("uranium","U238"),Map.entry("uranium_dioxide","U238"),Map.entry("uranium_trioxide","U238"),Map.entry("uranium_octoxide","U238"),Map.entry("uranyl_nitrate","U238"),Map.entry("uranyl_sulfate","U238"),Map.entry("uranyl_chloride","U238"),Map.entry("thorium_dioxide","Th232"),Map.entry("thorium_nitrate","Th232"),Map.entry("thorium_chloride","Th232"),Map.entry("radium_chloride","Ra226"),Map.entry("radium_sulfate","Ra226"),Map.entry("radium_carbonate","Ra226"),Map.entry("plutonium_dioxide","Pu239"),Map.entry("plutonium_trichloride","Pu239"),Map.entry("plutonium_nitrate","Pu239"),Map.entry("americium_dioxide","Am241"),Map.entry("americium_chloride","Am241"),Map.entry("actinium_chloride","Ac227"),Map.entry("technetium_dioxide","Tc99"),Map.entry("sodium_pertechnetate","Tc99"),Map.entry("polonium_dioxide","Po210"),Map.entry("sodium_iodide_131","I131"),Map.entry("cesium_chloride_137","Cs137"),Map.entry("cobalt_chloride_60","Co60"));
 private RadioChemicals(){}
}
