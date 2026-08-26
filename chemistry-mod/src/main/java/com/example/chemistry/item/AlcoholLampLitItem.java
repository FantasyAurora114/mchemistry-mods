package com.example.chemistry.item;

import com.example.chemistry.block.AlcoholLampBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * The lit alcohol lamp: sneak-right-click places a lit lamp, otherwise it
 * works like flint & steel (the placed-lamp special case is handled by the
 * lamp block, which turns it into fire).
 */
public class AlcoholLampLitItem extends BlockItem {

    public AlcoholLampLitItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = this.getBlock().getStateForPlacement(context);
        if (state != null && this.canPlace(context, state)) {
            return state.setValue(AlcoholLampBlock.LIT, true);
        }
        return null;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return super.useOn(context);
        }
        Level level = context.getLevel();
        BlockPos firePos = context.getClickedPos().relative(context.getClickedFace());
        if (BaseFireBlock.canBePlacedAt(level, firePos, context.getHorizontalDirection())) {
            if (!level.isClientSide()) {
                level.playSound(null, firePos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS,
                        1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
                level.setBlock(firePos, BaseFireBlock.getState(level, firePos), 11);
                level.gameEvent(player, GameEvent.BLOCK_PLACE, context.getClickedPos());
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }
}
