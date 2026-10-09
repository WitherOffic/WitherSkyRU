import java.nio.file.Path;
import java.util.Optional;
import ru.wither.skyru.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringDecomposer;

/** Tests real styled Components and display-queue policy without starting Minecraft. */
public class TestChatNotifications {
    private static int passed;
    private static ChatNotificationTranslator translator;
    private static void check(String name, boolean condition) {
        if (!condition) throw new AssertionError(name);
        passed++;
    }
    private static Component translate(Component c) { return translator.translate(c, GuiMessageSource.SYSTEM_SERVER, false); }
    private static void eq(String source, String expected) {
        Component input = Component.literal(source);
        String result = translate(input).getString();
        check(source + " => " + result, result.equals(expected));
        check("original unchanged", input.getString().equals(source));
    }
    private static void flag(String name, Object value) throws Exception {
        var f = SkyRuClient.class.getDeclaredField(name); f.setAccessible(true); f.set(null, value);
    }
    private static Style styleAt(Component component, String word) {
        int target = component.getString().indexOf(word);
        int[] offset = {0}; Style[] result = {null};
        StringDecomposer.iterateFormatted(component, Style.EMPTY, (index, style, point) -> {
            if (offset[0] == target) result[0] = style;
            offset[0] += Character.charCount(point); return true;
        });
        return result[0];
    }
    public static void main(String[] args) throws Exception {
        var db = new OfflineDictionary(Path.of(args[0])); db.setRecordUnknown(false);
        translator = new ChatNotificationTranslator(db);
        check("families", translator.familyCount() == 11);
        eq("RARE DROP! Goblin Egg (+23  Magic Find)", "РЕДКАЯ ДОБЫЧА! Яйцо гоблина (+23  Шанс редкой добычи)");
        eq("[01:56:46] RARE DROP! Goblin Leggings (+23 ★ Magic Find)", "[01:56:46] РЕДКАЯ ДОБЫЧА! Поножи гоблина (+23 ★ Шанс редкой добычи)");
        eq("The [Goblin  head]Goblin was carrying a bag of coins! +6,240 Coins!", "У [Goblin  head]гоблина был мешок монет! +6,240 монет!");
        eq("The Goblin was carrying a bag of coins! +5,842 Coins!", "У гоблина был мешок монет! +5,842 монеты!");
        eq("The Goblin was carrying a bag of coins! +1 Coins!", "У гоблина был мешок монет! +1 монета!");
        eq("The sound of pickaxes clashing against the rock has attracted the attention of the POWDER GHAST!", "Стук кирок по камню привлёк внимание ПОРОШКОВОГО ГАСТА!");
        eq("Find the Powder Ghast near the Divan's Gateway!", "Найдите Порошкового гаста: Врата Дивана!");
        eq("BESTIARY [Goblin  head]Goblin IX ➡ X", "БЕСТИАРИЙ [Goblin  head]Гоблин IX ➡ X");
        eq("      BESTIARY", "      БЕСТИАРИЙ");
        eq("[SkyHanni] New rabbits are available at Hoppity's Shop!\n[CLICK to warp to hub or disable this feature]", "[SkyHanni] Новые кролики доступны в магазине Хоппити!\n[НАЖМИТЕ: перейти в хаб или отключить уведомления]");
        eq("[SkyHanni] New rabbits are available at Hoppity's Shop!\\n[CLICK to warp to hub or disable this feature]", "[SkyHanni] Новые кролики доступны в магазине Хоппити!\\n[НАЖМИТЕ: перейти в хаб или отключить уведомления]");
        eq("[Skyblocker] Found a Chocolate Dinner Egg at -93, 200, -122!", "[Skyblocker] Найдено шоколадное яйцо (ужин) по координатам -93, 200, -122!");
        eq("[Sacks] +4 items. (Last 5s.) (x2)", "[Мешки] +4 предмета. (За 5 с.) (x2)");
        eq("[Sacks] +1 items. (Last 0.5s.)", "[Мешки] +1 предмет. (За 0.5 с.)");
        eq("[Sacks] +11 items. (Last 5s.)", "[Мешки] +11 предметов. (За 5 с.)");
        eq("BESTIARY Goblin reached Tier 10!", "БЕСТИАРИЙ Гоблин: ступень 10!");
        eq("[02:01:03] You reached a 11 Kill Combo!", "[02:01:03] Вы достигли серии из 11 убийств!");
        eq("You reached a 21 Kill Combo!", "Вы достигли серии из 21 убийства!");
        eq("Your Kill Combo has expired!", "Ваша серия убийств закончилась!");
        eq("§6RARE DROP! §bGoblin Egg§b (+23  Magic Find)", "РЕДКАЯ ДОБЫЧА! Яйцо гоблина (+23  Шанс редкой добычи)");
        eq("[Skyblocker] Found a Chocolate Breakfast Egg at 1, 200, -2!", "[Skyblocker] Найдено шоколадное яйцо (завтрак) по координатам 1, 200, -2!");
        eq("[Skyblocker] Found a Chocolate Lunch Egg at 1, 200, -2!", "[Skyblocker] Найдено шоколадное яйцо (обед) по координатам 1, 200, -2!");
        eq("[04:43:31] [SkyHanni] You have 18k materials in stash, totalling 11 types. Click to pickup your stash!", "[04:43:31] [SkyHanni] В тайнике: 18k материалов, видов: 11. Нажмите, чтобы забрать вещи!");
        eq("[04:50:33] [SkyHanni] §bYou have §323k §bmaterials in stash, §btotalling §311 types§b. §bClick to §3pickup §byour stash!", "[04:50:33] [SkyHanni] В тайнике: 23k материалов, видов: 11. Нажмите, чтобы забрать вещи!");

        var hover = new HoverEvent.ShowText(Component.literal("original tooltip"));
        var click = new ClickEvent.RunCommand("/warp hub");
        Style item = Style.EMPTY.withColor(0x55ffff).withHoverEvent(hover);
        Style action = Style.EMPTY.withColor(0xffff55).withClickEvent(click);
        Component combo = Component.literal("You reached a ").withColor(0xffff55)
            .append(Component.literal("10").setStyle(action))
            .append(Component.literal(" Kill Combo!").withColor(0xffff55));
        check("combo number event retained", styleAt(translate(combo), "10").getClickEvent() == click);
        check("combo player excluded", translator.translate(combo, GuiMessageSource.PLAYER, false) == combo);
        check("combo signed excluded", translator.translate(combo, GuiMessageSource.SYSTEM_SERVER, true) == combo);
        Component unknownCombo = Component.literal("You reached a five Kill Combo!");
        check("unknown combo grammar excluded", translate(unknownCombo) == unknownCombo);
        Component rich = Component.literal("RARE DROP! ").withColor(0xffaa00)
            .append(Component.literal("Goblin Egg").setStyle(item))
            .append(Component.literal(" (+23  Magic Find)").withColor(0x55ffff));
        Component translated = translate(rich);
        check("rich rare", translated.getString().contains("Яйцо гоблина"));
        check("item hover retained", styleAt(translated, "Яйцо").getHoverEvent() == hover);
        check("item colour retained", styleAt(translated, "Яйцо").getColor().getValue() == 0x55ffff);
        Component shop = Component.literal("[SkyHanni] New rabbits are available at ")
            .append(Component.literal("Hoppity's Shop").withColor(0x55ff55))
            .append(Component.literal("!\n"))
            .append(Component.literal("[CLICK to warp to hub or disable this feature]").setStyle(action));
        Component shopRu = translator.translate(shop, GuiMessageSource.SYSTEM_CLIENT, false);
        check("local mod notification", shopRu.getString().contains("магазине Хоппити"));
        check("warp click retained", styleAt(shopRu, "[НАЖМИТЕ").getClickEvent() == click);
        check("shop colour", styleAt(shopRu, "магазине").getColor().getValue() == 0x55ff55);

        Component egg = Component.literal("[Skyblocker] Found a ")
            .append(Component.literal("Chocolate").withColor(0x55ff55))
            .append(Component.literal(" "))
            .append(Component.literal("Dinner").setStyle(action))
            .append(Component.literal(" "))
            .append(Component.literal("Egg").setStyle(item))
            .append(Component.literal(" at -93, 200, -122!").setStyle(action));
        Component eggRu = translator.translate(egg, GuiMessageSource.SYSTEM_CLIENT, false);
        check("meal event follows meaning", styleAt(eggRu, " (ужин)").getClickEvent() == click);
        check("egg hover follows meaning", styleAt(eggRu, "яйцо").getHoverEvent() == hover);
        check("coordinate action retained", styleAt(eggRu, "-93").getClickEvent() == click);

        var icon = new ObjectContents(new AtlasSprite(Identifier.parse("minecraft:gui"), Identifier.parse("minecraft:icon")), Optional.of(Component.literal("[Goblin  head]")));
        Component withIcon = Component.literal("RARE DROP! ")
            .append(MutableComponent.create(icon).setStyle(item))
            .append(Component.literal("Goblin Egg").setStyle(item))
            .append(Component.literal(" (+23  Magic Find)"));
        Component iconRu = translate(withIcon);
        check("rich icon translated", iconRu.getString().contains("Яйцо гоблина"));
        check("icon contents identity", iconRu.getSiblings().stream().anyMatch(c -> c.getContents() == icon));
        check("unknown untouched", translate(Component.literal("Unknown server notification ZX-98")).getString().equals("Unknown server notification ZX-98"));
        Component player = Component.literal("RARE DROP! Goblin Egg (+23  Magic Find)");
        check("player channel untouched", translator.translate(player, GuiMessageSource.PLAYER, false) == player);
        check("signed untouched", translator.translate(player, GuiMessageSource.SYSTEM_SERVER, true) == player);
        Component ordinary = Component.literal("[VIP] Wither: RARE DROP! Goblin Egg (+23  Magic Find)");
        check("unsigned player-shaped message untouched", translate(ordinary) == ordinary);
        Component unsupported = Component.literal("RARE ").withColor(0xff0000)
            .append(Component.literal("DROP! ").withColor(0x00ff00))
            .append(Component.literal("Goblin Egg (+23  Magic Find)"));
        check("ambiguous style fails closed", translate(unsupported) == unsupported);
        Component hostile = Component.literal("[SkyHanni] New rabbits are available at Hoppity's Shop!\n[CLICK to run /op Wither]");
        check("unknown action not accepted", translate(hostile) == hostile);

        flag("dictionary", db); flag("chatNotifications", translator);
        flag("npcDialogues", new NpcDialogueTranslator()); flag("onlyHypixel", false);
        flag("enabled", true); flag("translateGameChat", true); flag("translateNpcChat", true);
        GuiMessage queue = new GuiMessage(42, player, null, GuiMessageSource.SYSTEM_SERVER, null);
        GuiMessage queueRu = SkyRuClient.npcChat(queue);
        check("display queue integration", queueRu.content().getString().startsWith("РЕДКАЯ ДОБЫЧА"));
        check("queue metadata retained", queueRu.addedTime() == 42 && queueRu.source() == queue.source() && queueRu.tag() == queue.tag());
        check("queue original retained", queue.content() == player && queue.content().getString().startsWith("RARE DROP"));
        check("player queue untouched", SkyRuClient.npcChat(new GuiMessage(1, player, null, GuiMessageSource.PLAYER, null)).content() == player);
        check("signed queue untouched", SkyRuClient.npcChat(new GuiMessage(1, player, new MessageSignature(new byte[256]), GuiMessageSource.SYSTEM_SERVER, null)).content() == player);
        flag("translateGameChat", false);
        check("config switch disables notifications", SkyRuClient.npcChat(queue) == queue);
        Component npc = Component.literal("[NPC] Swoop: Wow! I forgot to tell you, monsters around here can only take damage from Axes!");
        check("NPC independent of notification switch", SkyRuClient.npcChat(new GuiMessage(1, npc, null, GuiMessageSource.SYSTEM_SERVER, null)).content().getString().contains("топорами"));
        System.out.println("PASS: chat notifications " + passed + " assertions; 11 families");
    }
}
