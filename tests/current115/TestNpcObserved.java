import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.StringDecomposer;
import ru.wither.skyru.*;

/** Replay only identified NPC messages; no game, network or player-chat export. */
public class TestNpcObserved {
    private static int checks;
    private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);checks++;}
    private static String visible(Component source){var b=new StringBuilder();StringDecomposer.iterateFormatted(source,Style.EMPTY,(i,s,cp)->{b.appendCodePoint(cp);return true;});return b.toString();}
    public static void main(String[] args)throws Exception{
        Path dir=Path.of(args[1]);var npc=new NpcDialogueTranslator();
        var root=JsonParser.parseString(Files.readString(dir.resolve("NPC_DIALOGUES_RU.json"))).getAsJsonObject();
        var targets=new HashMap<String,String>();for(var raw:root.getAsJsonArray("records")){var row=raw.getAsJsonObject();targets.put(row.get("source").getAsString(),row.get("translation").getAsString());}
        var samples=JsonParser.parseString(Files.readString(dir.resolve("NPC_NIGHT_PASS12_ALL_STYLES.json"))).getAsJsonArray();
        var sources=new HashSet<String>();int failed=0,passed=0;
        for(var raw:samples){var row=raw.getAsJsonObject();String source=row.get("source").getAsString(),sample=row.get("sample").getAsString(),target=targets.get(source);
            check(source.startsWith("[NPC] "),"not NPC");check(sample.replaceAll("(?i)§[0-9a-fk-or]","").equals(source),"snapshot mismatch");sources.add(source);
            check(target!=null,"no exact target for "+source);
            var original=Component.literal(sample);var translated=npc.translate(original);
            if(!visible(translated).equals(target)){failed++;System.out.println("FAILED NPC: "+source+"\nLEGACY: "+sample+"\nGOT: "+visible(translated));continue;}passed++;
            check(original.getString().equals(sample),"original mutated");
            for(int repeat=0;repeat<5;repeat++){
                check(visible(npc.translate(translated)).equals(target),"idempotence");
                for(String timestamp:List.of("[00:00:00] ","[23:59:59] ")){
                    var timed=Component.literal(timestamp+sample);check(visible(npc.translate(timed)).equals(timestamp+target),"timestamp replay");check(timed.getString().equals(timestamp+sample),"timestamp original changed");
                }
            }
            Style action=Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/unchanged"))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("unchanged"))).withInsertion("unchanged");
            var interactive=Component.literal(sample).setStyle(action);var result=npc.translate(interactive);check(visible(result).equals(target),"action-bearing replay");
            StringDecomposer.iterateFormatted(result,Style.EMPTY,(i,s,cp)->{check(Objects.equals(s.getClickEvent(),action.getClickEvent()),"click lost");check(Objects.equals(s.getHoverEvent(),action.getHoverEvent()),"hover lost");check(Objects.equals(s.getInsertion(),action.getInsertion()),"insertion lost");return true;});
            check(interactive.getString().equals(sample),"action original changed");
        }
        check(samples.size()==76&&sources.size()==76,"fixed snapshot size");
        System.out.println("NPC SNAPSHOT: "+passed+" rendered / "+failed+" unsupported / "+samples.size()+" distinct legacy styles / "+sources.size()+" sources; "+checks+" checks; offline system-message assumption, not live channel verification");
        if(failed>0&&(args.length<3||!args[2].equals("audit-only")))throw new AssertionError("unsupported observed NPC styles");
    }
}
