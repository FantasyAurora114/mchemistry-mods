package com.example.chemistry.electrical;
import com.example.chemistry.registry.ModEntities;
import com.example.chemistry.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.player.Player;
/** A separate ionic connection; it is never accepted as an electron wire. */
public class SaltBridgeItem extends Item {
    public SaltBridgeItem(Properties p){super(p.stacksTo(1));}
    public static double grams(ItemStack s){return Math.clamp(s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getDoubleOr("bridge_kno3_g",1),0,1);}
    public static ItemStack filled(double grams){var s=new ItemStack(ModItems.SALT_BRIDGE.get());var t=new CompoundTag();t.putDouble("bridge_kno3_g",Math.clamp(grams,0,1));s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));return s;}
    public static void click(ElectroDeviceEntity cell,Player p,ItemStack held){
        if(p.level().isClientSide())return;
        var t=held.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();String pending=t.getStringOr("bridge_first","");
        if(p.isShiftKeyDown()){t.remove("bridge_first");held.set(DataComponents.CUSTOM_DATA,CustomData.of(t));return;}
        net.minecraft.world.entity.Entity first=null;
        try {first=p.level().getEntity(java.util.UUID.fromString(pending));} catch(IllegalArgumentException ignored) {t.remove("bridge_first");}
        if(pending.isEmpty()||!(first instanceof ElectroDeviceEntity e)||!e.isHalfCell()){
            t.putString("bridge_first",cell.getUUID().toString());held.set(DataComponents.CUSTOM_DATA,CustomData.of(t));tell(p,"已固定盐桥第一端，点击另一半电池；潜行取消");return;
        }
        var other=(ElectroDeviceEntity)first;
        if(other==cell)return;
        if(other.position().distanceTo(cell.position())>3){tell(p,"盐桥最长3格");return;}
        if(GalvanicSystem.bridge(cell)!=null||GalvanicSystem.bridge(other)!=null){tell(p,"每只半电池只能接一条盐桥");return;}
        var bridge=new SaltBridgeEntity(ModEntities.SALT_BRIDGE.get(),p.level());bridge.connect(other,0,cell,0,false);bridge.setGrams(grams(held));p.level().addFreshEntity(bridge);
        t.remove("bridge_first");held.set(DataComponents.CUSTOM_DATA,CustomData.of(t));if(!p.isCreative())held.shrink(1);tell(p,"盐桥已连接：仅传递离子，不代替电线");
    }
    private static void tell(Player p,String s){p.displayClientMessage(net.minecraft.network.chat.Component.literal(s),true);}
}
