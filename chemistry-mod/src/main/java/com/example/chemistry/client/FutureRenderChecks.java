package com.example.chemistry.client;
import com.example.chemistry.ChemistryMod;
import com.example.chemistry.data.FutureChemicals;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.transfer.BottleCodes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid=ChemistryMod.MODID,value=Dist.CLIENT)
public final class FutureRenderChecks {
    private static boolean done;
    private static void verify(ItemStack stack){var mc=Minecraft.getInstance();var state=new ItemStackRenderState();mc.getItemModelResolver().updateForTopItem(state,stack,ItemDisplayContext.GUI,mc.level,null,0);if(state.isEmpty()||state.getModelBoundingBox().getYsize()<=0)throw new IllegalStateException("Empty batch reagent model "+BottleCodes.substanceKeyOf(stack));}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(done||!Boolean.getBoolean("mchemistry.verifyFutureBatches")||Minecraft.getInstance().getOverlay()!=null)return;
        done=true;int count=0;
        for(var r:FutureChemicals.ALL){
            if(r.phase().equals("SOLID")){
                for(boolean open:new boolean[]{false,true}){var jar=new ItemStack(ModItems.SOLID_JAR.get());BottleCodes.setSolid(jar,r.id(),open);BottleCodes.refreshModel(jar);verify(jar);count++;}
                var powder=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry","loose_"+r.id()));verify(new ItemStack(powder));count++;
            }
            if(r.phase().equals("LIQUID")||r.ceiling()>0){String id=r.phase().equals("LIQUID")?r.id():r.id()+"_solution";for(var item:java.util.List.of(ModItems.LIQUID_BOTTLE.get(),ModItems.DROPPER_BOTTLE.get()))for(boolean open:new boolean[]{false,true}){var bottle=new ItemStack(item);BottleCodes.setLiquid(bottle,id,open,50);BottleCodes.refreshModel(bottle);verify(bottle);count++;}}
        }
        for(var e:com.example.chemistry.data.FutureElements.ALL)for(String form:java.util.List.of("ingot","dust","nugget")){
            if(form.equals("dust")&&java.util.Set.of("rubidium","caesium").contains(e.id()))continue;
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry","element_"+e.id()+"_"+form));verify(new ItemStack(item));count++;
        }
        for(String gas:java.util.List.of("phosgene","ozone","dinitrogen_tetroxide","hydrogen_fluoride","hydrogen_bromide","hydrogen_iodide","boron_trifluoride","silane")){
            for(boolean sealed:new boolean[]{false,true}){verify(ModItems.gasBottle(gas,sealed));count++;}
            for(boolean tall:new boolean[]{false,true}){verify(com.example.chemistry.item.GasCylinderItem.filled(tall,gas,tall?100000:10000));count++;}
        }
        ChemistryMod.LOGGER.info("Future reagent inventory models passed: {} opened/closed bottles, droppers and powders",count);
    }
    private FutureRenderChecks(){}
}
