package com.example.chemistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.chemistry.VesselHeating;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * 玻璃仪器的接口/插头系统。
 * <p>接口（SOCKET）可以接插头（PLUG），插头可以插入接口——
 * 例如蒸馏头：顶端是接口（可插温度计），底部和侧管是插头
 * （底部插进反应容器瓶口，侧管插进冷凝管接口）。
 * 烧瓶 / 锥形瓶 / 三颈烧瓶的瓶口都是接口。
 */
public final class GlassConnector {

    public enum ConnectorType {
        SOCKET,   // 接口（母口，接受插头）
        PLUG      // 插头（公口，插入接口）
    }

    /** 仪器模型帧里的一个连接点（16 分之一格坐标）。 */
    public record GlassPort(ConnectorType type, double x, double y, double z) {
    }

    private static final Map<String, List<GlassPort>> PORTS = new HashMap<>();

    static {
        // 烧瓶：瓶口是接口。
        PORTS.put("round_bottom_flask", List.of(new GlassPort(ConnectorType.SOCKET, 8.5, 10.05, 8.5)));
        PORTS.put("erlenmeyer_flask", List.of(new GlassPort(ConnectorType.SOCKET, 8.5, 9, 8.5)));
        for (String beaker : new String[] {"beaker_50ml", "beaker_100ml", "beaker_500ml", "beaker_1000ml"}) {
            PORTS.put(beaker, List.of(new GlassPort(ConnectorType.SOCKET, 8.5, 7.8, 8.5)));
        }
        PORTS.put("beaker_medium", List.of(new GlassPort(ConnectorType.SOCKET,8.5,10.5,8.5)));
        PORTS.put("beaker_tall", List.of(new GlassPort(ConnectorType.SOCKET,8.5,14,8.5)));
        // 磨口烧瓶 / 磨口锥形瓶 / 磨口平底烧瓶：磨口接口，可与玻璃仪器相连。
        PORTS.put("ground_glass_flask", List.of(new GlassPort(ConnectorType.SOCKET, 8.5, 10.05, 8.5)));
        PORTS.put("ground_glass_erlenmeyer", List.of(new GlassPort(ConnectorType.SOCKET, 8.5, 10, 8.5)));
        PORTS.put("flat_bottom_flask", List.of(new GlassPort(ConnectorType.SOCKET, 8.5, 10, 8.5)));
        PORTS.put("ground_glass_flat_bottom_flask", List.of(new GlassPort(ConnectorType.SOCKET, 8.5, 10, 8.5)));
        // 三颈烧瓶：三个瓶口都是接口（中间 + 两侧）。
        PORTS.put("three_neck_flask", List.of(
                new GlassPort(ConnectorType.SOCKET, ThreeNeckGeometry.PORTS[1][0], ThreeNeckGeometry.PORTS[1][1], 8.5),
                new GlassPort(ConnectorType.SOCKET, ThreeNeckGeometry.PORTS[0][0], ThreeNeckGeometry.PORTS[0][1], 8.5),
                new GlassPort(ConnectorType.SOCKET, ThreeNeckGeometry.PORTS[2][0], ThreeNeckGeometry.PORTS[2][1], 8.5)));
        PORTS.put("thermometer_sleeve", List.of(new GlassPort(ConnectorType.PLUG, 0, 0, 0)));
        // 蒸馏头：顶端接口、底部插头、侧管插头。
        PORTS.put("distillation_head", List.of(
                new GlassPort(ConnectorType.SOCKET, 7.0, 15.25, 8.0),
                new GlassPort(ConnectorType.PLUG, 7.0, 0.75, 8.0),
                new GlassPort(ConnectorType.PLUG, 12.73, 4.77, 8.0)));
        // 新模型：靠蒸馏头的一端为宽口接口，远端为细磨口接头。
        PORTS.put("straight_condenser", List.of(
                new GlassPort(ConnectorType.SOCKET, 8.0, 16.0, 8.0),
                new GlassPort(ConnectorType.PLUG, 8.0, 0.0, 8.0)));
        // 牛角管：入口接远端细磨口，出口面向接收瓶。
        PORTS.put("receiver_adapter_bent", List.of(
                new GlassPort(ConnectorType.SOCKET, 5.414214, 13.863961, 8.0),
                new GlassPort(ConnectorType.SOCKET, 9.5, 0.75, 8.0)));
        PORTS.put("receiver_adapter_straight", List.of(
                new GlassPort(ConnectorType.SOCKET, 8.5, 8.0, 0.0),
                new GlassPort(ConnectorType.SOCKET, 8.5, 8.0, 9.0)));
    }

    private GlassConnector() {
    }

    /** 某仪器的全部连接点；未知仪器返回空列表。 */
    public static List<GlassPort> ports(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }
        return PORTS.getOrDefault(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath(), List.of());
    }

    /** 该容器是否为"有口的反应容器"（各种烧瓶/三颈瓶/磨口仪器；普通锥形瓶、烧杯不可直连）。 */
    public static boolean isOpenMouthVessel(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.equals("three_neck_flask")
                || path.equals("ground_glass_flask") || path.equals("ground_glass_erlenmeyer")
                || path.equals("ground_glass_flat_bottom_flask");
    }

    /** 仪器是否带某种类型的连接点。 */
    public static boolean hasType(ItemStack stack, ConnectorType type) {
        return ports(stack).stream().anyMatch(p -> p.type() == type);
    }

    /** 是否有可用的插头（用于"把仪器插上去"的判定）。 */
    public static boolean hasFreePlug(ItemStack stack) {
        return hasType(stack, ConnectorType.PLUG);
    }

    /** 三颈烧瓶是否还有空闲瓶口（接口）。 */
    public static boolean threeNeckHasFreeSocket(ItemStack flask) {
        if (!VesselHeating.isThreeNeck(flask)) {
            return true;
        }
        for (int i = 0; i < 3; i++) {
            if (!VesselHeating.neckHasStopper(flask, i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 该铁架台上容器的瓶口接口是否已被占用（例如蒸馏头底部插头插在瓶口上）。
     * 端口被占用时禁止再塞橡胶塞 / 玻璃塞。
     */
    public static boolean isMouthPortOccupied(
            com.example.chemistry.blockentity.IronStandBlockEntity stand) {
        return stand != null && stand.hasDistillationHead();
    }

    /** 调试/描述用：连接点数量。 */
    public static int portCount(ItemStack stack) {
        return ports(stack).size();
    }

    /**
     * 射线选中端口：命中哪个端口的虚拟选中框就返回哪个（最近的一个）。
     * 与"橡胶管任意一段被剪刀选中"是同一原理——把端口当成一个可点的虚框。
     */
    @Nullable
    public static com.example.chemistry.entity.RubberTubeEntity.Port pickPort(
            Level level, Player player, java.util.Collection<com.example.chemistry.entity.RubberTubeEntity.Port> ports) {
        if (player == null) {
            return null;
        }
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(6.0));
        com.example.chemistry.entity.RubberTubeEntity.Port best = null;
        double bestD = Double.MAX_VALUE;
        for (com.example.chemistry.entity.RubberTubeEntity.Port p : ports) {
            if (p == null) {
                continue;
            }
            AABB box = p.selectionBox(level);
            if (box == null) {
                continue;
            }
            Optional<Vec3> hit = box.clip(from, to);
            if (hit.isPresent()) {
                double d = hit.get().distanceToSqr(from);
                if (d < bestD) {
                    bestD = d;
                    best = p;
                }
            }
        }
        return best;
    }
}
