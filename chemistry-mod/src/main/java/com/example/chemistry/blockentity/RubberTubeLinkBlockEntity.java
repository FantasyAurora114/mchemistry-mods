package com.example.chemistry.blockentity;

import com.example.chemistry.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Stores the two endpoints (world coordinates of the anchor face centres) of
 * the straight rubber tube that passes through this cell.
 */
public class RubberTubeLinkBlockEntity extends BlockEntity {

    private double ax;
    private double ay;
    private double az;
    private double bx;
    private double by;
    private double bz;

    public RubberTubeLinkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUBBER_TUBE_LINK.get(), pos, state);
    }

    public double getAx() {
        return ax;
    }

    public double getAy() {
        return ay;
    }

    public double getAz() {
        return az;
    }

    public double getBx() {
        return bx;
    }

    public double getBy() {
        return by;
    }

    public double getBz() {
        return bz;
    }

    public boolean hasEndpoints() {
        return ax != bx || ay != by || az != bz;
    }

    public void setEndpoints(double ax, double ay, double az, double bx, double by, double bz) {
        this.ax = ax;
        this.ay = ay;
        this.az = az;
        this.bx = bx;
        this.by = by;
        this.bz = bz;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** The client needs the endpoints to draw its slice of the tube. */
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
        output.putDouble("ax", ax);
        output.putDouble("ay", ay);
        output.putDouble("az", az);
        output.putDouble("bx", bx);
        output.putDouble("by", by);
        output.putDouble("bz", bz);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ax = input.getDoubleOr("ax", 0.0);
        ay = input.getDoubleOr("ay", 0.0);
        az = input.getDoubleOr("az", 0.0);
        bx = input.getDoubleOr("bx", 0.0);
        by = input.getDoubleOr("by", 0.0);
        bz = input.getDoubleOr("bz", 0.0);
    }
}
