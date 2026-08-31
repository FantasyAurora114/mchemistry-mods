package com.example.chemistry.blockentity;

import java.util.List;

import com.example.chemistry.TemperatureSystem;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.api.goggles.ChemGoggleLines;
import com.example.chemistry.api.goggles.IChemGoggleInfo;
import com.example.chemistry.registry.ModBlockEntities;
import com.example.chemistry.registry.ModItems;

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

/** Stores the vessel placed on the ground and advances its reactions. */
public class PlacedVesselBlockEntity extends BlockEntity implements IChemGoggleInfo {

    private ItemStack vessel = ItemStack.EMPTY;
    private ItemStack attached1 = ItemStack.EMPTY;
    private ItemStack attached2 = ItemStack.EMPTY;

    public PlacedVesselBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLACED_VESSEL.get(), pos, state);
    }

    public ItemStack getVessel() {
        return vessel;
    }

    public void setVessel(ItemStack stack) {
        this.vessel = stack.copy();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public ItemStack getAttached1() {
        return attached1;
    }

    public ItemStack getAttached2() {
        return attached2;
    }

    public void setAttached1(ItemStack stack) {
        this.attached1 = stack.copy();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setAttached2(ItemStack stack) {
        this.attached2 = stack.copy();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void tickServer(Level level) {
        if (level.isClientSide() || vessel.isEmpty()) {
            return;
        }
        if (VesselHeating.isTempLocked(vessel)) {
            // Temperature pinned by the I key: neither heat nor cool.
        } else {
            if (VesselHeating.blowtorchBelow(level, worldPosition)) {
                VesselHeating.heatFast(vessel, VesselHeating.BLOWTORCH_TEMP);
            } else if (VesselHeating.lampBelow(level, worldPosition)) {
                VesselHeating.heatSlow(vessel, VesselHeating.LAMP_TEMP);
            } else {
                VesselHeating.coolGradual(vessel);
            }
        }
        VesselHeating.Outcome outcome = VesselHeating.tick(vessel, level, worldPosition,
                ItemStack.EMPTY, false,
                !attached1.isEmpty() || !attached2.isEmpty(),
                level.getNearestPlayer(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5, 8.0, false));
        if (outcome == VesselHeating.Outcome.POPPED) {
            int holes = VesselHeating.getStopperHoles(vessel);
            net.minecraft.world.entity.item.ItemEntity stopper = new net.minecraft.world.entity.item.ItemEntity(
                    level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.7, worldPosition.getZ() + 0.5,
                    new ItemStack(ModItems.stopperForHoles(holes)));
            stopper.setDefaultPickUpDelay();
            level.addFreshEntity(stopper);
            for (ItemStack attached : new ItemStack[] {attached2, attached1}) {
                if (!attached.isEmpty()) {
                    net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                            level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.8,
                            worldPosition.getZ() + 0.5, attached.copy());
                    drop.setDefaultPickUpDelay();
                    level.addFreshEntity(drop);
                }
            }
            attached1 = ItemStack.EMPTY;
            attached2 = ItemStack.EMPTY;
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
        if (VesselHeating.isThreeNeck(vessel)) {
            StringBuilder necks = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                necks.append(VesselHeating.neckHasStopper(vessel, i) ? "●" : "○");
            }
            tooltip.add(Component.literal("瓶口（左中右）：" + necks));
        }
        if (ModItems.hasThermometer(attached1, attached2)) {
            tooltip.add(ChemGoggleLines.temp(TemperatureSystem.getTemp(vessel),
                    VesselHeating.isTempLocked(vessel)));
        } else {
            tooltip.add(Component.literal("温度：无法查看（未插温度计）"));
        }
        ChemGoggleLines.appendContents(tooltip, vessel);
        ChemGoggleLines.appendPressure(tooltip, vessel);
        if (!attached1.isEmpty() || !attached2.isEmpty()) {
            int n = (attached1.isEmpty() ? 0 : 1) + (attached2.isEmpty() ? 0 : 1);
            tooltip.add(Component.literal("已插入仪器：" + n + "件"));
        }
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("vessel", ItemStack.OPTIONAL_CODEC, vessel);
        output.store("attached1", ItemStack.OPTIONAL_CODEC, attached1);
        output.store("attached2", ItemStack.OPTIONAL_CODEC, attached2);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        vessel = input.read("vessel", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        attached1 = input.read("attached1", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        attached2 = input.read("attached2", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
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
