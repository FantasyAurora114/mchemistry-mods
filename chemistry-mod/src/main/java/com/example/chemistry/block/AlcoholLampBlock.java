package com.example.chemistry.block;

import com.example.chemistry.blockentity.AlcoholLampBlockEntity;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.level.LevelReader;

/**
 * 酒精灯: unlit / lit / capped states. The cap can be placed on an unlit lamp
 * and removed again; capping a lit lamp makes the cap permanently stuck and
 * extinguishes the flame. Lit lamps emit light, spawn flame particles at the
 * wick and work as a heat source for test tubes.
 */
public class AlcoholLampBlock extends Block implements EntityBlock {

    /** Which burner this block represents: ordinary lamp or 1200 C blowtorch. */
    public enum Kind {
        LAMP, BLOWTORCH
    }

    public static final MapCodec<AlcoholLampBlock> CODEC = simpleCodec(AlcoholLampBlock::new);
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final BooleanProperty CAPPED = BooleanProperty.create("capped");
    public static final BooleanProperty STUCK = BooleanProperty.create("stuck");

    private static final VoxelShape SHAPE = box(6.0, 0.0, 6.0, 11.0, 9.0, 11.0);
    private final Kind kind;

    public AlcoholLampBlock(Properties properties) {
        this(properties, Kind.LAMP);
    }

    public AlcoholLampBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LIT, false)
                .setValue(CAPPED, false)
                .setValue(STUCK, false));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public MapCodec<AlcoholLampBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, CAPPED, STUCK);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlcoholLampBlockEntity(
                kind == Kind.BLOWTORCH
                        ? ModBlockEntities.ALCOHOL_BLOWTORCH.get()
                        : ModBlockEntities.ALCOHOL_LAMP.get(),
                pos, state);
    }

    @Override
    @org.jetbrains.annotations.Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return (type == ModBlockEntities.ALCOHOL_LAMP.get()
                || type == ModBlockEntities.ALCOHOL_BLOWTORCH.get())
                ? (lvl, pos, st, be) -> ((AlcoholLampBlockEntity) be).tickServer(lvl)
                : null;
    }

    /** Placing any lamp variant carries the item's fuel into the block entity. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AlcoholLampBlockEntity be) {
            be.setFuel(fuelOf(stack));
        }
    }

    private static double fuelOf(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getDoubleOr("chem_fuel_ml", 0.0);
    }

    private static void setFuel(ItemStack stack, double fuel) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putDouble("chem_fuel_ml", fuel);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Flame + smoke particles at the wick while lit. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + (kind == Kind.BLOWTORCH ? 0.38 : 0.55);
        double z = pos.getZ() + 0.5;
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME,
                    x + (random.nextDouble() - 0.5) * 0.12,
                    y + (random.nextDouble() - 0.5) * 0.1,
                    z + (random.nextDouble() - 0.5) * 0.12,
                    0.0, 0.02, 0.0);
        }
        if (random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.SMOKE,
                    x + (random.nextDouble() - 0.5) * 0.1,
                    y + 0.3,
                    z + (random.nextDouble() - 0.5) * 0.1,
                    0.0, 0.05, 0.0);
        }
    }

    /** Standing on a lit lamp ignites the entity and deals fire damage. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier applier, boolean intersects) {
        if (state.getValue(LIT) && entity instanceof LivingEntity living) {
            if (level instanceof ServerLevel serverLevel) {
                living.hurtServer(serverLevel, living.damageSources().onFire(), 1.0F);
            }
            living.igniteForSeconds(3.0F);
        }
        super.entityInside(state, level, pos, entity, applier, intersects);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        // Cap on the lamp.
        if (stack.is(ModItems.ALCOHOL_LAMP_CAP.get())) {
            if (kind == Kind.BLOWTORCH) {
                // 酒精喷灯没有灯帽（不产生盖帽状态/物品）。
                if (!level.isClientSide()) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.alcohol_blowtorch.no_cap"), true);
                }
                return InteractionResult.SUCCESS;
            }
            if (state.getValue(CAPPED)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                if (state.getValue(LIT)) {
                    // Capping a lit lamp: flame snuffed, cap melts on and can never come off.
                    level.setBlock(pos, state.setValue(CAPPED, true).setValue(LIT, false).setValue(STUCK, true), 3);
                    player.displayClientMessage(Component.translatable("mchemistry.alcohol_lamp.stuck"), true);
                } else {
                    level.setBlock(pos, state.setValue(CAPPED, true).setValue(STUCK, false), 3);
                }
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 1.0F);
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        // A lit lamp used on a placed lamp turns it into a fire block.
        if (stack.is(ModItems.ALCOHOL_LAMP_LIT.get())
                || stack.is(ModItems.ALCOHOL_BLOWTORCH_LIT.get())) {
            if (!level.isClientSide()) {
                level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        // Flint & steel / fire charge light an unlit, uncapped lamp.
        if (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE)) {
            if (state.getValue(LIT)) {
                return InteractionResult.PASS;
            }
            if (state.getValue(CAPPED)) {
                if (!level.isClientSide()) {
                    player.displayClientMessage(Component.translatable("mchemistry.alcohol_lamp.cannot_light"), true);
                }
                return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide()) {
                AlcoholLampBlockEntity be = level.getBlockEntity(pos) instanceof AlcoholLampBlockEntity b ? b : null;
                if (be == null || be.getFuel() <= 0) {
                    player.displayClientMessage(Component.translatable("mchemistry.alcohol_lamp.no_fuel"), true);
                    return InteractionResult.SUCCESS;
                }
                level.setBlock(pos, state.setValue(LIT, true), 3);
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (stack.is(Items.FIRE_CHARGE)) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // 75% ethanol bottle: refill an unlit lamp; a lit lamp bursts into fire.
        String liquidId = com.example.chemistry.transfer.BottleCodes.liquidIdOf(stack);
        if ("ethanol_75".equals(liquidId)) {
            if (!level.isClientSide()) {
                if (state.getValue(CAPPED)) {
                    player.displayClientMessage(Component.translatable("mchemistry.alcohol_lamp.capped_refill"), true);
                } else if (state.getValue(LIT)) {
                    level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                    level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    player.igniteForSeconds(4.0F);
                    player.displayClientMessage(Component.translatable("mchemistry.alcohol_lamp.ethanol_fire"), true);
                } else if (level.getBlockEntity(pos) instanceof AlcoholLampBlockEntity be) {
                    double old = be.getFuel();
                    if (old >= AlcoholLampBlockEntity.CAPACITY - 0.5) {
                        player.displayClientMessage(Component.translatable("mchemistry.alcohol_lamp.full"), true);
                    } else {
                        be.setFuel(AlcoholLampBlockEntity.CAPACITY);
                        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                        player.displayClientMessage(Component.translatable("mchemistry.alcohol_lamp.refilled",
                                String.format("%.0f", AlcoholLampBlockEntity.CAPACITY - old)), true);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (liquidId != null) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("mchemistry.alcohol_lamp.wrong_fuel"), true);
            }
            return InteractionResult.SUCCESS;
        }
        // Fall through to the empty-hand interaction (cap removal).
        return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
    }

    /** Empty hand: remove the cap (unless it is stuck). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (!state.getValue(CAPPED)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            if (state.getValue(STUCK)) {
                player.displayClientMessage(Component.translatable("mchemistry.alcohol_lamp.cap_stuck"), true);
            } else {
                level.setBlock(pos, state.setValue(CAPPED, false), 3);
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
                ItemStack cap = new ItemStack(ModItems.ALCOHOL_LAMP_CAP.get());
                if (!player.getInventory().add(cap)) {
                    player.drop(cap, false);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Breaking gives the item form (unlit, or capped if it was capped). */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative()) {
            ItemStack drop = state.getValue(CAPPED) && kind == Kind.LAMP
                    ? new ItemStack(ModItems.ALCOHOL_LAMP_CAPPED.get())
                    : new ItemStack(kind == Kind.BLOWTORCH
                            ? ModItems.ALCOHOL_BLOWTORCH.get()
                            : ModItems.ALCOHOL_LAMP.get());
            if (level.getBlockEntity(pos) instanceof AlcoholLampBlockEntity be) {
                setFuel(drop, be.getFuel());
            }
            ItemEntity item = new ItemEntity(
                    (ServerLevel) level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, drop);
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos,
            BlockState state, boolean includeData) {
        ItemStack stack = new ItemStack(kind == Kind.BLOWTORCH
                ? ModItems.ALCOHOL_BLOWTORCH.get()
                : ModItems.ALCOHOL_LAMP.get());
        if (level.getBlockEntity(pos) instanceof AlcoholLampBlockEntity be) {
            setFuel(stack, be.getFuel());
        }
        return stack;
    }
}
