package com.example.chemistry.organic;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.garden.ChemicalGarden;
import com.example.chemistry.solution.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;
/** Finite molecular iodine distribution in selected binary solvents. K values are gameplay calibration. */
public final class Extraction {
    public static final Map<String,Double> K=Map.of("toluene",80.0,"carbon_tetrachloride",85.0,"ethyl_acetate",20.0,"n_hexane",70.0,"cyclohexane",70.0);
    public static String organicId(String solvent){return "iodine_"+solvent;}
    public static boolean iodine(String id){return id.equals("iodine_water")||K.keySet().stream().anyMatch(s->organicId(s).equals(id));}
    public static void tick(ItemStack s){
        var phases=LiquidPhases.read(s);if(!phases.modelled())return;
        if(phases.layers().size()!=2){
            if(ChemicalGarden.mass(s,"liquid","water")>0){double moved=0;for(String solvent:K.keySet()){String id=organicId(solvent);double m=ChemicalGarden.mass(s,"liquid",id);LabVesselItem.consumeMass(s,"liquid",id,m);moved+=m;}if(moved>0)LabVesselItem.addMass(s,"liquid","iodine_water",moved);}
            return;
        }
        var org=phases.layers().stream().filter(l->K.containsKey(l.name())).findFirst().orElse(null);
        if(org==null)return;var aq=phases.layers().stream().filter(l->l.name().equals("aqueous")).findFirst().orElseThrow();
        double amount=LabVesselItem.getContents(s).stream().filter(e->e.type().equals("liquid")&&iodine(e.id())).mapToDouble(LabVesselItem.Entry::amount).sum();
        double solid=ChemicalGarden.mass(s,"solid","iodine"),water=ChemicalGarden.mass(s,"liquid","water");
        // Dilute solubility ceiling; prevent unlimited dissolution of an arbitrarily large iodine crystal.
        double dissolved=Math.min(solid,Math.max(0,.0003*water*(1+K.get(org.name())*org.ml()/Math.max(1e-9,aq.ml()))-amount));
        if(amount+dissolved<=1e-12)return;
        double desired=(amount+dissolved)*K.get(org.name())*org.ml()/(aq.ml()+K.get(org.name())*org.ml());
        String target=organicId(org.name());double present=ChemicalGarden.mass(s,"liquid",target);
        if(dissolved<=1e-10&&Math.abs(desired-present)<=1e-8)return;
        var before=SolutionSpecies.analyticalSnapshot(s);var trial=s.copy();
        for(var entry:LabVesselItem.getContents(trial)){if(entry.type().equals("liquid")&&iodine(entry.id()))LabVesselItem.consumeMass(trial,"liquid",entry.id(),entry.amount());}
        LabVesselItem.consumeMass(trial,"solid","iodine",dissolved);
        LabVesselItem.addMass(trial,"liquid",target,desired);LabVesselItem.addMass(trial,"liquid","iodine_water",amount+dissolved-desired);
        if(!Conservation.compare(before,SolutionSpecies.analyticalSnapshot(trial)).conserved())throw new IllegalStateException("Extraction changed iodine inventory");
        com.example.chemistry.filtration.Filtration.commit(s,trial);
    }
    public static int tint(List<LabVesselItem.Entry> entries,double ml,int fallback,boolean aqueous){
        double grams=entries.stream().filter(e->iodine(e.id())).mapToDouble(LabVesselItem.Entry::amount).sum();
        double strength=1-Math.exp(-grams/Math.max(1e-9,ml)*(aqueous?10000:1800));int color=aqueous||entries.stream().anyMatch(e->e.id().equals("iodine_ethyl_acetate"))?0xA16628:0x7934A0,result=0;
        for(int shift:new int[]{0,8,16})result|=((int)Math.round(((fallback>>shift)&255)*(1-strength)+((color>>shift)&255)*strength))<<shift;
        return result;
    }
    private Extraction(){}
}
