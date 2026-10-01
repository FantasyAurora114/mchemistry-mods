package com.example.chemistry.entity;

import java.util.List;

import com.example.chemistry.DistillationAssembly;
import com.example.chemistry.TemperatureSystem;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.api.goggles.ChemGoggleLines;
import com.example.chemistry.api.goggles.IChemGoggleInfo;
import com.example.chemistry.registry.ModItems;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** One individually selectable part of an entity-based distillation apparatus. */
public class DistillationPartEntity extends TechnicalEntity implements IChemGoggleInfo {
    public static final int HEAD = 1;
    public static final int CONDENSER = 2;
    public static final int ADAPTER_BENT = 3;
    public static final int ADAPTER_STRAIGHT = 4;
    public static final int THERMOMETER = 5;

    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(DistillationPartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_STAND =
            SynchedEntityData.defineId(DistillationPartEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_STEAM =
            SynchedEntityData.defineId(DistillationPartEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FLOW =
            SynchedEntityData.defineId(DistillationPartEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_YAW =
            SynchedEntityData.defineId(DistillationPartEntity.class, EntityDataSerializers.FLOAT);

    public DistillationPartEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, HEAD);
        builder.define(DATA_STAND, "");
        builder.define(DATA_STEAM, false);
        builder.define(DATA_FLOW, false);
        builder.define(DATA_YAW, 0.0F);
    }

    public int kind() {
        return entityData.get(DATA_KIND);
    }

    public void setKind(int kind) {
        entityData.set(DATA_KIND, kind);
    }

    public String standId() {
        return entityData.get(DATA_STAND);
    }

    public void setStand(IronStandEntity stand) {
        entityData.set(DATA_STAND, stand.getUUID().toString());
        entityData.set(DATA_YAW, -stand.getFacing().toYRot());
    }

    public float assemblyYaw() {
        return entityData.get(DATA_YAW);
    }

    public boolean hasSteam() {
        return entityData.get(DATA_STEAM);
    }

    public boolean hasFlow() {
        return entityData.get(DATA_FLOW);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 4 == 0) {
            if (kind() == CONDENSER) {
                entityData.set(DATA_FLOW, com.example.chemistry.utility.UtilityConnections.cooling(this));
            } else if (kind() == HEAD) {
                entityData.set(DATA_STEAM, DistillationAssembly.isProducingSteam(this));
            } else if (kind() == ADAPTER_BENT) {
                entityData.set(DATA_FLOW, DistillationAssembly.isFlowing(this));
            }
        }
    }

    @Override
    public ItemStack toStack() {
        return switch (kind()) {
            case HEAD -> new ItemStack(ModItems.DISTILLATION_HEAD.get());
            case CONDENSER -> new ItemStack(ModItems.STRAIGHT_CONDENSER.get());
            case ADAPTER_BENT -> new ItemStack(ModItems.RECEIVER_ADAPTER_BENT.get());
            case ADAPTER_STRAIGHT -> new ItemStack(ModItems.RECEIVER_ADAPTER_STRAIGHT.get());
            case THERMOMETER -> new ItemStack(ModItems.THERMOMETER.get());
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        var sleeveResult = com.example.chemistry.ThermometerSleeves.interact(this, player, hand);
        if (sleeveResult != InteractionResult.PASS) return sleeveResult;
        return DistillationAssembly.interact(this, player, hand);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!DistillationAssembly.canRemove(this)) {
            return true;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean addGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(toStack().getHoverName());
        if (kind() == HEAD || kind() == THERMOMETER) {
            IronStandEntity stand = DistillationAssembly.standFor(this);
            PlacedVesselEntity source = stand == null ? null : stand.findMountedVessel();
            if (source != null && (DistillationAssembly.part(stand, THERMOMETER) != null
                    || com.example.chemistry.ThermometerSleeves.hasThermometer(this))) {
                tooltip.add(ChemGoggleLines.temp(TemperatureSystem.getTemp(source.getVessel()),
                        VesselHeating.isTempLocked(source.getVessel())));
            }
        }
        return true;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("part_kind", kind());
        output.putString("assembly_stand", standId());
        output.putBoolean("steam", hasSteam());
        output.putBoolean("flow", hasFlow());
        output.putFloat("assembly_yaw", assemblyYaw());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setKind(input.getIntOr("part_kind", HEAD));
        entityData.set(DATA_STAND, input.getStringOr("assembly_stand", ""));
        entityData.set(DATA_STEAM, input.getBooleanOr("steam", false));
        entityData.set(DATA_FLOW, input.getBooleanOr("flow", false));
        entityData.set(DATA_YAW, input.getFloatOr("assembly_yaw", 0.0F));
    }
}
