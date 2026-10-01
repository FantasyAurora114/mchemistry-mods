package com.example.chemistry.radiation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import com.mojang.serialization.MapCodec;
public final class ShieldedStorageBlock extends Block implements EntityBlock {
 public ShieldedStorageBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.OPEN,false).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH));}
 @Override public MapCodec<ShieldedStorageBlock> codec(){return simpleCodec(ShieldedStorageBlock::new);}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.OPEN,BlockStateProperties.HORIZONTAL_FACING);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,c.getHorizontalDirection().getOpposite());}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ShieldedStorageBlockEntity(p,s);}
 @Override protected InteractionResult useWithoutItem(BlockState s,Level level,BlockPos p,Player player,BlockHitResult hit){if(!level.isClientSide()&&level.getBlockEntity(p) instanceof ShieldedStorageBlockEntity be)player.openMenu(be);return InteractionResult.SUCCESS;}
 @Override protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return useWithoutItem(s,l,p,player,hit);}
 @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){if(!l.isClientSide()&&!player.isCreative())popResource(l,p,new net.minecraft.world.item.ItemStack(this));return super.playerWillDestroy(l,p,s,player);}
}
