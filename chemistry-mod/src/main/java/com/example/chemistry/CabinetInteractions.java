package com.example.chemistry;
import com.example.chemistry.block.ReagentCabinetBlock;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
@EventBusSubscriber(modid=ChemistryMod.MODID)
public final class CabinetInteractions {
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST) public static void click(PlayerInteractEvent.RightClickBlock e){
        if(!(e.getLevel().getBlockState(e.getPos()).getBlock() instanceof ReagentCabinetBlock))return;
        if(e.getSide().isClient())return;
        if(e.getEntity().isShiftKeyDown())ReagentCabinetBlock.setOpen(e.getLevel(),e.getPos(),false);
        else {var be=ReagentCabinetBlock.inventory(e.getLevel(),e.getPos());if(be!=null){ReagentCabinetBlock.setOpen(e.getLevel(),e.getPos(),true);e.getEntity().openMenu(be);}}
        e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
    }
}
