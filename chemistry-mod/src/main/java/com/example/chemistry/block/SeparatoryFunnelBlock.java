package com.example.chemistry.block;

import com.example.chemistry.ChemistryMod;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 分液漏斗: placeable separatory funnel. The piston (活塞) has open/closed
 * states and the top glass stopper (瓶塞) can be taken off / put on.
 * Right-click with an item toggles the piston; empty-hand right-click first
 * removes the stopper (dropping a glass stopper), or toggles the piston when
 * the stopper is already off. Right-clicking with a glass stopper puts it on.
 */
public class SeparatoryFunnelBlock extends Block {

    public static final MapCodec<SeparatoryFunnelBlock> CODEC = simpleCodec(SeparatoryFunnelBlock::new);
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final BooleanProperty HAS_STOPPER = BooleanProperty.create("has_stopper");

    private static final VoxelShape SHAPE = box(2.0, 0.0, 2.0, 5.0, 16.0, 5.0);

    public SeparatoryFunnelBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(OPEN, false)
                .setValue(HAS_STOPPER, true));
    }

    @Override
    public MapCodec<SeparatoryFunnelBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN, HAS_STOPPER);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(glassStopper()) && !state.getValue(HAS_STOPPER)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(HAS_STOPPER, true), 3);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        // Any other held item toggles the piston (open/closed).
        if (!level.isClientSide()) {
            level.setBlock(pos, state.cycle(OPEN), 3);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            if (state.getValue(HAS_STOPPER)) {
                // Take the glass stopper off.
                ItemStack stopper = new ItemStack(glassStopper());
                level.setBlock(pos, state.setValue(HAS_STOPPER, false), 3);
                level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!player.getInventory().add(stopper)) {
                    player.drop(stopper, false);
                }
            } else {
                // No stopper: empty hand toggles the piston.
                level.setBlock(pos, state.cycle(OPEN), 3);
                level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, 1.2F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static net.minecraft.world.item.Item glassStopper() {
        return BuiltInRegistries.ITEM.getValue(
                ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "glass_stopper"));
    }
}
