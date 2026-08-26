package com.example.chemistry.block;

import com.example.chemistry.blockentity.PlacedTestTubeBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A test tube placed upright on the ground. The block entity stores the tube
 * item (with contents/temperature data); the model switches between empty and
 * filled, and the contents layer is tinted by the tube's substance colour.
 */
public class PlacedTestTubeBlock extends Block implements EntityBlock {

    public static final MapCodec<PlacedTestTubeBlock> CODEC = simpleCodec(PlacedTestTubeBlock::new);
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");
    public static final BooleanProperty STOPPERED = BooleanProperty.create("stoppered");

    private static final VoxelShape SHAPE = Shapes.or(
            box(5, 0, 5, 11, 15, 11));

    public PlacedTestTubeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FILLED, false).setValue(STOPPERED, false));
    }

    @Override
    public MapCodec<PlacedTestTubeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILLED, STOPPERED);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlacedTestTubeBlockEntity(pos, state);
    }

    /** Empty-handed right-click picks the tube back up. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PlacedTestTubeBlockEntity be) {
            ItemStack tube = be.getTube();
            if (!tube.isEmpty()) {
                be.setTube(ItemStack.EMPTY);
                if (!player.getInventory().add(tube)) {
                    player.drop(tube, false);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PlacedTestTubeBlockEntity be) {
            ItemStack tube = be.getTube();
            if (!tube.isEmpty()) {
                be.setTube(ItemStack.EMPTY);
                net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, tube);
                drop.setDefaultPickUpDelay();
                level.addFreshEntity(drop);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
