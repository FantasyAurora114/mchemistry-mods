package com.example.chemistry.item;

import com.example.chemistry.entity.RubberTubeEntity.Anchor;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Gas nozzle with a rubber tube already slipped on: the nozzle counts as one
 * end of the tube, so right-clicking a point lays a sagging tube from the
 * nozzle (held by the player) to that point.
 */
public class GasNozzleTubedItem extends Item {

    public GasNozzleTubedItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        Anchor point = Anchor.block(context.getClickedPos(), context.getClickedFace());
        RubberTubeItem.createTube(level, player, stack, Anchor.entity(player.getUUID()), point);
        return InteractionResult.SUCCESS;
    }
}
