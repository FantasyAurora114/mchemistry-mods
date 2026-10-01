package com.example.chemistry.blockentity;

import java.util.List;

import com.example.chemistry.block.IronStandBlock;
import com.example.chemistry.VesselGasPhase;
import com.example.chemistry.TemperatureSystem;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.GasFlowEngine;
import com.example.chemistry.ReactionEngine;
import com.example.chemistry.ReactionPhenomena;
import com.example.chemistry.api.goggles.ChemGoggleLines;
import com.example.chemistry.api.goggles.IChemGoggleInfo;
import com.example.chemistry.data.Reactions;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.item.TestTubeItem;
import com.example.chemistry.registry.ModBlockEntities;
import com.example.chemistry.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Stores the test tube mounted on the iron stand. */
public class IronStandBlockEntity extends BlockEntity implements IChemGoggleInfo {

    private ItemStack tube = ItemStack.EMPTY;
    private ItemStack attached1 = ItemStack.EMPTY;
    private ItemStack attached2 = ItemStack.EMPTY;
    /** Vessel (烧瓶/锥形瓶/坩埚/蒸发皿) sitting on the ring / gauze attachment. */
    private ItemStack vessel = ItemStack.EMPTY;
    private boolean hasCondenser;
    /** 蒸馏头（接在圆底烧瓶瓶口上，导气管连冷凝管）。 */
    private boolean hasDistillationHead;
    /** 温度计插在蒸馏头顶端接口里。 */
    private boolean headThermometer;
    /** Receiver flask collecting the distillate. */
    private ItemStack receiver = ItemStack.EMPTY;
    /** 牛角管（接在冷凝管远端，通向接收瓶）。 */
    private ItemStack receiverAdapter = ItemStack.EMPTY;
    /** The mounted lamp is an alcohol blowtorch (1200 C) instead of a lamp. */
    private boolean lampBlowtorch;
    /** 酒精灯已盖上灯帽（随灯一起取下）。 */
    private boolean lampCapped;

    public IronStandBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IRON_STAND.get(), pos, state);
    }

    public ItemStack getTube() {
        return tube;
    }

    public ItemStack getAttached1() {
        return attached1;
    }

    public ItemStack getAttached2() {
        return attached2;
    }

    public ItemStack getVessel() {
        return vessel;
    }

    public boolean hasCondenser() {
        return hasCondenser;
    }

    public boolean hasDistillationHead() {
        return hasDistillationHead;
    }

    public boolean hasHeadThermometer() {
        return headThermometer;
    }

    public boolean isLampBlowtorch() {
        return lampBlowtorch;
    }

    public boolean isLampCapped() {
        return lampCapped;
    }

    public void setLampCapped(boolean capped) {
        this.lampCapped = capped;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setLampBlowtorch(boolean lampBlowtorch) {
        this.lampBlowtorch = lampBlowtorch;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public ItemStack getReceiver() {
        return receiver;
    }

    public ItemStack getReceiverAdapter() {
        return receiverAdapter;
    }

    public boolean hasReceiverAdapter() {
        return !receiverAdapter.isEmpty();
    }

    public void setReceiverAdapter(ItemStack stack) {
        this.receiverAdapter = stack.copy();
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setVessel(ItemStack stack) {
        setChanged();
        if (level != null && !level.isClientSide()) {
            boolean hasVessel = !stack.isEmpty();
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(IronStandBlock.HAS_VESSEL)
                    && state.getValue(IronStandBlock.HAS_VESSEL) != hasVessel) {
                level.setBlock(worldPosition, state.setValue(IronStandBlock.HAS_VESSEL, hasVessel), 3);
            }
            // setBlock may have replaced this block entity; write to the live one.
            if (level.getBlockEntity(worldPosition) instanceof IronStandBlockEntity be && be != this) {
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

    public void setCondenser(boolean value) {
        this.hasCondenser = value;
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = level.getBlockState(worldPosition);
            level.setBlock(worldPosition, state.setValue(IronStandBlock.HAS_CONDENSER, value), 3);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public void setDistillationHead(boolean value) {
        this.hasDistillationHead = value;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setHeadThermometer(boolean value) {
        this.headThermometer = value;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setReceiver(ItemStack stack) {
        this.receiver = stack.copy();
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = level.getBlockState(worldPosition);
            level.setBlock(worldPosition, state.setValue(IronStandBlock.HAS_RECEIVER, !receiver.isEmpty()), 3);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public void setAttached1(ItemStack stack) {
        this.attached1 = stack.copy();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        syncTubeThermometerFlag();
    }

    public void setAttached2(ItemStack stack) {
        this.attached2 = stack.copy();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        syncTubeThermometerFlag();
    }

    /** 试管装上/取下温度计后，把标志写回试管物品（拾取后仍能查看温度）。 */
    private void syncTubeThermometerFlag() {
        if (tube.isEmpty()) {
            return;
        }
        boolean has = com.example.chemistry.registry.ModItems.hasThermometer(attached1, attached2);
        net.minecraft.nbt.CompoundTag tag = tube.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (has) {
            tag.putBoolean("chem_thermometer", true);
        } else {
            tag.remove("chem_thermometer");
        }
        tube.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
        setChanged();
    }

    /** Lit lamp heats the mounted tube (600 C) or the vessel on the ring. */
    public void tickServer(Level level) {
        if (level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        boolean lamp = state.getValue(IronStandBlock.HAS_LAMP)
                && state.getValue(IronStandBlock.LAMP_LIT);
        boolean blowtorch = lamp && lampBlowtorch;
        Player nearest = level.getNearestPlayer(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5, 8.0, false);
        if (state.getValue(IronStandBlock.HAS_TUBE) && tube.getItem() instanceof TestTubeItem) {
            com.example.chemistry.GasBurners.heat(level, worldPosition, tube);
            if (lamp) {
                if(blowtorch)VesselHeating.heatFast(tube,1200);
                    else VesselHeating.heatSlow(tube,600);
            } else if (TemperatureSystem.getTemp(tube) != TemperatureSystem.ROOM_TEMP) {
                // The mounted tube cools back to room temperature when the
                // lamp is out, so heat-required reactions stop too.
                VesselHeating.coolGradual(tube);
            }
        }
        // The mounted tube also runs its reactions (heat-required ones start
        // once the lit lamp raises the temperature).
        if (state.getValue(IronStandBlock.HAS_TUBE) && !tube.isEmpty()) {
            ReactionEngine.checkAndStart(tube, nearest);
            ReactionEngine.Completion completed = ReactionEngine.tickResult(tube, nearest);
            if (completed != null) {
                ReactionPhenomena.spawn(level,
                        new net.minecraft.world.phys.Vec3(worldPosition.getX() + 0.5,
                                worldPosition.getY() + 0.8, worldPosition.getZ() + 0.5),
                        ReactionPhenomena.detect(completed.reaction(), tube));
                GasFlowEngine.enqueue(level, worldPosition, tube, completed);
            }
            // Gas produced in the mounted tube also flows along a connected
            // rubber tube (glass tube in the stopper -> collector).
            GasFlowEngine.pump(level, worldPosition, tube);
            // 敞口试管里比空气轻的气体慢慢逸出。
            VesselGasPhase.tickLeak(tube);
        }
        if (state.getValue(IronStandBlock.HAS_VESSEL) && !vessel.isEmpty()) {
            if (VesselHeating.isTempLocked(vessel)) {
                // Temperature pinned by the I key: neither heat nor cool.
            } else if (lamp) {
                if (blowtorch) {
                    VesselHeating.heatFast(vessel, VesselHeating.BLOWTORCH_TEMP);
                } else {
                    VesselHeating.heatSlow(vessel, 600.0);
                }
            } else {
                VesselHeating.coolGradual(vessel);
            }
            VesselHeating.Outcome outcome = VesselHeating.tick(
                    vessel, level, worldPosition, receiver, hasCondenser,
                    !attached1.isEmpty() || !attached2.isEmpty(), nearest);
            handleOutcome(outcome, level);
            setChanged();
        }
        if (!level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void handleOutcome(VesselHeating.Outcome outcome, Level level) {
        if (outcome == VesselHeating.Outcome.POPPED) {
            int holes = VesselHeating.getStopperHoles(vessel);
            net.minecraft.world.entity.item.ItemEntity stopper = new net.minecraft.world.entity.item.ItemEntity(
                    level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.6, worldPosition.getZ() + 0.5,
                    new ItemStack(ModItems.stopperForHoles(holes)));
            stopper.setDefaultPickUpDelay();
            level.addFreshEntity(stopper);
            for (ItemStack attached : new ItemStack[] {getAttached2(), getAttached1()}) {
                if (!attached.isEmpty()) {
                    net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                            level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.7,
                            worldPosition.getZ() + 0.5, attached.copy());
                    drop.setDefaultPickUpDelay();
                    level.addFreshEntity(drop);
                }
            }
            setAttached1(ItemStack.EMPTY);
            setAttached2(ItemStack.EMPTY);
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
    }

    public void setTube(ItemStack stack) {
        this.tube = stack.copy();
        setChanged();
        if (level != null && !level.isClientSide()) {
            boolean hasTube = !tube.isEmpty();
            boolean hasContents = !LabVesselItem.getContents(tube).isEmpty();
            boolean hasStopper = tube.getItem() instanceof TestTubeItem tt && tt.stopperHoles() > 0;
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(IronStandBlock.HAS_TUBE)
                    && (state.getValue(IronStandBlock.HAS_TUBE) != hasTube
                            || state.getValue(IronStandBlock.HAS_CONTENTS) != hasContents
                            || state.getValue(IronStandBlock.HAS_STOPPER) != hasStopper)) {
                level.setBlock(worldPosition, state.setValue(IronStandBlock.HAS_TUBE, hasTube)
                        .setValue(IronStandBlock.HAS_CONTENTS, hasContents)
                        .setValue(IronStandBlock.HAS_STOPPER, hasStopper), 3);
            }
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public boolean addGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean any = false;
        BlockState state = getBlockState();
        if (!tube.isEmpty()) {
            tooltip.add(Component.literal("铁架台试管"));
            if (ModItems.hasThermometer(attached1, attached2)) {
                tooltip.add(ChemGoggleLines.temp(TemperatureSystem.getTemp(tube)));
            } else {
                tooltip.add(Component.literal("温度：无法查看（未插温度计）"));
            }
            ChemGoggleLines.appendContents(tooltip, tube);
            if (tube.getItem() instanceof TestTubeItem tt) {
                if (tt.stopperHoles() > 0) {
                    tooltip.add(Component.literal("橡胶塞：" + tt.stopperHoles() + "孔"));
                }
                if (tt.isClamped()) {
                    tooltip.add(Component.literal("已套试管夹"));
                }
            }
            int n = (attached1.isEmpty() ? 0 : 1) + (attached2.isEmpty() ? 0 : 1);
            if (n > 0) {
                tooltip.add(Component.literal("已插入仪器：" + n + "件"));
            }
            if (state.getValue(IronStandBlock.HAS_LAMP) && state.getValue(IronStandBlock.LAMP_LIT)) {
                tooltip.add(Component.literal("酒精灯加热中（恒温600°C）"));
            }
            any = true;
        }
        if (!vessel.isEmpty()) {
            if (any) {
                tooltip.add(Component.empty());
            }
            tooltip.add(vessel.getHoverName().copy());
            if (ModItems.hasThermometer(attached1, attached2)) {
                tooltip.add(ChemGoggleLines.temp(TemperatureSystem.getTemp(vessel),
                        VesselHeating.isTempLocked(vessel)));
            } else {
                tooltip.add(Component.literal("温度：无法查看（未插温度计）"));
            }
            ChemGoggleLines.appendContents(tooltip, vessel);
            ChemGoggleLines.appendPressure(tooltip, vessel);
            if (hasCondenser) {
                tooltip.add(Component.literal("已连接冷凝管"));
            }
            if (!receiver.isEmpty()) {
                tooltip.add(Component.literal("接收瓶：" + receiver.getHoverName().getString()));
            }
            any = true;
        }
        return any;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("tube", ItemStack.OPTIONAL_CODEC, tube);
        output.store("attached1", ItemStack.OPTIONAL_CODEC, attached1);
        output.store("attached2", ItemStack.OPTIONAL_CODEC, attached2);
        output.store("vessel", ItemStack.OPTIONAL_CODEC, vessel);
        output.store("receiver", ItemStack.OPTIONAL_CODEC, receiver);
        output.store("receiver_adapter", ItemStack.OPTIONAL_CODEC, receiverAdapter);
        output.putBoolean("has_condenser", hasCondenser);
        output.putBoolean("has_distillation_head", hasDistillationHead);
        output.putBoolean("head_thermometer", headThermometer);
        output.putBoolean("lamp_blowtorch", lampBlowtorch);
        output.putBoolean("lamp_capped", lampCapped);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tube = input.read("tube", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        attached1 = input.read("attached1", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        attached2 = input.read("attached2", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        vessel = input.read("vessel", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        receiver = input.read("receiver", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        receiverAdapter = input.read("receiver_adapter", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        hasCondenser = input.getBooleanOr("has_condenser", false);
        hasDistillationHead = input.getBooleanOr("has_distillation_head", false);
        headThermometer = input.getBooleanOr("head_thermometer", false);
        lampBlowtorch = input.getBooleanOr("lamp_blowtorch", false);
        lampCapped = input.getBooleanOr("lamp_capped", false);
    }

    /** Sync the attached instruments (dropper contents matter client-side). */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
