import java.nio.file.*;
import com.google.gson.*;
import ru.wither.skyru.*;

/** Public frozen corpus only; classification input, never automatic translation edits. */
public class AuditUnchangedCorpus {
    public static void main(String[] args)throws Exception{
        Path project=Path.of(args[0]),output=Path.of(args[1]);
        var db=new OfflineDictionary(output.getParent().resolve("isolated-unchanged-config"));db.setRecordUnknown(false);
        var rows=new JsonArray();int total=0;
        for(String line:Files.readAllLines(project.resolve("translation-source/corpus.jsonl"))){
            var row=JsonParser.parseString(line).getAsJsonObject();String source=row.get("source").getAsString();total++;
            if(db.translateForSearch(source).equals(source))rows.add(row);
        }
        var result=new JsonObject();result.addProperty("sources",total);result.addProperty("unchanged",rows.size());result.add("rows",rows);
        result.addProperty("manual_proofread_complete",false);result.addProperty("all_unchanged_are_errors",false);
        Files.writeString(output,new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(result));
        System.out.println("UNCHANGED SCREEN: "+rows.size()+" / "+total+"; public corpus only, not a manual review");
    }
}
