package com.example.chemistry.block;

import com.example.chemistry.blockentity.ReagentCabinetBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.BlockHitResult;

public class ReagentCabinetBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private final boolean tall;
    public ReagentCabinetBlock(Properties properties, boolean tall) {
        super(properties); this.tall = tall;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER).setValue(OPEN, false));
    }
    @Override public MapCodec<? extends Block> codec() {
        return simpleCodec(p -> new ReagentCabinetBlock(p, tall));
    }
    public boolean isTall() { return tall; }
    public static boolean isUpper(BlockState s) { return s.getValue(HALF) == DoubleBlockHalf.UPPER; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, HALF, OPEN); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) {
        if (tall && (c.getClickedPos().getY() >= c.getLevel().getMaxY()
                || !c.getLevel().getBlockState(c.getClickedPos().above()).canBeReplaced(c))) return null;
        return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
    }
    @Override public void setPlacedBy(Level l, BlockPos p, BlockState s, LivingEntity e, ItemStack stack) {
        if (tall) l.setBlock(p.above(), s.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }
    // Neighbor updates remove the partner half. Its normal BE removal releases
    // inventory, avoiding a second manual drop path for upper-half destruction.
    @Override protected BlockState updateShape(BlockState s, LevelReader l, ScheduledTickAccess ticks,
            BlockPos p, Direction d, BlockPos neighbor, BlockState ns, RandomSource random) {
        if (tall && d == (isUpper(s) ? Direction.DOWN : Direction.UP)
                && (!ns.is(this) || isUpper(ns) == isUpper(s))) return Blocks.AIR.defaultBlockState();
        return super.updateShape(s,l,ticks,p,d,neighbor,ns,random);
    }
    public static ReagentCabinetBlockEntity inventory(Level l, BlockPos p) {
        BlockState s = l.getBlockState(p);
        if (!(s.getBlock() instanceof ReagentCabinetBlock)) return null;
        return l.getBlockEntity(isUpper(s) ? p.below() : p) instanceof ReagentCabinetBlockEntity be ? be : null;
    }
    public static void setOpen(Level l, BlockPos p, boolean open) {
        ReagentCabinetBlockEntity be = inventory(l,p);
        if (be == null) return;
        BlockPos base = be.getBlockPos(); BlockState s = be.getBlockState();
        l.setBlock(base,s.setValue(OPEN,open),3);
        if (((ReagentCabinetBlock)s.getBlock()).isTall() && l.getBlockState(base.above()).is(s.getBlock()))
            l.setBlock(base.above(),s.setValue(HALF,DoubleBlockHalf.UPPER).setValue(OPEN,open),3);
    }
    private InteractionResult interact(Level l, BlockPos p, Player player) {
        if (!l.isClientSide()) {
            if (player.isShiftKeyDown()) setOpen(l,p,false);
            else { ReagentCabinetBlockEntity be=inventory(l,p); if(be!=null) {setOpen(l,p,true);player.openMenu(be);} }
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState s, Level l, BlockPos p, Player player, InteractionHand hand, BlockHitResult hit) { return interact(l,p,player); }
    @Override protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player player, BlockHitResult hit) { return interact(l,p,player); }
    @Override public BlockState playerWillDestroy(Level l, BlockPos p, BlockState s, Player player) {
        if (!l.isClientSide() && player.isCreative()) { var be=inventory(l,p); if(be!=null) be.suppressCabinetDrop=true; }
        return super.playerWillDestroy(l,p,s,player);
    }
    @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new ReagentCabinetBlockEntity(p,s); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> type) {
        return l.isClientSide() ? (level,pos,state,be) -> { if(be instanceof ReagentCabinetBlockEntity cabinet) cabinet.animate(); } : null;
    }
    @Override protected BlockState rotate(BlockState s, Rotation r) {return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s, Mirror m) {return rotate(s,m.getRotation(s.getValue(FACING)));}
}
