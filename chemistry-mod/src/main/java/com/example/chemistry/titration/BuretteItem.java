package com.example.chemistry.titration;
import com.example.chemistry.item.LabVesselItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
public final class BuretteItem extends LabVesselItem {
    public static final int CAPACITY=50;
    public BuretteItem(Properties p){super(p.stacksTo(1),CAPACITY);}
    @Override public InteractionResult useOn(UseOnContext c){
        if(c.getPlayer()!=null&&!c.getLevel().isClientSide())c.getPlayer().displayClientMessage(
                net.minecraft.network.chat.Component.literal("右键空铁架台安装滴定管；装接收瓶后控制旋塞滴加"),true);
        return InteractionResult.SUCCESS;
    }
}
