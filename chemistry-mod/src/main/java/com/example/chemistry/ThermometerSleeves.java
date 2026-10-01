package com.example.chemistry;

import java.util.List;
import com.example.chemistry.entity.*;
import com.example.chemistry.registry.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/** Shared socket occupancy and lifecycle for entity-mounted thermometer adapters. */
@EventBusSubscriber(modid = ChemistryMod.MODID)
public final class ThermometerSleeves {
    private ThermometerSleeves() { }
    public record Mount(Vec3 position, float scale, float yaw, float tilt) { }
    public static List<ThermometerSleeveEntity> attached(Entity owner) {
        if (owner == null) return List.of();
        return owner.level().getEntitiesOfClass(ThermometerSleeveEntity.class, owner.getBoundingBox().inflate(4),
                s -> s.ownerId().equals(owner.getUUID().toString()) && !s.isRemoved());
    }
    public static boolean occupied(Entity owner, int port) {
        return attached(owner).stream().anyMatch(s -> s.port() == port);
    }
    public static boolean hasThermometer(Entity owner) {
        return attached(owner).stream().anyMatch(s -> !s.thermometer().isEmpty());
    }
    private static boolean groundVessel(ItemStack vessel) {
        String id = BuiltInRegistries.ITEM.getKey(vessel.getItem()).getPath();
        return id.startsWith("ground_glass_") || id.equals("three_neck_flask");
    }
    public static Mount mount(Entity owner, int port) {
        if (owner instanceof PlacedVesselEntity v && groundVessel(v.getVessel())) {
            boolean triple = VesselHeating.isThreeNeck(v.getVessel());
            if (port < 0 || port > 2 || (!triple && port != 1)) return null;
            Vec3 point = triple ? VesselHeating.neckWorldPositions(v.geometryOrigin(),v.getMountScale(),v.getMountOffX(),
                    v.getMountOffY(),v.getMountOffZ(),v.getMountYaw())[port]
                    : VesselHeating.mouthWorldPosition(v.geometryOrigin(),VesselHeating.vesselType(v.getVessel()),
                    v.getMountScale(),v.getMountOffX(),v.getMountOffY(),v.getMountOffZ(),v.getMountYaw());
            return new Mount(point,v.getMountScale(),v.getMountYaw(),triple ? (port==0?35F:port==2?-35F:0F):0F);
        }
        if (owner instanceof DistillationPartEntity part) {
            IronStandEntity stand=DistillationAssembly.standFor(part);
            if (stand==null || port!=0) return null;
            if (part.kind()==DistillationPartEntity.HEAD) {
                Vec3 point=DistillationAssembly.headTop(stand);
                return point==null?null:new Mount(point,.5F,part.assemblyYaw(),0);
            }
            if (part.kind()==DistillationPartEntity.ADAPTER_BENT || part.kind()==DistillationPartEntity.ADAPTER_STRAIGHT) {
                Vec3 point=DistillationAssembly.adapterOutlet(stand);
                return point==null?null:new Mount(point,.5F,part.assemblyYaw(),180);
            }
        }
        return null;
    }
    public static boolean available(Entity owner, int port) {
        if (mount(owner,port)==null || occupied(owner,port)) return false;
        if (owner instanceof PlacedVesselEntity v) {
            if (v.isReceiver()) return false;
            ItemStack vessel=v.getVessel();
            if (VesselHeating.isThreeNeck(vessel)) {
                if (VesselHeating.neckHasStopper(vessel,port) || VesselHeating.rubberHoles(vessel,port)>0) return false;
            } else if (VesselHeating.isSealed(vessel)) return false;
            return port!=1 || !DistillationAssembly.hasHead(v);
        }
        if (owner instanceof DistillationPartEntity p) {
            IronStandEntity stand=DistillationAssembly.standFor(p);
            return p.kind()==DistillationPartEntity.HEAD
                    ? DistillationAssembly.part(stand,DistillationPartEntity.THERMOMETER)==null
                    : DistillationAssembly.receiver(stand)==null;
        }
        return false;
    }
    /** Returns nearest actual ray intersection, avoiding a neighboring occupied neck. */
    private static int aimedPort(Entity owner, Player player) {
        int best=-1; double distance=Double.MAX_VALUE;
        for(int i=0;i<3;i++) {
            Mount m=mount(owner,i); if(m==null)continue;
            Vec3 from=player.getEyePosition(),to=from.add(player.getLookAngle().scale(6));
            // Head/adapter sockets are isolated; give their visible rim a forgiving target.
            // Keep three-neck flask sockets tight so adjacent necks remain distinct.
            double radius=owner instanceof DistillationPartEntity ? .15 : .10*m.scale()+.025;
            var hit=new net.minecraft.world.phys.AABB(m.position(),m.position()).inflate(radius).clip(from,to);
            if(hit.isPresent() && hit.get().distanceToSqr(from)<distance) { best=i;distance=hit.get().distanceToSqr(from); }
        }
        return best;
    }
    public static ThermometerSleeveEntity install(Entity owner, int port, ItemStack sleeve) {
        if (owner.level().isClientSide() || !available(owner,port) || !sleeve.is(ModItems.THERMOMETER_SLEEVE.get())) return null;
        var entity=new ThermometerSleeveEntity(ModEntities.THERMOMETER_SLEEVE.get(),owner.level());
        entity.attach(owner,port,sleeve);
        return owner.level().addFreshEntity(entity) ? entity : null;
    }
    public static InteractionResult interact(Entity owner,Player player,InteractionHand hand) {
        // Vanilla may hit a neighboring part's larger box before the aimed socket.
        // Resolve the assembly first; local handling must never call this proxy again.
        IronStandEntity stand = owner instanceof PlacedVesselEntity vessel
                ? DistillationAssembly.standFor(vessel)
                : owner instanceof DistillationPartEntity part ? DistillationAssembly.standFor(part) : null;
        if (stand != null) {
            InteractionResult result = proxy(stand, player, hand);
            if (result != InteractionResult.PASS) return result;
        }
        return interactLocal(owner, player, hand);
    }
    private static InteractionResult interactLocal(Entity owner,Player player,InteractionHand hand) {
        ItemStack held=player.getItemInHand(hand);
        // A large vessel/head hit box must not hide the adapter inside it.
        for(var sleeve:attached(owner)) {
            if(sleeve.virtualHitbox().clip(player.getEyePosition(),player.getEyePosition().add(player.getLookAngle().scale(6))).isPresent()
                    && (held.isEmpty() || held.is(ModItems.THERMOMETER.get()))) return sleeve.interact(player,hand);
        }
        int port=aimedPort(owner,player);
        if(held.is(ModItems.THERMOMETER_SLEEVE.get())) {
            if(port<0 || !available(owner,port)) {
                if(!owner.level().isClientSide())player.displayClientMessage(Component.literal("请对准空闲的磨口安装套管"),true);
            } else if(!owner.level().isClientSide() && install(owner,port,held)!=null && !player.isCreative())held.shrink(1);
            return InteractionResult.SUCCESS;
        }
        if(port>=0 && occupied(owner,port) && (held.is(ModItems.GLASS_STOPPER.get())
                || held.is(ModItems.RUBBER_STOPPER_1_HOLE.get()) || held.is(ModItems.RUBBER_STOPPER_2_HOLE.get())
                || held.is(ModItems.RUBBER_STOPPER_3_HOLE.get()) || held.is(ModItems.DISTILLATION_HEAD.get()))) {
            if(!owner.level().isClientSide())player.displayClientMessage(Component.literal("该磨口已安装温度计套管"),true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    public static boolean blocksStopper(PlacedVesselEntity v, Player player, Vec3 click) {
        int port=1;
        if(VesselHeating.isThreeNeck(v.getVessel())) {
            Vec3[] necks=VesselHeating.neckWorldPositions(v.geometryOrigin(),v.getMountScale(),v.getMountOffX(),v.getMountOffY(),v.getMountOffZ(),v.getMountYaw());
            port=VesselHeating.neckForRay(necks,player);
            if(port<0)port=VesselHeating.neckForClick(necks,click);
        }
        return occupied(v,port);
    }
    public static InteractionResult proxy(IronStandEntity stand, Player player, InteractionHand hand) {
        var owners=new java.util.ArrayList<Entity>();
        var head=DistillationAssembly.part(stand,DistillationPartEntity.HEAD);
        var adapter=DistillationAssembly.adapter(stand);
        if(head!=null)owners.add(head); if(adapter!=null)owners.add(adapter);
        if(stand.findMountedVessel()!=null)owners.add(stand.findMountedVessel());
        for(Entity owner:owners) {
            ItemStack held=player.getItemInHand(hand);
            if(held.is(ModItems.THERMOMETER_SLEEVE.get()) && aimedPort(owner,player)>=0)
                return interactLocal(owner,player,hand);
            for(var sleeve:attached(owner)) {
                if((held.isEmpty() || held.is(ModItems.THERMOMETER.get())) && sleeve.virtualHitbox().clip(
                        player.getEyePosition(),player.getEyePosition().add(player.getLookAngle().scale(6))).isPresent())
                    return sleeve.interact(player,hand);
            }
        }
        return InteractionResult.PASS;
    }
    public static ItemStack measuredVessel(Entity owner) {
        if(owner instanceof PlacedVesselEntity v)return v.getVessel();
        if(owner instanceof DistillationPartEntity p) {
            IronStandEntity stand=DistillationAssembly.standFor(p);
            if(stand!=null && stand.findMountedVessel()!=null)return stand.findMountedVessel().getVessel();
        }
        return ItemStack.EMPTY;
    }
    @SubscribeEvent public static void onOwnerRemoved(EntityLeaveLevelEvent event) {
        Entity owner=event.getEntity();
        if(!(owner.level() instanceof ServerLevel level) || owner.getRemovalReason()==null
                || !owner.getRemovalReason().shouldDestroy())return;
        if(!(owner instanceof PlacedVesselEntity || owner instanceof DistillationPartEntity))return;
        // Unloading a chunk does not destroy attachments. Actual pickup/break returns each once.
        for(var sleeve:attached(owner)) {
            for(ItemStack item:sleeve.contents())sleeve.spawnAtLocation(level,item);
            sleeve.discard();
        }
    }
}
