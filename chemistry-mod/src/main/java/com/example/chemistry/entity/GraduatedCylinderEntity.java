package com.example.chemistry.entity;

import com.example.chemistry.item.DropperHelper;
import com.example.chemistry.item.GraduatedCylinderItem;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.transfer.BottleCodes;

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

/**
 * 放下的量筒（技术性实体版）。状态只有“液体 id + 毫升”，存进 synced data，
 * 所以客户端渲染器能直接读到，保存/加载也自动完成。
 */
public class GraduatedCylinderEntity extends TechnicalEntity {

    private static final EntityDataAccessor<String> DATA_LIQUID =
            SynchedEntityData.defineId(GraduatedCylinderEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_ML =
            SynchedEntityData.defineId(GraduatedCylinderEntity.class, EntityDataSerializers.FLOAT);

    public GraduatedCylinderEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIQUID, "");
        builder.define(DATA_ML, 0.0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("liquid", getLiquid() == null ? "" : getLiquid());
        output.putFloat("ml", (float) getMl());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setContents(input.getStringOr("liquid", ""), input.getFloatOr("ml", 0.0F));
    }

    public String getLiquid() {
        String s = this.entityData.get(DATA_LIQUID);
        return s == null || s.isEmpty() ? null : s;
    }

    public double getMl() {
        return this.entityData.get(DATA_ML);
    }

    public void setContents(String liquid, double ml) {
        this.entityData.set(DATA_LIQUID, liquid == null ? "" : liquid);
        this.entityData.set(DATA_ML, (float) ml);
    }

    /** 当前量筒物品（含液体 NBT），用于掉落/拿起。 */
    public ItemStack toStack() {
        ItemStack stack = new ItemStack(ModItems.GRADUATED_CYLINDER.get());
        String liquid = getLiquid();
        if (liquid != null) {
            GraduatedCylinderItem.set(stack, liquid, getMl());
        }
        return stack;
    }

    /** 加入液体，返回实际加入量（与量筒物品的 add 语义一致）。 */
    public double addLiquid(String liquid, double ml) {
        String cur = getLiquid();
        if (cur != null && !cur.equals(liquid)) {
            return 0.0;
        }
        double have = getMl();
        double add = Math.min(ml, GraduatedCylinderItem.CAPACITY - have);
        if (add > 0.0) {
            setContents(liquid, have + add);
        }
        return add;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        // 潜行 + 空手：丢弃 1mL（不给物品）。
        if (player.isShiftKeyDown() && held.isEmpty()) {
            if (!level().isClientSide()) {
                String liquid = getLiquid();
                if (liquid != null && getMl() >= 1.0) {
                    setContents(liquid, getMl() - 1.0);
                    player.displayClientMessage(
                            Component.translatable("mchemistry.cylinder.discard"), true);
                    level().playSound(null, blockPosition(), SoundEvents.BOTTLE_EMPTY,
                            SoundSource.BLOCKS, 0.8F, 1.2F);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // 胶头滴管 / 滴瓶瓶塞：滴加 1mL。
        if (DropperHelper.isDropper(held) && !DropperHelper.isEmpty(held)) {
            if (!level().isClientSide()) {
                double added = addLiquid(DropperHelper.getLiquid(held), 1.0);
                if (added > 0) {
                    DropperHelper.setMl(held, DropperHelper.getMl(held) - 1);
                    player.displayClientMessage(
                            Component.translatable("mchemistry.cylinder.drip"), true);
                    level().playSound(null, blockPosition(), SoundEvents.BOTTLE_EMPTY,
                            SoundSource.BLOCKS, 0.8F, 1.2F);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // 细口瓶：倒入 5mL。
        String liquidId = BottleCodes.liquidIdOf(held);
        if (liquidId != null) {
            if (!level().isClientSide()) {
                addLiquid(liquidId, 5.0);
                player.displayClientMessage(
                        Component.translatable("mchemistry.cylinder.pour5"), true);
                level().playSound(null, blockPosition(), SoundEvents.BOTTLE_EMPTY,
                        SoundSource.BLOCKS, 0.8F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }
        // 空手：拿起量筒。
        if (held.isEmpty()) {
            if (pickUp(player)) {
                level().playSound(null, blockPosition(), SoundEvents.ITEM_PICKUP,
                        SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
