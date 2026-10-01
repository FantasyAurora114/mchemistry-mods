package com.example.chemistry.client.handbook;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.network.ChemistryNetworking;
import com.example.chemistry.registry.ModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** 客户端：手持化学手册右键打开界面；并把解锁响应接回客户端缓存。 */
@EventBusSubscriber(modid = ChemistryMod.MODID, value = Dist.CLIENT)
public final class HandbookClientEvents {

    static {
        // 服务器→客户端的解锁列表响应，由通用网络类转发到这里（通用类不能引用客户端类）。
        ChemistryNetworking.setUnlockResponseHandler((packet, context) ->
                context.enqueueWork(() -> HandbookUnlockCache.set(
                        packet.unlocked(), packet.discovered())));
    }

    private HandbookClientEvents() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (!held.is(ModItems.HANDBOOK.get())) {
            return;
        }
        // 不取消事件：服务器照常收到使用包并播放挥动动画。
        // 1.21.10 单机下该事件可能在服务器线程触发，直接 setScreen 会导致
        // "setScreen called from non-game thread"（界面状态可能异常），切回游戏线程再打开。
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player == event.getEntity() && mc.screen == null)
                mc.setScreen(new ChemistryHandbookScreen());
        });
    }
}
