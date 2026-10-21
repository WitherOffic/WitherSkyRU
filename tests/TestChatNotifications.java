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
        check("families", translator.familyCount() == 36);
        eq("You haven't claimed your Spooky Rewards yet!", "Вы ещё не забрали награды Жуткого фестиваля!");
        eq("Talk to the Spooky Man in the Hub!", "Поговорите с Жутким человеком в Хабе!");
        eq("Teleported you to The Barn!", "Перемещение: Амбар!");
        eq("[SkyHanni] You have 2 items in stash. Click to pickup your stash!", "[SkyHanni] В тайнике: 2 предмета. Нажмите, чтобы забрать вещи!");
        eq("Started pasting Cactus preset on Garden Plot - 7!", "Начато размещение шаблона «Кактус» на участке Сада № 7!");
        eq("Remember, you have to be on the island for the resources to be planted!", "Для размещения ресурсов вы должны оставаться на острове!");
        eq("Your Dolphin is Ready to pick up! [call]", "Ваш питомец «Дельфин» готов к получению! [ЗАБРАТЬ]");
        eq("You need the Cookie Buff to use this feature!", "Для этой функции нужен эффект печенья!");
        eq("Obtain a Booster Cookie from the community shop in the hub!", "Получите печенье-усилитель в общественном магазине в Хабе!");
        eq("[DropVision] v3.2.5+26.1.2 loaded  |  Detection OFF · 7/7 rarities · Bazaar + Auction pricing  |  /dp gui",
            "[DropVision] v3.2.5+26.1.2 загружен  |  Распознавание ВЫКЛ · 7/7 редкостей · Цены Базара и аукциона  |  /dp gui");
        eq("[03:32:52] [SkyHanni] §eYou have §62 §eitems in stash. §eClick to §6pickup §eyour stash!",
            "[03:32:52] [SkyHanni] В тайнике: 2 предмета. Нажмите, чтобы забрать вещи!");
        for(int count=0;count<=250;count++) {
            int tail=count%100,last=count%10;
            String noun=tail>=11 && tail<=14?"предметов":last==1?"предмет":last>=2 && last<=4?"предмета":"предметов";
            eq("[SkyHanni] You have "+count+" items in stash. Click to pickup your stash!",
                "[SkyHanni] В тайнике: "+count+" "+noun+". Нажмите, чтобы забрать вещи!");
        }
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
        eq("[Sacks] +1 item. (Last 5s.)", "[Мешки] +1 предмет. (За 5 с.)");
        eq("[Sacks] +20,032 items, -20,160 items. (Last 13s.)", "[Мешки] +20,032 предмета, -20,160 предметов. (За 13 с.)");
        eq("[Sacks] +30,866 items, -20,160 items. (Last 8s.)", "[Мешки] +30,866 предметов, -20,160 предметов. (За 8 с.)");
        eq("[Sacks] +1 item, -22 items. (Last 0.5s.) (×3)", "[Мешки] +1 предмет, -22 предмета. (За 0.5 с.) (×3)");
        for (int count = 0; count <= 250; count++) {
            int tail = count % 100, last = count % 10;
            String word = tail >= 11 && tail <= 14 ? "предметов" : last == 1 ? "предмет" : last >= 2 && last <= 4 ? "предмета" : "предметов";
            eq("[Sacks] +" + count + " items, -" + count + " items. (Last 5s.)",
                "[Мешки] +" + count + " " + word + ", -" + count + " " + word + ". (За 5 с.)");
        }
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
        Component sacks = Component.empty().append(Component.literal("[Sacks] ").setStyle(action))
            .append(Component.literal("+20,032 items").setStyle(item))
            .append(Component.literal(", ").setStyle(action))
            .append(Component.literal("-20,160 items").setStyle(action))
            .append(Component.literal(". (Last 13s.)").setStyle(item));
        Component sacksRu = translate(sacks);
        check("dual sacks styled text", sacksRu.getString().equals("[Мешки] +20,032 предмета, -20,160 предметов. (За 13 с.)"));
        check("sacks positive style", styleAt(sacksRu, "предмета").equals(item));
        check("sacks negative style", styleAt(sacksRu, "предметов").equals(action));
        check("sacks elapsed time style", styleAt(sacksRu, "13").equals(item));
        var collectPet = new ClickEvent.RunCommand("/petmenu");
        Component pet = Component.empty().append(Component.literal("Your ").setStyle(action))
            .append(Component.literal("Dolphin").setStyle(item))
            .append(Component.literal(" is Ready to pick up! ").setStyle(action))
            .append(Component.literal("[call]").setStyle(item.withClickEvent(collectPet)));
        Component petRu=translate(pet);
        check("pet display translation",petRu.getString().equals("Ваш питомец «Дельфин» готов к получению! [ЗАБРАТЬ]"));
        check("pet button keeps command",styleAt(petRu,"[ЗАБРАТЬ]").getClickEvent()==collectPet);
        check("pet name keeps hover",styleAt(petRu,"Дельфин").getHoverEvent()==hover);
        Component cookie=Component.empty().append(Component.literal("You need the ").setStyle(action))
            .append(Component.literal("Cookie Buff").setStyle(item))
            .append(Component.literal(" to use this feature!").setStyle(action));
        check("cookie styled clauses",translate(cookie).getString().equals("Для этой функции нужен эффект печенья!"));
        check("cookie term style/hover",styleAt(translate(cookie),"эффект печенья").equals(item));
        Component festival=Component.empty().append(Component.literal("Talk to the ").setStyle(action))
            .append(Component.literal("Spooky Man").setStyle(item))
            .append(Component.literal(" in the ").setStyle(action))
            .append(Component.literal("Hub").setStyle(item)).append(Component.literal("!").setStyle(action));
        check("NPC objective declension",translate(festival).getString().equals("Поговорите с Жутким человеком в Хабе!"));
        check("NPC objective styles",styleAt(translate(festival),"Жутким").equals(item));
        Component paste=Component.empty().append(Component.literal("Started pasting ").setStyle(action))
            .append(Component.literal("Cactus").setStyle(item))
            .append(Component.literal(" preset on Garden Plot - ").setStyle(action))
            .append(Component.literal("7").setStyle(item)).append(Component.literal("!").setStyle(action));
        check("crop translated with plot ID",translate(paste).getString().equals("Начато размещение шаблона «Кактус» на участке Сада № 7!"));
        check("crop hover retained",styleAt(translate(paste),"Кактус").getHoverEvent()==hover);
        check("plot ID style retained",styleAt(translate(paste),"7").equals(item));
        var pickup=new ClickEvent.RunCommand("/pickupstash");
        Component stash=Component.empty().append(Component.literal("[SkyHanni] You have ").setStyle(action))
            .append(Component.literal("2").setStyle(item))
            .append(Component.literal(" items in stash. Click to ").setStyle(action))
            .append(Component.literal("pickup").setStyle(item.withClickEvent(pickup)))
            .append(Component.literal(" your stash!").setStyle(action));
        check("stash coloured number retained",styleAt(translate(stash),"2").equals(item));
        check("stash pickup command retained",styleAt(translate(stash),"забрать").getClickEvent()==pickup);
        for(Component notification:java.util.List.of(pet,cookie,festival,paste,stash)) {
            check("new player channel excluded",translator.translate(notification,GuiMessageSource.PLAYER,false)==notification);
            check("new signed channel excluded",translator.translate(notification,GuiMessageSource.SYSTEM_SERVER,true)==notification);
            check("new local mod channel supported",translator.translate(notification,GuiMessageSource.SYSTEM_CLIENT,false)!=notification);
        }
        Component ambiguousCookie=Component.literal("You need the Cookie ").setStyle(action)
            .append(Component.literal("Buff").setStyle(item))
            .append(Component.literal(" to use this feature!").setStyle(action));
        check("mixed semantic term fails closed",translate(ambiguousCookie)==ambiguousCookie);
        Component unknownAction=Component.literal("Your Dolphin is Ready to pick up! [run /op]");
        check("unknown pet action excluded",translate(unknownAction)==unknownAction);
        Component playerShape=Component.literal("[VIP] Wither: You need the Cookie Buff to use this feature!");
        check("new rank-prefixed player text excluded",translate(playerShape)==playerShape);
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
        System.out.println("PASS: chat notifications " + passed + " assertions; " + translator.familyCount() + " families");
    }
}
