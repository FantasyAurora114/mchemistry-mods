package com.example.chemistry.client;
import com.example.chemistry.menu.ReagentCabinetMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public class ReagentCabinetScreen extends AbstractContainerScreen<ReagentCabinetMenu> {
    public ReagentCabinetScreen(ReagentCabinetMenu menu,Inventory inv,Component title){
        super(menu,inv,title);imageHeight=menu.rows*18+114;inventoryLabelY=menu.rows*18+21;
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        int x=leftPos,y=topPos;
        // Vanilla container bevel: light top/left, dark bottom/right and inset inventory slots.
        g.fill(x,y,x+imageWidth,y+imageHeight,0xff373737);
        g.fill(x+1,y+1,x+imageWidth-1,y+imageHeight-1,0xff8b8b8b);
        g.fill(x+2,y+2,x+imageWidth-2,y+imageHeight-2,0xffc6c6c6);
        g.fill(x+2,y+2,x+imageWidth-3,y+3,0xffffffff);
        g.fill(x+2,y+3,x+3,y+imageHeight-3,0xffffffff);
        g.fill(x+3,y+imageHeight-3,x+imageWidth-2,y+imageHeight-2,0xff555555);
        g.fill(x+imageWidth-3,y+3,x+imageWidth-2,y+imageHeight-3,0xff555555);
        for(var slot:menu.slots){int sx=x+slot.x-1,sy=y+slot.y-1;
            g.fill(sx,sy,sx+18,sy+18,0xffffffff);
            g.fill(sx,sy,sx+17,sy+17,0xff373737);
            g.fill(sx+1,sy+1,sx+17,sy+17,0xff8b8b8b);
        }
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
