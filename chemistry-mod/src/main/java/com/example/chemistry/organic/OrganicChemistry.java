package com.example.chemistry.organic;
import com.example.chemistry.*;
import com.example.chemistry.garden.ChemicalGarden;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.solution.*;
import net.minecraft.world.item.ItemStack;
/** Finite reversible esterification, plus alkaline hydrolysis; rates/K are calibrated game parameters. */
public final class OrganicChemistry {
    private static double n(ItemStack s,String id){return ChemicalGarden.mass(s,"liquid",id)/ChemicalGarden.mm("liquid",id);}
    private static void move(ItemStack s,String id,double moles){if(moles>0)LabVesselItem.addMass(s,"liquid",id,moles*ChemicalGarden.mm("liquid",id));else if(moles<0)LabVesselItem.consumeMass(s,"liquid",id,-moles*ChemicalGarden.mm("liquid",id));}
    public static boolean tick(ItemStack s){
        double ester=n(s,"ethyl_acetate");
        var trial=s.copy();boolean changed=false;
        for(String metal:java.util.List.of("sodium","potassium")){
            String base=metal+"_hydroxide_solution";double extent=Math.min(n(trial,"ethyl_acetate"),n(trial,base));
            extent=Math.min(extent,.00001*Math.clamp((TemperatureSystem.getTemp(s)+20)/45,.1,4));
            if(extent>1e-12){move(trial,"ethyl_acetate",-extent);move(trial,base,-extent);move(trial,metal+"_acetate_solution",extent);move(trial,"ethanol",extent);changed=true;}
        }
        boolean catalyst=ChemicalGarden.mass(trial,"liquid","sulfuric_acid_concentrated")>0||ChemicalGarden.mass(trial,"liquid","sulfuric_acid_dilute")>0||ChemicalGarden.mass(trial,"liquid","hydrochloric_acid")>0;
        if(!changed&&catalyst&&TemperatureSystem.getTemp(s)>=45){
            double acid=n(s,"acetic_acid"),alcohol=n(s,"ethanol"),water=n(s,"water");
            double lo=-Math.min(ester,water),hi=Math.min(acid,alcohol);
            for(int i=0;i<70;i++){double x=(lo+hi)/2;double q=(ester+x)*(water+x)-4*(acid-x)*(alcohol-x);if(q>0)hi=x;else lo=x;}
            double extent=(lo+hi)/2;double rate=.00001*Math.clamp((TemperatureSystem.getTemp(s)-25)/35,.1,3);extent=Math.clamp(extent,-rate,rate);
            if(Math.abs(extent)>1e-12){move(trial,"acetic_acid",-extent);move(trial,"ethanol",-extent);move(trial,"ethyl_acetate",extent);move(trial,"water",extent);changed=true;}
        }
        if(!changed)return false;
        if(!Conservation.compare(SolutionSpecies.analyticalSnapshot(s),SolutionSpecies.analyticalSnapshot(trial)).conserved())throw new IllegalStateException("Organic transaction unbalanced");
        com.example.chemistry.filtration.Filtration.commit(s,trial);return true;
    }
    private OrganicChemistry(){}
}
