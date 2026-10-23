import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;
import ru.wither.skyru.OfflineDictionary;
import ru.wither.skyru.SkyRuClient;
import ru.wither.skyru.TooltipTranslation;

/** Regressions for multi-style Figstone lore, live values and item-scoped matching. */
public final class TestFigstoneLore {
    private static int checks;
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    private static void flag(String name, Object value) throws Exception {
        var f = SkyRuClient.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }
    private static FormattedCharSequence glyphs(Component component) {
        return sink -> StringDecomposer.iterateFormatted(component, Style.EMPTY, sink);
    }
    private static String plain(FormattedCharSequence value) {
        var out = new StringBuilder();
        value.accept((i,style,point) -> { out.appendCodePoint(point); return true; });
        return out.toString();
    }
    private static MutableComponent styled(Object... parts) {
        MutableComponent result = Component.empty();
        for (int i=0;i<parts.length;i+=2) {
            result.append(Component.literal((String)parts[i]).setStyle((Style)parts[i+1]));
        }
        return result;
    }
    public static void main(String[] args) throws Exception {
        Path config = Path.of(args[0]); Files.createDirectories(config);
        var dictionary = new OfflineDictionary(config); dictionary.setRecordUnknown(false);
        flag("dictionary",dictionary); flag("enabled",true); flag("onlyHypixel",false);

        var normal = Style.EMPTY.withColor(0xaaaaaa).withInsertion("prose");
        var special = Style.EMPTY.withColor(0x55ff55).withBold(true)
            .withClickEvent(new ClickEvent.RunCommand("/preserve-sweep"))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("preserve-hover")));
        var numeric = Style.EMPTY.withColor(0x11ffaa).withInsertion("numeric");
        var rows = List.<Component>of(
            Component.literal("Figstone Splitter"),
            styled("Gains ",normal,"1",numeric," ",normal,"✿ ",special,"Sweep",special,
                " every ",normal,"10,000",numeric," logs",normal),
            styled("cut on Galatea. (max ",normal,"20",numeric,")",normal),
            styled("Logs Cut: ",normal,"24,377",numeric),
            styled("Throwing your Axe has no ",normal,"✿ ",special,"Sweep",special),
            styled("penalty for ",normal,"10",numeric,"s.",normal)
        );
        var expected = List.of(
            "Figstone Splitter",
            "Даёт +1 ✿ размаха за каждые 10,000 брёвен",
            "на Галатее (макс. +20).",
            "Срублено брёвен: 24,377",
            "После броска топора штраф к ✿ размаху",
            "не действует в течение 10 с."
        );
        var before = rows.stream().map(Component::getString).toList();
        var result = TooltipTranslation.components(rows,false);
        for (int i=0;i<rows.size();i++)
            check(result.get(i).getString().equals(expected.get(i)),
                "component "+i+": "+result.get(i).getString());
        check(rows.stream().map(Component::getString).toList().equals(before),
            "original tooltip components untouched");
        List<FormattedCharSequence> sequences = rows.stream().map(TestFigstoneLore::glyphs).toList();
        var translated = TooltipTranslation.styled(sequences,false);
        for (int i=0;i<rows.size();i++)
            check(plain(translated.get(i)).equals(expected.get(i)),
                "styled "+i+": "+plain(translated.get(i)));
        var numericStyleObserved = new boolean[1];
        var sweepStyleObserved = new boolean[1];
        translated.get(1).accept((i,style,cp) -> {
            if(style.equals(numeric)) numericStyleObserved[0] = true;
            if(style.equals(special)) sweepStyleObserved[0] = true;
            return true;
        });
        check(numericStyleObserved[0], "live numeric value style preserved");
        check(sweepStyleObserved[0], "Sweep icon/style/actions preserved");
        var unknown = new ArrayList<>(rows);
        unknown.set(0, Component.literal("Unrelated Axe"));
        var unrelated = TooltipTranslation.components(unknown,false);
        check(!unrelated.get(3).getString().equals(expected.get(3)),
            "unrelated item's similar tooltip is not rewritten");
        System.out.println("PASS Figstone lore: "+checks+" assertions");
    }
}
