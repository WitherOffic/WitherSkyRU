package ru.wither.skyru;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import ru.wither.skyru.mixin.TextTooltipAccessor;

/** Measure and wrap vanilla tooltip copies before drawing; unknown fixed panels fail closed. */
public final class TooltipTextFit {
    private static final ThreadLocal<Integer> PREPARED_DEPTH = ThreadLocal.withInitial(() -> 0);
    private TooltipTextFit() {}
    public static boolean currentPanel() {
        if (!SkyRuClient.tooltipTextFitActive()) return false;
        return RenderContext.current().tooltip();
    }

    public static boolean renderingPrepared() { return PREPARED_DEPTH.get() > 0; }

    /** Prevent translating wrapped fragments again or stacking collector/text scaling. */
    public static void renderPrepared(Runnable draw) {
        int depth = PREPARED_DEPTH.get();
        PREPARED_DEPTH.set(depth + 1);
        try { draw.run(); }
        finally {
            if (depth == 0) PREPARED_DEPTH.remove();
            else PREPARED_DEPTH.set(depth);
        }
    }

    public static int wrapWidth(int screenWidth) {
        return Math.max(1, Math.min(360, screenWidth - 24));
    }

    /** Exact vanilla text rows only: image/custom rows keep their own layout and identity. */
    public static List<ClientTooltipComponent> prepare(Font font, List<ClientTooltipComponent> rows, int screenWidth) {
        List<ClientTooltipComponent> result = new ArrayList<>(rows.size());
        boolean changed = false;
        boolean originalTitle = OriginalItemTitle.held();
        for (var row : rows) {
            if (row.getClass() == ClientTextTooltip.class && row instanceof TextTooltipAccessor access) {
                var source = access.witherskyru$text();
                var translated = originalTitle && result.isEmpty()
                    ? OriginalItemTitle.protect(source) : SkyRuClient.sequence(source);
                translated = TooltipFont.apply(translated);
                result.add(new PreparedText(row, SkyRuClient.tooltipWrap()
                    ? wrap(font, translated, wrapWidth(screenWidth)) : List.of(translated)));
                changed = true;
            } else result.add(row);
        }
        return changed ? result : rows;
    }

    /** Preserve style/font/click/hover per run, then let Minecraft perform word wrapping. */
    public static List<FormattedCharSequence> wrap(Font font, FormattedCharSequence text, int width) {
        List<FormattedText> runs = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        Style[] current = {Style.EMPTY};
        Runnable flush = () -> {
            if (!run.isEmpty()) {
                runs.add(FormattedText.of(run.toString(), current[0]));
                run.setLength(0);
            }
        };
        text.accept((index, style, point) -> {
            if (!Objects.equals(current[0], style)) flush.run();
            current[0] = style;
            run.appendCodePoint(point);
            return true;
        });
        flush.run();
        var lines = font.split(FormattedText.composite(runs), Math.max(1, width));
        return lines.isEmpty() ? List.of(FormattedCharSequence.EMPTY) : List.copyOf(lines);
    }

    public static final class PreparedText implements ClientTooltipComponent {
        private final ClientTooltipComponent original;
        private final List<FormattedCharSequence> lines;
        private final int minimumRowHeight;
        public PreparedText(ClientTooltipComponent original, List<FormattedCharSequence> lines) {
            this.original = original;
            this.lines = List.copyOf(lines);
            this.minimumRowHeight = TooltipFont.minimumRowHeight(this.lines);
            if (this.lines.isEmpty()) throw new IllegalArgumentException("At least one tooltip line is required");
        }
        public List<FormattedCharSequence> lines() { return lines; }
        @Override public int getWidth(Font font) {
            return lines.stream().mapToInt(font::width).max().orElse(0);
        }
        @Override public int getHeight(Font font) {
            return lines.size() * lineHeight(font);
        }
        private int lineHeight(Font font) { return Math.max(minimumRowHeight, original.getHeight(font)); }
        @Override public boolean showTooltipWithItemInHand() { return original.showTooltipWithItemInHand(); }
        @Override public void extractText(GuiGraphicsExtractor graphics, Font font, int x, int y) {
            renderPrepared(() -> {
                int lineHeight = lineHeight(font);
                for (int i = 0; i < lines.size(); i++) {
                    graphics.text(font, lines.get(i), x, y + i * lineHeight, -1, true);
                }
            });
        }
    }
    public static boolean eligibleFrames(List<String> frames) {
        boolean tooltip = false;
        for (String frame : frames) {
            String lower = frame.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("chatcomponent#") || lower.contains(".chat.")) return false;
            String method = lower.substring(lower.lastIndexOf('#') + 1);
            tooltip |= lower.contains("tooltip") &&
                (method.contains("extract") || method.contains("draw") || method.contains("render"));
        }
        return tooltip;
    }
}
