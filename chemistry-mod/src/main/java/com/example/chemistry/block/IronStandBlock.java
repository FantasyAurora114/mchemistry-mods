package com.example.chemistry.block;

import com.example.chemistry.StandPorts;
import com.example.chemistry.blockentity.IronStandBlockEntity;
import com.example.chemistry.entity.RubberTubeEntity;
import com.example.chemistry.client.InvisibleBlockClientExtensions;
import com.example.chemistry.item.DropperHelper;
import com.example.chemistry.item.DropperItem;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.item.RubberTubeItem;
import com.example.chemistry.item.GlassTubeTubedItem;
import com.example.chemistry.item.TestTubeItem;
import com.example.chemistry.ReactionEngine;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.LabInteractions;
import com.example.chemistry.registry.ModItems;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 铁架台: right-click with a test tube mounts it; sneak-right-click rotates the
 * clamp+tube through 8 directions. An unlit alcohol lamp can be placed on the
 * stand (flint & steel lights it); the flame follows the tube bottom. The whole
 * block is drawn by {@code IronStandRenderer}, so the facing rotates the rod
 * away from the player on placement.
 */
public class IronStandBlock extends Block implements EntityBlock {

    public static final MapCodec<IronStandBlock> CODEC = simpleCodec(IronStandBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 7);
    public static final BooleanProperty HAS_TUBE = BooleanProperty.create("has_tube");
    public static final BooleanProperty HAS_CONTENTS = BooleanProperty.create("has_contents");
    public static final BooleanProperty HAS_STOPPER = BooleanProperty.create("has_stopper");
    public static final BooleanProperty HAS_LAMP = BooleanProperty.create("has_lamp");
    public static final BooleanProperty LAMP_LIT = BooleanProperty.create("lamp_lit");
    /** Heating attachment: 0 = none, 1 = iron ring, 2 = asbestos gauze, 3 = clay gauze. */
    public static final IntegerProperty ATTACHMENT = IntegerProperty.create("attachment", 0, 3);
    public static final BooleanProperty HAS_VESSEL = BooleanProperty.create("has_vessel");
    public static final BooleanProperty HAS_CONDENSER = BooleanProperty.create("has_condenser");
    public static final BooleanProperty HAS_RECEIVER = BooleanProperty.create("has_receiver");

    private static final VoxelShape SHAPE = Shapes.or(
            box(4, 0, 4, 13, 2, 13),   // base plate
            box(8, 2, 11, 9, 16, 12)); // rod

    // Rod collision rotated with the facing (the visual uses -toYRot()).
    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            box(4, 0, 4, 13, 2, 13), box(7, 2, 4, 8, 16, 5));
    private static final VoxelShape SHAPE_SOUTH = SHAPE;
    private static final VoxelShape SHAPE_WEST = Shapes.or(
            box(4, 0, 4, 13, 2, 13), box(4, 2, 8, 5, 16, 9));
    private static final VoxelShape SHAPE_EAST = Shapes.or(
            box(4, 0, 4, 13, 2, 13), box(11, 2, 7, 12, 16, 8));

    public IronStandBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.SOUTH)
                .setValue(ROTATION, 0)
                .setValue(HAS_TUBE, false)
                .setValue(HAS_CONTENTS, false)
                .setValue(HAS_STOPPER, false)
                .setValue(HAS_LAMP, false)
                .setValue(LAMP_LIT, false)
                .setValue(ATTACHMENT, 0)
                .setValue(HAS_VESSEL, false)
                .setValue(HAS_CONDENSER, false)
                .setValue(HAS_RECEIVER, false));
    }

    @Override
    public MapCodec<IronStandBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROTATION, HAS_TUBE, HAS_CONTENTS, HAS_STOPPER, HAS_LAMP, LAMP_LIT,
                ATTACHMENT, HAS_VESSEL, HAS_CONDENSER, HAS_RECEIVER);
    }

    /** The rod points away from the placing player. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    /** Everything is drawn by the block-entity renderer. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_SOUTH;
        };
    }

    /** 碰撞箱 = 铁架台本体 + 架上反应容器的实体碰撞。 */
    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, CollisionContext context) {
        VoxelShape base = getShape(state, level, pos, context);
        if (state.getValue(HAS_VESSEL)
                && level.getBlockEntity(pos) instanceof IronStandBlockEntity be
                && !be.getVessel().isEmpty()) {
            int vtype = VesselHeating.vesselType(be.getVessel());
            if (vtype != 0) {
                VoxelShape vessel = VesselHeating.vesselCollisionShape(pos, vtype,
                        VESSEL_SCALE, VESSEL_OFF_X, VESSEL_OFF_Y, VESSEL_OFF_Z,
                        -state.getValue(FACING).toYRot());
                if (!vessel.isEmpty()) {
                    base = Shapes.or(base, vessel);
                }
            }
        }
        return base;
    }

    /** 铁架台的环形/烧瓶区域没有碰撞体，交互命中框放大到整格中部，
     *  让瓶口（即使视觉上悬空）也能被射线选中。 */
    @Override
    public VoxelShape getInteractionShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos) {
        return box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
    }

    /** Server-side tick: a lit lamp keeps the mounted tube at 600 °C. */
    @Override
    @Nullable
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return type == com.example.chemistry.registry.ModBlockEntities.IRON_STAND.get()
                ? (lvl, pos, st, be) -> ((IronStandBlockEntity) be).tickServer(lvl)
                : null;
    }

    /** Walking on the stand kicks up a few particles. */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity.tickCount % 3 == 0) {
            InvisibleBlockClientExtensions.spawnStepParticles(level, pos, entity,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                            com.example.chemistry.ChemistryMod.MODID, "block/iron_stand"));
        }
        super.stepOn(level, pos, state, entity);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IronStandBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof TestTubeItem) {
            // 带试管夹的试管自带夹子，不能放上铁架台（铁架台用自己的夹子）。
            if (stack.getItem() instanceof TestTubeItem tt && tt.isClamped()) {
                if (!level.isClientSide()) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.iron_stand.no_clamped"), true);
                }
                return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide()) {
                if (level.getBlockEntity(pos) instanceof IronStandBlockEntity be) {
                    // 原来已有试管时先还给玩家，避免被静默替换丢失
                    // （带试管夹的试管同样可以放上来）。
                    if (!be.getTube().isEmpty()) {
                        ItemStack old = be.getTube();
                        be.setTube(ItemStack.EMPTY);
                        if (!player.getInventory().add(old)) {
                            player.drop(old, false);
                        }
                    }
                    be.setTube(stack.copy());
                }
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        // Connect a wet rubber tube to a glass-tube head on the stand.
        if (stack.getItem() instanceof RubberTubeItem) {
            if (!level.isClientSide()) {
                handleTubeHead(level, player, stack, pos, state, hitResult.getLocation());
            }
            return InteractionResult.SUCCESS;
        }
        // 套着橡胶管的玻璃导管：把自由端连到铁架台的玻璃导管头上。
        if (stack.getItem() instanceof GlassTubeTubedItem) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof IronStandBlockEntity be) {
                if (be.hasDistillationHead()) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.rubber_tube.head_occupied"), true);
                    return InteractionResult.SUCCESS;
                }
                int slot = pickTubeSlot(level, pos, state, be, hitResult.getLocation());
                if (slot == 0) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.iron_stand.no_head"), true);
                } else if (RubberTubeItem.hasTubeAt(level,
                        RubberTubeEntity.Port.stand(pos.immutable(), slot))) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.rubber_tube.occupied"), true);
                } else {
                    RubberTubeItem.createTube(level, player, stack,
                            RubberTubeEntity.Port.entity(player.getUUID()),
                            RubberTubeEntity.Port.stand(pos.immutable(), slot));
                }
            }
            return InteractionResult.SUCCESS;
        }
        // Insert a glass delivery tube or dropper into the rubber stopper.
        // The 2-hole stopper holds two instruments; the 1-hole holds one.
        if (state.getValue(HAS_TUBE) && state.getValue(HAS_STOPPER)
                && level.getBlockEntity(pos) instanceof IronStandBlockEntity be
                && ((stack.is(ModItems.STRAIGHT_GLASS_TUBE.get()) || stack.is(ModItems.STRAIGHT_GLASS_TUBE_LONG.get()))
                        || stack.is(ModItems.RIGHT_ANGLE_GLASS_TUBE.get())
                        || stack.is(ModItems.RIGHT_ANGLE_GLASS_TUBE_LONG.get())
                        || stack.is(ModItems.LONG_STEM_FUNNEL.get())
                        || stack.is(ModItems.SEPARATORY_FUNNEL.get())
                        || stack.getItem() instanceof DropperItem
                        || stack.is(ModItems.THERMOMETER.get()))) {
            int holes = be.getTube().getItem() instanceof TestTubeItem tt ? tt.stopperHoles() : 0;
            if (!level.isClientSide()) {
                if (be.getAttached1().isEmpty()) {
                    be.setAttached1(stack.copy());
                    stack.shrink(1);
                    level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                } else if (be.getAttached2().isEmpty() && holes >= 2) {
                    be.setAttached2(stack.copy());
                    stack.shrink(1);
                    level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                } else if (holes <= 1) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.iron_stand.one_hole_full"), true);
                } else {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.iron_stand.no_hole"), true);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // Vessel on the ring / gauze / clay attachment (no tube mounted).
        if (state.getValue(ATTACHMENT) != 0 && !state.getValue(HAS_TUBE)
                && !state.getValue(HAS_VESSEL)
                && stack.getItem() instanceof LabVesselItem
                && !(stack.getItem() instanceof TestTubeItem)
                && !(stack.getItem() instanceof com.example.chemistry.organic.PhasePipetteItem)) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof IronStandBlockEntity be) {
                be.setVessel(stack.copy());
                stack.shrink(1);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        // Insert a glass tube / dropper / funnel through the flask's stopper.
        if (state.getValue(HAS_VESSEL) && level.getBlockEntity(pos) instanceof IronStandBlockEntity be
                && !be.getVessel().isEmpty() && VesselHeating.isSealed(be.getVessel())
                && VesselHeating.getStopperHoles(be.getVessel()) > 0
                && ((stack.is(ModItems.STRAIGHT_GLASS_TUBE.get()) || stack.is(ModItems.STRAIGHT_GLASS_TUBE_LONG.get()))
                        || stack.is(ModItems.RIGHT_ANGLE_GLASS_TUBE.get())
                        || stack.is(ModItems.RIGHT_ANGLE_GLASS_TUBE_LONG.get())
                        || stack.is(ModItems.LONG_STEM_FUNNEL.get())
                        || stack.is(ModItems.SEPARATORY_FUNNEL.get())
                        || stack.getItem() instanceof DropperItem
                        || stack.is(ModItems.THERMOMETER.get()))) {
            int holes = VesselHeating.getStopperHoles(be.getVessel());
            if (!level.isClientSide()) {
                if (be.getAttached1().isEmpty()) {
                    be.setAttached1(stack.copy());
                    stack.shrink(1);
                    level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                } else if (be.getAttached2().isEmpty() && holes >= 2) {
                    be.setAttached2(stack.copy());
                    stack.shrink(1);
                    level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                } else if (holes <= 1) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.iron_stand.one_hole_full"), true);
                } else {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.iron_stand.no_hole"), true);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // Add reagents on the placed vessel (open vessel, or through an
        // inserted dropper / funnel when sealed).
        if (state.getValue(HAS_VESSEL) && level.getBlockEntity(pos) instanceof IronStandBlockEntity be
                && !be.getVessel().isEmpty()) {
            if (!level.isClientSide()) {
                if (LabInteractions.interactPlacedVessel(stack, be.getVessel(),
                        be.getAttached1(), be.getAttached2(), player)) {
                    be.setVessel(be.getVessel());
                    level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8F, 1.2F);
                }
            }
            if (LabInteractions.isVesselRelevant(stack, be.getVessel(),
                    be.getAttached1(), be.getAttached2())) {
                return InteractionResult.SUCCESS;
            }
        }
        // 接口/插头装配：先按端口命中盒选中接口，再拿对应部件接上。
        if (state.getValue(HAS_VESSEL)
                && level.getBlockEntity(pos) instanceof IronStandBlockEntity be
                && !be.getVessel().isEmpty()) {
            int port = StandPorts.pick(level, player, pos, state, be);
            ItemStack vessel = be.getVessel();
            if (port != 0) {
                boolean handled = true;
                if (port == StandPorts.MOUTH) {
                    if (stack.is(ModItems.DISTILLATION_HEAD.get())
                            && !be.hasDistillationHead()
                            && !state.getValue(HAS_CONDENSER)
                            && com.example.chemistry.GlassConnector.isOpenMouthVessel(vessel)
                            && (!VesselHeating.isThreeNeck(vessel)
                                    || !VesselHeating.neckHasStopper(vessel, 1))
                            && !VesselHeating.isSealed(vessel)) {
                        if (!level.isClientSide()) {
                            be.setDistillationHead(true);
                            stack.shrink(1);
                            level.playSound(null, pos, SoundEvents.GLASS_PLACE,
                                    SoundSource.BLOCKS, 1.0F, 1.0F);
                        }
                    } else {
                        handled = false; // 塞橡胶塞/玻璃塞走下面的分支
                    }
                } else if (port == StandPorts.HEAD_ARM
                        && stack.is(ModItems.STRAIGHT_CONDENSER.get())
                        && !state.getValue(HAS_CONDENSER)
                        && be.hasDistillationHead()) {
                    if (!level.isClientSide()) {
                        be.setCondenser(true);
                        stack.shrink(1);
                        level.playSound(null, pos, SoundEvents.GLASS_PLACE,
                                SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                } else if (port == StandPorts.HEAD
                        && stack.is(ModItems.THERMOMETER.get())
                        && be.hasDistillationHead() && !be.hasHeadThermometer()) {
                    if (!level.isClientSide()) {
                        be.setHeadThermometer(true);
                        stack.shrink(1);
                        level.playSound(null, pos, SoundEvents.GLASS_PLACE,
                                SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                } else if (port == StandPorts.CONDENSER_END
                        && state.getValue(HAS_CONDENSER)
                        && !be.hasReceiverAdapter()
                        && (stack.is(ModItems.RECEIVER_ADAPTER_BENT.get())
                                || stack.is(ModItems.RECEIVER_ADAPTER_STRAIGHT.get()))) {
                    if (!level.isClientSide()) {
                        be.setReceiverAdapter(stack.copy());
                        stack.shrink(1);
                        level.playSound(null, pos, SoundEvents.GLASS_PLACE,
                                SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                } else if (port == StandPorts.RECEIVER
                        && state.getValue(HAS_CONDENSER)
                        && !state.getValue(HAS_RECEIVER)
                        && stack.is(ModItems.ERLENMEYER_FLASK.get())) {
                    if (!level.isClientSide()) {
                        be.setReceiver(stack.copy());
                        stack.shrink(1);
                        level.playSound(null, pos, SoundEvents.GLASS_PLACE,
                                SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                } else {
                    handled = false;
                }
                if (handled) {
                    return InteractionResult.SUCCESS;
                }
                // 对准了接口但部件不匹配：提示（塞子类走下面的分支）。
                if (stack.is(ModItems.DISTILLATION_HEAD.get())
                        || stack.is(ModItems.STRAIGHT_CONDENSER.get())
                        || stack.is(ModItems.THERMOMETER.get())
                        || stack.is(ModItems.RECEIVER_ADAPTER_BENT.get())
                        || stack.is(ModItems.RECEIVER_ADAPTER_STRAIGHT.get())
                        || stack.is(ModItems.ERLENMEYER_FLASK.get())) {
                    if (!level.isClientSide()) {
                        player.displayClientMessage(
                                Component.translatable("mchemistry.iron_stand.aim_port"), true);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }
        // 三颈烧瓶在铁圈上：把玻璃塞塞进正看着的瓶口。
        if (state.getValue(HAS_VESSEL) && stack.is(ModItems.GLASS_STOPPER.get())
                && level.getBlockEntity(pos) instanceof IronStandBlockEntity be
                && VesselHeating.isThreeNeck(be.getVessel())) {
            if (!level.isClientSide()) {
                ItemStack flask = be.getVessel();
                if (LabInteractions.tryPlugNeck(flask, stack, player, pos,
                        hitResult.getLocation(), VESSEL_SCALE, VESSEL_OFF_X,
                        VESSEL_OFF_Y, VESSEL_OFF_Z, -state.getValue(FACING).toYRot())) {
                    be.setVessel(flask);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // 橡胶塞：三颈瓶按点击位置塞进某个瓶口；其它烧瓶整瓶密封。
        if (state.getValue(HAS_VESSEL)
                && (stack.is(ModItems.RUBBER_STOPPER_1_HOLE.get())
                        || stack.is(ModItems.RUBBER_STOPPER_2_HOLE.get())
                        || stack.is(ModItems.RUBBER_STOPPER_3_HOLE.get()))
                && level.getBlockEntity(pos) instanceof IronStandBlockEntity be
                && !be.getVessel().isEmpty()) {
            // 瓶口端口被占用（如蒸馏头插头）：不能再塞橡胶塞。
            if (com.example.chemistry.GlassConnector.isMouthPortOccupied(be)) {
                if (!level.isClientSide()) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.iron_stand.port_blocks_stopper"), true);
                }
                return InteractionResult.SUCCESS;
            }
            int holes = stack.is(ModItems.RUBBER_STOPPER_3_HOLE.get()) ? 3
                    : stack.is(ModItems.RUBBER_STOPPER_2_HOLE.get()) ? 2 : 1;
            if (VesselHeating.isThreeNeck(be.getVessel())) {
                if (!level.isClientSide()) {
                    ItemStack v = be.getVessel();
                    if (LabInteractions.tryPlugRubberNeck(v, stack, player, pos,
                            hitResult.getLocation(), holes, VESSEL_SCALE, VESSEL_OFF_X,
                            VESSEL_OFF_Y, VESSEL_OFF_Z, -state.getValue(FACING).toYRot())) {
                        be.setVessel(v);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide()) {
                if (VesselHeating.isSealed(be.getVessel())) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.vessel.sealed"), true);
                } else {
                    ItemStack v = be.getVessel();
                    int vtype = VesselHeating.vesselType(v);
                    if (vtype != 0 && !VesselHeating.mouthForRay(pos, vtype,
                            VESSEL_SCALE, VESSEL_OFF_X, VESSEL_OFF_Y, VESSEL_OFF_Z,
                            -state.getValue(FACING).toYRot(), player)) {
                        player.displayClientMessage(
                                Component.translatable("mchemistry.flask.aim_neck"), true);
                    } else {
                        VesselHeating.seal(v, holes);
                        be.setVessel(v);
                        stack.shrink(1);
                        level.playSound(null, pos, SoundEvents.WOOL_PLACE,
                                SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
        // Crucible tongs pick up a crucible sitting on the ring.
        if (state.getValue(HAS_VESSEL) && stack.is(ModItems.CRUCIBLE_TONGS.get())
                && level.getBlockEntity(pos) instanceof IronStandBlockEntity be
                && be.getVessel().is(ModItems.CRUCIBLE.get())) {
            if (!level.isClientSide()) {
                ItemStack crucible = be.getVessel();
                be.setVessel(ItemStack.EMPTY);
                if (!player.getInventory().add(crucible)) {
                    player.drop(crucible, false);
                }
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        // Place an unlit alcohol lamp / blowtorch on the stand.
        if (!state.getValue(HAS_LAMP)
                && (stack.is(ModItems.ALCOHOL_LAMP.get())
                        || stack.is(ModItems.ALCOHOL_BLOWTORCH.get()))) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(HAS_LAMP, true).setValue(LAMP_LIT, false), 3);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (level.getBlockEntity(pos) instanceof IronStandBlockEntity standBe) {
                    standBe.setLampBlowtorch(stack.is(ModItems.ALCOHOL_BLOWTORCH.get()));
                }
            }
            stack.shrink(1);
            return InteractionResult.SUCCESS;
        }
        // 射线命中酒精灯：点燃 / 盖帽（不要求先拆其他部件）。
        if (hitsLamp(player, pos) && state.getValue(HAS_LAMP)) {
            if (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE)) {
                if (!state.getValue(LAMP_LIT)) {
                    if (!level.isClientSide()) {
                        level.setBlock(pos, state.setValue(LAMP_LIT, true), 3);
                        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE,
                                SoundSource.BLOCKS, 1.0F, 1.0F);
                        if (stack.is(Items.FIRE_CHARGE)) {
                            stack.shrink(1);
                        }
                    }
                }
                return InteractionResult.SUCCESS;
            }
            if (stack.is(ModItems.ALCOHOL_LAMP_CAP.get())) {
                boolean blowtorch = level.getBlockEntity(pos) instanceof IronStandBlockEntity standBe
                        && standBe.isLampBlowtorch();
                if (blowtorch) {
                    if (!level.isClientSide()) {
                        player.displayClientMessage(
                                Component.translatable("mchemistry.alcohol_blowtorch.no_cap"), true);
                    }
                    return InteractionResult.SUCCESS;
                }
                if (!level.isClientSide()) {
                    // 盖上灯帽：灯熄灭并标记已盖帽（随灯一起取下）。
                    level.setBlock(pos, state.setValue(LAMP_LIT, false), 3);
                    if (level.getBlockEntity(pos) instanceof IronStandBlockEntity standBe) {
                        standBe.setLampCapped(true);
                    }
                    stack.shrink(1);
                    level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }
        }
        // Flint & steel / fire charge light the lamp on the stand.
        if ((stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE))
                && state.getValue(HAS_LAMP) && !state.getValue(LAMP_LIT)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(LAMP_LIT, true), 3);
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (stack.is(Items.FIRE_CHARGE)) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // Attach an iron ring / asbestos gauze / clay gauze as a heating support.
        int attachType = attachmentType(stack);
        if (attachType != 0) {
            if (!level.isClientSide()) {
                if (state.getValue(ATTACHMENT) != 0) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.iron_stand.attachment_occupied"), true);
                } else {
                    level.setBlock(pos, state.setValue(ATTACHMENT, attachType), 3);
                    level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 0.6F, 1.0F);
                    stack.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            rotate(level, pos, state, player);
            return InteractionResult.SUCCESS;
        }
        // Fall through to the empty-hand interaction (pick the tube back up).
        // Never trigger the empty-hand pickup while holding an item: the server
        // calls useWithoutItem for ANY TryEmptyHand result, even with a
        // non-empty main hand.
        return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
    }

    /** Empty hand: sneak rotates; otherwise squeeze dropper / remove instrument / pick up. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            rotate(level, pos, state, player);
        } else if (hitsLamp(player, pos) && state.getValue(HAS_LAMP)) {
            // 射线命中酒精灯：随时取下（即使架上有烧瓶等其他部件）。
            if (!level.isClientSide()) {
                boolean blowtorch = level.getBlockEntity(pos) instanceof IronStandBlockEntity standBe
                        && standBe.isLampBlowtorch();
                boolean capped = level.getBlockEntity(pos) instanceof IronStandBlockEntity standBe2
                        && standBe2.isLampCapped();
                level.setBlock(pos, state.setValue(HAS_LAMP, false).setValue(LAMP_LIT, false), 3);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
                ItemStack lamp = new ItemStack(blowtorch
                        ? ModItems.ALCOHOL_BLOWTORCH.get()
                        : (capped ? ModItems.ALCOHOL_LAMP_CAPPED.get() : ModItems.ALCOHOL_LAMP.get()));
                if (!player.getInventory().add(lamp)) {
                    player.drop(lamp, false);
                }
            }
            return InteractionResult.SUCCESS;
        } else if (!level.isClientSide() && level.getBlockEntity(pos) instanceof IronStandBlockEntity be) {
            // 右击胶头滴管的红色胶头：挤压给液（替代 K 键）。
            Vec3 bulb = dropperBulbCenter(level, pos);
            if (bulb != null && hitResult.getLocation().distanceToSqr(bulb) <= 0.0081) {
                squeezeAt(level, pos, player);
                return InteractionResult.SUCCESS;
            }
            // 容器上的接口/塞子/附件：射线选中端口命中盒，取下对应部件。
            if (state.getValue(HAS_VESSEL) && !be.getVessel().isEmpty()) {
                int port = StandPorts.pick(level, player, pos, state, be);
                if (port == StandPorts.ATTACHED_1 || port == StandPorts.ATTACHED_2) {
                    int slot = port == StandPorts.ATTACHED_1 ? 1 : 2;
                    ItemStack removed = slot == 1 ? be.getAttached1() : be.getAttached2();
                    if (slot == 1) {
                        be.setAttached1(ItemStack.EMPTY);
                    } else {
                        be.setAttached2(ItemStack.EMPTY);
                    }
                    RubberTubeItem.dropTubesConnectedToStand(level, pos.immutable(), slot);
                    if (!player.getInventory().add(removed)) {
                        player.drop(removed, false);
                    }
                    return InteractionResult.SUCCESS;
                }
                if (port == StandPorts.HEAD) {
                    if (be.hasCondenser()) {
                        if (!level.isClientSide()) {
                            player.displayClientMessage(
                                    Component.translatable("mchemistry.iron_stand.remove_condenser_first"), true);
                        }
                        return InteractionResult.SUCCESS;
                    }
                    be.setDistillationHead(false);
                    ItemStack head = new ItemStack(ModItems.DISTILLATION_HEAD.get());
                    if (!player.getInventory().add(head)) {
                        player.drop(head, false);
                    }
                    if (be.hasHeadThermometer()) {
                        be.setHeadThermometer(false);
                        ItemStack thermo = new ItemStack(ModItems.THERMOMETER.get());
                        if (!player.getInventory().add(thermo)) {
                            player.drop(thermo, false);
                        }
                    }
                    return InteractionResult.SUCCESS;
                }
                boolean three = VesselHeating.isThreeNeck(be.getVessel());
                if (port == StandPorts.MOUTH || port == StandPorts.NECK_LEFT
                        || port == StandPorts.NECK_RIGHT) {
                    if (three) {
                        int neck = port == StandPorts.NECK_LEFT ? 0
                                : port == StandPorts.NECK_RIGHT ? 2 : 1;
                        ItemStack flask = be.getVessel();
                        if (LabInteractions.tryUnplugRubberNeck(flask, neck, player)
                                || LabInteractions.tryUnplugNeck(flask, neck, player)) {
                            be.setVessel(flask);
                            return InteractionResult.SUCCESS;
                        }
                        player.displayClientMessage(
                                Component.translatable("mchemistry.flask.neck_empty"), true);
                        return InteractionResult.SUCCESS;
                    }
                    // 单口瓶：取下瓶口橡胶塞（有附件时提示先取附件）。
                    ItemStack v = be.getVessel();
                    if (VesselHeating.isSealed(v)) {
                        if (!be.getAttached1().isEmpty() || !be.getAttached2().isEmpty()) {
                            player.displayClientMessage(
                                    Component.translatable(
                                            "mchemistry.iron_stand.remove_instruments_first"), true);
                        } else {
                            int holes = VesselHeating.getStopperHoles(v);
                            VesselHeating.unseal(v);
                            be.setVessel(v);
                            ItemStack stopper = new ItemStack(ModItems.stopperForHoles(holes));
                            if (!player.getInventory().add(stopper)) {
                                player.drop(stopper, false);
                            }
                        }
                        return InteractionResult.SUCCESS;
                    }
                }
            }
            // 试管（夹子上）的附件保持固定顺序：先取附件再取试管。
            ItemStack removed = null;
            int removedSlot = 0;
            if (state.getValue(HAS_TUBE) && !be.getAttached2().isEmpty()) {
                removed = be.getAttached2();
                be.setAttached2(ItemStack.EMPTY);
                removedSlot = 2;
            } else if (state.getValue(HAS_TUBE) && !be.getAttached1().isEmpty()) {
                removed = be.getAttached1();
                be.setAttached1(ItemStack.EMPTY);
                removedSlot = 1;
            }
            if (removed != null) {
                RubberTubeItem.dropTubesConnectedToStand(level, pos.immutable(), removedSlot);
                if (!player.getInventory().add(removed)) {
                    player.drop(removed, false);
                }
            } else if (state.getValue(HAS_TUBE)) {
                ItemStack tube = be.getTube();
                if (!tube.isEmpty()) {
                    be.setTube(ItemStack.EMPTY);
                    if (!player.getInventory().add(tube)) {
                        player.drop(tube, false);
                    }
                }
            } else if (state.getValue(HAS_RECEIVER)) {
                ItemStack receiver = be.getReceiver();
                be.setReceiver(ItemStack.EMPTY);
                if (!player.getInventory().add(receiver)) {
                    player.drop(receiver, false);
                }
            } else if (be.hasReceiverAdapter()) {
                ItemStack adapter = be.getReceiverAdapter();
                be.setReceiverAdapter(ItemStack.EMPTY);
                if (!player.getInventory().add(adapter)) {
                    player.drop(adapter, false);
                }
            } else if (state.getValue(HAS_CONDENSER)) {
                be.setCondenser(false);
                ItemStack condenser = new ItemStack(ModItems.STRAIGHT_CONDENSER.get());
                if (!player.getInventory().add(condenser)) {
                    player.drop(condenser, false);
                }
            } else if (be.hasDistillationHead()) {
                be.setDistillationHead(false);
                ItemStack head = new ItemStack(ModItems.DISTILLATION_HEAD.get());
                if (!player.getInventory().add(head)) {
                    player.drop(head, false);
                }
                if (be.hasHeadThermometer()) {
                    be.setHeadThermometer(false);
                    ItemStack thermo = new ItemStack(ModItems.THERMOMETER.get());
                    if (!player.getInventory().add(thermo)) {
                        player.drop(thermo, false);
                    }
                }
            } else if (state.getValue(HAS_VESSEL)) {
                ItemStack vessel = be.getVessel();
                for (ItemStack attached : new ItemStack[] {be.getAttached2(), be.getAttached1()}) {
                    if (!attached.isEmpty()) {
                        if (!player.getInventory().add(attached)) {
                            player.drop(attached, false);
                        }
                    }
                }
                be.setAttached1(ItemStack.EMPTY);
                be.setAttached2(ItemStack.EMPTY);
                be.setVessel(ItemStack.EMPTY);
                if (!player.getInventory().add(vessel)) {
                    player.drop(vessel, false);
                }
            } else if (state.getValue(HAS_LAMP)) {
                boolean blowtorch = level.getBlockEntity(pos) instanceof IronStandBlockEntity standBe
                        && standBe.isLampBlowtorch();
                level.setBlock(pos, state.setValue(HAS_LAMP, false).setValue(LAMP_LIT, false), 3);
                level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                ItemStack lamp = new ItemStack(blowtorch
                        ? ModItems.ALCOHOL_BLOWTORCH.get()
                        : ModItems.ALCOHOL_LAMP.get());
                if (!player.getInventory().add(lamp)) {
                    player.drop(lamp, false);
                }
            } else if (state.getValue(ATTACHMENT) != 0) {
                int type = state.getValue(ATTACHMENT);
                level.setBlock(pos, state.setValue(ATTACHMENT, 0), 3);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.0F);
                ItemStack attach = attachmentItem(type);
                if (!player.getInventory().add(attach)) {
                    player.drop(attach, false);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** 玩家视线是否命中铁架台底座上的酒精灯。 */
    private static boolean hitsLamp(Player player, BlockPos pos) {
        if (player == null) {
            return false;
        }
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(6.0));
        AABB box = new AABB(pos.getX() + 0.33, pos.getY() + 0.08, pos.getZ() + 0.33,
                pos.getX() + 0.70, pos.getY() + 0.38, pos.getZ() + 0.70);
        return box.clip(from, to).isPresent();
    }

    /** Type code for a heating-attachment item (0 = not one). */
    private static int attachmentType(ItemStack stack) {
        if (stack.is(ModItems.IRON_RING.get())) {
            return 1;
        }
        if (stack.is(ModItems.ASBESTOS_GAUZE.get())) {
            return 2;
        }
        if (stack.is(ModItems.CLAY_GAUZE.get())) {
            return 3;
        }
        return 0;
    }

    private static ItemStack attachmentItem(int type) {
        return switch (type) {
            case 1 -> new ItemStack(ModItems.IRON_RING.get());
            case 2 -> new ItemStack(ModItems.ASBESTOS_GAUZE.get());
            default -> new ItemStack(ModItems.CLAY_GAUZE.get());
        };
    }

    /** Rubber-tube connection at a glass-tube head (shared with the sneak path
     *  in RubberTubeItem, so tube-endpoint clicks always win). */
    public static void handleTubeHead(Level level, Player player, ItemStack stack,
            BlockPos pos, BlockState state, net.minecraft.world.phys.Vec3 click) {
        if (!RubberTubeItem.isWet(stack)) {
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.need_wet"), true);
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof IronStandBlockEntity be)) {
            return;
        }
        // 蒸馏头已占用瓶口：橡胶管不能接在烧瓶的导气管头上。
        if (be.hasDistillationHead()) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.rubber_tube.head_occupied"), true);
            return;
        }
        int slot = pickTubeSlot(level, pos, state, be, click);
        if (slot == 0) {
            player.displayClientMessage(Component.translatable("mchemistry.iron_stand.no_head"), true);
            return;
        }
        RubberTubeEntity.Port head = RubberTubeEntity.Port.stand(pos.immutable(), slot);
        if (RubberTubeItem.hasTubeAt(level, head)) {
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.occupied"), true);
            return;
        }
        // 副手拿玻璃导管：从手上的导管直接连到这个导管头（一次右键完成）。
        if (com.example.chemistry.item.GlassTubeItem.isGlassTube(player.getOffhandItem())) {
            RubberTubeItem.createTube(level, player, stack,
                    RubberTubeEntity.Port.entity(player.getUUID()), head);
            return;
        }
        RubberTubeEntity.Port pending = RubberTubeItem.readPending(stack);
        if (pending == null) {
            RubberTubeItem.startPending(level, player, stack, head);
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.start_stand"), true);
        } else if (RubberTubeItem.sameAnchor(pending, head)) {
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.same"), true);
        } else {
            RubberTubeItem.createTube(level, player, stack, pending, head);
        }
    }

    /** Choose which glass-tube head a click targets: a two-hole stopper has
     *  two heads offset by ±0.55; pick by the click position, then fall back
     *  to the other head when the clicked one is already occupied. */
    private static int pickTubeSlot(Level level, BlockPos pos, BlockState state,
            IronStandBlockEntity be, net.minecraft.world.phys.Vec3 click) {
        boolean a1 = isGlassTube(be.getAttached1());
        boolean a2 = isGlassTube(be.getAttached2());
        if (!a1 && !a2) {
            return 0;
        }
        if (a1 != a2) {
            return a1 ? 1 : 2;
        }
        boolean vesselCase = !be.getVessel().isEmpty();
        int holes = vesselCase
                ? VesselHeating.getStopperHoles(be.getVessel())
                : (be.getTube().getItem() instanceof TestTubeItem tt ? tt.stopperHoles() : 0);
        double zCenter = vesselCase ? 7.5 : 8.8333;
        double hole1 = vesselCase ? (holes < 2 ? 0.0 : -0.55) : (holes < 2 ? 0.0 : 0.55);
        double hole2 = vesselCase ? 0.55 : -0.55;
        double z1 = (zCenter + hole1) / 16.0;
        double z2 = (zCenter + hole2) / 16.0;
        int byClick = 1;
        if (click != null) {
            Direction facing = state.getValue(FACING);
            double yaw = Math.toRadians(-facing.toYRot());
            double c = Math.cos(yaw);
            double s = Math.sin(yaw);
            double dx = click.x - (pos.getX() + 0.5);
            double dz = click.z - (pos.getZ() + 0.5);
            double lz = 0.5 + dx * s + dz * c;
            byClick = Math.abs(lz - z1) <= Math.abs(lz - z2) ? 1 : 2;
        }
        if (!RubberTubeItem.hasTubeAt(level,
                RubberTubeEntity.Port.stand(pos.immutable(), byClick))) {
            return byClick;
        }
        int other = byClick == 1 ? 2 : 1;
        return RubberTubeItem.hasTubeAt(level,
                RubberTubeEntity.Port.stand(pos.immutable(), other)) ? byClick : other;
    }

    /** Squeeze an inserted dropper (K key): drip 1 mL into the mounted tube. */
    public static boolean squeezeAt(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof IronStandBlockEntity be)) {
            return false;
        }
        ItemStack tube = be.getTube();
        if (tube.isEmpty()) {
            return false;
        }
        ItemStack dropper = !be.getAttached2().isEmpty() && DropperHelper.isDropper(be.getAttached2())
                ? be.getAttached2()
                : DropperHelper.isDropper(be.getAttached1()) ? be.getAttached1() : ItemStack.EMPTY;
        if (dropper.isEmpty()) {
            return false;
        }
        if (DropperHelper.isEmpty(dropper)) {
            // An empty dropper comes off on the next right-click.
            return false;
        }
        String liquid = DropperHelper.getLiquid(dropper);
        if (DropperHelper.pour(dropper,tube,player.isShiftKeyDown())) {
            ReactionEngine.checkAndStart(tube, player);
            be.setTube(tube);
            be.setChanged();
            level.playSound(null, be.getBlockPos(), SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8F, 1.2F);
        } else {
            player.displayClientMessage(Component.translatable("mchemistry.iron_stand.tube_full"), true);
        }
        return true;
    }

    // 铁圈上烧瓶的渲染变换（与 IronStandRenderer.renderVessel 一致）。
    public static final double VESSEL_SCALE = 0.6;
    public static final double VESSEL_OFF_X = (8.5 - 8.5 * VESSEL_SCALE) / 16.0;
    public static final double VESSEL_OFF_Y = 9.5 / 16.0;
    public static final double VESSEL_OFF_Z = (9.0 - 8.5 * VESSEL_SCALE) / 16.0;

    /** 铁架台上胶头滴管红色胶头的世界坐标（无滴管时返回 null）。 */
    public static Vec3 dropperBulbCenter(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof IronStandBlockEntity be)) {
            return null;
        }
        int slot;
        if (DropperHelper.isDropper(be.getAttached2())) {
            slot = 2;
        } else if (DropperHelper.isDropper(be.getAttached1())) {
            slot = 1;
        } else {
            return null;
        }
        int holes = be.getTube().getItem() instanceof TestTubeItem tt ? tt.stopperHoles() : 0;
        BlockState state = level.getBlockState(pos);
        double angle = Math.toRadians(state.getValue(ROTATION) * 45.0);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double hole = slot == 1 ? (holes < 2 ? 0.0 : 0.55) : -0.55;
        // 与 IronStandRenderer.renderAttached 相同的口部坐标 + 沿口轴向上 2.9 到胶头。
        double mx = 8.5 + 0.3333 * cos - 2.5 * sin;
        double my = 9.5 + 0.3333 * sin + 2.5 * cos;
        double bx = mx - 2.9 * sin;
        double by = my + 2.9 * cos;
        double bz = 8.8333 + hole;
        double lx = bx / 16.0;
        double ly = by / 16.0;
        double lz = bz / 16.0;
        Direction facing = state.getValue(FACING);
        double yaw = Math.toRadians(-facing.toYRot());
        double c = Math.cos(yaw);
        double s2 = Math.sin(yaw);
        double wx = pos.getX() + 0.5 + (lx - 0.5) * c + (lz - 0.5) * s2;
        double wz = pos.getZ() + 0.5 - (lx - 0.5) * s2 + (lz - 0.5) * c;
        return new Vec3(wx, pos.getY() + ly, wz);
    }

    private static boolean isGlassTube(ItemStack stack) {
        return (stack.is(ModItems.STRAIGHT_GLASS_TUBE.get()) || stack.is(ModItems.STRAIGHT_GLASS_TUBE_LONG.get()))
                || stack.is(ModItems.RIGHT_ANGLE_GLASS_TUBE.get())
                || stack.is(ModItems.RIGHT_ANGLE_GLASS_TUBE_LONG.get());
    }

    /** Rotate one step; a fully inverted tube spills its contents. */
    private void rotate(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide()) {
            return;
        }
        BlockState rotated = state.cycle(ROTATION);
        level.setBlock(pos, rotated, 3);
        if (rotated.getValue(ROTATION) == 4 && level.getBlockEntity(pos) instanceof IronStandBlockEntity be) {
            ItemStack tube = be.getTube();
            if (!LabVesselItem.getContents(tube).isEmpty()) {
                LabVesselItem.clearContents(tube);
                be.setTube(tube);
                player.displayClientMessage(
                        Component.translatable("mchemistry.iron_stand.spilled"), true);
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof IronStandBlockEntity be) {
            RubberTubeItem.dropTubesConnectedToStand(level, pos, 1);
            RubberTubeItem.dropTubesConnectedToStand(level, pos, 2);
            for (ItemStack attached : new ItemStack[] {be.getAttached2(), be.getAttached1()}) {
                if (!attached.isEmpty()) {
                    net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                            level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, attached.copy());
                    drop.setDefaultPickUpDelay();
                    level.addFreshEntity(drop);
                }
            }
            ItemStack tube = be.getTube();
            if (!tube.isEmpty()) {
                be.setTube(ItemStack.EMPTY);
                net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, tube);
                drop.setDefaultPickUpDelay();
                level.addFreshEntity(drop);
            }
            ItemStack vessel = be.getVessel();
            if (!vessel.isEmpty()) {
                net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, vessel);
                drop.setDefaultPickUpDelay();
                level.addFreshEntity(drop);
            }
            if (be.hasReceiverAdapter()) {
                net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                        be.getReceiverAdapter().copy());
                drop.setDefaultPickUpDelay();
                level.addFreshEntity(drop);
            }
            if (be.hasDistillationHead()) {
                net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                        new ItemStack(ModItems.DISTILLATION_HEAD.get()));
                drop.setDefaultPickUpDelay();
                level.addFreshEntity(drop);
                if (be.hasHeadThermometer()) {
                    net.minecraft.world.entity.item.ItemEntity thermoDrop = new net.minecraft.world.entity.item.ItemEntity(
                            level, pos.getX() + 0.5, pos.getY() + 0.35, pos.getZ() + 0.5,
                            new ItemStack(ModItems.THERMOMETER.get()));
                    thermoDrop.setDefaultPickUpDelay();
                    level.addFreshEntity(thermoDrop);
                }
            }
            if (be.hasCondenser()) {
                net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                        new ItemStack(ModItems.STRAIGHT_CONDENSER.get()));
                drop.setDefaultPickUpDelay();
                level.addFreshEntity(drop);
            }
            ItemStack receiver = be.getReceiver();
            if (!receiver.isEmpty()) {
                net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, receiver);
                drop.setDefaultPickUpDelay();
                level.addFreshEntity(drop);
            }
        }
        if (!level.isClientSide() && state.getValue(HAS_LAMP) && !player.isCreative()) {
            boolean blowtorch = level.getBlockEntity(pos) instanceof IronStandBlockEntity standBe
                    && standBe.isLampBlowtorch();
            net.minecraft.world.entity.item.ItemEntity lamp = new net.minecraft.world.entity.item.ItemEntity(
                    level, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                    new ItemStack(blowtorch
                            ? ModItems.ALCOHOL_BLOWTORCH.get()
                            : ModItems.ALCOHOL_LAMP.get()));
            lamp.setDefaultPickUpDelay();
            level.addFreshEntity(lamp);
        }
        if (!level.isClientSide() && state.getValue(ATTACHMENT) != 0 && !player.isCreative()) {
            net.minecraft.world.entity.item.ItemEntity attach = new net.minecraft.world.entity.item.ItemEntity(
                    level, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                    attachmentItem(state.getValue(ATTACHMENT)));
            attach.setDefaultPickUpDelay();
            level.addFreshEntity(attach);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
