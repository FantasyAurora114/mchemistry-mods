package com.example.chemistry.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * 技术性实体：不可见、无物理、不可推动/受伤，但可被射线点选。
 *
 * <p>与 {@link RubberTubeEntity} 同一思路——实体本身不是“活物”，而是某个
 * 仪器的载体：用一个 {@link #virtualHitbox()}（比视觉模型更大的 AABB）提供
 * 一个“虚拟命中框”，让玩家即使对着“空气”（模型悬空处）也能选中并交互。
 * 子类把自身状态放进 synced data / NBT，并用 EntityRenderer 画模型。
 */
public abstract class TechnicalEntity extends Entity {

    protected TechnicalEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** 当前仪器的物品形态（拿起/掉落用）。 */
    public abstract ItemStack toStack();

    /** 左键破坏时掉落的所有物品（默认就是 {@link #toStack()}）。 */
    protected java.util.List<ItemStack> dropsOnBreak() {
        ItemStack s = toStack();
        return s.isEmpty() ? java.util.List.of() : java.util.List.of(s);
    }

    /** 虚拟命中框（世界坐标 AABB），射线命中即选中该实体。默认取包围盒。 */
    public AABB virtualHitbox() {
        return getBoundingBox();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** 左键“破坏”：掉落物品并移除（创造模式不掉落）。 */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!level.isClientSide()) {
            ItemStack stack = toStack();
            Entity attacker = source.getEntity();
            boolean creative = attacker instanceof Player p && p.isCreative();
            if (!creative) {
                for (ItemStack drop : dropsOnBreak()) {
                    if (!drop.isEmpty()) {
                        this.spawnAtLocation(level, drop);
                    }
                }
            }
            this.discard();
        }
        return true;
    }

    /** 拿起：把物品给玩家并移除实体（服务端）。 */
    protected boolean pickUp(Player player) {
        if (level().isClientSide()) {
            return false;
        }
        ItemStack stack = toStack();
        this.discard();
        if (!stack.isEmpty()) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        return true;
    }

    // Synced data is for network updates; persistent fields must be saved by each subclass.
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }
}
