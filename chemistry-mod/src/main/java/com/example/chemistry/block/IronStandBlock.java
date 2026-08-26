package com.example.chemistry.block;

import com.example.chemistry.blockentity.IronStandBlockEntity;
import com.example.chemistry.entity.RubberTubeEntity;
import com.example.chemistry.client.InvisibleBlockClientExtensions;
import com.example.chemistry.item.DropperHelper;
import com.example.chemistry.item.DropperItem;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.item.RubberTubeItem;
import com.example.chemistry.item.GasNozzleTubedItem;
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
import net.minecraft.world.phys.BlockHitResult;
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
            if (!level.isClientSide()) {
                if (level.getBlockEntity(pos) instanceof IronStandBlockEntity be) {
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
        // A tubed gas nozzle connects its free end to a glass-tube head.
        if (stack.getItem() instanceof GasNozzleTubedItem) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof IronStandBlockEntity be) {
                int slot = pickTubeSlot(level, pos, state, be, hitResult.getLocation());
                if (slot == 0) {
                    player.displayClientMessage(Component.translatable("mchemistry.iron_stand.no_head"), true);
                } else if (RubberTubeItem.hasTubeAt(level,
                        RubberTubeEntity.Anchor.stand(pos.immutable(), slot))) {
                    player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.occupied"), true);
                } else {
                    RubberTubeItem.createTube(level, player, stack,
                            RubberTubeEntity.Anchor.entity(player.getUUID()),
                            RubberTubeEntity.Anchor.stand(pos.immutable(), slot));
                }
            }
            return InteractionResult.SUCCESS;
        }
        // Insert a glass delivery tube or dropper into the rubber stopper.
        // The 2-hole stopper holds two instruments; the 1-hole holds one.
        if (state.getValue(HAS_TUBE) && state.getValue(HAS_STOPPER)
                && level.getBlockEntity(pos) instanceof IronStandBlockEntity be
                && (stack.is(ModItems.STRAIGHT_GLASS_TUBE.get())
                        || stack.is(ModItems.RIGHT_ANGLE_GLASS_TUBE.get())
                        || stack.is(ModItems.RIGHT_ANGLE_GLASS_TUBE_LONG.get())
                        || stack.is(ModItems.LONG_STEM_FUNNEL.get())
                        || stack.is(ModItems.SEPARATORY_FUNNEL.get())
                        || stack.getItem() instanceof DropperItem)) {
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
                } else if (holes < 2) {
                    player.displayClientMessage(Component.translatable("mchemistry.iron_stand.need_two_holes"), true);
                } else {
                    player.displayClientMessage(Component.translatable("mchemistry.iron_stand.no_hole"), true);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // Vessel on the ring / gauze / clay attachment (no tube mounted).
        if (state.getValue(ATTACHMENT) != 0 && !state.getValue(HAS_TUBE)
                && !state.getValue(HAS_VESSEL)
                && stack.getItem() instanceof LabVesselItem
                && !(stack.getItem() instanceof TestTubeItem)) {
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
                && (stack.is(ModItems.STRAIGHT_GLASS_TUBE.get())
                        || stack.is(ModItems.RIGHT_ANGLE_GLASS_TUBE.get())
                        || stack.is(ModItems.RIGHT_ANGLE_GLASS_TUBE_LONG.get())
                        || stack.is(ModItems.LONG_STEM_FUNNEL.get())
                        || stack.is(ModItems.SEPARATORY_FUNNEL.get())
                        || stack.getItem() instanceof DropperItem)) {
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
                } else if (holes < 2) {
                    player.displayClientMessage(Component.translatable("mchemistry.iron_stand.need_two_holes"), true);
                } else {
                    player.displayClientMessage(Component.translatable("mchemistry.iron_stand.no_hole"), true);
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
        // Attach a straight condenser to a round-bottom flask on the ring.
        if (state.getValue(HAS_VESSEL) && !state.getValue(HAS_CONDENSER)
                && stack.is(ModItems.STRAIGHT_CONDENSER.get())
                && level.getBlockEntity(pos) instanceof IronStandBlockEntity be
                && be.getVessel().is(ModItems.ROUND_BOTTOM_FLASK.get())
                && !VesselHeating.isSealed(be.getVessel())) {
            if (!level.isClientSide()) {
                be.setCondenser(true);
                stack.shrink(1);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        // Attach an Erlenmeyer flask as the distillate receiver.
        if (state.getValue(HAS_CONDENSER) && !state.getValue(HAS_RECEIVER)
                && stack.is(ModItems.ERLENMEYER_FLASK.get())
                && level.getBlockEntity(pos) instanceof IronStandBlockEntity be) {
            if (!level.isClientSide()) {
                be.setReceiver(stack.copy());
                stack.shrink(1);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        // Seal the vessel with a rubber stopper (1 or 2 holes).
        if (state.getValue(HAS_VESSEL)
                && (stack.is(ModItems.RUBBER_STOPPER_1_HOLE.get())
                        || stack.is(ModItems.RUBBER_STOPPER_2_HOLE.get()))
                && level.getBlockEntity(pos) instanceof IronStandBlockEntity be
                && !be.getVessel().isEmpty()) {
            if (!level.isClientSide()) {
                if (VesselHeating.isSealed(be.getVessel())) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.vessel.sealed"), true);
                } else {
                    ItemStack v = be.getVessel();
                    int holes = stack.is(ModItems.RUBBER_STOPPER_2_HOLE.get()) ? 2 : 1;
                    VesselHeating.seal(v, holes);
                    be.setVessel(v);
                    stack.shrink(1);
                    level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
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
        // Place an unlit alcohol lamp on the stand.
        if (stack.is(ModItems.ALCOHOL_LAMP.get()) && !state.getValue(HAS_LAMP)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(HAS_LAMP, true).setValue(LAMP_LIT, false), 3);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            stack.shrink(1);
            return InteractionResult.SUCCESS;
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
        } else if (!level.isClientSide() && level.getBlockEntity(pos) instanceof IronStandBlockEntity be) {
            ItemStack removed = null;
            int removedSlot = 0;
            if (!be.getAttached2().isEmpty()) {
                removed = be.getAttached2();
                be.setAttached2(ItemStack.EMPTY);
                removedSlot = 2;
            } else if (!be.getAttached1().isEmpty()) {
                removed = be.getAttached1();
                be.setAttached1(ItemStack.EMPTY);
                removedSlot = 1;
            }
            if (removed != null) {
                // Taking the instrument off drops the whole rubber tube that
                // was connected to its head.
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
            } else if (state.getValue(HAS_CONDENSER)) {
                be.setCondenser(false);
                ItemStack condenser = new ItemStack(ModItems.STRAIGHT_CONDENSER.get());
                if (!player.getInventory().add(condenser)) {
                    player.drop(condenser, false);
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
                level.setBlock(pos, state.setValue(HAS_LAMP, false).setValue(LAMP_LIT, false), 3);
                level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                ItemStack lamp = new ItemStack(ModItems.ALCOHOL_LAMP.get());
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
        int slot = pickTubeSlot(level, pos, state, be, click);
        if (slot == 0) {
            player.displayClientMessage(Component.translatable("mchemistry.iron_stand.no_head"), true);
            return;
        }
        RubberTubeEntity.Anchor head = RubberTubeEntity.Anchor.stand(pos.immutable(), slot);
        if (RubberTubeItem.hasTubeAt(level, head)) {
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.occupied"), true);
            return;
        }
        RubberTubeEntity.Anchor pending = RubberTubeItem.readPending(stack);
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
                RubberTubeEntity.Anchor.stand(pos.immutable(), byClick))) {
            return byClick;
        }
        int other = byClick == 1 ? 2 : 1;
        return RubberTubeItem.hasTubeAt(level,
                RubberTubeEntity.Anchor.stand(pos.immutable(), other)) ? byClick : other;
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
        if (LabVesselItem.addLiquid(tube, liquid, 5)) {
            DropperHelper.setMl(dropper, DropperHelper.getMl(dropper) - 5);
            ReactionEngine.checkAndStart(tube, player);
            be.setTube(tube);
            be.setChanged();
            level.playSound(null, be.getBlockPos(), SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8F, 1.2F);
        } else {
            player.displayClientMessage(Component.translatable("mchemistry.iron_stand.tube_full"), true);
        }
        return true;
    }

    private static boolean isGlassTube(ItemStack stack) {
        return stack.is(ModItems.STRAIGHT_GLASS_TUBE.get())
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
            net.minecraft.world.entity.item.ItemEntity lamp = new net.minecraft.world.entity.item.ItemEntity(
                    level, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                    new ItemStack(ModItems.ALCOHOL_LAMP.get()));
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
