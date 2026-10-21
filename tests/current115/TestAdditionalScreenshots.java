import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.*;
import net.minecraft.util.StringDecomposer;
import ru.wither.skyru.*;

/** User screenshot expectations and actual public NPC colour runs; no client launch. */
public class TestAdditionalScreenshots {
    private static int checks;
    private static void check(String name, boolean condition) { if (!condition) throw new AssertionError(name); checks++; }
    private static Style styleAt(Component text, String word) {
        int wanted = text.getString().replaceAll("(?i)§[0-9a-fk-or]", "").indexOf(word); int[] at = {0}; Style[] found = {null};
        StringDecomposer.iterateFormatted(text, Style.EMPTY, (i, style, cp) -> {
            if (at[0] == wanted) found[0] = style;
            at[0] += Character.charCount(cp); return true;
        }); return found[0];
    }
    public static void main(String[] args) throws Exception {
        var db = new OfflineDictionary(Path.of(args[0])); db.setRecordUnknown(false);
        var chat = new ChatNotificationTranslator(db);
        String[][] messages = {
            {"Your Agronomy Sack is full of Seeds!", "Ваш мешок для урожая заполнен семенами!"},
            {"§6§lGROSS! §7A §2 Pest §7has appeared in §aPlot §7- §b4§7!", "ФУ!  Вредитель появился на участке № 4!"},
            {" ☘ Farming Fortune has been reduced by 5%!", " ☘ Удача фермерства снижена на 5%!"},
            {"CLICK HERE to teleport to the plot!", "НАЖМИТЕ ЗДЕСЬ, чтобы переместиться на участок!"},
            {"Inventory full! Don't forget to check out your Storage inside the SkyBlock Menu!", "Инвентарь заполнен! Загляните в хранилище в меню SkyBlock!"},
            {"Inventory full? Don't forget to check out your Storage inside the SkyBlock Menu!", "Инвентарь заполнен? Загляните в хранилище в меню SkyBlock!"},
            {"You cannot transfer anything into sacks!", "Не удаётся переложить предметы в мешки!"},
            {"[SkyHanni] The Broodmother was killed!", "[SkyHanni] Матка пауков убита!"},
            {"[Sacks] +1,635 items, -321 items. (Last 30s.)", "[Мешки] +1,635 предметов, -321 предмет. (За 30 с.)"},
            {"[Sacks] +895 items, -181 items. (Last 16s.)", "[Мешки] +895 предметов, -181 предмет. (За 16 с.)"}
            ,{"BESTIARY Beetle I", "БЕСТИАРИЙ Жук I"}
            ,{"RARE DROP! Cicada Symphony Vinyl (+1.5)", "РЕДКАЯ ДОБЫЧА! Пластинка «Симфония цикад» (+1.5)"}
            ,{"RARE DROP! Enchanted Melon x15 (+4)", "РЕДКАЯ ДОБЫЧА! Зачарованный арбуз x15 (+4)"}
        };
        for (String[] pair : messages) {
            Component input = Component.literal(pair[0]);
            Component output = chat.translate(input, GuiMessageSource.SYSTEM_SERVER, false);
            check("chat " + output.getString(), output.getString().equals(pair[1]));
            check("signed not changed", chat.translate(input, GuiMessageSource.SYSTEM_SERVER, true) == input);
            check("PLAYER not changed", chat.translate(input, GuiMessageSource.PLAYER, false) == input);
            check("original retained", input.getString().equals(pair[0]));
        }
        var warp = new ClickEvent.RunCommand("/tptoplot 4");
        Component action = Component.empty().append(Component.literal("CLICK HERE").setStyle(Style.EMPTY.withBold(true).withClickEvent(warp)))
            .append(Component.literal(" to teleport to the plot!"));
        Component actionRu = chat.translate(action, GuiMessageSource.SYSTEM_SERVER, false);
        check("warp click retained", styleAt(actionRu, "НАЖМИТЕ").getClickEvent() == warp);
        check("warp label bold", Boolean.TRUE.equals(styleAt(actionRu, "НАЖМИТЕ").isBold()));
        Style rareItem = Style.EMPTY.withColor(0x55ff55).withHoverEvent(new HoverEvent.ShowText(Component.literal("Original hover")));
        Style rareCount = Style.EMPTY.withColor(0xffff55);
        Component compact = Component.empty().append(Component.literal("RARE DROP! ").withColor(0xffaa00))
            .append(Component.literal("Enchanted Melon").setStyle(rareItem))
            .append(Component.literal(" x15").setStyle(rareCount)).append(Component.literal(" (+4)").withColor(0x55ffff));
        Component compactRu = chat.translate(compact, GuiMessageSource.SYSTEM_SERVER, false);
        check("compact drop item hover/style", styleAt(compactRu, "Зачарованный").equals(rareItem));
        check("compact drop quantity style", styleAt(compactRu, "x15").equals(rareCount));
        check("compact drop stat glyph preserved", compactRu.getString().endsWith("(+4)"));
        var npc = new NpcDialogueTranslator();
        Map<String,JsonObject> records = new HashMap<>();
        for (var raw : JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("NPC_DIALOGUES_RU.json"))).getAsJsonObject().getAsJsonArray("records")) {
            var row = raw.getAsJsonObject(); records.put(row.get("source").getAsString(), row);
        }
        var samples = JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("NPC_0.15.14_STYLED_SAMPLES.json"))).getAsJsonArray();
        check("17 observed NPC samples", samples.size() == 17);
        for (var raw : samples) {
            var row = raw.getAsJsonObject(); String source = row.get("source").getAsString(), expected = row.get("translation").getAsString();
            Component input = Component.literal(source), output = npc.translate(input);
            check("actual styled NPC " + output.getString(), output.getString().equals(expected));
            check("NPC source unchanged", input.getString().equals(source));
            check("NPC timestamp", npc.translate(Component.literal("[06:06:49] " + source)).getString().equals("[06:06:49] " + expected));
            var record = records.get(source.replaceAll("(?i)§[0-9a-fk-or]", ""));
            if (record.has("style_anchors")) for (var anchor : record.getAsJsonArray("style_anchors")) {
                String en = anchor.getAsJsonObject().get("source").getAsString(), ru = anchor.getAsJsonObject().get("target").getAsString();
                check("actual NPC anchor style " + en, Objects.equals(styleAt(input, en), styleAt(output, ru)));
            }
        }
        check("Jeff independent", npc.translate(Component.literal("[NPC] Jeff: Well, well! You got yourself a real Farming Tool, huh?")).getString()
            .equals("[NPC] Jeff: Ну надо же! Обзавёлся настоящим фермерским инструментом, да?"));
        check("Bramass independent", npc.translate(Component.literal("[NPC] Bramass Beastslayer: Killing enough mobs in a given Family unlocks rewards.")).getString()
            .equals("[NPC] Bramass Beastslayer: Убив достаточно мобов одного семейства, ты разблокируешь награды."));
        String[][] prices = {
            {"Hold SHIFT for stack total (32 items)", "Удерживайте Shift: итог стопки (32 шт.)"},
            {"Hold", "Удерживайте"}, {"for stack total (", "для итога стопки ("},
            {"32 items)", "32 шт.)"}, {"(960.0 each)", "(960.0 за шт.)"},
            {"55.11M (1.72M each)", "55.11M (1.72M за шт.)"}
        };
        for (String[] pair : prices) check("price " + pair[0], db.translateForSearch(pair[0]).equals(pair[1]));
        var names = SignSearchNames.load(db);
        check("Enchanted Wheat original", names.suggestions("зачарованная пшеница", 5).contains("Enchanted Wheat"));
        check("Wheat original", names.suggestions("пшеница", 5).contains("Wheat"));
        check("case and ё normalization", names.suggestions("  ЗАЧАРОВАННАЯ   ПШЕНИЦА  ", 5).contains("Enchanted Wheat"));
        for (String input : List.of("/bz пшеница", "пшеница; /op test", "", "Wheat", "неизвестныйпредметZX999"))
            check("unsafe/English/unknown query untouched", names.suggestions(input, 5).isEmpty());
        var ambiguous = new SignSearchNames(List.of("Alpha", "Beta"), s -> "Тестовый предмет");
        check("ambiguous originals both offered", ambiguous.suggestions("тестовый предмет", 5).equals(List.of("Alpha", "Beta")));
        check("suggestion bound", names.suggestions("зачар", 5).size() <= 5);
        var generous = new SignSearchNames(List.of("Wheat", "Enchanted Wheat", "Alpha", "Beta"), s -> s.equals("Wheat") ? "Пшеница" : s.equals("Enchanted Wheat") ? "Зачарованная пшеница" : "Тестовый предмет");
        check("exact original wins over broader variants", generous.suggestions("пшеница", 5).equals(List.of("Wheat")));
        check("zero suggestion limit", generous.suggestions("пшеница", 0).isEmpty());
        check("unknown all preserved", generous.suggestions("пшеницазз", 5).isEmpty());
        System.out.println("PASS: additional screenshots/sign-name suggestions " + checks + " assertions");
    }
}
