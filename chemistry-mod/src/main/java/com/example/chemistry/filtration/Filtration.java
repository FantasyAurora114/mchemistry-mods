package com.example.chemistry.filtration;

import com.example.chemistry.VesselHeating;
import com.example.chemistry.item.LabVesselItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/** Transactions use grams for each species; the receiver and retained mother liquor share one debit. */
public final class Filtration {
    public static final double MAX_RESIDUE_GRAMS=20;
    public static double liquidVolume(ItemStack s){return LabVesselItem.getContents(s).stream().filter(e->e.type().equals("liquid")).mapToDouble(e->LabVesselItem.entryVolume(s,e)).sum();}
    public static double solids(ItemStack s){return LabVesselItem.getContents(s).stream().filter(e->e.type().equals("solid")).mapToDouble(LabVesselItem.Entry::amount).sum();}
    public static void commit(ItemStack original,ItemStack result){
        original.set(DataComponents.CUSTOM_DATA,result.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY));
        if(result.has(DataComponents.CUSTOM_MODEL_DATA))original.set(DataComponents.CUSTOM_MODEL_DATA,result.get(DataComponents.CUSTOM_MODEL_DATA));
        else original.remove(DataComponents.CUSTOM_MODEL_DATA);
    }
    public static boolean pour(ItemStack source,ItemStack target,double maxVolume){
        if(com.example.chemistry.organic.OrganicApparatus.covered(source)||com.example.chemistry.organic.OrganicApparatus.covered(target)||source==target||!(source.getItem() instanceof LabVesselItem)||!(target.getItem() instanceof LabVesselItem v)
                ||VesselHeating.isSealed(source)||VesselHeating.isSealed(target))return false;
        var from=source.copy();var to=target.copy();LabVesselItem.normalizeSolutions(from);LabVesselItem.normalizeSolutions(to);
        double volume=LabVesselItem.usedVolume(from);if(volume<=0)return false;
        double f=Math.min(1,Math.min(maxVolume,Math.max(0,v.capacity()-LabVesselItem.usedVolume(to)))/volume);
        if(f>=1&&!com.example.chemistry.radiation.RadioLedger.carriers(from).isEmpty())f=.999; // A real retained film, subsequently removable by rinsing.
        if(f<=0)return false;
        if(LabVesselItem.getContents(from).stream().anyMatch(e->!e.type().equals("solid")&&!e.type().equals("liquid")))return false;
        var moved=new java.util.ArrayList<LabVesselItem.Entry>();
        for(var e:LabVesselItem.getContents(from)){double amount=e.amount()*f;moved.add(new LabVesselItem.Entry(e.type(),e.id(),amount));LabVesselItem.consumeMass(from,e.type(),e.id(),amount);LabVesselItem.addMass(to,e.type(),e.id(),amount);}
        if(LabVesselItem.usedVolume(to)>v.capacity()+1e-7)return false;
        com.example.chemistry.ThermalSystem.mix(from,to,moved);com.example.chemistry.ThermalSystem.transferPending(from,to,f);
        com.example.chemistry.radiation.RadioLedger.inherit(source,target,to);
        commit(source,from);commit(target,to);return true;
    }
    /** Returns discharged mL; a fully retained portion can enter the wet paper without a drip. */
    public static double step(ItemStack pending,ItemStack paper,ItemStack receiver){
        if(com.example.chemistry.organic.OrganicApparatus.covered(receiver)||!(receiver.getItem() instanceof LabVesselItem vessel)||VesselHeating.isSealed(receiver))return 0;
        double liquid=liquidVolume(pending),solid=solids(pending),retained=solids(paper);
        if(liquid<=1e-9||(!paper.isEmpty()&&retained>=MAX_RESIDUE_GRAMS-1e-9))return 0;
        double free=Math.max(0,vessel.capacity()-LabVesselItem.usedVolume(receiver));if(free<=1e-9)return 0;
        if(paper.isEmpty()){
            double before=LabVesselItem.usedVolume(receiver);
            return pour(pending,receiver,Math.min(1,free))?LabVesselItem.usedVolume(receiver)-before:0;
        }
        double fraction=Math.min(1,Math.min(1/(1+retained/5),free)/liquid);
        if(solid>0)fraction=Math.min(fraction,(MAX_RESIDUE_GRAMS-retained)/solid);
        if(fraction<=0)return 0;
        var from=pending.copy();var cake=paper.copy();var to=receiver.copy();
        double heldVolume=Math.min(liquid*fraction,Math.max(0,Math.min(5,(retained+solid*fraction)*.2)-liquidVolume(cake)));
        double retention=heldVolume/(liquid*fraction);
        var movedTo=new java.util.ArrayList<LabVesselItem.Entry>();var movedCake=new java.util.ArrayList<LabVesselItem.Entry>();
        for(var e:LabVesselItem.getContents(from)){
            if(!e.type().equals("liquid")&&!e.type().equals("solid"))return 0;
            double amount=e.amount()*fraction;LabVesselItem.consumeMass(from,e.type(),e.id(),amount);
            if(e.type().equals("solid")){LabVesselItem.addMass(cake,e.type(),e.id(),amount);movedCake.add(new LabVesselItem.Entry(e.type(),e.id(),amount));}
            else{LabVesselItem.addMass(cake,e.type(),e.id(),amount*retention);LabVesselItem.addMass(to,e.type(),e.id(),amount*(1-retention));movedCake.add(new LabVesselItem.Entry(e.type(),e.id(),amount*retention));movedTo.add(new LabVesselItem.Entry(e.type(),e.id(),amount*(1-retention)));}
        }
        if(LabVesselItem.usedVolume(to)>vessel.capacity()+1e-7)return 0;
        com.example.chemistry.ThermalSystem.mix(from,to,movedTo);com.example.chemistry.ThermalSystem.mix(from,cake,movedCake);
        double toFraction=fraction*(1-retention),cakeFraction=fraction*retention;
        com.example.chemistry.ThermalSystem.transferPending(from,to,toFraction);
        if(toFraction<1)com.example.chemistry.ThermalSystem.transferPending(from,cake,cakeFraction/(1-toFraction));
        com.example.chemistry.radiation.RadioLedger.inherit(pending,paper,cake);com.example.chemistry.radiation.RadioLedger.inherit(pending,receiver,to);
        commit(pending,from);commit(paper,cake);commit(receiver,to);
        return liquid*fraction-heldVolume;
    }
    private Filtration(){}
}
