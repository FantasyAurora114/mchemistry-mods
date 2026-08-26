package com.example.chemistry.blockentity;

import java.util.List;

import com.example.chemistry.api.goggles.ChemGoggleLines;
import com.example.chemistry.api.goggles.IChemGoggleInfo;
import com.example.chemistry.block.GasWashingBottleBlock;
import com.example.chemistry.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.mojang.serialization.Codec;

/** Stores the washing liquid inside a placed gas washing bottle (洗气瓶). */
public class GasWashingBottleBlockEntity extends BlockEntity implements IChemGoggleInfo {

    public static final double CAPACITY = 250.0;

    private String liquidId = "";
    private double ml = 0;

    public GasWashingBottleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_WASHING_BOTTLE.get(), pos, state);
    }

    public String getLiquidId() {
        return liquidId;
    }

    public double getMl() {
        return ml;
    }

    public boolean isEmpty() {
        return liquidId.isEmpty() || ml <= 0;
    }

    /** Add liquid; returns true when it fit. */
    public boolean addLiquid(String id, double amountMl) {
        if (ml + amountMl > CAPACITY + 0.001) {
            return false;
        }
        if (liquidId.isEmpty()) {
            liquidId = id;
        } else if (!liquidId.equals(id)) {
            return false;
        }
        ml += amountMl;
        setChanged();
        sync();
        return true;
    }

    public void clear() {
        liquidId = "";
        ml = 0;
        setChanged();
        sync();
    }

    @Override
    public boolean addGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("洗气瓶"));
        if (isEmpty()) {
            tooltip.add(Component.literal("洗涤液：空"));
        } else {
            tooltip.add(Component.literal("洗涤液：" + ChemGoggleLines.liquidName(liquidId)
                    + " " + String.format("%.0f", ml) + "mL"));
        }
        if (level != null) {
            var state = level.getBlockState(worldPosition);
            boolean stopper = state.getValue(GasWashingBottleBlock.STOPPER);
            int tubes = state.getValue(GasWashingBottleBlock.TUBES);
            tooltip.add(Component.literal(stopper ? "瓶塞：已塞上" : "瓶塞：已拔出"));
            if (tubes > 0) {
                tooltip.add(Component.literal("导管：" + tubes + "根"));
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("liquid", Codec.STRING, liquidId);
        output.store("ml", Codec.DOUBLE, ml);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        liquidId = input.read("liquid", Codec.STRING).orElse("");
        ml = input.read("ml", Codec.DOUBLE).orElse(0.0);
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
