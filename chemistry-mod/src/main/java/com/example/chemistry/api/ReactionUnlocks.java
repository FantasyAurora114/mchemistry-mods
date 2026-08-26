package com.example.chemistry.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * MCI 解锁机制：一个反应必须先在某位玩家的实验室容器中完成一次，工业
 * 合成塔才允许该玩家使用它进行制造。解锁记录按玩家持久化保存在其
 * persistent data 中（反应 key = 反应的 display 文本，唯一）。
 */
public final class ReactionUnlocks {

    private static final String KEY = "mci_unlocked_reactions";

    private ReactionUnlocks() {
    }

    /** 记录一次实验室完成，并提示玩家。重复完成不重复记录。 */
    public static void unlock(Player player, String reactionKey) {
        if (player == null || reactionKey == null || reactionKey.isEmpty()) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        ListTag list = data.getListOrEmpty(KEY);
        StringTag entry = StringTag.valueOf(reactionKey);
        if (!list.contains(entry)) {
            list.add(entry);
            data.put(KEY, list);
            player.displayClientMessage(
                    Component.translatable("mchemistry.reaction_unlocked",
                            Component.literal(reactionKey)), true);
        }
    }

    /** 该玩家是否已在实验室中完成过此反应。 */
    public static boolean isUnlocked(Player player, String reactionKey) {
        return player != null && reactionKey != null
                && player.getPersistentData().getListOrEmpty(KEY)
                        .contains(StringTag.valueOf(reactionKey));
    }
}
