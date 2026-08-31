package com.example.chemistry.block;

import com.example.chemistry.blockentity.MagneticStirrerBlockEntity;
import com.example.chemistry.LabInteractions;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.registry.ModBlockEntities;
import com.example.chemistry.registry.ModItems;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 磁力搅拌机：放烧瓶，右键搅拌子丢入，取下烧瓶时搅拌子自动拿出。 */
public class MagneticStirrerBlock extends Block implements EntityBlock {

    public static final MapCodec<MagneticStirrerBlock> CODEC = simpleCodec(MagneticStirrerBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = box(4.0, 0.0, 4.0, 12.0, 6.0, 14.0);

    public MagneticStirrerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<MagneticStirrerBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 操作面板（正面）朝向玩家。
        return defaultBlockState().setValue(FACING,
                context.getHorizontalDirection().getOpposite());
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

    /** 碰撞箱 = 搅拌机底座 + 架上烧瓶的实体碰撞。 */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        VoxelShape base = getShape(state, level, pos, context);
        if (level.getBlockEntity(pos) instanceof MagneticStirrerBlockEntity be
                && !be.getFlask().isEmpty()) {
            int vtype = VesselHeating.vesselType(be.getFlask());
            if (vtype != 0) {
                VoxelShape vessel = VesselHeating.vesselCollisionShape(pos, vtype,
                        SCALE, OFF, 5.8 / 16.0, OFF, 0.0);
                if (!vessel.isEmpty()) {
                    base = Shapes.or(base, vessel);
                }
            }
        }
        return base;
    }

    /** 搅拌机底盘矮，烧瓶瓶口悬空；交互命中框放大到整格中部。 */
    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagneticStirrerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof MagneticStirrerBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        // 三颈烧瓶：把玻璃塞塞进正看着的瓶口。
        if (stack.is(ModItems.GLASS_STOPPER.get()) && VesselHeating.isThreeNeck(be.getFlask())) {
            if (!level.isClientSide()) {
                ItemStack flask = be.getFlask();
                if (LabInteractions.tryPlugNeck(flask, stack, player, pos,
                        hitResult.getLocation(), SCALE, OFF, 5.8 / 16.0, OFF, 0.0)) {
                    be.setFlask(flask);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // 放烧瓶（只有烧瓶可以放）。
        if (be.getFlask().isEmpty() && HeatingMantleBlock.isFlask(stack)) {
            if (!level.isClientSide()) {
                be.setFlask(stack.copy());
                stack.shrink(1);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        // 搅拌子右键丢入（有烧瓶且还没放搅拌子时）。
        if (!be.getFlask().isEmpty() && !be.hasStirBar()
                && stack.is(ModItems.STIR_BAR.get())) {
            if (!level.isClientSide()) {
                be.setStirBar(true);
                stack.shrink(1);
                level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 0.7F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof MagneticStirrerBlockEntity be
                && !be.getFlask().isEmpty()) {
            if (!level.isClientSide()) {
                ItemStack flask = be.getFlask();
                if (VesselHeating.isThreeNeck(flask)) {
                    int neck = LabInteractions.pickNeck(player, pos, hitResult.getLocation(),
                            SCALE, OFF, 5.8 / 16.0, OFF, 0.0);
                    if (neck >= 0) {
                        if (LabInteractions.tryUnplugRubberNeck(flask, neck, player)
                                || LabInteractions.tryUnplugNeck(flask, neck, player)) {
                            be.setFlask(flask);
                            return InteractionResult.SUCCESS;
                        }
                        player.displayClientMessage(
                                Component.translatable("mchemistry.flask.neck_empty"), true);
                        return InteractionResult.SUCCESS;
                    }
                }
                // 单口烧瓶：对准瓶口取下橡胶塞。
                int vtype = VesselHeating.vesselType(flask);
                if (vtype != 0 && VesselHeating.isSealed(flask)
                        && VesselHeating.mouthForRay(pos, vtype, SCALE, OFF,
                                5.8 / 16.0, OFF, 0.0, player)) {
                    int holes = VesselHeating.getStopperHoles(flask);
                    VesselHeating.unseal(flask);
                    be.setFlask(flask);
                    ItemStack stopper = new ItemStack(ModItems.stopperForHoles(holes));
                    if (!player.getInventory().add(stopper)) {
                        player.drop(stopper, false);
                    }
                    return InteractionResult.SUCCESS;
                }
                boolean bar = be.hasStirBar();
                be.setFlask(ItemStack.EMPTY);
                be.setStirBar(false);
                if (!player.getInventory().add(flask)) {
                    player.drop(flask, false);
                }
                // 搅拌子自动拿出。
                if (bar) {
                    ItemStack stirBar = new ItemStack(ModItems.STIR_BAR.get());
                    if (!player.getInventory().add(stirBar)) {
                        player.drop(stirBar, false);
                    }
                    player.displayClientMessage(
                            Component.translatable("mchemistry.stirrer.bar_back"), true);
                }
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static final double SCALE = 0.5;
    private static final double OFF = (8.5 - 8.5 * SCALE) / 16.0;
}
