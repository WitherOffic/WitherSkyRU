import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import ru.wither.skyru.*;

/** Frozen corpus screening, not proof of complete translation or live server inventory. */
public class AuditEnchantmentCoverage {
    private static final Pattern PROSE=Pattern.compile("(?i)\\b(the|you|your|of|when|from|while|increases?|reduces?|grants?|saves?|gain|chance|damage|enemies|arrows|time)\\b");
    private static final Pattern NUMBER=Pattern.compile("[0-9][0-9,.]*(?:%|k)?");
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static Component styled(String text){
        var result=Component.empty();int at=0;var matcher=NUMBER.matcher(text);
        while(matcher.find()){
            result.append(Component.literal(text.substring(at,matcher.start())).setStyle(Style.EMPTY.withColor(0xaaaaaa)));
            result.append(Component.literal(matcher.group()).setStyle(Style.EMPTY.withColor(0x55ff55)));at=matcher.end();
        }
        return result.append(Component.literal(text.substring(at)).setStyle(Style.EMPTY.withColor(0xaaaaaa)));
    }
    public static void main(String[] args)throws Exception {
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var items=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("item-context.json"))).getAsJsonArray();
        var rows=new JsonArray();var bases=new TreeSet<String>();int cards=0,titleUnchanged=0,plainCandidates=0,styledCandidates=0;
        for(var raw:items){var item=raw.getAsJsonObject();String id=item.get("id").getAsString();
            if(!item.get("name").getAsString().equals("Enchanted Book")||!id.matches("[A-Z0-9_]+;[1-9][0-9]{0,2}"))continue;
            var lore=item.getAsJsonArray("lore");if(lore.size()<4)throw new AssertionError("Missing book context: "+id);
            String title=lore.get(2).getAsString();var body=new ArrayList<String>();
            for(int i=3;i<lore.size();i++){String line=lore.get(i).getAsString();if(line.isBlank())break;body.add(line);}
            if(body.isEmpty())throw new AssertionError("Missing effect paragraph: "+id);
            String source=String.join("\n",body),plain=db.translate(source);
            String actual=String.join("\n",TooltipTranslation.components(body.stream().map(AuditEnchantmentCoverage::styled).toList(),false).stream().map(Component::getString).toList());
            String translatedTitle=SkyRuClient.component(styled(title)).getString();boolean unchanged=title.equals(translatedTitle),plainFlag=PROSE.matcher(plain).find(),styledFlag=PROSE.matcher(actual).find();
            bases.add(id.substring(0,id.indexOf(';')));cards++;if(unchanged)titleUnchanged++;if(plainFlag)plainCandidates++;if(styledFlag)styledCandidates++;
            var row=new JsonObject();row.addProperty("id",id);row.addProperty("source_title",title);row.addProperty("translated_title",translatedTitle);row.addProperty("unchanged_title",unchanged);
            row.addProperty("effect_source",source);row.addProperty("plain_result",plain);row.addProperty("synthetic_numeric_style_result",actual);
            row.addProperty("plain_english_prose_candidate",plainFlag);row.addProperty("styled_english_prose_candidate",styledFlag);rows.add(row);
        }
        var report=new JsonObject();report.addProperty("scope","Frozen item-context books only; synthetic numeric styles. Candidates require review, not confirmed errors; not all live server enchantments.");
        report.addProperty("book_level_cards",cards);report.addProperty("distinct_base_ids",bases.size());report.addProperty("unchanged_titles",titleUnchanged);
        report.addProperty("plain_english_prose_candidates",plainCandidates);report.addProperty("styled_english_prose_candidates",styledCandidates);report.add("records",rows);
        Files.writeString(Path.of(args[2]),new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(report)+"\n");
        System.out.println("ENCHANTMENT SCREEN: cards="+cards+" bases="+bases.size()+" unchanged titles="+titleUnchanged+" plain prose candidates="+plainCandidates+" styled candidates="+styledCandidates+"; not confirmed error counts or all server coverage");
    }
}
