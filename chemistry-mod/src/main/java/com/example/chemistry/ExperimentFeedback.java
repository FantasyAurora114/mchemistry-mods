package com.example.chemistry;

import com.example.chemistry.item.ChemGogglesItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Analytical messages require goggles; physical phenomena remain visible to everyone. */
public final class ExperimentFeedback {
    public static boolean send(Player player, Component message) {
        if (player == null || !ChemGogglesItem.isWearing(player)) return false;
        player.displayClientMessage(message,true);
        return true;
    }
    private ExperimentFeedback() {}
}
