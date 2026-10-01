package com.example.chemistry.filtration;
import com.example.chemistry.ChemistryMod;
import com.example.chemistry.entity.IronStandEntity;
import com.example.chemistry.registry.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
@EventBusSubscriber(modid=ChemistryMod.MODID)
public final class FilterConnections {
    public static FilterFunnelEntity attached(IronStandEntity stand){return stand.level().getEntitiesOfClass(FilterFunnelEntity.class,stand.getBoundingBox().inflate(3)).stream().filter(f->f.ownerId().equals(stand.getUUID().toString())).findFirst().orElse(null);}
    public static InteractionResult interact(IronStandEntity stand,Player player,InteractionHand hand){
        var held=player.getItemInHand(hand);var existing=attached(stand);
        if(existing!=null){
            Vec3 eye=player.getEyePosition();
            if(existing.virtualHitbox().clip(eye,eye.add(player.getLookAngle().scale(6))).isPresent())return existing.interact(player,hand);
            if(!stand.level().isClientSide())player.displayClientMessage(net.minecraft.network.chat.Component.literal("请先取下过滤装置，再拆卸或装配铁架台"),true);
            return InteractionResult.SUCCESS;
        }
        if(!held.is(ModItems.FILTER_FUNNEL.get())&&!com.example.chemistry.organic.OrganicApparatus.is(held,"buchner_funnel"))return InteractionResult.PASS;
        if(!stand.level().isClientSide()){
            if(stand.hasTube()||stand.hasVessel()||stand.findMountedVessel()!=null||stand.findMountedLamp()!=null||stand.getAttachment()!=0){player.displayClientMessage(net.minecraft.network.chat.Component.literal("请使用空铁架台安装过滤装置"),true);return InteractionResult.SUCCESS;}
            var funnel=new FilterFunnelEntity(ModEntities.FILTER_FUNNEL.get(),stand.level());funnel.unpack(held);funnel.owner(stand);stand.level().addFreshEntity(funnel);held.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
    @SubscribeEvent public static void removed(EntityLeaveLevelEvent event){var owner=event.getEntity();
        if(owner instanceof IronStandEntity stand&&owner.level() instanceof net.minecraft.server.level.ServerLevel level&&owner.getRemovalReason()!=null&&owner.getRemovalReason().shouldDestroy()){
            var filter=attached(stand);if(filter!=null){filter.spawnAtLocation(level,filter.toStack());filter.discard();}
        }
    }
    private FilterConnections(){}
}
