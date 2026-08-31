package com.example.chemistry.block;

import com.example.chemistry.PurityHelper;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.blockentity.WaterTroughBlockEntity;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.item.RubberTubeItem;
import com.example.chemistry.entity.RubberTubeEntity;
import com.example.chemistry.entity.RubberTubeEntity.Port;
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
import net.minecraft.world.phys.shapes.Shapes;
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
    @org.jetbrains.annotations.Nullable
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return type == com.example.chemistry.registry.ModBlockEntities.WATER_TROUGH.get()
                ? (lvl, pos, st, be) -> ((WaterTroughBlockEntity) be).tickServer(lvl)
                : null;
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

    /** 碰撞箱 = 水槽槽体 + 冰浴中浸泡的烧瓶的实体碰撞。 */
    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, CollisionContext context) {
        VoxelShape base = getShape(state, level, pos, context);
        if (state.getValue(FILLED) == Fill.ICE
                && level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be
                && !be.getFlask().isEmpty()) {
            int vtype = VesselHeating.vesselType(be.getFlask());
            if (vtype != 0) {
                double off = (8.5 - 8.5 * 0.7) / 16.0;
                VoxelShape vessel = VesselHeating.vesselCollisionShape(pos, vtype,
                        0.7, off, 1.0 / 16.0, off, 0.0);
                if (!vessel.isEmpty()) {
                    base = Shapes.or(base, vessel);
                }
            }
        }
        return base;
    }

    /** 冰浴里烧瓶的瓶口悬空在槽沿上方；交互命中框放到整格高度，让瓶口可点。 */
    @Override
    public VoxelShape getInteractionShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos) {
        return box(3.0, 0.0, 3.0, 14.0, 16.0, 14.0);
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
        // 套着橡胶管的玻璃导管：水槽里已有导管则把自由端连上去；没有则
        // 以 45° 插进水槽并把管道连到玩家（物品被消耗，导管留在水槽里）。
        if (stack.getItem() instanceof com.example.chemistry.item.GlassTubeTubedItem
                && state.getValue(FILLED) == Fill.WATER) {
            if (!level.isClientSide()) {
                if (state.getValue(NOZZLE) == NozzleDir.NONE) {
                    NozzleDir dir = NozzleDir.fromDirection(player.getDirection());
                    level.setBlock(pos, state.setValue(NOZZLE, dir), 3);
                    if (level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be) {
                        be.setTubeType(com.example.chemistry.item.GlassTubeItem.tubeType(stack));
                    }
                    RubberTubeItem.createTube(level, player, stack,
                            Port.nozzle(pos, dir.toDirection()),
                            Port.entity(player.getUUID()));
                    stack.shrink(1);
                } else {
                    Port tube = Port.nozzle(pos, state.getValue(NOZZLE).toDirection());
                    if (RubberTubeItem.hasTubeAt(level, tube)) {
                        player.displayClientMessage(
                                Component.translatable("mchemistry.rubber_tube.occupied"), true);
                    } else {
                        RubberTubeItem.createTube(level, player, stack,
                                Port.entity(player.getUUID()), tube);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
        // 排水法集气: put an empty gas collecting bottle upside down into the
        // water — it fills with water and waits for gas.
        boolean emptyOrWaterJar = com.example.chemistry.transfer.BottleCodes.isGasBottle(stack)
                && (com.example.chemistry.transfer.BottleCodes.isEmpty(stack)
                        || com.example.chemistry.transfer.BottleCodes.isWater(stack));
        if (emptyOrWaterJar
                && state.getValue(FILLED) == Fill.WATER
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
        if (com.example.chemistry.transfer.BottleCodes.isGasBottle(stack)
                && com.example.chemistry.transfer.BottleCodes.isEmpty(stack)
                && level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be
                && be.hasBottle()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("mchemistry.water_trough.bottle_present"), true);
            }
            return InteractionResult.SUCCESS;
        }
        // Place a glass tube into the water at 45 degrees (4 directions)
        // (原导气嘴功能，三种玻璃导管都可以)。
        if (stack.getItem() instanceof com.example.chemistry.item.GlassTubeItem
                && state.getValue(FILLED) == Fill.WATER && state.getValue(NOZZLE) == NozzleDir.NONE) {
            if (!level.isClientSide()) {
                NozzleDir dir = NozzleDir.fromDirection(player.getDirection());
                level.setBlock(pos, state.setValue(NOZZLE, dir), 3);
                if (level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be) {
                    be.setTubeType(com.example.chemistry.item.GlassTubeItem.tubeType(stack));
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
        // 冰浴：装满冰块的水槽可以浸泡烧瓶。
        if (state.getValue(FILLED) == Fill.ICE
                && level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be
                && be.getFlask().isEmpty()
                && com.example.chemistry.block.HeatingMantleBlock.isFlask(stack)) {
            if (!level.isClientSide()) {
                be.setFlask(stack.copy());
                stack.shrink(1);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        // 向冰浴中浸泡的烧瓶装药（药匙/镊子/滴管/液体细口瓶等）。
        if (state.getValue(FILLED) == Fill.ICE
                && level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be
                && !be.getFlask().isEmpty()
                && com.example.chemistry.LabInteractions.isVesselRelevant(
                        stack, be.getFlask(), ItemStack.EMPTY, ItemStack.EMPTY)) {
            if (!level.isClientSide()) {
                if (com.example.chemistry.LabInteractions.interactPlacedVessel(
                        stack, be.getFlask(), ItemStack.EMPTY, ItemStack.EMPTY, player)) {
                    be.setFlask(be.getFlask());
                    level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY,
                            SoundSource.BLOCKS, 0.8F, 1.2F);
                }
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
            Port nozzle = Port.nozzle(pos, Direction.UP);
            if (RubberTubeItem.hasTubeAt(level, nozzle)) {
                player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.occupied"), true);
                return;
            }
            if (com.example.chemistry.item.GlassTubeItem.isGlassTube(player.getOffhandItem())) {
                RubberTubeItem.createTube(level, player, stack,
                        Port.entity(player.getUUID()), nozzle);
                return;
            }
            Port pending = RubberTubeItem.readPending(stack);
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
            Port nozzle = Port.nozzle(pos, state.getValue(NOZZLE).toDirection());
            if (RubberTubeItem.hasTubeAt(level, nozzle)) {
                player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.occupied"), true);
                return;
            }
            if (com.example.chemistry.item.GlassTubeItem.isGlassTube(player.getOffhandItem())) {
                RubberTubeItem.createTube(level, player, stack,
                        Port.entity(player.getUUID()), nozzle);
                return;
            }
            Port pending = RubberTubeItem.readPending(stack);
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
                        ? ModItems.gasBottle(gasId, false)
                        : ModItems.emptyGasJar();
                PurityHelper.setPurity(out, purity);
                RubberTubeItem.dropTubesAtBlockPos(level, pos.immutable(), true);
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
            Port nozzleAnchor = Port.nozzle(pos.immutable(),
                    state.getValue(NOZZLE).toDirection());
            if (RubberTubeItem.handleNozzleEmptyClick(level, player, nozzleAnchor)) {
                return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide()) {
                RubberTubeItem.dropTubesAtBlockPos(level, pos.immutable(), true);
                level.setBlock(pos, state.setValue(NOZZLE, NozzleDir.NONE), 3);
                int tubeType = level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be
                        ? be.getTubeType() : 1;
                ItemStack tubeDrop = new ItemStack(tubeItemFor(tubeType));
                if (!player.getInventory().add(tubeDrop)) {
                    player.drop(tubeDrop, false);
                }
                player.displayClientMessage(
                        Component.translatable("mchemistry.water_trough.tube_out"), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(FILLED) == Fill.ICE) {
            if (!level.isClientSide()) {
                if (level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be
                        && !be.getFlask().isEmpty()) {
                    ItemStack flask = be.getFlask();
                    // 先取下烧瓶瓶口的橡胶塞（对准瓶口）。
                    int vtype = VesselHeating.vesselType(flask);
                    double off = (8.5 - 8.5 * 0.7) / 16.0;
                    if (vtype != 0 && VesselHeating.isSealed(flask)
                            && VesselHeating.mouthForRay(pos, vtype, 0.7,
                                    off, 1.0 / 16.0, off, 0.0, player)) {
                        int holes = VesselHeating.getStopperHoles(flask);
                        VesselHeating.unseal(flask);
                        be.setFlask(flask);
                        ItemStack stopper = new ItemStack(ModItems.stopperForHoles(holes));
                        if (!player.getInventory().add(stopper)) {
                            player.drop(stopper, false);
                        }
                        return InteractionResult.SUCCESS;
                    }
                    // 再取出浸泡的烧瓶。
                    be.setFlask(ItemStack.EMPTY);
                    if (!player.getInventory().add(flask)) {
                        player.drop(flask, false);
                    }
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP,
                            SoundSource.BLOCKS, 0.8F, 1.0F);
                    return InteractionResult.SUCCESS;
                }
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
        if (!level.isClientSide()) {
            // 破坏水槽：无论什么模式都把连在导管上的橡胶管清掉。
            RubberTubeItem.dropTubesAtBlockPos(level, pos.immutable(), !player.isCreative());
            if (!player.isCreative()) {
                if (state.getValue(NOZZLE) != NozzleDir.NONE) {
                    int tubeType = level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be
                            ? be.getTubeType() : 1;
                    dropItem(level, pos, new ItemStack(tubeItemFor(tubeType)));
                }
                if (level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be && be.hasBottle()) {
                    ItemStack out = be.getFillMl() > 0 && !be.getGasId().isEmpty()
                            ? ModItems.gasBottle(be.getGasId(), false)
                            : ModItems.emptyGasJar();
                    PurityHelper.setPurity(out, be.getPurity());
                    dropItem(level, pos, out);
                }
                if (level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be
                        && !be.getFlask().isEmpty()) {
                    dropItem(level, pos, be.getFlask());
                }
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

    /** 导管类型 1=直管 2=90度管 3=90度长管 → 对应物品。 */
    private static net.minecraft.world.item.Item tubeItemFor(int type) {
        return switch (type) {
            case 2 -> ModItems.RIGHT_ANGLE_GLASS_TUBE.get();
            case 3 -> ModItems.RIGHT_ANGLE_GLASS_TUBE_LONG.get();
            default -> ModItems.STRAIGHT_GLASS_TUBE.get();
        };
    }
}
