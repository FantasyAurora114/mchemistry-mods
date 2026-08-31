package com.example.chemistry.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 标签：先在铁砧上命名，然后副手拿集气瓶/细口瓶/广口瓶、主手拿标签
 * 右键即可把标签贴到瓶子上（瓶子显示名变为标签文字，标签被消耗）。
 * 标签文字同时记录在物品 CUSTOM_DATA 的 chem_label 里，集气瓶放置/拾取
 * 时也能带过去。
 */
public class LabelItem extends Item {

    public static final String KEY_LABEL = "chem_label";

    public LabelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return attach(level, player, hand);
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
        return attach(context.getLevel(), player, context.getHand());
    }

    /** 把主手标签的名字贴到副手瓶子上。 */
    private static InteractionResult attach(Level level, Player player, InteractionHand hand) {
        ItemStack label = player.getItemInHand(hand);
        ItemStack bottle = player.getOffhandItem();
        if (!isLabelableBottle(bottle)) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.label.no_bottle"), true);
            return InteractionResult.SUCCESS;
        }
        Component name = label.get(DataComponents.CUSTOM_NAME);
        if (name == null || name.getString().isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.label.no_name"), true);
            return InteractionResult.SUCCESS;
        }
        // 瓶子显示名 = 标签文字；同时记录文字，放置/拾取时可恢复。
        bottle.set(DataComponents.CUSTOM_NAME, name.copy());
        CompoundTag tag = bottle.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(KEY_LABEL, name.getString());
        bottle.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        label.shrink(1);
        level.playSound(null, player.blockPosition(), SoundEvents.ITEM_FRAME_ADD_ITEM,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(
                Component.translatable("mchemistry.label.attached", name.getString()), true);
        return InteractionResult.SUCCESS;
    }

    /** 集气瓶 / 细口瓶（液体）/ 广口瓶（固体）都算可贴标签的瓶子。 */
    public static boolean isLabelableBottle(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return com.example.chemistry.transfer.BottleCodes.isGasBottle(stack)
                || com.example.chemistry.transfer.BottleCodes.isLiquidBottle(stack)
                || com.example.chemistry.transfer.BottleCodes.isSolidJar(stack);
    }
}
