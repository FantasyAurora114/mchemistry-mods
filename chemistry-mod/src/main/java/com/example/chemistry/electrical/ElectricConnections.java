package com.example.chemistry.electrical;

import java.util.*;
import com.example.chemistry.ChemistryMod;
import com.example.chemistry.registry.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid=ChemistryMod.MODID)
public final class ElectricConnections {
    public static final double MAX_LENGTH=8;
    private static final String KEY="ec_pending_wire";
    public static List<ElectricWireEntity> wires(Entity e){return e.level().getEntitiesOfClass(ElectricWireEntity.class,e.getBoundingBox().inflate(MAX_LENGTH+2),w->!w.isRemoved()&&!(w instanceof SaltBridgeEntity)&&!(w instanceof com.example.chemistry.utility.UtilityLineEntity));}
    public static boolean occupied(ElectroDeviceEntity e,int slot){return wires(e).stream().anyMatch(w->!w.preview()&&w.uses(e,slot));}
    public static boolean isPending(Player p,ElectricWireEntity w){if(w instanceof com.example.chemistry.utility.UtilityLineEntity utility)return com.example.chemistry.utility.UtilityConnections.isPending(p,utility);return java.util.stream.Stream.of(p.getMainHandItem(),p.getOffhandItem()).anyMatch(s->s.is(ModItems.ELECTRICAL_WIRE.get())&&tag(s).getStringOr(KEY,"").equals(w.getUUID().toString()));}
    private static CompoundTag tag(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    private static void clear(ItemStack s){var t=tag(s);t.remove(KEY);s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));}
    private static ElectricWireEntity pending(Player p,ItemStack s){try{Entity e=p.level().getEntity(UUID.fromString(tag(s).getStringOr(KEY,"")));return e instanceof ElectricWireEntity w&&w.preview()&&w.b().equals(p.getUUID().toString())?w:null;}catch(IllegalArgumentException ex){return null;}}
    public static void cancel(Player p,ItemStack s){var w=pending(p,s);if(w!=null)w.discard();clear(s);tell(p,"已取消接线，未消耗电线");}
    private static void tell(Player p,String text){p.displayClientMessage(Component.literal(text),true);}
    public static void click(ElectroDeviceEntity target,int slot,Player p,ItemStack stack){
        if(p.level().isClientSide())return;
        if(p.isShiftKeyDown()){cancel(p,stack);return;}
        if(slot<0){tell(p,"请对准红色或黑色接线柱");return;}
        if(occupied(target,slot)){tell(p,"这个接线柱已经接有电线");return;}
        ElectricWireEntity w=pending(p,stack);
        if(w==null){clear(stack);w=new ElectricWireEntity(ModEntities.ELECTRIC_WIRE.get(),p.level());w.connect(target,slot,p,-1,true);p.level().addFreshEntity(w);var t=tag(stack);t.putString(KEY,w.getUUID().toString());stack.set(DataComponents.CUSTOM_DATA,CustomData.of(t));tell(p,"已固定第一端，请点击另一接线柱；潜行右键取消");return;}
        Entity first=w.endpoint(true);
        if(!(first instanceof ElectroDeviceEntity device)){cancel(p,stack);return;}
        if(first==target&&w.ap()==slot){tell(p,"请选择另一个接线柱");return;}
        if(occupied(device,w.ap())){cancel(p,stack);return;}
        if(device.terminal(w.ap()).distanceTo(target.terminal(slot))>MAX_LENGTH){tell(p,"电线最长 8 格");return;}
        w.connect(device,w.ap(),target,slot,false);clear(stack);if(!p.isCreative())stack.shrink(1);tell(p,"电线已连接");
    }
    /** Follow a simple series loop; duplicate ports, cycles and other supplies are rejected. */
    public static List<Connection> series(ElectroDeviceEntity source,int channel){
        int start=channel*2,end=start+1;ElectroDeviceEntity at=source;int port=start;
        Set<UUID> seen=new HashSet<>();List<Connection> result=new ArrayList<>();
        for(int step=0;step<64;step++){
            ElectricWireEntity link=null;
            for(var w:wires(at))if(!w.preview()&&w.uses(at,port)){if(link!=null)return List.of();link=w;}
            if(link==null)return List.of();
            boolean first=link.a().equals(at.getUUID().toString());var far=link.endpoint(!first);
            int entry=first?link.bp():link.ap();
            if(far==source)return entry==end?List.copyOf(result):List.of();
            if(!(far instanceof ElectroDeviceEntity cell)||cell.isPower()||cell.isHalfCell()||cell.isRemoved()
                    ||entry<0||entry>1||!seen.add(cell.getUUID()))return List.of();
            result.add(new Connection(cell,entry,1-entry));at=cell;port=1-entry;
        }
        return List.of();
    }
    public static Connection circuit(ElectroDeviceEntity source){var chain=series(source,0);return chain.size()==1?chain.getFirst():null;}
    public record Connection(ElectroDeviceEntity cell,int positive,int negative){}
    public static boolean shorted(ElectroDeviceEntity source){return shorted(source,0);}
    public static boolean shorted(ElectroDeviceEntity source,int channel){return wires(source).stream().anyMatch(w->!w.preview()&&w.uses(source,channel*2)&&w.uses(source,channel*2+1));}
    public static boolean cut(Player p){if(com.example.chemistry.utility.UtilityConnections.cut(p))return true;if(!p.getMainHandItem().is(Items.SHEARS))return false;
        var from=p.getEyePosition();var to=from.add(p.getLookAngle().scale(6));
        for(var bridge:p.level().getEntitiesOfClass(SaltBridgeEntity.class,p.getBoundingBox().inflate(5)))if(bridge.rayHit(from,to)){var item=bridge.toStack();bridge.discard();if(!p.getInventory().add(item))p.drop(item,false);return true;}
        for(var wire:wires(p))if(!wire.preview()&&wire.rayHit(from,to)){var item=wire.toStack();wire.discard();if(!p.getInventory().add(item))p.drop(item,false);return true;}return false;}
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST) public static void air(PlayerInteractEvent.RightClickItem e){if(!e.getLevel().isClientSide()&&cut(e.getEntity())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}}
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST) public static void block(PlayerInteractEvent.RightClickBlock e){
        if(e.getEntity().isShiftKeyDown()&&e.getLevel().getBlockState(e.getPos()).is(com.example.chemistry.registry.ModBlocks.WATER_TROUGH.get())&&e.getItemStack().getItem() instanceof com.example.chemistry.item.LabVesselItem&&e.getLevel().getBlockState(e.getPos()).getValue(com.example.chemistry.block.WaterTroughBlock.FILLED)!=com.example.chemistry.block.WaterTroughBlock.Fill.ICE){
            com.example.chemistry.block.WaterTroughBlock.transferBath(e.getLevel(),e.getPos(),e.getEntity(),e.getItemStack());e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;
        }
        if(e.getEntity().isShiftKeyDown()&&e.getLevel().getBlockState(e.getPos()).is(com.example.chemistry.registry.ModBlocks.DEEP_WATER_TROUGH.get())){
            if(!e.getLevel().isClientSide()){var tank=com.example.chemistry.block.DeepWaterTroughBlock.device(e.getLevel(),e.getPos());if(tank!=null)tank.interact(e.getEntity(),e.getHand());}
            e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;
        }
        if(!e.getLevel().isClientSide()&&cut(e.getEntity())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}}
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST) public static void entity(PlayerInteractEvent.EntityInteract e){if(!e.getLevel().isClientSide()&&cut(e.getEntity())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}}
    @SubscribeEvent public static void removed(EntityLeaveLevelEvent event){Entity e=event.getEntity();if(!(e instanceof ElectroDeviceEntity)||!(e.level() instanceof ServerLevel level)||e.getRemovalReason()==null||!e.getRemovalReason().shouldDestroy())return;
        for(var bridge:level.getEntitiesOfClass(SaltBridgeEntity.class,e.getBoundingBox().inflate(4)))if(bridge.uses(e,0)){bridge.spawnAtLocation(level,bridge.toStack());bridge.discard();}
        for(var wire:wires(e))if(wire.a().equals(e.getUUID().toString())||wire.b().equals(e.getUUID().toString())){if(!wire.preview())wire.spawnAtLocation(level,wire.toStack());wire.discard();}
        if(!((ElectroDeviceEntity)e).isPower()){ com.example.chemistry.item.RubberTubeItem.dropTubesConnectedToStand(level,e.blockPosition(),3);
            com.example.chemistry.item.RubberTubeItem.dropTubesConnectedToStand(level,e.blockPosition(),4); }
    }
}
