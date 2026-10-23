package ru.wither.skyru;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private static final Pattern REQUEST_MATCH_0=Pattern.compile("^\\[NPC] Pesthunter Pamela: Let Phillip finish teaching you about (?:[] )?Pests, then come chat with me!(?: \\((?:x|×)[0-9]{1,6}\\))?$");
    private static final Pattern REQUEST_MATCH_1=Pattern.compile("^\\[NPC] Pesthunter Phillip: Howdy, (?:\\[[A-Za-z0-9_]{1,16} head\\]|\\uFFFC)?[A-Za-z0-9_]{1,16}!$");
    // Two observed public receipts; speaker-gated on insertion, never scanned per draw.
    private static final Pattern PHILLIP_REWARD69=Pattern.compile("^\\[NPC] Pesthunter Phillip: In exchange for ([0-9]{1,6}) Pests, I've given you \\+([0-9]{1,6}) Farming Fortune for ([0-9]{1,5})(m)!(?: \\((?:x|×)[0-9]{1,6}\\))?$");
    private static final Pattern PHILLIP_LIMIT69=Pattern.compile("^\\[NPC] Pesthunter Phillip: You've exchanged enough Pests with me recently\\. You still have ([0-9]{1,6}) Pests in your Vacuum Bag!(?: \\((?:x|×)[0-9]{1,6}\\))?$");
    private static final Pattern REQUEST_MATCH_2=Pattern.compile("Protection [IVXLCDM]{1,8} Book");
    private static final Pattern HEAD_DECORATION=Pattern.compile("^(?:\\[[^\\]\\n]{1,60} head\\]|\\uFFFC)\\s*");
    private static final Pattern TIME = Pattern.compile("^\\[\\d{2}:\\d{2}:\\d{2}\\] ");
    private static final Pattern DISPLAY_COUNTER=Pattern.compile(" \\((?:x|×)[0-9]{1,6}\\)$");
    private static String withoutDisplayCounter(String body){
        if(!body.endsWith(")"))return body;
        var counter=DISPLAY_COUNTER.matcher(body);return counter.find()?body.substring(0,counter.start()):body;
    }
    private static final Pattern RARE = Pattern.compile("^RARE DROP! (.{1,160}) \\(\\+([\\d,. ]+) ([^()\\n]*?)Magic Find\\)$");
    private static final Pattern RARE_COMPACT = Pattern.compile("^RARE DROP! (.{1,160}?)(?: (x\\d+))? \\(\\+([\\d,.]+)\\uE02B\\)$");
    private static final Pattern COINS = Pattern.compile("^The ((?:\\[[^\\]\\n]{1,60} head\\]|\\uFFFC)?Goblin) was carrying a bag of coins! \\+([\\d,]+) Coins!$");
    private static final String GHAST = "The sound of pickaxes clashing against the rock has attracted the attention of the POWDER GHAST!";
    private static final Pattern FIND = Pattern.compile("^Find the Powder Ghast near the (.{1,80})!$");
    private static final Pattern BESTIARY = Pattern.compile("^BESTIARY (.{1,100}) ([IVXLCDM]+) (?:➡|➜|→) ([IVXLCDM]+)$");
    private static final Pattern BESTIARY_TIER = Pattern.compile("^BESTIARY (.{1,100}) reached Tier (\\d{1,6})!$");
    private static final Pattern BESTIARY_SINGLE = Pattern.compile("^BESTIARY (.{1,100}) ([IVXLCDM]+)$");
    // Screenshot f7f57fa5/bbb3bde7: exact server block, not global REWARD/Health words.
    private static final String MILESTONE_HEADER="BESTIARY MILESTONE";
    private static final Pattern MILESTONE_TIERS=Pattern.compile("^([IVXLCDM]{1,12}) {0,2}[➜➡→] {0,2}([IVXLCDM]{1,12})$");
    private static final Pattern MILESTONE_HEALTH=Pattern.compile("^\\+[1-9][0-9]{0,5} {0,1}[❤] Health$");
    private static final Pattern KILL_COMBO = Pattern.compile("^You reached a (\\d{1,9}) Kill Combo!$");
    private static final Pattern SHOP = Pattern.compile("^\\[SkyHanni\\] New rabbits are available at Hoppity's Shop!(?:(?:\\n|\\\\n)\\[CLICK to warp to hub or disable this feature\\])?$");
    private static final Pattern EGG = Pattern.compile("^\\[Skyblocker\\] Found a (Chocolate) (Breakfast|Lunch|Dinner) (Egg) at (-?\\d+, -?\\d+, -?\\d+)!$");
    private static final Pattern SACKS = Pattern.compile("^\\[Sacks\\] \\+([\\d,]+) items?(?:, -([\\d,]+) items?)?\\. \\(Last ([\\d.]+)s\\.\\)(?: \\((?:x|×)\\d+\\))?$");
    private static final Pattern STASH = Pattern.compile("^\\[SkyHanni\\] You have ([\\d,.]+[kKmMbB]?) materials in stash, totalling (\\d+) types\\. Click to pickup your stash!$");
    private static final Pattern STASH_ITEMS = Pattern.compile("^\\[SkyHanni\\] You have ([\\d,]+) items? in stash\\. Click to pickup your stash!$");
    private static final Pattern TELEPORT = Pattern.compile("^Teleported you to ([^!\\n]{1,100})!$");
    private static final Pattern PASTING = Pattern.compile("^Started pasting (Wheat|Carrot|Potato|Nether Wart|Sugar Cane|Mushroom|Cocoa Beans|Cactus|Pumpkin|Melon) preset on Garden Plot - (\\d{1,3})!$");
    private static final Pattern PET_READY = Pattern.compile("^Your ([A-Za-z][A-Za-z '’-]{0,80}) is Ready to pick up! \\[call\\]$");
    private static final Pattern OBTAIN_COOKIE = Pattern.compile("^Obtain a (Booster Cookie) from the community shop in the (hub|Hub)!$");
    private static final Pattern DROPVISION = Pattern.compile("^\\[DropVision\\] v([A-Za-z0-9.+-]{1,60}) loaded +\\| +Detection (OFF|ON) · \\d+/\\d+ rarities · Bazaar \\+ Auction pricing +\\| +/dp gui$");
    private static final Pattern PEST = Pattern.compile("^GROSS! A ([^A-Za-zА-Яа-яЁё0-9\\n]{0,8})Pest has appeared in Plot - (\\d{1,3})!$");
    private static final Pattern PEST_FORTUNE = Pattern.compile("^([^A-Za-zА-Яа-яЁё0-9\\n]{0,8})Farming Fortune has been reduced by (\\d+(?:\\.\\d+)?)%!$");
    private static final Pattern COLLECTION_UP=Pattern.compile("^COLLECTION LEVEL UP ([A-Za-z][A-Za-z '’-]{0,80}) ([IVXLCDM]{1,12})(?:➜|→|➡)([IVXLCDM]{1,12})$");
    private static final Pattern COLLECTION_XP=Pattern.compile("^\\+([0-9]{1,6}) SkyBlock XP$");
    // Source: cached SkyHanni regexesModern, mining.commissions.complete, 75ca647.
    // Only already reviewed commission titles; arbitrary player names are not a title.
    private static final Pattern COMMISSION = Pattern.compile("^(Mithril Miner|Titanium Miner|Goblin Slayer|Ice Walker Slayer|Elusive Goblin Slayer) Commission Complete! Visit the King to claim your rewards!$");
    private static final java.util.Map<String,String> MAYHEM = java.util.Map.of(
        "Your Pickaxe Ability cooldown was reduced from your Mineshaft Mayhem perk!", "Способность вашей кирки перезаряжается быстрее благодаря перку «Хаос в штольнях»!",
        "You received a  Cold Resistance buff from your Mineshaft Mayhem perk!", "Перк «Хаос в штольнях» повысил вашу  устойчивость к холоду!",
        "You received a  Mining Fortune buff from your Mineshaft Mayhem perk!", "Перк «Хаос в штольнях» повысил вашу  удачу добычи!",
        "You received a  Mining Speed buff from your Mineshaft Mayhem perk!", "Перк «Хаос в штольнях» повысил вашу  скорость добычи!",
        "Your Suspicious Scrap chance was buffed by your Mineshaft Mayhem perk!", "Перк «Хаос в штольнях» повысил шанс получить подозрительный обломок!");
    // Four actual request grammars; no inferred flower, player name or quantity.
    private static final String PABLO_PREFIX = "^\\[NPC] Pablo: (?:✆ )?";
    private static final String FLOWER = "(Dandelion|Poppy|Blue Orchid|Allium|Azure Bluet|Red Tulip|Orange Tulip|White Tulip|Pink Tulip|Oxeye Daisy|Sunflower|Lilac|Rose Bush|Peony)";
    private static final Pattern[] PABLO = {
        Pattern.compile(PABLO_PREFIX+"Are you available\\? I desperately need an? "+FLOWER+" today\\.$"),
        Pattern.compile(PABLO_PREFIX+"Bring me that "+FLOWER+" as soon as you can!$"),
        Pattern.compile(PABLO_PREFIX+"Could you bring me an? "+FLOWER+"\\?$"),
        Pattern.compile(PABLO_PREFIX+"I really need an? "+FLOWER+" today, do you have one you could spare\\?$" )
    };
    // Primary cached regexesModern 75ca647: garden.contest.personal.best.*.
    // The non-record result also occurs in the frozen observed Jacob fixture.
    private static final String COUNT = "([0-9]{1,12}|[1-9][0-9]{0,2}(?:,[0-9]{3}){1,3})";
    // Observed updated document; rare crop names and pest grammar additionally
    // grounded in the installed SkyHanni regexesModern. No free-form chat input.
    private static final Pattern CROP_NOTICE=Pattern.compile("^(?:VERY )?RARE CROP! (Aggourdian|Botroot|Burrowing Spores|Cactus Flower|Cane Knot|Carrot Zest|Tasty Cheese|Compost|Cornucopia|Cropie|Crystalized Moonlight|Deepfries|Designer Coffee Beans|Dung|Feastfungus|Fermento|Floral Gelatin|Helianthus|Honey Jar|Jelly|Melon Juice|Plant Matter|Rarefinder Chip|Salted Sunflower Seeds|Seasoning|Squash|Warty)(?: \\(\\+([0-9]{1,3}(?:\\.[0-9]{1,3})?)%\\))?(?: \\((?:x|×)[0-9]{1,6}\\))?$");
    private static final Pattern CULTIVATING_UP=Pattern.compile("^(Cultivating) (I|II|III|IV|V|VI|VII|VIII|IX) on your ([^\\r\\n]{1,160}) was upgraded to (Cultivating) (II|III|IV|V|VI|VII|VIII|IX|X)!$");
    private static final List<String> CULTIVATING_LEVELS=List.of("I","II","III","IV","V","VI","VII","VIII","IX","X");
    private static final Pattern MULTI_PEST=Pattern.compile("^(GROSS|EW|EWW|YUCK|UGH)! ([2-9]) ([]) (Pests?) have spawned in Plot - ([0-9]{1,3})!$");
    private static final Pattern REQUIRED_ITEM=Pattern.compile("^You don't have enough of the required item!(?: \\((?:x|×)[0-9]{1,6}\\))?$");
    private static final Pattern PLAY_EXP=Pattern.compile("^You earned "+COUNT+" (GEXP) \\+ "+COUNT+" (Event EXP) from playing (SkyBlock)!$");
    private static final Pattern JACOB_COLLECTED = Pattern.compile("^\\[NPC] Jacob: You collected "+COUNT+" items!( PERSONAL BEST!)?$");
    private static final Pattern JACOB_PREVIOUS = Pattern.compile("^\\[NPC] Jacob: Your previous Personal Best was "+COUNT+"\\.$");
    private static final Pattern JACOB_FORTUNE = Pattern.compile("^\\[NPC] Jacob: Your (Personal Bests) perk is now granting you (\\+[0-9]{1,6}(?:\\.[0-9]{1,3})?) (Wheat|Carrot|Potato|Nether Wart|Sugar Cane|Mushroom|Cocoa Beans|Cactus|Pumpkin|Melon) (Fortune)!$");
    private static final String JACOB_CROP70="(Wheat|Carrot|Potato|Nether Wart|Sugar Cane|Mushroom|Cocoa Beans|Cactus|Pumpkin|Melon)";
    private static final Pattern JACOB_RECORD70=Pattern.compile("^\\[NPC] Jacob: PERSONAL BEST! You've surpassed your previous record of "+COUNT+" items collected in the "+JACOB_CROP70+" (Contest)!$");
    private static final Pattern JACOB_BRACKET70=Pattern.compile("^\\[NPC] Jacob: You placed in the (BRONZE|SILVER|GOLD|PLATINUM|DIAMOND) bracket in the "+JACOB_CROP70+" (Contest)!$");
    private static final Pattern JACOB_PARTICIPATION70=Pattern.compile("^\\[NPC] Jacob: You didn't earn a medal, but you may claim a participation reward in the "+JACOB_CROP70+" (Contest)!$");
    private static final Map<String,String> JACOB_RANKS70=Map.of("BRONZE","БРОНЗА","SILVER","СЕРЕБРО","GOLD","ЗОЛОТО","PLATINUM","ПЛАТИНА","DIAMOND","АЛМАЗ");
    // Source-backed time sentence pair; numeric values are never recalculated.
    private static final String KAT_DURATION70="([0-9]{1,4} (?:days?|hours?|minutes?|seconds?)(?: [0-9]{1,4} (?:days?|hours?|minutes?|seconds?)){0,3})";
    private static final Pattern KAT_PICKUP70=Pattern.compile("^\\[NPC] Kat: You can pick it up in "+KAT_DURATION70+"\\.$");
    private static final Pattern KAT_COMEBACK70=Pattern.compile("^\\[NPC] Kat: Come back in "+KAT_DURATION70+" to pick it up!$");
    private static final Pattern KAT_UNIT70=Pattern.compile("([0-9]{1,4}) (days?|hours?|minutes?|seconds?)");
    private static final String MAXWELL_NAME70="((?:\\[[A-Za-z0-9_]{1,16} head\\]|\\uFFFC)?)([A-Za-z0-9_]{1,16})";
    private static final Pattern MAXWELL_POWER70=Pattern.compile("^\\[NPC] Maxwell: "+MAXWELL_NAME70+", that's an excellent power!$");
    private static final Pattern MAXWELL_CHOICE70=Pattern.compile("^\\[NPC] Maxwell: A wise choice, "+MAXWELL_NAME70+"!$");
    // SkyHanni TrevorFeatures / TrevorTracker / TrapperMobArea primary source:
    // closed known categories and locations, never invented animal/count/instructions.
    private static final Pattern TREVOR = Pattern.compile("^\\[NPC] Trevor: You can find your (TRACKABLE|UNTRACKABLE|UNDETECTED|ENDANGERED|ELUSIVE) animal near the (Oasis|Mushroom Gorge|Overgrown Mushroom Cave|Desert Settlement|Glowing Mushroom Cave|Desert Mountain)\\.$");
    // Cached primary achievements.starcult. It announces a reward, not a fetch quest.
    private static final Pattern DALIR = Pattern.compile("^\\[NPC] Dalir: You’ve now attended ([0-9]{1,12}) meetings of the (Cult of the Fallen Star)! As a reward, here's some (Starfall)!$");
    // Cached primary achievements.da-item-count: a completed-purchase total, not a quest.
    private static final Pattern LUCIUS = Pattern.compile("^\\[NPC] Lucius: You've purchased ([0-9]{1,12}) items from the (Dark Auction)\\.$");
    // Primary DarkAuctionItemDisplay: raw NPC shapes plus that mod's optional item suffix.
    private static final Pattern SIRIUS_LOT=Pattern.compile("^\\[NPC] Sirius: (First|Next) up we have an? ([^,()\\n]{1,160}), the starting bid is "+COUNT+" (Coins)!$");
    private static final Pattern SIRIUS_BID=Pattern.compile("^\\[NPC] Sirius: The highest bidder was ([A-Za-z0-9_]{1,16}) with a bid of "+COUNT+" (Coins)!(?: \\(([^()\\n]{1,160})\\))?$");
    private static final Pattern SIRIUS_NO_BID=Pattern.compile("^\\[NPC] Sirius: No one made any bids!( Let's move on to the next item\\.)?(?: \\(([^()\\n]{1,160})\\))?$");
    // Primary CompactStarlynSisters / StarlynSisterType, not the wildcard NPC name.
    private static final String SISTER_PREFIX="^\\[NPC] (Agatha|Miria): ";
    private static final Pattern SISTER_BRACKET=Pattern.compile(SISTER_PREFIX+"You reached the (COMMON|UNCOMMON|RARE|EPIC|LEGENDARY) Bracket in my contest!$");
    private static final Pattern SISTER_POINTS=Pattern.compile(SISTER_PREFIX+"You earned a total of "+COUNT+" points!( That's a new PERSONAL BEST!)?$");
    private static final Pattern SISTER_PREVIOUS=Pattern.compile(SISTER_PREFIX+"Your previous Personal Best was "+COUNT+"\\.$");
    private static final Pattern SISTER_CLAIM=Pattern.compile(SISTER_PREFIX+"Come see me at (Murkwater Loch) to claim your rewards!$");
    private static final Pattern SISTER_KEEP=Pattern.compile(SISTER_PREFIX+"Keep it up!$");
    private static final Pattern SISTER_LOG_RECORD=Pattern.compile(SISTER_PREFIX+"PERSONAL BEST! You've surpassed your previous record of "+COUNT+" (Fig|Mangrove) logs collected in my Contest!$");
    private static final Pattern FOREST_COLLECTION=Pattern.compile("^PERSONAL BEST: You increased your (Fig|Mangrove) Collection by "+COUNT+" during the contest! That's "+COUNT+" more than your previous best!$");
    private static final Pattern FOREST_SWEEP=Pattern.compile("^Your total (∮) (Sweep) is now increased by ([0-9]{1,6}(?:\\.[0-9]{1,6})?)%!$");
    private record Atom(int start, int end, String text, Style style, Component opaque) {}
    private static final Pattern STORY_YES_NO=Pattern.compile("^Select an option: *\\[(NO)\\] *\\[(YES)\\]$");
    private static final Pattern ST_JERRY_GREETING=Pattern.compile("^\\[NPC] St\\. Jerry: 'Tis the Season of Jerry, ([A-Za-z0-9_]{1,16})!(?: \\((?:x|×)[0-9]{1,6}\\))?$");
    // Bounded source-backed Slayer display shapes; no state/history or player text.
    private static final String SLAYER_TYPE="(Zombie|Spider|Wolf|Enderman|Blaze|Vampire)";
    private static final java.util.Map<String,String> SLAYER_TYPES=java.util.Map.of("Zombie","зомби","Spider","пауков","Wolf","волков","Enderman","эндерменов","Blaze","всполохов","Vampire","вампиров");
    private static final java.util.Map<String,String> SLAYER_BOSSES=java.util.Map.of("Revenant Horror","Возрождённый ужас","Tarantula Broodfather","Прародитель тарантулов","Sven Packmaster","Свен — вожак стаи","Voidgloom Seraph","Серафим мрачной пустоты","Inferno Demonlord","Повелитель демонов Инферно","Riftstalker Bloodfiend","Кровавый демон Разлома");
    private static final Pattern SLAYER_SPAWN=Pattern.compile("^SLAYER BOSS! The (Revenant Horror|Tarantula Broodfather|Sven Packmaster|Voidgloom Seraph|Inferno Demonlord|Riftstalker Bloodfiend) ([IVXLCDM]{1,12}) spawned!$");
    private static final Pattern SLAYER_HEAL=Pattern.compile("^Your Slayer (?:Kill|kill) gave you "+COUNT+" HP healing for ([0-9]{1,6}) seconds!$");
    private static final Pattern SLAYER_CLAIM=Pattern.compile("^» Talk to (Maddox) to claim your "+SLAYER_TYPE+" Slayer XP!$");
    private static final Pattern SLAYER_LEVEL=Pattern.compile("^"+SLAYER_TYPE+" Slayer LVL ([0-9]{1,2}) - Next LVL (?:([0-9]{1,2}) )?in "+COUNT+" XP!$");
    private static final Pattern SLAYER_QUOTA=Pattern.compile("^» Slay "+COUNT+" Combat XP worth of (Zombies|Spiders|Wolves|Endermen|Blazes|Vampires)\\.$");
    private static final Pattern SLAYER_BITS=Pattern.compile("^\\[SkyHanni] You have gained "+COUNT+" Bits\\.$");
    private static final Pattern SLAYER_TIME=Pattern.compile("^\\[SkyHanni] Slayer quest took ([0-9]{1,6}(?:\\.[0-9]{1,6})?)(s) to complete\\.$");
    private static final Pattern SLAYER_KILL_TIME=Pattern.compile("^\\[Skyblocker] Slayer убит за ([0-9]{1,6}(?:\\.[0-9]{1,6})?)(s)!$");
    private static final Pattern STORY_FACTION=Pattern.compile("^Select an option: *\\[(BARBARIANS)\\] *\\[(MAGES)\\]$");
    // Closed recorded layouts only: through five buttons,including both published Carnival arrow glyphs.
    private static final Pattern SAFETY_LAYOUT=Pattern.compile("^Select an option: *(?:(?:[➜🡺] *)?\\[[^\\[\\]\\r\\n]{1,180}\\] *){1,11}$");
    private static final Pattern SAFETY_BUTTON=Pattern.compile("\\[([^\\[\\]\\r\\n]{1,180})\\]");
    private static final java.util.Map<List<String>,List<String>> SAFETY_CHOICES=java.util.Map.ofEntries(
        java.util.Map.entry(List.of("A... what?"),List.of("Приключение... какое?")),
        java.util.Map.entry(List.of("You told me to come talk to you."),List.of("Ты сама велела мне с тобой поговорить.")),
        java.util.Map.entry(List.of("Chocolate?","Caramel?","Ice Cream?"),List.of("Шоколад?","Карамель?","Мороженое?")),
        java.util.Map.entry(List.of("No?"),List.of("Нет?")),
        java.util.Map.entry(List.of("Can I get the necklace?"),List.of("Можно мне ожерелье?")),
        java.util.Map.entry(List.of("Can I have one?"),List.of("Можно мне такое?")),
        java.util.Map.entry(List.of("What do I need to do for the necklace?"),List.of("Что нужно сделать ради ожерелья?")),
        java.util.Map.entry(List.of("My most sincere condolences."),List.of("Мои искренние соболезнования.")),
        java.util.Map.entry(List.of("Ok but, where is Ramero now?"),List.of("Хорошо, но где сейчас Рамеро?")),
        java.util.Map.entry(List.of("Thank you Joliet, we will find him promptly."),List.of("Спасибо, Джолиет, мы быстро его найдём.")),
        java.util.Map.entry(List.of("I'll try my best."),List.of("Я сделаю всё возможное.")),
        java.util.Map.entry(List.of("Where is your lover?"),List.of("Где твой возлюбленный?")),
        java.util.Map.entry(List.of("We found a bouquet made with love!"),List.of("Мы нашли букет, составленный с любовью!")),
        java.util.Map.entry(List.of("We need to know his whereabouts, he's about to be murdered."),List.of("Нужно знать, где он: его вот-вот убьют.")),
        java.util.Map.entry(List.of("Calm down, we need your help."),List.of("Успокойся, нам нужна твоя помощь.")),
        java.util.Map.entry(List.of("Ok. Do you have any idea who would want to harm Ramero?"),List.of("Ладно. Знаешь кого-нибудь, кто хочет навредить Рамеро?")),
        java.util.Map.entry(List.of("Calm down."),List.of("Успокойся.")),
        java.util.Map.entry(List.of("Not really..."),List.of("Не совсем...")),
        java.util.Map.entry(List.of("This is very awkward. I actually murdered him. I'm terribly sorry."),List.of("Очень неловко. Вообще-то я его убил. Мне ужасно жаль.")),
        java.util.Map.entry(List.of("I honestly don't know..."),List.of("Я правда не знаю...")),
        java.util.Map.entry(List.of("Now that you mention it, his body wasn't there..."),List.of("Раз уж ты об этом заговорила, его тела там не было...")),
        java.util.Map.entry(List.of("We are investigating the future murder of Ramero."),List.of("Мы расследуем будущее убийство Рамеро.")),
        java.util.Map.entry(List.of("Are you the murderer?"),List.of("Ты убийца?")),
        java.util.Map.entry(List.of("That would be of great assistance..."),List.of("Это бы очень помогло...")),
        java.util.Map.entry(List.of("No, I already murdered Ramero."),List.of("Нет, я уже убил Рамеро.")),
        java.util.Map.entry(List.of("Have you stolen his body?"),List.of("Ты украл его тело?")),
        java.util.Map.entry(List.of("I don't know."),List.of("Не знаю.")),
        java.util.Map.entry(List.of("Yes."),List.of("Да.")),
        java.util.Map.entry(List.of("That's impossible, I went there a long time ago."),List.of("Невозможно, я ходил туда давным-давно.")),
        java.util.Map.entry(List.of("I thought so too..."),List.of("Я тоже так думал...")),
        java.util.Map.entry(List.of("That's not your fault."),List.of("Ты не виноват.")),
        java.util.Map.entry(List.of("Well..."),List.of("Ну...")),
        java.util.Map.entry(List.of("I'm afraid so, but we found you right on time."),List.of("Боюсь, да. Но мы нашли тебя вовремя.")),
        java.util.Map.entry(List.of("I don't think this place is safe for you then."),List.of("Тогда, думаю, тебе здесь небезопасно.")),
        java.util.Map.entry(List.of("I will help you, let's get to your house."),List.of("Я помогу, пойдём к тебе домой.")),
        java.util.Map.entry(List.of("Give materials","No thanks!"),List.of("Отдать материалы","Нет, спасибо!")),
        java.util.Map.entry(List.of("I Accept","I Decline"),List.of("Я согласен","Я отказываюсь")),
        java.util.Map.entry(List.of("Let's do it","It's too risky"),List.of("Сделаем это","Слишком рискованно")),
        java.util.Map.entry(List.of("Sure, I guess?"),List.of("Наверное, да?")),
        java.util.Map.entry(List.of("WHAT?"),List.of("ЧТО?")),
        java.util.Map.entry(List.of("Yes of course"),List.of("Да, конечно")),
        java.util.Map.entry(List.of("Bring it on."),List.of("Давай начнём.")),
        java.util.Map.entry(List.of("Yeah, obviously."),List.of("Да, очевидно.")),
        java.util.Map.entry(List.of("Because it increases the odds!","Because I felt like it! >:( "),List.of("Потому что так выше шансы!","Потому что захотелось! >:( ")),
        java.util.Map.entry(List.of("How did you get up here? You were just further down the mountain."),List.of("Как ты сюда попал? Ты же только что был ниже по склону.")),
        java.util.Map.entry(List.of("I have truly seen everything now."),List.of("Теперь я действительно видел всё.")),
        java.util.Map.entry(List.of("I understand."),List.of("Понимаю.")),
        java.util.Map.entry(List.of("I lost my Ubik's Cube."),List.of("Я потерял куб Убика.")),
        java.util.Map.entry(List.of("Hi?"),List.of("Привет?")),
        java.util.Map.entry(List.of("Why do you say so few words?"),List.of("Почему ты так немногословен?")),
        java.util.Map.entry(List.of("Why are the horses exploding?"),List.of("Почему лошади взрываются?")),
        java.util.Map.entry(List.of("Hunting?"),List.of("Охота?")),
        java.util.Map.entry(List.of("So, how do we access it?"),List.of("И как нам получить эту силу?")),
        java.util.Map.entry(List.of("A Shard?"),List.of("Осколок?")),
        java.util.Map.entry(List.of("Interesting. What does a Shard look like?"),List.of("Интересно. А как выглядит осколок?")),
        java.util.Map.entry(List.of("Does the type of the Shard matter?"),List.of("Тип осколка имеет значение?")),
        java.util.Map.entry(List.of("The culprit?"),List.of("Преступник?")),
        java.util.Map.entry(List.of("A reverse murder?"),List.of("Убийство наоборот?")),
        java.util.Map.entry(List.of("Sure."),List.of("Конечно.")),
        java.util.Map.entry(List.of("Perhaps their lover is the florist?"),List.of("Может, его возлюбленная — цветочница?")),
        java.util.Map.entry(List.of("This is complicated."),List.of("Всё так запутанно.")),
        java.util.Map.entry(List.of("What about that ring we found?"),List.of("А как насчёт найденного нами кольца?")),
        java.util.Map.entry(List.of("I hope we're not too late."),List.of("Надеюсь, мы не опоздали.")),
        java.util.Map.entry(List.of("Perhaps we missed something at his house?"),List.of("Может, мы что-то упустили в его доме?")),
        java.util.Map.entry(List.of("Joliet bought the dress right before the picture was taken."),List.of("Джолиет купила платье прямо перед тем, как сделали снимок.")),
        java.util.Map.entry(List.of("Smart, he knows he's in danger."),List.of("Разумно: он знает, что ему грозит опасность.")),
        java.util.Map.entry(List.of("How do we get inside?"),List.of("Как нам попасть внутрь?")),
        java.util.Map.entry(List.of("Who could this be?"),List.of("Кто же это?")),
        java.util.Map.entry(List.of("We are investigating the reverse murder of Ramero."),List.of("Мы расследуем убийство Рамеро наоборот.")),
        java.util.Map.entry(List.of("Hold on, I'm the murderer?"),List.of("Погоди, убийца — это я?")),
        java.util.Map.entry(List.of("You're joking, right?"),List.of("Ты ведь шутишь?")),
        java.util.Map.entry(List.of("Maybe? I don't know."),List.of("Может быть? Не знаю.")),
        java.util.Map.entry(List.of("I know. Don't you remember me?"),List.of("Знаю. Ты меня не помнишь?")),
        java.util.Map.entry(List.of("Sigh..."),List.of("Эх...")),
        java.util.Map.entry(List.of("You can't be serious."),List.of("Ты же не всерьёз.")),
        java.util.Map.entry(List.of("Have you looked in his house yet?"),List.of("Ты уже осмотрел его дом?")),
        java.util.Map.entry(List.of("Do we really have to go through this?"),List.of("Неужели нам снова нужно через всё это пройти?")),
        java.util.Map.entry(List.of("I think we should go to the florist because the bouquet over there was made with love."),List.of("Думаю, нужно пойти к цветочнице: тот букет сделан с любовью.")),
        java.util.Map.entry(List.of("I don't know what to believe anymore."),List.of("Я уже не знаю, чему верить.")),
        java.util.Map.entry(List.of("He saw the murderer leave to the Blacksmith with the murder weapon."),List.of("Он видел, как убийца пошёл к кузнецу с орудием убийства.")),
        java.util.Map.entry(List.of("The Blacksmith is a dead end and I'm tired of this case..."),List.of("Кузнец — тупик, а я уже устал от этого дела...")),
        java.util.Map.entry(List.of("Let's skip directly to Joliet, she is at Taylor's shop."),List.of("Пойдём сразу к Джолиет, она в магазине Тейлор.")),
        java.util.Map.entry(List.of("Whatever you say."),List.of("Как скажешь.")),
        java.util.Map.entry(List.of("I think it's going to break very soon."),List.of("Думаю, её очень скоро проломят.")),
        java.util.Map.entry(List.of("Are you serious?"),List.of("Ты серьёзно?")),
        java.util.Map.entry(List.of("Hold up, was that really me, or another version of me??"),List.of("Погоди, это правда был я или другая моя версия??")),
        java.util.Map.entry(List.of("First you made me murder Ramero, and now... what?!"),List.of("Сначала ты заставил меня убить Рамеро, а теперь... что?!")),
        java.util.Map.entry(List.of("What will happen to me now?"),List.of("Что теперь со мной будет?")),
        java.util.Map.entry(List.of("None of that would have happened without you!"),List.of("Без тебя ничего бы этого не случилось!")),
        java.util.Map.entry(List.of("You did WHAT?!?!"),List.of("Ты сделал ЧТО?!?!")),
        java.util.Map.entry(List.of("..."),List.of("...")),
        java.util.Map.entry(List.of("Not you again..."),List.of("Только не ты опять...")),
        java.util.Map.entry(List.of("What wrongdoing?"),List.of("Какое преступление?")),
        java.util.Map.entry(List.of("Sure..."),List.of("Конечно...")),
        java.util.Map.entry(List.of("Of course!"),List.of("Конечно!")),
        java.util.Map.entry(List.of("What are attributes?"),List.of("Что такое свойства?")),
        java.util.Map.entry(List.of("Anything else?"),List.of("Что-нибудь ещё?")),
        java.util.Map.entry(List.of("It was an amazing experience."),List.of("Это было потрясающе.")),
        java.util.Map.entry(List.of("Yes","Later, maybe"),List.of("Да","Может, позже")),
        java.util.Map.entry(List.of("What kind of domain?"),List.of("Какая область влияния?")),
        java.util.Map.entry(List.of("Not very original."),List.of("Не слишком оригинально.")),
        java.util.Map.entry(List.of("It's an interesting theory"),List.of("Интересная теория")),
        java.util.Map.entry(List.of("So the Starborns were spectating them"),List.of("То есть Звёздорождённые за ними наблюдали")),
        java.util.Map.entry(List.of("I see where this is going"),List.of("Понимаю, к чему всё идёт")),
        java.util.Map.entry(List.of("Those Precursors are very human after all"),List.of("Всё-таки эти Предтечи очень похожи на людей")),
        java.util.Map.entry(List.of("Can Starborns even die?"),List.of("А Звёздорождённые вообще могут умереть?")),
        java.util.Map.entry(List.of("So how does the story end?"),List.of("И чем закончилась эта история?")),
        java.util.Map.entry(List.of("The future? When exactly?"),List.of("В будущее? Когда именно?")),
        java.util.Map.entry(List.of("So the Precursors will just appear out of nowhere one day?"),List.of("Значит, однажды Предтечи просто появятся из ниоткуда?")),
        java.util.Map.entry(List.of("Enter Time Chamber","Maybe later..."),List.of("Войти во Временную камеру","Может, позже...")),
        java.util.Map.entry(List.of("Enter Time Chamber","Maybe later...","Ponder with Oruo"),List.of("Войти во Временную камеру","Может, позже...","Побеседовать с Оруо")),
        java.util.Map.entry(List.of("Yes"),List.of("Да")),
        java.util.Map.entry(List.of("You'll see"),List.of("Увидишь")),
        java.util.Map.entry(List.of("No, I'm good.","I have a few questions..."),List.of("Нет, всё понятно.","У меня есть пара вопросов...")),
        java.util.Map.entry(List.of("Why didn't the wizards simply build the portal at the top of the Mountain?"),List.of("Почему маги просто не построили портал на вершине горы?")),
        java.util.Map.entry(List.of("The Wizard mentioned a great danger approaching our world. What is it?"),List.of("Волшебник говорил, что нашему миру грозит большая опасность. Что это?")),
        java.util.Map.entry(List.of("Why did you choose to enter the breach?"),List.of("Почему ты решил войти в пролом?")),
        java.util.Map.entry(List.of("Yes. That is not weird at all."),List.of("Да. Совсем не странно.")),
        java.util.Map.entry(List.of("Open your shop","I have a few questions..."),List.of("Открой магазин","У меня есть пара вопросов...")),
        java.util.Map.entry(List.of("What's a berry?"),List.of("Что такое ягода?")),
        java.util.Map.entry(List.of("So far so good"),List.of("Пока всё понятно")),
        java.util.Map.entry(List.of("YES","NO"),List.of("ДА","НЕТ")),
        java.util.Map.entry(List.of("Do you know anything about it?"),List.of("Ты что-нибудь об этом знаешь?")),
        java.util.Map.entry(List.of("Yes, how do you know?"),List.of("Да. Откуда ты знаешь?")),
        java.util.Map.entry(List.of("Why are you trapped?"),List.of("Почему ты здесь заперт?")),
        java.util.Map.entry(List.of("How do we tame it?"),List.of("Как нам его обуздать?")),
        java.util.Map.entry(List.of("Why?"),List.of("Почему?")),
        java.util.Map.entry(List.of("Ok. What is the 4th one?"),List.of("Ладно. А какое 4-е?")),
        java.util.Map.entry(List.of("I'm following. What is the 5th dimension?"),List.of("Понятно. А что за 5-е измерение?")),
        java.util.Map.entry(List.of("Sure, but what about the mountain?"),List.of("Хорошо, а что насчёт горы?")),
        java.util.Map.entry(List.of("How do I reach it then?"),List.of("И как же мне туда добраться?")),
        java.util.Map.entry(List.of("Yes. Do you have a soul for me?"),List.of("Да. У тебя найдётся для меня душа?")),
        java.util.Map.entry(List.of("Yes","No"),List.of("Да","Нет")),
        java.util.Map.entry(List.of("I should be good, thanks.","What sorts of games can I play?","How can I get more Carnival Tickets?","How do I earn Carnival Tokens?","Where can I turn my Carnival Tokens in?"),List.of("Всё понятно, спасибо.","В какие игры можно сыграть?","Где взять ещё карнавальные билеты?","Как заработать карнавальные жетоны?","Где потратить карнавальные жетоны?")),
        java.util.Map.entry(List.of("I should be good, thanks.","How can I get more Carnival Tickets?","How do I earn Carnival Tokens?","Where can I turn my Carnival Tokens in?"),List.of("Всё понятно, спасибо.","Где взять ещё карнавальные билеты?","Как заработать карнавальные жетоны?","Где потратить карнавальные жетоны?")),
        java.util.Map.entry(List.of("I should be good, thanks.","What sorts of games can I play?","How do I earn Carnival Tokens?","Where can I turn my Carnival Tokens in?"),List.of("Всё понятно, спасибо.","В какие игры можно сыграть?","Как заработать карнавальные жетоны?","Где потратить карнавальные жетоны?")),
        java.util.Map.entry(List.of("I should be good, thanks.","What sorts of games can I play?","How can I get more Carnival Tickets?","Where can I turn my Carnival Tokens in?"),List.of("Всё понятно, спасибо.","В какие игры можно сыграть?","Где взять ещё карнавальные билеты?","Где потратить карнавальные жетоны?")),
        java.util.Map.entry(List.of("I should be good, thanks.","What sorts of games can I play?","How can I get more Carnival Tickets?","How do I earn Carnival Tokens?"),List.of("Всё понятно, спасибо.","В какие игры можно сыграть?","Где взять ещё карнавальные билеты?","Как заработать карнавальные жетоны?")),
        java.util.Map.entry(List.of("You guessed it!","Could you tell me the rules again?","Nope."),List.of("Угадал!","Можешь повторить правила?","Нет.")),
        java.util.Map.entry(List.of("Let's do it!","I think I'll reconsider."),List.of("Давай!","Пожалуй, я передумал.")),
        java.util.Map.entry(List.of("Sure does, partner! I'd like to give it a try.","I think I'll save this one for later."),List.of("Всё ясно, напарник! Хочу попробовать.","Думаю, сыграю позже.")),
        java.util.Map.entry(List.of("Sure thing, partner!","Could ya tell me the rules again?","I'd like to do somthin' else fer now."),List.of("Конечно, напарник!","Повтори правила, ладно?","Пока займусь чем-нибудь другим.")),
        java.util.Map.entry(List.of("More ready than a rooster at dawn!","Reckon I'll wrangle up a fresh thought instead."),List.of("Готов, как петух на рассвете!","Пожалуй, лучше ещё подумаю.")),
        java.util.Map.entry(List.of("Aye aye, Captain!","I'll find me a different game."),List.of("Так точно, капитан!","Поищу другую игру.")),
        java.util.Map.entry(List.of("Aye sure do!","Could ye tell me the rules again, Captain?","I'll set me sights elsewhere."),List.of("Ещё бы!","Повтори правила, капитан.","Поищу другое занятие.")),
        java.util.Map.entry(List.of("I be ready as the tide!","Avast, I've changed me mind..."),List.of("Готов, как прилив!","Полундра, я передумал...")),
        java.util.Map.entry(List.of("What's an Abiphone?"),List.of("Что такое Абифон?")),
        java.util.Map.entry(List.of("I guess."),List.of("Пожалуй.")),
        java.util.Map.entry(List.of("How are perks chosen?","What's the deal with Ministers?","What are Special Mayors?","I think I'm good."),List.of("Как выбираются бонусы?","Как устроена система министров?","Кто такие особые мэры?","Вопросов больше нет.")),
        java.util.Map.entry(List.of("Sure.","I'd rather talk about special Mayors.","Nah."),List.of("Конечно.","Лучше расскажите об особых мэрах.","Нет.")),
        java.util.Map.entry(List.of("How are perks chosen?","I'd rather talk about special Mayors.","Nah."),List.of("Как выбираются бонусы?","Лучше расскажите об особых мэрах.","Нет.")),
        java.util.Map.entry(List.of("Sure","Nah, I'm good"),List.of("Конечно","Нет, спасибо")),
        java.util.Map.entry(List.of("Ok, then what?"),List.of("Хорошо, а дальше?")),
        java.util.Map.entry(List.of("Accessory Power?"),List.of("Сила аксессуаров?")),
        java.util.Map.entry(List.of("Magical Power?"),List.of("Магическая сила?")),
        java.util.Map.entry(List.of("That's amazing!"),List.of("Потрясающе!")),
        java.util.Map.entry(List.of("Absolutely!","Tell me more about the Safety Squad.","Not right now."),List.of("Конечно!","Расскажите об отряде безопасности.","Не сейчас.")),
        java.util.Map.entry(List.of("Sounds good with me!","Give me a moment."),List.of("Согласен!","Дайте мне минуту.")),
        java.util.Map.entry(List.of("Who, me?"),List.of("Кто, я?")),
        java.util.Map.entry(List.of("What is a safety-related incident anyway?"),List.of("Что вы называете опасным происшествием?")),
        java.util.Map.entry(List.of("What's the point of Account Safety?"),List.of("Зачем нужна безопасность аккаунта?")),
        java.util.Map.entry(List.of("I don't want to lose my account!"),List.of("Я не хочу потерять свой аккаунт!")),
        java.util.Map.entry(List.of("That doesn't sound good."),List.of("Звучит неприятно.")),
        java.util.Map.entry(List.of("Stranger danger, understood."),List.of("Незнакомцам не доверять. Понял.")),
        java.util.Map.entry(List.of("I'm not stupid enough to open a random link!"),List.of("Я не настолько глуп, чтобы открыть случайную ссылку!")),
        java.util.Map.entry(List.of("C'mon, I'm sure it's not thaaat complicated."),List.of("Да ладно, не может быть всё настолько сложно.")),
        java.util.Map.entry(List.of("Brigette, you're quite scary."),List.of("Brigette, вы меня пугаете.")),
        java.util.Map.entry(List.of("The /report command. Noted."),List.of("Команда /report. Запомнил.")),
        java.util.Map.entry(List.of("Links for SkyBlock outside SkyBlock? Like what?"),List.of("Ссылки на SkyBlock вне игры? Например?")),
        java.util.Map.entry(List.of("I will be careful when clicking on links, I promise!"),List.of("Обещаю осторожно переходить по ссылкам!")),
        java.util.Map.entry(List.of("I'm good, thanks."),List.of("Я в порядке, спасибо.")),
        java.util.Map.entry(List.of("Hello, Jimmy."),List.of("Привет, Jimmy.")),
        java.util.Map.entry(List.of("Security is indeed very important."),List.of("Безопасность и правда очень важна.")),
        java.util.Map.entry(List.of("Cool."),List.of("Здорово.")),
        java.util.Map.entry(List.of("Deal."),List.of("Договорились.")),
        java.util.Map.entry(List.of("I volunteered to be part of the Safety Squad."),List.of("Я добровольно вступил в отряд безопасности.")),
        java.util.Map.entry(List.of("Wait, what?"),List.of("Подождите, что?")),
        java.util.Map.entry(List.of("What just happened?"),List.of("Что сейчас произошло?")),
        java.util.Map.entry(List.of("Yeah."),List.of("Да.")),
        java.util.Map.entry(List.of("How does this scam work?"),List.of("Как устроена эта афера?")),
        java.util.Map.entry(List.of("Yikes."),List.of("Жуть.")),
        java.util.Map.entry(List.of("It was quite a hectic day."),List.of("День выдался очень насыщенным.")),
        java.util.Map.entry(List.of("Okay!"),List.of("Хорошо!")),
        java.util.Map.entry(List.of("YEET FAIRY"),List.of("ШВЫРНУТЬ ФЕЮ")),
        java.util.Map.entry(List.of("What do you need?"),List.of("Что вам нужно?")),
        java.util.Map.entry(List.of("Do you have an Abiphone?"),List.of("У вас есть Abiphone?")),
        java.util.Map.entry(List.of("Sure...","You look like a mole yourself"),List.of("Ладно...","Ты и сам похож на крота")),
        java.util.Map.entry(List.of("I know that you are a mole"),List.of("Я знаю, что ты крот")),
        java.util.Map.entry(List.of("What's a pest?"),List.of("Что за вредители?")),
        java.util.Map.entry(List.of("What am I supposed to do about this?"),List.of("И что мне с этим делать?")),
        java.util.Map.entry(List.of("It was okay."),List.of("Нормально.")),
        java.util.Map.entry(List.of("I'm sorry but your cat is dead."),List.of("Мне жаль, но ваш кот мёртв.")),
        java.util.Map.entry(List.of("Yes!"),List.of("Да!")),
        java.util.Map.entry(List.of("NO","YES"),List.of("НЕТ","ДА")),
        java.util.Map.entry(List.of("❈","✧","⸕","☘","✎","❤"),List.of("❈","✧","⸕","☘","✎","❤")),
        java.util.Map.entry(List.of("No I don't, there is no Sun in here..."),List.of("Нет, здесь вообще нет солнца...")),
        java.util.Map.entry(List.of("3","6","9"),List.of("3","6","9")),
        java.util.Map.entry(List.of("12","18","24"),List.of("12","18","24")),
        java.util.Map.entry(List.of("30","40","50"),List.of("30","40","50")),
        java.util.Map.entry(List.of("40","50"),List.of("40","50")),
        java.util.Map.entry(List.of("Let's do it!","Maybe later"),List.of("Приступим!","Может, позже")),
        java.util.Map.entry(List.of("I sure am!","I'll come back later"),List.of("Ещё как!","Вернусь позже")),
        java.util.Map.entry(List.of("Can't help you with that, sorry"),List.of("Тут я помочь не могу, извини")),
        java.util.Map.entry(List.of("Sounds like a you problem."),List.of("Похоже, это твоя проблема.")),
        java.util.Map.entry(List.of("Hold up, how do you know that?"),List.of("Погоди, откуда ты это знаешь?")),
        java.util.Map.entry(List.of("I've had enough, this isn't fun anymore"),List.of("С меня хватит, это уже не смешно")),
        java.util.Map.entry(List.of("What's wrong?"),List.of("Что случилось?")),
        java.util.Map.entry(List.of("I promise.","Depends what we're dealing with here..."),List.of("Обещаю.","Смотря с чем мы имеем дело...")),
        java.util.Map.entry(List.of("I don't see any holes, or worms."),List.of("Не вижу ни дыр, ни червей.")),
        java.util.Map.entry(List.of("How am I supposed to help?"),List.of("И чем я могу помочь?")),
        java.util.Map.entry(List.of("Frogs? But you called them Wormholes..."),List.of("Лягушки? Но ты говоришь «червоточины»...")),
        java.util.Map.entry(List.of("That's a lot of yap."),List.of("Ну и болтовня.")),
        java.util.Map.entry(List.of("What are Elusive Shards?"),List.of("Что за осколки неуловимых существ?")),
        java.util.Map.entry(List.of("Yep!","Can you give me an example?"),List.of("Ага!","Можешь привести пример?")),
        java.util.Map.entry(List.of("Yep!","Can you give me another example"),List.of("Ага!","Можешь привести ещё пример?")),
        java.util.Map.entry(List.of("Yep!","Can you give me one last example?"),List.of("Ага!","Можешь привести последний пример?")),
        java.util.Map.entry(List.of("Yep!"),List.of("Ага!")),
        java.util.Map.entry(List.of("3","4","5"),List.of("3","4","5")),
        java.util.Map.entry(List.of("5,000","26,000","28,000","30,000","32,000","34,000","36,000","38,000","40,000","50,000","55,000"),List.of("5,000","26,000","28,000","30,000","32,000","34,000","36,000","38,000","40,000","50,000","55,000")),
        java.util.Map.entry(List.of("You okay?"),List.of("Ты в порядке?")),
        java.util.Map.entry(List.of("Cables?"),List.of("Кабели?")),
        java.util.Map.entry(List.of("What's my reward?"),List.of("А моя награда?")),
        java.util.Map.entry(List.of("Your brother sent me."),List.of("Меня послал твой брат.")),
        java.util.Map.entry(List.of("I don't know his name."),List.of("Я не знаю его имени.")),
        java.util.Map.entry(List.of("I am ill-prepared."),List.of("Я плохо подготовлен.")),
        java.util.Map.entry(List.of("I'm sorry."),List.of("Прости.")),
        java.util.Map.entry(List.of("Vaguely."),List.of("Краем уха.")),
        java.util.Map.entry(List.of("Get on with it."),List.of("Ближе к делу.")),
        java.util.Map.entry(List.of("Put it on my bill."),List.of("Запишите на мой счёт... то есть клюв.")),
        java.util.Map.entry(List.of("No, what's that?"),List.of("Нет, а что это?")),
        java.util.Map.entry(List.of("Yeah","Never heard of him"),List.of("Да","Никогда о нём не слышал")),
        java.util.Map.entry(List.of("Are you okay?"),List.of("Ты в порядке?")),
        java.util.Map.entry(List.of("Nice!","Suuuuuure..."),List.of("Круто!","Ну коне-е-ечно...")),
        java.util.Map.entry(List.of("Do you know where he is?"),List.of("Знаешь, где он?")),
        java.util.Map.entry(List.of("No."),List.of("Нет.")),
        java.util.Map.entry(List.of("Have you seen anything weird lately?"),List.of("В последнее время ничего странного не видел?")),
        java.util.Map.entry(List.of("How can I help?"),List.of("Чем я могу помочь?")),
        java.util.Map.entry(List.of("GIVE ITEM"),List.of("ПЕРЕДАТЬ ПРЕДМЕТ")),
        java.util.Map.entry(List.of("START CONTEST"),List.of("НАЧАТЬ СОСТЯЗАНИЕ")),
        java.util.Map.entry(List.of("Fairy Souls?"),List.of("Души фей?")),
        java.util.Map.entry(List.of("Uh... What?"),List.of("Эм... Что?")),
        java.util.Map.entry(List.of("Who are you?"),List.of("Кто ты?")),
        java.util.Map.entry(List.of("What are you doing here?"),List.of("Что ты здесь делаешь?")),
        java.util.Map.entry(List.of("Tell me more about these rewards."),List.of("Расскажи подробнее об этих наградах.")),
        java.util.Map.entry(List.of("Of course."),List.of("Конечно.")),
        java.util.Map.entry(List.of("LEAVE"),List.of("УЙТИ")),
        java.util.Map.entry(List.of("Yeah. Why is that?"),List.of("Да. Почему так?")),
        java.util.Map.entry(List.of("How do I prove that I'm strong enough?"),List.of("Как доказать, что я достаточно силён?")),
        java.util.Map.entry(List.of("Uh, yeah. I'm the best."),List.of("Эм, да. Я лучший.")),
        java.util.Map.entry(List.of("I need a new recommendation letter."),List.of("Мне нужно новое рекомендательное письмо.")),
        java.util.Map.entry(List.of("What happens if I have more Cold Resistance than Cold?"),List.of("А если сопротивление холоду выше самого холода?")),
        java.util.Map.entry(List.of("No, I am very silly","Yes, I am very smart"),List.of("Нет, я очень глупый","Да, я очень умный")),
        java.util.Map.entry(List.of("Sure let's go."),List.of("Конечно, вперёд.")),
        java.util.Map.entry(List.of("Can't say I have."),List.of("Не припомню.")),
        java.util.Map.entry(List.of("And how do I give it that 'little push'?"),List.of("А как мне её «подтолкнуть»?")),
        java.util.Map.entry(List.of("So I shoot rocks with a Time Gun to make them older?"),List.of("То есть стрелять в камни из Ружья времени, чтобы их состарить?")),
        java.util.Map.entry(List.of("Uhh, yes?"),List.of("Э-э, да?")),
        java.util.Map.entry(List.of("Wait... are you a talking pickaxe?"),List.of("Погоди... ты говорящая кирка?")),
        java.util.Map.entry(List.of("Well, you were just lying there..."),List.of("Ну, ты просто лежал здесь...")),
        java.util.Map.entry(List.of("You are a pickaxe that sells pickaxes?"),List.of("Ты кирка, которая продаёт кирки?")),
        java.util.Map.entry(List.of("You make a compelling point."),List.of("Убедительный довод.")),
        java.util.Map.entry(List.of("Nice... I'm happy for you?"),List.of("Здорово... я рад за тебя?")),
        java.util.Map.entry(List.of("Deface them?!"),List.of("Испачкать их?!")),
        java.util.Map.entry(List.of("What kind of game?"),List.of("Что за игра?")),
        java.util.Map.entry(List.of("Congratulations!"),List.of("Поздравляю!")),
        java.util.Map.entry(List.of("It is indeed!"),List.of("Да, именно так!")),
        java.util.Map.entry(List.of("Sure!"),List.of("Конечно!")),
        java.util.Map.entry(List.of("What Elite?"),List.of("Что за Элита?")),
        java.util.Map.entry(List.of("No"),List.of("Нет")),
        java.util.Map.entry(List.of("Trade","No thanks"),List.of("Обменять","Нет, спасибо")),
        java.util.Map.entry(List.of("He wears a gaslight on his head."),List.of("Он носит газовый фонарь на голове.")),
        java.util.Map.entry(List.of("They definitely look like me."),List.of("Они определённо похожи на меня.")),
        java.util.Map.entry(List.of("I don't have it yet."),List.of("У меня его пока нет.")),
        java.util.Map.entry(List.of("There you go."),List.of("Вот, держи.")),
        java.util.Map.entry(List.of("I lost my Golden Belt."),List.of("Я потерял свой золотой пояс.")),
        java.util.Map.entry(List.of("Buy tip"),List.of("Купить подсказку")),
        java.util.Map.entry(List.of("I'm here to cause trouble, of course","I have a letter of recommendation"),List.of("Разумеется, я здесь, чтобы устроить неприятности","У меня есть рекомендательное письмо")),
        java.util.Map.entry(List.of("I'm sorry but who are you?"),List.of("Простите, а вы кто?")),
        java.util.Map.entry(List.of("I suppose I don't have a choice in the matter."),List.of("Полагаю, выбора у меня нет.")),
        java.util.Map.entry(List.of("What's in it for me?"),List.of("А что я с этого получу?")),
        java.util.Map.entry(List.of("Okay, I'm listening..."),List.of("Ладно, я слушаю...")),
        java.util.Map.entry(List.of("I just want the Timecharm and I'm out."),List.of("Мне нужен только оберег времени, и я уйду.")),
        java.util.Map.entry(List.of("I got it."),List.of("Он у меня есть.")),
        java.util.Map.entry(List.of("Yep."),List.of("Ага.")),
        java.util.Map.entry(List.of("Indeed."),List.of("Верно.")),
        java.util.Map.entry(List.of(".tuo siht kcehc ,seY"),List.of(".итромсоп ,аД")),
        java.util.Map.entry(List.of("For sure."),List.of("Конечно.")),
        java.util.Map.entry(List.of("Yep, I got it."),List.of("Ага, он у меня есть.")),
        java.util.Map.entry(List.of("TELEPORT BACK"),List.of("ТЕЛЕПОРТИРОВАТЬСЯ НАЗАД")),
        java.util.Map.entry(List.of("That's not very polite"),List.of("Это не очень вежливо")),
        java.util.Map.entry(List.of("No. There is a darkness in Seraphine"),List.of("Нет. В Серафине есть тьма")),
        java.util.Map.entry(List.of("Barry said you could help"),List.of("Барри сказал, что ты можешь помочь")),
        java.util.Map.entry(List.of("What is the first step?"),List.of("С чего начать?")),
        java.util.Map.entry(List.of("You could just ask them why."),List.of("Можно просто спросить их почему.")),
        java.util.Map.entry(List.of("Hearing them out would help with the election."),List.of("Если их выслушать, это поможет на выборах.")),
        java.util.Map.entry(List.of("What...?"),List.of("Что?..")),
        java.util.Map.entry(List.of("Huhhh"),List.of("А-а-а?")),
        java.util.Map.entry(List.of("Slow down please."),List.of("Помедленнее, пожалуйста.")),
        java.util.Map.entry(List.of("Maybe you should explain that to Soma yourself..."),List.of("Может, тебе стоит самому объяснить это Соме...")),
        java.util.Map.entry(List.of("Why don't you just tell Soma what she owes?"),List.of("Почему бы просто не сказать Соме, сколько она должна?")),
        java.util.Map.entry(List.of("Who is Shen?"),List.of("Кто такой Шэнь?")),
        java.util.Map.entry(List.of("What else?"),List.of("Что ещё?")),
        java.util.Map.entry(List.of("Is there anything else?"),List.of("Есть что-нибудь ещё?")),
        java.util.Map.entry(List.of("But?"),List.of("Но?")),
        java.util.Map.entry(List.of("Are you lying?"),List.of("Ты лжёшь?")),
        java.util.Map.entry(List.of("I hope I never meet you again."),List.of("Надеюсь, мы больше никогда не встретимся.")),
        java.util.Map.entry(List.of("I'm starting to feel why."),List.of("Кажется, я начинаю понимать почему.")),
        java.util.Map.entry(List.of("Sure, what do you need?"),List.of("Конечно, что тебе нужно?")),
        java.util.Map.entry(List.of("Sure","No thanks..."),List.of("Конечно","Нет, спасибо...")),
        java.util.Map.entry(List.of("Ok."),List.of("Хорошо.")),
        java.util.Map.entry(List.of("Okay?"),List.of("Ладно?")),
        java.util.Map.entry(List.of("Wait you're scared of insects?"),List.of("Погоди, ты боишься насекомых?")),
        java.util.Map.entry(List.of("Don't you petsit spiders and bees?"),List.of("Разве ты не присматриваешь за пауками и пчёлами?")),
        java.util.Map.entry(List.of("Then why were you hired to disinfest?"),List.of("Тогда зачем тебя наняли уничтожать вредителей?")),
        java.util.Map.entry(List.of("I am sure","Fine I'll buy your sword"),List.of("Я не сомневаюсь","Ладно, я куплю твой меч")),
        java.util.Map.entry(List.of("Cloak","Belt","Necklace","Gauntlet","Armor"),List.of("Плащ","Пояс","Ожерелье","Рукавица","Броня")),
        java.util.Map.entry(List.of("Is it chum?"),List.of("Это прикормка?")),
        java.util.Map.entry(List.of("Have a look at this letter."),List.of("Взгляни на это письмо.")),
        java.util.Map.entry(List.of("What is it?"),List.of("Что это?")),
        java.util.Map.entry(List.of("Absolutely!","Let me think about it a bit more."),List.of("Безусловно!","Дай мне ещё немного подумать.")),
        java.util.Map.entry(List.of("I'm good to go!","Give me a moment."),List.of("Я готов!","Дай мне минутку.")),
        java.util.Map.entry(List.of("Wow, thanks!"),List.of("Ух ты, спасибо!")),
        java.util.Map.entry(List.of("Yes","Well","No"),List.of("Да","Ну...","Нет")),
        java.util.Map.entry(List.of("Of course!","Can you at least explain what the Great Spook is..."),List.of("Конечно!","Можешь хотя бы объяснить, что такое Великий ужас...")),
        java.util.Map.entry(List.of("Are you making progress at least?"),List.of("Ну хоть какие-то успехи есть?")),
        java.util.Map.entry(List.of("Sorry, what?"),List.of("Прости, что?")),
        java.util.Map.entry(List.of("You want me to cook for you now?"),List.of("Теперь ты хочешь, чтобы я готовил тебе еду?")),
        java.util.Map.entry(List.of("This isn't very healthy..."),List.of("Это не очень полезно для здоровья...")),
        java.util.Map.entry(List.of("Yes","Huh?"),List.of("Да","А?")),
        java.util.Map.entry(List.of("I can help","I still don't know what The Great Spook is..."),List.of("Я могу помочь","Я всё ещё не знаю, что такое Великий ужас...")),
        java.util.Map.entry(List.of("I don't understand, aren't we done?"),List.of("Не понимаю, разве мы не закончили?")),
        java.util.Map.entry(List.of("And did you succeed?"),List.of("И у тебя получилось?")),
        java.util.Map.entry(List.of("Why aren't you there yourself?"),List.of("Почему ты сам не там?")),
        java.util.Map.entry(List.of("Absolutely.","I'm doing it for gear."),List.of("Безусловно.","Я делаю это ради снаряжения.")),
        java.util.Map.entry(List.of("It was my destiny."),List.of("Такова была моя судьба.")),
        java.util.Map.entry(List.of("What is this place?"),List.of("Что это за место?")),
        java.util.Map.entry(List.of("Teleport to the Wizard's Path"),List.of("Переместиться на Тропу волшебника")),
        java.util.Map.entry(List.of("He barricaded himself inside, can you help us break in?"),List.of("Он забаррикадировался внутри. Поможешь нам пробраться?")),
        java.util.Map.entry(List.of("Absolutely."),List.of("Безусловно.")),
        java.util.Map.entry(List.of("Yep.","Actually, I want to do something else."),List.of("Ага.","Вообще-то я хочу заняться другим.")),
        java.util.Map.entry(List.of("Let's race again.","I would like to do another trial."),List.of("Давай ещё одну гонку.","Хочу пройти другое испытание.")),
        java.util.Map.entry(List.of("Bring it on!"),List.of("Давай, я готов!")),
        java.util.Map.entry(List.of("Let's brawl again.","I would like to do another trial."),List.of("Давай ещё раз подерёмся.","Хочу пройти другое испытание.")),
        java.util.Map.entry(List.of("I get it. Let's fight!"),List.of("Понятно. Давай сражаться!")),
        java.util.Map.entry(List.of("Yes please!","No thanks.","No, please don't ask me again."),List.of("Да, пожалуйста!","Нет, спасибо.","Нет, пожалуйста, больше не спрашивай."))
    );
    // Three actual story utterances only. The captured username is never replaced.
    private static final Pattern ROMERO_GREETING=Pattern.compile("^\\[NPC] Romero: It's you, ([A-Za-z0-9_]{1,16})!(?: \\((?:x|×)[0-9]{1,6}\\))?$");
    private static final Pattern ROMERO_WEDDING=Pattern.compile("^\\[NPC] Romero: ([A-Za-z0-9_]{1,16})! You're the best person to turn the day into something memorable!(?: \\((?:x|×)[0-9]{1,6}\\))?$");
    private static final Pattern JULIETTE_THANKS=Pattern.compile("^\\[NPC] Juliette: You're the best, ([A-Za-z0-9_]{1,16})!(?: \\((?:x|×)[0-9]{1,6}\\))?$");
    private static final Pattern FARMER_GREETING=Pattern.compile("^\\[NPC] Farmer Rigby: Howdy, ([A-Za-z0-9_]{1,16})!(?: \\((?:x|×)[0-9]{1,6}\\))?$");
    // Public Kat transcript + observed Mithril Golem result. Validate identity against pet titles.
    private static final String KAT_END="(?: \\((?:x|×)[0-9]{1,6}\\))?$";
    private static final String KAT_PET="([A-Za-z][A-Za-z '-]{0,60})";
    private static final String KAT_RARITY="(UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC)";
    private static final Pattern KAT_RESULT=Pattern.compile("^\\[NPC] Kat: I was able to upgrade your pet "+KAT_PET+" to "+KAT_RARITY+"\\."+KAT_END);
    private static final Pattern KAT_START=Pattern.compile("^\\[NPC] Kat: I'll get your "+KAT_PET+" upgraded to "+KAT_RARITY+" in no time!"+KAT_END);
    private static final Pattern KAT_CARE=Pattern.compile("^\\[NPC] Kat: I'm currently taking care of your "+KAT_PET+"!"+KAT_END);
    // Only the observed minute/second shape; do not infer unseen day/hour variants.
    private static final Pattern KAT_PICKUP_TIME=Pattern.compile("^\\[NPC] Kat: You can pick it up in ([0-9]{1,4}) minutes? ([0-5]?[0-9]) seconds?\\."+KAT_END);
    private static final Pattern SUSAN_VOLUNTEER=Pattern.compile("^\\[NPC] Susan: Awesome! You're our ([0-9]{1,12})(st|nd|rd|th) volunteer!"+KAT_END);
    private static final Pattern BRIGETTE_CHAIR=Pattern.compile("^\\[NPC] Brigette: Sorry ((?:\\[[A-Za-z0-9_]{1,16} head\\]|\\uFFFC)?)([A-Za-z0-9_]{1,16}), I'll go grab a chair real quick, and get someone to clean up this mess\\."+KAT_END);
    private static final Pattern ELIZABETH_HELLO=Pattern.compile("^\\[NPC] Elizabeth: Hey ((?:\\[[A-Za-z0-9_]{1,16} head\\]|\\uFFFC)?)([A-Za-z0-9_]{1,16})!"+KAT_END);
    // Two screenshot-confirmed crop-lab lines. Match only on receipt for these speakers.
    private static final Pattern BETH_HELP88=Pattern.compile("^\\[NPC] Beth: Jake, you've done incredible work here\\. But you don't have to do it alone anymore\\. Let me and "+MAXWELL_NAME70+" help you\\.$");
    private static final Pattern JAKE_TRUST88=Pattern.compile("^\\[NPC] Jake: And you, "+MAXWELL_NAME70+", you've been instrumental in getting us to this point\\. You should have access to the Crop Analyzer\\. You've earned my trust\\.$");
    // Screenshot-confirmed server notice; number is not an enchantment or Mk. tier.
    private static final Pattern ITEM_LEVEL=Pattern.compile("^LEVEL UP! Your ([^\\r\\n]{1,160}) just leveled up to Level ([1-9][0-9]{0,5})!(?: \\((?:x|×)[0-9]{1,6}\\))?$");
    // Seven source-recorded romance rewards. No arbitrary item/pet/name wildcard.
    private static final java.util.Map<String,String> LOVE_UPGRADES=java.util.Map.of(
        "Shiny Yellow Rock","Yellow Rock of Love", "Yellow Rock of Love","Mediocre Ring of Love",
        "Mediocre Ring of Love","Rubbish Ring of Love", "Rubbish Ring of Love","Modest Ring of Love",
        "Modest Ring of Love","Refined Ring of Love", "Refined Ring of Love","Classy Ring of Love",
        "Classy Ring of Love","Exquisite Ring of Love");
    private static final Pattern LOVE_UPGRADE=Pattern.compile("^Your (Shiny Yellow Rock|Yellow Rock of Love|Mediocre Ring of Love|Rubbish Ring of Love|Modest Ring of Love|Refined Ring of Love|Classy Ring of Love) was upgraded to (Yellow Rock of Love|Mediocre Ring of Love|Rubbish Ring of Love|Modest Ring of Love|Refined Ring of Love|Classy Ring of Love|Exquisite Ring of Love)!(?: \\((?:x|×)[0-9]{1,6}\\))?$");

    // Narrow source-backed Dungeon ability/healing forms. Names are never translated.
    private static final String DUNGEON_NUMBER="([0-9]{1,12}(?:\\.[0-9]{1,3})?|[1-9][0-9]{0,2}(?:,[0-9]{3}){1,3}(?:\\.[0-9]{1,3})?)";
    private static final String DUNGEON_PLAYER="([A-Za-z0-9_]{1,16})";
    private static final Pattern DUNGEON_COOLDOWN=Pattern.compile("^This ability is on cooldown for "+DUNGEON_NUMBER+"s\\.$");
    private static final Pattern DUNGEON_MANA=Pattern.compile("^You need at least "+DUNGEON_NUMBER+" mana to activate this!$");
    private static final Pattern DUNGEON_HIT=Pattern.compile("^Your (Guided Sheep|Implosion) hit ([0-9]{1,6}) enemy for "+DUNGEON_NUMBER+" damage\\.$");
    private static final Pattern DUNGEON_HEALED=Pattern.compile("^You were healed for "+DUNGEON_NUMBER+" health by "+DUNGEON_PLAYER+"!$");
    private static final Pattern DUNGEON_HEALER=Pattern.compile("^"+DUNGEON_PLAYER+" healed you for "+DUNGEON_NUMBER+" health!$");
    private static final Pattern DUNGEON_TETHER=Pattern.compile("^Your tether with "+DUNGEON_PLAYER+" healed you for "+DUNGEON_NUMBER+" health\\.$");
    private static final Pattern DUNGEON_FAIRY_SELF=Pattern.compile("^Your fairy healed (yourself) for "+DUNGEON_NUMBER+" health!$");
    private static final Pattern DUNGEON_FAIRY_PLAYER=Pattern.compile("^Your fairy healed "+DUNGEON_PLAYER+" for "+DUNGEON_NUMBER+" health!$");
    private static final Pattern DUNGEON_ABSORPTION=Pattern.compile("^You gained "+DUNGEON_NUMBER+" HP worth of absorption for (3)s from "+DUNGEON_PLAYER+"!$");
    private static final Pattern DUNGEON_STRENGTH=Pattern.compile("^"+DUNGEON_PLAYER+" granted you "+DUNGEON_NUMBER+" strength for (20) seconds!$");
    private static final Pattern DUNGEON_BERSERK=Pattern.compile("^\\[(Berserk)] (Melee Damage|Walk Speed) ([0-9]{1,9}(?:\\.[0-9]{1,3})?%?) -> ([0-9]{1,9}(?:\\.[0-9]{1,3})?%?)$");
    // Frozen SkyHanni DungeonChatFilter / DungeonBossApi source forms, not player prose.
    private static final Pattern DUNGEON_COUNTDOWN=Pattern.compile("^([A-Za-z0-9_]{1,16}) has started the dungeon countdown\\. The dungeon will begin in (1) minute\\.$");
    private static final Pattern DUNGEON_READY=Pattern.compile("^([A-Za-z0-9_]{1,16}) is now ready!$");
    private static final Pattern DUNGEON_START=Pattern.compile("^Dungeon starts in ([1-9][0-9]{0,2}) seconds\\.$");
    private static final Pattern DUNGEON_PROGRESS=Pattern.compile("^([A-Za-z0-9_]{1,16}) (activated|completed) a (lever|terminal|device)! \\(([0-9])/([1-9])\\)$");
    record Part(String text, int start, int end) {}
    // First-party SkyHanni matcher examples, pinned1de2862; not an observed channel capture.
    private static final Pattern EXPERIMENT_CLAIM=Pattern.compile("^You claimed the (Superpairs|Chronomatron|Ultrasequencer) rewards!$");
    private static final Pattern EXPERIMENT_CHARGE=Pattern.compile("^☕ You bought a bonus charge for the Experimentation Table! \\(([1-3])/3\\)$");
    private static final Pattern EXPERIMENT_XP_REWARD=Pattern.compile("^\\+([0-9]{1,9}(?:,[0-9]{3}){0,3}(?:\\.[0-9]{1,4})?[kMB]?) (Enchanting Exp)$");
    record Edit(int start, int end, List<Part> parts) {}
    private static final NamedNpcTemplates NAMED_NPC_TEMPLATES = new NamedNpcTemplates();
    private static final RyanDialogueTemplates RYAN_DIALOGUES = new RyanDialogueTemplates();
    private final OfflineDictionary dictionary;

    public ChatNotificationTranslator(OfflineDictionary dictionary) { this.dictionary = dictionary; }
    public int familyCount() { return 36; }
    public int dungeonFamilyCount() { return 6; }
    public int dungeonCombatFamilyCount() { return 14; }
    public int npcRequestFamilyCount() { return PABLO.length; }
    public int npcTemplateFamilyCount() { return PABLO.length + 15; }

    public Component translate(Component original, GuiMessageSource channel, boolean signed) {
        return render(original, channel, signed, false);
    }

    /** Separate NPC toggle: these requests are never part of ordinary notification translation. */
    public Component translateNpcRequest(Component original, GuiMessageSource channel, boolean signed) {
        if (channel != GuiMessageSource.SYSTEM_SERVER) return original;
        return render(original, channel, signed, true);
    }

    private Component render(Component original, GuiMessageSource channel, boolean signed, boolean npcRequest) {
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
            int paddingStart=offset;
            int bodyEnd=source.length();
            while(offset<bodyEnd&&source.charAt(offset)==' ')offset++;
            while(bodyEnd>offset&&source.charAt(bodyEnd-1)==' ')bodyEnd--;
            String body = source.substring(offset,bodyEnd);
            List<Edit> edits = npcRequest ? planNpcRequest(body) : planNotifications(body,channel,offset-paddingStart);
            if (edits.isEmpty()) return original;
            final int editOffset=offset;
            List<Edit> absolute = new ArrayList<>();
            for (Edit e : edits) absolute.add(new Edit(e.start + offset, e.end + offset,
                e.parts.stream().map(p -> new Part(p.text, p.start + editOffset, p.end + editOffset)).toList()));
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

    private List<Edit> planNpcRequest(String body) {
        // All reviewed NPC request grammars require this exact tag. Non-NPC
        // server notices have a separate handler; skip their NPC regex cascade.
        if(!body.startsWith("[NPC] "))return List.of();
        if (dictionary.hasCustomOverride(List.of(body,withoutDisplayCounter(body)))) return List.of();
        if(body.startsWith("[NPC] Ryan: "))return RYAN_DIALOGUES.plan(body,dictionary,NAMED_NPC_TEMPLATES);
        // Each new grammar is tested only for its closed speaker, on chat receipt.
        if(body.startsWith("[NPC] Maxwell: ")){var reply=planMaxwell70(body);if(!reply.isEmpty())return reply;}
        if(body.startsWith("[NPC] Beth: Jake, ")||body.startsWith("[NPC] Jake: And you, ")){var reply=planCropLab88(body);if(!reply.isEmpty())return reply;}
        if(body.startsWith("[NPC] Jacob: ")){var result=planJacob70(body);if(!result.isEmpty())return result;}
        if(body.startsWith("[NPC] Pesthunter Phillip: ")){
            var receipt=planPhillip69(body);if(!receipt.isEmpty())return receipt;
        }
        if(body.startsWith("[NPC] Maddox: ✆ Someone answers!")&&withoutDisplayCounter(body).equals("[NPC] Maddox: ✆ Someone answers!")&&!dictionary.hasCustomOverride(List.of("Someone answers!"))){var edits=new ArrayList<Edit>();literal(edits,body,"Someone answers!","Кто-то ответил!");return edits;}
        if(body.startsWith("[NPC] St. Jerry: 'Tis the ")){
            var greeting=ST_JERRY_GREETING.matcher(body);
            if(greeting.matches()&&!dictionary.hasCustomOverride(List.of("Season of Jerry"))){
                var edits=new ArrayList<Edit>();literal(edits,body,"'Tis the ","Наступил ");literal(edits,body,"Season of Jerry","сезон Джерри");return edits;
            }
        }
        // One prefix check on receipt; never a render-time/per-frame rule scan.
        if(body.startsWith("[NPC] Elizabeth: Hey ")){
            var hello=ELIZABETH_HELLO.matcher(body);
            if(hello.matches()&&!dictionary.hasCustomOverride(List.of("Hey "))
                &&(!hello.group(1).startsWith("[")||hello.group(1).equals("["+hello.group(2)+" head]"))){
                var edits=new ArrayList<Edit>();literal(edits,body,"Hey ","Привет, ");return edits;
            }
        }
        var kat=planKat(body);if(!kat.isEmpty())return kat;
        if(body.startsWith("[NPC] Kat: ")){var wait=planKatWait70(body);if(!wait.isEmpty())return wait;}
        var registration=SUSAN_VOLUNTEER.matcher(body);
        if(registration.matches()){
            var edits=new ArrayList<Edit>();literal(edits,body,"Awesome! You're our ","Отлично! Вы наш доброволец №");
            replace(edits,registration.start(2),registration.end(2),"");literal(edits,body," volunteer!","!");return edits;
        }
        var chair=BRIGETTE_CHAIR.matcher(body);
        if(chair.matches()){
            if(chair.group(1).startsWith("[")&&!chair.group(1).equals("["+chair.group(2)+" head]"))return List.of();
            var edits=new ArrayList<Edit>();literal(edits,body,"Sorry ","Извините, ");
            literal(edits,body,"I'll go grab a chair real quick, and get someone to clean up this mess.","я быстро принесу стул и попрошу кого-нибудь убрать этот беспорядок.");
            return edits; // Entire name/head/action span is untouched.
        }
        var greeting=ROMERO_GREETING.matcher(body);
        var farmer=FARMER_GREETING.matcher(body);
        // "friend" is also a fixed first-interaction word. A registered player token wins.
        if(farmer.matches()&&(!farmer.group(1).equals("friend")||PlayerNames.contains(farmer.group(1)))){var edits=new ArrayList<Edit>();literal(edits,body,"Howdy, ","Здравствуйте, ");return edits;}
        if(greeting.matches()){var edits=new ArrayList<Edit>();literal(edits,body,"It's you, ","Это вы, ");return edits;}
        greeting=JULIETTE_THANKS.matcher(body);
        if(greeting.matches()){
            // Same reviewed wording, but preserve the published highlight on "best".
            return NAMED_NPC_TEMPLATES.plan(body,dictionary);
        }
        greeting=ROMERO_WEDDING.matcher(body);
        if(greeting.matches()){
            return NAMED_NPC_TEMPLATES.plan(body,dictionary);
        }
        // Observed Garden dialogue: keep both NPC identities and the pest glyph.
        if (REQUEST_MATCH_0.matcher(body).matches()) {
            if (dictionary.hasCustomOverride(List.of("Pests"))) return List.of();
            List<Edit> edits=new ArrayList<>();
            literal(edits,body,"Let ","Сначала ");
            literal(edits,body," finish teaching you about "," должен закончить рассказывать вам о ");
            literal(edits,body,"Pests","вредителях");
            literal(edits,body,", then come chat with me!","; затем поговорите со мной!");
            return edits;
        }
        if(REQUEST_MATCH_1.matcher(body).matches()){
            var edits=new ArrayList<Edit>();literal(edits,body,"Howdy, ","Здравствуйте, ");return edits;
        }
        List<Edit> auction=planAuction(body);
        if(!auction.isEmpty())return auction;
        Matcher purchases=LUCIUS.matcher(body);
        if(purchases.matches()){
            if(dictionary.hasCustomOverride(List.of("Dark Auction")))return List.of();
            List<Edit> edits=new ArrayList<>();
            literal(edits,body,"You've purchased ","Ты купил ");
            replace(edits,purchases.end(1),purchases.start(2)," "+noun(purchases.group(1),"предмет","предмета","предметов")+" на ");
            replace(edits,purchases.start(2),purchases.end(2),"Тёмном аукционе");
            return edits; // Digits retain their original per-character styles/actions.
        }
        List<Edit> sister=planSister(body);
        if(!sister.isEmpty())return sister;
        Matcher meetings = DALIR.matcher(body);
        if (meetings.matches()) {
            if (dictionary.hasCustomOverride(List.of(meetings.group(2)))) return List.of();
            String reward=dictionary.translateForSearch(meetings.group(3));
            if (reward.equals(meetings.group(3))) return List.of();
            List<Edit> edits=new ArrayList<>();
            literal(edits,body,"You’ve now attended ","Ты уже побывал на ");
            replace(edits,meetings.end(1),meetings.start(2)," "+noun(meetings.group(1),"встрече","встречах","встречах")+" ");
            replace(edits,meetings.start(2),meetings.end(2),"Культа Падшей звезды");
            replace(edits,meetings.end(2),meetings.start(3),"! В награду — ");
            replace(edits,meetings.start(3),meetings.end(3),reward);
            return edits;
        }
        Matcher hunt = TREVOR.matcher(body);
        if (hunt.matches()) {
            String place = dictionary.translateForSearch(hunt.group(2));
            if (place.equals(hunt.group(2))) return List.of();
            List<Edit> edits = new ArrayList<>();
            literal(edits,body,"You can find your ","Ищи животное категории «");
            replace(edits,hunt.end(1),hunt.start(2),"» возле локации «");
            replace(edits,hunt.start(2),hunt.end(2),place);
            replace(edits,hunt.end(2),body.length(),"».");
            return edits;
        }
        Matcher count = JACOB_COLLECTED.matcher(withoutDisplayCounter(body));
        if (count.matches()) {
            List<Edit> edits = new ArrayList<>();
            literal(edits, body, "You collected ", "Ты собрал ");
            // Numeric trailing space may be yellow while "items" is white.
            replace(edits, count.end(1)+1, count.end(1)+" items".length(), noun(count.group(1),"предмет","предмета","предметов"));
            if (count.group(2) != null) literal(edits,body,"PERSONAL BEST","ЛИЧНЫЙ РЕКОРД");
            return edits; // Count and its original per-character styles are not rewritten.
        }
        count = JACOB_PREVIOUS.matcher(body);
        if (count.matches()) {
            List<Edit> edits = new ArrayList<>();
            literal(edits, body, "Your previous Personal Best was ", "Твой предыдущий личный рекорд — ");
            return edits;
        }
        Matcher bonus = JACOB_FORTUNE.matcher(body);
        if (bonus.matches()) {
            String crop = dictionary.translateForSearch(bonus.group(3));
            if (crop.equals(bonus.group(3))) return List.of();
            List<Edit> edits = new ArrayList<>();
            literal(edits, body, "Your ", "Бонус «");
            replace(edits,bonus.start(1),bonus.end(1),"Личные рекорды");
            literal(edits, body, " perk is now granting you ", "» теперь даёт ");
            // Move only the semantic words; retain the bonus, glyph and crop actions.
            edits.add(new Edit(bonus.end(2),bonus.end(4),List.of(
                new Part(" к удаче при сборе культуры «",bonus.start(4),bonus.end(4)),
                new Part(crop,bonus.start(3),bonus.end(3)),
                new Part("»",bonus.start(4),bonus.end(4)))));
            return edits;
        }
        for (int index=0; index<PABLO.length; index++) {
            Matcher m=PABLO[index].matcher(body);
            if (!m.matches()) continue;
            String flower=dictionary.translateForSearch(m.group(1));
            if (flower.equals(m.group(1)) || dictionary.hasCustomOverride(List.of(body))) return List.of();
            int start="[NPC] Pablo: ".length();
            if (body.startsWith("✆ ",start)) start+=2;
            String lead=switch(index) {
                case 0 -> "Ты свободен? Сегодня мне очень нужен этот цветок: ";
                case 1 -> "Принеси мне этот цветок как можно скорее: ";
                case 2 -> "Можешь принести мне этот цветок: ";
                default -> "Сегодня мне очень нужен этот цветок: ";
            };
            String tail=switch(index) {
                case 0 -> ".";
                case 1 -> "!";
                case 2 -> "?";
                default -> ". У тебя найдётся один для меня?";
            };
            List<Edit> edits=new ArrayList<>();
            replace(edits,start,m.start(1),lead);
            replace(edits,m.start(1),m.end(1),flower);
            replace(edits,m.end(1),body.length(),tail);
            return edits;
        }
        return NAMED_NPC_TEMPLATES.plan(body,dictionary);
    }

    private List<Edit> planAuction(String body){
        Matcher m=SIRIUS_LOT.matcher(body);List<Edit> edits=new ArrayList<>();
        if(m.matches()){
            if(dictionary.hasCustomOverride(List.of(m.group(2),"Coins","Protection","Book")))return List.of();
            replace(edits,m.start(1),m.end(1),m.group(1).equals("First")?"Первый лот":"Следующий лот");
            replace(edits,m.end(1),m.start(2),": ");
            auctionItem(edits,m,2);
            replace(edits,m.end(2),m.start(3),". Начальная ставка — ");
            replace(edits,m.start(4),m.end(4),noun(m.group(3),"монета","монеты","монет"));
        }else if((m=SIRIUS_BID.matcher(body)).matches()){
            if(dictionary.hasCustomOverride(List.of("Coins"))||m.group(4)!=null&&dictionary.hasCustomOverride(List.of(m.group(4),"Protection","Book")))return List.of();
            literal(edits,body,"The highest bidder was ","Наибольшую ставку сделал ");
            replace(edits,m.end(1),m.start(2)," — ");
            replace(edits,m.start(3),m.end(3),noun(m.group(2),"монета","монеты","монет"));
            if(m.group(4)!=null)auctionItem(edits,m,4);
            // The player's name and every price glyph are never replacement spans.
        }else if((m=SIRIUS_NO_BID.matcher(body)).matches()){
            if(m.group(2)!=null&&dictionary.hasCustomOverride(List.of(m.group(2),"Protection","Book")))return List.of();
            literal(edits,body,"No one made any bids!","Никто не сделал ставку!");
            if(m.group(1)!=null)replace(edits,m.start(1),m.end(1)," Перейдём к следующему лоту.");
            if(m.group(2)!=null)auctionItem(edits,m,2);
        }
        return edits;
    }
    private void auctionItem(List<Edit> edits,Matcher m,int group){
        String name=m.group(group),translated=dictionary.translateForSearch(name);
        // One primary example is an enchantment book; preserve its original Roman level.
        if(REQUEST_MATCH_2.matcher(name).matches()){
            String enchant=dictionary.translateForSearch("Protection");
            if(!enchant.equals("Protection")){
                int start=m.start(group),book=start+name.length()-4,levelEnd=start+name.length()-5;
                List<Part> parts=new ArrayList<>();
                parts.add(new Part("Книга «",book,book+4));parts.add(new Part(enchant,start,start+10));
                parts.add(new Part(" ",start+10,start+11));
                for(int i=start+11;i<levelEnd;i++)parts.add(new Part(m.group().substring(i,i+1),i,i+1));
                parts.add(new Part("»",book,book+4));
                edits.add(new Edit(start,m.end(group),parts));return;
            }
        }
        if(!translated.equals(name))replace(edits,m.start(group),m.end(group),translated);
    }

    private List<Edit> planSister(String body) {
        List<Edit> edits=new ArrayList<>();
        Matcher m=SISTER_LOG_RECORD.matcher(body);
        if(m.matches()) {
            String wood=dictionary.translateForSearch(m.group(3));
            if(wood.equals(m.group(3)))return List.of();
            literal(edits,body,"PERSONAL BEST","ЛИЧНЫЙ РЕКОРД");
            literal(edits,body,"! You've surpassed your previous record of ","! Ты побил свой прежний рекорд в моём соревновании: ");
            int log=body.indexOf("logs",m.end(3));
            edits.add(new Edit(m.start(3),body.length()-1,List.of(
                new Part(noun(m.group(2),"бревно","бревна","брёвен"),log,log+4),
                new Part(" (",m.end(3),log),new Part(wood,m.start(3),m.end(3)),
                new Part(")",log+4,body.length()-1))));
        } else if((m=SISTER_BRACKET.matcher(body)).matches()) {
            literal(edits,body,"You reached the ","Ты попал в категорию ");
            literal(edits,body," Bracket in my contest!"," в моём соревновании!");
        } else if((m=SISTER_POINTS.matcher(body)).matches()) {
            literal(edits,body,"You earned a total of ","Ты набрал ");
            replace(edits,m.end(2),m.end(2)+" points".length()," "+noun(m.group(2),"очко","очка","очков"));
            if(m.group(3)!=null) {
                literal(edits,body," That's a new "," Это новый ");
                literal(edits,body,"PERSONAL BEST","ЛИЧНЫЙ РЕКОРД");
            }
        } else if((m=SISTER_PREVIOUS.matcher(body)).matches()) {
            literal(edits,body,"Your previous Personal Best was ","Твой предыдущий личный рекорд — ");
        } else if((m=SISTER_CLAIM.matcher(body)).matches()) {
            String place=dictionary.translateForSearch(m.group(2));
            if(place.equals(m.group(2)))return List.of();
            literal(edits,body,"Come see me at ","Приходи ко мне в локацию «");
            replace(edits,m.start(2),m.end(2),place);
            literal(edits,body," to claim your rewards!","», чтобы забрать награды!");
        } else if(SISTER_KEEP.matcher(body).matches()) {
            literal(edits,body,"Keep it up!","Так держать!");
        }
        return edits;
    }

    private record QuizLine66(String source,String target) {}
    // Closed, observed server quiz. No session state, runtime regex, or retained Style.
    private static final Map<String,List<QuizLine66>> SAFETY_QUIZ66=safetyQuiz66();
    private static Map<String,List<QuizLine66>> safetyQuiz66(){
        var rows=new java.util.HashMap<String,List<QuizLine66>>();
        for(String[] pair:new String[][]{
            {"Why is account security important?","Почему важна безопасность аккаунта?"},
            {"If someone in your party asks you to join their Discord to","Если участник группы зовёт вас в Discord,"},
            {"voice chat, and sends the invite code in chat without the","чтобы поговорить, и присылает код приглашения без"},
            {"\"discord.gg\", what should you do?","«discord.gg», что следует сделать?"},
            {"Your friend sends you a link telling you to go vote for their","Друг присылает ссылку и просит проголосовать за него."},
            {"prize, You click the link, and it's asking you to log in with an","Вы переходите по ней, а сайт требует войти"},
            {"account. What should you do?","в аккаунт. Что следует сделать?"}})
            rows.put(pair[0],List.of(new QuizLine66(pair[0],pair[1])));
        for(String level:List.of("1","2","3"))rows.put("Question #"+level,List.of(new QuizLine66("Question #","Вопрос №")));
        for(var answers:List.of(
            List.of(new QuizLine66("It can lead you to lose a lot of items and money.","Можно потерять много предметов и денег."),new QuizLine66("It can lead you to lose access to your Minecraft account.","Можно потерять доступ к аккаунту Minecraft."),new QuizLine66("It can lead you to lose your Hypixel SkyBlock profile.","Можно потерять профиль Hypixel SkyBlock.")),
            List.of(new QuizLine66("Manually type the invite link in the browser and join the discord.","Вручную ввести ссылку-приглашение в браузере и зайти в Discord."),new QuizLine66("Leave the party and report the player.","Покинуть группу и пожаловаться на игрока."),new QuizLine66("Add the player on discord and ask for the invite link there.","Добавить игрока в Discord и попросить ссылку там.")),
            List.of(new QuizLine66("Register for an account and log in.","Создать аккаунт и войти."),new QuizLine66("Use a linked account (Google, Facebook etc.) to log in.","Войти через связанный аккаунт (Google, Facebook и т. д.)."),new QuizLine66("Look for signs of your friend getting hacked, and don't click on that website any further.","Проверить, не взломали ли друга, и больше ничего не нажимать на этом сайте.")))){
            var full=new StringBuilder("\n    @ ");
            for(int i=0;i<answers.size();i++){if(i>0)full.append(" \n    @ ");full.append(answers.get(i).source());}
            rows.put(full.toString(),answers);
            // Same captured controls may arrive as separate messages after another mod's wrapping.
            for(var answer:answers)rows.put("@ "+answer.source(),List.of(answer));
        }
        return Map.copyOf(rows);
    }
    private List<Edit> planSafetyQuiz66(String body){
        var rows=SAFETY_QUIZ66.get(body);if(rows==null)return List.of();
        if(dictionary.hasCustomOverride(List.of(body,"Account Safety")))return List.of();
        var edits=new ArrayList<Edit>();
        for(var row:rows){if(dictionary.hasCustomOverride(List.of(row.source())))return List.of();literal(edits,body,row.source(),row.target());}
        return edits;
    }
    private List<Edit> planNotifications(String body,GuiMessageSource channel,int padding){
        if(channel==GuiMessageSource.SYSTEM_SERVER){var quiz=planSafetyQuiz66(body);if(!quiz.isEmpty())return quiz;}
        var slayer=planSlayer(body,channel);if(!slayer.isEmpty())return slayer;
        if(channel==GuiMessageSource.SYSTEM_SERVER){
            var experiment=planExperiment(body);if(!experiment.isEmpty())return experiment;
            var centered=planCenteredBestiaryReward(body,padding);if(!centered.isEmpty())return centered;
            var milestone=planBestiaryMilestone(body);if(!milestone.isEmpty())return milestone;
            var safety=planSafetyChoices(body);if(!safety.isEmpty())return safety;
            var love=planLoveUpgrade(body);if(!love.isEmpty())return love;
            var leveled=planItemLevel(body);if(!leveled.isEmpty())return leveled;
            Matcher choice=STORY_YES_NO.matcher(body);
            boolean yesNo=choice.matches();
            if(!yesNo)choice=STORY_FACTION.matcher(body);
            if(yesNo||choice.matches()){
                if(dictionary.hasCustomOverride(List.of(body,"Select an option:",choice.group(1),choice.group(2))))return List.of();
                var edits=new ArrayList<Edit>();
                literal(edits,body,"Select an option:",yesNo?"Выберите ответ:":"Выберите фракцию:");
                replace(edits,choice.start(1),choice.end(1),yesNo?"НЕТ":"ВАРВАРЫ");
                replace(edits,choice.start(2),choice.end(2),yesNo?"ДА":"МАГИ");
                return edits; // Only visible labels; original click commands/hover remain in their own spans.
            }
            if(body.equals("Plant 2 carrots next to each other and water them in your Greenhouse")&&!dictionary.hasCustomOverride(List.of(body))){
                var edits=new ArrayList<Edit>();literal(edits,body,"Plant ","Посадите ");literal(edits,body," carrots next to each other and water them in your Greenhouse"," моркови рядом друг с другом в теплице и полейте их");return edits;
            }
            String objective=switch(body){
                case "NEW OBJECTIVE" -> "НОВАЯ ЦЕЛЬ";
                case "Plant a carrot" -> "Посадите морковь";
                case "Water the carrot" -> "Полейте морковь";
                case "Wait for the carrot to grow" -> "Дождитесь, пока морковь вырастет";
                case "Harvest the carrot" -> "Соберите морковь";
                case "Wait for the Greenhouse to complete building" -> "Дождитесь завершения строительства теплицы";
                case "Talk to Sam at the Greenhouse" -> "Поговорите с Sam в теплице";
                case "Analyze growing crops in the Greenhouse with the Plant Diagnostics Tool" -> "Изучите растущие культуры в теплице с помощью анализатора растений";
                case "Spread a mutation in the Greenhouse" -> "Получите мутацию в теплице";
                case "Harvest the mutation in the Greenhouse" -> "Соберите мутировавшую культуру в теплице";
                case "Talk to the Carpenter in the Greenhouse" -> "Поговорите с Плотником в теплице";
                default -> null;
            };
            if(objective!=null&&!dictionary.hasCustomOverride(List.of(body))){
                var edits=new ArrayList<Edit>();replace(edits,0,body.length(),objective);return edits;
            }
            String hydro=withoutDisplayCounter(body);
            if(!dictionary.hasCustomOverride(List.of(body,hydro))&&hydro.equals("Your HydroCan™ 1000 is out of water!")){
                var edits=new ArrayList<Edit>();literal(edits,body,"Your ","В лейке ");literal(edits,body," is out of water!"," закончилась вода!");return edits;
            }
            if(!dictionary.hasCustomOverride(List.of(body,hydro))&&hydro.equals("You don't have water in your HydroCan...")){
                var edits=new ArrayList<Edit>();literal(edits,body,"You don't have water in your ","В вашей лейке ");literal(edits,body,"..."," нет воды...");return edits;
            }
        }
        if(channel==GuiMessageSource.SYSTEM_SERVER){var observed=planDocumentNotifications(body);if(!observed.isEmpty())return observed;}
        if(channel==GuiMessageSource.SYSTEM_SERVER){var collection=planCollections(body);if(!collection.isEmpty())return collection;}
        List<Edit> dungeon=channel==GuiMessageSource.SYSTEM_SERVER?planDungeon(body):List.of();
        if(dungeon.isEmpty()&&channel==GuiMessageSource.SYSTEM_SERVER)dungeon=planDungeonCombat(body);
        return dungeon.isEmpty()?plan(body):dungeon;
    }

    public int katPetFamilyCount(){return 3;}
    public int slayerNotificationFamilyCount(){return 12;}
    private List<Edit> planSlayer(String body,GuiMessageSource channel){
        if(body.isEmpty())return List.of();
        // Cheap literal rejection before any new regex/override allocation.
        boolean mod=body.startsWith("[SkyHanni] You have gained ")||body.startsWith("[SkyHanni] Slayer quest took ")||body.startsWith("[Skyblocker] Slayer убит за ");
        boolean game=channel==GuiMessageSource.SYSTEM_SERVER&&(body.startsWith("SLAYER ")||body.startsWith("NICE! SLAYER ")||body.startsWith("Your Slayer ")||body.startsWith("» Talk to Maddox ")||body.startsWith("» Slay ")||body.startsWith("✆ RING")||body.startsWith("RING...")||body.startsWith("Zombie Slayer LVL ")||body.startsWith("Spider Slayer LVL ")||body.startsWith("Wolf Slayer LVL ")||body.startsWith("Enderman Slayer LVL ")||body.startsWith("Blaze Slayer LVL ")||body.startsWith("Vampire Slayer LVL "));
        if(!mod&&!game)return List.of();
        String originalBody=body;body=withoutDisplayCounter(body);
        if(dictionary.hasCustomOverride(List.of(originalBody,body,"Slayer")))return List.of();
        var edits=new ArrayList<Edit>();Matcher m;
        if(game&&(m=SLAYER_SPAWN.matcher(body)).matches()){
            if(!InterfaceDisplay.roman(m.group(2))||dictionary.hasCustomOverride(List.of("SLAYER BOSS!",m.group(1),"☠ "+m.group(1))))return List.of();
            literal(edits,body,"SLAYER BOSS!","БОСС ОХОТЫ!");literal(edits,body,"The ","");replace(edits,m.start(1),m.end(1),SLAYER_BOSSES.get(m.group(1)));literal(edits,body," spawned!"," появился!");
        }else if(game&&(m=SLAYER_HEAL.matcher(body)).matches()){
            if(dictionary.hasCustomOverride(List.of("HP","healing","Slayer")))return List.of();
            replace(edits,0,m.start(1),"За убийство босса — лечение: ");literal(edits,body,"HP","здоровья");literal(edits,body," healing for "," на ");literal(edits,body," seconds!"," с!");
        }else if(game&&body.equals("NICE! SLAYER BOSS SLAIN!")){
            literal(edits,body,"NICE!","ОТЛИЧНО!");literal(edits,body,"SLAYER BOSS SLAIN!","БОСС ПОБЕЖДЁН!");
        }else if(game&&(m=SLAYER_CLAIM.matcher(body)).matches()){
            if(dictionary.hasCustomOverride(List.of("Maddox",m.group(2)+" Slayer","XP")))return List.of();
            literal(edits,body,"Talk to ","Поговорите с ");replace(edits,m.start(1),m.end(1),"Мэддоксом");literal(edits,body," to claim your "," и заберите ");
            int slayerWord=m.end(2)+1,xpWord=body.lastIndexOf("XP!");
            edits.add(new Edit(m.start(2),body.length(),List.of(new Part("опыт ",xpWord,xpWord+2),new Part("охоты на ",slayerWord,slayerWord+6),new Part(SLAYER_TYPES.get(m.group(2)),m.start(2),m.end(2)),new Part("!",body.length()-1,body.length()))));
        }else if(game&&(body.equals("SLAYER QUEST COMPLETE!")||body.equals("SLAYER QUEST STARTED!"))){
            literal(edits,body,"SLAYER QUEST","ЗАДАНИЕ ОХОТЫ");literal(edits,body,body.endsWith("COMPLETE!")?"COMPLETE!":"STARTED!",body.endsWith("COMPLETE!")?"ЗАВЕРШЕНО!":"НАЧАТО!");
        }else if(game&&(m=SLAYER_LEVEL.matcher(body)).matches()){
            if(m.group(3)!=null&&Integer.parseInt(m.group(3))!=Integer.parseInt(m.group(2))+1||dictionary.hasCustomOverride(List.of(m.group(1)+" Slayer","LVL","XP")))return List.of();
            replace(edits,m.start(1),m.end(1),"Охота на "+SLAYER_TYPES.get(m.group(1)));literal(edits,body," Slayer LVL ",", ур. ");
            if(m.group(3)==null)replace(edits,m.end(2),m.start(4)," — до следующего уровня: ");
            else{replace(edits,m.end(2),m.start(3)," — до ур. ");literal(edits,body," in ",": ");}
            literal(edits,body,"XP","опыта");
        }else if(game&&(m=SLAYER_QUOTA.matcher(body)).matches()){
            if(dictionary.hasCustomOverride(List.of("Combat XP",m.group(2))))return List.of();String mob=switch(m.group(2)){case "Zombies"->"зомби";case "Spiders"->"пауков";case "Wolves"->"волков";case "Endermen"->"эндерменов";case "Blazes"->"всполохов";default->"вампиров";};
            literal(edits,body,"Slay ","Наберите ");literal(edits,body,"Combat XP","опыта боя");literal(edits,body," worth of ",", убивая ");replace(edits,m.start(2),m.end(2),mob);
        }else if(game&&List.of("RING...","RING... RING...","RING... RING... RING...","✆ RING...","✆ RING... RING...","✆ RING... RING... RING...").contains(body)){
            if(dictionary.hasCustomOverride(List.of("RING...")))return List.of();for(int at=body.indexOf("RING...");at>=0;at=body.indexOf("RING...",at+7))replace(edits,at,at+7,"ЗВОНОК...");
        }else if(mod&&(m=SLAYER_BITS.matcher(body)).matches()){
            if(dictionary.hasCustomOverride(List.of("Bits")))return List.of();literal(edits,body,"You have gained ","Вы получили ");literal(edits,body,"Bits",noun(m.group(1),"бит","бита","битов"));
        }else if(mod&&(m=SLAYER_TIME.matcher(body)).matches()){
            literal(edits,body,"Slayer quest took ","Задание охоты выполнено за ");replace(edits,m.start(2),m.end(2)," с");literal(edits,body," to complete.",".");
        }else if(mod&&(m=SLAYER_KILL_TIME.matcher(body)).matches()){
            literal(edits,body,"Slayer убит за ","Босс побеждён за ");replace(edits,m.start(2),m.end(2)," с");
        }
        return edits;
    }
    private List<Edit> planExperiment(String original){
        if(!original.startsWith("You claimed the ")&&!original.startsWith("☕ You bought a bonus charge for the ")&&!original.startsWith("+")&&!original.startsWith("XX ULTRA-RARE BOOK!"))return List.of();
        String body=withoutDisplayCounter(original);Matcher m;
        if(dictionary.hasCustomOverride(List.of(original,body,"Experimentation Table")))return List.of();
        var edits=new ArrayList<Edit>();
        if((m=EXPERIMENT_CLAIM.matcher(body)).matches()){
            if(dictionary.hasCustomOverride(List.of(m.group(1),"rewards")))return List.of();
            literal(edits,body,"You claimed the ","Вы получили награды за ");
            replace(edits,m.start(1),m.end(1),ExperimentDisplay.GAMES.get(m.group(1)));
            literal(edits,body," rewards!","!");
        }else if((m=EXPERIMENT_CHARGE.matcher(body)).matches()){
            literal(edits,body,"You bought a bonus charge for the ","Вы купили дополнительную попытку для ");
            literal(edits,body,"Experimentation Table","стола экспериментов");
        }else if((m=EXPERIMENT_XP_REWARD.matcher(body)).matches()){
            if(dictionary.hasCustomOverride(List.of("Enchanting Exp")))return List.of();
            replace(edits,m.start(2),m.end(2),"опыта зачарования");
        }else if(body.equals("XX ULTRA-RARE BOOK! XX")){
            if(dictionary.hasCustomOverride(List.of("ULTRA-RARE BOOK!")))return List.of();
            literal(edits,body,"ULTRA-RARE BOOK!","ОЧЕНЬ РЕДКАЯ КНИГА!");
        }
        return edits;
    }
    public int experimentNotificationFamilyCount(){return 4;}
    public int itemLevelFamilyCount(){return 1;}
    public int loveUpgradeFamilyCount(){return 1;}
    private List<Edit> planCenteredBestiaryReward(String body,int padding){
        // Actual latest.log 00:06:30: distinct messages, 36/34 centering spaces.
        // Closed centered server notice shape; no history/state/style cache and no
        // unpadded standalone REWARD/Health translation. Preserve all original spaces.
        if(padding<16||padding>60)return List.of();
        if(body.equals("REWARD")){
            if(dictionary.hasCustomOverride(List.of(body)))return List.of();
            var edits=new ArrayList<Edit>();replace(edits,0,body.length(),"НАГРАДА");return edits;
        }
        if(!body.startsWith("+")||!body.endsWith(" Health")||!MILESTONE_HEALTH.matcher(body).matches()
            ||dictionary.hasCustomOverride(List.of(body,"Health")))return List.of();
        var edits=new ArrayList<Edit>();literal(edits,body,"Health","Здоровье");return edits;
    }
    private List<Edit> planBestiaryMilestone(String body){
        if(body.equals("CLICK HERE to claim it!")||body.equals(">>> CLICK HERE to claim it! <<<")){
            if(dictionary.hasCustomOverride(List.of(body,"CLICK HERE","to claim it!")))return List.of();
            var edits=new ArrayList<Edit>();literal(edits,body,"CLICK HERE","НАЖМИТЕ ЗДЕСЬ");
            literal(edits,body,"to claim it!","чтобы забрать награду!");return edits;
        }
        // One literal guard on receipt; no new regex cascade or render-time state.
        if(body.indexOf(MILESTONE_HEADER)<0)return List.of();
        if(body.equals(MILESTONE_HEADER)){
            if(dictionary.hasCustomOverride(List.of(body)))return List.of();
            var edits=new ArrayList<Edit>();replace(edits,0,body.length(),"ЭТАП БЕСТИАРИЯ");return edits;
        }
        String[] rows=body.split("\\n",-1);if(rows.length>16)return List.of();
        var lines=new ArrayList<String>();var offsets=new ArrayList<Integer>();int offset=0;
        for(String row:rows){
            int start=0,end=row.length();var time=TIME.matcher(row);if(time.find())start=time.end();
            while(start<end&&row.charAt(start)==' ')start++;while(end>start&&row.charAt(end-1)==' ')end--;
            String line=row.substring(start,end);
            if(!line.isEmpty()&&!milestoneSeparator(line)){lines.add(line);offsets.add(offset+start);}
            offset+=row.length()+1;
        }
        if(lines.size()!=4||!lines.get(0).equals(MILESTONE_HEADER)||!lines.get(2).equals("REWARD"))return List.of();
        var tiers=MILESTONE_TIERS.matcher(lines.get(1));
        if(!tiers.matches()||!InterfaceDisplay.roman(tiers.group(1))||!InterfaceDisplay.roman(tiers.group(2))
            ||!MILESTONE_HEALTH.matcher(lines.get(3)).matches())return List.of();
        if(dictionary.hasCustomOverride(List.of(body,MILESTONE_HEADER,"REWARD","Health",lines.get(3))))return List.of();
        var edits=new ArrayList<Edit>();
        replace(edits,offsets.get(0),offsets.get(0)+MILESTONE_HEADER.length(),"ЭТАП БЕСТИАРИЯ");
        replace(edits,offsets.get(2),offsets.get(2)+"REWARD".length(),"НАГРАДА");
        int health=offsets.get(3)+lines.get(3).length()-"Health".length();
        replace(edits,health,health+"Health".length(),"Здоровье");return edits;
    }
    private static boolean milestoneSeparator(String line){
        if(line.length()<10||line.length()>120)return false;
        for(int i=0;i<line.length();i++)if("-─━▬═".indexOf(line.charAt(i))<0)return false;
        return true;
    }
    private List<Edit> planLoveUpgrade(String body){
        var m=LOVE_UPGRADE.matcher(body);if(!m.matches()||!java.util.Objects.equals(LOVE_UPGRADES.get(m.group(1)),m.group(2)))return List.of();
        if(dictionary.hasCustomOverride(List.of(body,withoutDisplayCounter(body),m.group(1),m.group(2))))return List.of();
        String old=dictionary.translateForSearch(m.group(1)),next=dictionary.translateForSearch(m.group(2));
        if(old.equals(m.group(1))||next.equals(m.group(2)))return List.of();
        var edits=new ArrayList<Edit>();replace(edits,0,m.start(1),"Ваш предмет «");
        replace(edits,m.start(1),m.end(1),old);replace(edits,m.end(1),m.start(2),"» улучшен до «");
        replace(edits,m.start(2),m.end(2),next);replace(edits,m.end(2),m.end(2)+1,"»!");return edits;
    }
    private List<Edit> planSafetyChoices(String body){
        if(!body.startsWith("Select an option:"))return List.of();
        String base=withoutDisplayCounter(body);if(!SAFETY_LAYOUT.matcher(base).matches())return List.of();
        var labels=new ArrayList<String>();var ranges=new ArrayList<int[]>();var buttons=SAFETY_BUTTON.matcher(base);
        while(buttons.find()){labels.add(buttons.group(1));ranges.add(new int[]{buttons.start(1),buttons.end(1)});}
        var targets=SAFETY_CHOICES.get(labels);if(targets==null)return List.of();
        var keys=new ArrayList<String>(labels);keys.add(body);keys.add(base);keys.add(base.stripTrailing());keys.add("Select an option:");keys.add("Safety Squad");keys.add("Account Safety");
        if(dictionary.hasCustomOverride(keys))return List.of();
        var edits=new ArrayList<Edit>();literal(edits,body,"Select an option:","Выберите ответ:");
        for(int i=0;i<labels.size();i++)replace(edits,ranges.get(i)[0],ranges.get(i)[1],targets.get(i));
        return edits; // Translate visible labels only; never execute/rewrite click commands.
    }
    private List<Edit> planMaxwell70(String body){
        String base=withoutDisplayCounter(body);var m=MAXWELL_POWER70.matcher(base);boolean power=m.matches();
        if(!power){m=MAXWELL_CHOICE70.matcher(base);if(!m.matches())return List.of();}
        if(m.group(1).startsWith("[")&&!m.group(1).equals("["+m.group(2)+" head]"))return List.of();
        if(dictionary.hasCustomOverride(power?List.of("excellent","power"):List.of("wise","choice")))return List.of();
        var edits=new ArrayList<Edit>();
        if(power){literal(edits,body,", that's an ",", ");int excellent=base.indexOf("excellent",m.end(2)),term=base.indexOf("power",m.end(2));replace(edits,excellent,excellent+9,"отличная");replace(edits,term,term+5,"сила");}
        else{int start="[NPC] Maxwell: ".length();replace(edits,start,start+2,"");replace(edits,start+2,start+6,"Мудрый");replace(edits,start+7,start+13,"выбор");}
        return edits; // Never replaces a nickname, rank, opaque head, command or event.
    }
    private List<Edit> planCropLab88(String body){
        String base=withoutDisplayCounter(body);boolean beth=base.startsWith("[NPC] Beth: ");
        var m=(beth?BETH_HELP88:JAKE_TRUST88).matcher(base);if(!m.matches())return List.of();
        if(m.group(1).startsWith("[")&&!m.group(1).equals("["+m.group(2)+" head]"))return List.of();
        if(dictionary.hasCustomOverride(beth?List.of(base,"Jake","help you."):List.of(base,"Crop Analyzer","earned my trust")))return List.of();
        var edits=new ArrayList<Edit>();
        if(beth){
            literal(edits,body,"Jake, you've done incredible work here. But you don't have to do it alone anymore. Let me and ","Джейк, ты проделал здесь невероятную работу. Но теперь тебе не нужно делать всё одному. Позволь мне и ");
            replace(edits,m.end(2),base.length()," помочь тебе.");
        }else{
            literal(edits,body,"And you, ","А ты, ");
            replace(edits,m.end(2),base.indexOf("Crop Analyzer",m.end(2)),", очень помог нам дойти до этого момента. Теперь у тебя есть доступ к ");
            literal(edits,body,"Crop Analyzer","анализатору культур");
            literal(edits,body,". You've earned my trust.",". Ты заслужил моё доверие.");
        }
        return edits; // Variable name/head and all of its current events are untouched.
    }
    private List<Edit> planKatWait70(String body){
        String base=withoutDisplayCounter(body);var m=KAT_PICKUP70.matcher(base);boolean pickup=m.matches();
        if(!pickup){m=KAT_COMEBACK70.matcher(base);if(!m.matches())return List.of();}
        if(dictionary.hasCustomOverride(List.of("day","days","hour","hours","minute","minutes","second","seconds")))return List.of();
        var tokens=KAT_UNIT70.matcher(base);tokens.region(m.start(1),m.end(1));var edits=new ArrayList<Edit>();int previous=-1;
        while(tokens.find()){
            String unit=tokens.group(2);int rank=unit.startsWith("day")?0:unit.startsWith("hour")?1:unit.startsWith("minute")?2:3;
            int value=Integer.parseInt(tokens.group(1));if(rank<=previous||rank==1&&value>23||rank>=2&&value>59)return List.of();previous=rank;
            replace(edits,tokens.start(2),tokens.end(2),switch(rank){case 0->"дн";case 1->"ч";case 2->"мин";default->"с";});
        }
        if(pickup)literal(edits,body,"You can pick it up in ","Питомца можно забрать через ");
        else{literal(edits,body,"Come back in ","Возвращайся через ");literal(edits,body,"to pick it up!","чтобы забрать питомца!");}
        return edits;
    }
    private List<Edit> planJacob70(String body){
        String base=withoutDisplayCounter(body);var m=JACOB_RECORD70.matcher(base);int kind=0;
        if(!m.matches()){m=JACOB_BRACKET70.matcher(base);kind=1;if(!m.matches()){m=JACOB_PARTICIPATION70.matcher(base);kind=2;if(!m.matches())return List.of();}}
        int cropGroup=kind==2?1:2,contestGroup=cropGroup+1;String crop=m.group(cropGroup);
        // The existing Garden resource owns this fully translated literal and its styles.
        if(kind==2&&crop.equals("Carrot"))return List.of();
        if(dictionary.hasCustomOverride(kind==1?List.of(crop,"Contest",m.group(1)):List.of(crop,"Contest","PERSONAL BEST")))return List.of();
        String translated=dictionary.translateForSearch(crop);if(translated.equals(crop))return List.of();var edits=new ArrayList<Edit>();
        if(kind==0){literal(edits,body,"PERSONAL BEST","ЛИЧНЫЙ РЕКОРД");literal(edits,body,"You've surpassed your previous record of ","Ты побил прежний рекорд: ");literal(edits,body,"items collected in the ",noun(m.group(1),"предмет","предмета","предметов")+" на ");}
        else if(kind==1){literal(edits,body,"You placed in the ","Твоя категория — «");replace(edits,m.start(1),m.end(1),JACOB_RANKS70.get(m.group(1))+"»");literal(edits,body,"bracket in the ","на ");}
        else literal(edits,body,"You didn't earn a medal, but you may claim a participation reward in the ","Медаль не получена, но награда за участие доступна на ");
        // Reorder only semantic terms, using the current Contest and crop styles separately.
        edits.add(new Edit(m.start(cropGroup),m.end(contestGroup),List.of(
            new Part("соревновании по культуре «",m.start(contestGroup),m.end(contestGroup)),
            new Part(translated,m.start(cropGroup),m.end(cropGroup)),
            new Part("»",m.start(contestGroup),m.end(contestGroup)))));return edits;
    }
    private List<Edit> planPhillip69(String body){
        if(dictionary.hasCustomOverride(List.of("Pests","Farming Fortune","Vacuum Bag")))return List.of();
        var edits=new ArrayList<Edit>();var reward=PHILLIP_REWARD69.matcher(body);
        if(reward.matches()){
            literal(edits,body,"In exchange for ","В обмен на ");
            literal(edits,body,"Pests",noun(reward.group(1),"вредителя","вредителей","вредителей"));
            literal(edits,body,", I've given you ",", я дал тебе ");
            literal(edits,body," Farming Fortune"," фермерской удачи");
            // The preceding space may still have the coloured stat's Style.
            replace(edits,reward.end(2)+" Farming Fortune ".length(),reward.start(3)-1,"на");
            replace(edits,reward.start(4),reward.end(4)," мин");
            return edits; // All count/bonus/duration glyphs and the plus sign are copied.
        }
        var limit=PHILLIP_LIMIT69.matcher(body);
        if(limit.matches()){
            literal(edits,body,"You've exchanged enough Pests with me recently. You still have ","Ты недавно уже сдал достаточно вредителей. У тебя осталось ");
            int pests=body.indexOf("Pests",limit.end(1));
            replace(edits,pests,pests+5,noun(limit.group(1),"вредитель","вредителя","вредителей"));
            literal(edits,body,"in your","в");
            literal(edits,body,"Vacuum Bag","пылесборнике");
        }
        return edits;
    }
    private List<Edit> planKat(String body){
        var timer=KAT_PICKUP_TIME.matcher(body);
        if(timer.matches()){
            if(dictionary.hasCustomOverride(List.of("minutes","seconds")))return List.of();
            var edits=new ArrayList<Edit>();
            literal(edits,body,"You can pick it up in ","Питомца можно забрать через ");
            replace(edits,timer.end(1),timer.start(2)," мин ");
            replace(edits,timer.end(2),timer.end(2)+(body.startsWith(" seconds",timer.end(2))?8:7)," с");
            return edits; // Do not replace/recalculate the actual digit spans.
        }
        Matcher m=KAT_RESULT.matcher(body);boolean result=m.matches(),start=false;
        if(!result){m=KAT_START.matcher(body);start=m.matches();}
        if(!result&&!start){m=KAT_CARE.matcher(body);if(!m.matches())return List.of();}
        String pet=m.group(1);
        if(!KnownPetNames.contains(pet)||dictionary.hasCustomOverride(KnownPetNames.keys(pet)))return List.of();
        String name=KnownPetNames.translation(pet,dictionary::translateForSearch);if(name==null)return List.of();
        if((result||start)&&dictionary.hasCustomOverride(List.of(m.group(2))))return List.of();
        var edits=new ArrayList<Edit>();
        String prefix=result?"Ваш питомец «":start?"Я скоро улучшу питомца «":"Я сейчас занимаюсь питомцем «";
        replace(edits,body.indexOf(": ")+2,m.start(1),prefix);
        replace(edits,m.start(1),m.end(1),name);
        if(result||start){
            replace(edits,m.end(1),m.start(2),result?"» улучшен до редкости ":"» до редкости ");
            if(start)replace(edits,m.end(2),m.end(2)+" in no time!".length(),"!");
        }else replace(edits,m.end(1),m.end(1)+1,"»!");
        return edits;
    }
    private List<Edit> planItemLevel(String body){
        var m=ITEM_LEVEL.matcher(body);if(!m.matches())return List.of();
        var keys=new ArrayList<String>();keys.add(body);keys.add(withoutDisplayCounter(body));keys.add("LEVEL UP!");keys.add(m.group(1));
        var nameEdits=dictionary.itemTitleEdits(m.group(1));if(nameEdits.isEmpty())return List.of();
        for(var e:nameEdits)keys.add(m.group(1).substring(e.start(),e.end()));
        if(dictionary.hasCustomOverride(keys))return List.of();
        var edits=new ArrayList<Edit>();
        literal(edits,body,"LEVEL UP!","УРОВЕНЬ ПОВЫШЕН!");
        replace(edits,"LEVEL UP! ".length(),m.start(1),"Предмет «");
        for(var e:nameEdits)replace(edits,m.start(1)+e.start(),m.start(1)+e.end(),e.replacement());
        replace(edits,m.end(1),m.start(2),"» достиг уровня ");
        return edits; // Actual level, timestamp, suffix and their actions remain untouched.
    }

    /** Four observed collection notice shapes; padding and timestamps remain original. */
    public int collectionFamilyCount(){return 4;}
    public int documentNotificationFamilyCount(){return 5;}
    private List<Edit> planDocumentNotifications(String body){
        if(dictionary.hasCustomOverride(List.of(body)))return List.of();
        var edits=new ArrayList<Edit>();Matcher m;
        if((m=CROP_NOTICE.matcher(body)).matches()){
            if(dictionary.hasCustomOverride(List.of(m.group(1),withoutDisplayCounter(body),"RARE CROP!","VERY RARE CROP!","RARE","CROP!")))return List.of();
            String crop=dictionary.translateForSearch(m.group(1));
            if(body.startsWith("VERY "))literal(edits,body,"VERY","ОЧЕНЬ");
            literal(edits,body,"RARE","РЕДКИЙ");literal(edits,body,"CROP!","УРОЖАЙ!");
            if(!crop.equals(m.group(1)))replace(edits,m.start(1),m.end(1),crop);
        }else if((m=CULTIVATING_UP.matcher(body)).matches()){
            if(CULTIVATING_LEVELS.indexOf(m.group(5))!=CULTIVATING_LEVELS.indexOf(m.group(2))+1
                ||dictionary.hasCustomOverride(List.of("Cultivating",m.group(3))))return List.of();
            String name=dictionary.translateForSearch(m.group(3));if(name.equals(m.group(3)))return List.of();
            replace(edits,m.start(1),m.end(1),"Возделывание");
            replace(edits,m.end(2),m.start(3)," на предмете «");
            replace(edits,m.start(3),m.end(3),name);
            replace(edits,m.end(3),m.start(4),"» повышено до ");
            replace(edits,m.start(4),m.end(4),"Возделывание");
        }else if((m=MULTI_PEST.matcher(body)).matches()){
            replace(edits,m.start(1),m.end(1),"ФУ");
            replace(edits,m.start(4),m.end(4),noun(m.group(2),"вредитель","вредителя","вредителей"));
            replace(edits,m.end(4),m.start(5)," появились на участке № ");
        }else if(REQUIRED_ITEM.matcher(body).matches()){
            literal(edits,body,"You don't have enough of the required item!","Не хватает нужного предмета!");
        }else if((m=PLAY_EXP.matcher(body)).matches()){
            if(dictionary.hasCustomOverride(List.of("GEXP","Event EXP")))return List.of();
            replace(edits,0,m.start(1),"Вы получили ");
            replace(edits,m.start(2),m.end(2),"опыта гильдии");
            replace(edits,m.start(4),m.end(4),"опыта события");
            replace(edits,m.end(4),m.start(5)," за игру в ");
        }
        return edits;
    }
    private List<Edit> planCollections(String body){
        if(dictionary.hasCustomOverride(List.of(body)))return List.of();
        var result=new ArrayList<Edit>();int offset=0;
        for(String raw:body.split("\\n",-1)){
            int start=0,end=raw.length();var time=TIME.matcher(raw);if(time.find())start=time.end();
            while(start<end&&raw.charAt(start)==' ')start++;while(end>start&&raw.charAt(end-1)==' ')end--;
            String line=raw.substring(start,end);var edits=new ArrayList<Edit>();
            if(!dictionary.hasCustomOverride(InterfaceDisplay.keys(line))){
                var m=COLLECTION_UP.matcher(line);
                if(m.matches()){
                    String name=dictionary.translateForSearch(m.group(1));
                    if(!name.equals(m.group(1))&&!dictionary.hasCustomOverride(List.of(m.group(1),"COLLECTION LEVEL UP"))){
                        literal(edits,line,"COLLECTION LEVEL UP","УРОВЕНЬ КОЛЛЕКЦИИ ПОВЫШЕН");replace(edits,m.start(1),m.end(1),name);
                    }
                }else if(line.equals("REWARDS")||line.equals("REWARDS:")){
                    replace(edits,0,line.length(),line.endsWith(":")?"НАГРАДЫ:":"НАГРАДЫ");
                }else if((m=COLLECTION_XP.matcher(line)).matches()){
                    if(!dictionary.hasCustomOverride(List.of("SkyBlock XP")))replace(edits,m.end(1),line.length()," опыта SkyBlock");
                }else if(line.endsWith(" Recipe")){
                    for(var e:InterfaceDisplay.edits(line,dictionary::translateForSearch))replace(edits,e.start(),e.end(),e.replacement());
                }
            }
            int lineOffset=offset+start;
            for(var e:edits)result.add(new Edit(e.start+lineOffset,e.end+lineOffset,e.parts.stream().map(a->new Part(a.text,a.start+lineOffset,a.end+lineOffset)).toList()));
            offset+=raw.length()+1;
        }return result;
    }

    private List<Edit> planDungeon(String body){
        if(dictionary.hasCustomOverride(List.of(body)))return List.of();
        List<Edit> edits=new ArrayList<>();
        Matcher m=DUNGEON_COUNTDOWN.matcher(body);
        if(m.matches()){
            replace(edits,m.end(1),m.start(2)," запустил обратный отсчёт. Подземелье начнётся через ");
            replace(edits,m.end(2),body.length()," минуту.");
        }else if((m=DUNGEON_READY.matcher(body)).matches()){
            replace(edits,m.end(1),body.length()," готов!");
        }else if((m=DUNGEON_START.matcher(body)).matches()){
            replace(edits,0,m.start(1),"Подземелье начнётся через ");
            replace(edits,m.end(1),body.length()," "+noun(m.group(1),"секунду","секунды","секунд")+".");
        }else if((m=DUNGEON_PROGRESS.matcher(body)).matches()){
            boolean device=m.group(3).equals("device");
            if(!m.group(2).equals(device?"completed":"activated")
                ||Integer.parseInt(m.group(4))>Integer.parseInt(m.group(5)))return List.of();
            String action=device?" завершил работу с устройством":m.group(3).equals("lever")?" активировал рычаг":" активировал терминал";
            replace(edits,m.end(1),m.start(4),action+"! (");
        }
        return edits; // Original nickname and every count glyph remain outside edit spans.
    }


    private List<Edit> planDungeonCombat(String body){
        if(dictionary.hasCustomOverride(List.of(body)))return List.of();
        List<Edit> edits=new ArrayList<>();Matcher m=DUNGEON_COOLDOWN.matcher(body);
        if(m.matches()){
            replace(edits,0,m.start(1),"Способность перезаряжается. Осталось ");
            replace(edits,m.end(1),body.length()," с.");
        }else if((m=DUNGEON_MANA.matcher(body)).matches()){
            replace(edits,0,m.start(1),"Для этой способности нужно не менее ");
            replace(edits,m.end(1),body.length()," маны!");
        }else if((m=DUNGEON_HIT.matcher(body)).matches()){
            if(dictionary.hasCustomOverride(List.of(m.group(1))))return List.of();
            String ability=m.group(1).equals("Guided Sheep")?"Управляемая овца":"Имплозия";
            replace(edits,0,m.start(1),"Способность «");
            replace(edits,m.start(1),m.end(1),ability);
            replace(edits,m.end(1),m.start(2),"» задела ");
            replace(edits,m.end(2),m.start(3)," "+noun(m.group(2),"врага","врагов","врагов")+", нанеся ");
            replace(edits,m.end(3),body.length()," урона.");
        }else if((m=DUNGEON_HEALED.matcher(body)).matches()){
            replace(edits,0,m.start(1),"Вам восстановили ");
            replace(edits,m.end(1),m.start(2)," здоровья. Источник лечения: ");
        }else if((m=DUNGEON_HEALER.matcher(body)).matches()){
            replace(edits,m.end(1),m.start(2)," восстановил вам ");
            replace(edits,m.end(2),body.length()," здоровья!");
        }else if((m=DUNGEON_TETHER.matcher(body)).matches()){
            replace(edits,0,m.start(1),"Связь с ");
            replace(edits,m.end(1),m.start(2)," восстановила вам ");
            replace(edits,m.end(2),body.length()," здоровья.");
        }else if((m=DUNGEON_FAIRY_SELF.matcher(body)).matches()){
            replace(edits,0,m.start(1),"Ваша фея восстановила ");
            replace(edits,m.start(1),m.end(1),"вам");
            replace(edits,m.end(1),m.start(2)," ");
            replace(edits,m.end(2),body.length()," здоровья!");
        }else if((m=DUNGEON_FAIRY_PLAYER.matcher(body)).matches()){
            replace(edits,0,m.start(1),"Ваша фея восстановила игроку ");
            replace(edits,m.end(1),m.start(2)," ");
            replace(edits,m.end(2),body.length()," здоровья!");
        }else if(body.equals("BUFF! You have gained Healing V!")){
            if(dictionary.hasCustomOverride(List.of("Healing V")))return List.of();
            literal(edits,body,"BUFF!","УСИЛЕНИЕ!");
            literal(edits,body,"You have gained ","Вы получили эффект ");
            literal(edits,body,"Healing V","Исцеление V");
        }else if((m=DUNGEON_ABSORPTION.matcher(body)).matches()){
            replace(edits,0,m.start(1),"Вы получили ");
            replace(edits,m.end(1),m.start(2)," временного здоровья на ");
            replace(edits,m.end(2),m.start(3)," с от ");
        }else if((m=DUNGEON_STRENGTH.matcher(body)).matches()){
            replace(edits,m.end(1),m.start(2)," дал вам ");
            replace(edits,m.end(2),m.start(3)," силы на ");
            replace(edits,m.end(3),body.length()," секунд!");
        }else if((m=DUNGEON_BERSERK.matcher(body)).matches()){
            boolean percent=m.group(2).equals("Melee Damage");
            if(m.group(3).endsWith("%")!=percent||m.group(4).endsWith("%")!=percent
                ||dictionary.hasCustomOverride(List.of("Berserk",m.group(2))))return List.of();
            replace(edits,m.start(1),m.end(1),"Берсерк");
            replace(edits,m.start(2),m.end(2),percent?"Урон в ближнем бою":"Скорость передвижения");
        }
        return edits;
    }
    private List<Edit> plan(String body) {
        List<Edit> edits = new ArrayList<>();
        Matcher m = RARE.matcher(body);
        if ((m = FOREST_COLLECTION.matcher(body)).matches()) {
            if(dictionary.hasCustomOverride(List.of(body)))return List.of();
            String wood=dictionary.translateForSearch(m.group(1));
            if(wood.equals(m.group(1)))return List.of();
            literal(edits,body,"PERSONAL BEST","ЛИЧНЫЙ РЕКОРД");
            literal(edits,body,"You increased your ","За соревнование твоя коллекция «");
            replace(edits,m.start(1),m.end(1),wood);
            literal(edits,body," Collection by ","» выросла на ");
            literal(edits,body," during the contest! That's ","! Это на ");
            literal(edits,body," more than your previous best!"," больше твоего прежнего рекорда!");
        } else if((m=FOREST_SWEEP.matcher(body)).matches()) {
            if(dictionary.hasCustomOverride(List.of(body,"Sweep")))return List.of();
            if(!dictionary.translateForSearch("Sweep").equals("Размах"))return List.of();
            literal(edits,body,"Your total ","Ваш общий ");
            replace(edits,m.start(2),m.end(2),"Размах");
            literal(edits,body," is now increased by "," теперь увеличен на ");
        } else if ((m = COMMISSION.matcher(body)).matches()) {
            replace(edits, m.start(1), m.end(1), dictionary.translateForSearch(m.group(1)));
            literal(edits, body, " Commission Complete!", " — задание выполнено!");
            literal(edits, body, "Visit the ", "Поговорите с ");
            literal(edits, body, "King", "королём");
            literal(edits, body, " to claim your rewards!", ", чтобы забрать награды!");
        } else if (MAYHEM.containsKey(body)) {
            // These source-backed messages have no dynamic amount or player name.
            // Only a uniform complete clause is rewritten; unusual colours fail closed.
            replace(edits, 0, body.length(), MAYHEM.get(body));
        } else if (m.usePattern(RARE).matches()) {
            literal(edits, body, "RARE DROP!", "РЕДКАЯ ДОБЫЧА!");
            item(edits, m, 1);
            int stat = body.lastIndexOf("Magic Find");
            replace(edits, stat, stat + "Magic Find".length(), "Шанс редкой добычи");
        } else if ((m = RARE_COMPACT.matcher(body)).matches()) {
            literal(edits, body, "RARE DROP!", "РЕДКАЯ ДОБЫЧА!");
            item(edits, m, 1); // xN, amount and the stat glyph remain original.
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
        } else if ((m = BESTIARY_SINGLE.matcher(body)).matches()) {
            literal(edits, body, "BESTIARY", "БЕСТИАРИЙ");
            item(edits, m, 1);
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
            for (int group = 1; group <= 2; group++) {
                if (m.group(group) == null) continue;
                int start = m.end(group) + 1;
                int length = body.startsWith("items", start) ? 5 : 4;
                replace(edits, start, start + length, noun(m.group(group), "предмет", "предмета", "предметов"));
            }
            literal(edits, body, "Last ", "За ");
            replace(edits, m.end(3), m.end(3) + 1, " с");
        } else if ((m = STASH.matcher(body)).matches()) {
            literal(edits, body, "You have ", "В тайнике: ");
            literal(edits, body, "materials in stash, totalling ", "материалов, видов: ");
            literal(edits, body, " types", "");
            literal(edits, body, "Click to ", "Нажмите, чтобы ");
            literal(edits, body, "pickup", "забрать");
            literal(edits, body, "your stash!", "вещи!");
        } else if ((m = STASH_ITEMS.matcher(body)).matches()) {
            literal(edits, body, "You have ", "В тайнике: ");
            literal(edits, body, body.contains(" items ") ? "items in stash." : "item in stash.",
                noun(m.group(1), "предмет", "предмета", "предметов") + ".");
            literal(edits, body, "Click to ", "Нажмите, чтобы ");
            literal(edits, body, "pickup", "забрать");
            literal(edits, body, "your stash!", "вещи!");
        } else if (body.equals("You haven't claimed your Spooky Rewards yet!")) {
            literal(edits, body, "You haven't claimed your ", "Вы ещё не забрали ");
            literal(edits, body, "Spooky Rewards", "награды Жуткого фестиваля");
            literal(edits, body, " yet!", "!");
        } else if (body.equals("Talk to the Spooky Man in the Hub!")) {
            literal(edits, body, "Talk to the ", "Поговорите с ");
            literal(edits, body, "Spooky Man", "Жутким человеком");
            literal(edits, body, " in the ", " в ");
            literal(edits, body, "Hub", "Хабе");
        } else if ((m = TELEPORT.matcher(body)).matches()) {
            literal(edits, body, "Teleported you to ", "Перемещение: ");
            item(edits, m, 1);
        } else if ((m = PASTING.matcher(body)).matches()) {
            literal(edits, body, "Started pasting ", "Начато размещение шаблона «");
            item(edits, m, 1);
            literal(edits, body, " preset on Garden Plot - ", "» на участке Сада № ");
        } else if (body.equals("Remember, you have to be on the island for the resources to be planted!")) {
            replace(edits, 0, body.length(), "Для размещения ресурсов вы должны оставаться на острове!");
        } else if ((m = PET_READY.matcher(body)).matches()) {
            literal(edits, body, "Your ", "Ваш питомец «");
            item(edits, m, 1);
            literal(edits, body, " is Ready to pick up! ", "» готов к получению! ");
            literal(edits, body, "[call]", "[ЗАБРАТЬ]");
        } else if (body.equals("You need the Cookie Buff to use this feature!")) {
            literal(edits, body, "You need the ", "Для этой функции нужен ");
            literal(edits, body, "Cookie Buff", "эффект печенья");
            literal(edits, body, " to use this feature!", "!");
        } else if ((m = OBTAIN_COOKIE.matcher(body)).matches()) {
            literal(edits, body, "Obtain a ", "Получите ");
            replace(edits, m.start(1), m.end(1), "печенье-усилитель");
            literal(edits, body, " from the community shop in the ", " в общественном магазине в ");
            replace(edits, m.start(2), m.end(2), "Хабе");
        } else if ((m = DROPVISION.matcher(body)).matches()) {
            literal(edits, body, "loaded", "загружен");
            literal(edits, body, "Detection ", "Распознавание ");
            replace(edits, m.start(2), m.end(2), m.group(2).equals("OFF") ? "ВЫКЛ" : "ВКЛ");
            literal(edits, body, "rarities", "редкостей");
            literal(edits, body, "Bazaar + Auction pricing", "Цены Базара и аукциона");
        } else if (body.equals("Your Agronomy Sack is full of Seeds!")) {
            literal(edits, body, "Your ", "Ваш ");
            literal(edits, body, "Agronomy Sack", "мешок для урожая");
            literal(edits, body, " is full of ", " заполнен ");
            literal(edits, body, "Seeds", "семенами");
        } else if ((m = PEST.matcher(body)).matches()) {
            literal(edits, body, "GROSS!", "ФУ!");
            literal(edits, body, "A ", "");
            literal(edits, body, "Pest", "Вредитель");
            literal(edits, body, " has appeared in ", " появился на ");
            literal(edits, body, "Plot", "участке");
            literal(edits, body, " - ", " № ");
        } else if (PEST_FORTUNE.matcher(body).matches()) {
            literal(edits, body, "Farming Fortune", "Удача фермерства");
            literal(edits, body, " has been reduced by ", " снижена на ");
        } else if (body.equals("CLICK HERE to teleport to the plot!")) {
            literal(edits, body, "CLICK HERE", "НАЖМИТЕ ЗДЕСЬ");
            literal(edits, body, " to teleport to the plot!", ", чтобы переместиться на участок!");
        } else if (body.equals("Inventory full! Don't forget to check out your Storage inside the SkyBlock Menu!")
            || body.equals("Inventory full? Don't forget to check out your Storage inside the SkyBlock Menu!")) {
            literal(edits, body, "Inventory full", "Инвентарь заполнен");
            literal(edits, body, " Don't forget to check out your ", " Загляните в ");
            literal(edits, body, "Storage", "хранилище");
            literal(edits, body, " inside the ", " в ");
            literal(edits, body, "SkyBlock Menu", "меню SkyBlock");
        } else if (body.equals("You cannot transfer anything into sacks!")) {
            replace(edits, 0, body.length(), "Не удаётся переложить предметы в мешки!");
        } else if (body.equals("[SkyHanni] The Broodmother was killed!")) {
            literal(edits, body, "The Broodmother was killed!", "Матка пауков убита!");
        }
        return edits;
    }

    private void item(List<Edit> edits, Matcher m, int group) {
        String full = m.group(group);
        String plain = HEAD_DECORATION.matcher(full).replaceFirst("");
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
        // Legacy formatting often colours the separator after a highlighted word.
        // Boundary whitespace is not a semantic word; internal mixed styles still fail closed.
        int contentStart = start, contentEnd = end;
        while (contentStart < contentEnd && Character.isWhitespace(characterAt(atoms, contentStart))) contentStart++;
        while (contentEnd > contentStart && Character.isWhitespace(characterAt(atoms, contentEnd - 1))) contentEnd--;
        if (contentStart < contentEnd) { start = contentStart; end = contentEnd; }
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
    private static char characterAt(List<Atom> atoms, int position) {
        for (Atom atom : atoms) if (position >= atom.start && position < atom.end)
            return atom.text.charAt(position - atom.start);
        throw new IllegalArgumentException("Uncovered text");
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
