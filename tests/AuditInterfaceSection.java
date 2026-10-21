import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;

/** Selected public frozen strings only; regex repository markers are identified, not live UI evidence. */
public class AuditInterfaceSection {
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    public static void main(String[] args)throws Exception{
        Path project=Path.of(args[0]),out=Path.of(args[1]);var db=new OfflineDictionary(out.getParent().resolve("isolated-interface"));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var filter=Pattern.compile(args[2]);var rows=new JsonArray();
        for(String line:Files.readAllLines(project.resolve("translation-source/corpus.jsonl"))){var raw=JsonParser.parseString(line).getAsJsonObject();String s=raw.get("source").getAsString();if(!filter.matcher(s).find())continue;
            var row=new JsonObject();row.addProperty("source",s);row.addProperty("plain",db.translate(s));row.addProperty("display",SkyRuClient.component(Component.literal(s)).getString());row.addProperty("repository_regex_marker",s.contains("\\")||s.contains("(?:"));rows.add(row);
        }
        Files.writeString(out,new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(rows));System.out.println("INTERFACE SELECTION: "+rows.size()+"; frozen corpus, not live GUI coverage");
    }
}
