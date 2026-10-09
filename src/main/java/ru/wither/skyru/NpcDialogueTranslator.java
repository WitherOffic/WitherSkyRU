package ru.wither.skyru;

import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.util.StringDecomposer;

/** Exact, source-backed dialogue translations. No player chat, guesses or I/O at render time. */
public final class NpcDialogueTranslator {
    private static final Pattern LEGACY_COLORS = Pattern.compile("(?i)§[0-9a-fk-or]");
    private static final Pattern TIME = Pattern.compile("^\\[\\d{2}:\\d{2}:\\d{2}\\] ");
    private record Anchor(String source, String target) {}
    private record Entry(String target, int prefixLength, List<Anchor> anchors) {}
    private record Span(int start, int end, String text, Style style) {}
    private record Region(int start, int end, Style style) {}
    private final Map<String, Entry> entries;

    public NpcDialogueTranslator() throws IOException {
        var stream = NpcDialogueTranslator.class.getResourceAsStream("/data/witherskyru/npc-dialogues.json");
        if (stream == null) throw new IOException("Missing NPC dialogue resource");
        Map<String, Entry> loaded = new HashMap<>();
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            for (var row : root.getAsJsonArray("records")) {
                var record = row.getAsJsonObject();
                String source = record.get("source").getAsString();
                String target = record.get("translation").getAsString();
                String prefix = "[NPC] " + record.get("speaker").getAsString() + ": ";
                if (!source.startsWith(prefix) || !target.startsWith(prefix))
                    throw new IOException("NPC name/prefix must be retained");
                if (source.startsWith("✆ ", prefix.length()) && target.startsWith("✆ ", prefix.length()))
                    prefix += "✆ ";
                if (source.length() > 4096 || target.length() > 4096 || source.equals(target))
                    throw new IOException("Invalid NPC translation");
                List<Anchor> anchors = new ArrayList<>();
                if (record.has("style_anchors")) {
                    for (var raw : record.getAsJsonArray("style_anchors")) {
                        var anchor = raw.getAsJsonObject();
                        String original = anchor.get("source").getAsString();
                        String translated = anchor.get("target").getAsString();
                        if (original.isEmpty() || translated.isEmpty()
                            || uniquePosition(source, original, prefix.length()) < 0
                            || uniquePosition(target, translated, prefix.length()) < 0)
                            throw new IOException("Ambiguous NPC style anchor");
                        anchors.add(new Anchor(original, translated));
                    }
                }
                if (loaded.putIfAbsent(source, new Entry(target, prefix.length(), List.copyOf(anchors))) != null)
                    throw new IOException("Duplicate NPC source");
            }
        } catch (RuntimeException e) {
            throw new IOException("Invalid NPC dialogue JSON", e);
        }
        entries = Map.copyOf(loaded);
    }

    public int phraseCount() { return entries.size(); }

    /** Unknown messages, non-literal trees and unsupported formatting remain the original object. */
    public Component translate(Component original) {
        if (original == null) return null;
        String source = LEGACY_COLORS.matcher(original.getString()).replaceAll("");
        var time = TIME.matcher(source);
        int timeLength = time.find() ? time.end() : 0;
        Entry entry = entries.get(source.substring(timeLength));
        if (entry != null && timeLength > 0)
            entry = new Entry(source.substring(0, timeLength) + entry.target(), entry.prefixLength() + timeLength, entry.anchors());
        if (entry == null || !literalTree(original, 0, new int[] {0})) return original;

        List<Span> spans = new ArrayList<>();
        int[] offset = {0};
        StringBuilder run = new StringBuilder(), visible = new StringBuilder();
        Style[] currentStyle = {null};
        Runnable flush = () -> {
            if (!run.isEmpty()) {
                spans.add(new Span(offset[0] - run.length(), offset[0], run.toString(), currentStyle[0]));
                run.setLength(0);
            }
        };
        // The game's own parser resolves § formatting and inherited Component styles.
        // Logical text order is retained; we do not use a visual/bidi string as a lookup key.
        StringDecomposer.iterateFormatted(original, Style.EMPTY, (index, style, codePoint) -> {
            if (!java.util.Objects.equals(currentStyle[0], style)) flush.run();
            currentStyle[0] = style;
            run.appendCodePoint(codePoint);
            visible.appendCodePoint(codePoint);
            offset[0] += Character.charCount(codePoint);
            return true;
        });
        flush.run();
        if (!visible.toString().equals(source)) return original;
        Style bodyStyle = styleAt(spans, entry.prefixLength());
        if (bodyStyle == null) return original;

        List<Region> sourceAnchors = new ArrayList<>();
        List<Region> targetAnchors = new ArrayList<>();
        for (Anchor anchor : entry.anchors()) {
            int from = uniquePosition(source, anchor.source(), entry.prefixLength());
            int to = uniquePosition(entry.target(), anchor.target(), entry.prefixLength());
            Style style = uniformStyle(spans, from, from + anchor.source().length());
            if (style == null) return original;
            sourceAnchors.add(new Region(from, from + anchor.source().length(), style));
            targetAnchors.add(new Region(to, to + anchor.target().length(), style));
        }
        targetAnchors.sort(java.util.Comparator.comparingInt(Region::start));
        int previousEnd = entry.prefixLength();
        for (Region region : targetAnchors) {
            if (region.start() < previousEnd) return original;
            previousEnd = region.end();
        }
        // Never arbitrarily assign a click/hover event or coloured word to a different meaning.
        // Only a uniform body and explicitly authored semantic anchors are supported initially.
        for (Span span : spans) {
            int start = Math.max(entry.prefixLength(), span.start());
            int end = span.end();
            // Formatting often includes a separator after a highlighted term.
            // Whitespace has no translated semantic meaning; do not reject its anchor.
            while (start < end && Character.isWhitespace(source.charAt(start))) start++;
            while (end > start && Character.isWhitespace(source.charAt(end - 1))) end--;
            if (start >= end || span.style().equals(bodyStyle)) continue;
            boolean anchored = false;
            for (Region region : sourceAnchors) {
                if (start >= region.start() && end <= region.end()) { anchored = true; break; }
            }
            if (!anchored) return original;
        }

        MutableComponent result = Component.empty();
        appendSource(result, spans, 0, entry.prefixLength());
        int cursor = entry.prefixLength();
        for (Region region : targetAnchors) {
            append(result, entry.target().substring(cursor, region.start()), bodyStyle);
            append(result, entry.target().substring(region.start(), region.end()), region.style());
            cursor = region.end();
        }
        append(result, entry.target().substring(cursor), bodyStyle);
        return result;
    }

    private static boolean literalTree(Component component, int depth, int[] count) {
        if (depth > 18 || ++count[0] > 128 || !(component.getContents() instanceof PlainTextContents)) return false;
        for (Component child : component.getSiblings())
            if (!literalTree(child, depth + 1, count)) return false;
        return true;
    }

    private static int uniquePosition(String text, String fragment, int start) {
        int position = text.indexOf(fragment, start);
        return position >= 0 && text.indexOf(fragment, position + 1) < 0 ? position : -1;
    }

    private static Style styleAt(List<Span> spans, int position) {
        for (Span span : spans) if (span.start() <= position && span.end() > position) return span.style();
        return null;
    }

    private static Style uniformStyle(List<Span> spans, int start, int end) {
        Style result = null;
        int covered = 0;
        for (Span span : spans) {
            int overlap = Math.min(end, span.end()) - Math.max(start, span.start());
            if (overlap <= 0) continue;
            if (result != null && !result.equals(span.style())) return null;
            result = span.style();
            covered += overlap;
        }
        return covered == end - start ? result : null;
    }

    private static void appendSource(MutableComponent result, List<Span> spans, int start, int end) {
        for (Span span : spans) {
            int from = Math.max(start, span.start()), to = Math.min(end, span.end());
            if (from < to) append(result, span.text().substring(from - span.start(), to - span.start()), span.style());
        }
    }

    private static void append(MutableComponent result, String text, Style style) {
        if (!text.isEmpty()) result.append(Component.literal(text).setStyle(style));
    }
}
