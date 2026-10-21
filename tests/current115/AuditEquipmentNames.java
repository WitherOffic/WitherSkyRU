import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.OfflineDictionary;

/** Read-only audit of cached equipment names; no Minecraft or server access. */
public class AuditEquipmentNames {
    private static final String TYPES="SWORD|LONGSWORD|BOW|HELMET|CHESTPLATE|LEGGINGS|BOOTS|AXE|HOE|PICKAXE|DRILL|FISHING ROD|FISHING WEAPON|WAND|FARMING TOOL|GAUNTLET|SHOVEL|CHISEL|VACUUM|SHEARS|WATERING CAN|FISHING NET|LASSO|CARNIVAL MASK|HATCESSORY|BELT|NECKLACE|CLOAK|GLOVES|BRACELET";
    private static final java.util.regex.Pattern TYPE=java.util.regex.Pattern.compile("(?:COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL|VERY SPECIAL|SUPREME|ULTIMATE) (?:DUNGEON )?("+TYPES+")");
    public static void main(String[] args) throws Exception {
        Path project=args.length > 1 ? Path.of(args[1]).toAbsolutePath().getParent() : Path.of(args[0]).toAbsolutePath();
        var db=new OfflineDictionary(project.resolve("build/equipment-audit-config"));
        db.setRecordUnknown(false);
        Map<String,String> genders=new HashMap<>();
        for(String file:List.of("item-genders.tsv","item-gender-overrides.tsv"))
            for(String row:Files.readAllLines(project.resolve("src/main/resources/data/witherskyru/"+file)))
                if(!row.isBlank() && !row.startsWith("#")) { var p=row.split("\t"); genders.put(p[0],p[1]); }
        Map<String,String[]> modifiers=new TreeMap<>();
        for(String row:Files.readAllLines(project.resolve("src/main/resources/data/witherskyru/reforges.tsv")))
            if(!row.isBlank() && !row.startsWith("#")) {var p=row.split("\t"); modifiers.put(p[0],Arrays.copyOfRange(p,1,5));}
        Set<String> suffixes=new HashSet<>();
        for(String row:Files.readAllLines(project.resolve("src/main/resources/data/witherskyru/reforge-placement.tsv")))
            if(!row.isBlank() && !row.startsWith("#")) suffixes.add(row.split("\t")[0]);
        String[][] decorations={{"",""},{""," ✪✪✪✪✪"},{""," ➊➋➌➍➎"},{""," [10]"},{"⚚ "," ✪✪✪"},{"§9§l"," ✪✪✪§r"},
            {"✦ "," ★★★★★"},{"⚚ "," ➊➋➌➍➎"},{""," ✪✪✪✪✪ [10]"},{""," ⓵⓶⓷⓸⓹"},{"[10] "," ✪✪✪"},{"(✦) "," [10]"}};
        int count=0, failures=0;
        Set<String> names=new TreeSet<>();
        Map<String,Set<String>> categories=new TreeMap<>();
        var authored=JsonParser.parseString(Files.readString(project.resolve("translation-source/translations.json")))
            .getAsJsonObject().getAsJsonObject("translations");
        int exactTitleCases=0;
        var items=JsonParser.parseString(Files.readString(project.resolve("translation-source/item-context.json"))).getAsJsonArray();
        for(var entry:items) {
            var item=entry.getAsJsonObject();
            boolean equipment=false;
            for(var lore:item.getAsJsonArray("lore")) {
                var match=TYPE.matcher(lore.getAsString());
                if(match.matches()) {
                    equipment=true;
                    categories.computeIfAbsent(match.group(1),key->new TreeSet<>()).add(item.get("name").getAsString());
                }
            }
            if(equipment) names.add(item.get("name").getAsString());
        }
        for(String name:names) {
            String base=db.translateForSearch(name);
            if(base.equals(name)) { failures++; System.out.println("MISSING BASE: "+name); }
            if(!genders.containsKey(name)) { failures++; System.out.println("MISSING GENDER: "+name); continue; }
            int form=switch(genders.getOrDefault(name,"m")) { case "f"->1; case "n"->2; case "p"->3; default->0; };
            for(var modifier:modifiers.entrySet()) for(String[] decoration:decorations) {
                String expected=suffixes.contains(modifier.getKey())?base+" "+modifier.getValue()[form]:modifier.getValue()[form]+" "+base;
                String fullTitle=modifier.getKey()+" "+name;
                // Giant Cleaver is also a canonical item, not necessarily Giant + Cleaver.
                // Exact full-title translations have priority over mechanical composition.
                if(authored.has(fullTitle)) {expected=authored.get(fullTitle).getAsString();exactTitleCases++;}
                count++;
                String input=decoration[0]+modifier.getKey()+" "+name+decoration[1];
                String actual=db.translateForSearch(input);
                String target=decoration[0]+expected+decoration[1];
                if(!actual.equals(target)) {
                    failures++;
                    if(failures<30) System.out.println("FAIL: "+input+" => "+actual+" ; expected "+target);
                }
            }
        }
        Map<String,Integer> categoryCounts=new TreeMap<>(); categories.forEach((kind,entries)->categoryCounts.put(kind,entries.size()));
        var result=new JsonObject();
        result.addProperty("version", args.length>2?args[2]:"audit");
        result.addProperty("unique_names",names.size()); result.addProperty("modifiers",modifiers.size());
        result.addProperty("decoration_variants",decorations.length); result.addProperty("composition_cases",count);
        result.addProperty("findings",failures); result.add("categories",new Gson().toJsonTree(categoryCounts));
        result.addProperty("exact_full_title_cases",exactTitleCases);
        result.addProperty("live_server_tested",false); result.addProperty("all_combinations_game_legal",false);
        Path report=project.resolve("build/equipment-audit-"+(args.length>2?args[2]:"audit")+".json");
        Files.writeString(report,new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n");
        System.out.println("AUDIT: "+result);
        if(failures != 0) throw new AssertionError("Equipment title findings: "+failures);
        System.out.println("PASS: equipment audit "+names.size()+" unique equipment names, "+count+" reforge cases, "+failures+" findings");
    }
}
