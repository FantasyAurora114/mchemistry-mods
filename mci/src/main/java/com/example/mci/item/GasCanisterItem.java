/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.example.chemistry.data.GasJars
 *  com.example.chemistry.data.GasJars$GasJar
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.context.UseOnContext
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 */
package com.example.mci.item;

import com.example.mci.blockentity.SynthesisTowerBlockEntity;
import com.example.chemistry.data.GasJars;
import com.example.mci.item.GasCanisterHelper;
import com.example.mci.storage.ChemGasTank;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class GasCanisterItem
extends Item {
    public GasCanisterItem(Item.Properties properties) {
        super(properties);
    }

    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide() || player == null) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SynthesisTowerBlockEntity)) {
            return InteractionResult.PASS;
        }
        SynthesisTowerBlockEntity tower = (SynthesisTowerBlockEntity)be;
        ItemStack stack = context.getItemInHand();
        ChemGasTank tank = tower.getGasTank();
        String canisterGas = GasCanisterHelper.getGasId(stack);
        long canisterAmount = GasCanisterHelper.getAmount(stack);
        if (canisterGas != null && canisterAmount > 0L) {
            long accepted = tank.addWithPurity(canisterGas, canisterAmount, GasCanisterHelper.getPurity(stack));
            if (accepted > 0L) {
                GasCanisterHelper.drain(stack, accepted);
                player.displayClientMessage((Component)Component.literal((String)("\u5df2\u5411\u5408\u6210\u5854\u6c14\u4f53\u7f50\u6ce8\u5165 " + accepted + " mB " + GasCanisterItem.gasName(canisterGas))), true);
            } else {
                player.displayClientMessage((Component)Component.literal((String)"\u5408\u6210\u5854\u6c14\u4f53\u7f50\u5df2\u6ee1\u6216\u88c5\u6709\u5176\u4ed6\u6c14\u4f53"), true);
            }
        } else if (!tank.isEmpty()) {
            String gasId = tank.getGasId();
            long extracted = tank.extract(Math.min(16000L, tank.getAmount()), false);
            if (extracted > 0L) {
                GasCanisterHelper.fill(stack, gasId, extracted, tank.getPurity());
                player.displayClientMessage((Component)Component.literal((String)("\u5df2\u4ece\u5408\u6210\u5854\u6c14\u4f53\u7f50\u88c5\u5165 " + extracted + " mB " + GasCanisterItem.gasName(gasId))), true);
            }
        } else {
            player.displayClientMessage((Component)Component.literal((String)"\u5408\u6210\u5854\u6c14\u4f53\u7f50\u662f\u7a7a\u7684"), true);
        }
        tower.setChanged();
        return InteractionResult.SUCCESS;
    }

    private static String gasName(String gasId) {
        for (GasJars.GasJar gas : GasJars.ALL) {
            if (!gas.id().equals(gasId)) continue;
            return gas.chinese();
        }
        return gasId;
    }
}
