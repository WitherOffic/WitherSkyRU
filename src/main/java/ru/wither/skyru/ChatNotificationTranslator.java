package ru.wither.skyru;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.util.StringDecomposer;

/** Anchored notification whitelist. Never guesses translations for ordinary player chat. */
public final class ChatNotificationTranslator {
    private static final Pattern TIME = Pattern.compile("^\\[\\d{2}:\\d{2}:\\d{2}\\] ");
    private static final Pattern RARE = Pattern.compile("^RARE DROP! (.{1,160}) \\(\\+([\\d,. ]+) ([^()\\n]*?)Magic Find\\)$");
    private static final Pattern COINS = Pattern.compile("^The ((?:\\[[^\\]\\n]{1,60} head\\]|\\uFFFC)?Goblin) was carrying a bag of coins! \\+([\\d,]+) Coins!$");
    private static final String GHAST = "The sound of pickaxes clashing against the rock has attracted the attention of the POWDER GHAST!";
    private static final Pattern FIND = Pattern.compile("^Find the Powder Ghast near the (.{1,80})!$");
    private static final Pattern BESTIARY = Pattern.compile("^BESTIARY (.{1,100}) ([IVXLCDM]+) (?:➡|➜|→) ([IVXLCDM]+)$");
    private static final Pattern BESTIARY_TIER = Pattern.compile("^BESTIARY (.{1,100}) reached Tier (\\d{1,6})!$");
    private static final Pattern KILL_COMBO = Pattern.compile("^You reached a (\\d{1,9}) Kill Combo!$");
    private static final Pattern SHOP = Pattern.compile("^\\[SkyHanni\\] New rabbits are available at Hoppity's Shop!(?:(?:\\n|\\\\n)\\[CLICK to warp to hub or disable this feature\\])?$");
    private static final Pattern EGG = Pattern.compile("^\\[Skyblocker\\] Found a (Chocolate) (Breakfast|Lunch|Dinner) (Egg) at (-?\\d+, -?\\d+, -?\\d+)!$");
    private static final Pattern SACKS = Pattern.compile("^\\[Sacks\\] \\+([\\d,]+) items\\. \\(Last ([\\d.]+)s\\.\\)(?: \\((?:x|×)\\d+\\))?$");
    private static final Pattern STASH = Pattern.compile("^\\[SkyHanni\\] You have ([\\d,.]+[kKmMbB]?) materials in stash, totalling (\\d+) types\\. Click to pickup your stash!$");
    private record Atom(int start, int end, String text, Style style, Component opaque) {}
    private record Part(String text, int start, int end) {}
    private record Edit(int start, int end, List<Part> parts) {}
    private final OfflineDictionary dictionary;

    public ChatNotificationTranslator(OfflineDictionary dictionary) { this.dictionary = dictionary; }
    public int familyCount() { return 11; }

    public Component translate(Component original, GuiMessageSource channel, boolean signed) {
        if (original == null || signed || (channel != GuiMessageSource.SYSTEM_SERVER
            && channel != GuiMessageSource.SYSTEM_CLIENT)) return original;
        try {
            List<Atom> atoms = new ArrayList<>();
            StringBuilder visible = new StringBuilder();
            collect(original, Style.EMPTY, atoms, visible, 0, new int[]{0});
            if (visible.length() > 8192) return original;
            String source = visible.toString();
            Matcher timestamp = TIME.matcher(source);
            int offset = timestamp.find() ? timestamp.end() : 0;
            String body = source.substring(offset);
            List<Edit> edits = plan(body);
            if (edits.isEmpty()) return original;
            List<Edit> absolute = new ArrayList<>();
            for (Edit e : edits) absolute.add(new Edit(e.start + offset, e.end + offset,
                e.parts.stream().map(p -> new Part(p.text, p.start + offset, p.end + offset)).toList()));
            absolute.sort(Comparator.comparingInt(Edit::start));
            int cursor = 0;
            MutableComponent result = Component.empty();
            for (Edit e : absolute) {
                if (e.start < cursor || e.end <= e.start) return original;
                for (Atom atom : atoms) if (atom.opaque != null && atom.start < e.end && atom.end > e.start)
                    return original; // Icons, item heads and non-literal contents are never rewritten.
                appendOriginal(result, atoms, cursor, e.start);
                for (Part part : e.parts) {
                    Style style = uniform(atoms, part.start, part.end);
                    if (style == null) return original; // No speculative reassignment of events/colours.
                    if (!part.text.isEmpty()) result.append(Component.literal(part.text).setStyle(style));
                }
                cursor = e.end;
            }
            appendOriginal(result, atoms, cursor, source.length());
            return result;
        } catch (RuntimeException unsupported) { return original; }
    }

    private List<Edit> plan(String body) {
        List<Edit> edits = new ArrayList<>();
        Matcher m = RARE.matcher(body);
        if (m.matches()) {
            literal(edits, body, "RARE DROP!", "РЕДКАЯ ДОБЫЧА!");
            item(edits, m, 1);
            int stat = body.lastIndexOf("Magic Find");
            replace(edits, stat, stat + "Magic Find".length(), "Шанс редкой добычи");
        } else if ((m = COINS.matcher(body)).matches()) {
            literal(edits, body, "The ", "У ");
            int name = m.end(1) - "Goblin".length();
            replace(edits, name, m.end(1), "гоблина");
            literal(edits, body, " was carrying a bag of coins! ", " был мешок монет! ");
            literal(edits, body, "Coins!", noun(m.group(2), "монета", "монеты", "монет") + "!");
        } else if (body.equals(GHAST)) {
            literal(edits, body, "The sound of pickaxes clashing against the rock has attracted the attention of the ", "Стук кирок по камню привлёк внимание ");
            literal(edits, body, "POWDER GHAST", "ПОРОШКОВОГО ГАСТА");
        } else if ((m = FIND.matcher(body)).matches()) {
            literal(edits, body, "Find the Powder Ghast near the ", "Найдите Порошкового гаста: ");
            item(edits, m, 1);
        } else if ((m = BESTIARY.matcher(body)).matches()) {
            literal(edits, body, "BESTIARY", "БЕСТИАРИЙ");
            item(edits, m, 1);
        } else if ((m = BESTIARY_TIER.matcher(body)).matches()) {
            literal(edits, body, "BESTIARY", "БЕСТИАРИЙ");
            item(edits, m, 1);
            literal(edits, body, " reached Tier ", ": ступень ");
        } else if ((m = KILL_COMBO.matcher(body)).matches()) {
            literal(edits, body, "You reached a ", "Вы достигли серии из ");
            literal(edits, body, " Kill Combo!", " " + noun(m.group(1), "убийства", "убийств", "убийств") + "!");
        } else if (body.equals("Your Kill Combo has expired!")) {
            replace(edits, 0, body.length(), "Ваша серия убийств закончилась!");
        } else if (body.strip().equals("BESTIARY")) {
            literal(edits, body, "BESTIARY", "БЕСТИАРИЙ");
        } else if (SHOP.matcher(body).matches()) {
            literal(edits, body, "New rabbits are available at ", "Новые кролики доступны в ");
            literal(edits, body, "Hoppity's Shop", "магазине Хоппити");
            literal(edits, body, "[CLICK to warp to hub or disable this feature]", "[НАЖМИТЕ: перейти в хаб или отключить уведомления]");
        } else if ((m = EGG.matcher(body)).matches()) {
            literal(edits, body, "Found a ", "Найдено ");
            String meal = switch (m.group(2)) { case "Breakfast" -> "завтрак"; case "Lunch" -> "обед"; default -> "ужин"; };
            // Semantic pieces permit word reordering without losing each word's style/events.
            edits.add(new Edit(m.start(1), m.end(3), List.of(
                new Part("шоколадное ", m.start(1), m.end(1)),
                new Part("яйцо", m.start(3), m.end(3)),
                new Part(" (" + meal + ")", m.start(2), m.end(2)))));
            literal(edits, body, " at ", " по координатам ");
        } else if ((m = SACKS.matcher(body)).matches()) {
            literal(edits, body, "[Sacks]", "[Мешки]");
            literal(edits, body, "items.", noun(m.group(1), "предмет", "предмета", "предметов") + ".");
            literal(edits, body, "Last ", "За ");
            replace(edits, m.end(2), m.end(2) + 1, " с");
        } else if ((m = STASH.matcher(body)).matches()) {
            literal(edits, body, "You have ", "В тайнике: ");
            literal(edits, body, "materials in stash, totalling ", "материалов, видов: ");
            literal(edits, body, " types", "");
            literal(edits, body, "Click to ", "Нажмите, чтобы ");
            literal(edits, body, "pickup", "забрать");
            literal(edits, body, "your stash!", "вещи!");
        }
        return edits;
    }

    private void item(List<Edit> edits, Matcher m, int group) {
        String full = m.group(group);
        String plain = full.replaceFirst("^(?:\\[[^\\]\\n]{1,60} head\\]|\\uFFFC)\\s*", "");
        String translated = dictionary.translate(plain);
        if (!plain.equals(translated)) replace(edits, m.end(group) - plain.length(), m.end(group), translated);
    }

    private static void literal(List<Edit> edits, String source, String old, String translated) {
        int at = source.indexOf(old);
        if (at >= 0) replace(edits, at, at + old.length(), translated);
    }
    private static void replace(List<Edit> edits, int start, int end, String text) {
        edits.add(new Edit(start, end, List.of(new Part(text, start, end))));
    }
    private static String noun(String number, String one, String few, String many) {
        String digits = number.replace(",", "");
        int last = Integer.parseInt(digits.substring(Math.max(0, digits.length() - 2)));
        return last >= 11 && last <= 14 ? many : last % 10 == 1 ? one : last % 10 >= 2 && last % 10 <= 4 ? few : many;
    }

    private static void collect(Component node, Style inherited, List<Atom> atoms,
        StringBuilder visible, int depth, int[] count) {
        if (depth > 18 || ++count[0] > 256 || visible.length() > 8192) throw new IllegalArgumentException("Large tree");
        Style style = node.getStyle().applyTo(inherited);
        if (node.getContents() instanceof PlainTextContents plain) {
            StringBuilder run = new StringBuilder();
            Style[] current = {null};
            Runnable flush = () -> {
                if (run.isEmpty()) return;
                int start = visible.length();
                visible.append(run);
                atoms.add(new Atom(start, visible.length(), run.toString(), current[0], null));
                run.setLength(0);
            };
            StringDecomposer.iterateFormatted(plain.text(), style, (index, glyphStyle, point) -> {
                if (!glyphStyle.equals(current[0])) flush.run();
                current[0] = glyphStyle;
                run.appendCodePoint(point);
                return true;
            });
            flush.run();
        } else {
            Component opaque = node.plainCopy().setStyle(style);
            String text = opaque.getString();
            if (text.isEmpty() || text.indexOf('§') >= 0) throw new IllegalArgumentException("Opaque text");
            int start = visible.length();
            visible.append(text);
            atoms.add(new Atom(start, visible.length(), text, style, opaque));
        }
        for (Component sibling : node.getSiblings()) collect(sibling, style, atoms, visible, depth + 1, count);
    }

    private static Style uniform(List<Atom> atoms, int start, int end) {
        Style style = null;
        int covered = 0;
        for (Atom atom : atoms) {
            int size = Math.min(end, atom.end) - Math.max(start, atom.start);
            if (size <= 0) continue;
            if (atom.opaque != null || (style != null && !style.equals(atom.style))) return null;
            style = atom.style;
            covered += size;
        }
        return covered == end - start ? style : null;
    }
    private static void appendOriginal(MutableComponent result, List<Atom> atoms, int start, int end) {
        for (Atom atom : atoms) {
            int from = Math.max(start, atom.start), to = Math.min(end, atom.end);
            if (to <= from) continue;
            if (atom.opaque != null) {
                if (from != atom.start || to != atom.end) throw new IllegalArgumentException("Split icon");
                result.append(atom.opaque);
            } else result.append(Component.literal(atom.text.substring(from - atom.start, to - atom.start)).setStyle(atom.style));
        }
    }
}
