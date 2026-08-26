package com.example.chemistry.item;

import com.example.chemistry.block.GasCollectingBottleBlock;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block item for the sealed EMPTY gas bottle: it always places with its glass
 * plate on (an empty collecting bottle is not open).
 */
public class SealedBottleBlockItem extends BlockItem {

    public SealedBottleBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(GasCollectingBottleBlock.HAS_PLATE, true);
    }
}
