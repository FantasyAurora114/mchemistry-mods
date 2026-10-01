package com.example.chemistry.organic;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import com.mojang.serialization.MapCodec;
public final class LabCeilingLightBlock extends Block {
 public LabCeilingLightBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.LIT,true));}
 @Override protected MapCodec<LabCeilingLightBlock> codec(){return simpleCodec(LabCeilingLightBlock::new);}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.LIT);}
 @Override protected VoxelShape getShape(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,CollisionContext c){return Block.box(1,13,1,15,16,15);}
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!l.isClientSide())l.setBlock(p,s.cycle(BlockStateProperties.LIT),3);return InteractionResult.SUCCESS;}
}
