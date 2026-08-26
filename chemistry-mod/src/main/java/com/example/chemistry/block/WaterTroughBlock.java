package com.example.chemistry.block;

import com.example.chemistry.PurityHelper;
import com.example.chemistry.blockentity.WaterTroughBlockEntity;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.item.RubberTubeItem;
import com.example.chemistry.entity.RubberTubeEntity;
import com.example.chemistry.entity.RubberTubeEntity.Anchor;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 水槽: empty / water / ice states; buckets fill and drain water, ice blocks
 *  fill ice. An inverted gas collecting bottle can sit in the water for
 *  排水法集气 (water-displacement gas collection). */
public class WaterTroughBlock extends Block implements EntityBlock {

    public enum Fill implements net.minecraft.util.StringRepresentable {
        EMPTY("empty"),
        WATER("water"),
        ICE("ice");

        private final String name;

        Fill(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<WaterTroughBlock> CODEC = simpleCodec(WaterTroughBlock::new);
    public static final EnumProperty<Fill> FILLED = EnumProperty.create("filled", Fill.class);
    public enum NozzleDir implements net.minecraft.util.StringRepresentable {
        NONE("none"), NORTH("north"), SOUTH("south"), EAST("east"), WEST("west");
        private final String name;
        NozzleDir(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
        public static NozzleDir fromDirection(Direction d) {
            return switch (d) {
                case NORTH -> NORTH;
                case SOUTH -> SOUTH;
                case EAST -> EAST;
                default -> WEST;
            };
        }
        public Direction toDirection() {
            return switch (this) {
                case NORTH -> Direction.NORTH;
                case SOUTH -> Direction.SOUTH;
                case EAST -> Direction.EAST;
                case WEST -> Direction.WEST;
                default -> Direction.NORTH;
            };
        }
    }
    public static final EnumProperty<NozzleDir> NOZZLE = EnumProperty.create("nozzle", NozzleDir.class);

    private static final VoxelShape SHAPE = box(3.0, 0.0, 3.0, 14.0, 4.0, 14.0);

    public WaterTroughBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FILLED, Fill.EMPTY)
                .setValue(NOZZLE, NozzleDir.NONE));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaterTroughBlockEntity(pos, state);
    }

    @Override
    public MapCodec<WaterTroughBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILLED, NOZZLE);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        // Rubber tube: dip into the trough water to wet it, or (when already
        // wet) anchor/connect at the trough nozzle. Always consumes the click,
        // so holding a tube can never take the nozzle out. An empty main hand
        // with a tube in the offhand still routes to the tube (tube wins).
        if (stack.getItem() instanceof RubberTubeItem
                || (stack.isEmpty() && player.getOffhandItem().getItem() instanceof RubberTubeItem)) {
            ItemStack tube = stack.isEmpty() ? player.getOffhandItem() : stack;
            if (!level.isClientSide()) {
                handleTube(level, player, tube, pos, state);
            }
            return InteractionResult.SUCCESS;
        }
        // 排水法集气: put an empty gas collecting bottle upside down into the
        // water — it fills with water and waits for gas.
        if (stack.is(ModItems.EMPTY_GAS_JAR.get()) && state.getValue(FILLED) == Fill.WATER
                && level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be
                && !be.hasBottle()) {
            if (!level.isClientSide()) {
                be.placeBottle();
                stack.shrink(1);
                level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,
                        SoundSource.BLOCKS, 0.8F, 1.0F);
                player.displayClientMessage(
                        Component.translatable("mchemistry.water_trough.bottle_in"), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(ModItems.EMPTY_GAS_JAR.get())
                && level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be
                && be.hasBottle()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("mchemistry.water_trough.bottle_present"), true);
            }
            return InteractionResult.SUCCESS;
        }
        // Place a gas nozzle into the water at 45 degrees (4 directions).
        if ((stack.is(ModItems.GAS_NOZZLE.get()) || stack.is(ModItems.GAS_NOZZLE_TUBED.get()))
                && state.getValue(FILLED) == Fill.WATER && state.getValue(NOZZLE) == NozzleDir.NONE) {
            if (!level.isClientSide()) {
                NozzleDir dir = NozzleDir.fromDirection(player.getDirection());
                level.setBlock(pos, state.setValue(NOZZLE, dir), 3);
                if (stack.is(ModItems.GAS_NOZZLE_TUBED.get())) {
                    // The tube on the nozzle connects from the trough to the player.
                    RubberTubeItem.createTube(level, player, stack,
                            Anchor.nozzle(pos, dir.toDirection()), Anchor.entity(player.getUUID()));
                }
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        boolean waterBucket = stack.is(Items.WATER_BUCKET) || stack.is(ModItems.CHEMICAL_WATER_BUCKET.get());
        if (state.getValue(FILLED) == Fill.EMPTY && waterBucket) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(FILLED, Fill.WATER), 3);
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(FILLED) == Fill.WATER && stack.is(Items.BUCKET)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(FILLED, Fill.EMPTY), 3);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                player.setItemInHand(hand, new ItemStack(Items.WATER_BUCKET));
            }
            return InteractionResult.SUCCESS;
        }
        // Ice block fills the trough (on empty or water).
        if (stack.is(Items.ICE) && state.getValue(FILLED) != Fill.ICE) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(FILLED, Fill.ICE), 3);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
    }

    /**
     * Rubber-tube interaction shared by the block handler and (for the sneak
     * path) the rubber tube item itself.
     */
    public static void handleTube(Level level, Player player, ItemStack stack, BlockPos pos, BlockState state) {
        boolean hasWater = state.getValue(FILLED) == Fill.WATER;
        boolean hasNozzle = state.getValue(NOZZLE) != NozzleDir.NONE;
        boolean hasBottle = level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be
                && be.hasBottle();
        if (!RubberTubeItem.isWet(stack)) {
            if (hasWater) {
                RubberTubeItem.setWet(stack);
                player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.wet"), true);
            } else {
                player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.need_wet"), true);
            }
            return;
        }
        if (hasBottle) {
            // 排水法: the rubber tube feeds gas into the inverted bottle's
            // mouth, which sits just under the water surface.
            Anchor nozzle = Anchor.nozzle(pos, Direction.UP);
            if (RubberTubeItem.hasTubeAt(level, nozzle)) {
                player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.occupied"), true);
                return;
            }
            Anchor pending = RubberTubeItem.readPending(stack);
            if (pending == null) {
                RubberTubeItem.startPending(level, player, stack, nozzle);
                player.displayClientMessage(
                        Component.translatable("mchemistry.rubber_tube.start_nozzle"), true);
            } else if (RubberTubeItem.sameAnchor(pending, nozzle)) {
                player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.same"), true);
            } else {
                RubberTubeItem.createTube(level, player, stack, pending, nozzle);
            }
        } else if (hasNozzle) {
            Anchor nozzle = Anchor.nozzle(pos, state.getValue(NOZZLE).toDirection());
            if (RubberTubeItem.hasTubeAt(level, nozzle)) {
                player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.occupied"), true);
                return;
            }
            Anchor pending = RubberTubeItem.readPending(stack);
            if (pending == null) {
                RubberTubeItem.startPending(level, player, stack, nozzle);
                player.displayClientMessage(
                        Component.translatable("mchemistry.rubber_tube.start_nozzle"), true);
            } else if (RubberTubeItem.sameAnchor(pending, nozzle)) {
                player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.same"), true);
            } else {
                RubberTubeItem.createTube(level, player, stack, pending, nozzle);
            }
        }
    }

    /** Empty hand: take the inverted bottle out, then the nozzle, then ice. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be && be.hasBottle()) {
            if (!level.isClientSide()) {
                String gasId = be.getGasId();
                int fillMl = be.getFillMl();
                double purity = be.getPurity();
                ItemStack out = fillMl > 0 && !gasId.isEmpty()
                        ? new ItemStack(ModItems.openGasJar(gasId))
                        : new ItemStack(ModItems.EMPTY_GAS_JAR.get());
                PurityHelper.setPurity(out, purity);
                RubberTubeItem.detachTubesAt(level, Anchor.nozzle(pos.immutable(), Direction.UP));
                be.takeBottle();
                if (!player.getInventory().add(out)) {
                    player.drop(out, false);
                }
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                player.displayClientMessage(
                        Component.translatable("mchemistry.water_trough.bottle_out"), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(NOZZLE) != NozzleDir.NONE) {
            // Never remove the nozzle while the player is handling a rubber
            // tube (e.g. holding it in the offhand): connect instead.
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();
            ItemStack tube = main.getItem() instanceof RubberTubeItem ? main
                    : off.getItem() instanceof RubberTubeItem ? off : ItemStack.EMPTY;
            if (!tube.isEmpty()) {
                if (!level.isClientSide()) {
                    handleTube(level, player, tube, pos, state);
                }
                return InteractionResult.SUCCESS;
            }
            // Empty hand with a tube pending on the player: complete the
            // connection; an occupied nozzle is never removed by a click.
            Anchor nozzleAnchor = Anchor.nozzle(pos.immutable(),
                    state.getValue(NOZZLE).toDirection());
            if (RubberTubeItem.handleNozzleEmptyClick(level, player, nozzleAnchor)) {
                return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide()) {
                RubberTubeItem.detachTubesAt(level, nozzleAnchor);
                level.setBlock(pos, state.setValue(NOZZLE, NozzleDir.NONE), 3);
                ItemStack nozzle = new ItemStack(ModItems.GAS_NOZZLE.get());
                if (!player.getInventory().add(nozzle)) {
                    player.drop(nozzle, false);
                }
                player.displayClientMessage(
                        Component.literal("取下了导气嘴"), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(FILLED) == Fill.ICE) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(FILLED, Fill.EMPTY), 3);
                ItemStack ice = new ItemStack(Items.ICE);
                if (!player.getInventory().add(ice)) {
                    player.drop(ice, false);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** Breaking drops the nozzle and any inverted bottle still inside. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative()) {
            if (state.getValue(NOZZLE) != NozzleDir.NONE) {
                RubberTubeItem.detachTubesAt(level,
                        Anchor.nozzle(pos.immutable(), state.getValue(NOZZLE).toDirection()));
                dropItem(level, pos, new ItemStack(ModItems.GAS_NOZZLE.get()));
            }
            if (level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be && be.hasBottle()) {
                ItemStack out = be.getFillMl() > 0 && !be.getGasId().isEmpty()
                        ? new ItemStack(ModItems.openGasJar(be.getGasId()))
                        : new ItemStack(ModItems.EMPTY_GAS_JAR.get());
                PurityHelper.setPurity(out, be.getPurity());
                dropItem(level, pos, out);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private static void dropItem(Level level, BlockPos pos, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.3,
                pos.getZ() + 0.5, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }
}
