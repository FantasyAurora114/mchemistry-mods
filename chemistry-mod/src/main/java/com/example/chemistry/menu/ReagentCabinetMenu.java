package com.example.chemistry.menu;

import com.example.chemistry.item.*;
import com.example.chemistry.registry.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class ReagentCabinetMenu extends AbstractContainerMenu {
    private final Container cabinet;
    public final int rows;
    public ReagentCabinetMenu(int id, Inventory inv, int rows){this(id,inv,new SimpleContainer(rows*3),rows);}
    public ReagentCabinetMenu(int id, Inventory inv, Container cabinet, int rows){
        super(rows==5?ModMenus.TALL_CABINET.get():ModMenus.BASE_CABINET.get(),id);
        checkContainerSize(cabinet,rows*3);this.cabinet=cabinet;this.rows=rows;
        for(int y=0;y<rows;y++)for(int x=0;x<3;x++)addSlot(new Slot(cabinet,y*3+x,62+x*18,18+y*18){
            @Override public boolean mayPlace(ItemStack s){return accepts(s);}
            @Override public int getMaxStackSize(){return 1;}
        });
        int top=rows*18+32;
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+y*9+x,8+x*18,top+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inv,x,8+x*18,top+58));
    }
    public static boolean accepts(ItemStack stack){
        var item=stack.getItem();
        if(item instanceof LiquidBottleItem || item instanceof SolidBottleItem || item instanceof DropperBottleItem || item instanceof GraduatedCylinderItem) return true;
        if(stack.is(ModItems.ERLENMEYER_FLASK.get()) || stack.is(ModItems.GROUND_GLASS_ERLENMEYER.get()))return true;
        var id=BuiltInRegistries.ITEM.getKey(item);
        return item instanceof LabVesselItem && id.getNamespace().equals("mchemistry") && id.getPath().startsWith("beaker_");
    }
    @Override public boolean stillValid(Player p){return cabinet.stillValid(p);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();int size=rows*3;
        if(index<size){if(!moveItemStackTo(stack,size,slots.size(),true))return ItemStack.EMPTY;}
        else {if(!accepts(stack)||!moveItemStackTo(stack,0,size,false))return ItemStack.EMPTY;}
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        slot.onTake(player,stack);return copy;
    }
}
