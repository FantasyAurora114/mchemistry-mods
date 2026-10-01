package com.example.chemistry.organic;

import com.example.chemistry.ReactionEngine;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.titration.LiquidTransfer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** An aliquot container preserving all liquid constituents, using the existing dropper appearance. */
public final class PhasePipetteItem extends LabVesselItem {
    public PhasePipetteItem(Properties properties){super(properties.stacksTo(1),25);}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){return InteractionResult.SUCCESS;}
    public static boolean interact(Player player,ItemStack pipette,ItemStack vessel){
        if(!(vessel.getItem() instanceof LabVesselItem)||vessel.getItem() instanceof PhasePipetteItem)return false;
        if(VesselHeating.isSealed(vessel)){player.displayClientMessage(Component.literal("请先打开容器"),true);return true;}
        boolean empty=LabVesselItem.getContents(pipette).isEmpty();double moved;
        if(empty){
            if(!LiquidPhases.read(vessel).modelled()){player.displayClientMessage(Component.literal("该混合物尚未建立分层模型，无法选择取液层"),true);return true;}
            moved=LiquidTransfer.sample(vessel,pipette,25,player.isShiftKeyDown());
        }else{moved=LiquidTransfer.pour(pipette,vessel,player.isShiftKeyDown()?1:5);if(moved>0)ReactionEngine.checkAndStart(vessel,player);}
        player.displayClientMessage(Component.literal(moved>0?String.format(java.util.Locale.ROOT,
                empty?"已取液 %.3f mL":"已转入 %.3f mL",moved):"没有可转移的液体或接收容量不足"),true);
        return true;
    }
}
