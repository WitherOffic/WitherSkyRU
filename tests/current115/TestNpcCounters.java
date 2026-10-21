import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Observed pass-10 messages, retained counter glyphs and semantic actions. */
public class TestNpcCounters {
    private static int checks;
    private static void eq(String got,String expected){if(!got.equals(expected))throw new AssertionError(got+"; expected "+expected);checks++;}
    private static String visible(Component source){var b=new StringBuilder();StringDecomposer.iterateFormatted(source,Style.EMPTY,(i,s,cp)->{b.appendCodePoint(cp);return true;});return b.toString();}
    private static List<Style> styles(Component source){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(source,Style.EMPTY,(i,s,cp)->{for(int n=0;n<Character.charCount(cp);n++)out.add(s);return true;});return out;}
    private static void retained(Component before,Component after,String from,String to,boolean uniform){
        int at=visible(before).indexOf(from),target=visible(after).indexOf(to);if(at<0||target<0)throw new AssertionError("missing semantic span");
        var old=styles(before);var current=styles(after);
        for(int i=0;i<to.length();i++){
            Style expected=old.get(at+(uniform?0:i));
            if(!current.get(target+i).equals(expected))throw new AssertionError("NPC style/action lost: "+from+" char "+i);
            checks++;
        }
    }
    public static void main(String[] args)throws Exception{
        var npc=new NpcDialogueTranslator();Path dir=Path.of(args[1]);
        var rows=JsonParser.parseString(Files.readString(dir.resolve("NPC_DIALOGUES_RU.json"))).getAsJsonObject().getAsJsonArray("records");
        var selected=new HashMap<String,JsonObject>();
        for(var raw:rows){var row=raw.getAsJsonObject();if(row.get("source_key").getAsString().startsWith("night-pass10."))selected.put(row.get("source").getAsString(),row);}
        if(selected.size()!=15||rows.size()!=npc.phraseCount())throw new AssertionError("exact record count");checks++;
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
        }
        var samples=JsonParser.parseString(Files.readString(dir.resolve("NPC_NIGHT_PASS10_STYLED_SAMPLES.json"))).getAsJsonArray();
        if(samples.size()!=15)throw new AssertionError("frozen sample count");checks++;
        for(var raw:samples){var sample=raw.getAsJsonObject();var row=selected.get(sample.get("source").getAsString());if(row==null)throw new AssertionError("unselected sample");
            String legacy=sample.get("sample").getAsString(),plain=legacy.replaceAll("(?i)§[0-9a-fk-or]","");
            String time=plain.matches("^\\[\\d{2}:\\d{2}:\\d{2}\\] .*")?plain.substring(0,11):"";
            String target=time+row.get("translation").getAsString();
            var original=Component.literal(legacy).setStyle(body);var translated=npc.translate(original);
            eq(translated.getString(),target);eq(original.getString(),legacy);
            for(int pass=0;pass<5;pass++){
                eq(visible(translated),target);eq(npc.translate(translated).getString(),target);
                String prefix="[NPC] "+row.get("speaker").getAsString()+": ";retained(original,translated,prefix,prefix,false);
                if(row.has("style_anchors"))for(var a:row.getAsJsonArray("style_anchors")){var anchor=a.getAsJsonObject();retained(original,translated,anchor.get("source").getAsString(),anchor.get("target").getAsString(),true);}
                if(row.has("preserve_suffix")){String suffix=row.get("preserve_suffix").getAsString();retained(original,translated,suffix,suffix,false);}
            }
        }
        String base="[NPC] Grandma Wolf: I've already upgraded your pet to the maximum tier! Leave this little old Grandma alone!";
        for(int value:List.of(2,3)){
            String suffix=" (x"+value+")",source=base+suffix,target=selected.get(source).get("translation").getAsString();
            var original=Component.literal(base).setStyle(body);
            for(int i=0;i<suffix.length();i++)original.append(Component.literal(suffix.substring(i,i+1)).setStyle(Style.EMPTY.withColor(0x110000+i*0x1100)
                .withClickEvent(new ClickEvent.RunCommand("/counter-"+i)).withHoverEvent(new HoverEvent.ShowText(Component.literal("counter hover "+i))).withInsertion("counter insertion "+i)));
            var translated=npc.translate(original);eq(translated.getString(),target);eq(original.getString(),source);
            retained(original,translated,suffix,suffix,false);
            for(int i=base.indexOf(": ")+2;i<target.length()-suffix.length();i++){if(!styles(translated).get(i).equals(body))throw new AssertionError("counter action leaked to prose");checks++;}
            int cut=base.indexOf(": ")+8;
            for(Style different:List.of(body.withColor(0xff5555),body.withBold(true),body.withClickEvent(new ClickEvent.RunCommand("/different")))){
                var unsafe=Component.literal(base.substring(0,cut)).setStyle(body).append(Component.literal(base.substring(cut)).setStyle(different)).append(Component.literal(suffix));
                if(npc.translate(unsafe)!=unsafe)throw new AssertionError("different body flattened");checks++;
            }
        }
        for(String suffix:List.of(" (x4)"," (x0)"," (x22)"," (x-2)"," (x2) extra")){
            var unknown=Component.literal(base+suffix);if(npc.translate(unknown)!=unknown)throw new AssertionError("unrecorded counter guessed");checks++;
        }
        var player=Component.literal("[VIP] Player: "+base+" (x2)");if(npc.translate(player)!=player)throw new AssertionError("player quote translated");checks++;
        System.out.println("PASS: NPC counters "+checks+" assertions; 15 exact entries / 15 frozen styled samples / "+npc.phraseCount()+" total / five repeats / each counter glyph and action / unknown guards");
    }
}
