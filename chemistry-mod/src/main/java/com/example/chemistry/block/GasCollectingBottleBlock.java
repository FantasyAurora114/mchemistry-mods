package com.example.chemistry.block;

import com.example.chemistry.item.RubberTubeItem;
import com.example.chemistry.entity.RubberTubeEntity;
import com.example.chemistry.blockentity.GasCollectingBottleBlockEntity;
import com.example.chemistry.entity.RubberTubeEntity.Anchor;
import com.example.chemistry.registry.ModItems;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 集气瓶: placeable bottle; sneak + right-click flips the up/down orientation,
 *  glass-sheet/nozzle items add those parts, empty-hand right-click removes the
 *  nozzle then the glass plate (the bottle itself stays). Breaking drops the
 *  complete gas bottle (plus the nozzle if one was inserted). The block entity
 *  remembers the gas. */
public class GasCollectingBottleBlock extends Block implements EntityBlock {

    public static final MapCodec<GasCollectingBottleBlock> CODEC = simpleCodec(GasCollectingBottleBlock::new);
    public static final BooleanProperty INVERTED = BooleanProperty.create("inverted");
    public static final BooleanProperty HAS_PLATE = BooleanProperty.create("has_plate");
    public static final BooleanProperty HAS_NOZZLE = BooleanProperty.create("has_nozzle");

    private static final VoxelShape SHAPE = box(5.0, 0.0, 5.0, 12.0, 7.2, 12.0);

    public GasCollectingBottleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(INVERTED, false)
                .setValue(HAS_PLATE, false)
                .setValue(HAS_NOZZLE, false));
    }

    @Override
    public MapCodec<GasCollectingBottleBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INVERTED, HAS_PLATE, HAS_NOZZLE);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GasCollectingBottleBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        // Wet rubber tube: anchor/connect at the gas nozzle inserted in the
        // mouth. The tube wins over nozzle removal, also when the main hand is
        // empty but the offhand holds the tube.
        if ((stack.getItem() instanceof RubberTubeItem
                || (stack.isEmpty() && player.getOffhandItem().getItem() instanceof RubberTubeItem))
                && state.getValue(HAS_NOZZLE)) {
            ItemStack tube = stack.isEmpty() ? player.getOffhandItem() : stack;
            if (!level.isClientSide()) {
                handleTubeNozzle(level, player, tube, pos, state);
            }
            return InteractionResult.SUCCESS;
        }
        // Insert a gas nozzle through the glass-plate opening.
        if (stack.is(ModItems.GAS_NOZZLE.get()) && state.getValue(HAS_PLATE) && !state.getValue(HAS_NOZZLE)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(HAS_NOZZLE, true), 3);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        // Put the glass plate on the mouth.
        if (stack.is(ModItems.GLASS_SHEET.get()) && !state.getValue(HAS_PLATE) && !state.getValue(HAS_NOZZLE)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(HAS_PLATE, true), 3);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        // Empty-hand right-click removes the nozzle, then the glass plate.
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        // Any other held item does nothing, so the bottle is never picked up or
        // broken by a right-click.
        return InteractionResult.SUCCESS;
    }

    /** Rubber-tube connection at the bottle's nozzle (shared with the sneak
     *  path in RubberTubeItem). */
    public static void handleTubeNozzle(Level level, Player player, ItemStack stack, BlockPos pos, BlockState state) {
        if (!RubberTubeItem.isWet(stack)) {
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.need_wet"), true);
            return;
        }
        Anchor nozzle = Anchor.nozzle(pos, Direction.UP);
        if (RubberTubeItem.hasTubeAt(level, nozzle)) {
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.occupied"), true);
            return;
        }
        Anchor pending = RubberTubeItem.readPending(stack);
        if (pending == null) {
            RubberTubeItem.startPending(level, player, stack, nozzle);
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.start_nozzle"), true);
        } else if (RubberTubeItem.sameAnchor(pending, nozzle)) {
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.same"), true);
        } else {
            RubberTubeItem.createTube(level, player, stack, pending, nozzle);
        }
    }

    /** Empty hand: remove the nozzle first, then the glass plate. The bottle itself stays. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        // Never remove the nozzle while the player is handling a rubber tube
        // (e.g. holding it in the offhand): connect instead.
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ItemStack tube = main.getItem() instanceof RubberTubeItem ? main
                : off.getItem() instanceof RubberTubeItem ? off : ItemStack.EMPTY;
        if (state.getValue(HAS_NOZZLE) && !tube.isEmpty()) {
            if (!level.isClientSide()) {
                handleTubeNozzle(level, player, tube, pos, state);
            }
            return InteractionResult.SUCCESS;
        }
        // Empty hand: complete a pending tube anchored to the player, and
        // never remove a nozzle that already has a tube on it.
        if (state.getValue(HAS_NOZZLE)
                && RubberTubeItem.handleNozzleEmptyClick(level, player,
                        RubberTubeEntity.Anchor.nozzle(pos.immutable(), Direction.UP))) {
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            ItemStack give;
            if (state.getValue(HAS_NOZZLE)) {
                RubberTubeItem.detachTubesAt(level, RubberTubeEntity.Anchor.nozzle(pos.immutable(), Direction.UP));
                give = new ItemStack(ModItems.GAS_NOZZLE.get());
                level.setBlock(pos, state.setValue(HAS_NOZZLE, false), 3);
            } else if (state.getValue(HAS_PLATE)) {
                give = new ItemStack(ModItems.GLASS_SHEET.get());
                level.setBlock(pos, state.setValue(HAS_PLATE, false), 3);
            } else {
                // No plate and no nozzle: nothing to remove.
                return InteractionResult.SUCCESS;
            }
            level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!player.getInventory().add(give)) {
                player.drop(give, false);
            }
            player.displayClientMessage(
                    Component.literal("取下了导气嘴"), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** Sneak + right-click (handled by the event bus): flip the up/down orientation. */
    public static void toggleOrientation(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.cycle(INVERTED), 3);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative()
                && level.getBlockEntity(pos) instanceof GasCollectingBottleBlockEntity be) {
            String gasId = be.getGasId();
            ItemStack drop = bottleItemFor(state, gasId);
            com.example.chemistry.PurityHelper.setPurity(drop, be.getPurity());
            net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(
                    level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, drop);
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
            if (state.getValue(HAS_NOZZLE)) {
                RubberTubeItem.dropTubesConnectedToNozzle(level, pos);
                net.minecraft.world.entity.item.ItemEntity nozzle = new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                        new ItemStack(ModItems.GAS_NOZZLE.get()));
                nozzle.setDefaultPickUpDelay();
                level.addFreshEntity(nozzle);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** The item form matching the placed bottle: sealed with plate, or open. */
    private static ItemStack bottleItemFor(BlockState state, String gasId) {
        if (state.getValue(HAS_PLATE)) {
            return gasId.isEmpty()
                    ? new ItemStack(ModItems.EMPTY_GAS_JAR.get())
                    : new ItemStack(ModItems.gasJarItem(gasId));
        }
        return gasId.isEmpty()
                ? new ItemStack(ModItems.EMPTY_GAS_JAR.get())
                : new ItemStack(ModItems.openGasJar(gasId));
    }
}
