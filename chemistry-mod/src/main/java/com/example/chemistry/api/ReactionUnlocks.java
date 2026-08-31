package com.example.chemistry.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import com.example.chemistry.data.Element;
import com.example.chemistry.data.Elements;
import com.example.chemistry.data.GasJars;
import com.example.chemistry.data.Liquids;
import com.example.chemistry.data.Reactions;
import com.example.chemistry.data.Solids;

/**
 * MCI 解锁机制：一个反应必须先在某位玩家的实验室容器中完成一次，工业
 * 合成塔才允许该玩家使用它进行制造；物质则须先被玩家获取/持有才会在
 * 化学手册中显示。解锁记录按玩家持久化保存在其 persistent data 中。
 */
public final class ReactionUnlocks {

    private static final String KEY_REACTIONS = "mci_unlocked_reactions";
    private static final String KEY_SUBSTANCES = "mci_discovered_substances";

    private ReactionUnlocks() {
    }

    /** 记录一次实验室完成，并提示玩家。重复完成不重复记录。 */
    public static void unlock(Player player, String reactionKey) {
        if (player == null || reactionKey == null || reactionKey.isEmpty()) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        ListTag list = data.getListOrEmpty(KEY_REACTIONS);
        StringTag entry = StringTag.valueOf(reactionKey);
        if (!list.contains(entry)) {
            list.add(entry);
            data.put(KEY_REACTIONS, list);
            player.displayClientMessage(
                    Component.translatable("mchemistry.reaction_unlocked",
                            Component.literal(reactionKey)), true);
        }
    }

    /** 该玩家是否已在实验室中完成过此反应。 */
    public static boolean isUnlocked(Player player, String reactionKey) {
        return player != null && reactionKey != null
                && player.getPersistentData().getListOrEmpty(KEY_REACTIONS)
                        .contains(StringTag.valueOf(reactionKey));
    }

    /** 该玩家已解锁的全部反应（display 文本列表）。 */
    public static java.util.List<String> all(Player player) {
        if (player == null) {
            return java.util.List.of();
        }
        ListTag list = player.getPersistentData().getListOrEmpty(KEY_REACTIONS);
        java.util.List<String> out = new java.util.ArrayList<>();
        for (net.minecraft.nbt.Tag t : list) {
            if (t instanceof StringTag s) {
                out.add(s.value());
            }
        }
        return out;
    }

    /** 记录一次物质发现（首次持有即记录，重复不重复记录）。 */
    public static void discoverSubstance(Player player, String substanceKey) {
        if (player == null || substanceKey == null || substanceKey.isEmpty()) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        ListTag list = data.getListOrEmpty(KEY_SUBSTANCES);
        StringTag entry = StringTag.valueOf(substanceKey);
        if (!list.contains(entry)) {
            list.add(entry);
            data.put(KEY_SUBSTANCES, list);
        }
    }

    /** 该玩家是否已发现该物质（key = handbook 的 infoKey）。 */
    public static boolean isSubstanceDiscovered(Player player, String substanceKey) {
        return player != null && substanceKey != null
                && player.getPersistentData().getListOrEmpty(KEY_SUBSTANCES)
                        .contains(StringTag.valueOf(substanceKey));
    }

    /** 该玩家已发现的全部物质 key。 */
    public static java.util.List<String> allSubstances(Player player) {
        if (player == null) {
            return java.util.List.of();
        }
        ListTag list = player.getPersistentData().getListOrEmpty(KEY_SUBSTANCES);
        java.util.List<String> out = new java.util.ArrayList<>();
        for (net.minecraft.nbt.Tag t : list) {
            if (t instanceof StringTag s) {
                out.add(s.value());
            }
        }
        return out;
    }

    /** 一键解锁全部：所有反应 + 所有元素/固体/液体/气体。 */
    public static void unlockAll(Player player) {
        if (player == null) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        ListTag reactions = data.getListOrEmpty(KEY_REACTIONS);
        for (Reactions.Reaction r : Reactions.ALL) {
            StringTag e = StringTag.valueOf(r.display());
            if (!reactions.contains(e)) {
                reactions.add(e);
            }
        }
        data.put(KEY_REACTIONS, reactions);

        ListTag substances = data.getListOrEmpty(KEY_SUBSTANCES);
        for (Element e : Elements.ALL) {
            addSubstance(substances, "element_" + e.id());
        }
        for (Solids.Solid s : Solids.ALL) {
            addSubstance(substances, "solid_" + s.id());
        }
        for (Liquids.Liquid l : Liquids.ALL) {
            addSubstance(substances, "liquid_" + l.id());
        }
        for (GasJars.GasJar g : GasJars.ALL) {
            addSubstance(substances, "gas_collecting_bottle_" + g.id());
        }
        data.put(KEY_SUBSTANCES, substances);
        player.displayClientMessage(
                Component.translatable("mchemistry.handbook.unlocked_all"), true);
    }

    /** 一键锁定全部：清空反应解锁与物质发现记录。 */
    public static void lockAll(Player player) {
        if (player == null) {
            return;
        }
        player.getPersistentData().remove(KEY_REACTIONS);
        player.getPersistentData().remove(KEY_SUBSTANCES);
        player.displayClientMessage(
                Component.translatable("mchemistry.handbook.locked_all"), true);
    }

    private static void addSubstance(ListTag list, String key) {
        StringTag e = StringTag.valueOf(key);
        if (!list.contains(e)) {
            list.add(e);
        }
    }
}
