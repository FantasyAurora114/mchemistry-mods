package com.example.chemistry.electrical;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModEntities;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;
public class ElectroDeviceItem extends LabVesselItem {
    public ElectroDeviceItem(Properties p,int capacity){super(p.stacksTo(1),capacity);}
    @Override public InteractionResult useOn(UseOnContext c){var player=c.getPlayer();if(player==null)return InteractionResult.PASS;
        var pos=c.getClickedPos().relative(c.getClickedFace());var level=c.getLevel();double scale=capacity()==1000?1:ElectroDeviceEntity.MODEL_SCALE,height=capacity()==1000?1.5:(capacity()>0?1.74:.85)*scale;
        boolean smallDevice=c.getItemInHand().is(com.example.chemistry.registry.ModItems.GALVANIC_HALF_CELL.get())||c.getItemInHand().is(com.example.chemistry.registry.ModItems.LAB_RESISTOR.get())||c.getItemInHand().is(com.example.chemistry.registry.ModItems.LAB_VOLTMETER.get());if(smallDevice)height=.75;
        AABB bounds=new AABB(pos.getX()+.5-.5*scale,pos.getY(),pos.getZ()+.5-.5*scale,pos.getX()+.5+.5*scale,pos.getY()+height,pos.getZ()+.5+.5*scale);
        if(!level.noCollision(bounds)||!level.getEntitiesOfClass(ElectroDeviceEntity.class,bounds).isEmpty())return InteractionResult.FAIL;
        if(level.isClientSide())return InteractionResult.SUCCESS;
        var device=new ElectroDeviceEntity(ModEntities.ELECTRO_DEVICE.get(),level);device.setStack(c.getItemInHand().copyWithCount(1));device.setYRot(player.getDirection().toYRot());device.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);if(capacity()==1000){level.setBlock(pos,com.example.chemistry.registry.ModBlocks.DEEP_WATER_TROUGH.get().defaultBlockState(),3);device.flag("host_block",true);}
        level.addFreshEntity(device);
        if(!player.isCreative())c.getItemInHand().shrink(1);return InteractionResult.SUCCESS;
    }
}
