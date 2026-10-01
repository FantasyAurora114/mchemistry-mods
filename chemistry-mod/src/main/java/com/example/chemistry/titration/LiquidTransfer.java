package com.example.chemistry.titration;
import com.example.chemistry.ThermalSystem;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.filtration.Filtration;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.solution.Conservation;
import com.example.chemistry.solution.SolutionSpecies;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Homogeneous liquid-only portions, retaining each solute's actual mass and sensible heat. */
public final class LiquidTransfer {
    public static double pour(ItemStack source,ItemStack target,double requested){
        return portion(source,target,requested,null);
    }
    /** Top/bottom sampling stops at the current phase boundary. Stirred samples remain homogeneous. */
    public static double sample(ItemStack source,ItemStack target,double requested,boolean bottom){
        var normalized=source.copy();LabVesselItem.normalizeSolutions(normalized);
        var state=com.example.chemistry.organic.LiquidPhases.read(normalized);
        if(!state.modelled())return 0;
        if(!state.separated())return pour(source,target,requested);
        return portion(source,target,requested,(bottom?state.bottom():state.top()).entries());
    }
    private static double portion(ItemStack source,ItemStack target,double requested,List<LabVesselItem.Entry> selected){
        if(com.example.chemistry.organic.OrganicApparatus.covered(source)||com.example.chemistry.organic.OrganicApparatus.covered(target)||source==target||!(source.getItem() instanceof LabVesselItem)||!(target.getItem() instanceof LabVesselItem vessel)
                ||!Double.isFinite(requested)||requested<=0||VesselHeating.isSealed(source)||VesselHeating.isSealed(target))return 0;
        var from=source.copy();var to=target.copy();LabVesselItem.normalizeSolutions(from);LabVesselItem.normalizeSolutions(to);
        var entries=selected==null?LabVesselItem.getContents(from).stream().filter(e->e.type().equals("liquid")).toList():selected;
        double initialWater=LabVesselItem.getContents(from).stream().filter(e->e.id().equals("water")&&e.type().equals("liquid")).mapToDouble(LabVesselItem.Entry::amount).sum();
        double volume=com.example.chemistry.organic.LiquidPhases.volume(from,entries);
        double moved=Math.min(requested,Math.min(volume,Math.max(0,vessel.capacity()-LabVesselItem.usedVolume(to))));
        if(moved<=1e-9)return 0;
        double fraction=moved/volume;var transferred=new ArrayList<LabVesselItem.Entry>();
        var before=SolutionSpecies.analyticalSnapshot(from).plus(SolutionSpecies.analyticalSnapshot(to));
        for(var e:entries){
            double amount=e.amount()*fraction;transferred.add(new LabVesselItem.Entry(e.type(),e.id(),amount));
            LabVesselItem.consumeMass(from,e.type(),e.id(),amount);LabVesselItem.addMass(to,e.type(),e.id(),amount);
        }
        if(LabVesselItem.usedVolume(to)>vessel.capacity()+1e-7)return 0;
        if(!Conservation.compare(before,SolutionSpecies.analyticalSnapshot(from).plus(SolutionSpecies.analyticalSnapshot(to))).conserved())
            throw new IllegalStateException("Liquid transfer lost matter");
        ThermalSystem.mix(from,to,transferred);double movedWater=transferred.stream().filter(e->e.id().equals("water")).mapToDouble(LabVesselItem.Entry::amount).sum();
        ThermalSystem.transferPending(from,to,initialWater>0?movedWater/initialWater:0);
        if(com.example.chemistry.organic.LiquidPhases.read(source).dispersed())com.example.chemistry.organic.LiquidPhases.tick(to,true);
        Filtration.commit(source,from);Filtration.commit(target,to);return moved;
    }
    private LiquidTransfer(){}
}
