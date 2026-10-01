package com.example.chemistry;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.data.ChemicalInfoProvider;
import com.example.chemistry.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.*;
import java.util.*;

/** A 0.05 mL aliquot is removed from the vessel; testing consumes the aliquot and one paper. */
public final class GlassRodSampling {
    private static CompoundTag tag(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static boolean dip(Player player,ItemStack rod,ItemStack vessel){
        if(com.example.chemistry.organic.OrganicApparatus.covered(vessel))return false;
        if(VesselHeating.isSealed(vessel)){player.displayClientMessage(Component.literal("请先打开容器"),true);return false;}
        if(tag(rod).contains("rod_sample")){player.displayClientMessage(Component.literal("玻璃棒已有样品，请先用试纸检测"),true);return false;}
        LabVesselItem.normalizeSolutions(vessel);
        com.example.chemistry.solution.AcidBaseEquilibrium.tick(vessel);
        var phases=com.example.chemistry.organic.LiquidPhases.read(vessel);
        var liquids=phases.separated()?phases.top().entries():LabVesselItem.getContents(vessel).stream().filter(e->e.type().equals("liquid")).toList();
        double volume=liquids.stream().mapToDouble(e->LabVesselItem.entryVolume(vessel,e)).sum();if(volume<=0)return false;
        var equilibrium=com.example.chemistry.solution.AcidBaseEquilibrium.read(liquids,volume);
        var edta = com.example.chemistry.solution.EdtaEquilibrium.read(liquids, volume);
        // Only explicitly known waterless organic samples suppress pH; legacy aqueous reagent IDs may hide solvent water.
        boolean aqueous=liquids.stream().anyMatch(e->!Set.of("toluene","carbon_tetrachloride","ethanol","methanol","acetone","glycerol").contains(e.id()));
        double fraction=Math.min(.05,volume)/volume;ListTag sample=new ListTag();
        for(var e:liquids){var t=new CompoundTag();double mass=e.amount()*fraction;t.putString("id",e.id());t.putDouble("grams",mass);sample.add(t);LabVesselItem.consumeMass(vessel,"liquid",e.id(),mass);}
        var t=tag(rod);t.put("rod_sample",sample);t.putDouble("rod_sample_ml",volume*fraction);t.putDouble("rod_sample_ph",edta!=null?edta.ph():equilibrium!=null?equilibrium.ph():estimate(liquids,volume));t.putBoolean("rod_sample_equilibrium",edta!=null||equilibrium!=null);t.putBoolean("rod_sample_aqueous",aqueous);rod.set(DataComponents.CUSTOM_DATA,CustomData.of(t));
        player.displayClientMessage(Component.literal("玻璃棒已蘸取少量液体；另一只手持 pH 试纸右键检测"),true);return true;
    }
    /** Approximate paper reading from the existing acidity catalogue; not a general activity solver. */
    public static double estimate(List<LabVesselItem.Entry> entries,double ml){
        var edta = com.example.chemistry.solution.EdtaEquilibrium.read(entries, ml);
        if (edta != null) return edta.ph();
        var equilibrium=com.example.chemistry.solution.AcidBaseEquilibrium.read(entries,ml);
        if(equilibrium!=null)return equilibrium.ph();
        double acid=0,base=0;
        for(var e:entries){
            int equivalents=switch(e.id()){case "sodium_hydroxide_solution","potassium_hydroxide_solution"->-1;case "limewater_clear"->-2;case "nitric_acid","nitric_acid_concentrated","hydrochloric_acid","hydrochloric_acid_concentrated"->1;default->0;};
            if(equivalents!=0){double moles=e.amount()/ChemicalInfoProvider.molarMassOf("liquid_"+e.id());if(equivalents>0)acid+=1000*moles*equivalents;else base-=1000*moles*equivalents;continue;}
            var info=ChemicalInfoProvider.forItem("liquid_"+e.id());if(info==null)continue;String ph=info.ph();
            if(ph.equals("n/a"))continue;
            try{double value=Double.parseDouble(ph.split("\\|")[0].replace("<","").replace(">",""));double volume=e.amount()/Math.max(.01,ChemicalInfoProvider.densityOfLiquid(e.id()));
                if(value<7)acid+=volume*Math.pow(10,-value);else if(value>7)base+=volume*Math.pow(10,value-14);
            }catch(NumberFormatException ignored){ /* Catalogue ranges have no calibrated scalar reading. */ }
        }
        double net=(acid-base)/Math.max(.00001,ml);return Math.clamp(net>1e-7?-Math.log10(net):net< -1e-7?14+Math.log10(-net):7,0,14);
    }
    public static boolean test(Player p,ItemStack main,ItemStack off){
        ItemStack rod=main.is(ModItems.GLASS_ROD.get())?main:off.is(ModItems.GLASS_ROD.get())?off:ItemStack.EMPTY;
        ItemStack paper=rod==main?off:main;
        if(rod.isEmpty()||!net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(paper.getItem()).getPath().equals("ph_test_paper")||!tag(rod).contains("rod_sample"))return false;
        double rawPh=tag(rod).getDoubleOr("rod_sample_ph",7);
        double ph=Double.isFinite(rawPh)?Math.clamp(rawPh,0,14):7;
        boolean aqueous=tag(rod).getBooleanOr("rod_sample_aqueous",true);
        boolean equilibrium=tag(rod).getBooleanOr("rod_sample_equilibrium",false);ItemStack used=new ItemStack(ModItems.USED_PH_PAPER.get());
        used.set(DataComponents.CUSTOM_NAME,Component.literal(!aqueous?"pH 试纸：不适用（非水样品）":String.format(Locale.ROOT,
                equilibrium?"pH 试纸：约 %.0f":"pH 试纸：估计 %.0f（未建模体系）",ph)));
        var reading=new CompoundTag();if(aqueous)reading.putDouble("ph_value",ph);reading.putBoolean("ph_equilibrium",equilibrium);reading.putBoolean("ph_applicable",aqueous);
        used.set(DataComponents.CUSTOM_DATA,CustomData.of(reading));
        int[] palette={0xB62D36,0xCB4234,0xE76A36,0xED943E,0xF1BB48,0xDDCF55,0xA5BD5A,0x6DA85D,0x419F85,0x379995,0x398FA7,0x4867A1,0x66549B,0x815291,0x984A86};
        used.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(List.of(),List.of(),List.of(),List.of(aqueous?palette[(int)Math.round(ph)]:0xDDCFB0)));
        var t=tag(rod);t.remove("rod_sample");t.remove("rod_sample_ml");t.remove("rod_sample_ph");t.remove("rod_sample_equilibrium");t.remove("rod_sample_aqueous");rod.set(DataComponents.CUSTOM_DATA,CustomData.of(t));paper.shrink(1);
        if(!p.getInventory().add(used))p.drop(used,false);ExperimentFeedback.send(p,used.getHoverName());return true;
    }
    private GlassRodSampling(){}
}
