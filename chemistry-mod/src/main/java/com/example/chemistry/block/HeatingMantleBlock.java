package com.example.chemistry.block;

import com.example.chemistry.blockentity.HeatingMantleBlockEntity;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.LabInteractions;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 加热套：有方向（模型西面是正面/按钮面）。只能放烧瓶，用上下键调节
 *  设定温度，烧瓶缓慢升温到设定温度。 */
public class HeatingMantleBlock extends Block implements EntityBlock {

    public static final MapCodec<HeatingMantleBlock> CODEC = simpleCodec(HeatingMantleBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = box(2.0, 0.0, 2.0, 14.0, 6.0, 14.0);

    public HeatingMantleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<HeatingMantleBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 按钮面（模型西面）朝向玩家：放在玩家面前时正面朝玩家。
        return defaultBlockState().setValue(FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    /** 碰撞箱 = 加热套外壳 + 套内烧瓶（露出外壳的瓶身/瓶口部分）。 */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        VoxelShape base = getShape(state, level, pos, context);
        if (level.getBlockEntity(pos) instanceof HeatingMantleBlockEntity be
                && !be.getFlask().isEmpty()) {
            int vtype = VesselHeating.vesselType(be.getFlask());
            if (vtype != 0) {
                VoxelShape vessel = VesselHeating.vesselCollisionShape(pos, vtype,
                        SCALE, OFF, 2.5 / 16.0, OFF, 0.0);
                if (!vessel.isEmpty()) {
                    base = Shapes.or(base, vessel);
                }
            }
        }
        return base;
    }

    /** 加热套外壳矮（y=0..6），烧瓶瓶口在其上方悬空；交互命中框放大到整格中部。 */
    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeatingMantleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.HEATING_MANTLE.get()
                ? (lvl, pos, st, be) -> ((HeatingMantleBlockEntity) be).tickServer(lvl)
                : null;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof HeatingMantleBlockEntity be)) {
            return InteractionResult.PASS;
        }
        // 用温度计右键加热套：直接读出烧瓶当前温度。
        if (stack.is(com.example.chemistry.registry.ModItems.THERMOMETER.get())
                && !be.getFlask().isEmpty()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("mchemistry.thermometer.read",
                                String.format("%.0f",
                                        com.example.chemistry.TemperatureSystem
                                                .getTemp(be.getFlask()))), true);
            }
            return InteractionResult.SUCCESS;
        }
        // 向套内烧瓶装药（药匙/镊子/滴管/液体细口瓶等）。
        if (!be.getFlask().isEmpty()
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
        // 三颈烧瓶：把玻璃塞塞进正看着的瓶口。
        if (stack.is(ModItems.GLASS_STOPPER.get()) && VesselHeating.isThreeNeck(be.getFlask())) {
            if (!level.isClientSide()) {
                ItemStack flask = be.getFlask();
                if (LabInteractions.tryPlugNeck(flask, stack, player, pos,
                        hitResult.getLocation(), SCALE, OFF, 2.5 / 16.0, OFF, 0.0)) {
                    be.setFlask(flask);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // 空手右键：交给 useWithoutItem 取出烧瓶。
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        // 放入烧瓶（只有烧瓶可以放）。
        if (be.getFlask().isEmpty() && isFlask(stack)) {
            if (!level.isClientSide()) {
                be.setFlask(stack.copy());
                stack.shrink(1);
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        if (be.getFlask().isEmpty()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("mchemistry.heating_mantle.only_flask"), true);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof HeatingMantleBlockEntity be
                && !be.getFlask().isEmpty()) {
            if (!level.isClientSide()) {
                // 先拆三颈瓶所看瓶口的塞子（橡胶或玻璃），再取烧瓶。
                ItemStack flask = be.getFlask();
                if (VesselHeating.isThreeNeck(flask)) {
                    int neck = LabInteractions.pickNeck(player, pos, hitResult.getLocation(),
                            SCALE, OFF, 2.5 / 16.0, OFF, 0.0);
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
                                2.5 / 16.0, OFF, 0.0, player)) {
                    int holes = VesselHeating.getStopperHoles(flask);
                    VesselHeating.unseal(flask);
                    be.setFlask(flask);
                    ItemStack stopper = new ItemStack(ModItems.stopperForHoles(holes));
                    if (!player.getInventory().add(stopper)) {
                        player.drop(stopper, false);
                    }
                    return InteractionResult.SUCCESS;
                }
                be.setFlask(ItemStack.EMPTY);
                if (!player.getInventory().add(flask)) {
                    player.drop(flask, false);
                }
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static final double SCALE = 0.7;
    private static final double OFF = (8.5 - 8.5 * SCALE) / 16.0;

    /** 加热套只能放烧瓶。 */
    public static boolean isFlask(ItemStack stack) {
        if (!(stack.getItem() instanceof LabVesselItem)) {
            return false;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.endsWith("flask");
    }
}
