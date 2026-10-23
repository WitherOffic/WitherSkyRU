package ru.wither.skyru;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringDecomposer;

/** Tooltip title policy shared by all overloads; first row is a display copy. */
public final class TooltipTranslation {
    private TooltipTranslation() {}
    public static List<FormattedCharSequence> styled(List<FormattedCharSequence> rows, boolean originalTitle) {
        if(rows==null||!SkyRuClient.active())return rows;
        try(var context=RenderContext.open()){return styledRows(rows,originalTitle);}
    }
    private static List<FormattedCharSequence> styledRows(List<FormattedCharSequence> rows, boolean originalTitle) {
        if (rows == null || !SkyRuClient.active() || UserInputText.rendering()) return rows;
        List<String> sourceText = rows.stream().map(OriginalItemTitle::unwrap).map(TooltipTranslation::plain).toList();
        CollectionContributors.remember(sourceText);
        List<FormattedCharSequence> out = new ArrayList<>(rows.size());
        java.util.Set<Integer> reviewedRows = new java.util.HashSet<>();
        boolean changed = false;
        for (int i = 0; i < rows.size(); i++) {
            var source = OriginalItemTitle.unwrap(rows.get(i));
            var power = PowerStoneTranslation.render(source,sourceText,i);
            var lore = BoosterUpgradeLore.styled(rows,sourceText,i);
            if(lore==null)lore=ReviewedLoreStyles.render(source,sourceText,i);
            if(lore==null)lore=FigstoneLore.styled(sourceText,i,source);
            if (lore != null) reviewedRows.add(i);
            var next = originalTitle && i == 0 ? OriginalItemTitle.protect(source) : lore!=null?lore:power!=null?power:SkyRuClient.sequence(source);
            out.add(next); changed |= next != rows.get(i);
        }
        List<FormattedCharSequence> contextual = new ArrayList<>(out);
        boolean usable = true;
        for (var edit : SkyRuClient.tooltipContextEdits(sourceText)) {
            if (reviewedRows.contains(edit.row())) continue;
            if (originalTitle && edit.row() == 0) { usable = false; break; }
            if (edit.optional() && !plain(contextual.get(edit.row())).contains(edit.before())) continue;
            var next = rewrite(contextual.get(edit.row()), edit.before(), edit.after());
            if (next == null) { usable = false; break; }
            contextual.set(edit.row(), next);
        }
        if (usable && !contextual.equals(out)) { out = contextual; changed = true; }
        return changed ? out : rows;
    }
    public static List<Component> components(List<Component> rows, boolean originalTitle) {
        if(rows==null||!SkyRuClient.active())return rows;
        try(var context=RenderContext.open()){return componentRows(rows,originalTitle);}
    }
    private static List<Component> componentRows(List<Component> rows, boolean originalTitle) {
        if (rows == null || !SkyRuClient.active() || UserInputText.rendering()) return rows;
        List<String> sourceText = rows.stream().map(c -> LegacyFormatting.strip(c.getString())).toList();
        CollectionContributors.remember(sourceText);
        List<Component> out = new ArrayList<>(rows.size());
        java.util.Set<Integer> reviewedRows = new java.util.HashSet<>();
        boolean changed = false;
        for (int i = 0; i < rows.size(); i++) {
            Component power = PowerStoneTranslation.render(rows.get(i),sourceText,i);
            Component lore = BoosterUpgradeLore.component(rows,sourceText,i);
            if(lore==null)lore=ReviewedLoreStyles.render(rows.get(i),sourceText,i);
            if(lore==null)lore=FigstoneLore.component(sourceText,i,rows.get(i));
            if (lore != null) reviewedRows.add(i);
            Component next = originalTitle && i == 0 ? rows.get(i) : lore!=null?lore:power!=null?power:SkyRuClient.component(rows.get(i));
            out.add(next); changed |= next != rows.get(i);
        }
        List<Component> contextual = new ArrayList<>(out);
        boolean usable = true;
        for (var edit : SkyRuClient.tooltipContextEdits(sourceText)) {
            if (reviewedRows.contains(edit.row())) continue;
            if (originalTitle && edit.row() == 0) { usable = false; break; }
            Component row = contextual.get(edit.row());
            if (edit.optional() && !row.getString().contains(edit.before())) continue;
            FormattedCharSequence formatted = sink -> StringDecomposer.iterateFormatted(row, Style.EMPTY, sink);
            var rewritten = rewrite(formatted, edit.before(), edit.after());
            if (rewritten == null) { usable = false; break; }
            MutableComponent rebuilt = Component.empty();
            StringBuilder run = new StringBuilder();
            Style[] currentStyle = {null};
            Runnable flush = () -> {
                if (!run.isEmpty()) { rebuilt.append(Component.literal(run.toString()).setStyle(currentStyle[0])); run.setLength(0); }
            };
            rewritten.accept((i, s, cp) -> {
                if (!s.equals(currentStyle[0])) flush.run();
                currentStyle[0] = s; run.appendCodePoint(cp); return true;
            });
            flush.run();
            contextual.set(edit.row(), rebuilt);
        }
        if (usable && !contextual.equals(out)) { out = contextual; changed = true; }
        return changed ? out : rows;
    }

    private static String plain(FormattedCharSequence row) {
        StringBuilder text = new StringBuilder();
        row.accept((i, s, cp) -> { text.appendCodePoint(cp); return true; });
        return text.toString();
    }

    /** Changes only the reviewed literal span. Mixed styles inside it are not flattened. */
    private static FormattedCharSequence rewrite(FormattedCharSequence row, String before, String after) {
        String text = plain(row);
        int start = text.indexOf(before), end = start + before.length();
        if (start < 0 || text.indexOf(before, end) >= 0) return null;
        List<Style> styles = new ArrayList<>();
        row.accept((i, style, cp) -> { for (int n = 0; n < Character.charCount(cp); n++) styles.add(style); return true; });
        Style replacementStyle = styles.get(start);
        for (int i = start + 1; i < end; i++) if (!replacementStyle.equals(styles.get(i))) return null;
        String next = text.substring(0, start) + after + text.substring(end);
        List<Style> nextStyles = new ArrayList<>(next.length());
        nextStyles.addAll(styles.subList(0, start));
        for (int i = 0; i < after.length(); i++) nextStyles.add(replacementStyle);
        nextStyles.addAll(styles.subList(end, styles.size()));
        return sink -> {
            for (int i = 0; i < next.length();) {
                int cp = next.codePointAt(i);
                if (!sink.accept(i, nextStyles.get(i), cp)) return false;
                i += Character.charCount(cp);
            }
            return true;
        };
    }
}
