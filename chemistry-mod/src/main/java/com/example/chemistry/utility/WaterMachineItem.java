package com.example.chemistry.utility;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModEntities;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;
public class WaterMachineItem extends LabVesselItem {
    public WaterMachineItem(Properties p){super(p.stacksTo(1),1000);}
    @Override public InteractionResult useOn(UseOnContext c){var pos=c.getClickedPos().relative(c.getClickedFace());var l=c.getLevel();if(!l.noCollision(new AABB(pos.getX()+.1,pos.getY(),pos.getZ()+.1,pos.getX()+.9,pos.getY()+.9,pos.getZ()+.9)))return InteractionResult.FAIL;if(l.isClientSide())return InteractionResult.SUCCESS;var e=new WaterMachineEntity(ModEntities.WATER_MACHINE.get(),l);e.setStack(c.getItemInHand().copyWithCount(1));e.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);if(c.getPlayer()!=null)e.setYRot(c.getPlayer().getDirection().toYRot());l.addFreshEntity(e);if(c.getPlayer()==null||!c.getPlayer().isCreative())c.getItemInHand().shrink(1);return InteractionResult.SUCCESS;}
}
