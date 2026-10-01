package com.example.chemistry.client.handbook;

import java.lang.reflect.*;
import java.util.*;
import com.example.chemistry.ChemistryMod;
import com.example.chemistry.data.Reactions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in rendered client regression: catalytic reaction, back, live unlock refresh and Escape. */
@EventBusSubscriber(modid=ChemistryMod.MODID,value=Dist.CLIENT)
public final class HandbookRegressionChecks {
    private static int ticks,stage;private static Screen previous;private static ChemistryHandbookScreen book;
    private static Object field(String name)throws ReflectiveOperationException{var f=ChemistryHandbookScreen.class.getDeclaredField(name);f.setAccessible(true);return f.get(book);}
    private static void call(String name,Class<?> type,Object value)throws ReflectiveOperationException{var m=ChemistryHandbookScreen.class.getDeclaredMethod(name,type);m.setAccessible(true);m.invoke(book,value);}
    private static void page(String expected)throws ReflectiveOperationException{if(!field("page").toString().equals(expected))throw new IllegalStateException("Handbook expected "+expected+" got "+field("page"));}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){var mc=Minecraft.getInstance();if(!Boolean.getBoolean("mchemistry.verifyHandbook")||stage==5||mc.getOverlay()!=null||++ticks%10!=0)return;
        try {
            if(stage==0){previous=mc.screen;book=new ChemistryHandbookScreen();mc.setScreen(book);var catalytic=Reactions.ALL.stream().filter(r->r.catalyst().equals("manganese_dioxide")&&r.reactants().stream().anyMatch(i->i.id().equals("hydrogen_peroxide"))).findFirst().orElseThrow();HandbookUnlockCache.set(List.of(catalytic.display()),List.of("liquid_hydrogen_peroxide"));call("openCategory",HandbookEntries.Category.class,HandbookEntries.Category.REACTIONS);call("openEntry",HandbookEntries.Entry.class,HandbookEntries.Entry.reaction(catalytic,net.minecraft.world.item.ItemStack.EMPTY));page("REACTION");stage=1;}
            else if(stage==1){double x=((Number)field("leftPos")).doubleValue()+40,y=((Number)field("topPos")).doubleValue()+23;book.mouseClicked(new MouseButtonEvent(x,y,new MouseButtonInfo(0,0)),false);page("LIST");book.tick();stage=2;}
            else if(stage==2){call("openCategory",HandbookEntries.Category.class,HandbookEntries.Category.LIQUIDS);var entry=HandbookEntries.entries(HandbookEntries.Category.LIQUIDS).stream().filter(e->"liquid_hydrogen_peroxide".equals(e.infoKey())).findFirst().orElseThrow();call("openEntry",HandbookEntries.Entry.class,entry);page("ENTRY");stage=3;}
            else if(stage==3){book.keyPressed(new KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE,0,0));if(mc.screen==book)throw new IllegalStateException("Escape did not close handbook");stage=4;}
            else if(stage==4){mc.setScreen(previous);ChemistryMod.LOGGER.info("Handbook rendered regression passed: catalytic reaction -> return -> reagent -> Escape; no empty entry page");stage=5;}
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Handbook regression failed",e);}
    }
    private HandbookRegressionChecks(){}
}
