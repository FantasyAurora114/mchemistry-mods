package com.example.chemistry.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 化学手册：界面由客户端事件 {@code HandbookClientEvents} 打开，
 * 这里只返回 SUCCESS 让服务器播放挥动动画（通用类不得引用客户端类）。
 */
public class HandbookItem extends Item {

    public HandbookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return InteractionResult.SUCCESS;
    }
}
