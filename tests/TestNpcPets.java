import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Complete observed pet/Garden messages, not inferred dialogue templates. */
public class TestNpcPets {
    private static int checks;
    private static void eq(String got,String expected){if(!got.equals(expected))throw new AssertionError(got+"; expected "+expected);checks++;}
    private static String visible(Component source){var b=new StringBuilder();StringDecomposer.iterateFormatted(source,Style.EMPTY,(i,s,cp)->{b.appendCodePoint(cp);return true;});return b.toString();}
    private static List<Style> styles(Component source){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(source,Style.EMPTY,(i,s,cp)->{for(int n=0;n<Character.charCount(cp);n++)out.add(s);return true;});return out;}
    private static void anchor(Component before,Component after,String from,String to){
        int at=visible(before).indexOf(from),target=visible(after).indexOf(to);if(at<0||target<0)throw new AssertionError("missing semantic anchor");
        var old=styles(before);var current=styles(after);Style expected=old.get(at);
        for(int i=at;i<at+from.length();i++)if(!old.get(i).equals(expected))throw new AssertionError("sample anchor not uniform");
        for(int i=target;i<target+to.length();i++){if(!current.get(i).equals(expected))throw new AssertionError("anchor style/action changed: "+from);checks++;}
    }
    public static void main(String[] args)throws Exception{
        var npc=new NpcDialogueTranslator();Path dir=Path.of(args[1]);
        var rows=JsonParser.parseString(Files.readString(dir.resolve("NPC_DIALOGUES_RU.json"))).getAsJsonObject().getAsJsonArray("records");
        var selected=new HashMap<String,JsonObject>();
        for(var raw:rows){var row=raw.getAsJsonObject();if(row.get("source_key").getAsString().startsWith("night-pass11."))selected.put(row.get("source").getAsString(),row);}
        if(selected.size()!=27||rows.size()!=npc.phraseCount())throw new AssertionError("exact record count");checks++;
        Style body=Style.EMPTY.withColor(0xffffff).withClickEvent(new ClickEvent.RunCommand("/body"))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("body hover"))).withInsertion("body insertion");
        for(var row:selected.values()){
            String source=row.get("source").getAsString(),target=row.get("translation").getAsString();
            var original=Component.literal(source).setStyle(body);var translated=npc.translate(original);
            for(int pass=0;pass<5;pass++){
                eq(translated.getString(),target);eq(original.getString(),source);
                for(var style:styles(translated)){if(!style.equals(body))throw new AssertionError("uniform action lost");checks++;}
                translated=npc.translate(translated);
            }
            eq(npc.translate(Component.literal("[01:02:03] "+source)).getString(),"[01:02:03] "+target);
            // Independently coloured/action-bearing semantic anchors, including plain log words.
            var rich=Component.empty();int cursor=0,order=0;var anchors=new ArrayList<JsonObject>();
            if(row.has("style_anchors"))for(var a:row.getAsJsonArray("style_anchors"))anchors.add(a.getAsJsonObject());
            anchors.sort(Comparator.comparingInt(a->source.indexOf(a.get("source").getAsString(),source.indexOf(": ")+2)));
            for(var a:anchors){String from=a.get("source").getAsString();int at=source.indexOf(from,source.indexOf(": ")+2);
                rich.append(Component.literal(source.substring(cursor,at)).setStyle(body));
                rich.append(Component.literal(from).setStyle(Style.EMPTY.withColor(0x110000+(++order)*0x1100)
                    .withClickEvent(new ClickEvent.RunCommand("/anchor-"+order)).withHoverEvent(new HoverEvent.ShowText(Component.literal("anchor hover "+order))).withInsertion("anchor insertion "+order)));
                cursor=at+from.length();
            }
            rich.append(Component.literal(source.substring(cursor)).setStyle(body));var result=npc.translate(rich);eq(result.getString(),target);eq(rich.getString(),source);
            for(var a:anchors)anchor(rich,result,a.get("source").getAsString(),a.get("target").getAsString());
        }
        var samples=JsonParser.parseString(Files.readString(dir.resolve("NPC_NIGHT_PASS11_STYLED_SAMPLES.json"))).getAsJsonArray();
        if(samples.size()!=27)throw new AssertionError("frozen sample count");checks++;
        for(var raw:samples){var sample=raw.getAsJsonObject();var row=selected.get(sample.get("source").getAsString());if(row==null)throw new AssertionError("unselected sample");
            String legacy=sample.get("sample").getAsString(),plain=legacy.replaceAll("(?i)§[0-9a-fk-or]","");
            String time=plain.matches("^\\[\\d{2}:\\d{2}:\\d{2}\\] .*")?plain.substring(0,11):"",target=time+row.get("translation").getAsString();
            var original=Component.literal(legacy).setStyle(body);var translated=npc.translate(original);
            eq(translated.getString(),target);eq(original.getString(),legacy);
            for(int pass=0;pass<5;pass++){
                eq(visible(translated),target);eq(npc.translate(translated).getString(),target);
                String prefix="[NPC] "+row.get("speaker").getAsString()+": ";anchor(original,translated,prefix,prefix);
                if(row.has("style_anchors"))for(var a:row.getAsJsonArray("style_anchors")){var semantic=a.getAsJsonObject();anchor(original,translated,semantic.get("source").getAsString(),semantic.get("target").getAsString());}
                eq(npc.translate(Component.literal(legacy)).getString(),target);
            }
        }
        for(String source:List.of("[NPC] Kat: Come back in 20 minutes to pick it up!","[NPC] Kat: I was able to upgrade your pet Rabbit to MYTHIC.","[NPC] Kat: I was able to upgrade your pet Wolf to RARE.","[NPC] Kat: New unobserved line ZX-901!")){
            var unknown=Component.literal(source);if(npc.translate(unknown)!=unknown)throw new AssertionError("unrecorded dialogue guessed");checks++;
        }
        String source="[NPC] Kat: I was able to upgrade your pet Rabbit to EPIC.";int at=source.indexOf("Rabbit");
        var mixed=Component.literal(source.substring(0,at)).setStyle(body).append(Component.literal("Rab").withColor(0x55ff55)).append(Component.literal("bit").withColor(0xff5555)).append(Component.literal(source.substring(at+6)).setStyle(body));
        if(npc.translate(mixed)!=mixed)throw new AssertionError("multi-style pet anchor flattened");checks++;
        var player=Component.literal("[VIP] Player: "+source);if(npc.translate(player)!=player)throw new AssertionError("player quote translated");checks++;
        System.out.println("PASS: NPC pets/Garden "+checks+" assertions; 27 exact entries / 27 frozen styled samples / "+npc.phraseCount()+" total / five repeats / semantic styles and actions / unknown guards");
    }
}
