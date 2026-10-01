package com.example.chemistry.utility;
import com.example.chemistry.ChemistryMod;
import com.example.chemistry.entity.AlcoholLampEntity;
import com.example.chemistry.item.SplintItem;
import com.example.chemistry.registry.ModItems;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
@EventBusSubscriber(modid=ChemistryMod.MODID)
public final class SplintLighting {
 @SubscribeEvent public static void entity(PlayerInteractEvent.EntityInteract e){if(!(e.getItemStack().getItem() instanceof SplintItem s)||s.isGlowing()||!(e.getTarget() instanceof AlcoholLampEntity lamp)||!lamp.isLit())return;e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);if(e.getLevel().isClientSide())return;var held=e.getItemStack();var lit=new ItemStack(ModItems.GLOWING_SPLINT.get());held.shrink(1);if(held.isEmpty())e.getEntity().setItemInHand(e.getHand(),lit);else if(!e.getEntity().getInventory().add(lit))e.getEntity().drop(lit,false);}
 @SubscribeEvent public static void block(PlayerInteractEvent.RightClickBlock e){if(!(e.getItemStack().getItem() instanceof SplintItem s)||s.isGlowing())return;var burner=com.example.chemistry.block.GasApplianceBlock.device(e.getLevel(),e.getPos());if(burner==null||!burner.burning())return;e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);if(e.getLevel().isClientSide())return;var held=e.getItemStack();var lit=new ItemStack(ModItems.GLOWING_SPLINT.get());held.shrink(1);if(held.isEmpty())e.getEntity().setItemInHand(e.getHand(),lit);else if(!e.getEntity().getInventory().add(lit))e.getEntity().drop(lit,false);}
 private SplintLighting(){}
}
