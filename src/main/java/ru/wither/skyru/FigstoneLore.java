package ru.wither.skyru;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;

/**
 * Reviewed Figstone Splitter lore only. Match full original tooltip rows instead of
 * translating their English/color fragments independently. Unknown shapes fail closed.
 * Uses the original effective Style for every copied value/glyph, never mutating items.
 */
final class FigstoneLore {
    private FigstoneLore() {}

    private static final Pattern GAIN = Pattern.compile(
        "^Gains ([0-9]{1,3}) (.{0,8}?)Sweep every ([0-9][0-9,]{0,9}) logs$");
    private static final Pattern GALATEA = Pattern.compile(
        "^cut on Galatea\\. \\(max ([0-9]{1,3})\\)$");
    private static final Pattern THROW = Pattern.compile(
        "^Throwing your Axe has no (.{0,8}?)Sweep$");
    private static final Pattern PENALTY = Pattern.compile(
        "^penalty for ([0-9]{1,3})s\\.$");
    private static final Pattern LOGS = Pattern.compile(
        "^Logs Cut: ([0-9][0-9,]{0,12})$");

    private static boolean item(List<String> rows) {
        return rows != null && rows.size() <= 80 && !rows.isEmpty()
            && rows.getFirst() != null && rows.getFirst().contains("Figstone Splitter");
    }

    static FormattedCharSequence styled(List<String> rows, int index,
                                        FormattedCharSequence original) {
        if (!item(rows) || index <= 0 || index >= rows.size() || original == null) return null;
        String text = rows.get(index);
        if (text == null || text.isEmpty() || text.length() > 160) return null;
        StyledRuns source = StyledRuns.collect(original, 180);
        if (!text.equals(source.text) || source.runs.isEmpty()) return null;
        Style base = source.styleAt(0);
        List<FormattedCharSequence> target = new ArrayList<>(9);
        Matcher match = GAIN.matcher(text);
        if (match.matches() && index + 1 < rows.size()
            && GALATEA.matcher(rows.get(index + 1)).matches()) {
            int sweep = text.indexOf("Sweep", match.end(2));
            if (!source.uniform(0, match.start(1))
                || !source.uniform(sweep, sweep + 5)) return null;
            target.add(FormattedCharSequence.forward("Даёт +", base));
            source.appendOriginal(target, match.start(1), match.end(1));
            target.add(FormattedCharSequence.forward(" ", base));
            source.appendOriginal(target, match.start(2), match.end(2));
            target.add(FormattedCharSequence.forward("размаха", source.styleAt(sweep)));
            target.add(FormattedCharSequence.forward(" за каждые ", base));
            source.appendOriginal(target, match.start(3), match.end(3));
            target.add(FormattedCharSequence.forward(" брёвен", base));
            return FormattedCharSequence.composite(target);
        }
        match = GALATEA.matcher(text);
        if (match.matches() && index > 0 && GAIN.matcher(rows.get(index - 1)).matches()) {
            target.add(FormattedCharSequence.forward("на Галатее (макс. +", base));
            source.appendOriginal(target, match.start(1), match.end(1));
            target.add(FormattedCharSequence.forward(").", base));
            return FormattedCharSequence.composite(target);
        }
        match = THROW.matcher(text);
        if (match.matches() && index + 1 < rows.size()
            && PENALTY.matcher(rows.get(index + 1)).matches()) {
            int sweep = text.indexOf("Sweep", match.end(1));
            if (!source.uniform(0, match.start(1))
                || !source.uniform(sweep, sweep + 5)) return null;
            target.add(FormattedCharSequence.forward("После броска топора штраф к ", base));
            source.appendOriginal(target, match.start(1), match.end(1));
            target.add(FormattedCharSequence.forward("размаху", source.styleAt(sweep)));
            return FormattedCharSequence.composite(target);
        }
        match = PENALTY.matcher(text);
        if (match.matches() && index > 0 && THROW.matcher(rows.get(index - 1)).matches()) {
            target.add(FormattedCharSequence.forward("не действует в течение ", base));
            source.appendOriginal(target, match.start(1), match.end(1));
            target.add(FormattedCharSequence.forward(" с.", base));
            return FormattedCharSequence.composite(target);
        }
        match = LOGS.matcher(text);
        if (match.matches() && source.uniform(0, match.start(1))) {
            target.add(FormattedCharSequence.forward("Срублено брёвен: ", base));
            source.appendOriginal(target, match.start(1), match.end(1));
            return FormattedCharSequence.composite(target);
        }
        return null;
    }

    static Component component(List<String> rows, int index, Component original) {
        if (original == null || !literalOnly(original, 0) || !item(rows)) return null;
        FormattedCharSequence formatted = sink ->
            StringDecomposer.iterateFormatted(original, Style.EMPTY, sink);
        FormattedCharSequence result = styled(rows, index, formatted);
        if (result == null) return null;
        StyledRuns runs = StyledRuns.collect(result, 400);
        MutableComponent translated = Component.empty();
        for (var run : runs.runs) translated.append(
            Component.literal(runs.text.substring(run.start(), run.end()))
                .setStyle(run.style()));
        return translated;
    }

    private static boolean literalOnly(Component component, int depth) {
        if (depth > 18 || !(component.getContents() instanceof PlainTextContents)) return false;
        for (Component sibling : component.getSiblings())
            if (!literalOnly(sibling, depth + 1)) return false;
        return true;
    }
}
