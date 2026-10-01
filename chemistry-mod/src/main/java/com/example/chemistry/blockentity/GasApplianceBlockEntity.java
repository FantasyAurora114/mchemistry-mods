package com.example.chemistry.blockentity;

import com.example.chemistry.FlowNetwork;
import com.example.chemistry.GasFlowEngine;
import com.example.chemistry.ThermalSystem;
import com.example.chemistry.block.GasApplianceBlock;
import com.example.chemistry.entity.RubberTubeEntity;
import com.example.chemistry.entity.RubberTubeEntity.Port;
import com.example.chemistry.item.GasCylinderItem;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.item.RubberTubeItem;
import com.example.chemistry.registry.ModBlockEntities;
import com.example.chemistry.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** A finite gas source or a methane burner sharing the existing tube network. */
public final class GasApplianceBlockEntity extends BlockEntity implements com.example.chemistry.api.goggles.IChemGoggleInfo {
    public static final int FLOW_ML = 5;
    public static final int BURN_ML = 2;
    private String gas = "";
    private ItemStack nuclearSample = ItemStack.EMPTY;
    public void setRadioSample(ItemStack sample) { nuclearSample=sample.copy(); setChanged(); }
    private int remaining;
    private boolean valve;
    private boolean blue = true;
    private boolean burning;
    private double heatJ;
    private boolean dropped;
    public boolean suppressDrop;

    public GasApplianceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_APPLIANCE.get(), pos, state);
    }

    public int kind() { return ((GasApplianceBlock) getBlockState().getBlock()).kind(); }
    public int capacity() { return kind() == 0 ? 100 : kind() == 2 ? 100000 : 10000; }
    public String gas() { return gas; }
    public int remaining() { return remaining; }
    public boolean valve() { return valve; }
    public boolean blue() { return blue; }
    public boolean burning() { return burning; }
    public void loadCylinder(String id, int ml) { gas = id; remaining = Math.clamp(ml, 0, capacity()); sync(); }
    public void toggleValve() { valve = !valve; if (!valve) burning = false; sync(); }
    public void toggleAir() { blue = !blue; sync(); }
    public boolean ignite() {
        if (kind() != 0 || !valve || remaining < BURN_ML || !gas.equals("methane")) return false;
        burning = true; sync(); return true;
    }
    public Port port() { return Port.block(worldPosition, getBlockState().getValue(GasApplianceBlock.FACING)); }
    public Vec3 portPosition() {
        double x = kind() == 0 ? 11.0 / 16 : kind() == 2 ? 10.72 / 16 : 8.4502 / 16;
        double y = kind() == 0 ? 1.9 / 16 : kind() == 2 ? 28.225 / 16 : 6.642 / 16;
        double a = Math.toRadians(rotation());
        return new Vec3(worldPosition.getX() + .5 + (x - .5) * Math.cos(a),
                worldPosition.getY() + y, worldPosition.getZ() + .5 - (x - .5) * Math.sin(a));
    }
    public float rotation() {
        return switch (getBlockState().getValue(GasApplianceBlock.FACING)) {
            case SOUTH -> 180; case EAST -> -90; case WEST -> 90; default -> 0;
        };
    }
    public int receive(String id, int ml) {
        if (ml <= 0 || (kind() == 0 && (!valve || !id.equals("methane"))) || (kind() != 0 && valve)) return 0;
        if (!gas.isEmpty() && remaining > 0 && !gas.equals(id)) return 0;
        int accepted = Math.min(ml, capacity() - remaining);
        if (accepted > 0) { gas = id; remaining += accepted; sync(); }
        return accepted;
    }

    /** Backpressure keeps undelivered gas in the line; the source never creates or deletes it. */
    public void tickServer() {
        if (!(level instanceof ServerLevel server)) return;
        heatJ = 0;
        int previousRemaining = remaining; boolean previousBurning = burning;
        if (kind() == 0) {
            if (!valve || remaining < BURN_ML) burning = false;
            if (burning) {
                remaining -= BURN_ML;
                // CH4 + 2 O2 -> CO2 + 2 H2O(g), lower heating value ~802 kJ/mol.
                // Heat capture is a gameplay efficiency, not a measured burner parameter.
                heatJ = BURN_ML / GasFlowEngine.ML_PER_MOL * 802000 * (blue ? .30 : .15);
                if (server.getGameTime() % 4 == 0) server.sendParticles(blue ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
                        worldPosition.getX() + .5, worldPosition.getY() + .94, worldPosition.getZ() + .5,
                        1, .015, .035, .015, 0);
            } else if (valve && remaining > 0) remaining--;
            if (previousRemaining != remaining || previousBurning != burning) sync(); return;
        }
        if (!valve) return;
        var tubes = RubberTubeItem.findTubesAt(level, port());
        if (tubes.isEmpty()) {
            if (remaining > 0) {
                int escaped=Math.min(FLOW_ML,remaining);
                if(gas.equals("radon")&&escaped>0){var plume=new ItemStack(ModItems.RADIOACTIVE_WASTE_BOTTLE.get());LabVesselItem.addMass(plume,"gas","radon",escaped*222.0/24465);com.example.chemistry.radiation.RadioLedger.copyState(nuclearSample,plume);com.example.chemistry.radiation.RadiationContamination.spill(server,worldPosition,plume);}
                remaining -= escaped;
                if (server.getGameTime() % 10 == 0) {
                    Vec3 p = portPosition(); server.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 1, .02, .02, .02, .01);
                }
            }
        } else for (var tube : tubes) {
            Port far = GasFlowEngine.otherAnchor(tube, port());
            if (far == null) continue;
            // Storage volume is not the throughput limit: a short line can refill within a tick.
            int supplyBudget = FLOW_ML, outletBudget = FLOW_ML;
            for (int step = 0; step <= FLOW_ML; step++) {
                if (tube.hasTransit() && outletBudget > 0) {
                    int offered = Math.min(outletBudget, tube.getTransitMl());
                    int accepted = Math.clamp(FlowNetwork.deliver(level, far,
                            FlowNetwork.Packet.gas(tube.getTransitGas(), offered, tube.getTransitPurity())), 0, offered);
                    tube.takeTransit(accepted); outletBudget -= accepted;
                    if (accepted < offered) break;
                }
                int into = Math.min(supplyBudget, Math.min(remaining,
                        GasFlowEngine.tubeCapacity(level, tube) - tube.getTransitMl()));
                if (into <= 0) break;
                int stored = tube.addTransit(gas, into, 1);
                remaining -= stored; supplyBudget -= stored;
                if (stored == 0 || supplyBudget == 0 || outletBudget == 0) break;
            }
        }
        if (previousRemaining != remaining) sync();
    }

    public boolean heat(ItemStack vessel) {
        if (!burning || heatJ <= 0 || !(vessel.getItem() instanceof LabVesselItem)) return false;
        double temperature = com.example.chemistry.TemperatureSystem.getTemp(vessel);
        double limit = blue ? 1200 : 600;
        double usable = Math.min(heatJ, Math.max(0, limit - temperature) * ThermalSystem.capacity(vessel));
        if (usable <= 0) return false;
        heatJ -= usable; ThermalSystem.addHeat(vessel, usable, "bunsen_burner"); return true;
    }
    public ItemStack dropStack() {
        var stack=kind() == 0 ? new ItemStack(ModItems.BUNSEN_BURNER.get()) : GasCylinderItem.filled(kind() == 2, gas, remaining);
        com.example.chemistry.radiation.RadioLedger.copyState(nuclearSample,stack);return stack;
    }
    @Override public boolean addGoggleInfo(java.util.List<net.minecraft.network.chat.Component> lines, boolean details) {
        String name = com.example.chemistry.data.GasJars.ALL.stream().filter(g -> g.id().equals(gas)).map(com.example.chemistry.data.GasJars.GasJar::chinese).findFirst().orElse(gas.isEmpty() ? "空" : gas);
        lines.add(net.minecraft.network.chat.Component.literal(kind() == 0 ? "本生灯" : name + "钢瓶"));
        lines.add(net.minecraft.network.chat.Component.literal("余量 " + remaining + "/" + capacity() + " mL；阀门 " + (valve ? "开" : "关")));
        if (kind() == 0) lines.add(net.minecraft.network.chat.Component.literal((burning ? "燃烧中" : "未点燃") + "；" + (blue ? "蓝焰" : "黄焰")));
        else if (details) lines.add(net.minecraft.network.chat.Component.literal("25°C参考体积；已连接气路 " + (level == null ? 0 : RubberTubeItem.findTubesAt(level, port()).size()) + " 条"));
        return true;
    }
    public void sync() { setChanged(); if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (dropped || level == null || level.isClientSide()) return;
        dropped = true; RubberTubeItem.dropTubesAtBlockPos(level, pos, true);
        if (!suppressDrop) net.minecraft.world.level.block.Block.popResource(level, pos, dropStack());
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out); out.store("radio_sample",ItemStack.OPTIONAL_CODEC,nuclearSample); out.putString("gas", gas); out.putInt("remaining_ml", remaining);
        out.putBoolean("valve", valve); out.putBoolean("blue", blue); out.putBoolean("burning", burning);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in); nuclearSample=in.read("radio_sample",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY); gas = in.getStringOr("gas", ""); remaining = Math.clamp(in.getIntOr("remaining_ml", 0), 0, capacity());
        valve = in.getBooleanOr("valve", false); blue = in.getBooleanOr("blue", true);
        burning = kind() == 0 && valve && remaining >= BURN_ML && in.getBooleanOr("burning", false);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
}
