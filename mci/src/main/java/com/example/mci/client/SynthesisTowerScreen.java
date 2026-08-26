/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.example.chemistry.data.GasJars
 *  com.example.chemistry.data.GasJars$GasJar
 *  com.example.chemistry.data.Liquids
 *  com.example.chemistry.data.Liquids$Liquid
 *  com.example.chemistry.registry.ModFluids
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
 *  net.minecraft.client.input.MouseButtonEvent
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.level.material.Fluid
 */
package com.example.mci.client;

import com.example.mci.blockentity.SynthesisTowerBlockEntity;
import com.example.chemistry.data.GasJars;
import com.example.chemistry.data.Liquids;
import com.example.mci.menu.SynthesisTowerMenu;
import com.example.chemistry.registry.ModFluids;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.material.Fluid;

public class SynthesisTowerScreen
extends AbstractContainerScreen<SynthesisTowerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath((String)"mchemistry", (String)"textures/gui/synthesis_tower.png");
    private static final int[] CONFIG_X = new int[]{44, 62, 80, 98};
    private static final int CONFIG_Y = 9;
    private static final int CONFIG_W = 18;
    private static final int CONFIG_H = 12;
    private static final int RUN_X = 16;
    private static final int RUN_Y = 80;
    private static final int RUN_W = 48;
    private static final int RUN_H = 12;
    private static final int ADJUST_BTN_W = 12;
    private static final int ADJUST_BTN_H = 12;
    private static final int TEMP_Y = 50;
    private static final int PRESSURE_Y = 66;
    private static final int MINUS_X = 14;
    private static final int PLUS_X = 53;
    private static final int DISPLAY_X = 31;
    private static final int DISPLAY_W = 17;
    private static final int GAS_WELL_X = 128;
    private static final int LIQUID_WELL_X = 151;
    private static final int WELL_Y = 27;
    private static final int WELL_W = 11;
    private static final int WELL_H = 55;
    private static final int TANK_TEXT_X = 66;
    private static final int GAS_NAME_Y = 44;
    private static final int GAS_AMOUNT_Y = 52;
    private static final int LIQUID_NAME_Y = 62;
    private static final int LIQUID_AMOUNT_Y = 70;
    private int pressedButton = -1;
    private long pressedAt = 0L;
    private float displayedGasFill = 0.0f;
    private float displayedLiquidFill = 0.0f;

    public SynthesisTowerScreen(SynthesisTowerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
    }

    protected void init() {
        super.init();
        ++this.leftPos;
        ++this.topPos;
    }

    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0.0f, 1.0f, 0.0f, 1.0f);
        for (int i = 0; i < 4; ++i) {
            int type = ((SynthesisTowerMenu)this.menu).blockEntity().inputType(i);
            String label = switch (type) {
                case 0 -> "\u6c14";
                case 1 -> "\u6db2";
                default -> "\u56fa";
            };
            guiGraphics.drawString(this.font, label, this.leftPos + CONFIG_X[i] + 5, this.topPos + 9 + 2, -1);
        }
        this.drawAdjustButton(guiGraphics, 14, 50, "\u2212");
        this.drawAdjustButton(guiGraphics, 53, 50, "+");
        this.drawAdjustButton(guiGraphics, 14, 66, "\u2212");
        this.drawAdjustButton(guiGraphics, 53, 66, "+");
        String tempText = this.temperature() + "\u2103";
        String pressureText = String.valueOf(this.pressure());
        guiGraphics.drawString(this.font, tempText, this.leftPos + 31 + (17 - tempText.length() * 8) / 2, this.topPos + 50 + 2, -1);
        guiGraphics.drawString(this.font, pressureText, this.leftPos + 31 + (17 - pressureText.length() * 8) / 2, this.topPos + 66 + 2, -1);
        guiGraphics.drawString(this.font, "\u542f\u52a8", this.leftPos + 16 + 16, this.topPos + 80 + 2, -1);
        this.displayedGasFill += (this.gasFill() - this.displayedGasFill) * 0.18f;
        this.displayedLiquidFill += (this.liquidFill() - this.displayedLiquidFill) * 0.18f;
        this.drawGauge(guiGraphics, 128, 27, this.gasColor(), this.displayedGasFill);
        this.drawGauge(guiGraphics, 151, 27, this.liquidColor(), this.displayedLiquidFill);
        guiGraphics.drawString(this.font, "\u6c14\u4f53\u7f50 " + this.gasNameOrEmpty(), this.leftPos + 66, this.topPos + 44, -13619152);
        guiGraphics.drawString(this.font, this.gasAmount() + "/64000 mB", this.leftPos + 66, this.topPos + 52, -11513776);
        guiGraphics.drawString(this.font, "\u6db2\u4f53\u7f50 " + this.liquidNameOrEmpty(), this.leftPos + 66, this.topPos + 62, -13619152);
        guiGraphics.drawString(this.font, this.liquidAmount() + "/64000 mB", this.leftPos + 66, this.topPos + 70, -11513776);
        this.drawButtonFeedback(guiGraphics, mouseX, mouseY);
    }

    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 10, -12566464);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 104, -12566464);
    }

    private void drawAdjustButton(GuiGraphics guiGraphics, int x, int y, String label) {
        guiGraphics.drawString(this.font, label, this.leftPos + x + 2, this.topPos + y + 2, -1);
    }

    private List<ButtonRect> buttonRects() {
        ArrayList<ButtonRect> rects = new ArrayList<ButtonRect>();
        for (int i = 0; i < 4; ++i) {
            rects.add(new ButtonRect(101 + i, CONFIG_X[i], 9, 18, 12));
        }
        rects.add(new ButtonRect(100, 16, 80, 48, 12));
        rects.add(new ButtonRect(105, 14, 50, 12, 12));
        rects.add(new ButtonRect(106, 53, 50, 12, 12));
        rects.add(new ButtonRect(107, 14, 66, 12, 12));
        rects.add(new ButtonRect(108, 53, 66, 12, 12));
        return rects;
    }

    private void drawButtonFeedback(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (ButtonRect rect : this.buttonRects()) {
            boolean pressed;
            boolean hovered = this.inRect(mouseX, mouseY, rect.x(), rect.y(), rect.w(), rect.h());
            boolean bl = pressed = rect.id() == this.pressedButton && System.currentTimeMillis() - this.pressedAt < 180L;
            if (!hovered && !pressed) continue;
            int x = this.leftPos + rect.x();
            int y = this.topPos + rect.y();
            int color = pressed ? Integer.MIN_VALUE : -855638017;
            guiGraphics.fill(x, y, x + rect.w(), y + 1, color);
            guiGraphics.fill(x, y + rect.h() - 1, x + rect.w(), y + rect.h(), color);
            guiGraphics.fill(x, y, x + 1, y + rect.h(), color);
            guiGraphics.fill(x + rect.w() - 1, y, x + rect.w(), y + rect.h(), color);
        }
    }

    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.inRect(mouseX, mouseY, 128, 27, 11, 55)) {
            guiGraphics.setComponentTooltipForNextFrame(this.font, this.gasTooltip(), mouseX, mouseY);
        } else if (this.inRect(mouseX, mouseY, 151, 27, 11, 55)) {
            guiGraphics.setComponentTooltipForNextFrame(this.font, this.liquidTooltip(), mouseX, mouseY);
        } else if (this.inRect(mouseX, mouseY, 31, 50, 17, 12) || this.inRect(mouseX, mouseY, 14, 50, 12, 12) || this.inRect(mouseX, mouseY, 53, 50, 12, 12)) {
            guiGraphics.setComponentTooltipForNextFrame(this.font, this.temperatureTooltip(), mouseX, mouseY);
        } else if (this.inRect(mouseX, mouseY, 31, 66, 17, 12) || this.inRect(mouseX, mouseY, 14, 66, 12, 12) || this.inRect(mouseX, mouseY, 53, 66, 12, 12)) {
            guiGraphics.setComponentTooltipForNextFrame(this.font, this.pressureTooltip(), mouseX, mouseY);
        } else {
            super.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    private void drawGauge(GuiGraphics guiGraphics, int x, int y, int color, float fill) {
        int left = this.leftPos + x;
        int top = this.topPos + y;
        int fillHeight = (int)(55.0f * Math.max(0.0f, Math.min(1.0f, fill)));
        if (fillHeight > 0) {
            guiGraphics.fill(left + 1, top + 55 - fillHeight, left + 11 - 1, top + 55 - 1, 0xFF000000 | color & 0xFFFFFF);
        }
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() == 0) {
            for (int i = 0; i < 4; ++i) {
                if (!this.inRect(event.x(), event.y(), CONFIG_X[i], 9, 18, 12)) continue;
                this.markPressed(101 + i);
                this.minecraft.gameMode.handleInventoryButtonClick(((SynthesisTowerMenu)this.menu).containerId, 101 + i);
                return true;
            }
            if (this.inRect(event.x(), event.y(), 16, 80, 48, 12)) {
                this.markPressed(100);
                this.minecraft.gameMode.handleInventoryButtonClick(((SynthesisTowerMenu)this.menu).containerId, 100);
                return true;
            }
            if (this.inRect(event.x(), event.y(), 14, 50, 12, 12)) {
                this.markPressed(105);
                this.minecraft.gameMode.handleInventoryButtonClick(((SynthesisTowerMenu)this.menu).containerId, 105);
                return true;
            }
            if (this.inRect(event.x(), event.y(), 53, 50, 12, 12)) {
                this.markPressed(106);
                this.minecraft.gameMode.handleInventoryButtonClick(((SynthesisTowerMenu)this.menu).containerId, 106);
                return true;
            }
            if (this.inRect(event.x(), event.y(), 14, 66, 12, 12)) {
                this.markPressed(107);
                this.minecraft.gameMode.handleInventoryButtonClick(((SynthesisTowerMenu)this.menu).containerId, 107);
                return true;
            }
            if (this.inRect(event.x(), event.y(), 53, 66, 12, 12)) {
                this.markPressed(108);
                this.minecraft.gameMode.handleInventoryButtonClick(((SynthesisTowerMenu)this.menu).containerId, 108);
                return true;
            }
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    private boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= (double)(this.leftPos + x) && mx <= (double)(this.leftPos + x + w) && my >= (double)(this.topPos + y) && my <= (double)(this.topPos + y + h);
    }

    private void markPressed(int buttonId) {
        this.pressedButton = buttonId;
        this.pressedAt = System.currentTimeMillis();
    }

    private double temperature() {
        SynthesisTowerBlockEntity be = ((SynthesisTowerMenu)this.menu).blockEntity();
        return be.hasClientSync() ? be.syncedTemperature() : be.getTemperature();
    }

    private double pressure() {
        SynthesisTowerBlockEntity be = ((SynthesisTowerMenu)this.menu).blockEntity();
        return be.hasClientSync() ? be.syncedPressure() : be.getPressure();
    }

    private float gasFill() {
        return (float)this.gasAmount() / 64000.0f;
    }

    private float liquidFill() {
        return (float)this.liquidAmount() / 64000.0f;
    }

    private long gasAmount() {
        SynthesisTowerBlockEntity be = ((SynthesisTowerMenu)this.menu).blockEntity();
        return be.hasClientSync() ? be.syncedGasAmount() : be.getGasTank().getAmount();
    }

    private String gasId() {
        SynthesisTowerBlockEntity be = ((SynthesisTowerMenu)this.menu).blockEntity();
        String id = be.hasClientSync() ? be.syncedGasId() : be.getGasTank().getGasId();
        return id == null ? "" : id;
    }

    private int liquidAmount() {
        SynthesisTowerBlockEntity be = ((SynthesisTowerMenu)this.menu).blockEntity();
        return be.hasClientSync() ? be.syncedFluidAmount() : be.getFluidTank().getAmount();
    }

    private String liquidId() {
        SynthesisTowerBlockEntity be = ((SynthesisTowerMenu)this.menu).blockEntity();
        String id = be.hasClientSync() ? be.syncedFluidId() : ModFluids.liquidIdFor((Fluid)be.getFluidTank().getFluid());
        return id == null ? "" : id;
    }

    private int gasColor() {
        for (GasJars.GasJar gas : GasJars.ALL) {
            if (!gas.id().equals(this.gasId())) continue;
            return gas.color();
        }
        return 0xFFFFFF;
    }

    private int liquidColor() {
        for (Liquids.Liquid liquid : Liquids.ALL) {
            if (!liquid.id().equals(this.liquidId())) continue;
            return liquid.color();
        }
        return 0xFFFFFF;
    }

    private String gasNameOrEmpty() {
        String id = this.gasId();
        return id.isEmpty() ? "\uff08\u7a7a\uff09" : SynthesisTowerScreen.gasName(id);
    }

    private String liquidNameOrEmpty() {
        String id = this.liquidId();
        return id.isEmpty() ? "\uff08\u7a7a\uff09" : SynthesisTowerScreen.liquidName(id);
    }

    private List<Component> gasTooltip() {
        ArrayList<Component> lines = new ArrayList<Component>();
        String id = this.gasId();
        String name = id.isEmpty() ? "\u7a7a" : SynthesisTowerScreen.gasName(id);
        double purity = ((SynthesisTowerMenu)this.menu).blockEntity().hasClientSync() ? ((SynthesisTowerMenu)this.menu).blockEntity().syncedGasPurity() : ((SynthesisTowerMenu)this.menu).blockEntity().getGasTank().getPurity();
        lines.add((Component)Component.literal((String)("\u6c14\u4f53\u7f50\uff1a" + name)));
        lines.add((Component)Component.literal((String)(this.gasAmount() + " / 64000 mB")));
        lines.add((Component)Component.literal((String)("\u7eaf\u5ea6 " + String.format("%.4f%%", purity * 100.0))));
        return lines;
    }

    private List<Component> liquidTooltip() {
        ArrayList<Component> lines = new ArrayList<Component>();
        String id = this.liquidId();
        String name = id.isEmpty() ? "\u7a7a" : SynthesisTowerScreen.liquidName(id);
        double purity = ((SynthesisTowerMenu)this.menu).blockEntity().hasClientSync() ? ((SynthesisTowerMenu)this.menu).blockEntity().syncedFluidPurity() : ((SynthesisTowerMenu)this.menu).blockEntity().getFluidTank().getPurity();
        lines.add((Component)Component.literal((String)("\u6db2\u4f53\u7f50\uff1a" + name)));
        lines.add((Component)Component.literal((String)(this.liquidAmount() + " / 64000 mB")));
        lines.add((Component)Component.literal((String)("\u7eaf\u5ea6 " + String.format("%.4f%%", purity * 100.0))));
        return lines;
    }

    private List<Component> temperatureTooltip() {
        ArrayList<Component> lines = new ArrayList<Component>();
        lines.add((Component)Component.literal((String)("\u6e29\u5ea6\uff1a" + String.format("%.0f\u2103", this.temperature()))));
        lines.add((Component)Component.literal((String)"\u2212 / + \u8c03\u8282\uff08\u00b110\u2103\uff09\uff0cShift \u5feb\u901f\u8c03\u8282\uff08\u00b1100\u2103\uff09"));
        lines.add((Component)Component.literal((String)"\u9ad8\u6e29\u53cd\u5e94\u9700\u8fbe\u5230\u5bf9\u5e94\u6e29\u5ea6"));
        return lines;
    }

    private List<Component> pressureTooltip() {
        ArrayList<Component> lines = new ArrayList<Component>();
        lines.add((Component)Component.literal((String)("\u538b\u529b\uff1a" + String.format("%.0f kPa", this.pressure()))));
        lines.add((Component)Component.literal((String)"\u2212 / + \u8c03\u8282\uff08\u00b11000 kPa\uff09\uff0cShift \u5feb\u901f\u8c03\u8282\uff08\u00b110000 kPa\uff09"));
        lines.add((Component)Component.literal((String)"\u5408\u6210\u6c28\u7b49\u53cd\u5e94\u9700\u8981\u9ad8\u538b"));
        return lines;
    }

    private static String gasName(String id) {
        for (GasJars.GasJar gas : GasJars.ALL) {
            if (!gas.id().equals(id)) continue;
            return gas.chinese();
        }
        return id;
    }

    private static String liquidName(String id) {
        for (Liquids.Liquid liquid : Liquids.ALL) {
            if (!liquid.id().equals(id)) continue;
            return liquid.chinese();
        }
        return id;
    }

    private record ButtonRect(int id, int x, int y, int w, int h) {
    }
}
