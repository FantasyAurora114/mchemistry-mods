package com.example.chemistry.block;

import com.example.chemistry.item.RubberTubeItem;
import com.example.chemistry.item.GlassTubeItem;
import com.example.chemistry.item.GlassTubeTubedItem;
import com.example.chemistry.item.LabelItem;
import com.example.chemistry.entity.RubberTubeEntity;
import com.example.chemistry.blockentity.GasCollectingBottleBlockEntity;
import com.example.chemistry.entity.RubberTubeEntity.Port;
import com.example.chemistry.registry.ModBlockEntities;
import com.example.chemistry.registry.ModItems;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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

    /** 集气瓶瓶口在碰撞体(至 y=7.2)之上悬空；交互命中框覆盖整个瓶身高度。 */
    @Override
    public VoxelShape getInteractionShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos) {
        return box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GasCollectingBottleBlockEntity(pos, state);
    }

    /** 服务器 tick：瓶内气体沿连接的橡胶管流向另一端。 */
    @Override
    @org.jetbrains.annotations.Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.GAS_COLLECTING_BOTTLE.get()
                ? (lvl, pos, st, be) -> ((GasCollectingBottleBlockEntity) be).tickServer(lvl)
                : null;
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
        // 用湿橡胶管连瓶口但瓶上还没插导管：提示先插导管。
        if ((stack.getItem() instanceof RubberTubeItem
                || (stack.isEmpty() && player.getOffhandItem().getItem() instanceof RubberTubeItem))
                && !state.getValue(HAS_NOZZLE)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("mchemistry.rubber_tube.need_bottle_nozzle"), true);
            }
            return InteractionResult.SUCCESS;
        }
        // 套着橡胶管的玻璃导管：把自由端连到瓶口导管。
        if (stack.getItem() instanceof GlassTubeTubedItem) {
            if (!state.getValue(HAS_NOZZLE)) {
                if (!level.isClientSide()) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.rubber_tube.need_bottle_nozzle"), true);
                }
                return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide()) {
                Port nozzle = Port.nozzle(pos, Direction.UP);
                if (RubberTubeItem.hasTubeAt(level, nozzle)) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.rubber_tube.occupied"), true);
                } else {
                    RubberTubeItem.createTube(level, player, stack,
                            Port.entity(player.getUUID()), nozzle);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // Insert a glass tube through the glass-plate opening (原导气嘴功能，
        // 三种玻璃导管都可以插)。
        if (stack.getItem() instanceof GlassTubeItem
                && state.getValue(HAS_PLATE) && !state.getValue(HAS_NOZZLE)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(HAS_NOZZLE, true), 3);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (level.getBlockEntity(pos) instanceof GasCollectingBottleBlockEntity be) {
                    be.setTubeType(GlassTubeItem.tubeType(stack));
                }
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
        Port nozzle = Port.nozzle(pos, Direction.UP);
        if (RubberTubeItem.hasTubeAt(level, nozzle)) {
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.occupied"), true);
            return;
        }
        // 副手拿玻璃导管：从手上的导管直接连到瓶口导管（一次右键完成）。
        if (com.example.chemistry.item.GlassTubeItem.isGlassTube(player.getOffhandItem())) {
            RubberTubeItem.createTube(level, player, stack,
                    Port.entity(player.getUUID()), nozzle);
            return;
        }
        Port pending = RubberTubeItem.readPending(stack);
        if (pending == null) {
            RubberTubeItem.startPending(level, player, stack, nozzle);
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.start_nozzle"), true);
        } else if (RubberTubeItem.sameAnchor(pending, nozzle)) {
            player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.same"), true);
        } else {
            RubberTubeItem.createTube(level, player, stack, pending, nozzle);
        }
    }

    /** Empty hand, right-click priority: remove the gas nozzle first (dropping
     *  the whole rubber tube connected to it), then the glass plate, then pick
     *  the bottle up. A rubber tube held in the offhand still connects. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        // Offhand rubber tube: connect to the nozzle (not remove it).
        ItemStack off = player.getOffhandItem();
        if (state.getValue(HAS_NOZZLE) && off.getItem() instanceof RubberTubeItem) {
            if (!level.isClientSide()) {
                handleTubeNozzle(level, player, off, pos, state);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            ItemStack give;
            if (state.getValue(HAS_NOZZLE)) {
                // The whole rubber tube connected to this tube head drops.
                RubberTubeItem.dropTubesConnectedToNozzle(level, pos);
                int tubeType = level.getBlockEntity(pos) instanceof GasCollectingBottleBlockEntity be
                        ? be.getTubeType() : 1;
                give = new ItemStack(tubeItemFor(tubeType));
                level.setBlock(pos, state.setValue(HAS_NOZZLE, false), 3);
                player.displayClientMessage(
                        Component.translatable("mchemistry.gas_bottle.tube_out"), true);
            } else if (state.getValue(HAS_PLATE)) {
                give = new ItemStack(ModItems.GLASS_SHEET.get());
                level.setBlock(pos, state.setValue(HAS_PLATE, false), 3);
                player.displayClientMessage(
                        Component.translatable("mchemistry.gas_bottle.plate_out"), true);
            } else {
                // Nothing attached: pick the whole bottle up (keeps gas/purity).
                if (!(level.getBlockEntity(pos) instanceof GasCollectingBottleBlockEntity be)) {
                    return InteractionResult.SUCCESS;
                }
                ItemStack bottle = bottleItemFor(state, be.getGasId());
                be.writeToItem(bottle);
                com.example.chemistry.PurityHelper.setPurity(bottle, be.getPurity());
                applyLabel(bottle, be.getLabelName());
                level.removeBlock(pos, false);
                if (!player.getInventory().add(bottle)) {
                    player.drop(bottle, false);
                }
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!player.getInventory().add(give)) {
                player.drop(give, false);
            }
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
        if (!level.isClientSide()) {
            // 破坏集气瓶：无论什么模式都把连在瓶口导管上的橡胶管清掉
            // （否则创造模式下管子会悬空），生存模式才掉落橡胶管物品。
            RubberTubeItem.dropTubesAtBlockPos(level, pos, !player.isCreative());
            if (!player.isCreative()
                    && level.getBlockEntity(pos) instanceof GasCollectingBottleBlockEntity be) {
                String gasId = be.getGasId();
                ItemStack drop = bottleItemFor(state, gasId);
                be.writeToItem(drop);
                com.example.chemistry.PurityHelper.setPurity(drop, be.getPurity());
                applyLabel(drop, be.getLabelName());
                net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, drop);
                item.setDefaultPickUpDelay();
                level.addFreshEntity(item);
                if (state.getValue(HAS_NOZZLE)) {
                    int tubeType = level.getBlockEntity(pos) instanceof GasCollectingBottleBlockEntity bottleBe
                            ? bottleBe.getTubeType() : 1;
                    net.minecraft.world.entity.item.ItemEntity tubeDrop = new net.minecraft.world.entity.item.ItemEntity(
                            level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                            new ItemStack(tubeItemFor(tubeType)));
                    tubeDrop.setDefaultPickUpDelay();
                    level.addFreshEntity(tubeDrop);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** The item form matching the placed bottle: sealed with plate, or open. */
    private static ItemStack bottleItemFor(BlockState state, String gasId) {
        if (state.getValue(HAS_PLATE)) {
            return gasId.isEmpty()
                    ? ModItems.emptyGasJar()
                    : ModItems.gasBottle(gasId, true);
        }
        return gasId.isEmpty()
                ? ModItems.emptyGasJar()
                : ModItems.gasBottle(gasId, false);
    }

    /** 导管类型 1=直管 2=90度管 3=90度长管 → 对应物品。 */
    private static net.minecraft.world.item.Item tubeItemFor(int type) {
        return switch (type) {
            case 2 -> ModItems.RIGHT_ANGLE_GLASS_TUBE.get();
            case 3 -> ModItems.RIGHT_ANGLE_GLASS_TUBE_LONG.get();
            case 4 -> ModItems.STRAIGHT_GLASS_TUBE_LONG.get();
            default -> ModItems.STRAIGHT_GLASS_TUBE.get();
        };
    }

    /** 把方块实体上存的标签文字写回拾取到的瓶子物品。 */
    private static void applyLabel(ItemStack stack, String label) {
        if (label == null || label.isEmpty()) {
            return;
        }
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(label));
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(LabelItem.KEY_LABEL, label);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
