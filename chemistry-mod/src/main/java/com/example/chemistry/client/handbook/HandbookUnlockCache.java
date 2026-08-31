package com.example.chemistry.client.handbook;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArraySet;

/** 客户端缓存：服务器返回的已解锁反应列表（工业合成塔解锁状态）。 */
public final class HandbookUnlockCache {

    private static final Set<String> UNLOCKED = new CopyOnWriteArraySet<>();
    private static final Set<String> DISCOVERED = new CopyOnWriteArraySet<>();
    private static final AtomicInteger VERSION = new AtomicInteger();

    private HandbookUnlockCache() {
    }

    public static void set(List<String> unlocked, List<String> discovered) {
        UNLOCKED.clear();
        UNLOCKED.addAll(unlocked);
        DISCOVERED.clear();
        DISCOVERED.addAll(discovered);
        VERSION.incrementAndGet();
    }

    public static boolean isUnlocked(String reactionDisplay) {
        return UNLOCKED.contains(reactionDisplay);
    }

    /** 该物质是否已发现（ELEMENTS/SOLIDS/LIQUIDS/GASES 分类据此过滤）。 */
    public static boolean isDiscovered(String substanceKey) {
        return DISCOVERED.contains(substanceKey);
    }

    /** 数据版本号：界面据此在收到服务器响应后刷新反应列表。 */
    public static int version() {
        return VERSION.get();
    }
}
