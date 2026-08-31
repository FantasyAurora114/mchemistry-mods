package com.example.chemistry.client.handbook;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.data.ChemicalInfoProvider;
import com.example.chemistry.data.Reactions;
import com.example.chemistry.network.ChemistryNetworking;
import com.example.chemistry.registry.ModItems;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 化学手册 GUI（自定义路线）：封面 → 分类列表 → 物质详情 / 反应页 / 指南。
 * 背景与图标材质在 textures/gui/handbook/ 下，文字与布局由代码绘制。
 */
public class ChemistryHandbookScreen extends Screen {

    private static final int GUI_W = 256;
    private static final int GUI_H = 200;

    private static final ResourceLocation COVER =
            ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "textures/gui/handbook/cover.png");
    private static final ResourceLocation PAGE =
            ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "textures/gui/handbook/page.png");
    private static final ResourceLocation BACK_ICON =
            ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "textures/gui/handbook/back.png");
    private static final ResourceLocation SEARCH_ICON =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/spyglass.png");

    private static final int COLOR_TEXT = 0xFF26221A;
    private static final int COLOR_GRAY = 0xFF6E6250;
    private static final int COLOR_LABEL = 0xFF8A6D3B;
    private static final int COLOR_LINK = 0xFF2D5A8A;
    private static final int COLOR_HOVER = 0x228A7F68;

    // 新书皮背景的纸面区域约为 x25..237 / y11..187，控件都放进纸面内。
    private static final Rect BACK = new Rect(34, 18, 23, 13);
    private static final int TITLE_X = 66;
    private static final int TITLE_Y = 18;
    private static final int LIST_ROW_Y = 44;
    private static final int LIST_ROW_STEP = 20;
    private static final int LIST_ICON_X = 34;
    private static final int LIST_TEXT_X = 56;
    private static final int LIST_SUB_X = 150;
    private static final int LIST_SCROLL_X = 230;
    private static final int LIST_SCROLL_Y = 44;
    private static final List<Rect> CATEGORY_CELLS = List.of(
            new Rect(40, 44, 76, 28), new Rect(144, 44, 76, 28),
            new Rect(40, 78, 76, 28), new Rect(144, 78, 76, 28),
            new Rect(40, 112, 76, 28), new Rect(144, 112, 76, 28),
            new Rect(40, 146, 76, 28), new Rect(144, 146, 76, 28));

    private enum Page { COVER, LIST, ENTRY, REACTION, GUIDE_TEXT }

    private Page page = Page.COVER;
    private HandbookEntries.Category category;
    private List<HandbookEntries.Entry> entries = List.of();
    private List<HandbookEntries.Entry> filtered = List.of();
    private int listScroll;
    private final StringBuilder search = new StringBuilder();

    private HandbookEntries.Entry entry;
    private List<Reactions.Reaction> related = List.of();
    private int relatedScroll;

    private Reactions.Reaction reaction;
    private List<String> guideLines = List.of();
    private String guideTitle = "";
    private int guideScroll;
    private int unlockVersion = -1;

    private int leftPos;
    private int topPos;

    public ChemistryHandbookScreen() {
        super(Component.literal("化学手册"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        leftPos = (this.width - GUI_W) / 2;
        topPos = (this.height - GUI_H) / 2;
        unlockVersion = HandbookUnlockCache.version();
        ClientPacketDistributor.sendToServer(new ChemistryNetworking.HandbookUnlockRequestPacket());
    }

    @Override
    public void tick() {
        int version = HandbookUnlockCache.version();
        if (version != unlockVersion) {
            unlockVersion = version;
            // 服务器返回解锁/发现列表后，刷新列表页与详情页相关反应。
            if (page == Page.LIST) {
                entries = category == HandbookEntries.Category.REACTIONS
                        ? unlockedReactions(HandbookEntries.entries(category))
                        : HandbookEntries.entries(category);
                refreshFiltered();
                listScroll = clamp(listScroll, 0, Math.max(0, filtered.size() - 7));
            } else if (page == Page.ENTRY) {
                related = unlockedRelated();
                relatedScroll = clamp(relatedScroll, 0, Math.max(0, related.size() - 2));
            }
        }
    }

    // ---- 渲染 ----

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 1.21.10 中 renderWithTooltipAndSubtitles 已在渲染前自动绘制一次背景，
        // 这里再调会重复应用模糊导致 "Can only blur once per frame" 崩溃。
        switch (page) {
            case COVER -> renderCover(g, mouseX, mouseY);
            case LIST -> renderList(g, mouseX, mouseY);
            case ENTRY -> renderEntry(g, mouseX, mouseY);
            case REACTION -> renderReaction(g, mouseX, mouseY);
            case GUIDE_TEXT -> renderGuideText(g, mouseX, mouseY);
        }
    }

    private void renderCover(GuiGraphics g, int mouseX, int mouseY) {
        blitFull(g, COVER);
        drawCentered(g, "化学手册", 128, 13, COLOR_TEXT);
        drawCentered(g, "Chemistry Handbook", 128, 24, COLOR_GRAY);
        HandbookEntries.Category[] cats = HandbookEntries.Category.values();
        for (int i = 0; i < Math.min(cats.length, CATEGORY_CELLS.size()); i++) {
            Rect cell = CATEGORY_CELLS.get(i);
            if (cell.contains(mouseX - leftPos, mouseY - topPos)) {
                g.fill(leftPos + cell.x, topPos + cell.y, leftPos + cell.x + cell.w,
                        topPos + cell.y + cell.h, COLOR_HOVER);
            }
            ItemStack icon = categoryIcon(cats[i]);
            if (!icon.isEmpty()) {
                g.renderItem(icon, leftPos + cell.x + 5, topPos + cell.y + 6);
            }
            g.drawString(font, cats[i].title, leftPos + cell.x + 27, topPos + cell.y + 11, 0xFF4A3B28);
        }
        drawCentered(g, "化学时代 · MChemistry", 128, 184, COLOR_GRAY);
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY) {
        blitFull(g, PAGE);
        drawBack(g);
        g.drawString(font, category.title, leftPos + TITLE_X, topPos + TITLE_Y, COLOR_TEXT);
        // 搜索框
        g.fill(leftPos + 150, topPos + 15, leftPos + 236, topPos + 31, 0xC0FFFFFF);
        g.fill(leftPos + 150, topPos + 14, leftPos + 236, topPos + 15, COLOR_LABEL);
        g.fill(leftPos + 150, topPos + 31, leftPos + 236, topPos + 32, COLOR_LABEL);
        g.fill(leftPos + 150, topPos + 15, leftPos + 151, topPos + 31, COLOR_LABEL);
        g.fill(leftPos + 235, topPos + 15, leftPos + 236, topPos + 31, COLOR_LABEL);
        blitIcon(g, SEARCH_ICON, leftPos + 154, topPos + 17);
        g.drawString(font, search.toString(), leftPos + 173, topPos + 18, COLOR_TEXT);

        List<HandbookEntries.Entry> shown = filtered;
        int rowsVisible = 7;
        if (shown.isEmpty()) {
            g.drawString(font, unlockVersion <= 0 ? "正在获取已发现/已解锁内容…"
                    : "暂无内容：持有物质或完成反应后自动出现，/chemistry open 可全部解锁",
                    leftPos + 24, topPos + 70, COLOR_GRAY);
            return;
        }
        for (int i = 0; i < rowsVisible; i++) {
            int idx = listScroll + i;
            if (idx >= shown.size()) {
                break;
            }
            int y = topPos + LIST_ROW_Y + i * LIST_ROW_STEP;
            if (mouseX >= leftPos + 34 && mouseX < leftPos + 236 && mouseY >= y && mouseY < y + 18) {
                g.fill(leftPos + 34, y, leftPos + 236, y + 18, COLOR_HOVER);
            }
            HandbookEntries.Entry e = shown.get(idx);
            if (!e.icon().isEmpty()) {
                g.renderItem(e.icon(), leftPos + LIST_ICON_X, y + 1);
            }
            g.drawString(font, truncate(e.title(), 92), leftPos + LIST_TEXT_X, y + 3, COLOR_TEXT);
            g.drawString(font, truncate(e.subtitle(), 70), leftPos + LIST_SUB_X, y + 3, COLOR_GRAY);
        }
        drawScrollbar(g, shown.size(), rowsVisible);
    }

    private void renderEntry(GuiGraphics g, int mouseX, int mouseY) {
        blitFull(g, PAGE);
        drawBack(g);
        g.drawString(font, truncate(entry.title(), 110), leftPos + TITLE_X, topPos + TITLE_Y, COLOR_TEXT);
        if (!entry.subtitle().isEmpty()) {
            String formula = entry.subtitle();
            g.drawString(font, formula, leftPos + 234 - font.width(formula), topPos + TITLE_Y, COLOR_GRAY);
        }
        // 大图标（2 倍）
        if (!entry.icon().isEmpty()) {
            g.pose().pushMatrix();
            g.pose().translate(leftPos + 34, topPos + 46);
            g.pose().scale(2.0F, 2.0F);
            g.renderItem(entry.icon(), 0, 0);
            g.pose().popMatrix();
        }
        ChemicalInfoProvider.ChemicalInfo info = entry.infoKey() == null
                ? null : ChemicalInfoProvider.forItem(entry.infoKey());
        if (info != null) {
            String[] labels = {"分子量", "密度", "熔点", "沸点", "毒性", "腐蚀性", "爆炸性", "pH", "外观/气味"};
            String[] values = {
                    info.molarMass(), info.density(), info.melting(), info.boiling(),
                    translate("toxicity.mchemistry." + info.toxicity()),
                    translate("corr.mchemistry." + info.corrosiveness()),
                    translate("expl.mchemistry." + info.explosiveness()),
                    info.ph(),
                    info.appearance() + "；" + info.odour()
            };
            for (int i = 0; i < labels.length; i++) {
                int y = topPos + 50 + i * 12;
                g.drawString(font, labels[i], leftPos + 80, y, COLOR_LABEL);
                g.drawString(font, truncate(values[i], 76), leftPos + 150, y, COLOR_TEXT);
            }
        }
        g.drawString(font, "相关反应", leftPos + 34, topPos + 152, COLOR_TEXT);
        int rowsVisible = 2;
        for (int i = 0; i < rowsVisible; i++) {
            int idx = relatedScroll + i;
            if (idx >= related.size()) {
                break;
            }
            int y = topPos + 164 + i * 12;
            if (mouseX >= leftPos + 34 && mouseX < leftPos + 236 && mouseY >= y && mouseY < y + 11) {
                g.fill(leftPos + 34, y, leftPos + 236, y + 11, COLOR_HOVER);
            }
            g.drawString(font, truncate(related.get(idx).display(), 196), leftPos + 36, y, COLOR_LINK);
        }
        if (related.size() > rowsVisible) {
            g.drawString(font, "… 共 " + related.size() + " 条（滚轮浏览）",
                    leftPos + 34, topPos + 186, COLOR_GRAY);
        }
    }

    private void renderReaction(GuiGraphics g, int mouseX, int mouseY) {
        blitFull(g, PAGE);
        drawBack(g);
        g.drawString(font, "化学反应", leftPos + TITLE_X, topPos + TITLE_Y, COLOR_GRAY);
        List<FormattedCharSequence> lines = font.split(Component.literal(reaction.display()), 216);
        int y = topPos + 42;
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, leftPos + 20, y, COLOR_TEXT);
            y += 10;
        }
        // 条件 chips
        List<String> chips = new ArrayList<>();
        chips.add(reaction.requiredTemp() > 0 ? "温度 ≥ " + reaction.requiredTemp() + "°C" : "常温");
        chips.add(reaction.requiredPressure() > 0 ? reaction.requiredPressure() + " kPa" : "常压");
        if (!reaction.catalyst().isEmpty()) {
            chips.add("催化 " + HandbookEntries.catalystName(reaction.catalyst()));
        }
        if (!reaction.concentration().isEmpty()) {
            chips.add(switch (reaction.concentration()) {
                case "dilute" -> "稀";
                case "concentrated" -> "浓";
                default -> reaction.concentration();
            });
        }
        int chipX = 20;
        int chipY = y + 8;
        for (String chip : chips) {
            int w = font.width(chip) + 10;
            if (chipX + w > 240) {
                chipX = 20;
                chipY += 18;
            }
            g.fill(leftPos + chipX, topPos + chipY, leftPos + chipX + w, topPos + chipY + 14, 0xFFD8CFB8);
            g.drawString(font, chip, leftPos + chipX + 5, topPos + chipY + 3, COLOR_TEXT);
            chipX += w + 4;
        }
        // 解锁状态
        g.drawString(font, "已解锁：工业合成塔可用", leftPos + 34, topPos + 160, 0xFF2E7D32);
        g.drawString(font, "反应在反应容器中自动检测，加入反应物后即可进行",
                leftPos + 34, topPos + 184, COLOR_GRAY);
    }

    private void renderGuideText(GuiGraphics g, int mouseX, int mouseY) {
        blitFull(g, PAGE);
        drawBack(g);
        g.drawString(font, guideTitle, leftPos + TITLE_X, topPos + TITLE_Y, COLOR_TEXT);
        int y = topPos + 44 - guideScroll * 12;
        for (String line : guideLines) {
            for (FormattedCharSequence part : font.split(Component.literal(line), 220)) {
                g.drawString(font, part, leftPos + 34, y, COLOR_TEXT);
                y += 12;
            }
            y += 2;
        }
    }

    private void blitFull(GuiGraphics g, ResourceLocation texture) {
        // 该重载参数顺序是 (u0, u1, v0, v1)；之前写成 (0,0,1,1) 会把 UV 变成
        // 零宽区域，整块画布只显示贴图角落一个像素（看起来就是纯深蓝）。
        g.blit(texture, leftPos, topPos, leftPos + GUI_W, topPos + GUI_H,
                0.0F, 1.0F, 0.0F, 1.0F);
    }

    private void blitIcon(GuiGraphics g, ResourceLocation texture, int x, int y) {
        g.blit(texture, x, y, 0, 0, 16, 16, 16, 16);
    }

    private void drawBack(GuiGraphics g) {
        // 原版 sprite 走图集系统，直接用资源路径可能渲染异常；
        // 这里用模组自带的副本 + 明确的 10 参 blit 保证只画一次。
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, BACK_ICON,
                leftPos + BACK.x, topPos + BACK.y, 0.0F, 0.0F, 23, 13, 23, 13);
    }

    private void drawCentered(GuiGraphics g, String text, int x, int y, int color) {
        g.drawString(font, text, leftPos + x - font.width(text) / 2, topPos + y, color);
    }

    private void drawScrollbar(GuiGraphics g, int total, int rowsVisible) {
        int trackX = leftPos + LIST_SCROLL_X;
        int trackY = topPos + LIST_SCROLL_Y;
        int trackH = rowsVisible * 20;
        g.fill(trackX, trackY, trackX + 6, trackY + trackH, 0xFF8A7F68);
        int maxScroll = Math.max(0, total - rowsVisible);
        float ratio = rowsVisible / (float) Math.max(total, 1);
        int thumbH = Math.max(14, (int) (trackH * Math.min(1.0F, ratio)));
        int thumbY = trackY + (maxScroll == 0 ? 0 : listScroll * (trackH - thumbH) / maxScroll);
        g.fill(trackX + 1, thumbY, trackX + 5, thumbY + thumbH, 0xFF4A4030);
    }

    private String truncate(String s, int width) {
        return font.plainSubstrByWidth(s, width);
    }

    private String translate(String key) {
        return Component.translatable(key).getString();
    }

    private static ItemStack categoryIcon(HandbookEntries.Category c) {
        return switch (c) {
            case ELEMENTS -> stack("element_iron_ingot");
            case SOLIDS -> stack("loose_sodium_chloride");
            case LIQUIDS -> ModItems.liquidBottle("water", true);
            case GASES -> ModItems.gasBottle("oxygen", true);
            case INSTRUMENTS -> stack("beaker");
            case REACTIONS -> stack("combustion_spoon");
            case GUIDE -> new ItemStack(ModItems.HANDBOOK.get());
        };
    }

    private static ItemStack stack(String id) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getValue(ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, id))
                .getDefaultInstance();
    }

    // ---- 输入 ----

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClicked) {
        double x = event.x() - leftPos;
        double y = event.y() - topPos;
        if (event.button() == 0) {
            switch (page) {
                case COVER -> {
                    HandbookEntries.Category[] cats = HandbookEntries.Category.values();
                    for (int i = 0; i < Math.min(cats.length, CATEGORY_CELLS.size()); i++) {
                        if (CATEGORY_CELLS.get(i).contains(x, y)) {
                            openCategory(cats[i]);
                            return true;
                        }
                    }
                }
                case LIST -> {
                    if (BACK.contains(x, y)) {
                        page = Page.COVER;
                        return true;
                    }
                    int row = (int) ((y - LIST_ROW_Y) / LIST_ROW_STEP);
                    if (row >= 0 && row < 7) {
                        int idx = listScroll + row;
                        if (idx < filtered.size()) {
                            openEntry(filtered.get(idx));
                            return true;
                        }
                    }
                }
                case ENTRY -> {
                    if (BACK.contains(x, y)) {
                        page = Page.LIST;
                        return true;
                    }
                    int row = (int) ((y - 164) / 12);
                    if (row >= 0 && row < 2) {
                        int idx = relatedScroll + row;
                        if (idx < related.size()) {
                            reaction = related.get(idx);
                            page = Page.REACTION;
                            return true;
                        }
                    }
                }
                case REACTION -> {
                    if (BACK.contains(x, y)) {
                        page = Page.ENTRY;
                        return true;
                    }
                }
                case GUIDE_TEXT -> {
                    if (BACK.contains(x, y)) {
                        page = Page.LIST;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClicked);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int step = (int) Math.signum(verticalAmount);
        switch (page) {
            case LIST -> listScroll = clamp(listScroll - step, 0, Math.max(0, filtered.size() - 7));
            case ENTRY -> relatedScroll = clamp(relatedScroll - step, 0, Math.max(0, related.size() - 2));
            case GUIDE_TEXT -> guideScroll = clamp(guideScroll - step, 0, Math.max(0, guideLines.size() * 2 - 12));
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (page == Page.LIST) {
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search.deleteCharAt(search.length() - 1);
                refreshFiltered();
                listScroll = 0;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ENTER && !search.isEmpty()) {
                search.setLength(0);
                refreshFiltered();
                listScroll = 0;
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (page == Page.LIST && event.isAllowedChatCharacter()) {
            search.append(event.codepointAsString());
            refreshFiltered();
            listScroll = 0;
            return true;
        }
        return super.charTyped(event);
    }

    private void openCategory(HandbookEntries.Category c) {
        category = c;
        entries = c == HandbookEntries.Category.REACTIONS
                ? unlockedReactions(HandbookEntries.entries(c))
                : HandbookEntries.entries(c);
        search.setLength(0);
        refreshFiltered();
        listScroll = 0;
        page = Page.LIST;
    }

    private void openEntry(HandbookEntries.Entry e) {
        if (category == HandbookEntries.Category.REACTIONS && e.reaction() != null) {
            reaction = e.reaction();
            page = Page.REACTION;
            return;
        }
        entry = e;
        related = unlockedRelated();
        relatedScroll = 0;
        if (!e.guideLines().isEmpty()) {
            guideTitle = e.title();
            guideLines = e.guideLines();
            guideScroll = 0;
            page = Page.GUIDE_TEXT;
        } else {
            page = Page.ENTRY;
        }
    }

    private void refreshFiltered() {
        String q = search.toString().toLowerCase();
        if (q.isEmpty()) {
            filtered = entries;
        } else {
            filtered = entries.stream()
                    .filter(e -> e.title().toLowerCase().contains(q)
                            || e.subtitle().toLowerCase().contains(q))
                    .toList();
        }
    }

    private static List<HandbookEntries.Entry> unlockedReactions(List<HandbookEntries.Entry> all) {
        return all.stream()
                .filter(e -> e.reaction() == null
                        || HandbookUnlockCache.isUnlocked(e.reaction().display()))
                .toList();
    }

    private List<Reactions.Reaction> unlockedRelated() {
        return HandbookEntries.relatedReactions(entry).stream()
                .filter(r -> HandbookUnlockCache.isUnlocked(r.display()))
                .toList();
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private record Rect(int x, int y, int w, int h) {
        boolean contains(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }
}
