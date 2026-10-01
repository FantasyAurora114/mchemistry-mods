package com.example.chemistry.organic;

import com.example.chemistry.data.ChemicalInfoProvider;
import com.example.chemistry.data.Liquids;
import com.example.chemistry.data.Solutions;
import com.example.chemistry.item.LabVesselItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Derived binary liquid phases; entries remain the sole material ledger.
 * Solubilities are coarse room-temperature NIOSH values. No ternary activity model. */
public final class LiquidPhases {
    private static final Map<String,Double> SOLUBILITY=Map.ofEntries(Map.entry("toluene",.07),Map.entry("carbon_tetrachloride",.05),Map.entry("ethyl_acetate",8.3),Map.entry("n_hexane",.001),Map.entry("cyclohexane",.005),Map.entry("n_butanol",7.7),Map.entry("isoamyl_alcohol",2.0),Map.entry("isoamyl_acetate",.2),Map.entry("methyl_salicylate",.07),Map.entry("styrene",.03),Map.entry("benzaldehyde",.6),Map.entry("benzyl_alcohol",4.0),Map.entry("aniline",3.5),Map.entry("butyric_acid",7.7),Map.entry("acetylacetone",16.0),Map.entry("diethyl_oxalate",.6));
    private static final Set<String> MISCIBLE=Set.of("water","ethanol","ethanol_75","ethanol_95","methanol","acetone","glycerol","acetic_acid","isopropanol","n_propanol","ethylene_glycol","acetonitrile","dimethyl_sulfoxide","propionic_acid","lactic_acid","ethyl_lactate");
    private static final String MIXING="chem_liquid_mixing";
    public record Layer(String name,List<LabVesselItem.Entry> entries,double ml,double density,int color) {
        public Layer { entries=List.copyOf(entries); }
    }
    public record State(List<Layer> layers,boolean modelled,boolean dispersed) {
        public State { layers=List.copyOf(layers); }
        public boolean separated(){return modelled&&layers.size()==2&&!dispersed;}
        public Layer top(){return layers.get(layers.size()-1);}
        public Layer bottom(){return layers.get(0);}
    }
    public static double volume(ItemStack stack,List<LabVesselItem.Entry> entries){return entries.stream().mapToDouble(e->LabVesselItem.entryVolume(stack,e)).sum();}
    public static int color(String id){
        return Liquids.ALL.stream().filter(l->l.id().equals(id)).mapToInt(Liquids.Liquid::color).findFirst().orElse(0x8FC8E8);
    }
    private static Layer layer(ItemStack stack,String name,List<LabVesselItem.Entry> entries,boolean aqueous){
        double ml=volume(stack,entries),grams=entries.stream().mapToDouble(LabVesselItem.Entry::amount).sum();
        double r=0,g=0,b=0,weight=0;
        for(var e:entries){double w=LabVesselItem.entryVolume(stack,e);int c=color(e.id());
            // Visible dissolved salts colour the water; colourless dissolved organic traces do not whiten it.
            if(aqueous&&SOLUBILITY.containsKey(e.id()))continue;
            if(Solutions.soluteOf(e.id())!=null&&c!=0x8FC8E8)w=Math.max(w,Math.min(ml,e.amount()*8));
            r+=((c>>16)&255)*w;g+=((c>>8)&255)*w;b+=(c&255)*w;weight+=w;
        }
        int rgb=weight>0?((int)(r/weight)<<16)|((int)(g/weight)<<8)|(int)(b/weight):0x8FC8E8;
        if(aqueous)rgb=com.example.chemistry.solution.CoordinationEquilibrium.liquidColor(stack,rgb);
        rgb=Extraction.tint(entries,ml,rgb,aqueous);
        return new Layer(name,entries,ml,ml>0?grams/ml:0,rgb);
    }
    public static State read(ItemStack stack){
        var entries=LabVesselItem.getContents(stack).stream().filter(e->e.type().equals("liquid")&&e.amount()>1e-10).toList();
        if(entries.isEmpty())return new State(List.of(),true,false);
        var solvents=entries.stream().filter(e->SOLUBILITY.containsKey(e.id())).toList();
        boolean supported=solvents.size()<=1&&entries.stream().allMatch(e->MISCIBLE.contains(e.id())||SOLUBILITY.containsKey(e.id())||Solutions.soluteOf(e.id())!=null||Extraction.iodine(e.id()));
        // A cosolvent changes the phase diagram, so never apply the binary partition to it.
        if(!solvents.isEmpty()&&entries.stream().anyMatch(e->MISCIBLE.contains(e.id())&&!e.id().equals("water")))supported=false;
        if(!supported)return new State(List.of(layer(stack,"unmodelled",entries,false)),false,false);
        var water=entries.stream().filter(e->e.id().equals("water")).findFirst().orElse(null);
        if(water==null||solvents.isEmpty())return new State(List.of(layer(stack,"single",entries,water!=null)),true,false);
        var organic=solvents.get(0);double dissolved=Math.min(organic.amount(),water.amount()*SOLUBILITY.get(organic.id())/100);
        var aq=new ArrayList<LabVesselItem.Entry>();var orgEntries=new ArrayList<LabVesselItem.Entry>();
        for(var e:entries)if(!e.id().equals(organic.id())) {
            if(e.id().equals(Extraction.organicId(organic.id()))) orgEntries.add(e);else aq.add(e);
        }
        if(dissolved>0)aq.add(new LabVesselItem.Entry("liquid",organic.id(),dissolved));
        if(organic.amount()-dissolved<=1e-9){aq.addAll(orgEntries);return new State(List.of(layer(stack,"aqueous",aq,true)),true,false);}
        orgEntries.add(new LabVesselItem.Entry("liquid",organic.id(),organic.amount()-dissolved));
        var aqueous=layer(stack,"aqueous",aq,true);var nonaqueous=layer(stack,organic.id(),orgEntries,false);
        var layers=aqueous.density()>=nonaqueous.density()?List.of(aqueous,nonaqueous):List.of(nonaqueous,aqueous);
        return new State(layers,true,mixing(stack)>0);
    }
    public static double mixing(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getDoubleOr(MIXING,0);}
    public static void tick(ItemStack stack,boolean stirred){
        var state=read(stack);var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        double before=mixing(stack),after=state.layers().size()==2?(stirred?1:Math.max(0,before-.02)):0;
        if(after==before)return;
        if(after==0)tag.remove(MIXING);else tag.putDouble(MIXING,after);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));LabVesselItem.updateTint(stack);
    }
    public static void appendInfo(List<net.minecraft.network.chat.Component> lines,ItemStack stack){
        var state=read(stack);if(state.layers().isEmpty())return;
        if(!state.modelled()) {if(LabVesselItem.getContents(stack).stream().anyMatch(e->SOLUBILITY.containsKey(e.id())))lines.add(net.minecraft.network.chat.Component.literal("液相：复杂体系，尚未建立分层模型"));return;}
        if(state.layers().size()!=2)return;
        if(state.dispersed()){lines.add(net.minecraft.network.chat.Component.literal("液相：搅拌分散中；静置后恢复分层"));return;}
        for(int i=state.layers().size()-1;i>=0;i--){var l=state.layers().get(i);String name=l.name().equals("aqueous")?"水相":com.example.chemistry.api.goggles.ChemGoggleLines.liquidName(l.name());
            lines.add(net.minecraft.network.chat.Component.literal(String.format(java.util.Locale.ROOT,"%s：%s %.2f mL",i==0?"下层":"上层",name,l.ml())));}
    }
    private LiquidPhases(){}
}
