package com.example.chemistry.filtration;
import com.example.chemistry.item.LabVesselItem;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
public class UsedFilterPaperItem extends LabVesselItem {
    public UsedFilterPaperItem(Properties p){super(p.stacksTo(1),25);}
    @Override public InteractionResult use(Level level,Player p,InteractionHand hand){
        var target=p.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
        if(!(target.getItem() instanceof LabVesselItem)||target.getItem() instanceof UsedFilterPaperItem)return InteractionResult.PASS;
        if(!level.isClientSide())Filtration.pour(p.getItemInHand(hand),target,Double.MAX_VALUE);
        return InteractionResult.SUCCESS;
    }
}
