package com.example.chemistry.organic;

import com.example.chemistry.*;
import com.example.chemistry.api.goggles.*;
import com.example.chemistry.entity.*;
import com.example.chemistry.filtration.Filtration;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.*;
import com.example.chemistry.titration.LiquidTransfer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.List;

/** A stand-mounted, vented gravity drain. A captured phase budget stops each run at its interface. */
public final class SeparatoryFunnelEntity extends TechnicalEntity implements IChemGoggleInfo {
    public static final float SCALE=.75F;
    private static final EntityDataAccessor<ItemStack> DEVICE=SynchedEntityData.defineId(SeparatoryFunnelEntity.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> RECEIVER=SynchedEntityData.defineId(SeparatoryFunnelEntity.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<String> OWNER=SynchedEntityData.defineId(SeparatoryFunnelEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> STATUS=SynchedEntityData.defineId(SeparatoryFunnelEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> OPEN=SynchedEntityData.defineId(SeparatoryFunnelEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CAPPED=SynchedEntityData.defineId(SeparatoryFunnelEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DELIVERED=SynchedEntityData.defineId(SeparatoryFunnelEntity.class,EntityDataSerializers.FLOAT);
    private double remaining;
    private String drainingPhase="";
    public SeparatoryFunnelEntity(EntityType<?> t,Level l){super(t,l);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(DEVICE,ItemStack.EMPTY);b.define(RECEIVER,ItemStack.EMPTY);b.define(OWNER,"");b.define(STATUS,"取下顶端玻璃塞后加液");b.define(OPEN,false);b.define(CAPPED,true);b.define(DELIVERED,0F);}
    public ItemStack device(){return entityData.get(DEVICE).copy();}
    public void device(ItemStack s){var copy=s.copy();if(copy.getItem() instanceof LabVesselItem){if(capped()&&!VesselHeating.isSealed(copy))VesselHeating.seal(copy,0);else if(!capped()&&VesselHeating.isSealed(copy))VesselHeating.unseal(copy);var tag=copy.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putBoolean("separatory_capped",capped());copy.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));LabVesselItem.updateTint(copy);}entityData.set(DEVICE,copy);}
    public ItemStack receiver(){return entityData.get(RECEIVER).copy();}public void receiver(ItemStack s){entityData.set(RECEIVER,s.copy());}
    public boolean open(){return entityData.get(OPEN);}public boolean capped(){return entityData.get(CAPPED);}public String status(){return entityData.get(STATUS);}public String ownerId(){return entityData.get(OWNER);}public double delivered(){return entityData.get(DELIVERED);}
    private void status(String text){entityData.set(STATUS,text);}
    public void close(){entityData.set(OPEN,false);}
    public void capped(boolean value){close();entityData.set(CAPPED,value);var s=device();if(value)VesselHeating.seal(s,0);else VesselHeating.unseal(s);device(s);status(value?"玻璃塞已插入；排液前请取下":"玻璃塞已取下，可加液或排液");}
    public void owner(IronStandEntity stand){entityData.set(OWNER,stand.getUUID().toString());setYRot(stand.getFacing().toYRot());setPos(stand.position().add(0,stand.clampLift(),0));setBoundingBox(virtualHitbox());}
    public Vec3 point(double x,double y,double z){double a=Math.toRadians(-getYRot());return position().add(x*Math.cos(a)+z*Math.sin(a),y,-x*Math.sin(a)+z*Math.cos(a));}
    public Vec3 outlet(){return point(.03125,.46-.8*SCALE/16,-.19);}
    public Vec3 control(){return point(.03125,.46+3.35*SCALE/16,-.19);}
    public Vec3 mouth(){return point(.03125,.46+15.15*SCALE/16,-.19);}
    @Override public AABB virtualHitbox(){var p=outlet();return new AABB(p.x-.22,getY()+.08,p.z-.22,p.x+.22,getY()+1.25,p.z+.22);}
    public double fillFrom(ItemStack source){
        if(capped()){status("请先取下顶端玻璃塞");return 0;}
        close();var s=device();double ml=LiquidTransfer.pour(source,s,25);
        if(ml>0){device(s);remaining=0;status("已加液，静置分层后打开旋塞");}return ml;
    }
    public boolean openValve(){
        close();var state=LiquidPhases.read(device());
        if(capped()){status("玻璃塞未取下，无法连续排液");return false;}
        if(!state.modelled()){status("复杂液相尚未建模，不能选择下层");return false;}
        if(state.dispersed()){status("液体仍在分散，请静置至重新分层");return false;}
        if(Filtration.solids(device())>1e-8){status("含有固体，请先过滤后分液");return false;}
        if(state.layers().isEmpty()){status("分液漏斗已空");return false;}
        remaining=state.bottom().ml();drainingPhase=state.bottom().name();entityData.set(OPEN,true);status(state.separated()?"正在排出下层；到界面自动停止":"正在排出剩余液体");return true;
    }
    public double drain(){
        if(!open())return 0;
        var from=device();var to=receiver();var state=LiquidPhases.read(from);
        if(capped()||to.isEmpty()||VesselHeating.isSealed(to)||!(to.getItem() instanceof LabVesselItem)||!state.modelled()||state.dispersed()||state.layers().isEmpty()||Filtration.solids(from)>1e-8){close();status("排液已停止：检查玻璃塞、接收瓶或液相状态");return 0;}
        if(!state.bottom().name().equals(drainingPhase)){close();status("液相已变化，请确认后重新打开旋塞");return 0;}
        double requested=Math.min(1,remaining);if(requested<=1e-8){close();status("下层已排完，换瓶后可再次打开旋塞");return 0;}
        double ml=LiquidTransfer.sample(from,to,requested,true);
        if(ml<=1e-9){close();status("接收瓶已满或无法接收液体");return 0;}
        remaining=Math.max(0,remaining-ml);entityData.set(DELIVERED,(float)(delivered()+ml));ReactionEngine.checkAndStart(to,null);device(from);receiver(to);
        if(remaining<=1e-7||Filtration.liquidVolume(from)<=1e-8){close();status("本层已排完；更换接收瓶后可排出剩余液体");}
        else if(ml<requested-1e-7){close();status("接收瓶已满，请更换");}
        return ml;
    }
    @Override public void tick(){super.tick();setBoundingBox(virtualHitbox());if(level().isClientSide()||tickCount%4!=0)return;
        var from=device();var to=receiver();com.example.chemistry.organic.OrganicChemistry.tick(from);PhaseSystem.tick(from,TemperatureSystem.getTemp(from));VesselHeating.coolGradual(from);device(from);
        if(to.getItem() instanceof LabVesselItem){PhaseSystem.tick(to,TemperatureSystem.getTemp(to));VesselHeating.coolGradual(to);receiver(to);}
        if(drain()>0&&level() instanceof ServerLevel server){var p=outlet();server.sendParticles(ParticleTypes.DRIPPING_WATER,p.x,p.y,p.z,1,0,0,0,0);}
    }
    @Override public ItemStack toStack(){var s=device();var t=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var ops=level().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        if(!receiver().isEmpty())t.put("separatory_receiver",ItemStack.CODEC.encodeStart(ops,receiver()).getOrThrow());else t.remove("separatory_receiver");
        t.putBoolean("separatory_open",open());t.putBoolean("separatory_capped",capped());t.putDouble("separatory_remaining_ml",remaining);t.putString("separatory_phase",drainingPhase);t.putDouble("separatory_delivered_ml",delivered());
        s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));LabVesselItem.updateTint(s);return s;
    }
    public void unpack(ItemStack stack){var s=stack.copyWithCount(1);var t=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var ops=level().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        receiver(t.get("separatory_receiver")==null?ItemStack.EMPTY:ItemStack.CODEC.parse(ops,t.get("separatory_receiver")).result().orElse(ItemStack.EMPTY));
        entityData.set(CAPPED,t.getBooleanOr("separatory_capped",true));entityData.set(OPEN,t.getBooleanOr("separatory_open",false));entityData.set(DELIVERED,(float)t.getDoubleOr("separatory_delivered_ml",0));remaining=t.getDoubleOr("separatory_remaining_ml",0);drainingPhase=t.getStringOr("separatory_phase","");
        for(String key:new String[]{"separatory_receiver","separatory_open","separatory_remaining_ml","separatory_phase","separatory_delivered_ml"})t.remove(key);
        s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));if(capped())VesselHeating.seal(s,0);else VesselHeating.unseal(s);device(s);
    }
    private void give(Player p,ItemStack s){if(!s.isEmpty()&&!p.getInventory().add(s))p.drop(s,false);}
    @Override public InteractionResult interact(Player p,InteractionHand hand){if(level().isClientSide())return InteractionResult.SUCCESS;
        var held=p.getItemInHand(hand);var hit=virtualHitbox().clip(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(6)));
        double y=hit.map(Vec3::y).orElse(control().y)-getY();boolean lower=y<.405, valve=y>.55&&y<.68, top=y>1.1;
        if(held.isEmpty()){
            if(lower&&!receiver().isEmpty()){close();give(p,receiver());receiver(ItemStack.EMPTY);}
            else if(valve){if(p.isShiftKeyDown()){if(openValve())drain();close();}else if(open())close();else openValve();}
            else if(top&&p.isShiftKeyDown()){
                close();if(!capped())status("请先塞好玻璃塞再摇匀");else{var s=device();PhaseSystem.dissolveAndCrystallize(s,true);device(s);status("已摇匀，静置后取塞分液");}
            }
            else if(p.isShiftKeyDown()){close();give(p,toStack());discard();}
            else if(top)capped(!capped());
            else status("空手点顶端取塞、点旋塞开关；下方取瓶；潜行点管身拆卸");
        }else if(held.getItem() instanceof PhasePipetteItem){close();var sample=lower?receiver():device();if(PhasePipetteItem.interact(p,held,sample)){if(lower)receiver(sample);else device(sample);}}
        else if(held.getItem() instanceof LabVesselItem&&!(held.getItem() instanceof SeparatoryFunnelItem)&&VesselHeating.vesselType(held)>0){
            if(lower&&receiver().isEmpty()&&!VesselHeating.isSealed(held)){close();receiver(held.copyWithCount(1));held.shrink(1);}else fillFrom(held);
        }else if(lower&&!receiver().isEmpty()){var sample=receiver();if(LabInteractions.interactPlacedVessel(held,sample,ItemStack.EMPTY,ItemStack.EMPTY,p))receiver(sample);}
        else if(held.is(ModItems.GLASS_ROD.get())){close();if(capped())status("请先取下玻璃塞");else{var s=device();LabInteractions.interactPlacedVessel(held,s,ItemStack.EMPTY,ItemStack.EMPTY,p);device(s);}}
        else if(!capped()){close();var s=device();if(LabInteractions.addToVessel(held,s,p))device(s);}
        else status("请先取下顶端玻璃塞");
        return InteractionResult.SUCCESS;
    }
    @Override protected void addAdditionalSaveData(ValueOutput o){o.store("device",ItemStack.OPTIONAL_CODEC,toStack());o.putString("owner",ownerId());}
    @Override protected void readAdditionalSaveData(ValueInput i){unpack(i.read("device",ItemStack.OPTIONAL_CODEC).orElse(new ItemStack(ModItems.SEPARATORY_FUNNEL.get())));entityData.set(OWNER,i.getStringOr("owner",""));}
    @Override public boolean addGoggleInfo(List<Component> lines,boolean sneak){lines.add(Component.literal("250 mL 分液漏斗"));lines.add(Component.literal(String.format(java.util.Locale.ROOT,"液体 %.2f / 250 mL · 已排 %.2f mL",Filtration.liquidVolume(device()),delivered())));lines.add(Component.literal((capped()?"玻璃塞已插入":"玻璃塞已取下")+" · "+(open()?"旋塞已开":"旋塞已关")));lines.add(Component.literal(status()));ChemGoggleLines.appendContents(lines,device());if(!receiver().isEmpty()){lines.add(Component.literal("接收瓶："));ChemGoggleLines.appendContents(lines,receiver());}return true;}
}
