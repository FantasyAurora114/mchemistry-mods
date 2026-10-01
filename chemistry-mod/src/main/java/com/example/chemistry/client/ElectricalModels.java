package com.example.chemistry.client;
import java.util.*;
import com.example.chemistry.ChemistryMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.*;
@EventBusSubscriber(modid=ChemistryMod.MODID,value=Dist.CLIENT)
public final class ElectricalModels {
    private static final Map<String,StandaloneModelKey<BlockStateModel>> KEYS=new HashMap<>();
    @SubscribeEvent public static void register(ModelEvent.RegisterStandalone event){
        for(String id:new String[]{"bench_power_supply","hofmann_voltameter"}){
            int size=id.equals("bench_power_supply")?ElectricalModelGeometry.BENCH_POWER_SUPPLY.length:ElectricalModelGeometry.HOFMANN_VOLTAMETER.length;
            for(int i=0;i<size;i++)add(event,id+"/part_"+i);
        }
        add(event,"electrical_unit");for(String id:new String[]{"watch_glass","buchner_funnel","lab_voltmeter","lab_resistor","glass_rod"})add(event,id);
        add(event,"straight_condenser_water");
        for(String id:new String[]{"temperature_controlled_circulator","circulating_water_vacuum_pump"})
            for(String part:new String[]{"body","lid","main","left","right"})add(event,id+"_"+part);
        for(int i=0;i<8;i++){add(event,"iron_stand_base_"+i);add(event,"iron_stand_clip_"+i);}
        add(event,"deep_pneumatic_trough");add(event,"trough_liquid_unit");
        for(String metal:new String[]{"copper","silver"})for(String color:new String[]{"red","black"})for(String part:new String[]{"clip","mesh"})add(event,"clip_mesh_"+metal+"_"+color+"_"+part);
        for(String part:new String[]{"glass","stopper","valve","clamp"})add(event,"separatory_"+part);
        for(int top=1;top<=20;top++){
            add(event,String.format(java.util.Locale.ROOT,"separatory_liquid_%02d",top));
            for(int bottom=1;bottom<top;bottom++)add(event,String.format(java.util.Locale.ROOT,"separatory_band_%02d_%02d",bottom,top));
        }
        for(int top=2;top<=20;top++)for(int bottom=1;bottom<top;bottom++)
            add(event,String.format(java.util.Locale.ROOT,"erlenmeyer_band_%02d_%02d",bottom,top));
        add(event,"burette_clamp");
        add(event,"burette_liquid_unit");
        for(String id:new String[]{"burette_glass","burette_alkali","burette_ptfe","burette_amber_glass","burette_amber_ptfe"})
            for(String part:new String[]{"glass","valve"})add(event,id+"_"+part);
        for(String part:new String[]{"glass","paper","clamp"})add(event,"filter_funnel_"+part);
    }
    private static void add(ModelEvent.RegisterStandalone e,String id){var key=new StandaloneModelKey<BlockStateModel>(() -> "mchemistry:block/"+id);KEYS.put(id,key);e.register(key,SimpleUnbakedStandaloneModel.blockStateModel(ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID,"block/"+id)));}
    public static BlockStateModel get(String id){var key=KEYS.get(id);return key==null?null:Minecraft.getInstance().getModelManager().getStandaloneModel(key);}
}
