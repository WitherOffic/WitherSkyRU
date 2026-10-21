import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Independent before/after snapshots;changed text alone is not a quality/FPS score. */
public final class AuditNpc106 {
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String plain(Component c){var b=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static String output(String s){return plain(SkyRuClient.npcChat(new GuiMessage(0,Component.literal(s),null,GuiMessageSource.SYSTEM_SERVER,null)).content());}
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);Files.createDirectories(root);var sources=Path.of(args[1]);var db=new OfflineDictionary(root.resolve("config"));db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",false);flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("npcDialogues",new NpcDialogueTranslator());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("winterDialogues",NpcDialogueTranslator.winterDialogues());flag("chatNotifications",new ChatNotificationTranslator(db));
        var cases=JsonParser.parseString(Files.readString(sources.resolve("NPC_CASES_0.15.106.json"))).getAsJsonObject().getAsJsonArray("cases");var npc=new JsonArray();
        for(var raw:cases){var r=raw.getAsJsonObject();var n=new JsonObject();String source=r.get("source").getAsString();n.addProperty("source",source);n.addProperty("kind",r.get("kind").getAsString());n.addProperty("flat",output(source));n.addProperty("styled",output(r.get("legacy").getAsString()));npc.add(n);}
        flag("translateGameChat",true);var choices=new JsonArray();
        var groups=JsonParser.parseString(Files.readString(sources.resolve("NPC_CHOICES_SOURCES_0.15.106.json"))).getAsJsonObject().getAsJsonArray("records");
        for(var raw:groups){var group=raw.getAsJsonObject();var seen=new HashSet<String>();for(var item:group.getAsJsonArray("all_occurrences")){var r=item.getAsJsonObject();String legacy=r.get("source_legacy_line").getAsString().strip();if(!seen.add(legacy))continue;String source=r.get("source").getAsString();var n=new JsonObject();n.addProperty("source",source);n.addProperty("legacy",legacy);n.addProperty("flat",output(source));n.addProperty("styled",output(legacy));choices.add(n);}}
        var jar=Path.of(NpcDialogueTranslator.class.getProtectionDomain().getCodeSource().getLocation().toURI());var out=new JsonObject();out.addProperty("runtime_jar_sha256",HexFormat.of().withUpperCase().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))));out.add("npc",npc);out.add("choices",choices);out.addProperty("scope","103 historical literals and56 current groups,including206 NPC/58 choice layouts;not a game completeness or FPS claim");Files.writeString(root.resolve("NEW_ROWS.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out));System.out.println("NEW SNAPSHOTS: NPC="+npc.size()+";choices="+choices.size());
    }
}
