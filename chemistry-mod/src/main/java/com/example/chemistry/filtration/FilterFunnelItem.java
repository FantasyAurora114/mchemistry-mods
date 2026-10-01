package com.example.chemistry.filtration;
import com.example.chemistry.item.LabVesselItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
public class FilterFunnelItem extends LabVesselItem {
    public FilterFunnelItem(Properties p){super(p.stacksTo(1),100);}
    @Override public InteractionResult useOn(UseOnContext c){if(c.getPlayer()!=null&&!c.getLevel().isClientSide())c.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.literal("拿过滤漏斗右键空铁架台安装"),true);return InteractionResult.SUCCESS;}
}
