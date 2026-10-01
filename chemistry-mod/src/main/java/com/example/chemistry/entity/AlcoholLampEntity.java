package com.example.chemistry.entity;

import com.example.chemistry.registry.ModItems;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * 酒精灯 / 酒精喷灯（技术性实体）。可落地，也可叠加在铁架台底座上。
 * 状态放 synced data：点燃/盖帽/喷灯/燃料。
 */
public class AlcoholLampEntity extends TechnicalEntity {

    public static final double CAPACITY = 120.0;

    private static final EntityDataAccessor<Boolean> DATA_LIT =
            SynchedEntityData.defineId(AlcoholLampEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CAPPED =
            SynchedEntityData.defineId(AlcoholLampEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BLOWTORCH =
            SynchedEntityData.defineId(AlcoholLampEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_FUEL =
            SynchedEntityData.defineId(AlcoholLampEntity.class, EntityDataSerializers.FLOAT);

    public AlcoholLampEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIT, false);
        builder.define(DATA_CAPPED, false);
        builder.define(DATA_BLOWTORCH, false);
        builder.define(DATA_FUEL, 0.0F);
    }

    public boolean isLit() {
        return this.entityData.get(DATA_LIT);
    }

    public void setLit(boolean v) {
        this.entityData.set(DATA_LIT, v);
    }

    public boolean isCapped() {
        return this.entityData.get(DATA_CAPPED);
    }

    public void setCapped(boolean v) {
        this.entityData.set(DATA_CAPPED, v);
    }

    public boolean isBlowtorch() {
        return this.entityData.get(DATA_BLOWTORCH);
    }

    public void setBlowtorch(boolean v) {
        this.entityData.set(DATA_BLOWTORCH, v);
    }

    public double getFuel() {
        return this.entityData.get(DATA_FUEL);
    }

    public void setFuel(double fuel) {
        this.entityData.set(DATA_FUEL, (float) Math.max(0.0, Math.min(CAPACITY, fuel)));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("lit", isLit());
        output.putBoolean("capped", isCapped());
        output.putBoolean("blowtorch", isBlowtorch());
        output.putFloat("fuel", (float) getFuel());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setLit(input.getBooleanOr("lit", false));
        setCapped(input.getBooleanOr("capped", false));
        setBlowtorch(input.getBooleanOr("blowtorch", false));
        setFuel(input.getFloatOr("fuel", 0.0F));
    }

    @Override
    public AABB virtualHitbox() {
        // 酒精灯矮小：只盖住灯体本身，不覆盖铁架台/锥形瓶。
        return getBoundingBox();
    }

    @Override
    public ItemStack toStack() {
        if (isBlowtorch()) {
            return new ItemStack(ModItems.ALCOHOL_BLOWTORCH.get());
        }
        if (isCapped()) {
            return new ItemStack(ModItems.ALCOHOL_LAMP_CAPPED.get());
        }
        return new ItemStack(ModItems.ALCOHOL_LAMP.get());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (isLit() && getFuel() > 0) {
            setFuel(getFuel() - 1.0 / 20.0);
            if (getFuel() <= 0) {
                setLit(false);
            }
        }
        if (level() instanceof ServerLevel server && isLit()) {
            double y = getY() + (isBlowtorch() ? 0.38 : 0.55);
            server.sendParticles(ParticleTypes.SMALL_FLAME, getX(), y, getZ(),
                    1, 0.06, 0.05, 0.06, 0.01);
            if (server.getGameTime() % 12 == 0) {
                server.sendParticles(ParticleTypes.SMOKE, getX(), y + 0.3, getZ(),
                        1, 0.05, 0.05, 0.05, 0.01);
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!level().isClientSide()) {
            // 盖帽（喷灯无盖）。
            if (held.is(ModItems.ALCOHOL_LAMP_CAP.get())) {
                if (isBlowtorch()) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.alcohol_blowtorch.no_cap"), true);
                } else if (!isCapped()) {
                    setCapped(true);
                    setLit(false);
                    held.shrink(1);
                    level().playSound(null, blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE,
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }
            // 点燃。
            if (held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE)) {
                if (isLit()) {
                    return InteractionResult.SUCCESS;
                }
                if (isCapped()) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.alcohol_lamp.cannot_light"), true);
                } else if (getFuel() <= 0) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.alcohol_lamp.no_fuel"), true);
                } else {
                    setLit(true);
                    if (held.is(Items.FIRE_CHARGE)) {
                        held.shrink(1);
                    }
                    level().playSound(null, blockPosition(), SoundEvents.FLINTANDSTEEL_USE,
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }
            // 75% 乙醇加油。
            String liquidId = com.example.chemistry.transfer.BottleCodes.liquidIdOf(held);
            if ("ethanol_75".equals(liquidId)) {
                if (isLit()) {
                    player.igniteForSeconds(4.0F);
                    player.displayClientMessage(
                            Component.translatable("mchemistry.alcohol_lamp.ethanol_fire"), true);
                } else if (isCapped()) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.alcohol_lamp.capped_refill"), true);
                } else {
                    setFuel(CAPACITY);
                    level().playSound(null, blockPosition(), SoundEvents.BOTTLE_EMPTY,
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }
            // 空手：熄灭（点燃时）或取回。
            if (held.isEmpty()) {
                if (isLit()) {
                    setLit(false);
                } else if (pickUp(player)) {
                    level().playSound(null, blockPosition(), SoundEvents.ITEM_PICKUP,
                            SoundSource.BLOCKS, 0.8F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }
        } else {
            if (held.isEmpty() || held.is(ModItems.ALCOHOL_LAMP_CAP.get())
                    || held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE)
                    || "ethanol_75".equals(
                            com.example.chemistry.transfer.BottleCodes.liquidIdOf(held))) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}
