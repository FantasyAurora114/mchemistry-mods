package com.example.chemistry.garden;
import com.example.chemistry.*;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.solution.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import java.util.*;

/** Membrane stages and bounded geometry reference actual precipitate; geometry never owns extra material. */
public final class ChemicalGarden {
    public static final String KEY="chem_garden";
    public record Stem(String salt,double mass,int segments,int age,double fill){}
    public static double mass(ItemStack s,String type,String id){return LabVesselItem.getContents(s).stream().filter(e->e.type().equals(type)&&e.id().equals(id)).mapToDouble(LabVesselItem.Entry::amount).sum();}
    private static CompoundTag tag(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    private static CompoundTag state(ItemStack s){return tag(s).getCompoundOrEmpty(KEY);}
    public static boolean beaker(ItemStack s){return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().startsWith("beaker_");}
    public static boolean eligible(ItemStack s){return beaker(s)&&mass(s,"liquid","water")>1&&mass(s,"liquid","sodium_silicate_solution")>1e-5&&!tag(s).getBooleanOr("chem_garden_broken",false);}
    public static boolean reserve(ItemStack s,String id){return eligible(s)&&(id.equals("copper_sulfate_anhydrous")||id.equals("iron_iii_chloride"));}
    public static void stir(ItemStack s){if(!state(s).isEmpty()){var t=tag(s);t.remove(KEY);t.putBoolean("chem_garden_broken",true);s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));LabVesselItem.updateTint(s);}}
    public static List<Stem> stems(ItemStack s){
        var g=state(s);var out=new ArrayList<Stem>();
        for(String salt:List.of("copper_sulfate_anhydrous","iron_iii_chloride")) {
            var p=g.getCompoundOrEmpty(salt);String product=salt.startsWith("copper")?"copper_ii_hydroxide":"iron_iii_hydroxide";
            double stored=p.getDoubleOr("mass",0),available=mass(s,"solid",product);
            double retained=Math.min(stored,available);int segments=Math.min(32,(int)Math.floor(p.getIntOr("segments",0)*(stored>0?retained/stored:0)));
            if(retained>0)out.add(new Stem(salt,retained,segments,p.getIntOr("age",0),p.getDoubleOr("fill",0)));
        }return List.copyOf(out);
    }
    public static void tick(ItemStack s){
        if(!eligible(s)||com.example.chemistry.VesselHeating.isSealed(s))return;
        var g=state(s);boolean changed=false;
        for(String salt:List.of("copper_sulfate_anhydrous","iron_iii_chloride")){
            var p=g.getCompoundOrEmpty(salt);double raw=mass(s,"solid",salt);
            if(raw<=1e-9&&p.isEmpty())continue;
            int age=Math.min(100000,p.getIntOr("age",0))+1; p.putInt("age",age);changed=true;
            int count=p.getIntOr("segments",0);
            double fill=LabVesselItem.usedVolume(s)/((LabVesselItem)s.getItem()).capacity();
            // Dissolve -> membrane -> swelling -> rupture; finite, provisional rates.
            boolean membrane=age==20||age==40;
            if((membrane||age>=80&&age%20==0)&&count<32&&(count+1)*.018<fill-.025&&raw>1e-9){
                boolean copper=salt.startsWith("copper");double silicateRatio=copper?1:1.5,waterRatio=copper?1:1.5;
                double n=Math.min(raw/mm("solid",salt),Math.min(mass(s,"liquid","sodium_silicate_solution")/(silicateRatio*mm("solid","sodium_silicate")),mass(s,"liquid","water")/(waterRatio*mm("liquid","water"))));
                double concentration=mass(s,"liquid","sodium_silicate_solution")/Math.max(1,mass(s,"liquid","water"));
                n=Math.min(n,.00002*Math.clamp(concentration/.05,.1,2));
                if(n>1e-12){
                    var before=SolutionSpecies.analyticalSnapshot(s);var trial=s.copy();
                    LabVesselItem.consumeMass(trial,"solid",salt,n*mm("solid",salt));
                    LabVesselItem.consumeMass(trial,"liquid","sodium_silicate_solution",n*silicateRatio*mm("solid","sodium_silicate"));
                    LabVesselItem.consumeMass(trial,"liquid","water",n*waterRatio*mm("liquid","water"));
                    String product=copper?"copper_ii_hydroxide":"iron_iii_hydroxide";
                    LabVesselItem.addMass(trial,"solid",product,n*mm("solid",product));
                    LabVesselItem.addMass(trial,"solid","silicon_dioxide",n*silicateRatio*mm("solid","silicon_dioxide"));
                    LabVesselItem.addMass(trial,"liquid",copper?"sodium_sulfate_solution":"sodium_chloride_solution",n*(copper?1:3)*mm("solid",copper?"sodium_sulfate":"sodium_chloride"));
                    if(!Conservation.compare(before,SolutionSpecies.analyticalSnapshot(trial)).conserved())throw new IllegalStateException("Garden material transaction unbalanced");
                    com.example.chemistry.filtration.Filtration.commit(s,trial);
                    p.putDouble("mass",p.getDoubleOr("mass",0)+n*mm("solid",product));p.putInt("segments",count+(membrane?0:1));p.putDouble("fill",fill);
                }
            }
            g.put(salt,p);
        }
        if(changed){var t=tag(s);t.put(KEY,g);s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));LabVesselItem.updateTint(s);}
    }
    public static double mm(String type,String id){var parts=SpeciesCatalog.components(new SpeciesCatalog.ContentKey(type,id));if(parts==null)throw new IllegalArgumentException(id);return parts.entrySet().stream().mapToDouble(e->SpeciesCatalog.get(e.getKey()).molarMass()*e.getValue()).sum();}
    public static void appendInfo(List<net.minecraft.network.chat.Component> lines,ItemStack s){
        var g=state(s);if(g.isEmpty())return;
        for(String salt:g.keySet()){var p=g.getCompoundOrEmpty(salt);int age=p.getIntOr("age",0);String phase=age<20?"表面溶解":age<40?"形成沉淀膜":age<80?"吸水膨胀":"破膜/管状生长";
            if(!eligible(s)||com.example.chemistry.VesselHeating.isSealed(s)||mass(s,"solid",salt)<=1e-9||p.getIntOr("segments",0)>=32||(p.getIntOr("segments",0)+1)*.018>=LabVesselItem.usedVolume(s)/((LabVesselItem)s.getItem()).capacity()-.025)phase="停止生长";
            lines.add(net.minecraft.network.chat.Component.literal("水中花园："+(salt.startsWith("copper")?"铜盐":"铁盐")+" "+phase+"；"+p.getIntOr("segments",0)+" 段"));}
    }
    private ChemicalGarden(){}
}
