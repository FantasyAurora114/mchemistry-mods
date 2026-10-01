package com.example.chemistry.client;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.block.ReagentCabinetBlock;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModBlocks;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.transfer.BottleCodes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Explicit developer visual fixture. Operates only in a disposable named world copy. */
@EventBusSubscriber(modid=ChemistryMod.MODID,value=Dist.CLIENT)
public final class LaboratoryVisualChecks {
    private static boolean opening,placed;
    private static int ticks;
    @SubscribeEvent
    public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        String name=System.getProperty("mchemistry.visualTestWorld","");
        if(!name.startsWith("mchemistry-visual-check-"))return;
        var mc=Minecraft.getInstance();
        if(mc.getOverlay()!=null)return;
        if(!opening&&mc.screen instanceof TitleScreen) {
            opening=true;mc.createWorldOpenFlows().openWorld(name,()->{throw new IllegalStateException("Visual fixture world failed to open");});
        }
        if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
        if(!placed) {
            placed=true;var server=mc.getSingleplayerServer();
            server.execute(()->{
                var level=server.overworld();level.setDayTime(6000);
                for(int x=-2;x<=6;x++)for(int z=-6;z<=3;z++) {
                    level.setBlock(new BlockPos(x,79,z),Blocks.SMOOTH_QUARTZ.defaultBlockState(),3);
                    for(int y=80;y<=84;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
                }
                for(int x=-2;x<=6;x++)for(int y=80;y<=84;y++)level.setBlock(new BlockPos(x,y,3),Blocks.WHITE_CONCRETE.defaultBlockState(),3);
                for(int x:new int[]{0,2}) {
                    var block=x==0?ModBlocks.TALL_REAGENT_CABINET.get():ModBlocks.BASE_REAGENT_CABINET.get();
                    var pos=new BlockPos(x,80,0);var state=block.defaultBlockState();
                    level.setBlock(pos,state,3);block.setPlacedBy(level,pos,state,null,new ItemStack(block));
                    ReagentCabinetBlock.setOpen(level,pos,x==2);
                    var cabinet=ReagentCabinetBlock.inventory(level,pos);
                    for(int slot=0;slot<cabinet.getContainerSize();slot++) {
                        ItemStack item;
                        if(slot%3==0){item=new ItemStack(ModItems.LIQUID_BOTTLE.get());BottleCodes.setLiquid(item,"copper_sulfate_solution",true,150);}
                        else if(slot%3==1){item=new ItemStack(ModItems.SOLID_JAR.get());BottleCodes.setSolid(item,"sodium_chloride",true);}
                        else {item=new ItemStack(ModItems.ERLENMEYER_FLASK.get());LabVesselItem.addMass(item,"liquid","water",100);}
                        cabinet.setItem(slot,item);
                    }
                }
                for(var player:server.getPlayerList().getPlayers()) {
                    player.setGameMode(GameType.CREATIVE);
                    player.teleportTo(level,1.5,80,-4.0,java.util.Set.of(),0,2,false);
                    for(int slot=0;slot<3;slot++) {
                        String id=new String[]{"beaker_100ml","beaker_medium","beaker_tall"}[slot];
                        var item=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry",id)));
                        LabVesselItem.addMass(item,"liquid","water",250);player.getInventory().setItem(slot,item);
                    }
                }
            });
        }
        ticks++;
        if(ticks==120)Screenshot.grab(mc.gameDirectory,"sept28-cabinet-water.png",mc.getMainRenderTarget(),1,
                message->ChemistryMod.LOGGER.info("Visual fixture screenshot: {}",message.getString()));
        if(ticks==180) {
            var server=mc.getSingleplayerServer();server.execute(()->{
                for(var p:server.getPlayerList().getPlayers())p.teleportTo(server.overworld(),.5,80,-1.8,java.util.Set.of(),0,0,false);
                ReagentCabinetBlock.setOpen(server.overworld(),new BlockPos(0,80,0),true);
            });
        }
        if(ticks==260)Screenshot.grab(mc.gameDirectory,"sept28-cabinet-open.png",mc.getMainRenderTarget(),1,
                message->ChemistryMod.LOGGER.info("Visual fixture screenshot: {}",message.getString()));
        if(ticks==310)mc.stop();
    }
    private LaboratoryVisualChecks() {}
}
