package com.example.chemistry.block;

import com.example.chemistry.blockentity.PlacedGraduatedCylinderBlockEntity;
import com.example.chemistry.item.DropperHelper;
import com.example.chemistry.item.GraduatedCylinderItem;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModItems;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 放下的量筒：细口瓶右键倒入 5mL；潜行+右键回收 1mL；空手拿起。 */
public class PlacedGraduatedCylinderBlock extends Block implements EntityBlock {

    public static final MapCodec<PlacedGraduatedCylinderBlock> CODEC = simpleCodec(PlacedGraduatedCylinderBlock::new);
    private static final VoxelShape SHAPE = box(6.0, 0.0, 6.0, 10.0, 15.0, 10.0);

    public PlacedGraduatedCylinderBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<PlacedGraduatedCylinderBlock> codec() {
        return CODEC;
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlacedGraduatedCylinderBlockEntity(pos, state);
    }

    /** 破坏量筒时把里面的量筒物品掉落出来（右键拿起时已先清空，不会重复掉落）。 */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof PlacedGraduatedCylinderBlockEntity be
                && !be.getCylinder().isEmpty()) {
            popResource(level, pos, be.getCylinder());
            be.setCylinder(ItemStack.EMPTY);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof PlacedGraduatedCylinderBlockEntity be)) {
            return InteractionResult.PASS;
        }
        // 胶头滴管 / 滴瓶瓶塞：向管内滴加 1mL。
        if (com.example.chemistry.item.DropperHelper.isDropper(stack)
                && !com.example.chemistry.item.DropperHelper.isEmpty(stack)) {
            if (!level.isClientSide()) {
                double added = GraduatedCylinderItem.add(be.getCylinder(),
                        com.example.chemistry.item.DropperHelper.getLiquid(stack), 1);
                if (added > 0) {
                    com.example.chemistry.item.DropperHelper.setMl(stack,
                            com.example.chemistry.item.DropperHelper.getMl(stack) - 1);
                    be.setCylinder(be.getCylinder());
                    player.displayClientMessage(
                            Component.translatable("mchemistry.cylinder.drip"), true);
                    level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY,
                            SoundSource.BLOCKS, 0.8F, 1.2F);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // 潜行+右键：丢弃 1mL（液体直接消失，不给任何物品）。
        if (player.isShiftKeyDown() && stack.isEmpty()) {
            if (!level.isClientSide()) {
                String liquid = GraduatedCylinderItem.getLiquid(be.getCylinder());
                if (liquid != null && GraduatedCylinderItem.getMl(be.getCylinder()) >= 1) {
                    GraduatedCylinderItem.set(be.getCylinder(), liquid,
                            GraduatedCylinderItem.getMl(be.getCylinder()) - 1);
                    be.setCylinder(be.getCylinder());
                    player.displayClientMessage(
                            Component.translatable("mchemistry.cylinder.discard"), true);
                    level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY,
                            SoundSource.BLOCKS, 0.8F, 1.2F);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // 细口瓶右键：倒入 5mL。
        String liquidId = com.example.chemistry.transfer.BottleCodes.liquidIdOf(stack);
        if (liquidId != null) {
            if (!level.isClientSide()) {
                double added = GraduatedCylinderItem.add(be.getCylinder(), liquidId, 5);
                be.setCylinder(be.getCylinder());
                player.displayClientMessage(
                        Component.translatable("mchemistry.cylinder.pour5"), true);
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof PlacedGraduatedCylinderBlockEntity be
                && !be.getCylinder().isEmpty()) {
            if (!level.isClientSide()) {
                ItemStack cyl = be.getCylinder();
                be.setCylinder(ItemStack.EMPTY);
                level.removeBlock(pos, false);
                if (!player.getInventory().add(cyl)) {
                    player.drop(cyl, false);
                }
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

}
