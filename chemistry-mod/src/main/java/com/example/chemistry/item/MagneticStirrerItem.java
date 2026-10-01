package com.example.chemistry.item;

import com.example.chemistry.entity.MagneticStirrerEntity;
import com.example.chemistry.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** 磁力搅拌机物品：右键放下时生成技术性实体（带虚拟命中框），不再落方块。 */
public class MagneticStirrerItem extends BlockItem {

    public MagneticStirrerItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(pos).canBeReplaced()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        MagneticStirrerEntity entity =
                new MagneticStirrerEntity(ModEntities.MAGNETIC_STIRRER.get(), level);
        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        entity.setFacing(context.getHorizontalDirection().getOpposite());
        level.addFreshEntity(entity);
        context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
