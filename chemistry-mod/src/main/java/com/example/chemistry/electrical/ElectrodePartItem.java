package com.example.chemistry.electrical;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;

/** A replaceable mesh carries its remaining metal and deposited coating with it. */
public final class ElectrodePartItem extends Item {
    public ElectrodePartItem(Properties p) { super(p.stacksTo(1)); }
    public static boolean clip(ItemStack s) { return s.getItem() instanceof ElectrodePartItem && id(s).startsWith("alligator_clip_"); }
    public static boolean mesh(ItemStack s) { return s.getItem() instanceof ElectrodePartItem && id(s).endsWith("_electrode_mesh"); }
    private static String id(ItemStack s) { return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath(); }
    public static String material(ItemStack s) { return mesh(s)?id(s).replace("_electrode_mesh",""):""; }
    public static CompoundTag data(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag(); }
    public static double grams(ItemStack s) { return mesh(s)?data(s).getDoubleOr("electrode_g",10):0; }
    public static void grams(ItemStack s,double g) { var t=data(s);t.putDouble("electrode_g",Math.max(0,g));s.set(DataComponents.CUSTOM_DATA,CustomData.of(t)); }
    public static double coating(ItemStack s,String metal) { return data(s).getDoubleOr("coating_"+metal,0); }
    public static void addCoating(ItemStack s,String metal,double g) { var t=data(s);t.putDouble("coating_"+metal,coating(s,metal)+g);s.set(DataComponents.CUSTOM_DATA,CustomData.of(t)); }
    public static ItemStack meshIn(Level l,ItemStack clip) { var t=data(clip);var encoded=t.get("electrode_mesh");return encoded==null?ItemStack.EMPTY:ItemStack.CODEC.parse(l.registryAccess().createSerializationContext(NbtOps.INSTANCE),encoded).getOrThrow(); }
    public static void meshIn(Level l,ItemStack clip,ItemStack mesh) {
        var t=data(clip);if(mesh.isEmpty())t.remove("electrode_mesh");else t.put("electrode_mesh",ItemStack.CODEC.encodeStart(l.registryAccess().createSerializationContext(NbtOps.INSTANCE),mesh).getOrThrow());
        clip.set(DataComponents.CUSTOM_DATA,CustomData.of(t));clip.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(java.util.List.of(),java.util.List.of(),mesh.isEmpty()?java.util.List.of():java.util.List.of(material(mesh)),java.util.List.of()));
    }
    @Override public InteractionResult use(Level l,Player p,InteractionHand h) {
        var held=p.getItemInHand(h);var other=h==InteractionHand.MAIN_HAND?p.getOffhandItem():p.getMainHandItem();
        if(!clip(held))return InteractionResult.PASS;
        if(l.isClientSide())return InteractionResult.SUCCESS;
        var old=meshIn(l,held);
        if(p.isShiftKeyDown()&&other.isEmpty()&&!old.isEmpty()) { meshIn(l,held,ItemStack.EMPTY);if(!p.getInventory().add(old))p.drop(old,false); }
        else if(mesh(other)&&old.isEmpty()) { meshIn(l,held,other.copyWithCount(1));other.shrink(1); }
        return InteractionResult.SUCCESS;
    }
}
