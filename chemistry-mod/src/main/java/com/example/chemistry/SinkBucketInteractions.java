package com.example.chemistry;
import com.example.chemistry.block.LaboratoryBenchBlock;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
/** Capture water buckets even while sneaking, before vanilla pours them beside the bench. */
@EventBusSubscriber(modid=ChemistryMod.MODID)
public final class SinkBucketInteractions {
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void fill(PlayerInteractEvent.RightClickBlock event){
        var player=event.getEntity();var level=event.getLevel();var pos=event.getPos();
        if(LaboratoryBenchBlock.fillFromBucket(player.getItemInHand(event.getHand()),level.getBlockState(pos),level,pos,player,event.getHand())){
            event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
    private SinkBucketInteractions(){}
}
