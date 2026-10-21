import ru.wither.skyru.OfflineDictionary;
import ru.wither.skyru.SkyRuClient;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class TestOffline {
  private static int passed=0;
  static void eq(String name, String actual, String expected) {
    if (!actual.equals(expected)) throw new AssertionError(name + " expected=["+expected+"] actual=["+actual+"]");
    passed++;
  }
  static void yes(String name, boolean condition) {if (!condition)throw new AssertionError(name); passed++;}
  public static void main(String[] args) throws Exception {
    Path dir=Files.createTempDirectory("witherskyru-test");
    OfflineDictionary db=new OfflineDictionary(dir);
    yes("phrase count",db.phraseCount()>=1100);
    eq("zone",db.translate("Dwarven Mines"),"Дварфские шахты");
    Files.writeString(dir.resolve("overrides.tsv"), "Dwarven Mines\tШахты дворфов\n");
    OfflineDictionary custom=new OfflineDictionary(dir);
    eq("override may replace shipped word",custom.translate("Dwarven Mines"),"Шахты дворфов");
    Files.delete(dir.resolve("overrides.tsv"));
    eq("menu",db.translate("Your Bags"),"Ваши сумки");
    eq("keep name",db.translate("Mithril"),"Mithril");
    eq("event",db.translate("SEASON OF JERRY"),"СЕЗОН ДЖЕРРИ");
    eq("numbers",db.translate("Mine 350 Mithril in the Dwarven Mines."),"Добудьте 350 Mithril в Дварфских шахтах.");
    eq("header+stats",db.translate("Purse: 245,449"),"Кошелёк: 245,449");
    eq("unknown safe",db.translate("The totally unknown item ZX-9921 must not be invented."),"The totally unknown item ZX-9921 must not be invented.");
    eq("click-safe URL",db.translate("https://hypixel.net/skyblock"),"https://hypixel.net/skyblock");
    eq("multi-line",db.translate("Your Bags\nDwarven Mines"),"Ваши сумки\nДварфские шахты");
    eq("style-code",db.translate("§aDwarven Mines"),"§aДварфские шахты");
    eq("color-rich unknown",db.translate("Hello §c Jerry"),"Hello §c Jerry");
    eq("player-like",db.translate("/warp hub"),"/warp hub");
    // Verify mod entrypoint behaves with a connected Hypixel server.
    new SkyRuClient().onInitializeClient();
    eq("mixin text bridge",SkyRuClient.text("Dwarven Mines"),"Дварфские шахты");
    Style clickAndHover = Style.EMPTY
        .withClickEvent(new net.minecraft.network.chat.ClickEvent.RunCommand("/warp dwarves"))
        .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(Component.literal("inspect item")))
        .withColor(0x55FF55);
    Component styled = Component.literal("Dwarven Mines").setStyle(clickAndHover);
    Component rendered = SkyRuClient.component(styled);
    eq("styled literal",rendered.getString(),"Дварфские шахты");
    yes("click hover style identity",rendered.getStyle()==clickAndHover);
    Component nested = Component.literal("").append(Component.literal("Dwarven Mines").setStyle(clickAndHover));
    Component nestedRU = SkyRuClient.component(nested);
    eq("nested literal",nestedRU.getString(),"Дварфские шахты");
    yes("nested event intact",nestedRU.getSiblings().get(0).getStyle()==clickAndHover);
    System.out.println("PASS: "+passed+" / "+passed+" assertions; phrases="+db.phraseCount()+"; patterns="+db.patternCount());
  }
}
