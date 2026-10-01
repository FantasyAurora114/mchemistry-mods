package com.example.chemistry.solution;

import java.util.Map;
import com.example.chemistry.data.BatchChemicals;
import com.example.chemistry.AqueousSolubility;
import com.example.chemistry.TemperatureSystem;
import com.example.chemistry.item.LabVesselItem;
import net.minecraft.world.item.ItemStack;

/** Hydration tracks actual crystal water. No duplicate hydrate solute or massless dehydration. */
public final class HydrateChemistry {
    public static double transition(String id){var r=BatchChemicals.find(id);return r==null||r.waters()==0?-1: switch(id){case "sodium_acetate_trihydrate"->58;case "sodium_sulfate_decahydrate"->32.4;case "sodium_carbonate_decahydrate"->32;default->100;};}
    public static boolean hydrate(String id){return transition(id)>0;}
    public static void convert(ItemStack stack,String id,double grams){
        var r=BatchChemicals.find(id);if(r==null||r.waters()==0)return;
        BatchChemistry.transact(stack,Map.of("solid:"+id,1),Map.of("solid:"+r.anhydrous(),1,"liquid:water",r.waters()),grams/r.mass());
    }
    public static void dissolve(ItemStack stack){
        double water=BatchChemistry.mass(stack,"liquid","water");if(water<=0)return;
        double temperature=TemperatureSystem.getTemp(stack);
        for(var r:BatchChemicals.ALL){
            if(r.waters()==0)continue;
            double base=BatchChemistry.molar("solid",r.anhydrous()),wm=SpeciesCatalog.get("water").molarMass();
            double cap=AqueousSolubility.gramsPer100gWater(r.anhydrous(),temperature)/100;
            double dissolved=BatchChemistry.mass(stack,"liquid",AqueousSolubility.solutionId(r.anhydrous()));
            double available=BatchChemistry.mass(stack,"solid",r.id());
            if(available>0 && dissolved<cap*water){
                double n=Math.min(.5/r.mass(),Math.min(available/r.mass(),(cap*water-dissolved)/Math.max(1e-9,base-cap*r.waters()*wm)));
                BatchChemistry.transact(stack,Map.of("solid:"+r.id(),1),Map.of("liquid:"+AqueousSolubility.solutionId(r.anhydrous()),1,"liquid:water",r.waters()),n);
            }else if(dissolved>cap*water+1e-5 && temperature<Math.min(30,transition(r.id()))){
                double n=Math.min(.1/r.mass(),(dissolved-cap*water)/Math.max(1e-9,base-cap*r.waters()*wm));
                BatchChemistry.transact(stack,Map.of("liquid:"+AqueousSolubility.solutionId(r.anhydrous()),1,"liquid:water",r.waters()),Map.of("solid:"+r.id(),1),n);
            }
            water=BatchChemistry.mass(stack,"liquid","water");
        }
    }
    /** Keep cooling crystallisation in this adapter so crystal water is deducted before generic dry salt forms. */
    public static boolean ownsCrystallization(String base,double temperature){return BatchChemicals.ALL.stream().anyMatch(r->r.waters()>0&&r.anhydrous().equals(base)&&temperature<Math.min(30,transition(r.id())));}
    private HydrateChemistry(){}
}
