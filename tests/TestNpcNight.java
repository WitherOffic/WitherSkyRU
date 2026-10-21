import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Fifteen authored messages, independent frozen legacy samples, semantic styles. */
public class TestNpcNight {
    private static int checks;
    private static void eq(String got,String expected){if(!got.equals(expected))throw new AssertionError(got+"; expected "+expected);checks++;}
    private static List<Style> styles(Component source){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(source,Style.EMPTY,(i,s,cp)->{for(int n=0;n<Character.charCount(cp);n++)out.add(s);return true;});return out;}
    private static String visible(Component source){var b=new StringBuilder();StringDecomposer.iterateFormatted(source,Style.EMPTY,(i,s,cp)->{b.appendCodePoint(cp);return true;});return b.toString();}
    private static void styleAnchor(Component before,Component after,String from,String to){
        int at=visible(before).indexOf(from),target=visible(after).indexOf(to);if(at<0||target<0)throw new AssertionError("missing anchor");
        var oldStyles=styles(before);var newStyles=styles(after);Style expected=oldStyles.get(at);
        for(int i=at;i<at+from.length();i++)if(!oldStyles.get(i).equals(expected))throw new AssertionError("sample anchor is not uniform");
        for(int i=target;i<target+to.length();i++){if(!newStyles.get(i).equals(expected))throw new AssertionError("NPC colour/action changed for "+from);checks++;}
    }
    public static void main(String[] args)throws Exception{
        Path sourceDir=Path.of(args[1]);var npc=new NpcDialogueTranslator();
        var entries=JsonParser.parseString(Files.readString(sourceDir.resolve("NPC_DIALOGUES_RU.json"))).getAsJsonObject().getAsJsonArray("records");
        var samples=JsonParser.parseString(Files.readString(sourceDir.resolve("NPC_NIGHT_PASS9_STYLED_SAMPLES.json"))).getAsJsonArray();
        var selected=new HashMap<String,JsonObject>();
        for(var raw:entries){var row=raw.getAsJsonObject();if(row.get("source_key").getAsString().startsWith("night-pass9."))selected.put(row.get("source").getAsString(),row);}
        if(selected.size()!=15||npc.phraseCount()!=entries.size())throw new AssertionError("NPC entry count mismatch");checks++;
        Style body=Style.EMPTY.withColor(0xffffff).withClickEvent(new ClickEvent.RunCommand("/keep"))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("keep hover")));
        for(var row:selected.values()){
            String source=row.get("source").getAsString(),target=row.get("translation").getAsString(),prefix="[NPC] "+row.get("speaker").getAsString()+": ";
            if(!target.startsWith(prefix))throw new AssertionError("NPC speaker changed");checks++;
            var original=Component.literal(source).setStyle(body);var translated=npc.translate(original);
            for(int pass=0;pass<5;pass++){
                eq(translated.getString(),target);eq(original.getString(),source);for(var style:styles(translated)){if(!style.equals(body))throw new AssertionError("uniform NPC action lost");checks++;}
                translated=npc.translate(translated);
            }
            eq(npc.translate(Component.literal("[01:02:03] "+source)).getString(),"[01:02:03] "+target);
        }
        int observedSamples=0;
        for(var raw:samples){var sample=raw.getAsJsonObject();var row=selected.get(sample.get("source").getAsString());if(row==null)continue;observedSamples++;
            String legacy=sample.get("sample").getAsString(),time=legacy.replaceAll("(?i)§[0-9a-fk-or]","");var match=Pattern.compile("^\\[\\d{2}:\\d{2}:\\d{2}\\] ").matcher(time);
            String timestamp=match.find()?match.group():"",target=timestamp+row.get("translation").getAsString();
            var original=Component.literal(legacy).setStyle(body);var translated=npc.translate(original);
            if(!translated.getString().equals(target))System.out.println("STYLES OF UNSUPPORTED SAMPLE: "+styles(original).stream().distinct().toList());
            eq(translated.getString(),target);eq(original.getString(),legacy);
            for(int pass=0;pass<5;pass++){
                eq(npc.translate(translated).getString(),target);
                styleAnchor(original,translated,"[NPC] "+row.get("speaker").getAsString()+": ","[NPC] "+row.get("speaker").getAsString()+": ");
                if(row.has("style_anchors"))for(var anchor:row.getAsJsonArray("style_anchors"))styleAnchor(original,translated,anchor.getAsJsonObject().get("source").getAsString(),anchor.getAsJsonObject().get("target").getAsString());
            }
        }
        if(observedSamples<15)throw new AssertionError("missing original samples");checks++;
        var unsupported=Component.literal("[NPC] Grandma Wolf: I've already upgraded your pet to the maximum tier! Leave this little old Grandma alone! §8(§7x§r4§8)");
        if(npc.translate(unsupported)!=unsupported)throw new AssertionError("counter variant guessed");checks++;
        var unknown=Component.literal("[NPC] Grandma Wolf: Invented test dialogue ZX-909!");if(npc.translate(unknown)!=unknown)throw new AssertionError("unknown NPC guessed");checks++;
        var player=Component.literal("[VIP] Player: "+selected.keySet().iterator().next());if(npc.translate(player)!=player)throw new AssertionError("player prefix treated as NPC");checks++;
        String neutral="[NPC] Carpenter: Luckily for you, I made a copy!";int cut=neutral.indexOf(": ")+8;
        Style explicitFalse=body.withBold(false).withItalic(false).withUnderlined(false).withStrikethrough(false).withObfuscated(false);
        var reset=Component.literal(neutral.substring(0,cut)).setStyle(body).append(Component.literal(neutral.substring(cut)).setStyle(explicitFalse));
        eq(npc.translate(reset).getString(),selected.get(neutral).get("translation").getAsString());
        for(Style different:List.of(body.withBold(true),body.withColor(0xff5555),body.withClickEvent(new ClickEvent.RunCommand("/different")),body.withHoverEvent(new HoverEvent.ShowText(Component.literal("different"))))){
            var unsupportedBody=Component.literal(neutral.substring(0,cut)).setStyle(body).append(Component.literal(neutral.substring(cut)).setStyle(different));
            if(npc.translate(unsupportedBody)!=unsupportedBody)throw new AssertionError("different body style/action flattened");checks++;
        }
        System.out.println("PASS: night NPCs "+checks+" assertions; 15 exact entries / "+observedSamples+" frozen styled samples / "+npc.phraseCount()+" total / five repeats / names and semantic actions / unknown guards");
    }
}
