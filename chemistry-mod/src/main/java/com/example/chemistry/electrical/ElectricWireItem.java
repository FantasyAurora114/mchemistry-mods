package com.example.chemistry.electrical;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public class ElectricWireItem extends Item {
    public ElectricWireItem(Properties properties) { super(properties); }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
        if(player.isShiftKeyDown()) {
            if(!level.isClientSide()) ElectricConnections.cancel(player,player.getItemInHand(hand));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
