package com.example.chemistry;

import com.example.chemistry.data.Solutions;
import com.example.chemistry.item.LabVesselItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import java.util.List;

/** Shared joule budget. Heat capacities are explicit gameplay approximations;
 * measured constants are only used for the named processes. */
public final class ThermalSystem {
    public static final double WATER_CP=4.18;
    public static final double WATER_VAPORIZATION=2256;
    public static final double STRONG_NEUTRALIZATION=57_900;
    public static final double ACETIC_NEUTRALIZATION=56_100;
    private static final String BOILING="chem_boiling_j";
    private static CompoundTag tag(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    private static void put(ItemStack s,CompoundTag t){s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));}
    public static double specificHeat(String type,String id){
        if(id.equals("water"))return WATER_CP;
        if(type.equals("solid")||id.startsWith("molten_"))return 1;
        return type.equals("liquid")?3:1;
    }
    public static double contentsCapacity(ItemStack s){
        return LabVesselItem.getContents(s).stream().mapToDouble(e->e.amount()*specificHeat(e.type(),e.id())).sum();
    }
    public static double capacity(ItemStack s){
        double vessel=s.getItem() instanceof LabVesselItem v ? Math.max(10,40*Math.pow(v.capacity()/250.,2./3.)):40;
        return vessel+contentsCapacity(s);
    }
    public static double accumulated(ItemStack s,String source){return tag(s).getCompoundOrEmpty("chem_heat_sources").getDoubleOr(source,0);}
    private static void record(ItemStack s,double joules,String source){
        var t=tag(s);var sources=t.getCompoundOrEmpty("chem_heat_sources").copy();
        sources.putDouble(source,sources.getDoubleOr(source,0)+joules);t.put("chem_heat_sources",sources);put(s,t);
    }
    public static double boilingEnergy(ItemStack s){return Math.max(0,tag(s).getDoubleOr(BOILING,0));}
    private static void boilingEnergy(ItemStack s,double value){var t=tag(s);if(value<=1e-9)t.remove(BOILING);else t.putDouble(BOILING,value);put(s,t);}
    private static boolean openWater(ItemStack s){return (!VesselHeating.isSealed(s)||com.example.chemistry.utility.VacuumState.enabled(s))&&LabVesselItem.getContents(s).stream()
            .anyMatch(e->e.type().equals("liquid")&&e.id().equals("water")&&e.amount()>0);}
    public static void addHeat(ItemStack s,double joules,String source){
        if(!(s.getItem() instanceof LabVesselItem)||!Double.isFinite(joules))throw new IllegalArgumentException("Invalid heat transaction");
        if(joules==0)return;
        record(s,joules,source);
        applyHeat(s,joules);
    }
    private static void applyHeat(ItemStack s,double joules){
        double remaining=joules;
        if(remaining<0){double consumed=Math.min(boilingEnergy(s),-remaining);boilingEnergy(s,boilingEnergy(s)-consumed);remaining+=consumed;}
        if(remaining==0)return;
        double current=TemperatureSystem.getTemp(s);
        double next=PhaseSystem.applyHeatJoules(s,current,remaining);
        double waterBoil=com.example.chemistry.utility.VacuumState.boilingPoint(s,"water");
        if(openWater(s)&&next>waterBoil){
            boilingEnergy(s,boilingEnergy(s)+(next-waterBoil)*capacity(s));next=waterBoil;
        }
        TemperatureSystem.setRawTemp(s,next);
    }
    /** Existing heat/cooling rates now enter the same energy budget. */
    public static void targetTemperature(ItemStack s,double target,String source){
        if(!Double.isFinite(target))throw new IllegalArgumentException("Invalid temperature");
        addHeat(s,(target-TemperatureSystem.getTemp(s))*capacity(s),source);
    }
    /** Latent values other than copper are provisional gameplay parameters. */
    public static double fusionJPerGram(String id){return id.equals("copper")?205:200;}
    public static double vaporizationJPerGram(String id){return id.equals("water")?WATER_VAPORIZATION:500;}
    public static double vaporizationLimit(ItemStack s,String id,double maxGrams,double boilingPoint){
        double sensible=Math.max(0,(TemperatureSystem.getTemp(s)-boilingPoint)*capacity(s));
        double stored=id.equals("water")?boilingEnergy(s):0;
        return Math.min(maxGrams,(sensible+stored)/vaporizationJPerGram(id));
    }
    /** Called only after an accepted mass leaves the liquid; full receivers cost no energy. */
    public static void vaporized(ItemStack s,String id,double grams,double boilingPoint){
        if(grams<=0)return;
        double energy=grams*vaporizationJPerGram(id);
        double stored=id.equals("water")?Math.min(boilingEnergy(s),energy):0;
        if(stored>0)boilingEnergy(s,boilingEnergy(s)-stored);
        record(s,-energy,"vaporization");
        double sensible=energy-stored;
        if(sensible>0)TemperatureSystem.setRawTemp(s,Math.max(boilingPoint,TemperatureSystem.getTemp(s)-sensible/capacity(s)));
        if(id.equals("water")&&!openWater(s)&&!VesselHeating.isSealed(s)&&boilingEnergy(s)>0){
            double remaining=boilingEnergy(s);boilingEnergy(s,0);applyHeat(s,remaining);
        }
    }
    /** Material carries its sensible heat; receiver glass is included once in final capacity. */
    public static void mix(ItemStack source,ItemStack target,List<LabVesselItem.Entry> transferred){
        com.example.chemistry.radiation.RadioLedger.onMix(source,target,transferred);
        double cp=transferred.stream().mapToDouble(e->e.amount()*specificHeat(e.type(),e.id())).sum();
        if(cp<=0)return;
        double q=cp*(TemperatureSystem.getTemp(source)-TemperatureSystem.getTemp(target));
        addHeat(target,q,"mixing");
    }
    public static void transferPending(ItemStack from,ItemStack to,double fraction){
        double q=boilingEnergy(from)*fraction;
        if(q>0){boilingEnergy(from,boilingEnergy(from)-q);record(from,-q,"transferred_latent");addHeat(to,q,"transferred_latent");}
    }
    /** Provisional game rupture/heat values, shared with the hazard model. */
    public static double reactionJoules(com.example.chemistry.data.Reactions.Reaction reaction,double extent){
        if(reaction.reactants().stream().noneMatch(e->e.id().equals("water")))return 0;
        double q=0;
        for(var e:reaction.reactants()){
            double perMole=switch(e.id()){
                case "sodium","potassium","lithium"->180000;
                case "sodium_hydride","potassium_hydride","calcium_hydride"->160000;
                default->0;
            };
            q+=extent*e.coefficient()*perMole;
        }
        return q;
    }
    private ThermalSystem(){}
}
