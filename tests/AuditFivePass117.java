import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Diagnostic screening,not an oracle for quality or an FPS measurement. */
public final class AuditFivePass117 {
    static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    static String plain(Component c){var b=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    static Component modern(String legacy){var c=Component.empty();var text=new StringBuilder();Style[] current={null};Runnable flush=()->{if(!text.isEmpty()){c.append(Component.literal(text.toString()).setStyle(current[0]));text.setLength(0);}};StringDecomposer.iterateFormatted(legacy,Style.EMPTY,(i,s,p)->{if(!s.equals(current[0])){flush.run();current[0]=s;}text.appendCodePoint(p);return true;});flush.run();return c;}
    static Component render(Component c,String kind){return kind.equals("chat")?SkyRuClient.npcChat(new GuiMessage(0,c,null,GuiMessageSource.SYSTEM_SERVER,null)).content():SkyRuClient.component(c);}
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]);Files.createDirectories(root);
        var db=new OfflineDictionary(root.resolve("isolated-config"));db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);flag("npcDialogues",new NpcDialogueTranslator());flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("winterDialogues",NpcDialogueTranslator.winterDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());flag("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());flag("chatNotifications",new ChatNotificationTranslator(db));
        var counts=new TreeMap<String,Integer>();int tested=0,layouts=0;
        try(var in=Files.newBufferedReader(Path.of(args[1]));var out=Files.newBufferedWriter(root.resolve("RUNTIME_ROWS.jsonl"))){String line;
            while((line=in.readLine())!=null){var row=JsonParser.parseString(line).getAsJsonObject();String source=row.get("source").getAsString(),kind=row.get("kind").getAsString();var result=new JsonObject();result.addProperty("source",source);result.addProperty("kind",kind);result.addProperty("dictionary_search_result",db.translateForSearch(source));String flat=plain(render(Component.literal(source),kind));result.addProperty("flat_result",flat);result.addProperty("changed_not_quality_proof",!flat.equals(source));var variants=new JsonArray();
                for(var raw:row.getAsJsonArray("layouts")){String legacy=raw.getAsString();if(!plain(Component.literal(legacy)).equals(source))throw new AssertionError("Input source/layout mismatch");String target=plain(render(Component.literal(legacy),kind)),structured=plain(render(modern(legacy),kind));var r=new JsonObject();r.addProperty("legacy",legacy);r.addProperty("result",target);r.addProperty("structured_component_result",structured);r.addProperty("same_flat_and_legacy_target",flat.equals(target));r.addProperty("same_flat_and_structured_target",flat.equals(structured));variants.add(r);layouts++;if(!flat.equals(target))counts.merge(kind+"_flat_legacy_disagree",1,Integer::sum);if(!flat.equals(structured))counts.merge(kind+"_flat_structured_disagree",1,Integer::sum);}
                result.add("layouts",variants);out.write(new Gson().toJson(result));out.newLine();tested++;counts.merge(kind+"_inputs",1,Integer::sum);counts.merge(kind+(flat.equals(source)?"_unchanged":"_changed_not_quality_proof"),1,Integer::sum);
            }
        }
        Path jar=Path.of(SkyRuClient.class.getProtectionDomain().getCodeSource().getLocation().toURI());var summary=new JsonObject();summary.addProperty("runtime_jar_sha256",HexFormat.of().withUpperCase().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))));summary.addProperty("inputs",tested);summary.addProperty("layouts",layouts);summary.add("counts",new Gson().toJsonTree(counts));summary.addProperty("scope","Actual offline dictionary and component/server-chat dispatch;changed text is not quality/completeness/FPS proof. No signed/player/client chat executed and no click action invoked.");Files.writeString(root.resolve("RUNTIME_SUMMARY.json"),new GsonBuilder().setPrettyPrinting().create().toJson(summary));System.out.println(summary);
    }
}
