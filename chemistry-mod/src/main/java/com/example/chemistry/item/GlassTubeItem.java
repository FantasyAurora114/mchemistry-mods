package com.example.chemistry.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 玻璃导管（直管 / 90度管 / 90度长管）：继承原“导气嘴”的全部功能——
 * 可以插到集气瓶口、以 45° 插到水槽里作为气体出口/入口、作为橡胶管的
 * 端点，也能拿在手里当导气出口（配火源点燃气体）。
 */
public class GlassTubeItem extends Item {

    public GlassTubeItem(Properties properties) {
        super(properties);
    }

    /** 导管类型（含套着橡胶管的变体）：1=直管，2=90度管，3=90度长管。 */
    public static int tubeType(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.endsWith("_tubed")) {
            path = path.substring(0, path.length() - "_tubed".length());
        }
        return switch (path) {
            case "right_angle_glass_tube" -> 2;
            case "right_angle_glass_tube_long" -> 3;
            case "straight_glass_tube_long" -> 4;
            default -> 1;
        };
    }

    /** 三种普通玻璃导管。 */
    public static boolean isGlassTube(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.equals("straight_glass_tube")
                || path.equals("straight_glass_tube_long")
                || path.equals("right_angle_glass_tube")
                || path.equals("right_angle_glass_tube_long");
    }

    /** 三种套着橡胶管的玻璃导管。 */
    public static boolean isTubedGlassTube(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.equals("straight_glass_tube_tubed")
                || path.equals("straight_glass_tube_long_tubed")
                || path.equals("right_angle_glass_tube_tubed")
                || path.equals("right_angle_glass_tube_long_tubed");
    }
}
