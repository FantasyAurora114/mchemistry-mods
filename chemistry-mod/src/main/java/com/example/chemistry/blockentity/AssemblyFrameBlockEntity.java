package com.example.chemistry.blockentity;

import java.util.HashMap;
import java.util.Map;

import com.example.chemistry.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * 机架块（AssemblyFrame）：一个 2×3 通用框架，槽位 Map 存子件 ItemStack，
 * 一个 BER 把多个子件一起画出来。子件行为由 IAssemblyPart（第 4 步）提供。
 */
public class AssemblyFrameBlockEntity extends BlockEntity {

    /** 2×3 = 6 个槽位。 */
    public static final int SLOTS = 6;

    private final Map<Integer, ItemStack> slots = new HashMap<>();

    public AssemblyFrameBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASSEMBLY_FRAME.get(), pos, state);
    }

    public ItemStack getPart(int slot) {
        return slots.getOrDefault(slot, ItemStack.EMPTY);
    }

    public void setPart(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOTS) {
            return;
        }
        if (stack.isEmpty()) {
            slots.remove(slot);
        } else {
            slots.put(slot, stack.copy());
        }
        setChanged();
        sync();
    }

    public java.util.Set<Integer> occupiedSlots() {
        return slots.keySet();
    }

    public int partCount() {
        return slots.size();
    }

    /** 第一个空槽位，-1 表示已满。 */
    public int firstEmpty() {
        for (int i = 0; i < SLOTS; i++) {
            if (!slots.containsKey(i)) {
                return i;
            }
        }
        return -1;
    }

    /** 槽位命中盒（方块本地坐标，2×3 网格）。 */
    public AABB slotBox(int slot) {
        double[] p = SLOT_POS[slot];
        return new AABB((p[0] - 2.0) / 16.0, (p[1] - 2.0) / 16.0, (p[2] - 2.0) / 16.0,
                (p[0] + 2.0) / 16.0, (p[1] + 2.0) / 16.0, (p[2] + 2.0) / 16.0);
    }

    /** 玩家视线命中的最近槽位；occupiedOnly=true 只看有子件的槽。 */
    public int pickSlot(Player player, BlockPos pos, boolean occupiedOnly) {
        if (player == null) {
            return -1;
        }
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(6.0));
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < SLOTS; i++) {
            if (occupiedOnly && getPart(i).isEmpty()) {
                continue;
            }
            Optional<Vec3> hit = slotBox(i).move(pos).clip(from, to);
            if (hit.isPresent()) {
                double d = hit.get().distanceToSqr(from);
                if (d < bestD) {
                    bestD = d;
                    best = i;
                }
            }
        }
        return best;
    }

    /**
     * 射线命中的子件端口（虚拟选中框）所在的槽位；-1 表示没命中任何端口。
     * 端口世界坐标 = 槽位中心 + 端口模型偏移（按占位尺寸 0.25 缩放）。
     */
    public int pickPartPort(Level level, BlockPos pos, Player player) {
        if (player == null) {
            return -1;
        }
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(6.0));
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack part = getPart(i);
            if (part.isEmpty()) {
                continue;
            }
            for (com.example.chemistry.GlassConnector.GlassPort gp
                    : com.example.chemistry.GlassConnector.ports(part)) {
                double[] s = SLOT_POS[i];
                Vec3 p = new Vec3(
                        pos.getX() + (s[0] + (gp.x() - 8.0) * 0.25) / 16.0,
                        pos.getY() + (s[1] + (gp.y() - 8.0) * 0.25) / 16.0,
                        pos.getZ() + (s[2] + (gp.z() - 8.0) * 0.25) / 16.0);
                Optional<Vec3> hit = new AABB(p.x - 0.35, p.y - 0.35, p.z - 0.35,
                        p.x + 0.35, p.y + 0.35, p.z + 0.35).clip(from, to);
                if (hit.isPresent()) {
                    double d = hit.get().distanceToSqr(from);
                    if (d < bestD) {
                        bestD = d;
                        best = i;
                    }
                }
            }
        }
        return best;
    }

    /** 2×3 槽位中心（方块本地 16 分之一格）。 */
    private static final double[][] SLOT_POS = {
            {4.0, 12.0, 8.0}, {12.0, 12.0, 8.0},
            {4.0, 7.0, 8.0}, {12.0, 7.0, 8.0},
            {4.0, 2.0, 8.0}, {12.0, 2.0, 8.0}};

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < SLOTS; i++) {
            output.store("slot_" + i, ItemStack.OPTIONAL_CODEC, getPart(i));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        slots.clear();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = input.read("slot_" + i, ItemStack.OPTIONAL_CODEC)
                    .orElse(ItemStack.EMPTY);
            if (!s.isEmpty()) {
                slots.put(i, s);
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
