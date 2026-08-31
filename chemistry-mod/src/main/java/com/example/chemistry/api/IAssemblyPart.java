package com.example.chemistry.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 可装配子件：放在机架块槽位里，由机架路由交互。
 * <p>机架只负责"命中哪个槽"，具体行为交给子件（第 4 步）。
 */
public interface IAssemblyPart {

    /** 手持物品点击该部件时调用；返回 PASS 表示不消费，继续走机架默认逻辑。 */
    default InteractionResult interactPart(Level level, Player player,
            BlockPos rackPos, int slot, ItemStack held, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    /** 手持物品点击该部件某个端口（虚拟选中框命中）时调用。 */
    default InteractionResult interactPort(Level level, Player player,
            BlockPos rackPos, int slot, ItemStack held, InteractionHand hand) {
        return interactPart(level, player, rackPos, slot, held, hand);
    }

    /** 从机架取下时返回物品（可携带内容物）。 */
    default ItemStack takeOff(Level level, Player player, BlockPos rackPos, int slot,
            ItemStack stack) {
        return stack;
    }
}
