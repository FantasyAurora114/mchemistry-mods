package com.example.chemistry.block;

import com.example.chemistry.blockentity.GasWashingBottleBlockEntity;
import com.example.chemistry.item.DropperHelper;
import com.example.chemistry.item.DropperItem;
import com.example.chemistry.registry.ModItems;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;

/** 洗气瓶: a placeable bottle with two forms — plain, and assembled with the
 *  rubber stopper + inlet/outlet glass tubes (插满). The assembled variant is
 *  a separate block item; breaking drops whichever form is placed. */
public class GasWashingBottleBlock extends Block implements EntityBlock {

    public static final MapCodec<GasWashingBottleBlock> CODEC = simpleCodec(GasWashingBottleBlock::new);
    public static final BooleanProperty STOPPER = BooleanProperty.create("stopper");
    public static final IntegerProperty TUBES = IntegerProperty.create("tubes", 0, 2);

    private static final VoxelShape SHAPE = box(5.0, 0.0, 5.0, 12.0, 10.0, 12.0);

    public GasWashingBottleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STOPPER, true).setValue(TUBES, 0));
    }

    @Override
    public MapCodec<GasWashingBottleBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STOPPER, TUBES);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    /** 洗气瓶瓶塞/导管口在碰撞体(至 y=10)之上悬空；交互命中框覆盖整个瓶身高度。 */
    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean assembled = context.getItemInHand().is(
                com.example.chemistry.registry.ModItems.GAS_WASHING_BOTTLE_ASSEMBLED.get());
        return defaultBlockState().setValue(STOPPER, true)
                .setValue(TUBES, assembled ? 2 : 0);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos,
            BlockState state, boolean includeData) {
        return new ItemStack(state.getValue(TUBES) >= 2
                ? com.example.chemistry.registry.ModItems.GAS_WASHING_BOTTLE_ASSEMBLED.get()
                : com.example.chemistry.registry.ModItems.GAS_WASHING_BOTTLE.get());
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative()) {
            int tubes = state.getValue(TUBES);
            ItemStack drop = new ItemStack(tubes >= 2
                    ? com.example.chemistry.registry.ModItems.GAS_WASHING_BOTTLE_ASSEMBLED.get()
                    : com.example.chemistry.registry.ModItems.GAS_WASHING_BOTTLE.get());
            ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.3,
                    pos.getZ() + 0.5, drop);
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
            if (tubes == 1) {
                ItemEntity tube = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.4,
                        pos.getZ() + 0.5,
                        new ItemStack(ModItems.RIGHT_ANGLE_GLASS_TUBE.get()));
                tube.setDefaultPickUpDelay();
                level.addFreshEntity(tube);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GasWashingBottleBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        int tubes = state.getValue(TUBES);
        if (!level.isClientSide()) {
            if (tubes >= 2) {
                // 后插的长导管先拔，再拔短导管。
                level.setBlock(pos, state.setValue(TUBES, 1), 3);
                giveOrDrop(player, new ItemStack(ModItems.RIGHT_ANGLE_GLASS_TUBE_LONG.get()));
                player.displayClientMessage(
                        Component.translatable("mchemistry.wash_bottle.tube_long_out"), true);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            } else if (tubes == 1) {
                level.setBlock(pos, state.setValue(TUBES, 0), 3);
                giveOrDrop(player, new ItemStack(ModItems.RIGHT_ANGLE_GLASS_TUBE.get()));
                player.displayClientMessage(
                        Component.translatable("mchemistry.wash_bottle.tube_short_out"), true);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            } else if (state.getValue(STOPPER)) {
                // Pull the stopper out; the player receives the 2-hole stopper.
                level.setBlock(pos, state.setValue(STOPPER, false), 3);
                giveOrDrop(player, new ItemStack(ModItems.RUBBER_STOPPER_2_HOLE.get()));
                player.displayClientMessage(
                        Component.translatable("mchemistry.wash_bottle.stopper_out"), true);
                level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
            } else if (consumeStopper(player)) {
                level.setBlock(pos, state.setValue(STOPPER, true), 3);
                player.displayClientMessage(
                        Component.translatable("mchemistry.wash_bottle.stopper_in"), true);
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
            } else {
                player.displayClientMessage(
                        Component.translatable("mchemistry.wash_bottle.need_stopper"), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof GasWashingBottleBlockEntity be)) {
            return InteractionResult.PASS;
        }
        int tubes = state.getValue(TUBES);
        boolean stopper = state.getValue(STOPPER);

        // Already stoppered: a second stopper is not consumed.
        if (stopper && stack.is(ModItems.RUBBER_STOPPER_2_HOLE.get())) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("mchemistry.wash_bottle.stopper_on"), true);
            }
            return InteractionResult.SUCCESS;
        }
        // Put the stopper back on an open bottle.
        if (!stopper && tubes == 0 && stack.is(ModItems.RUBBER_STOPPER_2_HOLE.get())) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(STOPPER, true), 3);
                stack.shrink(1);
                player.displayClientMessage(
                        Component.translatable("mchemistry.wash_bottle.stopper_in"), true);
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
            }
            return InteractionResult.SUCCESS;
        }
        // Insert the short 90-degree tube first.
        if (stack.is(ModItems.RIGHT_ANGLE_GLASS_TUBE.get())) {
            if (!stopper) {
                if (!level.isClientSide()) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.wash_bottle.need_stopper_first"), true);
                }
            } else if (tubes >= 1) {
                if (!level.isClientSide()) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.wash_bottle.tube_short_done"), true);
                }
            } else if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(TUBES, 1), 3);
                stack.shrink(1);
                player.displayClientMessage(
                        Component.translatable("mchemistry.wash_bottle.tube_short_in"), true);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        // The second hole only accepts the long tube (先短后长).
        if (stack.is(ModItems.RIGHT_ANGLE_GLASS_TUBE_LONG.get())) {
            if (!stopper) {
                if (!level.isClientSide()) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.wash_bottle.need_stopper_first"), true);
                }
            } else if (tubes == 0) {
                if (!level.isClientSide()) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.wash_bottle.need_short_first"), true);
                }
            } else if (tubes >= 2) {
                if (!level.isClientSide()) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.wash_bottle.tube_full"), true);
                }
            } else if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(TUBES, 2), 3);
                stack.shrink(1);
                player.displayClientMessage(
                        Component.translatable("mchemistry.wash_bottle.tube_long_in"), true);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        // Pour liquid only while the bottle is open.
        if (!stopper && tubes == 0) {
            if (stack.getItem() instanceof DropperItem && !DropperHelper.isEmpty(stack)) {
                if (!level.isClientSide()) {
                    String liquid = DropperHelper.getLiquid(stack);
                    if (be.addLiquid(liquid, Math.min(5,DropperHelper.getMl(stack)))) {
                        DropperHelper.setMl(stack, DropperHelper.getMl(stack) - 5);
                        player.displayClientMessage(
                                Component.translatable("mchemistry.wash_bottle.poured"), true);
                    } else {
                        player.displayClientMessage(
                                Component.translatable("mchemistry.wash_bottle.liquid_full"), true);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            String liquidId = com.example.chemistry.transfer.BottleCodes.liquidIdOf(stack);
            if (liquidId != null && !com.example.chemistry.transfer.BottleCodes.isSealed(stack)) {
                if (!level.isClientSide()) {
                    int amount=Math.min(25,com.example.chemistry.transfer.BottleCodes.volumeOf(stack));
                    if (amount>0 && be.addLiquid(liquidId, amount)) {
                        com.example.chemistry.transfer.BottleCodes.setVolume(stack,com.example.chemistry.transfer.BottleCodes.volumeOf(stack)-amount);
                        player.displayClientMessage(
                                Component.translatable("mchemistry.wash_bottle.poured"), true);
                    } else {
                        player.displayClientMessage(
                                Component.translatable("mchemistry.wash_bottle.liquid_full"), true);
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        // Stoppered: liquids are rejected (matches other sealed vessels).
        if (stopper && (stack.getItem() instanceof DropperItem
                || com.example.chemistry.transfer.BottleCodes.liquidIdOf(stack) != null)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("mchemistry.vessel.sealed"), true);
            }
            return InteractionResult.SUCCESS;
        }
        // 空手右键：交给 useWithoutItem（先拔导管，再拔/塞瓶塞）。
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return InteractionResult.PASS;
    }

    private static boolean consumeStopper(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack s = player.getInventory().getItem(slot);
            if (s.is(ModItems.RUBBER_STOPPER_2_HOLE.get())) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

}
