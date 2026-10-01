package com.example.chemistry.transfer;
import com.example.chemistry.item.*;
import com.example.chemistry.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
/** Atomic reagent transfers. Empty bottles retain their item and only lose the substance ID. */
public final class BottleQuantities {
    public static boolean pour(ItemStack source,ItemStack vessel,int requested){
        if(com.example.chemistry.organic.OrganicApparatus.covered(vessel))return false;
        String id=BottleCodes.liquidIdOf(source);if(id==null||BottleCodes.isSealed(source))return false;
        int amount=Math.min(requested,BottleCodes.volumeOf(source));if(amount<=0)return false;
        var before=vessel.copy();if(!LabVesselItem.addLiquid(vessel,id,amount))return false;com.example.chemistry.radiation.RadioLedger.inherit(source,before,vessel);
        BottleCodes.setVolume(source,BottleCodes.volumeOf(source)-amount);return true;
    }
    public static boolean fillDropper(ItemStack bottle,ItemStack dropper){
        String id=BottleCodes.liquidIdOf(bottle);if(id==null||BottleCodes.isSealed(bottle)||!DropperHelper.isEmpty(dropper))return false;
        int amount=Math.min(DropperHelper.CAPACITY,BottleCodes.volumeOf(bottle));if(amount<=0)return false;
        var before=dropper.copy();DropperHelper.fill(dropper,id,amount);com.example.chemistry.radiation.RadioLedger.inherit(bottle,before,dropper);BottleCodes.setVolume(bottle,BottleCodes.volumeOf(bottle)-amount);return true;
    }
    public static boolean takeSolid(ItemStack bottle,ItemStack tool){
        String id=BottleCodes.solidIdOf(bottle);if(id==null||BottleCodes.isSealed(bottle)||!SolidToolItem.isEmpty(tool))return false;
        double grams=Math.min(5,BottleCodes.solidGrams(bottle));if(grams<=0)return false;
        var before=tool.copy();SolidToolItem.pickUp(tool,id,grams);com.example.chemistry.radiation.RadioLedger.inherit(bottle,before,tool);BottleCodes.setSolidGrams(bottle,BottleCodes.solidGrams(bottle)-grams);return true;
    }
    public static boolean putTool(ItemStack tool,ItemStack vessel){
        if(com.example.chemistry.organic.OrganicApparatus.covered(vessel))return false;
        if(!(vessel.getItem() instanceof LabVesselItem v))return false;
        var copy=vessel.copy();if(!LabVesselItem.addMass(copy,"solid",SolidToolItem.getHeldSolid(tool),SolidToolItem.heldGrams(tool)))return false;
        if(LabVesselItem.usedVolume(copy)>v.capacity()+1e-6)return false;
        com.example.chemistry.radiation.RadioLedger.inherit(tool,vessel,copy);vessel.set(DataComponents.CUSTOM_DATA,copy.get(DataComponents.CUSTOM_DATA));LabVesselItem.updateTint(vessel);return true;
    }
    /** Main bottle pours into the other bottle; an empty main bottle may instead refill from offhand. */
    public static boolean refill(ItemStack main,ItemStack off){
        if((BottleCodes.isLiquidBottle(main)||BottleCodes.isDropperBottle(main))&&(BottleCodes.isLiquidBottle(off)||BottleCodes.isDropperBottle(off))){
            ItemStack from=BottleCodes.isEmpty(main)?off:main,to=from==main?off:main;
            if(BottleCodes.isSealed(from)||BottleCodes.isSealed(to))return false;
            String id=BottleCodes.liquidIdOf(from),other=BottleCodes.liquidIdOf(to);if(id==null||(other!=null&&!other.equals(id)))return false;
            int n=Math.min(25,Math.min(BottleCodes.volumeOf(from),BottleCodes.bottleCapacityOf(to)-BottleCodes.volumeOf(to)));if(n<=0)return false;
            var before=to.copy();BottleCodes.setLiquid(to,id,false,BottleCodes.volumeOf(to)+n);com.example.chemistry.radiation.RadioLedger.inherit(from,before,to);BottleCodes.refreshModel(to);BottleCodes.setVolume(from,BottleCodes.volumeOf(from)-n);return true;
        }
        if(BottleCodes.isSolidJar(main)&&!BottleCodes.isSealed(main)){
            String path=BottleCodes.pathOf(off),id=path.startsWith("loose_")?path.substring(6):SolidToolItem.getHeldSolid(off);
            double mass=BottleCodes.isSolidJar(off)?Math.min(5,BottleCodes.solidGrams(off)):off.getItem() instanceof SolidToolItem?SolidToolItem.heldGrams(off):off.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getDoubleOr("chem_grams",5);
            if(BottleCodes.isSolidJar(off)&&BottleCodes.isSealed(off))return false;
            if(id==null||mass<=0)return false;String existing=BottleCodes.solidIdOf(main);
            if(existing!=null&&!existing.equals(id)||BottleCodes.solidGrams(main)+mass>100+1e-8)return false;
            var before=main.copy();double amount=BottleCodes.solidGrams(main)+mass;BottleCodes.setSolid(main,id,false);BottleCodes.setSolidGrams(main,amount);com.example.chemistry.radiation.RadioLedger.inherit(off,before,main);
            if(BottleCodes.isSolidJar(off))BottleCodes.setSolidGrams(off,BottleCodes.solidGrams(off)-mass);else if(off.getItem() instanceof SolidToolItem)SolidToolItem.clear(off);else off.shrink(1);return true;
        }
        return false;
    }
    private BottleQuantities(){}
}
