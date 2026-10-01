package com.example.chemistry.block;

import com.example.chemistry.electrical.ElectroDeviceEntity;
import com.example.chemistry.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Solid collision host; the electrical entity is the single authority for contents and attachments. */
public final class DeepWaterTroughBlock extends Block {
    public DeepWaterTroughBlock(Properties p) { super(p); }
    public static ElectroDeviceEntity device(Level level,BlockPos pos) {
        return level.getEntitiesOfClass(ElectroDeviceEntity.class,new AABB(pos).inflate(.2),e->e.isTrough()&&e.blockPosition().equals(pos)).stream().findFirst().orElse(null);
    }
    @Override protected VoxelShape getShape(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,CollisionContext c){return box(.5,0,3,15.5,9,13);}
    @Override protected VoxelShape getCollisionShape(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,CollisionContext c){return getShape(s,l,p,c);}
    @Override protected InteractionResult useItemOn(ItemStack held,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit) {
        if(l.isClientSide())return InteractionResult.SUCCESS;var e=device(l,pos);return e==null?InteractionResult.PASS:e.interact(p,hand);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit) {
        return useItemOn(ItemStack.EMPTY,s,l,pos,p,InteractionHand.MAIN_HAND,hit);
    }
    @Override public BlockState playerWillDestroy(Level l,BlockPos pos,BlockState s,Player p) {
        var e=device(l,pos);if(e!=null&&!l.isClientSide()){if(!p.isCreative())e.spawnAtLocation((ServerLevel)l,e.toStack());e.discard();}
        return super.playerWillDestroy(l,pos,s,p);
    }
}
