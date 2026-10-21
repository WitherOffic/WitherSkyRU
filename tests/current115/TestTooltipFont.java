import java.nio.file.*;
import java.util.*;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import ru.wither.skyru.*;

/** Font and persistence policy without launching a client or loading system fonts. */
public class TestTooltipFont {
    private static int checks;
    private static void check(String label, boolean ok) { if (!ok) throw new AssertionError(label); checks++; }
    private static String plain(FormattedCharSequence text) {
        StringBuilder out = new StringBuilder(); text.accept((i, s, cp) -> { out.appendCodePoint(cp); return true; }); return out.toString();
    }
    private static final FontDescription UNIFORM = new FontDescription.Resource(Identifier.withDefaultNamespace("uniform"));
    private static class MetricsFont extends Font {
        private final StringSplitter splitter = new StringSplitter((cp, style) -> style.getFont().equals(UNIFORM) ? 8 : 5);
        MetricsFont() { super(null); }
        @Override public int width(FormattedCharSequence text) { return (int)Math.ceil(splitter.stringWidth(text)); }
        @Override public List<FormattedCharSequence> split(FormattedText text, int width) {
            List<FormattedCharSequence> out = new ArrayList<>();
            for (var line : splitter.splitLines(text, width, Style.EMPTY)) {
                List<FormattedCharSequence> runs = new ArrayList<>();
                line.visit((style, value) -> { runs.add(FormattedCharSequence.forward(value, style)); return Optional.empty(); }, Style.EMPTY);
                out.add(FormattedCharSequence.composite(runs));
            }
            return out;
        }
    }
    public static void main(String[] args) throws Exception {
        check("unknown mode safe", TooltipFont.Choice.parse("broken") == TooltipFont.Choice.ORIGINAL);
        check("null mode safe", TooltipFont.Choice.parse(null) == TooltipFont.Choice.ORIGINAL);
        check("case insensitive", TooltipFont.Choice.parse("UnIfOrM") == TooltipFont.Choice.UNIFORM);
        check("cycle", TooltipFont.Choice.ORIGINAL.next().next().next().next() == TooltipFont.Choice.ORIGINAL);
        check("Inter persistence name", TooltipFont.Choice.parse("inter_medium") == TooltipFont.Choice.INTER_MEDIUM);
        var click = new ClickEvent.RunCommand("/viewrecipe DIAMOND_PICKAXE");
        var hover = new HoverEvent.ShowText(Component.literal("original hover"));
        var rich = Style.EMPTY.withColor(0x55ffff).withBold(true).withItalic(true).withClickEvent(click).withHoverEvent(hover).withInsertion("raw");
        String value = "Алмазная кирка Health +120  😀 ♥";
        var original = FormattedCharSequence.forward(value, rich);
        check("default mode identity", TooltipFont.apply(original, TooltipFont.Choice.ORIGINAL) == original);
        var unicode = TooltipFont.apply(original, TooltipFont.Choice.UNIFORM);
        unicode.accept((i, style, cp) -> {
            boolean icon = cp == 0xe008 || cp == 0x1f600 || cp == 0x2665;
            check("glyph unchanged", value.codePointAt(i) == cp);
            check("icons retain font", style.getFont().equals(icon ? FontDescription.DEFAULT : UNIFORM));
            check("style only changes font", style.equals(icon ? rich : rich.withFont(UNIFORM)));
            check("actions intact", style.getClickEvent() == click && style.getHoverEvent() == hover);
            return true;
        });
        check("text stays identical", plain(unicode).equals(value) && plain(original).equals(value));
        int[] calls = {0};
        check("sink stops early", !unicode.accept((i, s, cp) -> { calls[0]++; return false; }) && calls[0] == 1);
        var customStyle = rich.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("skyblock", "icons")));
        var custom = TooltipFont.apply(FormattedCharSequence.forward("А A ", customStyle), TooltipFont.Choice.UNIFORM);
        custom.accept((i, s, cp) -> { check("custom font preserved", s == customStyle); return true; });
        var vanilla = TooltipFont.apply(unicode, TooltipFont.Choice.DEFAULT);
        vanilla.accept((i, s, cp) -> {
            check("default switch reversible", s.withFont(FontDescription.DEFAULT).equals(rich.withFont(FontDescription.DEFAULT)));
            return true;
        });
        var interFont = new FontDescription.Resource(Identifier.fromNamespaceAndPath("witherskyru", "inter_medium"));
        var inter = TooltipFont.apply(original, TooltipFont.Choice.INTER_MEDIUM);
        inter.accept((i, s, cp) -> {
            boolean icon = cp == 0xe008 || cp == 0x1f600 || cp == 0x2665;
            check("Inter style and symbols", s.equals(icon ? rich : rich.withFont(interFont)));
            return true;
        });
        TooltipFont.apply(inter, TooltipFont.Choice.UNIFORM).accept((i, s, cp) -> {
            boolean icon = cp == 0xe008 || cp == 0x1f600 || cp == 0x2665;
            check("Inter switches back", s.equals(icon ? rich : rich.withFont(UNIFORM)));
            return true;
        });
        var font = new MetricsFont();
        var sample = TooltipFont.apply(FormattedCharSequence.forward("Алмазная кирка Здоровье Защита", rich), TooltipFont.Choice.UNIFORM);
        for (int width = 8; width < 150; width += 3) {
            var lines = TooltipTextFit.wrap(font, sample, width);
            for (var line : lines) {
                check("selected metrics before wrapping", font.width(line) <= width);
                line.accept((i, s, cp) -> { check("font after wrapping", s.getFont().equals(UNIFORM)); return true; });
            }
            check("content survives wrap", String.join("", lines.stream().map(TestTooltipFont::plain).toList()).replace(" ", "")
                .equals(plain(sample).replace(" ", "")));
        }
        Files.createDirectories(Path.of(args[0]));
        Path directory = Files.createTempDirectory(Path.of(args[0]), "font-tests-");
        Path file = directory.resolve("settings.properties");
        Properties settings = new Properties();
        settings.setProperty("enabled", "true"); settings.setProperty("recordUnknown", "false"); settings.setProperty("unknownFutureKey", "preserve");
        try (var out = Files.newOutputStream(file)) { settings.store(out, "isolated test"); }
        TooltipFont.load(file, settings);
        check("missing field safe", TooltipFont.choice() == TooltipFont.Choice.ORIGINAL);
        TooltipFont.save(TooltipFont.Choice.INTER_MEDIUM, true);
        Properties interSaved = new Properties(); try (var in = Files.newInputStream(file)) { interSaved.load(in); }
        TooltipFont.load(file, interSaved);
        check("Inter choice reloads", TooltipFont.choice() == TooltipFont.Choice.INTER_MEDIUM && interSaved.getProperty("tooltipFont").equals("inter_medium"));
        TooltipFont.save(TooltipFont.Choice.UNIFORM, false);
        check("live font", TooltipFont.choice() == TooltipFont.Choice.UNIFORM);
        check("live wrap", !SkyRuClient.tooltipWrap());
        Properties persisted = new Properties(); try (var in = Files.newInputStream(file)) { persisted.load(in); }
        check("unrelated preserved", persisted.getProperty("unknownFutureKey").equals("preserve") && persisted.getProperty("recordUnknown").equals("false"));
        check("saved", persisted.getProperty("tooltipFont").equals("uniform") && persisted.getProperty("fitTooltipText").equals("false"));
        TooltipFont.load(file, persisted); check("restart choice", TooltipFont.choice() == TooltipFont.Choice.UNIFORM);
        Path broken = directory.resolve("directory-is-not-file"); Files.createDirectory(broken);
        TooltipFont.load(broken, persisted);
        try { TooltipFont.save(TooltipFont.Choice.DEFAULT, true); throw new AssertionError("expected save error"); }
        catch (java.io.IOException expected) { check("failure leaves live state", TooltipFont.choice() == TooltipFont.Choice.UNIFORM && !SkyRuClient.tooltipWrap()); }
        check("failure leaves target", Files.isDirectory(broken));
        TooltipFont.load(file, persisted); TooltipFont.save(TooltipFont.Choice.ORIGINAL, true);
        try (var files = Files.list(directory)) { check("no temporary residue", files.noneMatch(p -> p.getFileName().toString().endsWith(".tmp"))); }
        System.out.println("PASS: tooltip fonts " + checks + " assertions");
    }
}
