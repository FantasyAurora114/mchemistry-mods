package com.example.chemistry.radiation;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.*;
public final class ShieldedStorageBlockEntity extends BaseContainerBlockEntity {
 private NonNullList<ItemStack> items=NonNullList.withSize(27,ItemStack.EMPTY);private int users;
 public ShieldedStorageBlockEntity(BlockPos p,BlockState s){super(com.example.chemistry.registry.ModBlockEntities.SHIELDED_STORAGE.get(),p,s);}
 @Override public int getContainerSize(){return 27;}
 @Override protected NonNullList<ItemStack> getItems(){return items;}
 @Override protected void setItems(NonNullList<ItemStack> v){items=v;}
 @Override protected Component getDefaultName(){return getBlockState().getBlock().getName();}
 @Override protected AbstractContainerMenu createMenu(int id,Inventory inv){return ChestMenu.threeRows(id,inv,this);}
 @Override public void startOpen(net.minecraft.world.entity.ContainerUser p){if(!(p instanceof Player player && player.isSpectator())){users++;setOpen(true);}}
 @Override public void stopOpen(net.minecraft.world.entity.ContainerUser p){if(!(p instanceof Player player && player.isSpectator())){users=Math.max(0,users-1);setOpen(users>0);}}
 private void setOpen(boolean open){if(level!=null)level.setBlock(worldPosition,getBlockState().setValue(BlockStateProperties.OPEN,open),3);}
 @Override protected void saveAdditional(ValueOutput o){super.saveAdditional(o);o.store("shield_items",ItemStack.OPTIONAL_CODEC.listOf(),items);}
 @Override protected void loadAdditional(ValueInput i){super.loadAdditional(i);items=NonNullList.withSize(27,ItemStack.EMPTY);var v=i.read("shield_items",ItemStack.OPTIONAL_CODEC.listOf()).orElse(java.util.List.of());for(int j=0;j<Math.min(27,v.size());j++)items.set(j,v.get(j));}
 @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide())setOpen(false);}
 public boolean closed(){return !getBlockState().getValue(BlockStateProperties.OPEN);}
}
