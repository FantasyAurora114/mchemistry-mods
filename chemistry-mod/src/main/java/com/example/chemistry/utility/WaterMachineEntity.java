package com.example.chemistry.utility;
import com.example.chemistry.*;
import com.example.chemistry.entity.TechnicalEntity;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.titration.LiquidTransfer;
import com.example.chemistry.api.goggles.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Real reservoir and separate vacuum channels; internal power until mains devices exist. */
public class WaterMachineEntity extends TechnicalEntity implements IChemGoggleInfo {
    public static final float SCALE=.8F;
    private static final EntityDataAccessor<ItemStack> STACK=SynchedEntityData.defineId(WaterMachineEntity.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<String> STATUS=SynchedEntityData.defineId(WaterMachineEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> FLOW=SynchedEntityData.defineId(WaterMachineEntity.class,EntityDataSerializers.BOOLEAN);
    public WaterMachineEntity(EntityType<?> t,Level l){super(t,l);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(STACK,ItemStack.EMPTY);b.define(STATUS,"未启动");b.define(FLOW,false);}
    public ItemStack stack(){return entityData.get(STACK).copy();}public void setStack(ItemStack s){entityData.set(STACK,s.copyWithCount(1));}
    public boolean vacuum(){return stack().is(ModItems.CIRCULATING_WATER_VACUUM_PUMP.get());}
    public CompoundTag data(){return stack().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public double number(String key,double fallback){return data().getDoubleOr("machine_"+key,fallback);}
    public void setNumber(String key,double value){var s=stack();var t=data();t.putDouble("machine_"+key,value);s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));setStack(s);}
    public boolean flag(String key){return data().getBooleanOr("machine_"+key,false);}public void flag(String key,boolean v){var s=stack();var t=data();t.putBoolean("machine_"+key,v);s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));setStack(s);}
    public boolean flow(){return entityData.get(FLOW);}public String status(){return entityData.get(STATUS);}public void status(String s){entityData.set(STATUS,s);}
    public boolean ready(){return flag("on")&&BeakerWaterBath.water(stack())>=100&&TemperatureSystem.getTemp(stack())<60;}
    public Vec3 modelPoint(double x,double y,double z){double angle=Math.toRadians(-getYRot());return position().add((x-8)/16*SCALE*Math.cos(angle)+(z-7)/16*SCALE*Math.sin(angle),y/16*SCALE,-(x-8)/16*SCALE*Math.sin(angle)+(z-7)/16*SCALE*Math.cos(angle));}
    public Vec3 port(int slot){if(slot==2)return modelPoint(3.5,3.1,15);return vacuum()?modelPoint(slot==0?4.2:11.8,15.8,12.2):modelPoint(slot==0?5:11,13,16.5);}
    @Override public AABB virtualHitbox(){return new AABB(getX()-.43,getY(),getZ()-.4,getX()+.43,getY()+.88,getZ()+.48);}
    @Override public ItemStack toStack(){return stack();}
    public void tickServer(){var s=stack();if(flag("on")&&!vacuum()&&BeakerWaterBath.water(s)>=100){double target=Math.clamp(number("target",25),5,60),difference=target-TemperatureSystem.getTemp(s);ThermalSystem.addHeat(s,Math.copySign(Math.min(Math.abs(difference)*ThermalSystem.capacity(s),40),difference),"circulator");}else VesselHeating.coolGradual(s);PhaseSystem.tick(s,TemperatureSystem.getTemp(s));setStack(s);boolean active=false;if(vacuum()){if(ready()){for(int channel=0;channel<2;channel++)if(flag("channel_"+channel))active|=UtilityConnections.evacuate(this,channel)>0;status(active?"正在抽气；仅支持水的减压沸点":"等待密封容器、开放抽气通道与冷却水");}else status(flag("on")?"缺水或水温过高，抽气停止":"总开关已关闭");}else{active=ready()&&UtilityConnections.closedCoolingLoop(this)!=null;status(active?"闭合回路循环中；内置供电":flag("on")?"检查水量、水温及出水/回水管":"总开关已关闭");}entityData.set(FLOW,active);}
    @Override public void tick(){super.tick();setBoundingBox(virtualHitbox());if(!level().isClientSide()&&tickCount%4==0)tickServer();}
    @Override public InteractionResult interact(Player p,InteractionHand hand){if(level().isClientSide())return InteractionResult.SUCCESS;var held=p.getItemInHand(hand);if(UtilityConnections.click(p,hand))return InteractionResult.SUCCESS;if(held.is(Items.SHEARS)){UtilityConnections.cut(p);return InteractionResult.SUCCESS;}var s=stack();if(held.getItem() instanceof LabVesselItem){if(p.isShiftKeyDown())LiquidTransfer.pour(s,held,25);else LiquidTransfer.pour(held,s,25);setStack(s);return InteractionResult.SUCCESS;}if(held.is(Items.WATER_BUCKET)&&LabVesselItem.usedVolume(s)<1e-8){LabVesselItem.addMass(s,"liquid","water",1000);setStack(s);if(!p.isCreative())p.setItemInHand(hand,new ItemStack(Items.BUCKET));return InteractionResult.SUCCESS;}
        Vec3 local=local(p.getEyePosition().add(p.getLookAngle().scale(virtualHitbox().clip(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(6))).map(v->v.distanceTo(p.getEyePosition())).orElse(0.0))));
        if(held.isEmpty()){if(local.y>13){flag("lid",!flag("lid"));}else if(p.isShiftKeyDown()&&local.y<3){pickUp(p);}else if(vacuum()&&local.y>=9&&local.x<6){flag("channel_0",!flag("channel_0"));}else if(vacuum()&&local.y>=9&&local.x>10){flag("channel_1",!flag("channel_1"));}else if(!vacuum()&&local.y<10){double target=number("target",25)+(p.isShiftKeyDown()?-5:5);setNumber("target",Math.clamp(target,5,60));}else flag("on",!flag("on"));p.displayClientMessage(Component.literal(status()),true);return InteractionResult.SUCCESS;}
        if(LabInteractions.interactPlacedVessel(held,s,ItemStack.EMPTY,ItemStack.EMPTY,p)){setStack(s);return InteractionResult.SUCCESS;}return InteractionResult.PASS;}
    private Vec3 local(Vec3 p){var d=p.subtract(position());double a=Math.toRadians(getYRot());return new Vec3((d.x*Math.cos(a)+d.z*Math.sin(a))/SCALE*16+8,d.y/SCALE*16,(-d.x*Math.sin(a)+d.z*Math.cos(a))/SCALE*16+7);}
    @Override protected void addAdditionalSaveData(ValueOutput o){o.store("device",ItemStack.OPTIONAL_CODEC,stack());}
    @Override protected void readAdditionalSaveData(ValueInput i){setStack(i.read("device",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));}
    @Override public boolean addGoggleInfo(List<Component> lines,boolean sneak){lines.add(stack().getHoverName());lines.add(Component.literal(String.format(Locale.ROOT,"储液 %.1f/1000 mL · 水温 %.1f°C",LabVesselItem.usedVolume(stack()),TemperatureSystem.getTemp(stack()))));if(!vacuum())lines.add(Component.literal(String.format(Locale.ROOT,"设定 %.0f°C · %s",number("target",25),flow()?"循环供水":"未循环")));else for(int i=0;i<2;i++)lines.add(Component.literal(String.format(Locale.ROOT,"抽气口%d：%.2f kPa；累计排气 %.3f mL",i+1,number("pressure_"+i,101.325),number("exhaust_"+i,0))));lines.add(Component.literal(status()));lines.add(Component.literal("IEC为可拆连接外观；设备内置供电"));return true;}
}
