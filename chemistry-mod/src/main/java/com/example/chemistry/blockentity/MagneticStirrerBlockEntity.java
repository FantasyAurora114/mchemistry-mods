package com.example.chemistry.blockentity;

import java.util.List;

import com.example.chemistry.ReactionEngine;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** 磁力搅拌机方块实体：存放烧瓶和搅拌子，每 tick 触发反应检测（搅拌加速）。 */
public class MagneticStirrerBlockEntity extends BlockEntity implements IChemGoggleInfo {

    private ItemStack flask = ItemStack.EMPTY;
    private boolean stirBar;

    public MagneticStirrerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGNETIC_STIRRER.get(), pos, state);
    }

    public ItemStack getFlask() {
        return flask;
    }

    public void setFlask(ItemStack stack) {
        this.flask = stack.copy();
        setChanged();
        sync();
    }

    public boolean hasStirBar() {
        return stirBar;
    }

    public void setStirBar(boolean v) {
        this.stirBar = v;
        setChanged();
        sync();
    }

    public void tickServer(Level level) {
        if (level.isClientSide() || flask.isEmpty()) {
            return;
        }
        // 搅拌：有搅拌子时反应加速 2 倍（每 tick 推进 2 次）。
        Player nearest = level.getNearestPlayer(
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5, 8.0, false);
        int steps = stirBar ? 2 : 1;
        for (int i = 0; i < steps; i++) {
            ReactionEngine.checkAndStart(flask, nearest);
            ReactionEngine.tick(flask, nearest);
        }
        // 搅拌加速溶解（stirred=true）。
        com.example.chemistry.PhaseSystem.dissolveAndCrystallize(flask, stirBar);
        setChanged();
    }

    @Override
    public boolean addGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("磁力搅拌机"));
        if (flask.isEmpty()) {
            tooltip.add(Component.literal("烧瓶：未放入"));
            return true;
        }
        tooltip.add(Component.literal("烧瓶：" + flask.getHoverName().getString()));
        if (com.example.chemistry.VesselHeating.isThreeNeck(flask)) {
            StringBuilder necks = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                necks.append(com.example.chemistry.VesselHeating.neckHasStopper(flask, i) ? "●" : "○");
            }
            tooltip.add(Component.literal("瓶口（左中右）：" + necks));
        }
        tooltip.add(Component.literal("搅拌子：" + (stirBar ? "已放入（搅拌中）" : "未放入")));
        ChemGoggleLines.appendContents(tooltip, flask);
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
        output.store("flask", ItemStack.OPTIONAL_CODEC, flask);
        output.putBoolean("stir_bar", stirBar);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        flask = input.read("flask", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        stirBar = input.getBooleanOr("stir_bar", false);
    }
}
