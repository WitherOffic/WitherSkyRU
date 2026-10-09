import java.nio.file.*;
import java.util.List;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import ru.wither.skyru.*;

/** Real JAR dictionary, grammatical boundaries and styled NPCs; no game startup. */
public class TestEditorialQuality {
    private static int checks;
    private static void check(String name, boolean ok) {
        if (!ok) throw new AssertionError(name);
        checks++;
    }
    private static void eq(OfflineDictionary db, String source, String target) {
        String got = db.translate(source);
        check(source + " => " + got + " != " + target, got.equals(target));
    }
    private static void flag(String name, Object value) throws Exception {
        var field = SkyRuClient.class.getDeclaredField(name);
        field.setAccessible(true); field.set(null, value);
    }
    public static void main(String[] args) throws Exception {
        Path sourceDir = Path.of(args[1]);
        var db = new OfflineDictionary(Path.of(args[0])); db.setRecordUnknown(false);
        var map = JsonParser.parseString(Files.readString(sourceDir.resolve("translations.json"))).getAsJsonObject().getAsJsonObject("translations");
        check("loaded count", db.phraseCount() == map.size());
        var field = OfflineDictionary.class.getDeclaredField("exact"); field.setAccessible(true);
        @SuppressWarnings("unchecked") var loaded = (java.util.Map<String,String>)field.get(db);
        for (var row : map.entrySet()) {
            String source = row.getKey(), target = row.getValue().getAsString();
            check("stored exact pair", target.equals(loaded.get(source)));
            // Numeric-only displays intentionally bypass translation (SAFE policy).
            if (source.matches("(?s).*[\\p{L}].*") && !source.startsWith("/")) eq(db, source, target);
        }
        eq(db, "RIGHT CLICK Item Ability. Has a 5s\ncooldown.", "способности предмета по ПКМ. 5 с\nперезарядки.");
        eq(db, "healing by 40% for 4 seconds.", "исцеление на 40% на 4 секунды.");
        for (String n : List.of("5", "10", "20")) eq(db, n + " seconds.", n + " секунд.");
        for (int n = 1; n <= 250; n++) {
            int last = n % 100;
            int form = last >= 11 && last <= 14 ? 2 : last % 10 == 1 ? 0 : last % 10 >= 2 && last % 10 <= 4 ? 1 : 2;
            String nom = new String[]{"монета","монеты","монет"}[form];
            String acc = new String[]{"монету","монеты","монет"}[form];
            String gen = new String[]{"монеты","монет","монет"}[form];
            eq(db, "+" + n + " Coins", "+" + n + " " + nom);
            eq(db, "Price: " + n + " Coins", "Цена: " + n + " " + nom);
            eq(db, "You received " + n + " coins!", "Вы получили " + n + " " + acc + "!");
            eq(db, "You need " + n + " more coins!", "Вам не хватает " + n + " " + gen + "!");
            eq(db, "healing by 39% for " + n + " seconds.", "исцеление на 39% на " + n + " " + new String[]{"секунду","секунды","секунд"}[form] + ".");
        }
        eq(db, "Price: 0.5 Coins", "Цена: 0.5 монеты");
        eq(db, "Price: 1,001 Coins", "Цена: 1,001 монета");
        eq(db, "Price: 1,011 Coins", "Цена: 1,011 монет");
        eq(db, "+2 coins per kill", "+2 монеты за убийство");
        eq(db, "Costs 1 coin.", "Стоит 1 монету.");
        eq(db, "COMMON Enchanted Book", "COMMON Зачарованная книга");
        eq(db, "§fCOMMON", "§fCOMMON");
        eq(db, "[Common]", "[Common]");
        // Notification bodies are translated by the signed/channel-safe chat handler.
        eq(db, "RARE DROP!", "RARE DROP!");
        eq(db, "Talk to Jeff in your Barn", "Поговорите с Jeff в своём амбаре");
        Path authored = sourceDir.getParent().resolve("src/main/resources/data/witherskyru/editorial-numeric.tsv");
        for (String line : Files.readAllLines(authored)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] pair = line.split("\\t", 2);
            for (String number : List.of("7331", "1", "{future}")) {
                String en = pair[0], ru = pair[1];
                for (int i = 0; i < 4; i++) {
                    en = en.replace("{N" + i + "}", number).replace("{I" + i + "}", " ");
                    ru = ru.replace("{N" + i + "}", number).replace("{I" + i + "}", " ");
                }
                eq(db, en, TranslationGrammar.polish(ru));
            }
        }
        check("vanilla tooltip fits scope", TooltipTextFit.eligibleFrames(List.of("net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip#extractText")));
        check("custom tooltip fits scope", TooltipTextFit.eligibleFrames(List.of("some.mod.TooltipRenderer#draw")));
        check("chat excluded", !TooltipTextFit.eligibleFrames(List.of("net.minecraft.client.gui.components.ChatComponent#render", "some.mod.TooltipRenderer#draw")));
        check("inventory normal text excluded", !TooltipTextFit.eligibleFrames(List.of("net.minecraft.client.gui.screens.inventory.InventoryScreen#render")));
        check("tooltip measurements excluded", !TooltipTextFit.eligibleFrames(List.of("some.mod.TooltipComponent#getWidth")));
        check("renderer measurements excluded", !TooltipTextFit.eligibleFrames(List.of("some.mod.TooltipRenderer#getWidth")));
        var npc = new NpcDialogueTranslator();
        var npcRows = JsonParser.parseString(Files.readString(sourceDir.resolve("NPC_DIALOGUES_RU.json"))).getAsJsonObject().getAsJsonArray("records");
        check("NPC count", npc.phraseCount() == npcRows.size());
        for (var raw : npcRows) {
            var row = raw.getAsJsonObject();
            String en = row.get("source").getAsString(), ru = row.get("translation").getAsString();
            check("exact NPC", npc.translate(Component.literal(en)).getString().equals(ru));
            check("timestamp NPC", npc.translate(Component.literal("[04:43:26] " + en)).getString().equals("[04:43:26] " + ru));
        }
        var observed = JsonParser.parseString(Files.readString(sourceDir.resolve("SERVER_0.15_NPC_OBSERVED.json"))).getAsJsonArray();
        for (var raw : observed) {
            var row = raw.getAsJsonObject();
            String source = row.get("source").getAsString(), expectedNpc = null;
            for (var r : npcRows) if (r.getAsJsonObject().get("source").getAsString().equals(source))
                expectedNpc = r.getAsJsonObject().get("translation").getAsString();
            check("observed NPC has authored translation", expectedNpc != null);
            for (var sample : row.getAsJsonArray("styled_samples")) {
                String originalText = sample.getAsString();
                String plain = originalText.replaceAll("(?i)§[0-9a-fk-or]", "");
                String prefix = plain.matches("^\\[\\d{2}:\\d{2}:\\d{2}\\] .*" ) ? plain.substring(0,11) : "";
                Component original = Component.literal(originalText);
                check("actual 0.15 styled NPC", npc.translate(original).getString().equals(prefix + expectedNpc));
                check("actual 0.15 original preserved", original.getString().equals(originalText));
            }
        }
        Component unknownNpc = Component.literal("[NPC] Jeff: Unseen dialogue ZX-9921!");
        check("unseen NPC is not guessed", npc.translate(unknownNpc) == unknownNpc);
        eq(db, "The totally unknown ZX-9921 Pet", "The totally unknown ZX-9921 Pet");
        var samples = JsonParser.parseString(Files.readString(sourceDir.resolve("NPC_JEFF_0.14.3_STYLED_SAMPLES.json"))).getAsJsonObject().getAsJsonArray("samples");
        for (var raw : samples) {
            String en = raw.getAsString(), plain = en.replaceAll("(?i)§[0-9a-fk-or]", "");
            String body = plain.substring(11);
            String target = null;
            for (var row : npcRows) if (row.getAsJsonObject().get("source").getAsString().equals(body))
                target = row.getAsJsonObject().get("translation").getAsString();
            check("Jeff log sample exists", target != null);
            Component original = Component.literal(en);
            check("real styled Jeff", npc.translate(original).getString().equals(plain.substring(0,11) + target));
            check("real styled source intact", original.getString().equals(en));
        }
        var click = new ClickEvent.RunCommand("/warp garden");
        Style toolStyle = Style.EMPTY.withColor(0xffff55).withClickEvent(click);
        Component rich = Component.literal("[04:43:30] ").withColor(0xff55ff)
            .append(Component.literal("[NPC] Jeff: ").withColor(0x55ffff))
            .append(Component.literal("Have you picked up a ").withColor(0xffffff))
            .append(Component.literal("Specialized Farming Tool").setStyle(toolStyle))
            .append(Component.literal(" yet?").withColor(0xffffff));
        Component translated = npc.translate(rich);
        check("Jeff style translation", translated != rich && translated.getString().contains("специализированным фермерским инструментом"));
        check("original rich NPC intact", rich.getString().endsWith("Tool yet?"));
        boolean[] preserved = {false};
        net.minecraft.util.StringDecomposer.iterateFormatted(translated, Style.EMPTY, (i, style, cp) -> {
            if (style.equals(toolStyle)) preserved[0] = true; return true;
        });
        check("semantic anchor style/event intact", preserved[0]);
        flag("dictionary", db); flag("npcDialogues", npc); flag("onlyHypixel", false);
        flag("translateNpcChat", true);
        var queue = new GuiMessage(33, rich, null, GuiMessageSource.SYSTEM_SERVER, null);
        check("timestamped Jeff queue", SkyRuClient.npcChat(queue).content().getString().equals(translated.getString()));
        check("player Jeff imitation untouched", SkyRuClient.npcChat(new GuiMessage(33, rich, null, GuiMessageSource.PLAYER, null)).content() == rich);
        flag("translateNpcChat", false);
        check("NPC switch", SkyRuClient.npcChat(queue) == queue);
        System.out.println("PASS: editorial quality " + checks + " assertions");
    }
}
