package com.example.chemistry.item;

import com.example.chemistry.block.AlcoholLampBlock;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Block item for the capped alcohol lamp: placement keeps the cap on. */
public class AlcoholLampItem extends BlockItem {

    private final boolean capped;

    public AlcoholLampItem(Block block, Properties properties, boolean capped) {
        super(block, properties);
        this.capped = capped;
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = this.getBlock().getStateForPlacement(context);
        if (state != null && this.canPlace(context, state)) {
            return state.setValue(AlcoholLampBlock.CAPPED, capped);
        }
        return null;
    }
}
