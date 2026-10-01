package com.example.chemistry.organic;
import com.example.chemistry.ChemistryMod;
import com.example.chemistry.entity.IronStandEntity;
import com.example.chemistry.registry.ModEntities;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
@EventBusSubscriber(modid=ChemistryMod.MODID)
public final class SeparatoryConnections {
    public static SeparatoryFunnelEntity attached(IronStandEntity stand){return stand.level().getEntitiesOfClass(SeparatoryFunnelEntity.class,stand.getBoundingBox().inflate(3)).stream().filter(b->b.ownerId().equals(stand.getUUID().toString())).findFirst().orElse(null);}
    public static InteractionResult interact(IronStandEntity stand,Player player,InteractionHand hand){
        var existing=attached(stand);var held=player.getItemInHand(hand);
        if(existing!=null){
            var eye=player.getEyePosition();if(existing.virtualHitbox().clip(eye,eye.add(player.getLookAngle().scale(6))).isPresent())return existing.interact(player,hand);
            if(!stand.level().isClientSide())player.displayClientMessage(net.minecraft.network.chat.Component.literal("请先取下分液装置再改变铁架台装配"),true);
            return InteractionResult.SUCCESS;
        }
        if(!(held.getItem() instanceof SeparatoryFunnelItem))return InteractionResult.PASS;
        if(!stand.level().isClientSide()){
            if(stand.hasTube()||stand.hasVessel()||stand.findMountedVessel()!=null||stand.findMountedLamp()!=null||stand.getAttachment()!=0
                    ||com.example.chemistry.filtration.FilterConnections.attached(stand)!=null||com.example.chemistry.titration.BuretteConnections.attached(stand)!=null){player.displayClientMessage(net.minecraft.network.chat.Component.literal("请使用空铁架台安装分液漏斗"),true);return InteractionResult.SUCCESS;}
            var b=new SeparatoryFunnelEntity(ModEntities.SEPARATORY_FUNNEL.get(),stand.level());b.unpack(held);b.owner(stand);stand.level().addFreshEntity(b);held.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
    @SubscribeEvent public static void removed(EntityLeaveLevelEvent event){
        if(event.getEntity() instanceof IronStandEntity s&&s.level() instanceof net.minecraft.server.level.ServerLevel level
                &&s.getRemovalReason()!=null&&s.getRemovalReason().shouldDestroy()){
            var b=attached(s);if(b!=null){b.close();b.spawnAtLocation(level,b.toStack());b.discard();}
        }
    }
    private SeparatoryConnections(){}
}
