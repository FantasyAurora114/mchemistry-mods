package com.example.chemistry.entity;

import java.util.ArrayList;
import java.util.List;

import com.example.chemistry.LabInteractions;
import com.example.chemistry.DistillationAssembly;
import com.example.chemistry.ReactionEngine;
import com.example.chemistry.TemperatureSystem;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.api.goggles.ChemGoggleLines;
import com.example.chemistry.api.goggles.IChemGoggleInfo;
import com.example.chemistry.item.DropperHelper;
import com.example.chemistry.item.DropperItem;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.item.RubberTubeItem;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.entity.RubberTubeEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 落地容器（锥形瓶 / 烧瓶 / 三颈瓶 / 烧杯，技术性实体版）。vessel 与两个
 * 瓶口附件（玻璃导管、滴管、漏斗、温度计等）都存进 synced data，渲染与交互
 * 与旧的 {@code PlacedVesselBlock} 保持一致。
 */
public class PlacedVesselEntity extends TechnicalEntity implements IChemGoggleInfo {

    private static final EntityDataAccessor<ItemStack> DATA_VESSEL =
            SynchedEntityData.defineId(PlacedVesselEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_ATTACHED1 =
            SynchedEntityData.defineId(PlacedVesselEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_ATTACHED2 =
            SynchedEntityData.defineId(PlacedVesselEntity.class, EntityDataSerializers.ITEM_STACK);
    /** 挂载参数（挂在铁架台/三脚架等上时的缩放与偏移；落地默认 1.0/无偏移）。 */
    private static final EntityDataAccessor<Float> DATA_MOUNT_SCALE =
            SynchedEntityData.defineId(PlacedVesselEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MOUNT_OFF_X =
            SynchedEntityData.defineId(PlacedVesselEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MOUNT_OFF_Y =
            SynchedEntityData.defineId(PlacedVesselEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MOUNT_OFF_Z =
            SynchedEntityData.defineId(PlacedVesselEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MOUNT_YAW =
            SynchedEntityData.defineId(PlacedVesselEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_MOUNTED_STAND =
            SynchedEntityData.defineId(PlacedVesselEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_RECEIVER_STAND =
            SynchedEntityData.defineId(PlacedVesselEntity.class, EntityDataSerializers.STRING);

    /** 胶头滴管的红色胶头（模型局部坐标，y 约 11.9 处）世界位置。 */
    private static final Vec3 DROPPER_BULB = new Vec3(8.5 / 16.0, 11.9 / 16.0, 8.5 / 16.0);

    public PlacedVesselEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_VESSEL, ItemStack.EMPTY);
        builder.define(DATA_ATTACHED1, ItemStack.EMPTY);
        builder.define(DATA_ATTACHED2, ItemStack.EMPTY);
        builder.define(DATA_MOUNT_SCALE, 1.0F);
        builder.define(DATA_MOUNT_OFF_X, 0.0F);
        builder.define(DATA_MOUNT_OFF_Y, 0.0F);
        builder.define(DATA_MOUNT_OFF_Z, 0.0F);
        builder.define(DATA_MOUNT_YAW, 0.0F);
        builder.define(DATA_MOUNTED_STAND, "");
        builder.define(DATA_RECEIVER_STAND, "");
    }

    public ItemStack getVessel() {
        return this.entityData.get(DATA_VESSEL).copy();
    }

    public void setVessel(ItemStack stack) {
        this.entityData.set(DATA_VESSEL, stack.copy());
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

    public float getMountScale() {
        return this.entityData.get(DATA_MOUNT_SCALE);
    }

    public String getMountedStandId() { return entityData.get(DATA_MOUNTED_STAND); }
    public void setMountedStandId(String id) { entityData.set(DATA_MOUNTED_STAND,id); }
    public BlockPos geometryOrigin() {
        if(getMountScale()<.9F){
            var stand=DistillationAssembly.standFor(this);
            if(stand!=null)return stand.blockPosition();
            if(!getMountedStandId().isEmpty())return BlockPos.containing(getX(),getY()-getMountOffY(),getZ());
        }
        return blockPosition();
    }

    public double getMountOffX() {
        return this.entityData.get(DATA_MOUNT_OFF_X);
    }

    public double getMountOffY() {
        return this.entityData.get(DATA_MOUNT_OFF_Y);
    }

    public double getMountOffZ() {
        return this.entityData.get(DATA_MOUNT_OFF_Z);
    }

    public float getMountYaw() {
        return this.entityData.get(DATA_MOUNT_YAW);
    }

    public String getReceiverStandId() {
        return entityData.get(DATA_RECEIVER_STAND);
    }

    public void setReceiverStandId(String id) {
        entityData.set(DATA_RECEIVER_STAND, id);
    }

    public boolean isReceiver() {
        return !getReceiverStandId().isEmpty();
    }

    /** 设置挂载参数（scale 缩放，off 偏移，yaw 绕方块中心旋转）。 */
    public void setMount(float scale, double offX, double offY, double offZ, float yaw) {
        this.entityData.set(DATA_MOUNT_SCALE, scale);
        this.entityData.set(DATA_MOUNT_OFF_X, (float) offX);
        this.entityData.set(DATA_MOUNT_OFF_Y, (float) offY);
        this.entityData.set(DATA_MOUNT_OFF_Z, (float) offZ);
        this.entityData.set(DATA_MOUNT_YAW, yaw);
    }

    @Override
    public AABB virtualHitbox() {
        int type=VesselHeating.vesselType(getVessel());
        if(type==5||type==8||type==9){
            double[][] b=VesselHeating.vesselBounds(type);double scale=getMountScale();
            double radius=Math.max(b[1][0]-b[0][0],b[1][2]-b[0][2])/32*scale+.035;
            return new AABB(getX()-radius,getY(),getZ()-radius,getX()+radius,getY()+b[1][1]/16*scale+.02,getZ()+radius);
        }
        if (getMountScale() < 0.9F) {
            // 挂载在铁架台：实体位置已在铁圈上方，命中框只盖住锥形瓶本身。
            return new AABB(getX() - 0.32, getY(), getZ() - 0.32,
                    getX() + 0.32, getY() + 0.45, getZ() + 0.32);
        }
        // 落地容器矮（烧杯），但瓶口附件（导管/漏斗）悬空：命中框放大到整格。
        return new AABB(getX() - 0.5, getY(), getZ() - 0.5,
                getX() + 0.5, getY() + 1.0, getZ() + 0.5);
    }

    @Override
    public ItemStack toStack() {
        return getVessel().copy();
    }

    @Override
    protected List<ItemStack> dropsOnBreak() {
        List<ItemStack> drops = new ArrayList<>();
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

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        ItemStack vessel = getVessel();
        if (vessel.isEmpty()) {
            return;
        }
        if (!VesselHeating.isTempLocked(vessel)) {
            if (getMountScale() < 0.9F) {
                // 挂载在铁架台：酒精灯是叠加在同一方块的独立实体。
                AlcoholLampEntity lamp = findLampAtSamePos();
                if (lamp != null && lamp.isLit()) {
                    if (lamp.isBlowtorch()) {
                        VesselHeating.heatFast(vessel, VesselHeating.BLOWTORCH_TEMP);
                    } else {
                        VesselHeating.heatSlow(vessel, 600.0);
                    }
                } else {
                    VesselHeating.coolGradual(vessel);
                }
            } else if (VesselHeating.blowtorchBelow(level(), blockPosition())) {
                VesselHeating.heatFast(vessel, VesselHeating.BLOWTORCH_TEMP);
            } else if (VesselHeating.lampBelow(level(), blockPosition())) {
                VesselHeating.heatSlow(vessel, VesselHeating.LAMP_TEMP);
            } else {
                VesselHeating.coolGradual(vessel);
            }
        }
        boolean hasAttached = !getAttached1().isEmpty() || !getAttached2().isEmpty()
                || DistillationAssembly.hasHead(this) || com.example.chemistry.utility.UtilityConnections.vacuumConnected(this);
        PlacedVesselEntity receiver = DistillationAssembly.receiverForSource(this);
        ItemStack receivingVessel = receiver == null ? ItemStack.EMPTY : receiver.getVessel();
        com.example.chemistry.utility.BeakerWaterBath.tick(level(),blockPosition(),vessel);
        com.example.chemistry.garden.ChemicalGarden.tick(vessel);
        VesselHeating.Outcome outcome = VesselHeating.tick(vessel, level(), blockPosition(),
                receivingVessel, receiver != null && condenserReady(), hasAttached,
                level().getNearestPlayer(getX(), getY(), getZ(), 8.0, false));
        if (receiver != null) {
            receiver.setVessel(receivingVessel);
        }
        if (outcome == VesselHeating.Outcome.POPPED) {
            int holes = VesselHeating.getStopperHoles(vessel);
            dropItem(new ItemStack(ModItems.stopperForHoles(holes)));
            if (!getAttached2().isEmpty()) {
                dropItem(getAttached2().copy());
            }
            if (!getAttached1().isEmpty()) {
                dropItem(getAttached1().copy());
            }
            RubberTubeItem.dropTubesConnectedToStand(level(), blockPosition().immutable(), 1);
            RubberTubeItem.dropTubesConnectedToStand(level(), blockPosition().immutable(), 2);
            setAttached1(ItemStack.EMPTY);
            setAttached2(ItemStack.EMPTY);
            level().playSound(null, blockPosition(), SoundEvents.WOOL_BREAK,
                    SoundSource.BLOCKS, 1.0F, 0.8F);
            setVessel(vessel);
        } else if (outcome == VesselHeating.Outcome.CRACKED) {
            if (DistillationAssembly.hasHead(this)) {
                IronStandEntity stand = DistillationAssembly.standFor(this);
                if (stand != null) {
                    for (ItemStack part : DistillationAssembly.removeAll(stand)) {
                        dropItem(part);
                    }
                }
            }
            setVessel(ItemStack.EMPTY);
            level().levelEvent(2001, blockPosition(),
                    net.minecraft.world.level.block.Block.getId(
                            net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState()));
            level().playSound(null, blockPosition(), SoundEvents.GLASS_BREAK,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            discard();
        } else {
            setVessel(vessel);
        }
    }

    private boolean condenserReady(){
        var stand=DistillationAssembly.standFor(this);if(stand==null)return true;
        var condenser=DistillationAssembly.part(stand,DistillationPartEntity.CONDENSER);
        return condenser==null||!com.example.chemistry.utility.UtilityConnections.hasCoolingHose(condenser)
                ||com.example.chemistry.utility.UtilityConnections.cooling(condenser);
    }
    private AlcoholLampEntity findLampAtSamePos() {
        for (AlcoholLampEntity e : level().getEntitiesOfClass(AlcoholLampEntity.class,
                new AABB(blockPosition()))) {
            return e;
        }
        return null;
    }

    /** 在实体位置掉落一个物品（tick 里 level() 是 Level，需转 ServerLevel）。 */
    private void dropItem(ItemStack stack) {
        spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), stack);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        var coverResult=com.example.chemistry.organic.OrganicApparatus.placed(this,player,hand);
        if(coverResult!=InteractionResult.PASS)return coverResult;
        if(player.getItemInHand(hand).is(ModItems.USED_FILTER_PAPER.get())){
            if(!level().isClientSide()){var target=getVessel();com.example.chemistry.filtration.Filtration.pour(player.getItemInHand(hand),target,Double.MAX_VALUE);setVessel(target);}
            return InteractionResult.SUCCESS;
        }
        var bathResult=com.example.chemistry.utility.BeakerWaterBath.interact(this,player,hand);if(bathResult!=InteractionResult.PASS)return bathResult;
        var sleeveResult = com.example.chemistry.ThermometerSleeves.interact(this, player, hand);
        if (sleeveResult != InteractionResult.PASS) return sleeveResult;
        ItemStack held = player.getItemInHand(hand);
        if (getMountScale() < 0.9F && !isReceiver()) {
            InteractionResult assembly = DistillationAssembly.interactFromVessel(this, player, hand);
            if (assembly != InteractionResult.PASS) {
                return assembly;
            }
        }
        if (!level().isClientSide()) {
            BlockPos pos = geometryOrigin();
            Vec3 click = rayHit(player);
            ItemStack vessel = getVessel();
            if (vessel.isEmpty()) {
                return InteractionResult.PASS;
            }
            if (DistillationAssembly.hasHead(this)
                    && (held.is(ModItems.RUBBER_STOPPER_1_HOLE.get())
                            || held.is(ModItems.RUBBER_STOPPER_2_HOLE.get())
                            || held.is(ModItems.RUBBER_STOPPER_3_HOLE.get())
                            || held.is(ModItems.GLASS_STOPPER.get()))) {
                int neck = VesselHeating.isThreeNeck(vessel)
                        ? LabInteractions.pickNeck(player, pos, click,
                                getMountScale(), getMountOffX(), getMountOffY(),
                                getMountOffZ(), getMountYaw()) : 1;
                if (neck == 1) {
                    player.displayClientMessage(Component.literal("蒸馏头已占用瓶口"), true);
                    return InteractionResult.SUCCESS;
                }
            }
            if ((held.is(ModItems.GLASS_STOPPER.get()) || held.is(ModItems.RUBBER_STOPPER_1_HOLE.get())
                    || held.is(ModItems.RUBBER_STOPPER_2_HOLE.get()) || held.is(ModItems.RUBBER_STOPPER_3_HOLE.get()))
                    && com.example.chemistry.ThermometerSleeves.blocksStopper(this, player, click)) {
                player.displayClientMessage(Component.literal("该磨口已安装温度计套管"), true);
                return InteractionResult.SUCCESS;
            }
            // 1) 玻璃导管 / 滴管 / 漏斗 / 温度计 插入密封烧瓶的橡胶塞孔。
            if (VesselHeating.isSealed(vessel)
                    && VesselHeating.getStopperHoles(vessel) > 0
                    && ((held.is(ModItems.STRAIGHT_GLASS_TUBE.get()) || held.is(ModItems.STRAIGHT_GLASS_TUBE_LONG.get()))
                            || held.is(ModItems.RIGHT_ANGLE_GLASS_TUBE.get())
                            || held.is(ModItems.RIGHT_ANGLE_GLASS_TUBE_LONG.get())
                            || held.is(ModItems.LONG_STEM_FUNNEL.get())
                            || held.is(ModItems.SEPARATORY_FUNNEL.get())
                            || held.getItem() instanceof DropperItem
                            || held.is(ModItems.THERMOMETER.get()))) {
                // 对着瓶口操作（单口瓶），而不是右键瓶子任意位置。
                int vesselType = VesselHeating.vesselType(vessel);
                if (!VesselHeating.isThreeNeck(vessel)
                        && !VesselHeating.mouthForRay(pos, vesselType,
                                getMountScale(), getMountOffX(), getMountOffY(), getMountOffZ(), getMountYaw(), player)) {
                    return InteractionResult.SUCCESS;
                }
                int holes = VesselHeating.getStopperHoles(vessel);
                if (getAttached1().isEmpty()) {
                    setAttached1(held.copyWithCount(1));
                    held.shrink(1);
                    level().playSound(null, pos, SoundEvents.GLASS_PLACE,
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                } else if (getAttached2().isEmpty() && holes >= 2) {
                    setAttached2(held.copyWithCount(1));
                    held.shrink(1);
                    level().playSound(null, pos, SoundEvents.GLASS_PLACE,
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                } else if (holes <= 1) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.iron_stand.one_hole_full"), true);
                } else {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.iron_stand.no_hole"), true);
                }
                return InteractionResult.SUCCESS;
            }
            // 2) 橡胶塞：三颈瓶按瓶口，其它烧瓶整瓶密封。
            if (held.is(ModItems.RUBBER_STOPPER_1_HOLE.get())
                    || held.is(ModItems.RUBBER_STOPPER_2_HOLE.get())
                    || held.is(ModItems.RUBBER_STOPPER_3_HOLE.get())) {
                int holes = held.is(ModItems.RUBBER_STOPPER_3_HOLE.get()) ? 3
                        : held.is(ModItems.RUBBER_STOPPER_2_HOLE.get()) ? 2 : 1;
                if (VesselHeating.isThreeNeck(vessel)) {
                    if (LabInteractions.tryPlugRubberNeck(vessel, held, player, pos, click,
                            holes, getMountScale(), getMountOffX(), getMountOffY(), getMountOffZ(), getMountYaw())) {
                        setVessel(vessel);
                    }
                } else if (!VesselHeating.isSealed(vessel)) {
                    int vesselType = VesselHeating.vesselType(vessel);
                    if (!VesselHeating.mouthForRay(pos, vesselType,
                            getMountScale(), getMountOffX(), getMountOffY(), getMountOffZ(), getMountYaw(), player)) {
                        return InteractionResult.SUCCESS;
                    }
                    VesselHeating.seal(vessel, holes);
                    setVessel(vessel);
                    held.shrink(1);
                    level().playSound(null, pos, SoundEvents.WOOL_PLACE,
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }
            // 3) 三颈瓶：玻璃塞塞进玩家正看着的瓶口。
            if (held.is(ModItems.GLASS_STOPPER.get()) && VesselHeating.isThreeNeck(vessel)) {
                if (LabInteractions.tryPlugNeck(vessel, held, player, pos, click,
                        getMountScale(), getMountOffX(), getMountOffY(), getMountOffZ(), getMountYaw())) {
                    setVessel(vessel);
                }
                return InteractionResult.SUCCESS;
            }
            // 4) 装药 / 倒液 / 搅拌 / 温度计读数。
            if (LabInteractions.interactPlacedVessel(held, vessel,
                    getAttached1(), getAttached2(), player)) {
                setVessel(vessel);
                level().playSound(null, pos, SoundEvents.BOTTLE_EMPTY,
                        SoundSource.BLOCKS, 0.8F, 1.2F);
                return InteractionResult.SUCCESS;
            }
            // 5) 空手 → 拆附件 / 拆塞 / 取容器。
            if (held.isEmpty()) {
                return interactEmpty(player, click);
            }
        } else {
            // 客户端必须与服务端返回一致（SUCCESS），否则客户端会认为
            // 交互失败并重试，导致塞/拆反复执行、物品被重复返还。
            if (isRelevantHeld(held, getVessel())) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    /** 客户端判断：这个手持物品是否会被 {@link #interact} 服务端分支处理。 */
    private boolean isRelevantHeld(ItemStack held, ItemStack vessel) {
        if (held.isEmpty()) {
            return true;
        }
        if (held.is(ModItems.RUBBER_STOPPER_1_HOLE.get())
                || held.is(ModItems.RUBBER_STOPPER_2_HOLE.get())
                || held.is(ModItems.RUBBER_STOPPER_3_HOLE.get())) {
            return true;
        }
        if (held.is(ModItems.GLASS_STOPPER.get()) && VesselHeating.isThreeNeck(vessel)) {
            return true;
        }
        if ((held.is(ModItems.STRAIGHT_GLASS_TUBE.get()) || held.is(ModItems.STRAIGHT_GLASS_TUBE_LONG.get()))
                || held.is(ModItems.RIGHT_ANGLE_GLASS_TUBE.get())
                || held.is(ModItems.RIGHT_ANGLE_GLASS_TUBE_LONG.get())
                || held.is(ModItems.LONG_STEM_FUNNEL.get())
                || held.is(ModItems.SEPARATORY_FUNNEL.get())
                || held.is(ModItems.THERMOMETER.get())
                || held.getItem() instanceof DropperItem) {
            return true;
        }
        return LabInteractions.isVesselRelevant(held, vessel, getAttached1(), getAttached2());
    }

    private InteractionResult interactEmpty(Player player, Vec3 click) {
        BlockPos pos = geometryOrigin();
        ItemStack vessel = getVessel();
        if (vessel.isEmpty()) {
            return InteractionResult.SUCCESS;
        }
        // 右击胶头滴管红色胶头：挤压给液。
        if (hasDropper() && clickOnDropperBulb(click)) {
            squeezeDropper(player);
            return InteractionResult.SUCCESS;
        }
        // 瓶口附件：射线选中哪个就取下哪个。
        int vesselType = VesselHeating.vesselType(vessel);
        int slot = VesselHeating.pickAttached(player, pos, vessel, vesselType,
                getMountScale(), getMountOffX(), getMountOffY(), getMountOffZ(), getMountYaw(), getAttached1(), getAttached2());
        if (slot == 1 || slot == 2) {
            ItemStack removed = slot == 1 ? getAttached1() : getAttached2();
            if (slot == 1) {
                setAttached1(ItemStack.EMPTY);
            } else {
                setAttached2(ItemStack.EMPTY);
            }
            RubberTubeItem.dropTubesConnectedToStand(level(), pos.immutable(), slot);
            level().playSound(null, pos, SoundEvents.ITEM_PICKUP,
                    SoundSource.BLOCKS, 0.8F, 1.0F);
            giveToPlayer(player, removed);
            return InteractionResult.SUCCESS;
        }
        // 三颈瓶：拆正看着那个瓶口的塞子。
        if (VesselHeating.isThreeNeck(vessel)
                && VesselHeating.neckStopperCount(vessel) > 0) {
            int neck = LabInteractions.pickNeck(player, pos, click,
                    getMountScale(), getMountOffX(), getMountOffY(), getMountOffZ(), getMountYaw());
            if (neck >= 0) {
                boolean unplugged = LabInteractions.tryUnplugRubberNeck(vessel, neck, player);
                if (!unplugged) {
                    unplugged = LabInteractions.tryUnplugNeck(vessel, neck, player);
                }
                if (unplugged) {
                    setVessel(vessel);
                } else {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.flask.neck_empty"), true);
                }
                return InteractionResult.SUCCESS;
            }
        }
        // 单口瓶：对准瓶口拆橡胶塞。
        if (vesselType != 0 && VesselHeating.isSealed(vessel)
                && !VesselHeating.isThreeNeck(vessel)
                && VesselHeating.mouthForRay(pos, vesselType, getMountScale(), getMountOffX(), getMountOffY(), getMountOffZ(), getMountYaw(), player)) {
            int holes = VesselHeating.getStopperHoles(vessel);
            VesselHeating.unseal(vessel);
            setVessel(vessel);
            // 橡胶塞连同插在它孔里的玻璃导管/滴管/温度计一起取下。
            if (!getAttached2().isEmpty()) {
                giveToPlayer(player, getAttached2());
            }
            if (!getAttached1().isEmpty()) {
                giveToPlayer(player, getAttached1());
            }
            setAttached1(ItemStack.EMPTY);
            setAttached2(ItemStack.EMPTY);
            giveToPlayer(player, new ItemStack(ModItems.stopperForHoles(holes)));
            level().playSound(null, pos, SoundEvents.WOOL_BREAK,
                    SoundSource.BLOCKS, 0.9F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        if (DistillationAssembly.hasHead(this)) {
            player.displayClientMessage(Component.literal("请先取下蒸馏头"), true);
            return InteractionResult.SUCCESS;
        }
        // 取下整个容器（附件一起还给玩家）。
        for (ItemStack attached : new ItemStack[] {getAttached2(), getAttached1()}) {
            if (!attached.isEmpty()) {
                giveToPlayer(player, attached);
            }
        }
        setAttached1(ItemStack.EMPTY);
        setAttached2(ItemStack.EMPTY);
        setVessel(ItemStack.EMPTY);
        // 取下整个容器时，连在瓶口玻璃导管头上的橡胶管一起掉落。
        RubberTubeItem.dropTubesConnectedToStand(level(), pos.immutable(), 1);
        RubberTubeItem.dropTubesConnectedToStand(level(), pos.immutable(), 2);
        level().playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
        giveToPlayer(player, vessel);
        discard();
        return InteractionResult.SUCCESS;
    }

    private boolean hasDropper() {
        return DropperHelper.isDropper(getAttached1())
                || DropperHelper.isDropper(getAttached2());
    }

    /** 橡胶管连接到瓶口玻璃导管头（湿橡胶管右键本实体时由事件调用）。 */
    public void handleTubeHead(Player player, ItemStack stack) {
        if (!RubberTubeItem.isWet(stack)) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.rubber_tube.need_wet"), true);
            return;
        }
        int slot = 0;
        if (isGlassTube(getAttached1())) {
            slot = 1;
        } else if (isGlassTube(getAttached2())) {
            slot = 2;
        }
        if (slot == 0) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.iron_stand.no_head"), true);
            return;
        }
        BlockPos pos = geometryOrigin().immutable();
        RubberTubeEntity.Port head = RubberTubeEntity.Port.stand(pos, slot);
        if (RubberTubeItem.hasTubeAt(level(), head)) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.rubber_tube.occupied"), true);
            return;
        }
        RubberTubeEntity.Port pending = RubberTubeItem.readPending(stack);
        if (pending == null) {
            RubberTubeItem.startPending(level(), player, stack, head);
            player.displayClientMessage(
                    Component.translatable("mchemistry.rubber_tube.start_stand"), true);
        } else if (RubberTubeItem.sameAnchor(pending, head)) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.rubber_tube.same"), true);
        } else {
            RubberTubeItem.createTube(level(), player, stack, pending, head);
        }
    }

    private static boolean isGlassTube(ItemStack stack) {
        return (stack.is(ModItems.STRAIGHT_GLASS_TUBE.get()) || stack.is(ModItems.STRAIGHT_GLASS_TUBE_LONG.get()))
                || stack.is(ModItems.RIGHT_ANGLE_GLASS_TUBE.get())
                || stack.is(ModItems.RIGHT_ANGLE_GLASS_TUBE_LONG.get());
    }

    private boolean clickOnDropperBulb(Vec3 click) {
        Vec3 center = Vec3.atLowerCornerOf(blockPosition()).add(DROPPER_BULB);
        return click.distanceToSqr(center) <= 0.0081;
    }

    private void squeezeDropper(Player player) {
        ItemStack dropper = DropperHelper.isDropper(getAttached2())
                ? getAttached2()
                : DropperHelper.isDropper(getAttached1()) ? getAttached1() : ItemStack.EMPTY;
        if (dropper.isEmpty() || DropperHelper.isEmpty(dropper)) {
            return;
        }
        String liquid = DropperHelper.getLiquid(dropper);
        ItemStack vessel = getVessel();
        if (DropperHelper.pour(dropper,vessel,player.isShiftKeyDown())) {
            ReactionEngine.checkAndStart(vessel, player);
            setVessel(vessel);
            level().playSound(null, blockPosition(), SoundEvents.BOTTLE_EMPTY,
                    SoundSource.BLOCKS, 0.8F, 1.2F);
        } else {
            player.displayClientMessage(
                    Component.translatable("mchemistry.iron_stand.tube_full"), true);
        }
    }

    private Vec3 rayHit(Player player) {
        Vec3 from = player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        return virtualHitbox().clip(from, from.add(dir.scale(8.0)))
                .orElseGet(() -> new Vec3(getX(), getY() + 0.5, getZ()));
    }

    private void giveToPlayer(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    /** 左键破坏：连在瓶口玻璃导管头上的橡胶管一起掉落。 */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (DistillationAssembly.hasHead(this)) {
            return true;
        }
        if (!level.isClientSide()) {
            RubberTubeItem.dropTubesConnectedToStand(level, blockPosition().immutable(), 1);
            RubberTubeItem.dropTubesConnectedToStand(level, blockPosition().immutable(), 2);
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean addGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {
        ItemStack vessel = getVessel();
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
        if (ModItems.hasThermometer(getAttached1(), getAttached2())
                || com.example.chemistry.ThermometerSleeves.hasThermometer(this)) {
            tooltip.add(ChemGoggleLines.temp(TemperatureSystem.getTemp(vessel),
                    VesselHeating.isTempLocked(vessel)));
        } else {
            tooltip.add(Component.literal("温度：无法查看（未插温度计）"));
        }
        ChemGoggleLines.appendContents(tooltip, vessel);
        com.example.chemistry.utility.BeakerWaterBath.info(level(),vessel,tooltip);
        if (isPlayerSneaking) ChemGoggleLines.appendSpecies(tooltip, vessel);
        ChemGoggleLines.appendPressure(tooltip, vessel);
        int n = (getAttached1().isEmpty() ? 0 : 1) + (getAttached2().isEmpty() ? 0 : 1);
        if (n > 0) {
            tooltip.add(Component.literal("已插入仪器：" + n + "件"));
        }
        return true;
    }

    /** 显式持久化容器状态与挂载参数，避免仅依赖 synced data 的保存。 */
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("vessel", ItemStack.OPTIONAL_CODEC, getVessel());
        output.store("attached1", ItemStack.OPTIONAL_CODEC, getAttached1());
        output.store("attached2", ItemStack.OPTIONAL_CODEC, getAttached2());
        output.putFloat("mount_scale", getMountScale());
        output.putString("mounted_stand",getMountedStandId());
        output.putFloat("mount_off_x", (float) getMountOffX());
        output.putFloat("mount_off_y", (float) getMountOffY());
        output.putFloat("mount_off_z", (float) getMountOffZ());
        output.putFloat("mount_yaw", getMountYaw());
        output.putString("receiver_stand", getReceiverStandId());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setVessel(input.read("vessel", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        setAttached1(input.read("attached1", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        setAttached2(input.read("attached2", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        setMount(input.getFloatOr("mount_scale", 1.0F),
                input.getFloatOr("mount_off_x", 0.0F),
                input.getFloatOr("mount_off_y", 0.0F),
                input.getFloatOr("mount_off_z", 0.0F),
                input.getFloatOr("mount_yaw", 0.0F));
        setMountedStandId(input.getStringOr("mounted_stand", ""));
        setReceiverStandId(input.getStringOr("receiver_stand", ""));
    }
}
