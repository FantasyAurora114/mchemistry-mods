package com.example.chemistry.solution;

import java.util.Map;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.TemperatureSystem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;

/** Derived cobalt and nickel partitions; equilibrium constants are explicit gameplay approximations. */
public final class BatchCoordination {
    private static final String STATE="eq_cobalt_chloride";
    public static double cobaltTarget(SpeciesInventory totals,double litres,double celsius){
        double metal=totals.amount("cobalt_ii"),chloride=totals.amount("chloride")+totals.amount("aqueous_hcl");
        if(totals.amount("water")<=0||litres<=0||metal<=0||chloride<=0)return 0;
        // Endothermic formation: chloride concentration and temperature favour blue complex.
        double k=.02*Math.exp(Math.clamp((celsius-20)/18,-8,8));
        double lo=0,hi=Math.min(metal,chloride/4);
        for(int i=0;i<80;i++){double x=(lo+hi)/2;double free=(chloride-4*x)/litres;double target=k*(metal-x)*Math.pow(free,4);if(target>x)lo=x;else hi=x;}
        return (lo+hi)/2;
    }
    public static SpeciesInventory current(ItemStack stack,SpeciesInventory totals,double litres){
        if(totals.amount("cobalt_ii")<=0)return nickel(totals,litres);
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        double hcl=totals.amount("aqueous_hcl");
        if(hcl>0)totals=totals.transform(Map.of("aqueous_hcl",1),Map.of("hydrogen",1,"chloride",1),hcl);
        double maximum=Math.min(totals.amount("cobalt_ii"),totals.amount("chloride")/4);
        double bound=Math.clamp(tag.getDoubleOr(STATE,cobaltTarget(totals,litres,TemperatureSystem.getTemp(stack))),0,maximum);
        if(totals.amount("water")<=0)bound=0;
        if(bound>0)totals=totals.transform(Map.of("cobalt_ii",1,"chloride",4),Map.of("cobalt_tetrachloride",1),bound);
        return nickel(totals,litres);
    }
    private static SpeciesInventory nickel(SpeciesInventory totals,double litres){
        if(litres<=0||totals.amount("water")<=0||totals.amount("nickel_ii")<=0)return totals;
        double hydrated=totals.amount("batch:ammonia_water");
        if(hydrated>0)totals=totals.transform(Map.of("batch:ammonia_water",1),Map.of("ammonia",1,"water",1),hydrated);
        // Coupled ligand competition through one shared free-metal concentration.
        double metal=totals.amount("nickel_ii"),en=totals.amount("batch:ethylenediamine"),ammonia=totals.amount("ammonia");
        double lo=0,hi=metal,x=0,y=0;
        for(int i=0;i<100;i++){
            double free=(lo+hi)/2;
            x=ligandExtent(free,en,3,1e8,litres);y=ligandExtent(free,ammonia,6,1e7,litres);
            if(free+x+y>metal)hi=free;else lo=free;
        }
        double free=(lo+hi)/2;x=ligandExtent(free,en,3,1e8,litres);y=ligandExtent(free,ammonia,6,1e7,litres);
        double factor=Math.min(1,metal/Math.max(metal,x+y));x*=factor;y*=factor;
        if(x>1e-15)totals=totals.transform(Map.of("nickel_ii",1,"batch:ethylenediamine",3),Map.of("nickel_en",1),Math.min(x,totals.amount("batch:ethylenediamine")/3));
        if(y>1e-15)totals=totals.transform(Map.of("nickel_ii",1,"ammonia",6),Map.of("nickel_ammine",1),Math.min(y,Math.min(totals.amount("nickel_ii"),totals.amount("ammonia")/6)));
        return totals;
    }
    private static double ligandExtent(double free,double ligand,int n,double k,double litres){
        double lo=0,hi=ligand/n;
        for(int i=0;i<80;i++){double bound=(lo+hi)/2;double result=k*free*Math.pow(Math.max(0,(ligand-n*bound)/litres),n);if(result>bound)lo=bound;else hi=bound;}
        return (lo+hi)/2;
    }
    public static void tick(ItemStack stack){
        var totals=SolutionSpecies.analyticalSnapshot(stack);var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(totals.amount("cobalt_ii")<=0){if(tag.contains(STATE)){tag.remove(STATE);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}return;}
        double target=cobaltTarget(totals,SolutionSpecies.solutionLitres(stack),TemperatureSystem.getTemp(stack));
        double old=Math.clamp(tag.getDoubleOr(STATE,0),0,Math.min(totals.amount("cobalt_ii"),(totals.amount("chloride")+totals.amount("aqueous_hcl"))/4));
        double next=old+(target-old)*.08;tag.putDouble(STATE,next);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        if(Math.abs(next-old)>1e-12)LabVesselItem.updateTint(stack);
    }
    public static int color(ItemStack stack,int fallback){
        if(LabVesselItem.getContents(stack).stream().noneMatch(e->e.type().equals("liquid")&&(e.id().startsWith("cobalt_")||e.id().startsWith("nickel_"))))return fallback;
        var state=SolutionSpecies.snapshot(stack);double litres=SolutionSpecies.solutionLitres(stack);if(litres<=0)return fallback;
        double cobalt=state.amount("cobalt_ii")+state.amount("cobalt_tetrachloride");
        if(cobalt>0){int dye=BatchChemistry.mix(0xD976A7,0x2755B5,state.amount("cobalt_tetrachloride")/cobalt);fallback=BatchChemistry.mix(0xC4DCE2,dye,1-Math.exp(-cobalt/litres*80));}
        double nickel=state.amount("nickel_ii")+state.amount("nickel_en")+state.amount("nickel_ammine");
        if(nickel>0){int dye=BatchChemistry.mix(0x65AB72,0x765AC2,(state.amount("nickel_en")+state.amount("nickel_ammine"))/nickel);fallback=BatchChemistry.mix(fallback,dye,1-Math.exp(-nickel/litres*80));}
        return fallback;
    }
    public static void appendInfo(java.util.List<net.minecraft.network.chat.Component> lines,ItemStack stack){
        if(LabVesselItem.getContents(stack).stream().noneMatch(e->e.id().startsWith("cobalt_")||e.id().startsWith("nickel_")))return;
        var state=SolutionSpecies.snapshot(stack);
        for(String id:java.util.List.of("cobalt_tetrachloride","nickel_en","nickel_ammine"))if(state.amount(id)>1e-12)lines.add(net.minecraft.network.chat.Component.literal(String.format(java.util.Locale.ROOT,"%s %.4g mmol（配合平衡近似）",SpeciesCatalog.get(id).label(),state.amount(id)*1000)));
    }
    private BatchCoordination(){}
}
