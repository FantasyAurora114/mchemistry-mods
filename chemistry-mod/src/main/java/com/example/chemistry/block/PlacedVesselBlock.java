package com.example.chemistry.block;

import com.example.chemistry.LabInteractions;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.blockentity.PlacedVesselBlockEntity;
import com.example.chemistry.entity.RubberTubeEntity;
import com.example.chemistry.item.DropperItem;
import com.example.chemistry.item.RubberTubeItem;
import com.example.chemistry.registry.ModItems;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.network.chat.Component;

/** A vessel (锥形瓶 etc.) placed directly on the ground. Stores the vessel
 *  item (with contents) and draws it via {@code PlacedVesselRenderer}. */
public class PlacedVesselBlock extends Block implements EntityBlock {

    public static final MapCodec<PlacedVesselBlock> CODEC = simpleCodec(PlacedVesselBlock::new);
    private static final VoxelShape SHAPE = box(6.0, 0.0, 6.0, 11.0, 9.0, 11.0);

    public PlacedVesselBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<PlacedVesselBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** The interaction hitbox covers the stopper and inserted instruments too,
     *  so an empty hand can right-click the glass tube/funnel above the mouth
     *  to take it off (the physical shape stays small to not block walking). */
    @Override
    public VoxelShape getInteractionShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos) {
        return box(5.0, 0.0, 5.0, 12.0, 16.0, 12.0);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlacedVesselBlockEntity(pos, state);
    }

    @Override
    @org.jetbrains.annotations.Nullable
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return type == com.example.chemistry.registry.ModBlockEntities.PLACED_VESSEL.get()
                ? (lvl, pos, st, be) -> ((PlacedVesselBlockEntity) be).tickServer(lvl)
                : null;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof PlacedVesselBlockEntity be)
                || be.getVessel().isEmpty()) {
            return InteractionResult.PASS;
        }
        // Insert a glass tube / dropper / funnel through the flask's stopper.
        if (VesselHeating.isSealed(be.getVessel())
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
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "mchemistry.iron_stand.need_two_holes"), true);
                } else {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "mchemistry.iron_stand.no_hole"), true);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // Seal the flask with a rubber stopper (1 or 2 holes).
        if ((stack.is(ModItems.RUBBER_STOPPER_1_HOLE.get())
                || stack.is(ModItems.RUBBER_STOPPER_2_HOLE.get()))
                && !VesselHeating.isSealed(be.getVessel())) {
            if (!level.isClientSide()) {
                ItemStack v = be.getVessel();
                int holes = stack.is(ModItems.RUBBER_STOPPER_2_HOLE.get()) ? 2 : 1;
                VesselHeating.seal(v, holes);
                be.setVessel(v);
                stack.shrink(1);
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide() && LabInteractions.interactPlacedVessel(
                stack, be.getVessel(), be.getAttached1(), be.getAttached2(), player)) {
            be.setVessel(be.getVessel());
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8F, 1.2F);
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide() && LabInteractions.isVesselRelevant(
                stack, be.getVessel(), be.getAttached1(), be.getAttached2())) {
            return InteractionResult.SUCCESS;
        }
        // Empty hand: ask the server to run useWithoutItem (take instruments
        // off the stopper, then pick the vessel up) — the server only calls
        // useWithoutItem for a TRY_WITH_EMPTY_HAND result.
        return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PlacedVesselBlockEntity be) {
            // Mirror the iron-stand tube: take the inserted instruments off
            // first, then pick the vessel up.
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
                // Taking the glass tube out drops the whole rubber tube
                // connected to its head.
                RubberTubeItem.dropTubesConnectedToStand(level, pos.immutable(), removedSlot);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
                if (!player.getInventory().add(removed)) {
                    player.drop(removed, false);
                }
            } else if (VesselHeating.isSealed(be.getVessel())) {
                // Take the rubber stopper off before the vessel itself: hand
                // back a stopper matching the number of holes.
                ItemStack vessel = be.getVessel();
                int holes = VesselHeating.getStopperHoles(vessel);
                VesselHeating.unseal(vessel);
                be.setVessel(vessel);
                ItemStack stopper = new ItemStack(holes >= 2
                        ? ModItems.RUBBER_STOPPER_2_HOLE.get()
                        : ModItems.RUBBER_STOPPER_1_HOLE.get());
                if (!player.getInventory().add(stopper)) {
                    player.drop(stopper, false);
                }
                level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.9F, 1.0F);
            } else {
                ItemStack vessel = be.getVessel();
                if (!vessel.isEmpty()) {
                    be.setVessel(ItemStack.EMPTY);
                    level.removeBlock(pos, false);
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
                    if (!player.getInventory().add(vessel)) {
                        player.drop(vessel, false);
                    }
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative()
                && level.getBlockEntity(pos) instanceof PlacedVesselBlockEntity be
                && !be.getVessel().isEmpty()) {
            // Drop the instruments inserted through the stopper too, and free
            // any rubber tubes connected to their heads.
            for (int slot : new int[] {2, 1}) {
                ItemStack attached = slot == 2 ? be.getAttached2() : be.getAttached1();
                if (!attached.isEmpty()) {
                    RubberTubeItem.dropTubesConnectedToStand(level, pos.immutable(), slot);
                    net.minecraft.world.entity.item.ItemEntity drop =
                            new net.minecraft.world.entity.item.ItemEntity(
                                    level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                                    attached.copy());
                    drop.setDefaultPickUpDelay();
                    level.addFreshEntity(drop);
                }
            }
            net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                    level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, be.getVessel());
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Rubber-tube connection at a glass-tube head in the flask's stopper. */
    public static void handleTubeHead(Level level, Player player, ItemStack stack, BlockPos pos) {
        if (!RubberTubeItem.isWet(stack)) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.rubber_tube.need_wet"), true);
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof PlacedVesselBlockEntity be)) {
            return;
        }
        int slot = 0;
        if (isGlassTube(be.getAttached1())) {
            slot = 1;
        } else if (isGlassTube(be.getAttached2())) {
            slot = 2;
        }
        if (slot == 0) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.iron_stand.no_head"), true);
            return;
        }
        RubberTubeEntity.Anchor head = RubberTubeEntity.Anchor.stand(pos.immutable(), slot);
        if (RubberTubeItem.hasTubeAt(level, head)) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.rubber_tube.occupied"), true);
            return;
        }
        RubberTubeEntity.Anchor pending = RubberTubeItem.readPending(stack);
        if (pending == null) {
            RubberTubeItem.startPending(level, player, stack, head);
            player.displayClientMessage(
                    Component.translatable("mchemistry.rubber_tube.start_stand"), true);
        } else if (RubberTubeItem.sameAnchor(pending, head)) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.rubber_tube.same"), true);
        } else {
            RubberTubeItem.createTube(level, player, stack, pending, head);
        }
    }

    private static boolean isGlassTube(ItemStack stack) {
        return stack.is(ModItems.STRAIGHT_GLASS_TUBE.get())
                || stack.is(ModItems.RIGHT_ANGLE_GLASS_TUBE.get())
                || stack.is(ModItems.RIGHT_ANGLE_GLASS_TUBE_LONG.get());
    }
}
