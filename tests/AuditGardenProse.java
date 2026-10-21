import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import ru.wither.skyru.*;

/** Fixed public-context selection for human review, not a quality score. */
public class AuditGardenProse {
    private static List<String> tokens(String text,String regex) {return Pattern.compile(regex).matcher(text).results().map(m->m.group()).toList();}
    public static void main(String[] args) throws Exception {
        Path project=Path.of(args[0]), output=Path.of(args[1]);
        var db=new OfflineDictionary(output.getParent().resolve("isolated-garden-config")); db.setRecordUnknown(false);
        Set<String> ids=Set.of("COMPOST","DUNG","BIOFUEL","GARDEN_SCYTHE","BASIC_GARDENING_AXE","BASIC_GARDENING_HOE","ADVANCED_GARDENING_AXE","ADVANCED_GARDENING_HOE","SPRAYONATOR","PEST_TRAP","BASKET_OF_SEEDS","NETHER_WART_POUCH","ENCHANTED_COMPOST","CULTIVATING;1","ATTRIBUTE_SHARD_COMPOST_SPEED;1","ATTRIBUTE_SHARD_VISITOR_COMPOST;1","ATTRIBUTE_SHARD_VISITOR_DUNG;1","ATTRIBUTE_SHARD_VISITOR_CHEESE;1","ATTRIBUTE_SHARD_VISITOR_HONEY;1","ATTRIBUTE_SHARD_VISITOR_PLANT;1");
        if(args.length>2) {
            ids=new HashSet<>();
            for(var item:JsonParser.parseString(Files.readString(Path.of(args[2]))).getAsJsonArray())ids.add(item.getAsJsonObject().get("id").getAsString());
        }
        JsonArray rows=new JsonArray(), fragments=new JsonArray();
        int numberMismatches=0, rarityMismatches=0, idempotenceMismatches=0;
        var items=JsonParser.parseString(Files.readString(project.resolve("translation-source/item-context.json"))).getAsJsonArray();
        for(var raw:items) {
            var item=raw.getAsJsonObject(); if(!ids.contains(item.get("id").getAsString()))continue;
            var text=new ArrayList<String>(); for(var line:item.getAsJsonArray("lore"))text.add(line.getAsString().replaceAll("(?i)§[0-9a-fk-or]",""));
            String source=String.join("\n",text), target=db.translateForSearch(source);
            boolean numbers=tokens(source,"[0-9]+(?:[.,][0-9]+)*").equals(tokens(target,"[0-9]+(?:[.,][0-9]+)*"));
            boolean rarities=tokens(source,"\\b(?:COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL)\\b").equals(tokens(target,"\\b(?:COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL)\\b"));
            boolean stable=db.translateForSearch(target).equals(target);
            if(!numbers)numberMismatches++;if(!rarities)rarityMismatches++;if(!stable)idempotenceMismatches++;
            JsonObject row=new JsonObject(); row.add("id",item.get("id")); row.addProperty("source",source); row.addProperty("translation",target); rows.add(row);
            row.addProperty("numbers_preserved",numbers);row.addProperty("rarities_preserved",rarities);row.addProperty("idempotent",stable);
            System.out.println("ITEM "+item.get("id").getAsString()+"\nEN:\n"+source+"\nRU:\n"+target+"\n");
        }
        for(var line:Files.readAllLines(project.resolve("translation-source/corpus.jsonl"))) {
            var original=JsonParser.parseString(line).getAsJsonObject(); String source=original.get("source").getAsString();
            if(!source.toLowerCase(Locale.ROOT).contains("composter"))continue;
            JsonObject row=new JsonObject();row.addProperty("source",source);row.addProperty("translation",db.translateForSearch(source));fragments.add(row);
        }
        JsonObject result=new JsonObject();result.add("selected_item_blocks",rows);result.add("composter_corpus_fragments",fragments);
        result.addProperty("manual_review_completed",false);result.addProperty("live_server_complete",false);
        result.addProperty("numeric_token_mismatches",numberMismatches);result.addProperty("rarity_mismatches",rarityMismatches);result.addProperty("idempotence_mismatches",idempotenceMismatches);
        Files.writeString(output,new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(result)+"\n");
        System.out.println("SELECTED: "+rows.size()+" item blocks; "+fragments.size()+" composter fragments");
        if(numberMismatches+rarityMismatches+idempotenceMismatches!=0)throw new AssertionError("Selection invariants failed");
        System.out.println("PASS: selected item numbers, rarities and idempotence");
    }
}
