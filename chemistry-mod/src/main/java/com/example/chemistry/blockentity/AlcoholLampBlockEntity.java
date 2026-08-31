package com.example.chemistry.blockentity;

import com.example.chemistry.block.AlcoholLampBlock;
import com.example.chemistry.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.mojang.serialization.Codec;

/**
 * Fuel tank of a placed alcohol lamp. A lit lamp burns 1 mL of alcohol every
 * 60 seconds; when empty it extinguishes itself.
 */
public class AlcoholLampBlockEntity extends BlockEntity {

    /** mL of alcohol consumed per tick (1 mL / 60 s at 20 TPS). */
    public static final double BURN_PER_TICK = 1.0 / 1200.0;
    public static final double CAPACITY = 150.0;

    private double fuel;

    public AlcoholLampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALCOHOL_LAMP.get(), pos, state);
    }

    public AlcoholLampBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
            BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public double getFuel() {
        return fuel;
    }

    public void setFuel(double fuel) {
        this.fuel = Math.max(0.0, Math.min(CAPACITY, fuel));
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void tickServer(Level level) {
        if (level.isClientSide()) {
            return;
        }
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof AlcoholLampBlock)
                || !state.getValue(AlcoholLampBlock.LIT)) {
            return;
        }
        if (fuel <= 0) {
            level.setBlock(worldPosition, state.setValue(AlcoholLampBlock.LIT, false), 3);
            return;
        }
        fuel = Math.max(0.0, fuel - BURN_PER_TICK);
        if (fuel <= 0) {
            level.setBlock(worldPosition, state.setValue(AlcoholLampBlock.LIT, false), 3);
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("fuel", Codec.DOUBLE, fuel);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fuel = input.read("fuel", Codec.DOUBLE).orElse(0.0);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
