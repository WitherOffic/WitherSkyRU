import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import ru.wither.skyru.*;

/** Current public repository snapshot; candidates are not confirmed errors or live coverage. */
public final class AuditCurrentEnchantments {
    private static final Pattern PROSE=Pattern.compile("(?i)\\b(the|you|your|of|when|from|while|increases?|reduces?|grants?|saves?|gain|chance|damage|enemies|arrows|time)\\b");
    private static String plain(String s){return s.replaceAll("(?i)§[0-9a-fk-or]","");}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var corpus=JsonParser.parseString(Files.readString(Path.of(args[1]))).getAsJsonObject();var records=corpus.getAsJsonArray("records");
        int unchangedTitles=0,candidates=0;var out=new JsonArray();
        for(var raw:records){var r=raw.getAsJsonObject();var original=new ArrayList<Component>();
            for(var line:r.getAsJsonArray("effect_legacy"))original.add(Component.literal(line.getAsString()));
            var result=TooltipTranslation.components(original,false);String text=String.join("\n",result.stream().map(c->plain(c.getString())).toList());
            String title=plain(SkyRuClient.component(Component.literal(r.get("title_legacy").getAsString())).getString());
            boolean unchanged=title.equals(r.get("title_plain").getAsString()),candidate=PROSE.matcher(text).find();if(unchanged)unchangedTitles++;if(candidate)candidates++;
            var item=new JsonObject();item.addProperty("id",r.get("id").getAsString());item.addProperty("source_title",r.get("title_plain").getAsString());item.addProperty("translated_title",title);
            item.addProperty("actual_legacy_result",text);item.addProperty("english_prose_candidate",candidate);item.addProperty("unchanged_title",unchanged);out.add(item);
        }
        var report=new JsonObject();report.addProperty("scope","Current767 local repository books with actual legacy styles; automated candidates, not confirmed errors/full manual audit/all live server");
        report.addProperty("cards",records.size());report.addProperty("unchanged_titles",unchangedTitles);report.addProperty("english_prose_candidates",candidates);report.add("records",out);
        Files.writeString(Path.of(args[2]),new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(report)+"\n");
        System.out.println("CURRENT ENCHANTMENT SCREEN: cards="+records.size()+" unchangedTitles="+unchangedTitles+" actualLegacyProseCandidates="+candidates+"; not confirmed errors/all server");
    }
}
