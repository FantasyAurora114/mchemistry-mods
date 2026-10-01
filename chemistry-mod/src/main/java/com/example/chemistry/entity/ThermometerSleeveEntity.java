package com.example.chemistry.entity;

import java.util.List;
import java.util.UUID;
import com.example.chemistry.*;
import com.example.chemistry.api.goggles.IChemGoggleInfo;
import com.example.chemistry.api.goggles.ChemGoggleLines;
import com.example.chemistry.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** A separately selectable adapter. Its thermometer is a real saved ItemStack. */
public class ThermometerSleeveEntity extends TechnicalEntity implements IChemGoggleInfo {
    private static final EntityDataAccessor<String> OWNER = SynchedEntityData.defineId(ThermometerSleeveEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> PORT = SynchedEntityData.defineId(ThermometerSleeveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> SLEEVE = SynchedEntityData.defineId(ThermometerSleeveEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> THERMOMETER = SynchedEntityData.defineId(ThermometerSleeveEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(ThermometerSleeveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(ThermometerSleeveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TILT = SynchedEntityData.defineId(ThermometerSleeveEntity.class, EntityDataSerializers.FLOAT);
    public ThermometerSleeveEntity(EntityType<?> type, Level level) { super(type, level); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(OWNER, ""); b.define(PORT, 1); b.define(SLEEVE, ItemStack.EMPTY);
        b.define(THERMOMETER, ItemStack.EMPTY); b.define(SCALE, 1F); b.define(YAW, 0F); b.define(TILT, 0F);
    }
    public String ownerId() { return entityData.get(OWNER); }
    public int port() { return entityData.get(PORT); }
    public float scale() { return entityData.get(SCALE); }
    public float yaw() { return entityData.get(YAW); }
    public float tilt() { return entityData.get(TILT); }
    public ItemStack thermometer() { return entityData.get(THERMOMETER); }
    public void setThermometer(ItemStack stack) { entityData.set(THERMOMETER, stack.copy()); }
    public void attach(Entity owner, int port, ItemStack sleeve) {
        entityData.set(OWNER, owner.getUUID().toString()); entityData.set(PORT, port);
        entityData.set(SLEEVE, sleeve.copyWithCount(1)); updateMount(owner);
    }
    public Entity owner() {
        if (level() instanceof ServerLevel server) {
            try { return server.getEntity(UUID.fromString(ownerId())); }
            catch (IllegalArgumentException ex) { return null; }
        }
        return level().getEntities(this, getBoundingBox().inflate(4), e -> e.getUUID().toString().equals(ownerId()))
                .stream().findFirst().orElse(null);
    }
    public void updateMount(Entity owner) {
        var mount = ThermometerSleeves.mount(owner, port());
        if (mount == null) return;
        entityData.set(SCALE, mount.scale()); entityData.set(YAW, mount.yaw()); entityData.set(TILT, mount.tilt());
        setPos(mount.position().x, mount.position().y, mount.position().z);
        setBoundingBox(virtualHitbox());
    }
    @Override public void tick() {
        super.tick();
        setBoundingBox(virtualHitbox());
        if (!level().isClientSide()) { Entity host = owner(); if (host != null) updateMount(host); }
    }
    @Override public AABB virtualHitbox() {
        double angle = Math.toRadians(tilt()), yaw = Math.toRadians(yaw());
        Vec3 direction = new Vec3(-Math.sin(angle)*Math.cos(yaw), Math.cos(angle), Math.sin(angle)*Math.sin(yaw));
        Vec3 bottom = position().add(direction.scale(-.16*scale()));
        Vec3 top = position().add(direction.scale((thermometer().isEmpty() ? .28 : .44)*scale()));
        return new AABB(bottom, top).inflate(.065*scale());
    }
    @Override public ItemStack toStack() { return entityData.get(SLEEVE).copy(); }
    public List<ItemStack> contents() { return thermometer().isEmpty() ? List.of(toStack()) : List.of(toStack(), thermometer().copy()); }
    @Override protected List<ItemStack> dropsOnBreak() { return contents(); }
    private static void give(Player p, ItemStack stack) { if (!p.getInventory().add(stack)) p.drop(stack, false); }
    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.THERMOMETER.get()) && thermometer().isEmpty()) {
            setThermometer(held.copyWithCount(1)); if (!player.isCreative()) held.shrink(1);
        } else if (player.isShiftKeyDown() && held.isEmpty()) {
            for (ItemStack item : contents()) give(player, item); discard();
        } else if (held.isEmpty()) {
            if (!thermometer().isEmpty()) { give(player, thermometer().copy()); setThermometer(ItemStack.EMPTY); }
            else pickUp(player);
        } else if (!thermometer().isEmpty()) {
            ItemStack vessel = ThermometerSleeves.measuredVessel(owner());
            if (!vessel.isEmpty()) player.displayClientMessage(Component.translatable("mchemistry.thermometer.read",
                    String.format("%.0f", TemperatureSystem.getTemp(vessel))), true);
        }
        return InteractionResult.SUCCESS;
    }
    @Override public boolean addGoggleInfo(List<Component> lines, boolean sneaking) {
        lines.add(toStack().getHoverName());
        if (!thermometer().isEmpty()) {
            ItemStack vessel = ThermometerSleeves.measuredVessel(owner());
            if (!vessel.isEmpty()) lines.add(ChemGoggleLines.temp(TemperatureSystem.getTemp(vessel), VesselHeating.isTempLocked(vessel)));
        }
        return true;
    }
    @Override protected void addAdditionalSaveData(ValueOutput out) {
        out.putString("owner",ownerId()); out.putInt("port",port());
        out.store("sleeve",ItemStack.OPTIONAL_CODEC,toStack()); out.store("thermometer",ItemStack.OPTIONAL_CODEC,thermometer());
        out.putFloat("scale",scale()); out.putFloat("yaw",yaw()); out.putFloat("tilt",tilt());
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        entityData.set(OWNER,in.getStringOr("owner","")); entityData.set(PORT,in.getIntOr("port",1));
        entityData.set(SLEEVE,in.read("sleeve",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        setThermometer(in.read("thermometer",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        entityData.set(SCALE,in.getFloatOr("scale",1)); entityData.set(YAW,in.getFloatOr("yaw",0)); entityData.set(TILT,in.getFloatOr("tilt",0));
    }
}
