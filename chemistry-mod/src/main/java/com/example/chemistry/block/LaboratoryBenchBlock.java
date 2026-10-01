package com.example.chemistry.block;

import com.example.chemistry.blockentity.LaboratoryBenchBlockEntity;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.TemperatureSystem;
import com.example.chemistry.ThermalSystem;
import com.example.chemistry.VesselHeating;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Existing lab_table ID now maps to the new A bench. */
public class LaboratoryBenchBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    private final int variant;
    public LaboratoryBenchBlock(Properties p,int variant){super(p);this.variant=variant;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    public int variant(){return variant;}
    @Override public MapCodec<? extends Block> codec(){return simpleCodec(p->new LaboratoryBenchBlock(p,variant));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    private static final VoxelShape SINK_SHAPE=Shapes.or(
            box(0,0,0,16,11.5,16),box(0,11.5,0,16,16,4),box(0,11.5,12,16,16,16),
            box(0,11.5,4,3,16,12),box(13,11.5,4,16,16,12),
            box(7.35,16,13.3,8.65,16.35,14.6), box(7.65,16.35,13.6,8.35,20.3,14.3),
            box(7.65,20.3,12.7,8.35,21.25,14.3),box(7.65,21.25,10.2,8.35,22,13.4),
            box(7.65,20.5,9.5,8.35,21.25,10.9),box(7.65,19.79,9.5,8.35,20.5,10.2),
            box(4.8,16,13.2,6.2,17.2,14.7),box(9.8,16,13.2,11.2,17.2,14.7));
    private static VoxelShape sinkShape(Direction facing) {
        int turns=switch(facing){case EAST->1;case SOUTH->2;case WEST->3;default->0;};
        VoxelShape shape=SINK_SHAPE;
        for(int i=0;i<turns;i++){
            VoxelShape[] next={Shapes.empty()};
            shape.forAllBoxes((x1,y1,z1,x2,y2,z2)->next[0]=Shapes.or(next[0],
                    Shapes.box(1-z2,y1,x1,1-z1,y2,x2)));
            shape=next[0];
        }
        return shape;
    }
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return variant==2?sinkShape(s.getValue(FACING)):Shapes.block();}
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return getShape(s,l,p,c);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new LaboratoryBenchBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return (level,pos,state,be)->{if(be instanceof LaboratoryBenchBlockEntity b){if(level.isClientSide())b.animate();else b.tickWater();}};}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
    @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){if(!l.isClientSide()&&player.isCreative()&&l.getBlockEntity(p) instanceof LaboratoryBenchBlockEntity b)b.suppressDrop=true;return super.playerWillDestroy(l,p,s,player);}
    private static void say(Player p,String text){p.displayClientMessage(Component.literal(text),true);}
    private static int bay(int variant,double x,double y){
        if(variant==0){if(x<5.5)return x<2.75?0:1;if(x>10.5)return x<13.25?5:6;return y<5?2:y<9?3:4;}
        if(variant==1)return x<8?(x<4?0:1):(x<12?2:3);
        return x<3.5?2:x<8?0:x<12.5?1:3;
    }
    private static Vec3 local(BlockState s,BlockPos p,Vec3 v){return BenchGeometry.local(s,p,v);}
    public static boolean fillFromBucket(ItemStack held,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand){
        if(!(state.getBlock() instanceof LaboratoryBenchBlock bench)||bench.variant()!=2||!held.is(Items.WATER_BUCKET))return false;
        if(level.isClientSide())return true;
        if(!(level.getBlockEntity(pos) instanceof LaboratoryBenchBlockEntity tank))return false;
        if(!tank.addWater(1000)){say(player,"水箱已满或不足1000 mL空位（容量4000 mL）");return true;}
        if(!player.isCreative())player.setItemInHand(hand,new ItemStack(Items.BUCKET));
        level.playSound(null,pos,net.minecraft.sounds.SoundEvents.BUCKET_EMPTY,net.minecraft.sounds.SoundSource.BLOCKS,.6F,1F);
        say(player,"已向水箱加水："+tank.waterMl()+"/4000 mL；空手点水龙头旋钮出水");return true;
    }

    private InteractionResult interact(ItemStack held,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(fillFromBucket(held,s,l,p,player,hand))return InteractionResult.SUCCESS;
        Vec3 point=local(s,p,hit.getLocation());
        boolean basin=variant==2&&point.x>=3&&point.x<=13&&point.z>=4&&point.z<=12;
        if(held.getItem() instanceof com.example.chemistry.item.PlacedVesselItem vessel
                &&hit.getDirection()==Direction.UP&&!basin){
            return vessel.useOn(new net.minecraft.world.item.context.UseOnContext(player,hand,hit));
        }
        if(l.isClientSide())return InteractionResult.SUCCESS;
        if(!(l.getBlockEntity(p) instanceof LaboratoryBenchBlockEntity b))return InteractionResult.PASS;
        Vec3 h=local(s,p,hit.getLocation());double x=h.x,y=h.y,z=h.z;
        if(variant==2){
            if(held.isEmpty()&&y>=15.5&&y<=18&&z>=12.5&&z<=15.5){if(x<8){b.nextHot();say(player,b.hotSetting()==0?"热水已关闭":"热水设置为 "+b.hotSetting()+"°C");}else{b.toggleCold();say(player,b.coldOn()?"冷水已开启":"冷水已关闭");}return InteractionResult.SUCCESS;}
            if(y>=10&&z>=4&&z<=12&&x>=3&&x<=13||held.getItem() instanceof LabVesselItem&&y>=18&&z>=8.5&&z<=11&&x>=7&&x<=9){
                if(!b.flowing()||b.waterMl()==0){say(player,b.waterMl()==0?"水箱已空，请用水桶补水":"请打开冷水或热水旋钮");return InteractionResult.SUCCESS;}
                if(held.is(Items.BUCKET)){say(player,"普通水桶不能保存自来水中的溶质；请使用实验容器接水");return InteractionResult.SUCCESS;}
                if(held.getItem() instanceof LabVesselItem v){if(VesselHeating.isSealed(held)){say(player,"请先打开容器");return InteractionResult.SUCCESS;}int ml=(int)Math.min(25,Math.min(b.waterMl(),Math.floor(v.capacity()-LabVesselItem.usedVolume(held))));if(ml>0){
                    ItemStack trial=held.copy();double water=ml*.9995;boolean ok=LabVesselItem.addMass(trial,"liquid","water",water)&&LabVesselItem.addMass(trial,"liquid","calcium_chloride_solution",ml*.0002)&&LabVesselItem.addMass(trial,"liquid","magnesium_chloride_solution",ml*.0002)&&LabVesselItem.addMass(trial,"liquid","sodium_chloride_solution",ml*.0001);
                    if(ok&&b.consumeWater(ml)){double heat=water*ThermalSystem.WATER_CP+(ml-water)*3.0;ThermalSystem.addHeat(trial,heat*(b.outletTemperature()-TemperatureSystem.getTemp(held)),"tap_water");player.setItemInHand(hand,trial);say(player,"接取了"+ml+" mL自来水，出水设置"+b.outletTemperature()+"°C");}else say(player,"容器无法接水");return InteractionResult.SUCCESS;}}
                say(player,"水箱剩余 "+b.waterMl()+"/4000 mL；手持空容器点水池取水");return InteractionResult.SUCCESS;
            }
        }
        if(z>2.5&&z<15&&y>=14){say(player,variant==2?"水箱："+b.waterMl()+"/4000 mL":"实验台台面");return InteractionResult.SUCCESS;}
        if(variant==2&&y<10&&z>3&&x>=3.5&&x<=12.5&&(b.open(0)||b.open(1))){say(player,"内置水箱："+b.waterMl()+"/4000 mL；手持水桶右键加水");return InteractionResult.SUCCESS;}
        int bay=bay(variant,x,y);if(bay<0||bay>=b.bays())return InteractionResult.PASS;
        if(!b.open(bay)){b.toggle(bay);say(player,"已打开储物格 "+(bay+1));}
        else if(player.isShiftKeyDown()){b.toggle(bay);say(player,"已关闭储物格 "+(bay+1));}
        else if(player instanceof ServerPlayer sp){final int chosen=bay;sp.openMenu(new SimpleMenuProvider((id,inv,p2)->new ChestMenu(MenuType.GENERIC_9x1,id,inv,b.bay(chosen),1),Component.literal("实验台储物格 "+(bay+1))));}
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return interact(stack,s,l,p,player,hand,hit);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){return interact(ItemStack.EMPTY,s,l,p,player,InteractionHand.MAIN_HAND,hit);}
}
