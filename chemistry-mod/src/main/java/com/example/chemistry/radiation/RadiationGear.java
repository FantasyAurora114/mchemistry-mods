package com.example.chemistry.radiation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.resources.*;
public final class RadiationGear {
 private static final ArmorMaterial MATERIAL=new ArmorMaterial(ArmorMaterials.LEATHER.durability(),ArmorMaterials.LEATHER.defense(),ArmorMaterials.LEATHER.enchantmentValue(),ArmorMaterials.LEATHER.equipSound(),0,0,ArmorMaterials.LEATHER.repairIngredient(),ResourceKey.create(EquipmentAssets.ROOT_ID,ResourceLocation.fromNamespaceAndPath("mchemistry","radiation_suit")));
 public static Item.Properties properties(Item.Properties properties,ArmorType type){
  var builder=ItemAttributeModifiers.builder();for(var e:MATERIAL.createAttributes(type).modifiers())builder.add(e.attribute(),e.modifier(),e.slot());
  builder.add(Attributes.MOVEMENT_SPEED,new AttributeModifier(ResourceLocation.fromNamespaceAndPath("mchemistry","radiation_suit_"+type.getName()),-.02,AttributeModifier.Operation.ADD_MULTIPLIED_BASE),EquipmentSlotGroup.bySlot(type.getSlot()));
  return properties.humanoidArmor(MATERIAL,type).attributes(builder.build());
 }
 public static boolean isSuit(net.minecraft.world.item.ItemStack s){return com.example.chemistry.transfer.BottleCodes.pathOf(s).startsWith("radiation_")&&s.has(net.minecraft.core.component.DataComponents.EQUIPPABLE);}
 public static int pieces(net.minecraft.world.entity.LivingEntity e){int n=0;for(var slot:java.util.List.of(net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET))if(isSuit(e.getItemBySlot(slot)))n++;return n;}
 private RadiationGear(){}
}
