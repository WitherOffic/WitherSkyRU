import java.util.*;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;
import ru.wither.skyru.*;

/** Native word splitter with synthetic glyph metrics: no game, display, assets, or server. */
public class TestTooltipReadability {
    private static int checks;
    private static void check(String message, boolean condition) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
    private static String plain(FormattedCharSequence sequence) {
        StringBuilder out = new StringBuilder();
        sequence.accept((i, style, cp) -> { out.appendCodePoint(cp); return true; });
        return out.toString();
    }
    private static String withoutSpaces(String s) { return s.replaceAll("\\s", ""); }
    private static final class MetricsFont extends Font {
        private final StringSplitter splitter = new StringSplitter((cp, style) -> style.isBold() ? 6 : 5);
        MetricsFont() { super(null); }
        @Override public int width(FormattedCharSequence sequence) { return (int)Math.ceil(splitter.stringWidth(sequence)); }
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
    public static void main(String[] args) {
        Font font = new MetricsFont();
        String hint = "Нажмите, чтобы переместиться туда!";
        var source = FormattedCharSequence.forward(hint, Style.EMPTY.withColor(0xffff55));
        var wrapped = TooltipTextFit.wrap(font, source, 80);
        check("long translated hint wraps", wrapped.size() > 1);
        check("hint retains meaning", withoutSpaces(String.join("", wrapped.stream().map(TestTooltipReadability::plain).toList())).equals(withoutSpaces(hint)));
        for (var row : wrapped) check("wrapped hint fits without scaling", font.width(row) <= 80);
        var original = new ClientTextTooltip(source);
        var tooltip = new TooltipTextFit.PreparedText(original, wrapped);
        check("height grows before drawing", tooltip.getHeight(font) == wrapped.size() * 10);
        check("background width follows wrapped text", tooltip.getWidth(font) <= 80);
        check("hand visibility flag retained", tooltip.showTooltipWithItemInHand() == original.showTooltipWithItemInHand());

        var command = new ClickEvent.RunCommand("/warp garden");
        var hover = new HoverEvent.ShowText(Component.literal("unchanged hover"));
        Style rich = Style.EMPTY.withColor(0xaa00aa).withBold(true).withClickEvent(command).withHoverEvent(hover);
        String title = "Зачарованная книга 4  😀";
        var decorated = FormattedCharSequence.composite(
            FormattedCharSequence.forward("COMMON ", Style.EMPTY.withColor(0xffffff)),
            FormattedCharSequence.forward(title, rich));
        var richRows = TooltipTextFit.wrap(font, decorated, 70);
        for (var row : richRows) {
            check("styled wrapped row fits", font.width(row) <= 70);
            row.accept((i, style, cp) -> {
                if (style.isBold()) {
                    check("semantic style preserved", style.equals(rich));
                    check("click preserved", style.getClickEvent() == command);
                    check("hover preserved", style.getHoverEvent() == hover);
                }
                check("no surrogate treated as standalone glyph", cp < 0xd800 || cp > 0xdfff);
                return true;
            });
        }
        String rebuilt = String.join("", richRows.stream().map(TestTooltipReadability::plain).toList());
        check("rarity, digits, icon and emoji retained", withoutSpaces(rebuilt).equals(withoutSpaces("COMMON " + title)));
        check("source still original", plain(decorated).equals("COMMON " + title));

        for (int width = 6; width <= 300; width++) {
            for (String text : List.of(hint, "Кошелёк: 123,456.78", "COMMON Зачарованная книга", "Сверхдлинноесловобезпробелов", "😀  4", "строка\n\nещё строка")) {
                var lines = TooltipTextFit.wrap(font, FormattedCharSequence.forward(text, Style.EMPTY), width);
                check("layout has at least one line", !lines.isEmpty());
                for (var line : lines) check("layout stays within width", font.width(line) <= width);
                check("no content lost during wrapping", withoutSpaces(String.join("", lines.stream().map(TestTooltipReadability::plain).toList())).equals(withoutSpaces(text)));
            }
        }
        check("blank row retains height", TooltipTextFit.wrap(font, FormattedCharSequence.EMPTY, 80).size() == 1);
        check("explicit blank line retained", TooltipTextFit.wrap(font, FormattedCharSequence.forward("a\n\nb", Style.EMPTY), 100).size() == 3);
        check("screen margin and cap", TooltipTextFit.wrapWidth(256) == 232 && TooltipTextFit.wrapWidth(1920) == 360);
        check("narrow screen never gets negative width", TooltipTextFit.wrapWidth(10) == 1);
        List<ClientTooltipComponent> custom = List.of(new ClientTooltipComponent() {
            public int getHeight(Font ignored) { return 20; }
            public int getWidth(Font ignored) { return 80; }
        });
        check("unknown image/custom component is not replaced", TooltipTextFit.prepare(font, custom, 256) == custom);
        check("fixed tooltip must not shrink to unreadable size", !HudTextFit.canFit(50, font.width(source)));
        check("normal render starts outside prepared scope", !TooltipTextFit.renderingPrepared());
        TooltipTextFit.renderPrepared(() -> {
            check("prepared rendering bypasses draw-time translation", TooltipTextFit.renderingPrepared());
            TooltipTextFit.renderPrepared(() -> check("nested scope stays prepared", TooltipTextFit.renderingPrepared()));
            check("outer scope survives nested render", TooltipTextFit.renderingPrepared());
        });
        check("scope removed after rendering", !TooltipTextFit.renderingPrepared());
        try { TooltipTextFit.renderPrepared(() -> { throw new IllegalStateException("expected"); }); }
        catch (IllegalStateException expected) { check("expected exception", expected.getMessage().equals("expected")); }
        check("scope removed on exception", !TooltipTextFit.renderingPrepared());
        System.out.println("PASS: tooltip readability " + checks + " assertions");
    }
}
