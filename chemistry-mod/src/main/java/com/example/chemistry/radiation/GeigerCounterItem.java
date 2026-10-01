package com.example.chemistry.radiation;
import net.minecraft.world.item.Item;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
public final class GeigerCounterItem extends Item {
 public GeigerCounterItem(Properties p){super(p);}
 @Override public InteractionResult use(Level l,Player p,InteractionHand h){if(!l.isClientSide())p.displayClientMessage(net.minecraft.network.chat.Component.literal(RadiationSystem.reading(p)),false);return InteractionResult.SUCCESS;}
}
