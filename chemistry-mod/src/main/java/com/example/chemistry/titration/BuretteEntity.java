package com.example.chemistry.titration;

import com.example.chemistry.*;
import com.example.chemistry.api.goggles.*;
import com.example.chemistry.entity.*;
import com.example.chemistry.filtration.Filtration;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.List;
import java.util.Locale;

public final class BuretteEntity extends TechnicalEntity implements IChemGoggleInfo {
    public static final float SCALE=.58F;
    public static final double DROP_ML=.05;
    private static final EntityDataAccessor<ItemStack> DEVICE=SynchedEntityData.defineId(BuretteEntity.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> RECEIVER=SynchedEntityData.defineId(BuretteEntity.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<String> OWNER=SynchedEntityData.defineId(BuretteEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> OPEN=SynchedEntityData.defineId(BuretteEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DELIVERED=SynchedEntityData.defineId(BuretteEntity.class,EntityDataSerializers.FLOAT);
    public BuretteEntity(EntityType<?> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(DEVICE,ItemStack.EMPTY);b.define(RECEIVER,ItemStack.EMPTY);b.define(OWNER,"");b.define(OPEN,false);b.define(DELIVERED,0F);}
    public ItemStack device(){return entityData.get(DEVICE).copy();}public void device(ItemStack s){entityData.set(DEVICE,s.copy());}
    public ItemStack receiver(){return entityData.get(RECEIVER).copy();}public void receiver(ItemStack s){entityData.set(RECEIVER,s.copy());}
    public String ownerId(){return entityData.get(OWNER);}public boolean open(){return entityData.get(OPEN);}
    public void open(boolean value){entityData.set(OPEN,value);}public double delivered(){return entityData.get(DELIVERED);}
    public void owner(IronStandEntity s){entityData.set(OWNER,s.getUUID().toString());setYRot(s.getFacing().toYRot());setPos(s.position().add(0,s.clampLift(),0));setBoundingBox(virtualHitbox());}
    public Vec3 point(double x,double y,double z){double angle=Math.toRadians(-getYRot());return position().add(x*Math.cos(angle)+z*Math.sin(angle),y,-x*Math.sin(angle)+z*Math.cos(angle));}
    public Vec3 outlet(){return point(.03125,.42,-.19);}
    public Vec3 control(){return point(.03125,.42+5.4*SCALE/16,-.19);}
    @Override public AABB virtualHitbox(){var p=outlet();return new AABB(p.x-.20,getY()+.08,p.z-.20,p.x+.20,getY()+1.53,p.z+.20);}
    public double fillFrom(ItemStack source){var d=device();double ml=LiquidTransfer.pour(source,d,25);if(ml>0){device(d);entityData.set(DELIVERED,0F);open(false);}return ml;}
    public double drip(){
        var from=device();var to=receiver();
        double ml=LiquidTransfer.pour(from,to,DROP_ML);
        if(ml>0){ReactionEngine.checkAndStart(to,null);device(from);receiver(to);entityData.set(DELIVERED,(float)(delivered()+ml));}
        if(Filtration.liquidVolume(from)<=1e-8||ml<=0)open(false);
        return ml;
    }
    @Override public void tick(){super.tick();setBoundingBox(virtualHitbox());if(level().isClientSide())return;
        if(tickCount%4==0){
            var from=device();var to=receiver();
            if(from.getItem() instanceof LabVesselItem){PhaseSystem.tick(from,TemperatureSystem.getTemp(from));VesselHeating.coolGradual(from);device(from);}
            if(to.getItem() instanceof LabVesselItem){PhaseSystem.tick(to,TemperatureSystem.getTemp(to));ReactionEngine.checkAndStart(to,null);VesselHeating.coolGradual(to);receiver(to);}
            if(open()&&drip()>0&&level() instanceof ServerLevel server){var p=outlet();server.sendParticles(ParticleTypes.DRIPPING_WATER,p.x,p.y,p.z,1,0,0,0,0);}
        }
    }
    @Override public ItemStack toStack(){var s=device();var t=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        var ops=level().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        if(!receiver().isEmpty())t.put("burette_receiver",ItemStack.CODEC.encodeStart(ops,receiver()).getOrThrow());else t.remove("burette_receiver");
        t.putBoolean("burette_open",open());t.putDouble("burette_delivered_ml",delivered());s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));return s;
    }
    public void unpack(ItemStack s){var d=s.copyWithCount(1);var t=d.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var ops=level().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        receiver(t.get("burette_receiver")==null?ItemStack.EMPTY:ItemStack.CODEC.parse(ops,t.get("burette_receiver")).result().orElse(ItemStack.EMPTY));
        open(t.getBooleanOr("burette_open",false));entityData.set(DELIVERED,(float)t.getDoubleOr("burette_delivered_ml",0));
        t.remove("burette_receiver");t.remove("burette_open");t.remove("burette_delivered_ml");d.set(DataComponents.CUSTOM_DATA,CustomData.of(t));device(d);
    }
    private void give(Player p,ItemStack s){if(!s.isEmpty()&&!p.getInventory().add(s))p.drop(s,false);}
    @Override public InteractionResult interact(Player p,InteractionHand hand){
        if(level().isClientSide())return InteractionResult.SUCCESS;
        var held=p.getItemInHand(hand);var hit=virtualHitbox().clip(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(6)));
        double y=hit.map(Vec3::y).orElse(control().y)-getY();boolean lower=y<.40;boolean valve=y>=.45&&y<.75;
        if(held.getItem() instanceof com.example.chemistry.organic.PhasePipetteItem){
            var sampled=lower?receiver():device();
            if(com.example.chemistry.organic.PhasePipetteItem.interact(p,held,sampled)){
                if(lower)receiver(sampled);else{device(sampled);open(false);entityData.set(DELIVERED,0F);}
            }
        }else if(held.isEmpty()){
            if(lower&&!receiver().isEmpty()){open(false);give(p,receiver());receiver(ItemStack.EMPTY);}
            else if(valve){if(p.isShiftKeyDown()){open(false);drip();}else open(!open());}
            else if(p.isShiftKeyDown()){open(false);give(p,toStack());discard();}
            else if(!open()){entityData.set(DELIVERED,0F);p.displayClientMessage(Component.literal("累计滴加读数已归零"),true);}
        }else if(held.getItem() instanceof LabVesselItem&&!(held.getItem() instanceof BuretteItem)
                &&!(held.getItem() instanceof com.example.chemistry.filtration.FilterFunnelItem)){
            if(lower&&receiver().isEmpty()&&!VesselHeating.isSealed(held)){receiver(held.copyWithCount(1));held.shrink(1);}
            else fillFrom(held);
        }else if(lower&&!receiver().isEmpty()){
            var to=receiver();if(LabInteractions.interactPlacedVessel(held,to,ItemStack.EMPTY,ItemStack.EMPTY,p))receiver(to);
        }else{
            var d=device();if(LabInteractions.addToVessel(held,d,p)){device(d);open(false);entityData.set(DELIVERED,0F);}
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected void addAdditionalSaveData(ValueOutput o){o.store("device",ItemStack.OPTIONAL_CODEC,toStack());o.putString("owner",ownerId());}
    @Override protected void readAdditionalSaveData(ValueInput i){unpack(i.read("device",ItemStack.OPTIONAL_CODEC).orElse(new ItemStack(ModItems.BURETTE_GLASS.get())));entityData.set(OWNER,i.getStringOr("owner",""));}
    @Override public boolean addGoggleInfo(List<Component> lines,boolean sneak){
        lines.add(device().getHoverName());lines.add(Component.literal(String.format(Locale.ROOT,"余量 %.3f / 50 mL · 刻度 %.3f mL",Filtration.liquidVolume(device()),50-Filtration.liquidVolume(device()))));
        lines.add(Component.literal(String.format(Locale.ROOT,"累计滴加 %.3f mL · %s",delivered(),open()?"旋塞已开":"旋塞已关")));
        lines.add(Component.literal("空手点旋塞开关；潜行点旋塞滴一滴（0.05 mL）；点下方取瓶；潜行点管身拆卸"));
        if(!receiver().isEmpty()){lines.add(Component.literal("接收液："));ChemGoggleLines.appendContents(lines,receiver());if(sneak)ChemGoggleLines.appendSpecies(lines,receiver());}
        return true;
    }
}
