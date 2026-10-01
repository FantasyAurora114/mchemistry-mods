package com.example.chemistry.item;

import java.util.List;

import com.example.chemistry.GasFlowEngine;
import com.example.chemistry.block.GasCollectingBottleBlock;
import com.example.chemistry.block.IronStandBlock;
import com.example.chemistry.block.WaterTroughBlock;
import com.example.chemistry.entity.AnchorPositions;
import com.example.chemistry.entity.RubberTubeEntity;
import com.example.chemistry.entity.RubberTubeEntity.Port;

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
 * 橡胶管：右键水（水槽/水方块）沾湿后才能使用。右键两个已安装的管口（玻璃导管出口或仪器接口）铺一条自然下垂的橡胶管。
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
        if (clickedState.getBlock() instanceof com.example.chemistry.block.GasApplianceBlock) {
            com.example.chemistry.block.GasApplianceBlock.connect(level, player, stack, clicked);
            return InteractionResult.SUCCESS;
        }
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
        player.displayClientMessage(Component.literal("橡胶管只能连接已安装的玻璃导管或仪器管口"), true);
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
        if (target instanceof com.example.chemistry.entity.IronStandEntity stand) {
            var vessel=stand.findMountedVessel();
            if(vessel!=null){vessel.handleTubeHead(player,stack);return;}
            Port best=null; double distance=Double.MAX_VALUE;
            for(int slot=1;slot<=2;slot++){
                var tube=slot==1?stand.getAttached1():stand.getAttached2();
                if(!GlassTubeItem.isGlassTube(tube))continue;
                var point=Port.stand(stand.blockPosition(),slot);
                var world=point.worldPos(level);if(world==null)continue;
                double d=world.distanceTo(player.getEyePosition().add(player.getLookAngle().scale(world.distanceTo(player.getEyePosition()))));
                if(d<distance&&!hasTubeAt(level,point)){best=point;distance=d;}
            }
            if(best!=null){var pending=readPending(stack);if(pending==null)startPending(level,player,stack,best);else createTube(level,player,stack,pending,best);return;}
        }
        player.displayClientMessage(Component.literal("请连接已安装的玻璃导管管口"), true);
    }

    /** Reject ordinary blocks, players and removed attachments before consuming a hose. */
    public static boolean validPort(Level level, Port port) {
        if(port==null)return false;
        if(port.kind()==Port.KIND_STAND)return port.worldPos(level)!=null;
        if(port.kind()==Port.KIND_ENTITY)return level.getEntity(port.uuid()) instanceof com.example.chemistry.entity.GasCollectingBottleEntity bottle&&bottle.hasNozzle();
        var state=level.getBlockState(port.pos());
        if(port.kind()==Port.KIND_BLOCK)return state.getBlock() instanceof com.example.chemistry.block.GasApplianceBlock;
        if(port.kind()==Port.KIND_NOZZLE){
            if(state.getBlock() instanceof GasCollectingBottleBlock)return state.getValue(GasCollectingBottleBlock.HAS_NOZZLE);
            if(state.getBlock() instanceof WaterTroughBlock)return state.getValue(WaterTroughBlock.NOZZLE)!=WaterTroughBlock.NozzleDir.NONE
                    ||port.face()==Direction.UP&&level.getBlockEntity(port.pos()) instanceof com.example.chemistry.blockentity.WaterTroughBlockEntity trough&&trough.hasBottle();
            return level.getEntitiesOfClass(com.example.chemistry.entity.GasCollectingBottleEntity.class,new AABB(port.pos()))
                    .stream().anyMatch(com.example.chemistry.entity.GasCollectingBottleEntity::hasNozzle);
        }
        return false;
    }

    public static void createTube(Level level, Player player, ItemStack stack, Port a, Port b) {
        if (!validPort(level,a)||!validPort(level,b)||sameAnchor(a,b)) {
            player.displayClientMessage(Component.literal("两端都必须连接有效且不同的管口"),true);
            return;
        }
        if(hasTubeAt(level,a)||hasTubeAt(level,b)){
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.occupied"),true);
            return;
        }
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
        tube.setSupplyLine(stack.getItem() instanceof GasSupplyTubeItem);
        if (!level.isClientSide()) {
            tube.initAir();
        }
        level.addFreshEntity(tube);
        clearPending(stack);
        // A real rubber tube is only consumed once BOTH endpoints are set.
        if (stack.getItem() instanceof RubberTubeItem) {
            stack.shrink(1);
        }
        player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.placed"), true);
    }

    /** Set the pending start and show a temporary tube from it to the player. */
    public static void startPending(Level level, Player player, ItemStack stack, Port anchor) {
        if(!validPort(level,anchor))return;
        discardTempTube(level,stack);
        setPending(stack, anchor);
        if (level.isClientSide()) {
            return;
        }
        Vec3 spawn = anchorWorldPos(level, anchor);
        if (spawn == null) {
            spawn = player.position();
        }
        RubberTubeEntity temp = RubberTubeEntity.create(level, anchor,
                Port.entity(player.getUUID()), spawn);
        temp.setSupplyLine(stack.getItem() instanceof GasSupplyTubeItem);
        temp.setPreview(true);
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
    public static Vec3 anchorWorldPos(Level level, Port a) {
        // 端点世界坐标已收敛到 Port.worldPos。
        return a == null ? null : a.worldPos(level);
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
    public static boolean hasTubeAt(Level level, Port anchor) {
        return anchor!=null&&!findTubesAt(level,anchor).isEmpty();
    }

    /** Empty-hand right-click on a nozzle: complete a pending tube that is
     *  anchored to the player, or block removal while the nozzle is occupied.
     *  Returns true when the click is consumed without removing the nozzle. */
    public static boolean handleNozzleEmptyClick(Level level, Player player, Port nozzle) {
        if (level.isClientSide()) {
            return true;
        }
        if (hasTubeAt(level, nozzle)) {
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.occupied"), true);
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

    private static boolean isPlayerAnchor(Port anchor, java.util.UUID playerId) {
        return anchor != null && anchor.kind() == Port.KIND_ENTITY
                && playerId.equals(anchor.uuid());
    }

    /** Every rubber tube connected to the given anchor (stand head / nozzle). */
    public static java.util.List<RubberTubeEntity> findTubesAt(Level level, Port anchor) {
        if (anchor == null) {
            return java.util.List.of();
        }
        var point=anchor.worldPos(level);
        if(point==null)return java.util.List.of();
        return level.getEntitiesOfClass(RubberTubeEntity.class,
                new AABB(point,point).inflate(64.0)).stream()
                .filter(t -> !t.preview())
                .filter(t -> sameAnchor(t.getAnchorA(), anchor) || sameAnchor(t.getAnchorB(), anchor))
                .toList();
    }

    /** Removing an attached head/nozzle drops its hoses; never create free block anchors. */
    public static void detachTubesAt(Level level, Port anchor) {
        if (level.isClientSide() || anchor == null) {
            return;
        }
        for (RubberTubeEntity tube : findTubesAt(level, anchor)) {
            GasFlowEngine.rupture(level,tube);
            level.addFreshEntity(new ItemEntity(level,tube.getX(),tube.getY(),tube.getZ(),tube.dropItem()));
            tube.discard();
        }
    }

    /** Wet / dry state. */
    public static boolean isWet(ItemStack stack) {
        if (stack.getItem() instanceof GasSupplyTubeItem) return true;
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

    /** 副手拿玻璃导管时右键（对空）发送使用数据包，服务端执行“合成套管导管”。 */
    @Override
    public InteractionResult use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        if (GlassTubeItem.isGlassTube(player.getOffhandItem())) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public static Port readPending(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains("tube_anchor_type")) {
            return null;
        }
        String type = tag.getStringOr("tube_anchor_type", "");
        if (type.equals("entity")) {
            return Port.entity(new java.util.UUID(
                    tag.getLongOr("tube_anchor_uuid_most", 0L),
                    tag.getLongOr("tube_anchor_uuid_least", 0L)));
        }
        if (type.equals("stand")) {
            return Port.stand(
                    new BlockPos(tag.getIntOr("tube_anchor_x", 0),
                            tag.getIntOr("tube_anchor_y", 0),
                            tag.getIntOr("tube_anchor_z", 0)),
                    tag.getIntOr("tube_anchor_slot", 1));
        }
        BlockPos pos=new BlockPos(tag.getIntOr("tube_anchor_x",0),tag.getIntOr("tube_anchor_y",0),tag.getIntOr("tube_anchor_z",0));
        Direction face=Direction.byName(tag.getStringOr("tube_anchor_face","up"));
        if(face==null)face=Direction.UP;
        return type.equals("nozzle")?Port.nozzle(pos,face):Port.block(pos,face);
    }

    public static void setPending(ItemStack stack, Port a) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (a.kind() == Port.KIND_ENTITY) {
            tag.putString("tube_anchor_type", "entity");
            tag.putLong("tube_anchor_uuid_most", a.uuid().getMostSignificantBits());
            tag.putLong("tube_anchor_uuid_least", a.uuid().getLeastSignificantBits());
        } else if (a.kind() == Port.KIND_STAND) {
            tag.putString("tube_anchor_type", "stand");
            tag.putInt("tube_anchor_x", a.pos().getX());
            tag.putInt("tube_anchor_y", a.pos().getY());
            tag.putInt("tube_anchor_z", a.pos().getZ());
            tag.putInt("tube_anchor_slot", a.slot());
        } else {
            tag.putString("tube_anchor_type", a.kind()==Port.KIND_NOZZLE?"nozzle":"block");
            tag.putInt("tube_anchor_x", a.pos().getX());
            tag.putInt("tube_anchor_y", a.pos().getY());
            tag.putInt("tube_anchor_z", a.pos().getZ());
            tag.putString("tube_anchor_face", a.face().getName());
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void clearPending(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        for (String key : java.util.List.copyOf(tag.keySet())) if (key.startsWith("tube_anchor_") || key.equals("tube_temp_id")) tag.remove(key);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
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
                if (!tube.preview()) level.addFreshEntity(new ItemEntity(level, tube.getX(), tube.getY(), tube.getZ(),
                        tube.dropItem()));
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
                if (!tube.preview()) level.addFreshEntity(new ItemEntity(level, tube.getX(), tube.getY(), tube.getZ(),
                        tube.dropItem()));
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
                if (!tube.preview()) level.addFreshEntity(new ItemEntity(level, tube.getX(), tube.getY(), tube.getZ(),
                        tube.dropItem()));
            }
        }
    }

    /** Remove every rubber tube anchored at the given position (any anchor
     *  kind). The tube entity is always discarded; the rubber-tube item drops
     *  only when dropItem is true. Used when the anchor block is destroyed. */
    public static void dropTubesAtBlockPos(Level level, BlockPos pos, boolean dropItem) {
        if (level.isClientSide()) {
            return;
        }
        for (RubberTubeEntity tube : level.getEntitiesOfClass(RubberTubeEntity.class,
                new AABB(pos).inflate(64.0))) {
            if (anchorAt(tube.getAnchorA(), pos) || anchorAt(tube.getAnchorB(), pos)) {
                GasFlowEngine.rupture(level, tube);
                tube.discard();
                if (dropItem && !tube.preview()) {
                    level.addFreshEntity(new ItemEntity(level, tube.getX(), tube.getY(),
                            tube.getZ(),
                            tube.dropItem()));
                }
            }
        }
    }

    private static boolean anchorAt(Port a, BlockPos pos) {
        return a != null && a.pos() != null && pos.equals(a.pos());
    }

    private static boolean isStandAnchor(Port a, BlockPos pos, int slot) {
        return a != null && a.kind() == Port.KIND_STAND
                && pos.equals(a.pos()) && a.slot() == slot;
    }

    private static boolean isBlockAnchor(Port a, BlockPos pos, Direction face) {
        return a != null && a.kind() == Port.KIND_BLOCK
                && pos.equals(a.pos()) && a.face() == face;
    }

    private static boolean isNozzleAnchor(Port a, BlockPos pos) {
        return a != null && a.kind() == Port.KIND_NOZZLE && pos.equals(a.pos());
    }

    public static boolean sameAnchor(Port a, Port b) {
        if (a==null||b==null||a.kind() != b.kind()) {
            return false;
        }
        if (a.kind() == Port.KIND_ENTITY) {
            return a.uuid().equals(b.uuid());
        }
        if (a.kind() == Port.KIND_STAND) {
            return a.pos().equals(b.pos()) && a.slot() == b.slot();
        }
        return a.pos().equals(b.pos()) && a.face() == b.face();
    }
}
