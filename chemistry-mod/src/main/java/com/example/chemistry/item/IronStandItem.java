package com.example.chemistry.item;

import com.example.chemistry.entity.IronStandEntity;
import com.example.chemistry.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** 铁架台物品：右键放下时生成技术性实体（本体），挂载物另行叠加。 */
public class IronStandItem extends BlockItem {

    public IronStandItem(Block block, Properties properties) {
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
        IronStandEntity entity = new IronStandEntity(ModEntities.IRON_STAND.get(), level);
        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        entity.setFacing(context.getHorizontalDirection());
        entity.restoreExtensions(context.getItemInHand());
        if (!level.noCollision(entity.virtualHitbox())) return InteractionResult.FAIL;
        level.addFreshEntity(entity);
        context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
