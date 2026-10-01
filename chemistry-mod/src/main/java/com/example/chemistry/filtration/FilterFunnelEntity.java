package com.example.chemistry.filtration;

import java.util.*;
import com.example.chemistry.*;
import com.example.chemistry.api.goggles.IChemGoggleInfo;
import com.example.chemistry.entity.*;
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

public class FilterFunnelEntity extends TechnicalEntity implements IChemGoggleInfo {
    public static final float SCALE=.43F;
    private static final EntityDataAccessor<ItemStack> CONTENTS=SynchedEntityData.defineId(FilterFunnelEntity.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> PAPER=SynchedEntityData.defineId(FilterFunnelEntity.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> RECEIVER=SynchedEntityData.defineId(FilterFunnelEntity.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<String> OWNER=SynchedEntityData.defineId(FilterFunnelEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> PAUSED=SynchedEntityData.defineId(FilterFunnelEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> STATUS=SynchedEntityData.defineId(FilterFunnelEntity.class,EntityDataSerializers.STRING);
    public FilterFunnelEntity(EntityType<?> t,Level l){super(t,l);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(CONTENTS,ItemStack.EMPTY);b.define(PAPER,ItemStack.EMPTY);b.define(RECEIVER,ItemStack.EMPTY);b.define(OWNER,"");b.define(STATUS,"等待加液");b.define(PAUSED,false);}
    public ItemStack contents(){return entityData.get(CONTENTS).copy();}public ItemStack paper(){return entityData.get(PAPER).copy();}public ItemStack receiver(){return entityData.get(RECEIVER).copy();}
    public void contents(ItemStack s){entityData.set(CONTENTS,s.copy());}public void paper(ItemStack s){entityData.set(PAPER,s.copy());}public void receiver(ItemStack s){entityData.set(RECEIVER,s.copy());}
    public String ownerId(){return entityData.get(OWNER);}public void owner(IronStandEntity s){entityData.set(OWNER,s.getUUID().toString());setYRot(s.getFacing().toYRot());setPos(s.position().add(0,s.clampLift(),0));}
    public String status(){return entityData.get(STATUS);}
    public Vec3 point(double x,double y,double z){double angle=Math.toRadians(-getYRot()),dx=x,dz=z;return position().add(dx*Math.cos(angle)+dz*Math.sin(angle),y,-dx*Math.sin(angle)+dz*Math.cos(angle));}
    public Vec3 outlet(){return point(.03125,.46,-.14);}
    @Override public AABB virtualHitbox(){Vec3 p=outlet();return new AABB(p.x-.23,getY()+.12,p.z-.23,p.x+.23,getY()+.88,p.z+.23);}
    @Override public ItemStack toStack(){var s=contents();var tag=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var ops=level().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        if(!paper().isEmpty())tag.put("filter_paper",ItemStack.CODEC.encodeStart(ops,paper()).getOrThrow());else tag.remove("filter_paper");
        if(!receiver().isEmpty())tag.put("filter_receiver",ItemStack.CODEC.encodeStart(ops,receiver()).getOrThrow());else tag.remove("filter_receiver");
        tag.putBoolean("filter_paused",entityData.get(PAUSED));s.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return s;
    }
    public void unpack(ItemStack s){var copy=s.copyWithCount(1);var tag=copy.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var ops=level().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        if(tag.get("filter_paper")!=null)paper(ItemStack.CODEC.parse(ops,tag.get("filter_paper")).result().orElse(ItemStack.EMPTY));
        if(tag.get("filter_receiver")!=null)receiver(ItemStack.CODEC.parse(ops,tag.get("filter_receiver")).result().orElse(ItemStack.EMPTY));
        entityData.set(PAUSED,tag.getBooleanOr("filter_paused",false));tag.remove("filter_paused");
        tag.remove("filter_paper");tag.remove("filter_receiver");copy.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));contents(copy);
    }
    public boolean buchner(){return com.example.chemistry.organic.OrganicApparatus.is(contents(),"buchner_funnel");}
    public Vec3 vacuumPort(){return point(.23,.29,-.14);}
    public double process(){
        if(buchner()&&(paper().isEmpty()||!com.example.chemistry.organic.OrganicApparatus.is(receiver(),"suction_flask"))){entityData.set(STATUS,"布氏漏斗需要滤纸与厚壁抽滤瓶");return 0;}
        if(buchner()&&!com.example.chemistry.utility.UtilityConnections.filterVacuumReady(this)){entityData.set(STATUS,"请将抽滤瓶侧口连接运行中的真空泵");return 0;}
if(entityData.get(PAUSED)){entityData.set(STATUS,"已暂停：装入滤纸继续，空手点上方可恢复直接导流");return 0;}var from=contents();var cake=paper();var to=receiver();if(from.getItem() instanceof LabVesselItem)com.example.chemistry.PhaseSystem.tick(from,com.example.chemistry.TemperatureSystem.getTemp(from));if(to.getItem() instanceof LabVesselItem)com.example.chemistry.PhaseSystem.tick(to,com.example.chemistry.TemperatureSystem.getTemp(to));double volume=0;for(int pass=0;pass<(buchner()?6:1);pass++)volume+=Filtration.step(from,cake,to);contents(from);paper(cake);receiver(to);
        entityData.set(STATUS,to.isEmpty()?"请在下方安装接收容器":Filtration.liquidVolume(from)<=1e-9?"过滤完成":!cake.isEmpty()&&Filtration.solids(cake)>=20-1e-9?"滤渣已满，请更换滤纸":volume>0?(cake.isEmpty()?"未装滤纸：直接导流":"正在过滤"):"接收容器已满、封口或滤液不足");return volume;
    }
    @Override public void tick(){super.tick();setBoundingBox(virtualHitbox());if(level().isClientSide()||tickCount%4!=0)return;
        if(process()>0&&level() instanceof ServerLevel server){var p=outlet();server.sendParticles(ParticleTypes.DRIPPING_WATER,p.x,p.y,p.z,1,0,0,0,0);}
    }
    private void give(Player p,ItemStack s){if(!s.isEmpty()&&!p.getInventory().add(s))p.drop(s,false);}
    @Override public InteractionResult interact(Player p,InteractionHand hand){if(level().isClientSide())return InteractionResult.SUCCESS;var held=p.getItemInHand(hand);if(hand==InteractionHand.MAIN_HAND&&held.getItem() instanceof LabVesselItem&&p.getOffhandItem().is(ModItems.GLASS_ROD.get())){var target=contents();if(com.example.chemistry.organic.OrganicApparatus.interact(p,target,this::contents,outlet().add(0,.2,0)))return InteractionResult.SUCCESS;}
        var hit=virtualHitbox().clip(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(6)));boolean lower=hit.isPresent()&&hit.get().y<getY()+.43;
        if(held.getItem() instanceof com.example.chemistry.organic.PhasePipetteItem){
            var sampled=lower?receiver():contents();
            if(com.example.chemistry.organic.PhasePipetteItem.interact(p,held,sampled)){if(lower)receiver(sampled);else contents(sampled);}
        }else if(held.isEmpty()){
            if(lower&&!receiver().isEmpty()){give(p,receiver());receiver(ItemStack.EMPTY);}
            else if(p.isShiftKeyDown()){give(p,toStack());discard();}
            else if(!paper().isEmpty()){give(p,paper());paper(ItemStack.EMPTY);entityData.set(PAUSED,true);}
            else if(entityData.get(PAUSED)){entityData.set(PAUSED,false);}
            else p.displayClientMessage(Component.literal("空手点下方取接收瓶；点上方取滤纸并暂停；潜行点上方取整套"),true);
        }else if(held.is(ModItems.FILTER_PAPER.get())||held.is(ModItems.USED_FILTER_PAPER.get())){
            if(paper().isEmpty()){paper(held.is(ModItems.FILTER_PAPER.get())?new ItemStack(ModItems.USED_FILTER_PAPER.get()):held.copyWithCount(1));entityData.set(PAUSED,false);held.shrink(1);}
        }else if(held.getItem() instanceof LabVesselItem && !(held.getItem() instanceof FilterFunnelItem)){
            if(receiver().isEmpty()&&(lower||LabVesselItem.usedVolume(held)==0)){
                if(VesselHeating.vesselType(held)>0&&!VesselHeating.isSealed(held)){receiver(held.copyWithCount(1));held.shrink(1);}
            }else{var from=contents();if(Filtration.liquidVolume(held)>0&&Filtration.pour(held,from,25))contents(from);}
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected void addAdditionalSaveData(ValueOutput o){o.store("device",ItemStack.OPTIONAL_CODEC,toStack());o.putString("owner",ownerId());}
    @Override protected void readAdditionalSaveData(ValueInput i){unpack(i.read("device",ItemStack.OPTIONAL_CODEC).orElse(new ItemStack(ModItems.FILTER_FUNNEL.get())));entityData.set(OWNER,i.getStringOr("owner",""));}
    @Override public boolean addGoggleInfo(List<Component> lines,boolean sneak){lines.add(Component.literal(buchner()?"布氏抽滤装置":"重力过滤漏斗"));lines.add(Component.literal(String.format(java.util.Locale.ROOT,"待滤混合物 %.2f / 100 mL",LabVesselItem.usedVolume(contents()))));lines.add(Component.literal(paper().isEmpty()?"未安装滤纸":String.format(java.util.Locale.ROOT,"滤渣 %.2f / 20 g · 湿滤液 %.2f mL",Filtration.solids(paper()),Filtration.liquidVolume(paper()))));lines.add(Component.literal(status()));return true;}
}
