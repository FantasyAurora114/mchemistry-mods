package com.example.chemistry.blockentity;

import com.example.chemistry.block.ReagentCabinetBlock;
import com.example.chemistry.menu.ReagentCabinetMenu;
import com.example.chemistry.registry.ModBlockEntities;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

public class ReagentCabinetBlockEntity extends BaseContainerBlockEntity {
    private NonNullList<ItemStack> items;
    public boolean suppressCabinetDrop;
    private boolean dropped;
    public float previousOpen, openProgress;
    public ReagentCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REAGENT_CABINET.get(),pos,state);
        items=NonNullList.withSize(ReagentCabinetBlock.isUpper(state) ? 0 : rows()*3,ItemStack.EMPTY);
    }
    public int rows() {return ((ReagentCabinetBlock)getBlockState().getBlock()).isTall()?5:3;}
    @Override public int getContainerSize(){return items.size();}
    @Override public int getMaxStackSize(){return 1;}
    @Override public boolean canPlaceItem(int slot, ItemStack stack){return ReagentCabinetMenu.accepts(stack);}
    @Override protected NonNullList<ItemStack> getItems(){return items;}
    @Override protected void setItems(NonNullList<ItemStack> stacks){items=stacks;}
    @Override protected Component getDefaultName(){return getBlockState().getBlock().getName();}
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inv){return new ReagentCabinetMenu(id,inv,this,rows());}
    @Override public boolean stillValid(Player player){return super.stillValid(player) && getBlockState().getValue(ReagentCabinetBlock.OPEN);}
    @Override public void setChanged(){
        super.setChanged();
        if(level!=null && !level.isClientSide()) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    @Override protected void saveAdditional(ValueOutput output){
        super.saveAdditional(output);output.store("cabinet_items",ItemStack.OPTIONAL_CODEC.listOf(),items);
    }
    @Override protected void loadAdditional(ValueInput input){
        super.loadAdditional(input);
        var saved=input.read("cabinet_items",ItemStack.OPTIONAL_CODEC.listOf()).orElse(java.util.List.of());
        items=NonNullList.withSize(ReagentCabinetBlock.isUpper(getBlockState())?0:rows()*3,ItemStack.EMPTY);
        for(int i=0;i<Math.min(saved.size(),items.size());i++) items.set(i,saved.get(i));
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveCustomOnly(registries);}
    // Vanilla invokes this for breaking, replacement and explosions. Only the lower
    // half owns contents and the cabinet drop; state changes (opening) keep the BE.
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state){
        if(dropped || level==null || level.isClientSide())return;
        dropped=true;
        super.preRemoveSideEffects(pos,state);
        items.clear();
        if(!ReagentCabinetBlock.isUpper(state) && !suppressCabinetDrop) Block.popResource(level,pos,new ItemStack(state.getBlock()));
    }
    public void animate(){
        previousOpen=openProgress;
        openProgress=net.minecraft.util.Mth.clamp(openProgress+(getBlockState().getValue(ReagentCabinetBlock.OPEN)?.1f:-.1f),0,1);
    }
}
