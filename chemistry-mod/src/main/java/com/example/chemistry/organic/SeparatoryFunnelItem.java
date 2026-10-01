package com.example.chemistry.organic;
import com.example.chemistry.PhaseSystem;
import com.example.chemistry.LabInteractions;
import com.example.chemistry.item.LabVesselItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
public final class SeparatoryFunnelItem extends LabVesselItem {
    private static CustomData defaults(){var tag=new CompoundTag();tag.putBoolean("separatory_capped",true);tag.putBoolean("chem_sealed",true);return CustomData.of(tag);}
    public SeparatoryFunnelItem(Properties p){super(p.stacksTo(1).component(DataComponents.CUSTOM_DATA,defaults()),250);}
    @Override public InteractionResult useOn(UseOnContext c){if(c.getPlayer()!=null&&!c.getLevel().isClientSide())c.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.literal("手持分液漏斗右键空铁架台安装"),true);return InteractionResult.SUCCESS;}
    @Override public InteractionResult use(Level level,Player p,InteractionHand hand){
        var other=hand==InteractionHand.MAIN_HAND?p.getOffhandItem():p.getMainHandItem();
        if(LabInteractions.isTransferTool(other))return InteractionResult.SUCCESS;
        if(!level.isClientSide()&&p.isShiftKeyDown()&&other.isEmpty()){
            var s=p.getItemInHand(hand);var tag=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            if(tag.getBooleanOr("separatory_capped",true)&&!tag.getBooleanOr("separatory_open",false)){
                PhaseSystem.dissolveAndCrystallize(s,true);LabVesselItem.updateTint(s);p.displayClientMessage(net.minecraft.network.chat.Component.literal("已摇匀；静置分层后安装并排液"),true);
            }else p.displayClientMessage(net.minecraft.network.chat.Component.literal("请先塞好玻璃塞并关闭旋塞"),true);
        }
        return InteractionResult.SUCCESS;
    }
}
