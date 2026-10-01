package com.example.chemistry.utility;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
public class IECCableItem extends Item {
    public IECCableItem(Properties p){super(p);}
    @Override public InteractionResult use(Level l,Player p,InteractionHand h){if(l.isClientSide())return InteractionResult.SUCCESS;return UtilityConnections.click(p,h)?InteractionResult.SUCCESS:InteractionResult.PASS;}
    @Override public InteractionResult useOn(UseOnContext c){if(c.getLevel().isClientSide())return InteractionResult.SUCCESS;var p=c.getPlayer();if(p==null)return InteractionResult.PASS;if(UtilityConnections.click(p,c.getHand()))return InteractionResult.SUCCESS;return UtilityConnections.layPlug(p,c.getItemInHand(),c.getClickLocation().add(c.getClickedFace().getStepX()*.035,c.getClickedFace().getStepY()*.035,c.getClickedFace().getStepZ()*.035))?InteractionResult.SUCCESS:InteractionResult.PASS;}
}
