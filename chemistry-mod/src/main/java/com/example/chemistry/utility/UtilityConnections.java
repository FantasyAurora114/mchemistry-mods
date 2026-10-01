package com.example.chemistry.utility;

import java.util.*;
import com.example.chemistry.*;
import com.example.chemistry.entity.*;
import com.example.chemistry.electrical.ElectroDeviceEntity;
import com.example.chemistry.registry.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Typed two-end hoses share the electrical cable curve, but never enter its circuit graph. */
@EventBusSubscriber(modid=ChemistryMod.MODID)
public final class UtilityConnections {
    private static final String KEY="utility_pending";
    private record Target(Entity entity,int slot) {}
    public static List<UtilityLineEntity> lines(Entity e){return e.level().getEntitiesOfClass(UtilityLineEntity.class,e.getBoundingBox().inflate(10),w->!w.isRemoved());}
    public static Vec3 port(Entity e,int slot){
        if(e instanceof WaterMachineEntity m)return m.port(slot);
        if(e instanceof com.example.chemistry.filtration.FilterFunnelEntity f&&f.buchner()&&com.example.chemistry.organic.OrganicApparatus.is(f.receiver(),"suction_flask"))return f.vacuumPort();
        if(e instanceof IECPlugEntity)return e.position();
        if(e instanceof ElectroDeviceEntity m&&m.isPower()&&slot==4){double a=Math.toRadians(-m.getYRot());double x=.25*ElectroDeviceEntity.MODEL_SCALE,z=.50*ElectroDeviceEntity.MODEL_SCALE;return m.position().add(x*Math.cos(a)+z*Math.sin(a),.5*ElectroDeviceEntity.MODEL_SCALE,-x*Math.sin(a)+z*Math.cos(a));}
        if(e instanceof PlacedVesselEntity v){
            var attachment=slot==1?v.getAttached1():slot==2?v.getAttached2():ItemStack.EMPTY;
            if(!com.example.chemistry.item.GlassTubeItem.isGlassTube(attachment))return null;
            return com.example.chemistry.entity.RubberTubeEntity.Port.stand(v.geometryOrigin(),slot).worldPos(e.level());
        }
        if(e instanceof DistillationPartEntity p&&p.kind()==DistillationPartEntity.CONDENSER){
            // Same rotation/scale as DistillationPartRenderer. Lower inlet / upper outlet.
            double sc=DistillationAssembly.MODEL_SCALE,x=(slot==0?12.7:3.3)-8,y=(slot==0?3.1:12.9)-8,z=0;
            double c=Math.sqrt(.5),rx=(x-y)*c*sc/16-c*sc/2,ry=(x+y)*c*sc/16+c*sc/2+.3;
            double a=Math.toRadians(p.assemblyYaw());return p.position().add(rx*Math.cos(a)+z*Math.sin(a),ry,-rx*Math.sin(a)+z*Math.cos(a));
        }return null;
    }
    private static CompoundTag tag(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    private static void clear(ItemStack s){var t=tag(s);t.remove(KEY);s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));}
    public static boolean isPending(Player p,UtilityLineEntity line){return java.util.stream.Stream.of(p.getMainHandItem(),p.getOffhandItem()).anyMatch(s->tag(s).getStringOr(KEY,"").equals(line.getUUID().toString()));}
    private static UtilityLineEntity pending(Player p,ItemStack s){try{var e=p.level().getEntity(UUID.fromString(tag(s).getStringOr(KEY,"")));return e instanceof UtilityLineEntity w&&w.preview()&&w.b().equals(p.getUUID().toString())?w:null;}catch(IllegalArgumentException ex){return null;}}
    private static void tell(Player p,String s){p.displayClientMessage(Component.literal(s),true);}
    private static boolean occupied(Entity e,int slot){return lines(e).stream().anyMatch(w->!w.preview()&&w.uses(e,slot));}
    private static Target target(Player p,boolean iec,boolean vacuum){var from=p.getEyePosition();var end=from.add(p.getLookAngle().scale(6));Target closest=null;double distance=Double.MAX_VALUE;
        for(var e:p.level().getEntities(p,new AABB(from,end).inflate(.4),e->e instanceof WaterMachineEntity||iec&&e instanceof ElectroDeviceEntity||!iec&&e instanceof DistillationPartEntity||vacuum&&(e instanceof PlacedVesselEntity||e instanceof com.example.chemistry.filtration.FilterFunnelEntity))){
            int[] slots=e instanceof WaterMachineEntity?iec?new int[]{2}:new int[]{0,1}:e instanceof ElectroDeviceEntity?new int[]{4}:e instanceof DistillationPartEntity?new int[]{0,1}:e instanceof com.example.chemistry.filtration.FilterFunnelEntity?new int[]{0}:new int[]{1,2};
            if(e instanceof ElectroDeviceEntity d&&!d.isPower())continue;
            if(e instanceof DistillationPartEntity d&&d.kind()!=DistillationPartEntity.CONDENSER)continue;
            for(int i:slots){var point=port(e,i);if(point==null)continue;var hit=new AABB(point,point).inflate(.115).clip(from,end);if(hit.isPresent()&&from.distanceTo(hit.get())<distance){closest=new Target(e,i);distance=from.distanceTo(hit.get());}}
        }return closest;
    }
    private static String kind(Target t,boolean iec){return iec?"iec":t.entity instanceof WaterMachineEntity m&&m.vacuum()?"vacuum":"water";}
    public static boolean click(Player p,InteractionHand h){var s=p.getItemInHand(h);boolean iec=s.is(ModItems.IEC_CABLE.get());if(!iec&&!s.is(ModItems.RUBBER_TUBE.get()))return false;var w=pending(p,s);
        if(p.isShiftKeyDown()&&w!=null){if(!p.level().isClientSide()){w.discard();clear(s);}return true;}
        var t=target(p,iec,w!=null&&w.kind().equals("vacuum"));if(t==null)return false;if(p.level().isClientSide())return true;
        if(occupied(t.entity,t.slot)){tell(p,"接口已占用");return true;}
        if(w==null){clear(s);w=new UtilityLineEntity(ModEntities.UTILITY_LINE.get(),p.level());w.kind(kind(t,iec));w.connect(t.entity,t.slot,p,-1,true);p.level().addFreshEntity(w);var data=tag(s);data.putString(KEY,w.getUUID().toString());s.set(DataComponents.CUSTOM_DATA,CustomData.of(data));tell(p,iec?"已接入IEC口；点击方块放置另一端插头，潜行取消":"第一端已固定；点击另一接口，潜行取消");return true;}
        var first=w.endpoint(true);boolean valid=first!=null&&first!=t.entity&&!occupied(first,w.ap());
        if(w.kind().equals("iec"))valid=false;
        else if(w.kind().equals("vacuum"))valid&=first instanceof WaterMachineEntity m&&m.vacuum()&&((t.entity instanceof PlacedVesselEntity v&&VesselHeating.isSealed(v.getVessel())&&VesselHeating.getStopperHoles(v.getVessel())>0&&port(v,t.slot)!=null)||(t.entity instanceof com.example.chemistry.filtration.FilterFunnelEntity f&&f.buchner()&&port(f,t.slot)!=null));
        else valid&=(first instanceof WaterMachineEntity m&&!m.vacuum()&&t.entity instanceof DistillationPartEntity)||(first instanceof DistillationPartEntity&&t.entity instanceof WaterMachineEntity m2&&!m2.vacuum());
        if(!valid){tell(p,"接口不匹配；抽气需装有玻璃导管和带孔胶塞的密封容器，IEC另一端放在方块上");return true;}
        if(port(first,w.ap()).distanceTo(port(t.entity,t.slot))>8){tell(p,"管线最长8格");return true;}
        w.connect(first,w.ap(),t.entity,t.slot,false);clear(s);if(!p.isCreative())s.shrink(1);tell(p,"连接完成");return true;
    }
    public static boolean layPlug(Player p,ItemStack s,Vec3 at){var w=pending(p,s);if(w==null||!w.kind().equals("iec")||w.point(true)==null)return false;if(w.point(true).distanceTo(at)>8){tell(p,"导线最长8格");return true;}var plug=new IECPlugEntity(ModEntities.IEC_PLUG.get(),p.level());plug.setPos(at);p.level().addFreshEntity(plug);w.connect(w.endpoint(true),w.ap(),plug,0,false);clear(s);if(!p.isCreative())s.shrink(1);return true;}
    private static Entity other(UtilityLineEntity w,Entity e){return w.a().equals(e.getUUID().toString())?w.endpoint(false):w.endpoint(true);}
    private static int otherSlot(UtilityLineEntity w,Entity e){return w.a().equals(e.getUUID().toString())?w.bp():w.ap();}
    public static DistillationPartEntity closedCoolingLoop(WaterMachineEntity pump){UtilityLineEntity out=null,back=null;for(var w:lines(pump))if(!w.preview()&&w.kind().equals("water")){if(w.uses(pump,0))out=w;if(w.uses(pump,1))back=w;}if(out==null||back==null)return null;var condenser=other(out,pump);return condenser instanceof DistillationPartEntity part&&part.kind()==DistillationPartEntity.CONDENSER&&other(back,pump)==part&&otherSlot(out,pump)==0&&otherSlot(back,pump)==1?part:null;}
    public static WaterMachineEntity cooler(DistillationPartEntity condenser){for(var w:lines(condenser))if(!w.preview()&&w.kind().equals("water")&&other(w,condenser) instanceof WaterMachineEntity m&&closedCoolingLoop(m)==condenser)return m;return null;}
    public static boolean cooling(DistillationPartEntity condenser){var m=cooler(condenser);return m!=null&&m.ready();}
    public static boolean hasCoolingHose(DistillationPartEntity condenser){return lines(condenser).stream().anyMatch(w->!w.preview()&&w.kind().equals("water")&&(w.a().equals(condenser.getUUID().toString())||w.b().equals(condenser.getUUID().toString())));}
    public static double evacuate(WaterMachineEntity pump,int channel){if(!pump.ready()||!pump.flag("channel_"+channel))return 0;for(var line:lines(pump))if(!line.preview()&&line.kind().equals("vacuum")&&line.uses(pump,channel)){if(other(line,pump) instanceof com.example.chemistry.filtration.FilterFunnelEntity f&&f.buchner()&&port(f,0)!=null){pump.setNumber("pressure_"+channel,60);return 1;}if(other(line,pump) instanceof PlacedVesselEntity vessel){var s=vessel.getVessel();if(!VesselHeating.isSealed(s)||VesselHeating.getStopperHoles(s)==0||port(vessel,otherSlot(line,pump))==null)return 0;VacuumState.enable(s);var gases=VesselGasPhase.read(s);double total=gases.stream().mapToDouble(VesselGasPhase.Part::ml).sum();double draw=Math.min(5,Math.max(0,total-VesselGasPhase.freeVolumeMl(s)*20/101.325));double actual=0;for(var part:gases){double moved=total<=0?0:Math.min(part.ml(),draw*part.ml()/total);VesselGasPhase.remove(s,part.id(),moved);actual+=moved;pump.setNumber("exhaust_"+channel+"_"+part.id(),pump.number("exhaust_"+channel+"_"+part.id(),0)+moved);}vessel.setVessel(s);pump.setNumber("pressure_"+channel,VacuumState.pressure(s));pump.setNumber("exhaust_"+channel,pump.number("exhaust_"+channel,0)+actual);return actual;}}return 0;}
    public static boolean filterVacuumReady(com.example.chemistry.filtration.FilterFunnelEntity filter){return lines(filter).stream().anyMatch(w->!w.preview()&&w.kind().equals("vacuum")&&other(w,filter) instanceof WaterMachineEntity m&&m.ready()&&m.flag("channel_"+otherSlot(w,filter)));}
    public static boolean vacuumConnected(PlacedVesselEntity vessel){return lines(vessel).stream().anyMatch(w->!w.preview()&&w.kind().equals("vacuum")&&(w.a().equals(vessel.getUUID().toString())||w.b().equals(vessel.getUUID().toString())));}
    private static WaterMachineEntity sourceCooler(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos){
        for(var source:level.getEntitiesOfClass(PlacedVesselEntity.class,new AABB(pos).inflate(4))){
            if(!source.blockPosition().equals(pos))continue;
            var stand=DistillationAssembly.standFor(source);if(stand==null)continue;
            var condenser=DistillationAssembly.part(stand,DistillationPartEntity.CONDENSER);
            if(condenser!=null)return cooler(condenser);
        }return null;
    }
    public static double condensationBudget(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,String id,double grams,double vaporTemp){
        var machine=sourceCooler(level,pos);if(machine==null)return grams;
        if(!machine.ready())return 0;var water=machine.stack();double difference=vaporTemp-TemperatureSystem.getTemp(water);
        return Math.min(grams,Math.max(0,difference)*ThermalSystem.capacity(water)/Math.max(1,ThermalSystem.vaporizationJPerGram(id)));
    }
    public static void condensed(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,String id,double grams){
        var machine=sourceCooler(level,pos);if(machine==null)return;var water=machine.stack();
        ThermalSystem.addHeat(water,grams*ThermalSystem.vaporizationJPerGram(id),"condenser");machine.setStack(water);
    }
    private static void detach(UtilityLineEntity line){for(boolean first:new boolean[]{true,false}){var e=line.endpoint(first);if(e instanceof IECPlugEntity)e.discard();if(line.kind().equals("vacuum")&&e instanceof PlacedVesselEntity v){var s=v.getVessel();VacuumState.release(s);v.setVessel(s);}}line.discard();}
    public static boolean cut(Player p){var a=p.getEyePosition();var b=a.add(p.getLookAngle().scale(6));for(var w:lines(p))if(!w.preview()&&w.rayHit(a,b)){var item=w.toStack();detach(w);if(!p.getInventory().add(item))p.drop(item,false);return true;}return false;}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void block(PlayerInteractEvent.RightClickBlock e){if(click(e.getEntity(),e.getHand())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void item(PlayerInteractEvent.RightClickItem e){if(click(e.getEntity(),e.getHand())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void entity(PlayerInteractEvent.EntityInteract e){if(click(e.getEntity(),e.getHand())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}}
    @SubscribeEvent public static void removed(EntityLeaveLevelEvent event){var e=event.getEntity();if(!(e.level() instanceof ServerLevel level)||e.getRemovalReason()==null||!e.getRemovalReason().shouldDestroy()||e instanceof UtilityLineEntity||e instanceof IECPlugEntity)return;for(var w:lines(e))if(w.a().equals(e.getUUID().toString())||w.b().equals(e.getUUID().toString())){if(!w.preview())w.spawnAtLocation(level,w.toStack());detach(w);}}
    private UtilityConnections(){}
}
