package com.example.chemistry.api.goggles;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Implement this interface on a {@link BlockEntity} (reaction container /
 * lab apparatus) that should show information through the chemist's goggles
 * overlay when the player is wearing them and looking at the block.
 */
public interface IChemGoggleInfo {

    /**
     * @return {@code true} when at least one line was added and the overlay
     *         should be displayed, {@code false} otherwise.
     */
    boolean addGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking);
}
