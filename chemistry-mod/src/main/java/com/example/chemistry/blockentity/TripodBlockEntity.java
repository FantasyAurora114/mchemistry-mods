package com.example.chemistry.blockentity;

import java.util.List;

import com.example.chemistry.TemperatureSystem;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.api.goggles.ChemGoggleLines;
import com.example.chemistry.api.goggles.IChemGoggleInfo;
import com.example.chemistry.block.TripodBlock;
import com.example.chemistry.registry.ModBlockEntities;
import com.example.chemistry.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Stores the crucible / evaporating dish on the clay triangle; a lit lamp
 * underneath heats it for ignition reactions. */
public class TripodBlockEntity extends BlockEntity implements IChemGoggleInfo {

    private ItemStack vessel = ItemStack.EMPTY;

    public TripodBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRIPOD.get(), pos, state);
    }

    public ItemStack getVessel() {
        return vessel;
    }

    public void setVessel(ItemStack stack) {
        setChanged();
        if (level != null && !level.isClientSide()) {
            boolean hasVessel = !stack.isEmpty();
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(TripodBlock.HAS_VESSEL)
                    && state.getValue(TripodBlock.HAS_VESSEL) != hasVessel) {
                level.setBlock(worldPosition, state.setValue(TripodBlock.HAS_VESSEL, hasVessel), 3);
            }
            if (level.getBlockEntity(worldPosition) instanceof TripodBlockEntity be && be != this) {
                be.vessel = stack.copy();
                be.setChanged();
            } else {
                this.vessel = stack.copy();
            }
            level.sendBlockUpdated(worldPosition, state, state, 3);
        } else {
            this.vessel = stack.copy();
        }
    }

    /** Lit lamp + clay triangle: heat the vessel for ignition (灼烧, ~900 C). */
    public void tickServer(Level level) {
        if (level.isClientSide() || vessel.isEmpty()) {
            return;
        }
        BlockState state = getBlockState();
        boolean lamp = state.getValue(TripodBlock.HAS_LAMP) && state.getValue(TripodBlock.LAMP_LIT);
        if (lamp) {
            VesselHeating.heat(vessel, 900.0);
        } else {
            VesselHeating.cool(vessel);
        }
        VesselHeating.Outcome outcome = VesselHeating.tick(
                vessel, level, worldPosition, ItemStack.EMPTY, false,
                level.getNearestPlayer(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5, 8.0, false));
        if (outcome == VesselHeating.Outcome.POPPED) {
            net.minecraft.world.entity.item.ItemEntity stopper = new net.minecraft.world.entity.item.ItemEntity(
                    level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.7, worldPosition.getZ() + 0.5,
                    new ItemStack(ModItems.RUBBER_STOPPER_1_HOLE.get()));
            stopper.setDefaultPickUpDelay();
            level.addFreshEntity(stopper);
            level.playSound(null, worldPosition,
                    net.minecraft.sounds.SoundEvents.WOOL_BREAK,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
        } else if (outcome == VesselHeating.Outcome.CRACKED) {
            setVessel(ItemStack.EMPTY);
            level.levelEvent(2001, worldPosition,
                    net.minecraft.world.level.block.Block.getId(
                            net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState()));
            level.playSound(null, worldPosition,
                    net.minecraft.sounds.SoundEvents.GLASS_BREAK,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        setChanged();
    }

    @Override
    public boolean addGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {
        if (vessel.isEmpty()) {
            return false;
        }
        tooltip.add(vessel.getHoverName().copy());
        tooltip.add(ChemGoggleLines.temp(TemperatureSystem.getTemp(vessel)));
        ChemGoggleLines.appendContents(tooltip, vessel);
        ChemGoggleLines.appendPressure(tooltip, vessel);
        if (getBlockState().getValue(TripodBlock.HAS_LAMP)
                && getBlockState().getValue(TripodBlock.LAMP_LIT)) {
            tooltip.add(Component.literal("酒精灯灼烧中（约900°C）"));
        }
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("vessel", ItemStack.OPTIONAL_CODEC, vessel);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        vessel = input.read("vessel", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
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
