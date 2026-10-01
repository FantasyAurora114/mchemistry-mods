package com.example.chemistry.utility;
import com.example.chemistry.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
/** Gas quantities are reference mL; an evacuated sealed headspace is never refilled with air. */
public final class VacuumState {
    public static boolean enabled(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("chem_vacuum",false);}
    public static void enable(ItemStack s){if(enabled(s))return;VesselGasPhase.normalize(s);var t=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();t.putBoolean("chem_vacuum",true);s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));}
    public static void release(ItemStack s){if(!enabled(s))return;var t=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();t.remove("chem_vacuum");s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));VesselGasPhase.normalize(s);}
    public static double pressure(ItemStack s){double free=VesselGasPhase.freeVolumeMl(s);return free<=1e-9?101.325:101.325*VesselGasPhase.read(s).stream().mapToDouble(VesselGasPhase.Part::ml).sum()/free;}
    public static double boilingPoint(ItemStack s,String id){double standard=com.example.chemistry.data.ChemicalInfoProvider.boilingPointOf("liquid_"+id);if(!enabled(s)||!VesselHeating.isSealed(s)||!id.equals("water"))return standard;// NIST WebBook water Antoine coefficients (Stull 1947), P in bar, T in K.
        // https://webbook.nist.gov/cgi/cbook.cgi?ID=C7732185&Mask=4&Type=ANTOINE
        double bar=Math.clamp(pressure(s),20,101.325)/100;
        return Math.min(standard,1435.264/(4.6543-Math.log10(bar))+64.848-273.15);}
    private VacuumState(){}
}
