package com.example.chemistry.block;

import com.example.chemistry.blockentity.TripodBlockEntity;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.LabInteractions;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModItems;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 三脚架: a perfect equilateral triangle on top and three legs at 75 degrees
 * to the ground, scaled to 3/4 size. A clay triangle (泥三角) can be placed on
 * the top and an alcohol lamp underneath. Drawn entirely by
 * {@code TripodRenderer}, so the block itself is invisible.
 */
public class TripodBlock extends Block implements EntityBlock {

    public static final MapCodec<TripodBlock> CODEC = simpleCodec(TripodBlock::new);
    public static final BooleanProperty HAS_CLAY_TRIANGLE = BooleanProperty.create("has_clay_triangle");
    public static final BooleanProperty HAS_LAMP = BooleanProperty.create("has_lamp");
    public static final BooleanProperty LAMP_LIT = BooleanProperty.create("lamp_lit");
    public static final BooleanProperty HAS_VESSEL = BooleanProperty.create("has_vessel");

    // Scaled (0.75) footprint: feet and the top triangle.
    private static final VoxelShape SHAPE = Shapes.or(
            box(3.7, 0, 5.5, 12.3, 1.1, 12.8),   // the three feet
            box(5.6, 7.7, 6.6, 10.4, 8.5, 10.7)); // the top triangle

    public TripodBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HAS_CLAY_TRIANGLE, false)
                .setValue(HAS_LAMP, false)
                .setValue(LAMP_LIT, false)
                .setValue(HAS_VESSEL, false));
    }

    @Override
    public MapCodec<TripodBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_CLAY_TRIANGLE, HAS_LAMP, LAMP_LIT, HAS_VESSEL);
    }

    /** Drawn by the block-entity renderer. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TripodBlockEntity(pos, state);
    }

    @Override
    @org.jetbrains.annotations.Nullable
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return type == com.example.chemistry.registry.ModBlockEntities.TRIPOD.get()
                ? (lvl, pos, st, be) -> ((TripodBlockEntity) be).tickServer(lvl)
                : null;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        // Place the clay triangle on top of the tripod.
        if (stack.is(ModItems.CLAY_TRIANGLE.get()) && !state.getValue(HAS_CLAY_TRIANGLE)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(HAS_CLAY_TRIANGLE, true), 3);
                level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        // Place a crucible / evaporating dish on the clay triangle.
        if (state.getValue(HAS_CLAY_TRIANGLE) && !state.getValue(HAS_VESSEL)
                && stack.getItem() instanceof LabVesselItem
                && (stack.is(ModItems.CRUCIBLE.get()) || stack.is(ModItems.EVAPORATING_DISH.get()))) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TripodBlockEntity be) {
                be.setVessel(stack.copy());
                stack.shrink(1);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        // Add reagents / insert stopper instruments on the placed vessel.
        if (state.getValue(HAS_VESSEL) && level.getBlockEntity(pos) instanceof TripodBlockEntity be
                && !be.getVessel().isEmpty()) {
            if (!level.isClientSide()) {
                if (LabInteractions.interactPlacedVessel(stack, be.getVessel(),
                        ItemStack.EMPTY, ItemStack.EMPTY, player)) {
                    be.setVessel(be.getVessel());
                    level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8F, 1.2F);
                }
            }
            if (LabInteractions.isVesselRelevant(stack, be.getVessel(),
                    ItemStack.EMPTY, ItemStack.EMPTY)) {
                return InteractionResult.SUCCESS;
            }
        }
        // Crucible tongs pick up a crucible from the clay triangle.
        if (state.getValue(HAS_VESSEL) && stack.is(ModItems.CRUCIBLE_TONGS.get())
                && level.getBlockEntity(pos) instanceof TripodBlockEntity be
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
        // Seal the vessel with a rubber stopper (pressure system).
        if (state.getValue(HAS_VESSEL) && stack.is(ModItems.RUBBER_STOPPER_1_HOLE.get())
                && level.getBlockEntity(pos) instanceof TripodBlockEntity be
                && !be.getVessel().isEmpty() && !VesselHeating.isSealed(be.getVessel())) {
            if (!level.isClientSide()) {
                ItemStack v = be.getVessel();
                VesselHeating.seal(v, 1);
                be.setVessel(v);
                stack.shrink(1);
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        // Place an unlit alcohol lamp underneath the tripod.
        if (stack.is(ModItems.ALCOHOL_LAMP.get()) && !state.getValue(HAS_LAMP)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(HAS_LAMP, true).setValue(LAMP_LIT, false), 3);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            stack.shrink(1);
            return InteractionResult.SUCCESS;
        }
        // Flint & steel / fire charge light the lamp under the tripod.
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
        // Never trigger the empty-hand pickup while holding an item: the server
        // calls useWithoutItem for ANY TryEmptyHand result, even with a
        // non-empty main hand.
        return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            if (state.getValue(HAS_VESSEL) && level.getBlockEntity(pos) instanceof TripodBlockEntity be) {
                ItemStack vessel = be.getVessel();
                be.setVessel(ItemStack.EMPTY);
                if (!player.getInventory().add(vessel)) {
                    player.drop(vessel, false);
                }
            } else if (state.getValue(HAS_CLAY_TRIANGLE)) {
                level.setBlock(pos, state.setValue(HAS_CLAY_TRIANGLE, false), 3);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.7F, 1.0F);
                ItemStack clay = new ItemStack(ModItems.CLAY_TRIANGLE.get());
                if (!player.getInventory().add(clay)) {
                    player.drop(clay, false);
                }
            } else if (state.getValue(HAS_LAMP)) {
                level.setBlock(pos, state.setValue(HAS_LAMP, false).setValue(LAMP_LIT, false), 3);
                level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                ItemStack lamp = new ItemStack(ModItems.ALCOHOL_LAMP.get());
                if (!player.getInventory().add(lamp)) {
                    player.drop(lamp, false);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Breaking always returns the tripod item. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative()) {
            net.minecraft.world.entity.item.ItemEntity dropItem = new net.minecraft.world.entity.item.ItemEntity(
                    level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                    new ItemStack(ModItems.TRIPOD.get()));
            dropItem.setDefaultPickUpDelay();
            level.addFreshEntity(dropItem);
            if (state.getValue(HAS_CLAY_TRIANGLE)) {
                net.minecraft.world.entity.item.ItemEntity clay = new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                        new ItemStack(ModItems.CLAY_TRIANGLE.get()));
                clay.setDefaultPickUpDelay();
                level.addFreshEntity(clay);
            }
            if (state.getValue(HAS_VESSEL) && level.getBlockEntity(pos) instanceof TripodBlockEntity be
                    && !be.getVessel().isEmpty()) {
                net.minecraft.world.entity.item.ItemEntity vessel = new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, be.getVessel());
                vessel.setDefaultPickUpDelay();
                level.addFreshEntity(vessel);
            }
            if (state.getValue(HAS_LAMP)) {
                net.minecraft.world.entity.item.ItemEntity lamp = new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                        new ItemStack(ModItems.ALCOHOL_LAMP.get()));
                lamp.setDefaultPickUpDelay();
                level.addFreshEntity(lamp);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
