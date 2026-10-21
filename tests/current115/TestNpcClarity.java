import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.StringDecomposer;

/** All authored records replayed; nine existing directions additionally tested with actual colours. */
public class TestNpcClarity {
    private static int checks;
    private static void check(boolean v,String label){if(!v)throw new AssertionError(label);checks++;}
    private static String visible(Component c){var b=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    public static void main(String[] args)throws Exception{
        var npc=new NpcDialogueTranslator();Path dir=Path.of(args[1]);
        var rows=JsonParser.parseString(Files.readString(dir.resolve("NPC_DIALOGUES_RU.json"))).getAsJsonObject().getAsJsonArray("records");
        Map<String,JsonObject> targets=new HashMap<>();
        Style action=Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/unchanged"))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("original hover"))).withInsertion("original");
        for(var raw:rows){var row=raw.getAsJsonObject();String source=row.get("source").getAsString(),target=row.get("translation").getAsString();
            check(targets.put(source,row)==null,"unique source");
            check(target.startsWith("[NPC] "+row.get("speaker").getAsString()+": "),"speaker identity");
            var original=Component.literal(source).setStyle(action);
            for(int repeat=0;repeat<5;repeat++){
                var translated=npc.translate(original);check(visible(translated).equals(target),"authored record replay: "+source);
                check(visible(npc.translate(translated)).equals(target),"idempotence");check(original.getString().equals(source),"immutable source");
                StringDecomposer.iterateFormatted(translated,Style.EMPTY,(i,s,p)->{check(Objects.equals(s.getClickEvent(),action.getClickEvent()),"click retained");check(Objects.equals(s.getHoverEvent(),action.getHoverEvent()),"hover retained");check(Objects.equals(s.getInsertion(),action.getInsertion()),"insertion retained");return true;});
            }
        }
        var samples=JsonParser.parseString(Files.readString(dir.resolve("NPC_CONTINUOUS_PASS5_RELAY_SAMPLES.json"))).getAsJsonArray();
        for(var raw:samples){var sample=raw.getAsJsonObject();String source=sample.get("source").getAsString(),encoded=sample.get("sample").getAsString();
            check(encoded.replaceAll("(?i)§[0-9a-fk-or]","").equals(source),"frozen sample original");
            var row=targets.get(source);String target=row.get("translation").getAsString();
            for(int repeat=0;repeat<5;repeat++){
                var original=Component.literal(encoded).setStyle(action);Component shown=npc.translate(original);
                check(visible(shown).equals(target),"coloured direction "+source);check(original.getString().equals(encoded),"legacy source immutable");
                check(Objects.equals(at(original,"✆"),at(shown,"✆")),"phone prefix style/action retained");
                for(var a:row.has("style_anchors")?row.getAsJsonArray("style_anchors"):new JsonArray()){
                    var anchor=a.getAsJsonObject();Style from=at(original,anchor.get("source").getAsString()),to=at(shown,anchor.get("target").getAsString());
                    check(Objects.equals(from,to),"phone/location style/action retained");
                }
            }
        }
        check(npc.phraseCount()==rows.size(),"runtime record count");
        check(npc.translate(Component.literal("[NPC] Unknown ZX-99: Mine some mystery ore!")).getString().equals("[NPC] Unknown ZX-99: Mine some mystery ore!"),"unknown not guessed");
        System.out.println("PASS: NPC clarity "+checks+" assertions; "+rows.size()+" authored records replayed, "+samples.size()+" newly tested legacy variants of existing directions; not all server NPCs");
    }
    private static Style at(Component c,String word){int start=visible(c).indexOf(word);int[] pos={0};Style[] out={null};StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{if(pos[0]==start)out[0]=s;pos[0]+=Character.charCount(p);return true;});return out[0];}
}
