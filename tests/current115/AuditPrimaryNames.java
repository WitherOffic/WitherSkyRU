import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;

/** Read-only public API name replay. Unchanged names are candidates, not confirmed errors. */
public class AuditPrimaryNames {
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    public static void main(String[] args)throws Exception{
        Path project=Path.of(args[0]),primary=Path.of(args[1]),output=Path.of(args[2]);
        var input=JsonParser.parseString(Files.readString(primary)).getAsJsonObject();
        if(!input.get("success").getAsBoolean())throw new AssertionError("API unsuccessful");
        var db=new OfflineDictionary(output.getParent().resolve("isolated-primary-names"));db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var genders=new HashSet<String>();
        for(String file:List.of("item-genders.tsv","item-gender-overrides.tsv"))for(String line:Files.readAllLines(project.resolve("src/main/resources/data/witherskyru/"+file)))
            if(!line.isBlank()&&!line.startsWith("#"))genders.add(line.split("\t")[0]);
        var rows=new JsonArray();int unchanged=0,mixed=0,missingGender=0,idempotence=0;
        for(var raw:input.getAsJsonArray("items")){
            var item=raw.getAsJsonObject();if(!item.has("name"))continue;
            String source=item.get("name").getAsString(),plain=source.replaceAll("(?i)§[0-9a-fk-or]","");
            String translated=db.translateForSearch(source);
            String displayed=TooltipTranslation.components(List.of(Component.literal(source)),false).getFirst().getString();
            var row=new JsonObject();row.add("id",item.get("id"));row.addProperty("source",source);row.addProperty("plain",plain);
            row.addProperty("search",translated);row.addProperty("display",displayed);
            for(String field:List.of("category","tier"))row.add(field,item.has(field)?item.get(field):JsonNull.INSTANCE);
            boolean same=displayed.equals(source),partial=displayed.matches("(?s).*[А-Яа-яЁё].*")&&displayed.matches("(?s).*[A-Za-z].*");
            row.addProperty("unchanged_candidate",same);row.addProperty("mixed_candidate",partial);
            row.addProperty("has_gender",genders.contains(plain));
            if(same)unchanged++;if(partial)mixed++;
            if(item.has("category")&&item.get("category").getAsString().matches("SWORD|BOW|HELMET|CHESTPLATE|LEGGINGS|BOOTS|FISHING_ROD|FISHING_WEAPON|BELT|NECKLACE|CLOAK|GLOVES|BRACELET|AXE|HOE|PICKAXE|DRILL|WAND|GAUNTLET|SHOVEL|CHISEL|VACUUM|SHEARS|WATERING_CAN|FISHING_NET|LASSO")&&!genders.contains(plain))missingGender++;
            if(!db.translateForSearch(translated).equals(translated)){idempotence++;row.addProperty("repeat",db.translateForSearch(translated));}
            rows.add(row);
        }
        var result=new JsonObject();result.addProperty("source_url","https://api.hypixel.net/v2/resources/skyblock/items");result.add("lastUpdated",input.get("lastUpdated"));
        result.addProperty("records",rows.size());result.addProperty("unchanged_candidates",unchanged);result.addProperty("mixed_candidates",mixed);
        result.addProperty("equipment_missing_gender_candidates",missingGender);result.addProperty("idempotence_candidates",idempotence);
        result.addProperty("live_server_tested",false);result.add("rows",rows);
        Files.writeString(output,new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(result));
        System.out.println("PRIMARY NAME AUDIT: records="+rows.size()+" unchanged="+unchanged+" mixed="+mixed+" missing gender="+missingGender+" non-idempotent="+idempotence);
    }
}
