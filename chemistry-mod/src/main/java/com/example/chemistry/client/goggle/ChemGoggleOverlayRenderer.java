package com.example.chemistry.client.goggle;

import java.util.ArrayList;
import java.util.List;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.api.goggles.IChemGoggleInfo;
import com.example.chemistry.item.ChemGogglesItem;
import com.example.chemistry.registry.ModItems;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Client HUD overlay modelled after Create's GoggleOverlayRenderer: when the
 * player wears the chemist's goggles and looks at a block entity implementing
 * {@link IChemGoggleInfo}, a fading panel with the container's live state is
 * drawn near the screen centre.
 */
public class ChemGoggleOverlayRenderer {

    public static final GuiLayer OVERLAY = ChemGoggleOverlayRenderer::renderOverlay;

    private static final float PANEL_SCALE = .75F;
    private static int stableTextWidth;
    private static int hoverTicks = 0;
    private static BlockPos lastHovered = null;

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "chem_goggle_info"),
                OVERLAY);
    }

    public static void renderOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return;
        }
        HitResult hit = mc.hitResult;
        if (hit == null || mc.level == null || mc.player == null) {
            reset();
            return;
        }
        if (!ChemGogglesItem.isWearing(mc.player)) {
            reset();
            return;
        }
        IChemGoggleInfo info = null;
        BlockPos hovered = null;
        if (hit instanceof BlockHitResult blockHit) {
            BlockEntity be = mc.level.getBlockEntity(blockHit.getBlockPos());
            if (be == null) be = com.example.chemistry.block.GasApplianceBlock.device(mc.level, blockHit.getBlockPos());
            if (be instanceof IChemGoggleInfo g) {
                info = g;
                hovered = blockHit.getBlockPos();
            }
        } else if (hit instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof IChemGoggleInfo g) {
            info = g;
            hovered = entityHit.getEntity().blockPosition();
        }
        if (info == null) {
            reset();
            return;
        }
        List<Component> tooltip = new ArrayList<>();
        if (!info.addGoggleInfo(tooltip, mc.player.isShiftKeyDown()) || tooltip.isEmpty()) {
            reset();
            return;
        }
        if (!hovered.equals(lastHovered)) {
            hoverTicks = 0;
            stableTextWidth = 0;
        }
        lastHovered = hovered;
        hoverTicks++;
        drawPanel(guiGraphics, deltaTracker, tooltip);
    }

    private static void reset() {
        hoverTicks = 0;
        lastHovered = null;
        stableTextWidth = 0;
    }

    private static void drawPanel(GuiGraphics guiGraphics, DeltaTracker deltaTracker, List<Component> tooltip) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        int textWidth = 0;
        for (Component line : tooltip) {
            textWidth = Math.max(textWidth, font.width(line));
        }
        stableTextWidth = Math.max(stableTextWidth, textWidth);
        textWidth = stableTextWidth;
        int textHeight = 8;
        if (tooltip.size() > 1) {
            textHeight += 2;
            textHeight += (tooltip.size() - 1) * 10;
        }

        int width = (int)(guiGraphics.guiWidth() / PANEL_SCALE);
        int height = (int)(guiGraphics.guiHeight() / PANEL_SCALE);
        int posX = width / 2 + 4;
        int posY = height / 2 - 12;
        posX = Math.min(posX, width - textWidth - 24);
        posY = Math.min(posY, height - textHeight - 24);

        float fade = Mth.clamp((hoverTicks + deltaTracker.getGameTimeDeltaPartialTick(false)) / 24f, 0, 1);
        if (fade <= 0) {
            return;
        }
        if (fade < 1) {
            posX -= (int) (Math.pow(1 - fade, 3) * 8);
        }

        int boxX = posX - 4;
        int boxY = posY - 4;
        int boxW = textWidth + 8;
        int boxH = textHeight + 8;

        int bg = ((int) (0xF0 * fade) << 24) | 0x101010;
        int borderTop = ((int) (0x50 * fade) << 24) | 0x0000FF;
        int borderBot = ((int) (0x50 * fade) << 24) | 0x28007F;

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(PANEL_SCALE, PANEL_SCALE);
        guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, bg);
        guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + 1, borderTop);
        guiGraphics.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, borderBot);
        guiGraphics.fill(boxX, boxY, boxX + 1, boxY + boxH, borderTop);
        guiGraphics.fill(boxX + boxW - 1, boxY, boxX + boxW, boxY + boxH, borderBot);

        guiGraphics.renderItem(new ItemStack(ModItems.CHEM_GOGGLES.get()), boxX + 2, boxY - 18);

        for (int i = 0; i < tooltip.size(); i++) {
            int color = i == 0 ? 0xFFFFFFFF : 0xFFD0D0D0;
            guiGraphics.drawString(font, tooltip.get(i), posX, posY + i * 10, color, true);
        }
        guiGraphics.pose().popMatrix();
    }
}
