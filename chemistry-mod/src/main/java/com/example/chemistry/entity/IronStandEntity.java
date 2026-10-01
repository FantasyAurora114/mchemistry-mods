package com.example.chemistry.entity;

import java.util.ArrayList;
import java.util.List;

import com.example.chemistry.ReactionEngine;
import com.example.chemistry.DistillationAssembly;
import com.example.chemistry.ReactionPhenomena;
import com.example.chemistry.GasFlowEngine;
import com.example.chemistry.TemperatureSystem;
import com.example.chemistry.VesselGasPhase;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.data.Reactions;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.item.TestTubeItem;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 铁架台（技术性实体版）。状态全部放 synced data：朝向/旋转、试管、烧瓶、
 * 两个瓶口附件、接收瓶、牛角管、酒精灯、加热附件、冷凝管/蒸馏头/温度计。
 * 交互与渲染由 {@code IronStandEntityRenderer} 和后续迁移的 interact 完成。
 */
public class IronStandEntity extends TechnicalEntity {

    public static final int MAX_EXTENSION_RODS = 2;
    public static final double HEIGHT_STEP = .125;
    private static final EntityDataAccessor<Integer> DATA_RODS =
            SynchedEntityData.defineId(IronStandEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_LIFT =
            SynchedEntityData.defineId(IronStandEntity.class, EntityDataSerializers.FLOAT);
    private static final double VESSEL_SCALE = 0.6;
    private static final double VESSEL_OFF_X = (8.5 - 8.5 * VESSEL_SCALE) / 16.0;
    private static final double VESSEL_OFF_Y = 9.5 / 16.0;
    private static final double VESSEL_OFF_Z = (9.0 - 8.5 * VESSEL_SCALE) / 16.0;

    private static final EntityDataAccessor<Direction> DATA_FACING =
            SynchedEntityData.defineId(IronStandEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Integer> DATA_ROTATION =
            SynchedEntityData.defineId(IronStandEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HAS_TUBE =
            SynchedEntityData.defineId(IronStandEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_LAMP =
            SynchedEntityData.defineId(IronStandEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LAMP_LIT =
            SynchedEntityData.defineId(IronStandEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LAMP_BLOWTORCH =
            SynchedEntityData.defineId(IronStandEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LAMP_CAPPED =
            SynchedEntityData.defineId(IronStandEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ATTACHMENT =
            SynchedEntityData.defineId(IronStandEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HAS_CONDENSER =
            SynchedEntityData.defineId(IronStandEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_DISTILLATION_HEAD =
            SynchedEntityData.defineId(IronStandEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HEAD_THERMOMETER =
            SynchedEntityData.defineId(IronStandEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<ItemStack> DATA_TUBE =
            SynchedEntityData.defineId(IronStandEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_ATTACHED1 =
            SynchedEntityData.defineId(IronStandEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_ATTACHED2 =
            SynchedEntityData.defineId(IronStandEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_VESSEL =
            SynchedEntityData.defineId(IronStandEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_RECEIVER =
            SynchedEntityData.defineId(IronStandEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_RECEIVER_ADAPTER =
            SynchedEntityData.defineId(IronStandEntity.class, EntityDataSerializers.ITEM_STACK);

    public IronStandEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RODS, 0);
        builder.define(DATA_LIFT, 0F);
        builder.define(DATA_FACING, Direction.SOUTH);
        builder.define(DATA_ROTATION, 0);
        builder.define(DATA_HAS_TUBE, false);
        builder.define(DATA_HAS_LAMP, false);
        builder.define(DATA_LAMP_LIT, false);
        builder.define(DATA_LAMP_BLOWTORCH, false);
        builder.define(DATA_LAMP_CAPPED, false);
        builder.define(DATA_ATTACHMENT, 0);
        builder.define(DATA_HAS_CONDENSER, false);
        builder.define(DATA_HAS_DISTILLATION_HEAD, false);
        builder.define(DATA_HEAD_THERMOMETER, false);
        builder.define(DATA_TUBE, ItemStack.EMPTY);
        builder.define(DATA_ATTACHED1, ItemStack.EMPTY);
        builder.define(DATA_ATTACHED2, ItemStack.EMPTY);
        builder.define(DATA_VESSEL, ItemStack.EMPTY);
        builder.define(DATA_RECEIVER, ItemStack.EMPTY);
        builder.define(DATA_RECEIVER_ADAPTER, ItemStack.EMPTY);
    }

    public Direction getFacing() {
        return this.entityData.get(DATA_FACING);
    }

    public void setFacing(Direction d) {
        this.entityData.set(DATA_FACING, d);
    }

    public int getRotation() {
        return this.entityData.get(DATA_ROTATION);
    }

    public void setRotation(int r) {
        this.entityData.set(DATA_ROTATION, r);
    }

    public boolean hasTube() {
        return this.entityData.get(DATA_HAS_TUBE);
    }

    public ItemStack getTube() {
        return this.entityData.get(DATA_TUBE).copy();
    }

    public void setTube(ItemStack stack) {
        this.entityData.set(DATA_HAS_TUBE, !stack.isEmpty());
        this.entityData.set(DATA_TUBE, stack.copy());
    }

    public ItemStack getAttached1() {
        return this.entityData.get(DATA_ATTACHED1).copy();
    }

    public void setAttached1(ItemStack stack) {
        this.entityData.set(DATA_ATTACHED1, stack.copy());
    }

    public ItemStack getAttached2() {
        return this.entityData.get(DATA_ATTACHED2).copy();
    }

    public void setAttached2(ItemStack stack) {
        this.entityData.set(DATA_ATTACHED2, stack.copy());
    }

    public boolean hasVessel() {
        return !this.entityData.get(DATA_VESSEL).isEmpty();
    }

    public ItemStack getVessel() {
        return this.entityData.get(DATA_VESSEL).copy();
    }

    public void setVessel(ItemStack stack) {
        this.entityData.set(DATA_VESSEL, stack.copy());
    }

    public ItemStack getReceiver() {
        return this.entityData.get(DATA_RECEIVER).copy();
    }

    public void setReceiver(ItemStack stack) {
        this.entityData.set(DATA_RECEIVER, stack.copy());
    }

    public boolean hasReceiver() {
        return !this.entityData.get(DATA_RECEIVER).isEmpty();
    }

    public ItemStack getReceiverAdapter() {
        return this.entityData.get(DATA_RECEIVER_ADAPTER).copy();
    }

    public void setReceiverAdapter(ItemStack stack) {
        this.entityData.set(DATA_RECEIVER_ADAPTER, stack.copy());
    }

    public boolean hasReceiverAdapter() {
        return !this.entityData.get(DATA_RECEIVER_ADAPTER).isEmpty();
    }

    public boolean hasCondenser() {
        return this.entityData.get(DATA_HAS_CONDENSER);
    }

    public void setCondenser(boolean v) {
        this.entityData.set(DATA_HAS_CONDENSER, v);
    }

    public boolean hasDistillationHead() {
        return this.entityData.get(DATA_HAS_DISTILLATION_HEAD);
    }

    public void setDistillationHead(boolean v) {
        this.entityData.set(DATA_HAS_DISTILLATION_HEAD, v);
    }

    public boolean hasHeadThermometer() {
        return this.entityData.get(DATA_HEAD_THERMOMETER);
    }

    public void setHeadThermometer(boolean v) {
        this.entityData.set(DATA_HEAD_THERMOMETER, v);
    }

    public boolean hasLamp() {
        return this.entityData.get(DATA_HAS_LAMP);
    }

    public void setLamp(boolean v) {
        this.entityData.set(DATA_HAS_LAMP, v);
    }

    public boolean isLampLit() {
        return this.entityData.get(DATA_LAMP_LIT);
    }

    public void setLampLit(boolean v) {
        this.entityData.set(DATA_LAMP_LIT, v);
    }

    public boolean isLampBlowtorch() {
        return this.entityData.get(DATA_LAMP_BLOWTORCH);
    }

    public void setLampBlowtorch(boolean v) {
        this.entityData.set(DATA_LAMP_BLOWTORCH, v);
    }

    public boolean isLampCapped() {
        return this.entityData.get(DATA_LAMP_CAPPED);
    }

    public void setLampCapped(boolean v) {
        this.entityData.set(DATA_LAMP_CAPPED, v);
    }

    public int getAttachment() {
        return this.entityData.get(DATA_ATTACHMENT);
    }

    public void setAttachment(int v) {
        this.entityData.set(DATA_ATTACHMENT, v);
    }

    @Override
    public AABB virtualHitbox() {
        double a=Math.toRadians(-getFacing().toYRot());
        Vec3 rod=position().add(.03125*Math.cos(a)+.21875*Math.sin(a),0,
                -.03125*Math.sin(a)+.21875*Math.cos(a));
        return new AABB(rod.x-.08,getY(),rod.z-.08,rod.x+.08,
                getY()+1+extensionRods(),rod.z+.08);
    }

    @Override
    public ItemStack toStack() {
        ItemStack stack = new ItemStack(ModItems.IRON_STAND_ITEM.get());
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("stand_rods", extensionRods());
        tag.putFloat("stand_lift", (float) clampLift());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
        return stack;
    }

    public int extensionRods() { return entityData.get(DATA_RODS); }
    public double clampLift() { return entityData.get(DATA_LIFT); }
    public void restoreExtensions(ItemStack stack) {
        var tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        entityData.set(DATA_RODS, Math.clamp(tag.getIntOr("stand_rods", 0), 0, MAX_EXTENSION_RODS));
        entityData.set(DATA_LIFT, (float) Math.clamp(tag.getFloatOr("stand_lift", 0), -.25,
                extensionRods() + .25));
        setBoundingBox(virtualHitbox());
    }
    public boolean addExtension() {
        if (extensionRods() >= MAX_EXTENSION_RODS) return false;
        AABB rod=virtualHitbox();
        AABB added = new AABB(rod.minX, getY()+1+extensionRods(), rod.minZ,
                rod.maxX, getY()+2+extensionRods(), rod.maxZ);
        if (!level().noCollision(added)) return false;
        entityData.set(DATA_RODS, extensionRods()+1);
        setBoundingBox(virtualHitbox());
        return true;
    }
    public boolean removeExtension() {
        if (extensionRods() == 0 || clampLift() > extensionRods()-1+.25) return false;
        entityData.set(DATA_RODS, extensionRods()-1);
        setBoundingBox(virtualHitbox());
        return true;
    }
    public boolean adjustClamp(double requested) {
        if (!Double.isFinite(requested) || requested < -.25 || requested > extensionRods()+.25) return false;
        double delta = requested-clampLift();
        if (Math.abs(delta)<1e-7) return true;
        PlacedVesselEntity source = findMountedVessel();
        var attached = level().getEntities(this, virtualHitbox().inflate(4), e ->
                e instanceof DistillationPartEntity part && part.standId().equals(getUUID().toString())
                || e instanceof PlacedVesselEntity vessel && vessel.getReceiverStandId().equals(getUUID().toString())
                || e instanceof com.example.chemistry.filtration.FilterFunnelEntity f && f.ownerId().equals(getUUID().toString())
                || e instanceof com.example.chemistry.titration.BuretteEntity b && b.ownerId().equals(getUUID().toString())
                || e instanceof com.example.chemistry.organic.SeparatoryFunnelEntity f && f.ownerId().equals(getUUID().toString()));
        if (source != null && !level().noCollision(source.virtualHitbox().move(0,delta,0))) return false;
        for (var e : attached) if (!level().noCollision(e.getBoundingBox().move(0,delta,0))) return false;
        entityData.set(DATA_LIFT, (float) requested);
        if (source != null) {
            source.setMountedStandId(getUUID().toString());
            source.setPos(source.position().add(0,delta,0));
            source.setMount(source.getMountScale(), source.getMountOffX(), source.getMountOffY()+delta,
                    source.getMountOffZ(), source.getMountYaw());
            source.setBoundingBox(source.virtualHitbox());
        }
        for (var e : attached) {
            e.setPos(e.position().add(0,delta,0));
            if (e instanceof PlacedVesselEntity vessel) vessel.setMount(vessel.getMountScale(),
                    vessel.getMountOffX(), vessel.getMountOffY()+delta, vessel.getMountOffZ(), vessel.getMountYaw());
            if (e instanceof TechnicalEntity t) e.setBoundingBox(t.virtualHitbox());
        }
        return true;
    }
    private boolean rodAimed(Player player) {
        double a = Math.toRadians(-getFacing().toYRot());
        Vec3 center = position().add(.03125*Math.cos(a)+.21875*Math.sin(a),0,
                -.03125*Math.sin(a)+.21875*Math.cos(a));
        return new AABB(center.x-.07,getY()+.7,center.z-.07,center.x+.07,
                getY()+1+extensionRods(),center.z+.07).clip(player.getEyePosition(),
                player.getEyePosition().add(player.getLookAngle().scale(6))).isPresent();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && player.isCreative()) {
            DistillationAssembly.removeAll(this);
            PlacedVesselEntity vessel = findMountedVessel();
            if (vessel != null) {
                vessel.discard();
            }
            AlcoholLampEntity lamp = findMountedLamp();
            if (lamp != null) {
                lamp.discard();
            }
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected List<ItemStack> dropsOnBreak() {
        List<ItemStack> drops = new ArrayList<>();
        drops.add(toStack());
        drops.addAll(DistillationAssembly.removeAll(this));
        PlacedVesselEntity vessel = findMountedVessel();
        if (vessel != null) {
            if (!vessel.getVessel().isEmpty()) {
                drops.add(vessel.getVessel().copy());
            }
            if (!vessel.getAttached2().isEmpty()) {
                drops.add(vessel.getAttached2().copy());
            }
            if (!vessel.getAttached1().isEmpty()) {
                drops.add(vessel.getAttached1().copy());
            }
            vessel.discard();
        }
        AlcoholLampEntity lamp = findMountedLamp();
        if (lamp != null) {
            drops.add(lamp.toStack());
            lamp.discard();
        }
        if (!getTube().isEmpty()) {
            drops.add(getTube().copy());
        }
        if (!getReceiverAdapter().isEmpty()) {
            drops.add(getReceiverAdapter().copy());
        }
        if (!getReceiver().isEmpty()) {
            drops.add(getReceiver().copy());
        }
        if (!getVessel().isEmpty()) {
            drops.add(getVessel().copy());
        }
        if (!getAttached2().isEmpty()) {
            drops.add(getAttached2().copy());
        }
        if (!getAttached1().isEmpty()) {
            drops.add(getAttached1().copy());
        }
        return drops;
    }

    /** 拿起铁架台：先拿起叠加的锥形瓶/酒精灯实体，避免它们悬浮残留。 */
    @Override
    protected boolean pickUp(Player player) {
        for (ItemStack item : DistillationAssembly.removeAll(this)) {
            giveToPlayer(player, item);
        }
        PlacedVesselEntity vessel = findMountedVessel();
        if (vessel != null) {
            if (!vessel.getVessel().isEmpty()) {
                giveToPlayer(player, vessel.getVessel());
            }
            if (!vessel.getAttached2().isEmpty()) {
                giveToPlayer(player, vessel.getAttached2());
            }
            if (!vessel.getAttached1().isEmpty()) {
                giveToPlayer(player, vessel.getAttached1());
            }
            vessel.discard();
        }
        AlcoholLampEntity lamp = findMountedLamp();
        if (lamp != null) {
            giveToPlayer(player, lamp.toStack());
            lamp.discard();
        }
        return super.pickUp(player);
    }

    @Override
    public void tick() {
        super.tick();
        setBoundingBox(virtualHitbox());
        if (level().isClientSide()) {
            return;
        }
        // 酒精灯是叠加在底座上的独立实体。
        AlcoholLampEntity lampEntity = findMountedLamp();
        boolean lamp = clampLift() <= .25 && lampEntity != null && lampEntity.isLit();
        boolean blowtorch = lamp && lampEntity.isBlowtorch();
        var nearest = level().getNearestPlayer(getX(), getY(), getZ(), 8.0, false);
        if (hasTube() && !getTube().isEmpty()) {
            ItemStack tube = getTube();
            com.example.chemistry.GasBurners.heat(level(), blockPosition(), tube);
            if (tube.getItem() instanceof TestTubeItem) {
                if (lamp) {
                    if(blowtorch)VesselHeating.heatFast(tube,1200);
                    else VesselHeating.heatSlow(tube,600);
                } else if (TemperatureSystem.getTemp(tube) != TemperatureSystem.ROOM_TEMP) {
                    VesselHeating.coolGradual(tube);
                }
            }
            ReactionEngine.checkAndStart(tube, nearest);
            ReactionEngine.Completion completed = ReactionEngine.tickResult(tube, nearest);
            if (completed != null) {
                ReactionPhenomena.spawn(level(),
                        new net.minecraft.world.phys.Vec3(getX(), getY() + 0.8, getZ()),
                        ReactionPhenomena.detect(completed.reaction(), tube));
                GasFlowEngine.enqueue(level(), blockPosition(), tube, completed);
            }
            GasFlowEngine.pump(level(), blockPosition(), tube);
            VesselGasPhase.tickLeak(tube);
            setTube(tube);
        }
        // 锥形瓶 / 烧瓶是独立 PlacedVesselEntity，其加热/反应由该实体自己 tick。
    }

    private void handleOutcome(VesselHeating.Outcome outcome) {
        if (outcome == VesselHeating.Outcome.POPPED) {
            int holes = VesselHeating.getStopperHoles(getVessel());
            spawnAtLocation((net.minecraft.server.level.ServerLevel) level(),
                    new ItemStack(ModItems.stopperForHoles(holes)));
            if (!getAttached2().isEmpty()) {
                spawnAtLocation((net.minecraft.server.level.ServerLevel) level(),
                        getAttached2().copy());
            }
            if (!getAttached1().isEmpty()) {
                spawnAtLocation((net.minecraft.server.level.ServerLevel) level(),
                        getAttached1().copy());
            }
            setAttached1(ItemStack.EMPTY);
            setAttached2(ItemStack.EMPTY);
            level().playSound(null, blockPosition(), SoundEvents.WOOL_BREAK,
                    SoundSource.BLOCKS, 1.0F, 0.8F);
        } else if (outcome == VesselHeating.Outcome.CRACKED) {
            setVessel(ItemStack.EMPTY);
            level().levelEvent(2001, blockPosition(),
                    net.minecraft.world.level.block.Block.getId(
                            net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState()));
            level().playSound(null, blockPosition(), SoundEvents.GLASS_BREAK,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        var separating=com.example.chemistry.organic.SeparatoryConnections.interact(this,player,hand);
        if(separating!=InteractionResult.PASS)return separating;
        var titrating=com.example.chemistry.titration.BuretteConnections.interact(this,player,hand);
        if(titrating!=InteractionResult.PASS)return titrating;
        var filtering=com.example.chemistry.filtration.FilterConnections.interact(this,player,hand);
        if(filtering!=InteractionResult.PASS)return filtering;
        if (!level().isClientSide()) {
            BlockPos pos = blockPosition();
            // 0) 打火石 / 火焰弹：点燃/熄灭底座上的酒精灯（酒精灯实体 hitbox
            //    被铁架台盖住，无法直接点到，这里代理）。
            if (held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE)) {
                AlcoholLampEntity lamp = findMountedLamp();
                if (lamp != null) {
                    if (lamp.isLit()) {
                        lamp.setLit(false);
                    } else if (!lamp.isCapped() && lamp.getFuel() > 0) {
                        lamp.setLit(true);
                        if (held.is(Items.FIRE_CHARGE)) {
                            held.shrink(1);
                        }
                        level().playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE,
                                SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            if(held.getItem() instanceof com.example.chemistry.organic.PhasePipetteItem){
                var mounted=findMountedVessel();
                if(mounted!=null){var sample=mounted.getVessel();com.example.chemistry.organic.PhasePipetteItem.interact(player,held,sample);mounted.setVessel(sample);}
                else if(hasVessel()){var sample=getVessel();com.example.chemistry.organic.PhasePipetteItem.interact(player,held,sample);setVessel(sample);}
                else if(hasTube()){var sample=getTube();com.example.chemistry.organic.PhasePipetteItem.interact(player,held,sample);setTube(sample);}
                else player.displayClientMessage(Component.literal("请对准装有液体的容器取液"),true);
                return InteractionResult.SUCCESS;
            }
            if (held.is(ModItems.IRON_STAND_EXTENSION.get())) {
                if (addExtension()) { if (!player.isCreative()) held.shrink(1); }
                else player.displayClientMessage(Component.literal("最多加两节延长杆，且上方需留有空间"), true);
                return InteractionResult.SUCCESS;
            }
            if (rodAimed(player) && held.is(net.minecraft.world.item.Items.SHEARS)) {
                if (removeExtension()) giveToPlayer(player, new ItemStack(ModItems.IRON_STAND_EXTENSION.get()));
                else player.displayClientMessage(Component.literal("请先降低夹具，再拆最上方延长杆"), true);
                return InteractionResult.SUCCESS;
            }
            if (rodAimed(player) && held.isEmpty()) {
                boolean changed = adjustClamp(clampLift()+(player.isShiftKeyDown()?-HEIGHT_STEP:HEIGHT_STEP));
                player.displayClientMessage(Component.literal(changed
                        ? String.format(java.util.Locale.ROOT,"夹具高度 %.3f m", VESSEL_OFF_Y+clampLift())
                        : "已到高度边界，或仪器移动路径受阻"),true);
                return InteractionResult.SUCCESS;
            }
            InteractionResult distillation = DistillationAssembly.interact(this, player, hand);
            if (distillation != InteractionResult.PASS) {
                return distillation;
            }
            // 1) 铁圈 / 石棉网 / 泥三角。
            if (held.is(ModItems.IRON_RING.get())
                    || held.is(ModItems.ASBESTOS_GAUZE.get())
                    || held.is(ModItems.CLAY_GAUZE.get())) {
                if (getAttachment() == 0) {
                    setAttachment(held.is(ModItems.IRON_RING.get()) ? 1
                            : held.is(ModItems.ASBESTOS_GAUZE.get()) ? 2 : 3);
                    held.shrink(1);
                    level().playSound(null, pos, SoundEvents.METAL_PLACE,
                            SoundSource.BLOCKS, 0.8F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }
            // 2) 锥形瓶 / 烧瓶：作为独立挂载实体叠到铁圈上。
            if (getAttachment() != 0 && !hasTube()
                    && held.getItem() instanceof LabVesselItem
                    && !(held.getItem() instanceof TestTubeItem)) {
                if (findMountedVessel() == null) {
                    PlacedVesselEntity vessel =
                            new PlacedVesselEntity(ModEntities.PLACED_VESSEL.get(), level());
                    // 实体位置抬到铁圈上方，让 hitbox 不与底座的酒精灯重叠。
                    vessel.setPos(pos.getX() + 0.5, pos.getY() + VESSEL_OFF_Y + clampLift(),
                            pos.getZ() + 0.5);
                    vessel.setMount((float) VESSEL_SCALE, VESSEL_OFF_X, VESSEL_OFF_Y + clampLift(),
                            VESSEL_OFF_Z, -getFacing().toYRot());
                    vessel.setMountedStandId(getUUID().toString());
                    vessel.setVessel(held.copyWithCount(1));
                    level().addFreshEntity(vessel);
                    held.shrink(1);
                    level().playSound(null, pos, SoundEvents.GLASS_PLACE,
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }
            // 3) 试管。
            if (held.getItem() instanceof TestTubeItem tt) {
                if (tt.isClamped()) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.iron_stand.no_clamped"), true);
                    return InteractionResult.SUCCESS;
                }
                if (!getTube().isEmpty()) {
                    giveToPlayer(player, getTube());
                }
                setTube(held.copyWithCount(1));
                held.shrink(1);
                level().playSound(null, pos, SoundEvents.GLASS_PLACE,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            // 3.5) 酒精灯 / 喷灯：作为独立实体叠加在底座上。
            if (held.is(ModItems.ALCOHOL_LAMP.get()) || held.is(ModItems.ALCOHOL_BLOWTORCH.get())) {
                if (findMountedLamp() == null) {
                    AlcoholLampEntity lamp =
                            new AlcoholLampEntity(ModEntities.ALCOHOL_LAMP.get(), level());
                    double[] off = lampOffset();
                    lamp.setPos(pos.getX() + 0.5 + off[0], pos.getY() + off[1],
                            pos.getZ() + 0.5 + off[2]);
                    lamp.setBlowtorch(held.is(ModItems.ALCOHOL_BLOWTORCH.get()));
                    lamp.setFuel(AlcoholLampEntity.CAPACITY);
                    level().addFreshEntity(lamp);
                    held.shrink(1);
                    level().playSound(null, pos, SoundEvents.METAL_PLACE,
                            SoundSource.BLOCKS, 0.8F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }
            // 4) 空手：按 锥形瓶实体 → 试管 → 铁圈/石棉网/泥三角 → 铁架台 顺序取回。
            if (held.isEmpty()) {
                return interactEmpty(player);
            }
        } else {
            if(held.getItem() instanceof com.example.chemistry.organic.PhasePipetteItem){
                var mounted=findMountedVessel();
                if(mounted!=null){var sample=mounted.getVessel();com.example.chemistry.organic.PhasePipetteItem.interact(player,held,sample);mounted.setVessel(sample);}
                else if(hasVessel()){var sample=getVessel();com.example.chemistry.organic.PhasePipetteItem.interact(player,held,sample);setVessel(sample);}
                else if(hasTube()){var sample=getTube();com.example.chemistry.organic.PhasePipetteItem.interact(player,held,sample);setTube(sample);}
                else player.displayClientMessage(Component.literal("请对准装有液体的容器取液"),true);
                return InteractionResult.SUCCESS;
            }
            if (held.is(ModItems.IRON_STAND_EXTENSION.get())
                    || rodAimed(player) && (held.isEmpty() || held.is(Items.SHEARS)))
                return InteractionResult.SUCCESS;
            InteractionResult distillation = DistillationAssembly.interact(this, player, hand);
            if (distillation != InteractionResult.PASS) {
                return distillation;
            }
            if (isRelevantHeld(held)) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    /** 酒精灯在铁架台底座上的偏移（与旧 IronStandRenderer 的灯位一致）。 */
    private double[] lampOffset() {
        double angle = Math.toRadians(getRotation() * 45.0);
        double ox = 0.3333;
        double oy = -2.3333;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double bx = 8.5 + ox * cos - oy * sin;
        double by = 9.5 + ox * sin + oy * cos;
        double yModel = Math.max(2.0, Math.min(4.5, by - 5.9));
        return new double[] {(bx - 8.5) / 16.0, yModel / 16.0, (8.8333 - 8.5) / 16.0};
    }

    private InteractionResult interactEmpty(Player player) {
        BlockPos pos = blockPosition();
        // 锥形瓶/烧瓶是独立 PlacedVesselEntity，由它自己的 interact 取下；
        // 这里不再代取，避免右键铁架台本体时误把锥形瓶/试管拿走。
        AlcoholLampEntity lamp = findMountedLamp();
        if (lamp != null) {
            ItemStack stack = lamp.toStack();
            lamp.discard();
            giveToPlayer(player, stack);
            level().playSound(null, pos, SoundEvents.ITEM_PICKUP,
                    SoundSource.BLOCKS, 0.8F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        if (!getTube().isEmpty()) {
            ItemStack tube = getTube();
            if (!getAttached2().isEmpty()) {
                giveToPlayer(player, getAttached2());
            }
            if (!getAttached1().isEmpty()) {
                giveToPlayer(player, getAttached1());
            }
            setAttached1(ItemStack.EMPTY);
            setAttached2(ItemStack.EMPTY);
            setTube(ItemStack.EMPTY);
            giveToPlayer(player, tube);
            level().playSound(null, pos, SoundEvents.ITEM_PICKUP,
                    SoundSource.BLOCKS, 0.8F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        if (getAttachment() != 0 && aimAtAttachment(player)) {
            int type = getAttachment();
            setAttachment(0);
            giveToPlayer(player, attachmentItem(type));
            level().playSound(null, pos, SoundEvents.METAL_BREAK,
                    SoundSource.BLOCKS, 0.8F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        if (pickUp(player)) {
            level().playSound(null, pos, SoundEvents.ITEM_PICKUP,
                    SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    /** 玩家视线是否命中铁圈/石棉网/泥三角（环中心 y 9.0/16，半径约 0.28）。 */
    private boolean aimAtAttachment(Player player) {
        Vec3 from = player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        Vec3 center = new Vec3(getX(), getY() + 9.0 / 16.0, getZ());
        Vec3 v = center.subtract(from);
        double t = v.dot(dir);
        Vec3 closest = t > 0 ? from.add(dir.scale(t)) : from;
        return center.distanceTo(closest) <= 0.28;
    }

    private boolean isRelevantHeld(ItemStack held) {
        if (held.isEmpty()) {
            return true;
        }
        if (held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE)) {
            return true;
        }
        if (held.is(ModItems.IRON_RING.get())
                || held.is(ModItems.ASBESTOS_GAUZE.get())
                || held.is(ModItems.CLAY_GAUZE.get())) {
            return true;
        }
        if (held.getItem() instanceof LabVesselItem
                && !(held.getItem() instanceof TestTubeItem)) {
            return true;
        }
        if (held.is(ModItems.ALCOHOL_LAMP.get()) || held.is(ModItems.ALCOHOL_BLOWTORCH.get())) {
            return true;
        }
        return held.getItem() instanceof TestTubeItem;
    }

    public PlacedVesselEntity findMountedVessel() {
        for (PlacedVesselEntity e : level().getEntitiesOfClass(PlacedVesselEntity.class,
                virtualHitbox().inflate(.6))) {
            if (!e.isReceiver() && e.getMountScale() < 0.9F
                    && (e.getMountedStandId().equals(getUUID().toString())
                        || e.getMountedStandId().isEmpty() && Math.abs(e.getX()-getX())<.05
                            && Math.abs(e.getZ()-getZ())<.05
                            && (Math.abs(e.getY()-e.getMountOffY()-getY())<.05
                                || Math.abs(e.getY()-getY())<.05))) {
                return e;
            }
        }
        return null;
    }

    public AlcoholLampEntity findMountedLamp() {
        for (AlcoholLampEntity e : level().getEntitiesOfClass(AlcoholLampEntity.class,
                new AABB(blockPosition()))) {
            return e;
        }
        return null;
    }

    private static ItemStack attachmentItem(int type) {
        return switch (type) {
            case 1 -> new ItemStack(ModItems.IRON_RING.get());
            case 2 -> new ItemStack(ModItems.ASBESTOS_GAUZE.get());
            default -> new ItemStack(ModItems.CLAY_GAUZE.get());
        };
    }

    private void giveToPlayer(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("facing", getFacing().getName());
        output.putInt("rotation", getRotation());
        output.putInt("extension_rods", extensionRods());
        output.putFloat("clamp_lift", (float) clampLift());
        output.putInt("attachment", getAttachment());
        output.store("tube", ItemStack.OPTIONAL_CODEC, getTube());
        output.store("attached1", ItemStack.OPTIONAL_CODEC, getAttached1());
        output.store("attached2", ItemStack.OPTIONAL_CODEC, getAttached2());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setFacing(Direction.byName(input.getStringOr("facing", "south")));
        setRotation(input.getIntOr("rotation", 0));
        entityData.set(DATA_RODS, Math.clamp(input.getIntOr("extension_rods",0),0,MAX_EXTENSION_RODS));
        entityData.set(DATA_LIFT, (float) Math.clamp(input.getFloatOr("clamp_lift",0),-.25,extensionRods()+.25));
        setBoundingBox(virtualHitbox());
        setAttachment(input.getIntOr("attachment", 0));
        setTube(input.read("tube", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        setAttached1(input.read("attached1", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        setAttached2(input.read("attached2", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }
}
