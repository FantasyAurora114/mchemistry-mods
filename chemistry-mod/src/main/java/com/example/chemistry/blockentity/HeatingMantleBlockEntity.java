package com.example.chemistry.blockentity;

import java.util.List;

import com.example.chemistry.ReactionEngine;
import com.example.chemistry.TemperatureSystem;
import com.example.chemistry.VesselHeating;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.mojang.serialization.Codec;

/** 加热套方块实体：存放一个烧瓶，按设定温度缓慢加热。 */
public class HeatingMantleBlockEntity extends BlockEntity implements IChemGoggleInfo {

    public static final int MIN_TEMP = 20;
    public static final int MAX_TEMP = 450;
    public static final int TEMP_STEP = 10;

    private ItemStack flask = ItemStack.EMPTY;
    private int setTemp = 100;

    public HeatingMantleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATING_MANTLE.get(), pos, state);
    }

    public ItemStack getFlask() {
        return flask;
    }

    public void setFlask(ItemStack stack) {
        this.flask = stack.copy();
        setChanged();
        sync();
    }

    public int getSetTemp() {
        return setTemp;
    }

    /** 上下键调节设定温度（±10°C，限制在 MIN..MAX）。 */
    public void adjustSetTemp(int delta) {
        setTemp = Math.max(MIN_TEMP, Math.min(MAX_TEMP, setTemp + delta));
        setChanged();
        sync();
    }

    public void tickServer(Level level) {
        if (level.isClientSide() || flask.isEmpty()) {
            return;
        }
        // 升温与酒精灯相同（5°C/s 封顶），降温与室温冷却相同（每 2 秒 1°C）。
        if (!VesselHeating.isTempLocked(flask)) {
            double current = TemperatureSystem.getTemp(flask);
            if (current < setTemp - 0.5) {
                VesselHeating.heatSlow(flask, setTemp);
            } else if (current > setTemp + 0.5) {
                TemperatureSystem.setTemp(flask,
                        Math.max(setTemp, current - 1.0 / 40.0));
            }
        }
        ReactionEngine.checkAndStart(flask, level.getNearestPlayer(
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5, 8.0, false));
        ReactionEngine.tick(flask, null);
        setChanged();
        // 定期把烧瓶温度同步给客户端（护目镜才能看到温度变化）。
        if (level.getGameTime() % 10 == 0) {
            sync();
        }
    }

    @Override
    public boolean addGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("加热套"));
        tooltip.add(Component.literal("设定温度：" + setTemp + "°C"
                + "（上下键调节）"));
        if (flask.isEmpty()) {
            tooltip.add(Component.literal("烧瓶：未放入"));
        } else {
            tooltip.add(Component.literal("烧瓶：" + flask.getHoverName().getString()));
            if (VesselHeating.isThreeNeck(flask)) {
                StringBuilder necks = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    necks.append(VesselHeating.neckHasStopper(flask, i) ? "●" : "○");
                }
                tooltip.add(Component.literal("瓶口（左中右）：" + necks));
            }
            tooltip.add(ChemGoggleLines.temp(TemperatureSystem.getTemp(flask)));
            ChemGoggleLines.appendContents(tooltip, flask);
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
        output.store("flask", net.minecraft.world.item.ItemStack.OPTIONAL_CODEC, flask);
        output.store("set_temp", Codec.INT, setTemp);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        flask = input.read("flask", net.minecraft.world.item.ItemStack.OPTIONAL_CODEC)
                .orElse(ItemStack.EMPTY);
        setTemp = input.read("set_temp", Codec.INT).orElse(100);
        setTemp = Math.max(MIN_TEMP, Math.min(MAX_TEMP, setTemp));
    }
}
