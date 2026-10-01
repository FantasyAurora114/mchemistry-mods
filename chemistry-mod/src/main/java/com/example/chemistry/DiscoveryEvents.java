package com.example.chemistry;

import com.example.chemistry.api.ReactionUnlocks;
import com.example.chemistry.transfer.BottleCodes;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 物质发现：玩家背包/手上持有某个物质物品时自动记录为"已发现"，
 * 化学手册里只有已发现的元素/固体/液体/气体才会显示。
 */
@EventBusSubscriber(modid = ChemistryMod.MODID)
public final class DiscoveryEvents {

    private static final String[] ELEMENT_FORM_SUFFIXES = {"_ingot", "_tube", "_dust", "_nugget"};

    private DiscoveryEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        // 每 32 tick（约 1.6 秒）扫描一次，避免每 tick 都遍历背包。
        if ((player.tickCount & 31) != 0) {
            return;
        }
        net.minecraft.world.entity.player.Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            discover(player, inv.getItem(i));
        }
        discover(player, player.getOffhandItem());
    }

    private static void discover(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        String key = stack.getItem() instanceof com.example.chemistry.item.GasCylinderItem cylinder
                ? "gas_collecting_bottle_" + cylinder.gas(stack) : normalize(BottleCodes.substanceKeyOf(stack));
        if (key != null) {
            ReactionUnlocks.discoverSubstance(player, key);
        }
    }

    /** 把物品路径规范化成手册 infoKey：元素锭/粉/粒/玻封 → element_x，散装 → solid_x。 */
    private static String normalize(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        if (key.startsWith("element_")) {
            for (String suffix : ELEMENT_FORM_SUFFIXES) {
                if (key.endsWith(suffix)) {
                    return key.substring(0, key.length() - suffix.length());
                }
            }
            return key;
        }
        if (key.startsWith("loose_")) {
            return "solid_" + key.substring("loose_".length());
        }
        if (key.startsWith("solid_") || key.startsWith("liquid_")
                || key.startsWith("gas_collecting_bottle_")) {
            return key;
        }
        return null;
    }
}
