package com.example.chemistry.entity;

import java.util.ArrayList;
import java.util.List;

import com.example.chemistry.LabInteractions;
import com.example.chemistry.PhaseSystem;
import com.example.chemistry.ReactionEngine;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.block.HeatingMantleBlock;
import com.example.chemistry.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 磁力搅拌机（技术性实体版）：外壳 + 烧瓶 + 搅拌子。状态放 synced data，
 * 渲染由 {@code MagneticStirrerEntityRenderer} 完成，反应推进在 {@link #tick()}。
 */
public class MagneticStirrerEntity extends TechnicalEntity {

    private static final EntityDataAccessor<ItemStack> DATA_FLASK =
            SynchedEntityData.defineId(MagneticStirrerEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_STIR_BAR =
            SynchedEntityData.defineId(MagneticStirrerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Direction> DATA_FACING =
            SynchedEntityData.defineId(MagneticStirrerEntity.class, EntityDataSerializers.DIRECTION);

    private static final double SCALE = 0.5;
    private static final double OFF = (8.5 - 8.5 * SCALE) / 16.0;

    public MagneticStirrerEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FLASK, ItemStack.EMPTY);
        builder.define(DATA_STIR_BAR, false);
        builder.define(DATA_FACING, Direction.NORTH);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("flask", ItemStack.OPTIONAL_CODEC, getFlask());
        output.putBoolean("stir_bar", hasStirBar());
        output.putString("facing", getFacing().getName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setFlask(input.read("flask", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        setStirBar(input.getBooleanOr("stir_bar", false));
        Direction facing = Direction.byName(input.getStringOr("facing", "north"));
        setFacing(facing == null ? Direction.NORTH : facing);
    }

    public ItemStack getFlask() {
        return this.entityData.get(DATA_FLASK).copy();
    }

    public void setFlask(ItemStack stack) {
        this.entityData.set(DATA_FLASK, stack);
    }

    public boolean hasStirBar() {
        return this.entityData.get(DATA_STIR_BAR);
    }

    public void setStirBar(boolean v) {
        this.entityData.set(DATA_STIR_BAR, v);
    }

    public Direction getFacing() {
        return this.entityData.get(DATA_FACING);
    }

    public void setFacing(Direction facing) {
        this.entityData.set(DATA_FACING, facing);
    }

    /** 搅拌机底盘矮、烧瓶悬空：虚拟命中框放大到整格中部，空气处也能选中。 */
    @Override
    public AABB virtualHitbox() {
        return new AABB(getX() - 0.5, getY(), getZ() - 0.5,
                getX() + 0.5, getY() + 1.0, getZ() + 0.5);
    }

    @Override
    public ItemStack toStack() {
        return new ItemStack(ModItems.MAGNETIC_STIRRER.get());
    }

    /** 左键破坏：掉落机器 + 烧瓶 + 搅拌子。 */
    @Override
    protected List<ItemStack> dropsOnBreak() {
        List<ItemStack> drops = new ArrayList<>();
        drops.add(toStack());
        if (!getFlask().isEmpty()) {
            drops.add(getFlask().copy());
        }
        if (hasStirBar()) {
            drops.add(new ItemStack(ModItems.STIR_BAR.get()));
        }
        return drops;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        ItemStack flask = getFlask();
        if (flask.isEmpty()) {
            return;
        }
        Player nearest = level().getNearestPlayer(getX(), getY(), getZ(), 8.0, false);
        int steps = hasStirBar() ? 2 : 1;
        for (int i = 0; i < steps; i++) {
            ReactionEngine.checkAndStart(flask, nearest);
            ReactionEngine.tick(flask, nearest);
        }
        PhaseSystem.dissolveAndCrystallize(flask, hasStirBar());
        setFlask(flask);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!level().isClientSide()) {
            BlockPos pos = blockPosition();
            Vec3 click = rayHit(player);
            ItemStack flask = getFlask();

            // 空手：先拆塞 / 取烧瓶，烧瓶空着时拿起整台机器。
            if (held.isEmpty()) {
                if (!flask.isEmpty()) {
                    // 三颈瓶：对准瓶口拆塞。
                    if (VesselHeating.isThreeNeck(flask)) {
                        int neck = LabInteractions.pickNeck(player, pos, click,
                                SCALE, OFF, 5.8 / 16.0, OFF, 0.0);
                        if (neck >= 0) {
                            if (LabInteractions.tryUnplugRubberNeck(flask, neck, player)
                                    || LabInteractions.tryUnplugNeck(flask, neck, player)) {
                                setFlask(flask);
                            } else {
                                player.displayClientMessage(
                                        Component.translatable("mchemistry.flask.neck_empty"), true);
                            }
                            return InteractionResult.SUCCESS;
                        }
                    }
                    // 单口瓶：对准瓶口拆橡胶塞。
                    int vtype = VesselHeating.vesselType(flask);
                    if (vtype != 0 && VesselHeating.isSealed(flask)
                            && VesselHeating.mouthForRay(pos, vtype, SCALE, OFF,
                                    5.8 / 16.0, OFF, 0.0, player)) {
                        int holes = VesselHeating.getStopperHoles(flask);
                        VesselHeating.unseal(flask);
                        setFlask(flask);
                        giveToPlayer(player, new ItemStack(ModItems.stopperForHoles(holes)));
                        return InteractionResult.SUCCESS;
                    }
                    // 取下烧瓶，搅拌子自动拿出。
                    boolean bar = hasStirBar();
                    setFlask(ItemStack.EMPTY);
                    setStirBar(false);
                    giveToPlayer(player, flask);
                    if (bar) {
                        giveToPlayer(player, new ItemStack(ModItems.STIR_BAR.get()));
                        player.displayClientMessage(
                                Component.translatable("mchemistry.stirrer.bar_back"), true);
                    }
                    level().playSound(null, pos, SoundEvents.ITEM_PICKUP,
                            SoundSource.BLOCKS, 0.8F, 1.0F);
                    return InteractionResult.SUCCESS;
                }
                // 没有烧瓶：拿起整台机器（搅拌子一并归还）。
                boolean bar = hasStirBar();
                setStirBar(false);
                if (pickUp(player)) {
                    if (bar) {
                        giveToPlayer(player, new ItemStack(ModItems.STIR_BAR.get()));
                    }
                    level().playSound(null, pos, SoundEvents.ITEM_PICKUP,
                            SoundSource.BLOCKS, 0.8F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }
            // 三颈瓶：把玻璃塞塞进正看着的瓶口。
            if (held.is(ModItems.GLASS_STOPPER.get()) && VesselHeating.isThreeNeck(flask)) {
                if (LabInteractions.tryPlugNeck(flask, held, player, pos, click,
                        SCALE, OFF, 5.8 / 16.0, OFF, 0.0)) {
                    setFlask(flask);
                }
                return InteractionResult.SUCCESS;
            }
            // 放烧瓶（只有烧瓶可以放）。
            if (flask.isEmpty() && HeatingMantleBlock.isFlask(held)) {
                setFlask(held.copy());
                held.shrink(1);
                level().playSound(null, pos, SoundEvents.GLASS_PLACE,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            // 搅拌子右键丢入（有烧瓶且还没放搅拌子时）。
            if (!flask.isEmpty() && !hasStirBar() && held.is(ModItems.STIR_BAR.get())) {
                setStirBar(true);
                held.shrink(1);
                level().playSound(null, pos, SoundEvents.METAL_PLACE,
                        SoundSource.BLOCKS, 0.7F, 1.0F);
                return InteractionResult.SUCCESS;
            }
        } else {
            // 客户端与服务端一致返回 SUCCESS，避免 PASS 导致重试/重复返还物品。
            if (held.isEmpty()
                    || (held.is(ModItems.GLASS_STOPPER.get())
                            && VesselHeating.isThreeNeck(getFlask()))
                    || (getFlask().isEmpty() && HeatingMantleBlock.isFlask(held))
                    || (!getFlask().isEmpty() && !hasStirBar()
                            && held.is(ModItems.STIR_BAR.get()))) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    /** 玩家视线与虚拟命中框的交点（塞玻璃塞时作为点击点回退）。 */
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
}
