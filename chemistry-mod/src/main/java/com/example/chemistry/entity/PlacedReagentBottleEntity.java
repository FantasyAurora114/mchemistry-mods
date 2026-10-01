package com.example.chemistry.entity;

import com.example.chemistry.registry.ModEntities;
import net.minecraft.network.syncher.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.minecraft.core.Direction;

/** Free placement at the clicked point, with continuous player yaw and per-bottle hit boxes. */
public class PlacedReagentBottleEntity extends TechnicalEntity implements com.example.chemistry.api.goggles.IChemGoggleInfo {
    private static final EntityDataAccessor<ItemStack> STACK=SynchedEntityData.defineId(PlacedReagentBottleEntity.class,EntityDataSerializers.ITEM_STACK);
    public static final float WIDTH=.23F,HEIGHT=.40F;
    public PlacedReagentBottleEntity(EntityType<?> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(STACK,ItemStack.EMPTY);}
    public void setStack(ItemStack s){var copy=s.copyWithCount(1);com.example.chemistry.transfer.BottleCodes.refreshModel(copy);entityData.set(STACK,copy);}
    @Override public ItemStack toStack(){return entityData.get(STACK).copy();}
    @Override public AABB virtualHitbox(){return new AABB(getX()-WIDTH/2,getY(),getZ()-WIDTH/2,getX()+WIDTH/2,getY()+HEIGHT,getZ()+WIDTH/2);}
    @Override public void tick(){super.tick();setBoundingBox(virtualHitbox());
        if(!level().isClientSide()&&tickCount%20==0&&level().getBlockState(net.minecraft.core.BlockPos.containing(getX(),getY()-.02,getZ())).isAir()){
            if(level() instanceof net.minecraft.server.level.ServerLevel server)spawnAtLocation(server,toStack());discard();
        }
    }
    @Override public InteractionResult interact(Player p,InteractionHand hand){if(!level().isClientSide()&&p.getItemInHand(hand).isEmpty())pickUp(p);return InteractionResult.SUCCESS;}
    @Override protected void addAdditionalSaveData(ValueOutput out){out.store("bottle",ItemStack.OPTIONAL_CODEC,toStack());}
    @Override protected void readAdditionalSaveData(ValueInput in){setStack(in.read("bottle",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));}
    @Override public boolean addGoggleInfo(java.util.List<net.minecraft.network.chat.Component> lines,boolean sneaking){
        var s=toStack();lines.add(s.getHoverName());
        if(com.example.chemistry.transfer.BottleCodes.isSolidJar(s))lines.add(net.minecraft.network.chat.Component.literal(String.format(java.util.Locale.ROOT,"余量 %.2f / 100 g",com.example.chemistry.transfer.BottleCodes.solidGrams(s))));
        else lines.add(net.minecraft.network.chat.Component.literal("余量 "+com.example.chemistry.transfer.BottleCodes.volumeOf(s)+" / "+com.example.chemistry.transfer.BottleCodes.bottleCapacityOf(s)+" mL"));return true;
    }
    public static InteractionResult place(UseOnContext c){
        if(c.getClickedFace()!=Direction.UP||c.getPlayer()==null)return InteractionResult.PASS;
        var pos=c.getClickLocation();var level=c.getLevel();var e=new PlacedReagentBottleEntity(ModEntities.PLACED_REAGENT_BOTTLE.get(),level);
        e.setPos(pos);e.setYRot(c.getPlayer().getYRot());e.setStack(c.getItemInHand());
        if(!level.noCollision(e,e.virtualHitbox().deflate(.002))||!level.getEntities(e,e.virtualHitbox(),x->x instanceof TechnicalEntity).isEmpty())return InteractionResult.FAIL;
        if(!level.isClientSide()){level.addFreshEntity(e);if(!c.getPlayer().isCreative())c.getItemInHand().shrink(1);}
        return InteractionResult.SUCCESS;
    }
}
