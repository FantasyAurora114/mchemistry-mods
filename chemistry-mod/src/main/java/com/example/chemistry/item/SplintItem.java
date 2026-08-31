package com.example.chemistry.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** 木条 / 带火星的木条: used to test collected gases. The splint's use()
 *  returns SUCCESS when a gas jar is in the offhand so the right-click packet
 *  reaches the server (where the actual gas tests run). */
public class SplintItem extends Item {

    private final boolean glowing;

    public SplintItem(Properties properties, boolean glowing) {
        super(properties);
        this.glowing = glowing;
    }

    public boolean isGlowing() {
        return glowing;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (com.example.chemistry.transfer.BottleCodes.isGasBottle(player.getOffhandItem())) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
