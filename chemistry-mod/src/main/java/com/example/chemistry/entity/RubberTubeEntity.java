package com.example.chemistry.entity;

import java.util.Optional;
import java.util.UUID;

import com.example.chemistry.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A sagging rubber tube drawn between two anchors. An anchor is either a block
 * face (BlockPos + Direction) or an entity (UUID); entity anchors follow the
 * entity, so the tube hangs and sags naturally. All anchor data is synched to
 * clients so the renderer can draw the curve every frame.
 */
public class RubberTubeEntity extends Entity {

    /** One end of the tube: block face, entity, or a glass-tube head on a stand. */
    public record Anchor(int kind, @Nullable BlockPos pos, @Nullable Direction face,
            @Nullable UUID uuid, int slot) {
        public static final int KIND_BLOCK = 0;
        public static final int KIND_ENTITY = 1;
        public static final int KIND_STAND = 2;
        public static final int KIND_NOZZLE = 3;

        public static Anchor block(BlockPos pos, Direction face) {
            return new Anchor(KIND_BLOCK, pos, face, null, 0);
        }

        public static Anchor entity(UUID uuid) {
            return new Anchor(KIND_ENTITY, null, null, uuid, 0);
        }

        public static Anchor stand(BlockPos pos, int slot) {
            return new Anchor(KIND_STAND, pos, null, null, slot);
        }

        /** Gas nozzle placed in a water trough (pos + tilt direction). */
        public static Anchor nozzle(BlockPos pos, Direction face) {
            return new Anchor(KIND_NOZZLE, pos, face, null, 0);
        }
    }

    private static final EntityDataAccessor<Integer> DATA_A_KIND =
            SynchedEntityData.defineId(RubberTubeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<BlockPos>> DATA_A_POS =
            SynchedEntityData.defineId(RubberTubeEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Direction> DATA_A_FACE =
            SynchedEntityData.defineId(RubberTubeEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Long> DATA_A_UUID_MOST =
            SynchedEntityData.defineId(RubberTubeEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_A_UUID_LEAST =
            SynchedEntityData.defineId(RubberTubeEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_A_SLOT =
            SynchedEntityData.defineId(RubberTubeEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DATA_B_KIND =
            SynchedEntityData.defineId(RubberTubeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<BlockPos>> DATA_B_POS =
            SynchedEntityData.defineId(RubberTubeEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Direction> DATA_B_FACE =
            SynchedEntityData.defineId(RubberTubeEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Long> DATA_B_UUID_MOST =
            SynchedEntityData.defineId(RubberTubeEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_B_UUID_LEAST =
            SynchedEntityData.defineId(RubberTubeEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_B_SLOT =
            SynchedEntityData.defineId(RubberTubeEntity.class, EntityDataSerializers.INT);

    /** Gas in transit through this tube (server-side only). Gas enters at the
     *  source end, occupies the tube, and leaves at the far end — cutting the
     *  tube drops everything still inside. */
    private String transitGas = "";
    private int transitMl;
    private double transitPurity = 1.0;

    public RubberTubeEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public static RubberTubeEntity create(Level level, Anchor a, Anchor b, Vec3 spawn) {
        RubberTubeEntity tube = new RubberTubeEntity(ModEntities.RUBBER_TUBE.get(), level);
        tube.setAnchorA(a);
        tube.setAnchorB(b);
        tube.setPos(spawn.x, spawn.y, spawn.z);
        return tube;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_A_KIND, 0);
        builder.define(DATA_A_POS, Optional.empty());
        builder.define(DATA_A_FACE, Direction.UP);
        builder.define(DATA_A_UUID_MOST, 0L);
        builder.define(DATA_A_UUID_LEAST, 0L);
        builder.define(DATA_A_SLOT, 0);
        builder.define(DATA_B_KIND, 0);
        builder.define(DATA_B_POS, Optional.empty());
        builder.define(DATA_B_FACE, Direction.UP);
        builder.define(DATA_B_UUID_MOST, 0L);
        builder.define(DATA_B_UUID_LEAST, 0L);
        builder.define(DATA_B_SLOT, 0);
    }

    public void setAnchorA(Anchor a) {
        writeAnchor(a, DATA_A_KIND, DATA_A_POS, DATA_A_FACE, DATA_A_UUID_MOST, DATA_A_UUID_LEAST, DATA_A_SLOT);
    }

    public void setAnchorB(Anchor b) {
        writeAnchor(b, DATA_B_KIND, DATA_B_POS, DATA_B_FACE, DATA_B_UUID_MOST, DATA_B_UUID_LEAST, DATA_B_SLOT);
    }

    public boolean hasTransit() {
        return transitMl > 0 && !transitGas.isEmpty();
    }

    public String getTransitGas() {
        return transitGas;
    }

    public int getTransitMl() {
        return transitMl;
    }

    public double getTransitPurity() {
        return transitPurity;
    }

    /** Push gas into the tube from the source end; returns the accepted mL
     *  (0 when the tube is still carrying a different gas). */
    public int addTransit(String id, int ml, double purity) {
        if (ml <= 0) {
            return 0;
        }
        if (transitGas.isEmpty()) {
            transitGas = id;
            transitPurity = purity;
        } else if (!transitGas.equals(id)) {
            // A tube only carries one gas at a time; the caller retries next
            // tick once the current gas has cleared.
            return 0;
        } else {
            transitPurity = Math.min(transitPurity, purity);
        }
        transitMl += ml;
        return ml;
    }

    /** Pull up to max mL out of the far end; returns the amount removed. */
    public int takeTransit(int max) {
        int out = Math.min(max, transitMl);
        transitMl -= out;
        if (transitMl <= 0) {
            transitMl = 0;
            transitGas = "";
            transitPurity = 1.0;
        }
        return out;
    }

    @Nullable
    public Anchor getAnchorA() {
        return readAnchor(DATA_A_KIND, DATA_A_POS, DATA_A_FACE, DATA_A_UUID_MOST, DATA_A_UUID_LEAST, DATA_A_SLOT);
    }

    @Nullable
    public Anchor getAnchorB() {
        return readAnchor(DATA_B_KIND, DATA_B_POS, DATA_B_FACE, DATA_B_UUID_MOST, DATA_B_UUID_LEAST, DATA_B_SLOT);
    }

    private void writeAnchor(Anchor a, EntityDataAccessor<Integer> kind,
            EntityDataAccessor<Optional<BlockPos>> pos, EntityDataAccessor<Direction> face,
            EntityDataAccessor<Long> uuidMost, EntityDataAccessor<Long> uuidLeast,
            EntityDataAccessor<Integer> slot) {
        if (a == null) {
            return;
        }
        this.entityData.set(kind, a.kind());
        if (a.kind() == Anchor.KIND_ENTITY) {
            this.entityData.set(pos, Optional.empty());
            this.entityData.set(uuidMost, a.uuid().getMostSignificantBits());
            this.entityData.set(uuidLeast, a.uuid().getLeastSignificantBits());
        } else if (a.kind() == Anchor.KIND_STAND) {
            this.entityData.set(pos, Optional.of(a.pos()));
            this.entityData.set(uuidMost, 0L);
            this.entityData.set(uuidLeast, 0L);
            this.entityData.set(slot, a.slot());
        } else {
            this.entityData.set(pos, Optional.of(a.pos()));
            this.entityData.set(face, a.face());
            this.entityData.set(uuidMost, 0L);
            this.entityData.set(uuidLeast, 0L);
            this.entityData.set(slot, 0);
        }
    }

    private Anchor readAnchor(EntityDataAccessor<Integer> kind,
            EntityDataAccessor<Optional<BlockPos>> pos, EntityDataAccessor<Direction> face,
            EntityDataAccessor<Long> uuidMost, EntityDataAccessor<Long> uuidLeast,
            EntityDataAccessor<Integer> slot) {
        int k = this.entityData.get(kind);
        if (k == Anchor.KIND_ENTITY) {
            return Anchor.entity(new UUID(this.entityData.get(uuidMost), this.entityData.get(uuidLeast)));
        }
        Optional<BlockPos> p = this.entityData.get(pos);
        if (k == Anchor.KIND_STAND) {
            return p.map(blockPos -> Anchor.stand(blockPos, this.entityData.get(slot))).orElse(null);
        }
        if (k == Anchor.KIND_NOZZLE) {
            return p.map(blockPos -> Anchor.nozzle(blockPos, this.entityData.get(face))).orElse(null);
        }
        return p.map(blockPos -> Anchor.block(blockPos, this.entityData.get(face))).orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        saveAnchor(output, "a", getAnchorA());
        saveAnchor(output, "b", getAnchorB());
        output.putString("transit_gas", transitGas);
        output.putInt("transit_ml", transitMl);
        output.putDouble("transit_purity", transitPurity);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setAnchorA(readAnchor(input, "a"));
        setAnchorB(readAnchor(input, "b"));
        transitGas = input.getStringOr("transit_gas", "");
        transitMl = input.getIntOr("transit_ml", 0);
        transitPurity = input.getDoubleOr("transit_purity", 1.0);
        if (transitMl <= 0) {
            transitGas = "";
            transitPurity = 1.0;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return false;
    }

    private static void saveAnchor(ValueOutput output, String key, Anchor a) {
        if (a == null) {
            return;
        }
        ValueOutput child = output.child(key);
        child.putInt("kind", a.kind());
        if (a.kind() == Anchor.KIND_ENTITY) {
            child.store("uuid", UUIDUtil.CODEC, a.uuid());
        } else if (a.kind() == Anchor.KIND_STAND) {
            child.store("pos", BlockPos.CODEC, a.pos());
            child.putInt("slot", a.slot());
        } else if (a.kind() == Anchor.KIND_NOZZLE) {
            child.store("pos", BlockPos.CODEC, a.pos());
            child.store("face", Direction.CODEC, a.face());
        } else {
            child.store("pos", BlockPos.CODEC, a.pos());
            child.store("face", Direction.CODEC, a.face());
        }
    }

    @Nullable
    private static Anchor readAnchor(ValueInput input, String key) {
        Optional<ValueInput> child = input.child(key);
        if (child.isEmpty()) {
            return null;
        }
        ValueInput c = child.get();
        int kind = c.getIntOr("kind", 0);
        if (kind == Anchor.KIND_ENTITY) {
            UUID uuid = c.read("uuid", UUIDUtil.CODEC).orElse(null);
            return uuid == null ? null : Anchor.entity(uuid);
        }
        BlockPos pos = c.read("pos", BlockPos.CODEC).orElse(null);
        if (kind == Anchor.KIND_STAND) {
            return pos == null ? null : Anchor.stand(pos, c.getIntOr("slot", 1));
        }
        if (kind == Anchor.KIND_NOZZLE) {
            return pos == null ? null : Anchor.nozzle(pos, c.read("face", Direction.CODEC).orElse(Direction.UP));
        }
        Direction face = c.read("face", Direction.CODEC).orElse(Direction.UP);
        return pos == null ? null : Anchor.block(pos, face);
    }

    /** The tube can be right-clicked (to remove it) but is not pushable. */
    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
