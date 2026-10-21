package ru.wither.skyru;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/** Staged settings: Save applies immediately; Escape/Cancel makes no changes. */
public final class SkyRuSettingsScreen extends Screen {
    private final Screen parent;
    private TooltipFont.Choice selected = TooltipFont.choice();
    private boolean wrap = SkyRuClient.tooltipWrap();
    private String error;
    public SkyRuSettingsScreen(Screen parent) { super(Component.literal("WitherSkyRU — шрифт")); this.parent = parent; }
    @Override protected void init() {
        int boxWidth = Math.min(280, width - 24), left = (width - boxWidth) / 2;
        int top = Math.max(30, height / 2 - 80);
        addRenderableWidget(Button.builder(fontLabel(), button -> {
            selected = selected.next(); button.setMessage(fontLabel()); error = null;
        }).bounds(left, top, boxWidth, 20).build());
        addRenderableWidget(Button.builder(wrapLabel(), button -> {
            wrap = !wrap; button.setMessage(wrapLabel()); error = null;
        }).bounds(left, top + 24, boxWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Сбросить выбор"), button -> {
            selected = TooltipFont.Choice.ORIGINAL; wrap = true; rebuildWidgets();
        }).bounds(left, top + 48, boxWidth, 20).build());
        int half = (boxWidth - 4) / 2;
        addRenderableWidget(Button.builder(Component.literal("Сохранить"), button -> {
            try { TooltipFont.save(selected, wrap); onClose(); }
            catch (Exception ex) { error = "Не удалось сохранить настройки"; System.err.println("[WitherSkyRU] Font settings save failed: " + ex); }
        }).bounds(left, Math.min(height - 26, top + 148), half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Отмена"), button -> onClose())
            .bounds(left + half + 4, Math.min(height - 26, top + 148), half, 20).build());
    }
    private Component fontLabel() { return Component.literal("Шрифт подсказок: " + selected.label); }
    private Component wrapLabel() { return Component.literal("Перенос строк: " + (wrap ? "вкл." : "выкл.")); }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int top = Math.max(30, height / 2 - 80);
        graphics.centeredText(font, title, width / 2, 12, 0xffffffff);
        var sample = TooltipFont.apply(FormattedCharSequence.forward("Алмазная кирка · Здоровье +120", Style.EMPTY), selected);
        graphics.centeredText(font, sample, width / 2, top + 80, 0xff55ffff);
        graphics.centeredText(font, "Текущий — шрифт игры/ресурспака", width / 2, top + 100, 0xffaaaaaa);
        graphics.centeredText(font, "Только обычные подсказки предметов", width / 2, top + 114, 0xffaaaaaa);
        if (error != null) graphics.centeredText(font, error, width / 2, top + 130, 0xffff5555);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
