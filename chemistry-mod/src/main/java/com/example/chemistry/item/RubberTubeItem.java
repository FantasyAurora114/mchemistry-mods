package com.example.chemistry.item;

import java.util.List;

import com.example.chemistry.GasFlowEngine;
import com.example.chemistry.block.GasCollectingBottleBlock;
import com.example.chemistry.block.IronStandBlock;
import com.example.chemistry.block.WaterTroughBlock;
import com.example.chemistry.entity.AnchorPositions;
import com.example.chemistry.entity.RubberTubeEntity;
import com.example.chemistry.entity.RubberTubeEntity.Anchor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 橡胶管：右键水（水槽/水方块）沾湿后才能使用。右键两个锚点（方块面、
 * 实体或铁架台上玻璃导管的出口）铺一条自然下垂的橡胶管。
 */
public class RubberTubeItem extends Item {

    private static final String KEY_WET = "tube_wet";

    public RubberTubeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        if (stack.isEmpty()) {
            return InteractionResult.PASS;
        }
        // Sneak + right-click cancels a pending connection (no tube consumed).
        if (player.isShiftKeyDown() && readPending(stack) != null) {
            discardTempTube(level, stack);
            clearPending(stack);
            player.displayClientMessage(
                    Component.translatable("mchemistry.rubber_tube.cancel"), true);
            return InteractionResult.SUCCESS;
        }
        // Water has no block shape, so right-clicking it actually targets the
        // block underneath; check both the clicked block and the face-adjacent one.
        BlockPos clicked = context.getClickedPos();
        var clickedState = level.getBlockState(clicked);
        var adjacentState = level.getBlockState(clicked.relative(context.getClickedFace()));
        // Tube endpoints get the HIGHEST priority: a glass-tube head on an iron
        // stand, a nozzle in a water trough, or a nozzle in a gas bottle.
        if (clickedState.is(com.example.chemistry.registry.ModBlocks.IRON_STAND.get())) {
            IronStandBlock.handleTubeHead(level, player, stack, clicked, clickedState,
                    context.getClickLocation());
            return InteractionResult.SUCCESS;
        }
        // A glass-tube head in a vessel placed on the ground.
        if (clickedState.is(com.example.chemistry.registry.ModBlocks.PLACED_VESSEL.get())) {
            com.example.chemistry.block.PlacedVesselBlock.handleTubeHead(level, player, stack, clicked);
            return InteractionResult.SUCCESS;
        }
        // A water trough (reached on the sneak path, where the block's own
        // handler is skipped) wets the tube or anchors it at the nozzle.
        if (clickedState.getBlock() instanceof WaterTroughBlock) {
            WaterTroughBlock.handleTube(level, player, stack, clicked, clickedState);
            return InteractionResult.SUCCESS;
        }
        // A gas nozzle inserted in a placed gas bottle is also a tube anchor.
        if (clickedState.getBlock() instanceof GasCollectingBottleBlock
                && clickedState.getValue(GasCollectingBottleBlock.HAS_NOZZLE)) {
            GasCollectingBottleBlock.handleTubeNozzle(level, player, stack, clicked, clickedState);
            return InteractionResult.SUCCESS;
        }
        boolean onWater = isWater(clickedState) || isWater(adjacentState);
        if (onWater) {
            setWet(stack);
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.wet"), true);
            return InteractionResult.SUCCESS;
        }
        if (!isWet(stack)) {
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.need_wet"), true);
            return InteractionResult.SUCCESS;
        }
        Anchor point = Anchor.block(context.getClickedPos(), context.getClickedFace());
        Anchor pending = readPending(stack);
        if (pending == null) {
            startPending(level, player, stack, point);
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.start_block",
                    context.getClickedPos().getX(), context.getClickedPos().getY(),
                    context.getClickedPos().getZ()), true);
            return InteractionResult.SUCCESS;
        }
        if (sameAnchor(pending, point)) {
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.same"), true);
            return InteractionResult.SUCCESS;
        }
        createTube(level, player, stack, pending, point);
        return InteractionResult.SUCCESS;
    }

    private static boolean isWater(net.minecraft.world.level.block.state.BlockState state) {
        return state.getFluidState().is(Fluids.WATER)
                || state.getFluidState().is(Fluids.FLOWING_WATER);
    }

    /** Called from the entity-interact handler when an entity is clicked. */
    public static void onEntityClicked(Level level, Player player, ItemStack stack, Entity target) {
        if (!isWet(stack)) {
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.need_wet"), true);
            return;
        }
        Anchor point = Anchor.entity(target.getUUID());
        Anchor pending = readPending(stack);
        if (pending == null) {
            startPending(level, player, stack, point);
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.start_entity"), true);
        } else if (sameAnchor(pending, point)) {
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.same"), true);
        } else {
            createTube(level, player, stack, pending, point);
        }
    }

    public static void createTube(Level level, Player player, ItemStack stack, Anchor a, Anchor b) {
        discardTempTube(level, stack);
        Vec3 spawn = anchorWorldPos(level, a);
        if (spawn == null) {
            spawn = anchorWorldPos(level, b);
        }
        if (spawn == null) {
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.fail"), true);
            clearPending(stack);
            return;
        }
        RubberTubeEntity tube = RubberTubeEntity.create(level, a, b, spawn);
        level.addFreshEntity(tube);
        clearPending(stack);
        // A real rubber tube is only consumed once BOTH endpoints are set.
        if (stack.getItem() instanceof RubberTubeItem) {
            stack.shrink(1);
        }
        player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.placed"), true);
    }

    /** Set the pending start and show a temporary tube from it to the player. */
    public static void startPending(Level level, Player player, ItemStack stack, Anchor anchor) {
        setPending(stack, anchor);
        if (level.isClientSide()) {
            return;
        }
        Vec3 spawn = anchorWorldPos(level, anchor);
        if (spawn == null) {
            spawn = player.position();
        }
        RubberTubeEntity temp = RubberTubeEntity.create(level, anchor,
                Anchor.entity(player.getUUID()), spawn);
        level.addFreshEntity(temp);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("tube_temp_id", temp.getId());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Remove the temporary preview tube (the pending end is still kept). */
    public static void discardTempTube(Level level, ItemStack stack) {
        if (level.isClientSide()) {
            return;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains("tube_temp_id")) {
            Entity e = level.getEntity(tag.getIntOr("tube_temp_id", -1));
            if (e instanceof RubberTubeEntity) {
                e.discard();
            }
        }
    }

    /** World position of an anchor (face centre for blocks, entity position otherwise). */
    public static Vec3 anchorWorldPos(Level level, Anchor a) {
        if (a == null) {
            return null;
        }
        if (a.kind() == Anchor.KIND_BLOCK) {
            BlockPos p = a.pos();
            Direction f = a.face();
            return new Vec3(p.getX() + 0.5 + f.getStepX() * 0.5,
                    p.getY() + 0.5 + f.getStepY() * 0.5,
                    p.getZ() + 0.5 + f.getStepZ() * 0.5);
        }
        if (a.kind() == Anchor.KIND_STAND) {
            // Exact glass-tube head, shared with the renderer — the tube must
            // start/end at the tube mouth, not at the block centre.
            return AnchorPositions.standHead(level, a);
        }
        if (a.kind() == Anchor.KIND_NOZZLE) {
            // Exact nozzle / bottle-mouth tip, shared with the renderer.
            return AnchorPositions.nozzleTip(level, a);
        }
        Entity e = level.getEntity(a.uuid());
        return e != null ? e.position() : null;
    }

    /** A generous picking box spanning the whole tube, so shears can cut any
     *  visible segment by right-clicking it. Only used for ray-casting, never
     *  for physics. */
    public static AABB tubePickBox(Level level, RubberTubeEntity tube) {
        Vec3 a = anchorWorldPos(level, tube.getAnchorA());
        Vec3 b = anchorWorldPos(level, tube.getAnchorB());
        if (a == null) {
            a = tube.position();
        }
        if (b == null) {
            b = tube.position();
        }
        return new AABB(a, b).inflate(0.2);
    }

    /** True if a catheter endpoint (glass-tube head or gas nozzle) already has a
     *  rubber tube connected to it. */
    public static boolean hasTubeAt(Level level, Anchor anchor) {
        if (anchor == null || (anchor.kind() != Anchor.KIND_STAND && anchor.kind() != Anchor.KIND_NOZZLE)) {
            return false;
        }
        for (RubberTubeEntity tube : level.getEntitiesOfClass(RubberTubeEntity.class,
                new AABB(anchor.pos()).inflate(64.0))) {
            if (sameAnchor(tube.getAnchorA(), anchor) || sameAnchor(tube.getAnchorB(), anchor)) {
                return true;
            }
        }
        return false;
    }

    /** Empty-hand right-click on a nozzle: complete a pending tube that is
     *  anchored to the player, or block removal while the nozzle is occupied.
     *  Returns true when the click is consumed without removing the nozzle. */
    public static boolean handleNozzleEmptyClick(Level level, Player player, Anchor nozzle) {
        if (level.isClientSide()) {
            return true;
        }
        if (hasTubeAt(level, nozzle)) {
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.occupied"), true);
            return true;
        }
        java.util.UUID playerId = player.getUUID();
        for (RubberTubeEntity tube : level.getEntitiesOfClass(RubberTubeEntity.class,
                new AABB(player.blockPosition()).inflate(64.0))) {
            Anchor a = tube.getAnchorA();
            Anchor b = tube.getAnchorB();
            Anchor other = isPlayerAnchor(a, playerId) ? b
                    : isPlayerAnchor(b, playerId) ? a : null;
            if (other == null || other.pos() == null) {
                continue;
            }
            if (sameAnchor(other, nozzle)) {
                player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.same"), true);
                return true;
            }
            Vec3 spawn = anchorWorldPos(level, other);
            if (spawn == null) {
                spawn = player.position();
            }
            tube.discard();
            level.addFreshEntity(RubberTubeEntity.create(level, other, nozzle, spawn));
            consumeOneTube(player);
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.placed"), true);
            return true;
        }
        return false;
    }

    /** Consume one rubber tube from the player (used when the connection is
     *  completed without the tube being held in a hand). */
    private static void consumeOneTube(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack s = player.getInventory().getItem(slot);
            if (s.getItem() instanceof RubberTubeItem) {
                clearPending(s);
                s.shrink(1);
                return;
            }
        }
    }

    private static boolean isPlayerAnchor(Anchor anchor, java.util.UUID playerId) {
        return anchor != null && anchor.kind() == Anchor.KIND_ENTITY
                && playerId.equals(anchor.uuid());
    }

    /** Every rubber tube connected to the given anchor (stand head / nozzle). */
    public static java.util.List<RubberTubeEntity> findTubesAt(Level level, Anchor anchor) {
        if (anchor == null || anchor.pos() == null) {
            return java.util.List.of();
        }
        return level.getEntitiesOfClass(RubberTubeEntity.class,
                new AABB(anchor.pos()).inflate(64.0)).stream()
                .filter(t -> sameAnchor(t.getAnchorA(), anchor) || sameAnchor(t.getAnchorB(), anchor))
                .toList();
    }

    /** Detach one end of every tube from the given head/nozzle. The tube stays
     *  connected at its other end and dangles from the old position instead of
     *  dropping as an item. */
    public static void detachTubesAt(Level level, Anchor anchor) {
        if (level.isClientSide() || anchor == null) {
            return;
        }
        for (RubberTubeEntity tube : findTubesAt(level, anchor)) {
            Anchor free = Anchor.block(anchor.pos(), net.minecraft.core.Direction.UP);
            if (tube.getAnchorA() != null && tube.getAnchorA().equals(anchor)) {
                tube.setAnchorA(free);
            }
            if (tube.getAnchorB() != null && tube.getAnchorB().equals(anchor)) {
                tube.setAnchorB(free);
            }
        }
    }

    /** Wet / dry state. */
    public static boolean isWet(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getBooleanOr(KEY_WET, false);
    }

    public static void setWet(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(KEY_WET, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(), List.of("wet"), List.of()));
    }

    /** Let the client send the use packet so the server can attach a gas nozzle. */
    @Override
    public InteractionResult use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        if (player.getOffhandItem().getItem()
                == com.example.chemistry.registry.ModItems.GAS_NOZZLE.get()) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public static Anchor readPending(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains("tube_anchor_type")) {
            return null;
        }
        String type = tag.getStringOr("tube_anchor_type", "");
        if (type.equals("entity")) {
            return Anchor.entity(new java.util.UUID(
                    tag.getLongOr("tube_anchor_uuid_most", 0L),
                    tag.getLongOr("tube_anchor_uuid_least", 0L)));
        }
        if (type.equals("stand")) {
            return Anchor.stand(
                    new BlockPos(tag.getIntOr("tube_anchor_x", 0),
                            tag.getIntOr("tube_anchor_y", 0),
                            tag.getIntOr("tube_anchor_z", 0)),
                    tag.getIntOr("tube_anchor_slot", 1));
        }
        return Anchor.block(
                new BlockPos(tag.getIntOr("tube_anchor_x", 0),
                        tag.getIntOr("tube_anchor_y", 0),
                        tag.getIntOr("tube_anchor_z", 0)),
                Direction.byName(tag.getStringOr("tube_anchor_face", "up")));
    }

    public static void setPending(ItemStack stack, Anchor a) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (a.kind() == Anchor.KIND_ENTITY) {
            tag.putString("tube_anchor_type", "entity");
            tag.putLong("tube_anchor_uuid_most", a.uuid().getMostSignificantBits());
            tag.putLong("tube_anchor_uuid_least", a.uuid().getLeastSignificantBits());
        } else if (a.kind() == Anchor.KIND_STAND) {
            tag.putString("tube_anchor_type", "stand");
            tag.putInt("tube_anchor_x", a.pos().getX());
            tag.putInt("tube_anchor_y", a.pos().getY());
            tag.putInt("tube_anchor_z", a.pos().getZ());
            tag.putInt("tube_anchor_slot", a.slot());
        } else {
            tag.putString("tube_anchor_type", "block");
            tag.putInt("tube_anchor_x", a.pos().getX());
            tag.putInt("tube_anchor_y", a.pos().getY());
            tag.putInt("tube_anchor_z", a.pos().getZ());
            tag.putString("tube_anchor_face", a.face().getName());
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void clearPending(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_DATA);
    }

    /** Drop every tube connected to a stand's glass-tube slot (e.g. when removed). */
    public static void dropTubesConnectedToStand(Level level, BlockPos standPos, int slot) {
        if (level.isClientSide()) {
            return;
        }
        for (RubberTubeEntity tube : level.getEntitiesOfClass(RubberTubeEntity.class,
                new AABB(standPos).inflate(64.0))) {
            if (isStandAnchor(tube.getAnchorA(), standPos, slot)
                    || isStandAnchor(tube.getAnchorB(), standPos, slot)) {
                GasFlowEngine.rupture(level, tube);
                tube.discard();
                level.addFreshEntity(new ItemEntity(level, tube.getX(), tube.getY(), tube.getZ(),
                        new ItemStack(com.example.chemistry.registry.ModItems.RUBBER_TUBE.get())));
            }
        }
    }

    /** Drop every tube anchored at a block face (e.g. a trough nozzle). */
    public static void dropTubesConnectedToBlock(Level level, BlockPos pos, Direction face) {
        if (level.isClientSide()) {
            return;
        }
        for (RubberTubeEntity tube : level.getEntitiesOfClass(RubberTubeEntity.class,
                new AABB(pos).inflate(64.0))) {
            if (isBlockAnchor(tube.getAnchorA(), pos, face)
                    || isBlockAnchor(tube.getAnchorB(), pos, face)) {
                GasFlowEngine.rupture(level, tube);
                tube.discard();
                level.addFreshEntity(new ItemEntity(level, tube.getX(), tube.getY(), tube.getZ(),
                        new ItemStack(com.example.chemistry.registry.ModItems.RUBBER_TUBE.get())));
            }
        }
    }

    /** Drop every tube connected to a water-trough gas nozzle. */
    public static void dropTubesConnectedToNozzle(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        for (RubberTubeEntity tube : level.getEntitiesOfClass(RubberTubeEntity.class,
                new AABB(pos).inflate(64.0))) {
            if (isNozzleAnchor(tube.getAnchorA(), pos) || isNozzleAnchor(tube.getAnchorB(), pos)) {
                GasFlowEngine.rupture(level, tube);
                tube.discard();
                level.addFreshEntity(new ItemEntity(level, tube.getX(), tube.getY(), tube.getZ(),
                        new ItemStack(com.example.chemistry.registry.ModItems.RUBBER_TUBE.get())));
            }
        }
    }

    private static boolean isStandAnchor(Anchor a, BlockPos pos, int slot) {
        return a != null && a.kind() == Anchor.KIND_STAND
                && pos.equals(a.pos()) && a.slot() == slot;
    }

    private static boolean isBlockAnchor(Anchor a, BlockPos pos, Direction face) {
        return a != null && a.kind() == Anchor.KIND_BLOCK
                && pos.equals(a.pos()) && a.face() == face;
    }

    private static boolean isNozzleAnchor(Anchor a, BlockPos pos) {
        return a != null && a.kind() == Anchor.KIND_NOZZLE && pos.equals(a.pos());
    }

    public static boolean sameAnchor(Anchor a, Anchor b) {
        if (a.kind() != b.kind()) {
            return false;
        }
        if (a.kind() == Anchor.KIND_ENTITY) {
            return a.uuid().equals(b.uuid());
        }
        if (a.kind() == Anchor.KIND_STAND) {
            return a.pos().equals(b.pos()) && a.slot() == b.slot();
        }
        return a.pos().equals(b.pos()) && a.face() == b.face();
    }
}
