package com.example.chemistry.blockentity;

import java.util.List;

import com.example.chemistry.api.goggles.ChemGoggleLines;
import com.example.chemistry.api.goggles.IChemGoggleInfo;
import com.example.chemistry.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** 试管架：5 个孔，每孔一支试管（可正置/倒置），从左到右填充。 */
public class TestTubeRackBlockEntity extends BlockEntity implements IChemGoggleInfo {

    public static final int SLOTS = 5;
    /** 五个孔位的中心 x（16 分之一格）。 */
    private static final double[] SLOT_X = {3.0, 5.5, 8.0, 10.5, 13.0};
    private final ItemStack[] tubes = new ItemStack[SLOTS];
    private final boolean[] inverted = new boolean[SLOTS];

    public TestTubeRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEST_TUBE_RACK.get(), pos, state);
        for (int i = 0; i < SLOTS; i++) {
            tubes[i] = ItemStack.EMPTY;
        }
    }

    public ItemStack getTube(int slot) {
        return tubes[slot];
    }

    public void setTube(int slot, ItemStack stack, boolean inv) {
        if (slot < 0 || slot >= SLOTS) {
            return;
        }
        tubes[slot] = stack.copy();
        inverted[slot] = inv;
        setChanged();
        sync();
    }

    public boolean isInverted(int slot) {
        return inverted[slot];
    }

    /** 该槽试管的命中盒（方块本地坐标）：正置在孔洞处，倒置在凸起柱上方。 */
    public AABB slotBox(int slot) {
        double cx = SLOT_X[slot] / 16.0;
        double hw = 1.1 / 16.0;
        if (inverted[slot]) {
            return new AABB(cx - hw, 2.2 / 16.0, 5.2 / 16.0,
                    cx + hw, 9.0 / 16.0, 7.4 / 16.0);
        }
        return new AABB(cx - hw, 0.5 / 16.0, 8.4 / 16.0,
                cx + hw, 7.0 / 16.0, 10.6 / 16.0);
    }

    /**
     * 玩家视线命中的最近槽位。occupiedOnly=true 时只看有试管的槽
     * （用于空手取出），否则任意槽（用于放入，调用方再检查是否为空）。
     */
    public int pickSlot(Player player, BlockPos pos, boolean occupiedOnly) {
        if (player == null) {
            return -1;
        }
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(6.0));
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < SLOTS; i++) {
            if (occupiedOnly && tubes[i].isEmpty()) {
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

    /** 第一个空孔，-1 表示已满。 */
    public int firstEmpty() {
        for (int i = 0; i < SLOTS; i++) {
            if (tubes[i].isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /** 最右边有试管的孔，-1 表示全空。 */
    public int rightmostOccupied() {
        for (int i = SLOTS - 1; i >= 0; i--) {
            if (!tubes[i].isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean addGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("试管架"));
        int n = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (!tubes[i].isEmpty()) {
                n++;
            }
        }
        tooltip.add(Component.literal("试管：" + n + "/" + SLOTS));
        for (int i = 0; i < SLOTS; i++) {
            if (!tubes[i].isEmpty()) {
                tooltip.add(Component.literal((inverted[i] ? "倒置" : "正置")
                        + "：" + tubes[i].getHoverName().getString()));
            }
        }
        return true;
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        java.util.List<ItemStack> list = new java.util.ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            list.add(tubes[i]);
        }
        output.store("tubes", ItemStack.OPTIONAL_CODEC.listOf(), list);
        java.util.List<Boolean> inv = new java.util.ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            inv.add(inverted[i]);
        }
        output.store("inverted", com.mojang.serialization.Codec.BOOL.listOf(), inv);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        java.util.List<ItemStack> list = input.read(
                "tubes", ItemStack.OPTIONAL_CODEC.listOf()).orElse(java.util.List.of());
        for (int i = 0; i < SLOTS; i++) {
            tubes[i] = i < list.size() ? list.get(i) : ItemStack.EMPTY;
        }
        java.util.List<Boolean> invList = input.read(
                "inverted", com.mojang.serialization.Codec.BOOL.listOf())
                .orElse(java.util.List.of());
        for (int i = 0; i < SLOTS; i++) {
            inverted[i] = i < invList.size() && invList.get(i);
        }
        // 兼容旧存档：单个 tube 字段。
        if (list.isEmpty()) {
            ItemStack old = input.read("tube", ItemStack.OPTIONAL_CODEC)
                    .orElse(ItemStack.EMPTY);
            if (!old.isEmpty()) {
                tubes[0] = old;
                inverted[0] = input.getBooleanOr("inverted", false);
            }
        }
    }
}
