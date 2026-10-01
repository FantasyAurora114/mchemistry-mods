package com.example.chemistry.electrical;
import com.example.chemistry.item.LabVesselItem;
import net.minecraft.world.item.ItemStack;

/** Charge-limited water splitting. Mole and gram balances are authoritative; mL is a display/export unit. */
public final class WaterElectrolysis {
    public static final double FARADAY=96485.33212;
    public static final double WATER_MOLAR=18.015;
    public static final double GAS_ML_PER_MOLE=24465.0; // 25 °C, 1 atm reference volume used at the gas-pipe boundary.
    public static final double SIDE_CAPACITY_ML=50;
    public record Result(double charge,double waterGrams,double hydrogenMoles,double oxygenMoles){}
    public static Result calculate(double current,double seconds,double water,double hydrogenSpaceMl,double oxygenSpaceMl,double totalSpaceMl){
        if(!Double.isFinite(current+seconds+water+hydrogenSpaceMl+oxygenSpaceMl+totalSpaceMl)||current<=0||seconds<=0||water<=0)return new Result(0,0,0,0);
        double h=Math.min(current*seconds/(2*FARADAY),water/WATER_MOLAR);
        h=Math.max(0,Math.min(h,Math.min(hydrogenSpaceMl/GAS_ML_PER_MOLE,Math.min(2*oxygenSpaceMl/GAS_ML_PER_MOLE,totalSpaceMl/(1.5*GAS_ML_PER_MOLE-WATER_MOLAR)))));
        return new Result(h*2*FARADAY,h*WATER_MOLAR,h,h/2);
    }
    public static double water(ItemStack stack){return LabVesselItem.getContents(stack).stream().filter(e->e.type().equals("liquid")&&e.id().equals("water")).mapToDouble(LabVesselItem.Entry::amount).sum();}
    public static boolean supported(ItemStack stack){return LabVesselItem.getContents(stack).stream().allMatch(e->e.type().equals("liquid")&&(e.id().equals("water")||e.id().equals("sodium_hydroxide_solution")||e.id().equals("potassium_hydroxide_solution")));}
    public static double conductance(ItemStack stack){double water=water(stack);if(water<=0)return 0;double electrolyte=LabVesselItem.getContents(stack).stream().filter(e->e.id().equals("sodium_hydroxide_solution")||e.id().equals("potassium_hydroxide_solution")).mapToDouble(LabVesselItem.Entry::amount).sum();return electrolyte<=0?1e-6:Math.min(.5,5*electrolyte/water);}
    private WaterElectrolysis(){}
}
