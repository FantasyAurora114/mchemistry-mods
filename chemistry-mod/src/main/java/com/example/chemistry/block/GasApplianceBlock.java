package com.example.chemistry.block;

import com.example.chemistry.blockentity.GasApplianceBlockEntity;
import com.example.chemistry.item.GasCylinderItem;
import com.example.chemistry.item.RubberTubeItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class GasApplianceBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private final int kind;
    public GasApplianceBlock(Properties properties, int kind) {
        super(properties); this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }
    public int kind() { return kind; }
    @Override public MapCodec<? extends Block> codec() { return simpleCodec(p -> new GasApplianceBlock(p, kind)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, HALF); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind == 2 ? box(.8, 0, .8, 15.2, 16, 15.2)
                : kind == 1 ? box(6, 0, 6, 10, 7.7, 10) : box(5, 0, 5, 11, 13, 11);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) {
        if (kind == 2 && (c.getClickedPos().getY() >= c.getLevel().getMaxY()
                || !c.getLevel().getBlockState(c.getClickedPos().above()).canBeReplaced(c))) return null;
        return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        if (kind == 2) level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
        if (stack.getItem() instanceof GasCylinderItem item && device(level, pos) != null) {
            device(level, pos).loadCylinder(item.gas(stack), item.remaining(stack));
            device(level,pos).setRadioSample(stack);
        }
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighbour, BlockState other, RandomSource random) {
        if (kind == 2 && direction == (state.getValue(HALF) == DoubleBlockHalf.UPPER ? Direction.DOWN : Direction.UP)
                && (!other.is(this) || other.getValue(HALF) == state.getValue(HALF))) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, direction, neighbour, other, random);
    }
    public static GasApplianceBlockEntity device(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GasApplianceBlock)) return null;
        BlockPos base = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        return level.getBlockEntity(base) instanceof GasApplianceBlockEntity be ? be : null;
    }
    public static void connect(Level level, Player player, ItemStack stack, BlockPos pos) {
        var be = device(level, pos); if (be == null) return;
        if (!RubberTubeItem.isWet(stack)) { player.displayClientMessage(Component.literal("请先将橡胶管沾湿，或使用输气管"), true); return; }
        var pending = RubberTubeItem.readPending(stack);
        if (player.isShiftKeyDown() && pending != null) {
            RubberTubeItem.discardTempTube(level, stack); RubberTubeItem.clearPending(stack); return;
        }
        if (pending == null) RubberTubeItem.startPending(level, player, stack, be.port());
        else if (!RubberTubeItem.sameAnchor(pending, be.port())) RubberTubeItem.createTube(level, player, stack, pending, be.port());
    }
    private InteractionResult interact(ItemStack held, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        var be = device(level, pos); if (be == null) return InteractionResult.PASS;
        if (held.getItem() instanceof RubberTubeItem) { connect(level, player, held, pos); return InteractionResult.SUCCESS; }
        if (kind == 0 && held.is(Items.FLINT_AND_STEEL)) {
            boolean lit = be.ignite();
            player.displayClientMessage(Component.literal(lit ? "本生灯已点燃：CH₄ + 2O₂ → CO₂ + 2H₂O" : "先打开燃气旋钮，并接入甲烷供气"), true);
            if (lit) held.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty()) {
            if (kind == 0 && (player.isShiftKeyDown() || hit.getLocation().y - be.getBlockPos().getY() > 3.0 / 16)) be.toggleAir();
            else be.toggleValve();
            player.displayClientMessage(Component.literal(kind == 0
                    ? "燃气 " + (be.valve() ? "开" : "关") + "；进气 " + (be.blue() ? "蓝焰" : "黄焰") + "；甲烷 " + be.remaining() + " mL"
                    : "钢瓶阀门 " + (be.valve() ? "开" : "关") + "；余量 " + be.remaining() + "/" + be.capacity() + " mL"), true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    @Override protected InteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) { return interact(held, state, level, pos, player, hand, hit); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) { return interact(ItemStack.EMPTY, state, level, pos, player, InteractionHand.MAIN_HAND, hit); }
    @Override protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        BlockPos base = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        if (level.getBlockEntity(base) instanceof GasApplianceBlockEntity be) return be.dropStack();
        return new ItemStack(kind == 0 ? com.example.chemistry.registry.ModItems.BUNSEN_BURNER.get()
                : kind == 2 ? com.example.chemistry.registry.ModItems.GAS_CYLINDER_TALL.get()
                : com.example.chemistry.registry.ModItems.GAS_CYLINDER_SMALL.get());
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative()) { var be = device(level, pos); if (be != null) be.suppressDrop = true; }
        return super.playerWillDestroy(level, pos, state, player);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? null : new GasApplianceBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (l, p, s, be) -> { if (be instanceof GasApplianceBlockEntity device) device.tickServer(); };
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(FACING))); }
}
