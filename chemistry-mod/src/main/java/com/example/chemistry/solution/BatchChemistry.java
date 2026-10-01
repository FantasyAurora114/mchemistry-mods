package com.example.chemistry.solution;

import java.util.List;
import java.util.Map;
import com.example.chemistry.data.BatchChemicals;
import com.example.chemistry.data.BatchReactions;
import com.example.chemistry.data.Solutions;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.filtration.Filtration;
import net.minecraft.world.item.ItemStack;

/** Finite aqueous precipitation and amphoteric transactions. Master contents remain the source of truth. */
public final class BatchChemistry {
    private record Recipe(Map<String,Integer> lhs,Map<String,Integer> rhs,String display){}
    private static final List<Recipe> RECIPES=BatchReactions.ALL.stream().map(r->{
        var lhs=new java.util.LinkedHashMap<String,Integer>();var rhs=new java.util.LinkedHashMap<String,Integer>();
        r.reactants().forEach(e->lhs.put(e.type()+":"+e.id(),e.coefficient()));r.products().forEach(e->rhs.put(e.type()+":"+e.id(),e.coefficient()));
        return new Recipe(Map.copyOf(lhs),Map.copyOf(rhs),r.display());
    }).toList();
    private static final String COMPLETED="batch_completed";
    public static void unlock(ItemStack stack,net.minecraft.world.entity.player.Player player){
        if(player==null)return;
        var tag=stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if(!tag.contains(COMPLETED))return;
        for(var entry:tag.getListOrEmpty(COMPLETED))if(entry instanceof net.minecraft.nbt.StringTag text)com.example.chemistry.api.ReactionUnlocks.unlock(player,text.value());
        tag.remove(COMPLETED);stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
    }
    public static void recordCompleted(ItemStack stack,String display){
        var tag=stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        var list=tag.getListOrEmpty(COMPLETED);var key=net.minecraft.nbt.StringTag.valueOf(display);
        if(!list.contains(key)){list.add(key);tag.put(COMPLETED,list);stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));}
    }
    public static double mass(ItemStack stack,String type,String id){
        return LabVesselItem.getContents(stack).stream().filter(e->e.type().equals(type)&&(e.id().equals(id)||((id.equals("hydrochloric_acid")||id.equals("nitric_acid"))&&e.id().equals(id+"_concentrated")))).mapToDouble(LabVesselItem.Entry::amount).sum();
    }
    private static void consume(ItemStack stack,String type,String id,double grams){
        double first=Math.min(grams,LabVesselItem.getContents(stack).stream().filter(e->e.type().equals(type)&&e.id().equals(id)).mapToDouble(LabVesselItem.Entry::amount).sum());
        LabVesselItem.consumeMass(stack,type,id,first);
        if(grams>first&&(id.equals("hydrochloric_acid")||id.equals("nitric_acid")))LabVesselItem.consumeMass(stack,type,id+"_concentrated",grams-first);
    }
    public static double molar(String type,String id){
        var components=SpeciesCatalog.components(new SpeciesCatalog.ContentKey(type,id));
        if(components==null)throw new IllegalArgumentException("Missing batch formula "+type+":"+id);
        return components.entrySet().stream().mapToDouble(e->SpeciesCatalog.get(e.getKey()).molarMass()*e.getValue()).sum();
    }
    public static boolean transact(ItemStack stack,Map<String,Integer> lhs,Map<String,Integer> rhs,double maximum){
        double extent=maximum;
        for(var e:lhs.entrySet()){String[] key=e.getKey().split(":",2);extent=Math.min(extent,mass(stack,key[0],key[1])/molar(key[0],key[1])/e.getValue());}
        if(!Double.isFinite(extent)||extent<=1e-12)return false;
        ItemStack trial=stack.copy();var before=SolutionSpecies.analyticalSnapshot(stack);
        for(var e:lhs.entrySet()){String[] key=e.getKey().split(":",2);consume(trial,key[0],key[1],extent*e.getValue()*molar(key[0],key[1]));}
        for(var e:rhs.entrySet()){String[] key=e.getKey().split(":",2);LabVesselItem.addMass(trial,key[0],key[1],extent*e.getValue()*molar(key[0],key[1]));}
        var report=Conservation.compare(before,SolutionSpecies.analyticalSnapshot(trial));
        if(!report.conserved())throw new IllegalStateException("Unbalanced batch transaction "+lhs+" "+report.differences());
        Filtration.commit(stack,trial);return true;
    }
    public static void tick(ItemStack stack){
        if(mass(stack,"liquid","water")<=0)return;
        var available=new java.util.HashSet<String>();
        LabVesselItem.getContents(stack).forEach(e->{if(e.amount()>0)available.add(e.type()+":"+e.id());if(e.id().endsWith("_concentrated"))available.add(e.type()+":"+e.id().replace("_concentrated",""));});
        for(var recipe:RECIPES){
            if(!available.containsAll(recipe.lhs().keySet()))continue;
            if(recipe.display().startsWith("Ni²")&&approximatePh(stack)<7)continue;
            if(transact(stack,recipe.lhs(),recipe.rhs(),BatchPrecipitation.formationLimit(stack,recipe.lhs(),recipe.rhs(),.0002))){
                recordCompleted(stack,recipe.display());
                LabVesselItem.getContents(stack).forEach(e->{if(e.amount()>0)available.add(e.type()+":"+e.id());if(e.id().endsWith("_concentrated"))available.add(e.type()+":"+e.id().replace("_concentrated",""));});
            }
        }
        BatchPrecipitation.tick(stack);
        chromate(stack);
        HydrateChemistry.dissolve(stack);
    }
    /** Counterions are retained explicitly; acid/alkali consumes real equivalents. */
    private static void chromate(ItemStack stack){
        var entries=LabVesselItem.getContents(stack);
        if(entries.stream().noneMatch(e->e.id().contains("chromate")||e.id().contains("dichromate")))return;
        double ph=approximatePh(stack);
        for(String metal:List.of("potassium","sodium")){
            String chromate=metal+"_chromate_solution",dichromate=metal+"_dichromate_solution";
            if(ph<5.5)for(String acid:List.of("hydrochloric_acid","nitric_acid")){
                String spectator=metal+(acid.equals("hydrochloric_acid")?"_chloride_solution":"_nitrate_solution");
                transact(stack,Map.of("liquid:"+chromate,2,"liquid:"+acid,2),Map.of("liquid:"+dichromate,1,"liquid:"+spectator,2,"liquid:water",1),.0001);
            }
            else if(ph>8)for(String baseMetal:List.of("potassium","sodium")){
                Map<String,Integer> rhs=metal.equals(baseMetal)?Map.of("liquid:"+chromate,2,"liquid:water",1):Map.of("liquid:"+chromate,1,"liquid:"+baseMetal+"_chromate_solution",1,"liquid:water",1);
                transact(stack,Map.of("liquid:"+dichromate,1,"liquid:"+baseMetal+"_hydroxide_solution",2),rhs,.0001);
            }
        }
    }
    /** Strong acid/base readout for mixed salts; metal hydrolysis is not yet modelled. */
    public static double approximatePh(ItemStack stack){
        var exact=AcidBaseEquilibrium.read(stack);if(exact!=null)return exact.ph();
        double acid=0,base=0,litres=Math.max(.000001,SolutionSpecies.solutionLitres(stack));
        for(var e:LabVesselItem.getContents(stack)){
            if(!e.type().equals("liquid"))continue;
            if(e.id().startsWith("hydrochloric_acid"))acid+=e.amount()/molar("liquid","hydrochloric_acid");
            else if(e.id().startsWith("nitric_acid"))acid+=e.amount()/molar("liquid","nitric_acid");
            else if(e.id().equals("sodium_hydroxide_solution")||e.id().equals("potassium_hydroxide_solution"))base+=e.amount()/molar("liquid",e.id());
        }
        double excess=(acid-base)/litres;
        return Math.clamp(excess>1e-7?-Math.log10(excess):excess< -1e-7?14+Math.log10(-excess):7,0,14);
    }
    public static boolean indicator(String id){return List.of("methyl_red","bromothymol_blue","bromocresol_green","thymol_blue","methylene_blue","indigo_carmine","fluorescein","eosin_y").contains(id.replace("_solution",""));}
    public static int mix(int a,int b,double fraction){int rgb=0;double t=Math.clamp(fraction,0,1);for(int sh:new int[]{0,8,16})rgb|=(int)Math.round(((a>>sh)&255)*(1-t)+((b>>sh)&255)*t)<<sh;return rgb;}
    public static int color(ItemStack stack,int fallback){
        if(!(stack.getItem() instanceof LabVesselItem))return fallback;
        var entries=LabVesselItem.getContents(stack);
        boolean relevant=entries.stream().anyMatch(e->e.type().equals("liquid")&&(BatchChemicals.find(e.id().replace("_solution",""))!=null||e.id().contains("chromate")||e.id().contains("dichromate")));
        if(!relevant)return fallback;
        int color=fallback;double weight=0,red=0,green=0,blue=0;
        for(var e:entries){if(!e.type().equals("liquid")||!e.id().endsWith("_solution")||indicator(e.id()))continue;
            var r=BatchChemicals.find(e.id().replace("_solution",""));int rgb=r!=null?r.color():e.id().contains("dichromate")?0xDC762C:e.id().contains("chromate")?0xE9C52A:-1;
            if(rgb<0)continue;double w=e.amount()/molar("liquid",e.id());weight+=w;red+=((rgb>>16)&255)*w;green+=((rgb>>8)&255)*w;blue+=(rgb&255)*w;
        }
        if(weight>0){int rgb=((int)(red/weight)<<16)|((int)(green/weight)<<8)|(int)(blue/weight);color=mix(0xC4DCE2,rgb,1-Math.exp(-weight/Math.max(.001,SolutionSpecies.solutionLitres(stack))*80));}
        color=BatchCoordination.color(stack,color);
        double ph=entries.stream().anyMatch(e->indicator(e.id()))?approximatePh(stack):7;
        for(var e:LabVesselItem.getContents(stack)){
            if(!e.type().equals("liquid")||!indicator(e.id()))continue;
            String id=e.id().replace("_solution","");double pka=5.1;int low=0xC83E35,high=0xE7C43B;
            switch(id){
                case "bromothymol_blue"->{pka=7.1;low=0xE4CD36;high=0x276AAC;}
                case "bromocresol_green"->{pka=4.7;low=0xDFCE3D;high=0x27659C;}
                case "thymol_blue"->{pka=ph<4?1.7:8.9;low=ph<4?0xC83E35:0xE7C43B;high=ph<4?0xE7C43B:0x285FA6;}
                default->{if(!id.equals("methyl_red")){color=mix(color,BatchChemicals.find(id).color(),1-Math.exp(-e.amount()/Math.max(.001,SolutionSpecies.solutionLitres(stack))*20));continue;}}
            }
            int dye=mix(low,high,1/(1+Math.pow(10,pka-ph)));
            color=mix(color,dye,1-Math.exp(-e.amount()/Math.max(.001,SolutionSpecies.solutionLitres(stack))*1200));
        }
        return color;
    }
    private BatchChemistry(){}
}
