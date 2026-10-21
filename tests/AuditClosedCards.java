import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import ru.wither.skyru.*;

/** Display selected complete public cards through the current tooltip path. */
public class AuditClosedCards {
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    public static void main(String[] args)throws Exception{
        Path project=Path.of(args[0]),output=Path.of(args[1]);var db=new OfflineDictionary(output.getParent().resolve("isolated-cards-config"));db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var ids=Set.of(args[2].split(","));var results=new JsonArray();
        for(var raw:JsonParser.parseString(Files.readString(project.resolve("translation-source/item-context.json"))).getAsJsonArray()){
            var item=raw.getAsJsonObject();if(!ids.contains(item.get("id").getAsString()))continue;
            var input=new ArrayList<Component>();input.add(Component.literal(item.get("name").getAsString()));for(var line:item.getAsJsonArray("lore"))input.add(Component.literal(line.getAsString()));
            var result=TooltipTranslation.components(input,false);var row=new JsonObject();row.add("id",item.get("id"));row.add("origin",item.get("origin"));row.add("source",new Gson().toJsonTree(input.stream().map(Component::getString).toList()));row.add("translation",new Gson().toJsonTree(result.stream().map(Component::getString).toList()));results.add(row);
        }
        Files.writeString(output,new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(results));System.out.println("COMPLETE CARDS: "+results.size());
    }
}
