package com.example.chemistry.blockentity;

import com.example.chemistry.block.LaboratoryBenchBlock;
import com.example.chemistry.registry.ModBlockEntities;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** Seven independent bays in A, four in B/C; each bay owns nine item slots. */
public class LaboratoryBenchBlockEntity extends BlockEntity {
    private NonNullList<ItemStack> items;
    private int openBits;
    private int waterMl;
    private int hotSetting;
    private boolean coldOn;
    private boolean dropped;
    public boolean suppressDrop;
    public final float[] previousOpen=new float[7], openProgress=new float[7];
    public LaboratoryBenchBlockEntity(BlockPos p,BlockState s) {
        super(ModBlockEntities.LABORATORY_BENCH.get(),p,s);
        items=NonNullList.withSize(bays()*9,ItemStack.EMPTY);
    }
    public int bays(){return ((LaboratoryBenchBlock)getBlockState().getBlock()).variant()==0?7:4;}
    public ItemStack item(int bay,int slot){return items.get(bay*9+slot);}
    public void item(int bay,int slot,ItemStack stack){items.set(bay*9+slot,stack);sync();}
    public boolean open(int bay){return (openBits&(1<<bay))!=0;}
    public void toggle(int bay){openBits^=1<<bay;sync();}
    public int waterMl(){return waterMl;}
    public boolean addWater(int ml){if(ml<=0||waterMl+ml>4000)return false;waterMl+=ml;sync();return true;}
    public boolean consumeWater(int ml){if(ml<=0||waterMl<ml)return false;waterMl-=ml;sync();return true;}
    public int hotSetting(){return hotSetting;}
    public void nextHot(){hotSetting=hotSetting>=60?0:hotSetting<30?30:hotSetting+10;sync();}
    public boolean coldOn(){return coldOn;}
    public void toggleCold(){coldOn=!coldOn;sync();}
    public int outletTemperature(){return hotSetting>0?hotSetting:25;}
    public boolean flowing(){return hotSetting>0||coldOn;}
    public void tickWater(){
        if(level==null||level.isClientSide()||((LaboratoryBenchBlock)getBlockState().getBlock()).variant()!=2||!flowing()||waterMl<=0)return;
        long tick=level.getGameTime();
        if(tick%4==0)consumeWater(1); // Uncollected outflow drains at 5 mL/s.
        var spout=com.example.chemistry.block.BenchGeometry.world(getBlockState(),worldPosition,new net.minecraft.world.phys.Vec3(8,19.75,9.85));
        if(tick%8==0&&level instanceof net.minecraft.server.level.ServerLevel server)server.sendParticles(net.minecraft.core.particles.ParticleTypes.FALLING_WATER,spout.x,spout.y,spout.z,2,.01,.01,.01,.015);
        if(tick%40==0)level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.WATER_AMBIENT,net.minecraft.sounds.SoundSource.BLOCKS,.12F,1.3F);
    }
    public Container bay(int index){return new Bay(this,index);}
    public void animate(){for(int i=0;i<openProgress.length;i++){previousOpen[i]=openProgress[i];openProgress[i]=net.minecraft.util.Mth.clamp(openProgress[i]+(open(i)?.1f:-.1f),0,1);}}
    public void sync(){setChanged();if(level!=null&&!level.isClientSide())level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.store("bench_items",ItemStack.OPTIONAL_CODEC.listOf(),items);out.putInt("open_bits",openBits);out.putInt("water_ml",waterMl);out.putInt("hot_setting",hotSetting);out.putBoolean("cold_on",coldOn);}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);var saved=in.read("bench_items",ItemStack.OPTIONAL_CODEC.listOf()).orElse(java.util.List.of());items=NonNullList.withSize(bays()*9,ItemStack.EMPTY);for(int i=0;i<Math.min(items.size(),saved.size());i++)items.set(i,saved.get(i));openBits=in.getIntOr("open_bits",0);waterMl=Math.clamp(in.getIntOr("water_ml",0),0,4000);hotSetting=in.getIntOr("hot_setting",0);coldOn=in.getBooleanOr("cold_on",false);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveCustomOnly(registries);}
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){
        if(dropped||level==null||level.isClientSide())return;dropped=true;
        for(ItemStack s:items)if(!s.isEmpty())net.minecraft.world.Containers.dropItemStack(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,s);
        items.clear();if(!suppressDrop)net.minecraft.world.level.block.Block.popResource(level,pos,new ItemStack(state.getBlock()));
    }
    private static final class Bay implements Container {
        private final LaboratoryBenchBlockEntity bench;private final int index;
        Bay(LaboratoryBenchBlockEntity bench,int index){this.bench=bench;this.index=index;}
        @Override public int getContainerSize(){return 9;}
        @Override public boolean isEmpty(){for(int i=0;i<9;i++)if(!getItem(i).isEmpty())return false;return true;}
        @Override public ItemStack getItem(int slot){return bench.item(index,slot);}
        @Override public ItemStack removeItem(int slot,int amount){ItemStack s=net.minecraft.world.ContainerHelper.removeItem(bench.items,index*9+slot,amount);bench.sync();return s;}
        @Override public ItemStack removeItemNoUpdate(int slot){ItemStack s=bench.items.set(index*9+slot,ItemStack.EMPTY);bench.sync();return s;}
        @Override public void setItem(int slot,ItemStack stack){bench.item(index,slot,stack);}
        @Override public void setChanged(){bench.sync();}
        @Override public boolean stillValid(Player p){return bench.level!=null&&p.distanceToSqr(bench.worldPosition.getX()+.5,bench.worldPosition.getY()+.5,bench.worldPosition.getZ()+.5)<=64&&bench.open(index);}
        @Override public void clearContent(){for(int i=0;i<9;i++)bench.items.set(index*9+i,ItemStack.EMPTY);bench.sync();}
    }
}
