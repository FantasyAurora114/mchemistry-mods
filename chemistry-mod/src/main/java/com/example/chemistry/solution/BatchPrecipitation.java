package com.example.chemistry.solution;
import java.util.Map;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.TemperatureSystem;
import net.minecraft.world.item.ItemStack;
/** Ideal concentration Ksp extension. Constants at 25 C from the UMass chemistry table. */
public final class BatchPrecipitation {
    private record Salt(String id,String metal,String anion,int metalCount,int anionCount,double ksp){}
    private static final java.util.List<Salt> SALTS=java.util.List.of(
        new Salt("nickel_hydroxide","nickel_ii","hydroxide",1,2,2.8e-16),
        new Salt("zinc_hydroxide","zinc","hydroxide",1,2,4.5e-17),
        new Salt("lead_iodide","lead_ii","iodide",1,2,8.7e-9));
    private static Salt salt(String id){return SALTS.stream().filter(s->s.id().equals(id)).findFirst().orElse(null);}
    public static boolean manages(String id){return salt(id)!=null;}
    private static double k(Salt salt,ItemStack stack){
        // PbI2 has a positive dissolution enthalpy. The thermal factor is a gameplay approximation.
        return salt.ksp()*(salt.id().equals("lead_iodide")?Math.exp(Math.clamp((TemperatureSystem.getTemp(stack)-25)/20,-2,4)):1);
    }
    private static double q(Salt s,double metal,double anion,double litres){return Math.pow(Math.max(0,metal/litres),s.metalCount())*Math.pow(Math.max(0,anion/litres),s.anionCount());}
    public static double formationLimit(ItemStack stack,Map<String,Integer> lhs,Map<String,Integer> rhs,double maximum){
        if(lhs.keySet().stream().anyMatch(key->key.startsWith("solid:")||key.contains("acid")))return maximum;
        Salt salt=null;
        for(String key:rhs.keySet())if(key.startsWith("solid:")){salt=salt(key.substring(6));if(salt!=null)break;}
        if(salt==null)return maximum;
        double litres=SolutionSpecies.solutionLitres(stack);if(litres<=0)return 0;
        var view=SolutionSpecies.snapshot(stack);double metal=view.amount(salt.metal()),anion=view.amount(salt.anion());
        double lo=0,hi=Math.min(maximum,Math.min(metal/salt.metalCount(),anion/salt.anionCount()));
        double k=k(salt,stack);
        if(q(salt,metal,anion,litres)<=k)return 0;
        for(int i=0;i<80;i++){double x=(lo+hi)/2;if(q(salt,metal-x*salt.metalCount(),anion-x*salt.anionCount(),litres)>k)lo=x;else hi=x;}
        return (lo+hi)/2;
    }
    public static void tick(ItemStack stack){
        double litres=SolutionSpecies.solutionLitres(stack);if(litres<=0||BatchChemistry.mass(stack,"liquid","water")<=0)return;
        for(Salt salt:SALTS){
            double solid=BatchChemistry.mass(stack,"solid",salt.id()),solute=BatchChemistry.mass(stack,"liquid",salt.id()+"_solution");
            if(solid<=0&&solute<=0)continue;
            var state=SolutionSpecies.snapshot(stack);double metal=state.amount(salt.metal()),anion=state.amount(salt.anion()),k=k(salt,stack);
            double quotient=q(salt,metal,anion,litres),mm=BatchChemistry.molar("solid",salt.id());
            if(quotient<k&&solid>0){
                double lo=0,hi=Math.min(.01/mm,solid/mm);
                for(int i=0;i<80;i++){double x=(lo+hi)/2;if(q(salt,metal+x*salt.metalCount(),anion+x*salt.anionCount(),litres)<k)lo=x;else hi=x;}
                BatchChemistry.transact(stack,Map.of("solid:"+salt.id(),1),Map.of("liquid:"+salt.id()+"_solution",1),(lo+hi)/2);
            }else if(quotient>k&&solute>0){
                double lo=0,hi=Math.min(.01/mm,Math.min(solute/mm,Math.min(metal/salt.metalCount(),anion/salt.anionCount())));
                for(int i=0;i<80;i++){double x=(lo+hi)/2;if(q(salt,metal-x*salt.metalCount(),anion-x*salt.anionCount(),litres)>k)lo=x;else hi=x;}
                BatchChemistry.transact(stack,Map.of("liquid:"+salt.id()+"_solution",1),Map.of("solid:"+salt.id(),1),(lo+hi)/2);
            }
        }
    }
    private BatchPrecipitation(){}
}
