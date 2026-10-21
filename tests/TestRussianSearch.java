import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import com.google.gson.JsonParser;

/** Search-local aliases: Russian partial queries, unchanged English names/IDs. */
public class TestRussianSearch {
    private static int checks;
    private static void check(String label, boolean ok) {
        if (!ok) throw new AssertionError(label);
        checks++;
    }
    private static boolean match(String name, String query) {
        String lower = name.toLowerCase(Locale.ROOT), input = query.toLowerCase(Locale.ROOT);
        return lower.contains(input) || Arrays.stream(input.split(" ")).allMatch(lower::contains);
    }
    private static void flag(String name, Object value) throws Exception {
        var f = SkyRuClient.class.getDeclaredField(name); f.setAccessible(true); f.set(null, value);
    }
    public static void main(String[] args) throws Exception {
        var db = new OfflineDictionary(Path.of(args[0]));
        var aliases = new RussianSearchAliases(db::translateForSearch);
        for (String name : List.of("Diamond Pickaxe", "Iron Pickaxe", "Golden Pickaxe", "Enchanted Mithril", "Epic Void Sword", "Enchanted Book")) {
            String translated = db.translateForSearch(name);
            String full = aliases.name(name, "кирка");
            check("English still present", full.startsWith(name));
            check("Russian full match " + name, match(full, translated));
            check("English query untouched", aliases.name(name, "pickaxe").equals(name));
            check("source unchanged", db.translateForSearch(name).equals(translated));
        }
        String picks = aliases.name("Diamond Pickaxe", "кирка");
        check("screenshot query", match(picks, "Кирка"));
        check("partial", match(picks, "кир"));
        check("case-insensitive", match(picks, "КИРКА"));
        check("reversed words", match(picks, "кирка алмазная"));
        check("unknown query", !match(picks, "ZX-9921"));
        check("empty query untouched", aliases.name("Diamond Pickaxe", "").equals("Diamond Pickaxe"));
        check("ID query untouched", aliases.name("Diamond Pickaxe", ":minecraft:diamond_pickaxe").equals("Diamond Pickaxe"));
        check("unknown name not guessed", aliases.name("Unknown ZX-9921", "кирка").equals("Unknown ZX-9921"));
        var misses = OfflineDictionary.class.getDeclaredField("misses"); misses.setAccessible(true);
        check("search does not log unseen catalogue", ((Set<?>)misses.get(db)).isEmpty());
        var normalCache = OfflineDictionary.class.getDeclaredField("cache"); normalCache.setAccessible(true);
        check("search does not evict display cache", ((Map<?,?>)normalCache.get(db)).isEmpty());
        db.setRecordUnknown(false);
        var calls = new AtomicInteger();
        var cached = new RussianSearchAliases(name -> { calls.incrementAndGet(); return "Шахтёрская кирка"; });
        check("yo", match(cached.name("Test Pickaxe", "шахтёр"), "шахтёр"));
        check("e instead of yo", match(cached.name("Test Pickaxe", "шахтер"), "шахтер"));
        check("cached per name not per keystroke", calls.get() == 1);
        for (int i = 0; i < 17000; i++) cached.name("Item " + i, "кирка");
        cached.name("Test Pickaxe", "кирка");
        check("bounded cache evicts oldest", calls.get() == 17002);
        check("version supported", CompatibilityMixinPlugin.supportedRrvVersion("8.10.9+26.1.2"));
        check("future version safely skipped", !CompatibilityMixinPlugin.supportedRrvVersion("8.11.0+26.1.2"));
        check("other MC skipped", !CompatibilityMixinPlugin.supportedRrvVersion("8.10.9+26.1.1"));
        check("SkyRecipes inspected version", CompatibilityMixinPlugin.supportedSkyRecipesVersion("0.5.13"));
        check("SkyRecipes unknown version skipped", !CompatibilityMixinPlugin.supportedSkyRecipesVersion("0.5.14"));
        String[] itemNames={"Fishing Rod","Rod of Legends","Diamond Pickaxe","Unknown ZX9921"};
        String[] savedNames=itemNames.clone();
        var indexed=new RussianItemNameIndex(itemNames,db::translateForSearch);
        check("fishing rods Cyrillic index",indexed.prefix("удочка").equals(BitSet.valueOf(new long[]{3})));
        check("fishing rods prefix",indexed.prefix("удо").cardinality()==2);
        check("fishing rods uppercase",indexed.prefix("УДОЧКА").cardinality()==2);
        check("translated multiword name",indexed.phrase(2,"алмазная кирка"));
        check("unrelated unknown does not match",indexed.prefix("несуществующий").isEmpty());
        check("short prefix follows native rule",indexed.prefix("у").isEmpty());
        check("index does not mutate names",Arrays.equals(itemNames,savedNames));
        var returned=indexed.prefix("удочка"); returned.clear();
        check("returned bitsets cannot corrupt cache",indexed.prefix("удочка").cardinality()==2);
        var indexedYo=new RussianItemNameIndex(new String[]{"Test Pickaxe"},name -> "Шахтёрская кирка");
        check("index ё",indexedYo.prefix("шахтёр").cardinality()==1);
        check("index е",indexedYo.prefix("шахтер").cardinality()==1);
        check("phrase е",indexedYo.phrase(0,"шахтерская кирка"));
        var catalogue=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("item-context.json"))).getAsJsonArray();
        String[] catalogueNames=new String[catalogue.size()];
        for(int i=0;i<catalogue.size();i++) catalogueNames[i]=catalogue.get(i).getAsJsonObject().get("name").getAsString();
        String[] catalogueBefore=catalogueNames.clone();
        long started=System.nanoTime();
        var catalogueIndex=new RussianItemNameIndex(catalogueNames,db::translateForSearch);
        long builtMillis=(System.nanoTime()-started)/1_000_000;
        for(String query:List.of("удочка","удо","кирка","алмазная","шахтер","несуществующийZX9921")) {
            BitSet expected=new BitSet();
            for(int i=0;i<catalogueNames.length;i++) {
                String translated=db.translateForSearch(catalogueNames[i]);
                if(translated.equals(catalogueNames[i])) continue;
                String[] words=translated.toLowerCase(Locale.ROOT).replace('ё','е').split("[^\\p{L}\\p{N}]+");
                if(Arrays.stream(words).anyMatch(word -> word.length()>=2 && word.startsWith(query.toLowerCase(Locale.ROOT)))) expected.set(i);
            }
            check("whole catalogue prefix " + query,catalogueIndex.prefix(query).equals(expected));
            if(query.equals("удочка")) check("real catalogue contains translated rods",expected.cardinality()>5);
        }
        check("catalogue names unchanged",Arrays.equals(catalogueNames,catalogueBefore));
        System.out.println("PASS: name index " + catalogueNames.length + " snapshot items; build " + builtMillis + " ms");
        flag("dictionary", db); flag("searchAliases", null); flag("enabled", true); flag("onlyHypixel", false);
        Style style = Style.EMPTY.withItalic(true).withClickEvent(new ClickEvent.RunCommand("/viewrecipe DIAMOND_PICKAXE"));
        Component original = Component.literal("Diamond Pickaxe").setStyle(style);
        Component search = SkyRuClient.searchName(original, "кирка");
        check("real bridge alias", match(search.getString(), "кирка"));
        check("original component untouched", original.getString().equals("Diamond Pickaxe") && original.getStyle().equals(style));
        check("English returns identical component", SkyRuClient.searchName(original, "pickaxe") == original);
        flag("enabled", false);
        check("disabled bridge returns original", SkyRuClient.searchName(original, "кирка") == original);
        System.out.println("PASS: Russian search " + checks + " assertions");
    }
}
