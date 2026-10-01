package com.example.chemistry.solution;
import java.util.List;
import java.util.Map;
import com.example.chemistry.data.FutureReactions;
import com.example.chemistry.data.FutureChemicals;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.TemperatureSystem;
import net.minecraft.world.item.ItemStack;
/** Finite third-batch transformations; formula-complete materials only. */
public final class FutureChemistry {
    private record Rule(Map<String,Integer> lhs,Map<String,Integer> rhs,String display,int temperature,String catalyst){}
    private static final List<Rule> RULES=FutureReactions.ALL.stream().map(r->{var lhs=new java.util.LinkedHashMap<String,Integer>();var rhs=new java.util.LinkedHashMap<String,Integer>();r.reactants().forEach(e->lhs.put(e.type()+":"+e.id(),e.coefficient()));r.products().forEach(e->rhs.put(e.type()+":"+e.id(),e.coefficient()));return new Rule(Map.copyOf(lhs),Map.copyOf(rhs),r.display(),r.requiredTemp(),r.catalyst());}).toList();
    private static boolean acid(ItemStack s){return LabVesselItem.getContents(s).stream().anyMatch(e->e.type().equals("liquid")&&e.amount()>0&&(e.id().startsWith("hydrochloric_acid")||e.id().startsWith("sulfuric_acid")));}
    public static void tick(ItemStack stack){
        if(BatchChemistry.mass(stack,"liquid","water")<=0)return;
        var present=new java.util.HashSet<String>();LabVesselItem.getContents(stack).forEach(e->{if(e.amount()>0)present.add(e.type()+":"+e.id());});
        for(var rule:RULES){
            if(!present.containsAll(rule.lhs().keySet())||TemperatureSystem.getTemp(stack)<rule.temperature()||rule.catalyst().equals("acid")&&!acid(stack)||rule.catalyst().equals("alkaline")&&BatchChemistry.approximatePh(stack)<=7)continue;
            if(BatchChemistry.transact(stack,rule.lhs(),rule.rhs(),.00002)){
                BatchChemistry.recordCompleted(stack,rule.display());LabVesselItem.getContents(stack).forEach(e->{if(e.amount()>0)present.add(e.type()+":"+e.id());});
            }
        }
        if(acid(stack)&&TemperatureSystem.getTemp(stack)>=45){
            ester(stack,"liquid","lactic_acid","ethanol","ethyl_lactate");
            ester(stack,"liquid","acetic_acid","isoamyl_alcohol","isoamyl_acetate");
            ester(stack,"solid","salicylic_acid","methanol","methyl_salicylate");
        }
    }
    private static double n(ItemStack s,String type,String id){return BatchChemistry.mass(s,type,id)/BatchChemistry.molar(type,id);}
    private static void ester(ItemStack s,String acidType,String acid,String alcohol,String ester){
        double a=n(s,acidType,acid),b=n(s,"liquid",alcohol),e=n(s,"liquid",ester),w=n(s,"liquid","water");
        double lo=-Math.min(e,w),hi=Math.min(a,b);
        if(lo==0&&hi==0)return;
        for(int i=0;i<80;i++){double x=(lo+hi)/2;if((e+x)*(w+x)>4*(a-x)*(b-x))hi=x;else lo=x;}
        double x=Math.clamp((lo+hi)/2,-.00001,.00001);
        var left=Map.of(acidType+":"+acid,1,"liquid:"+alcohol,1);var right=Map.of("liquid:"+ester,1,"liquid:water",1);
        if(x>0)BatchChemistry.transact(s,left,right,x);else if(x<0)BatchChemistry.transact(s,right,left,-x);
    }
    public static int color(ItemStack stack,int fallback){
        var entries=LabVesselItem.getContents(stack);
        if(BatchChemistry.mass(stack,"solid","starch")>0&&(BatchChemistry.mass(stack,"liquid","iodine_water")+BatchChemistry.mass(stack,"solid","iodine"))>0)return BatchChemistry.mix(fallback,0x25307A,.9);
        double total=0,r=0,g=0,b=0;
        for(var entry:entries){if(!entry.type().equals("liquid"))continue;String id=entry.id().replace("_solution","");
            if(!id.equals("ferroin_chloride")&&!id.equals("nickel_phenanthroline_chloride"))continue;
            var reagent=FutureChemicals.find(id);double w=entry.amount();total+=w;r+=((reagent.color()>>16)&255)*w;g+=((reagent.color()>>8)&255)*w;b+=(reagent.color()&255)*w;
        }
        if(total>0){int rgb=((int)(r/total)<<16)|((int)(g/total)<<8)|(int)(b/total);return BatchChemistry.mix(fallback,rgb,1-Math.exp(-total/Math.max(.001,SolutionSpecies.solutionLitres(stack))*20));}
        return fallback;
    }
    private FutureChemistry(){}
}
