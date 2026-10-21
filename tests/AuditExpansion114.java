import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Same candidate inputs on both artifacts;does not execute click actions. */
public final class AuditExpansion114 {
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String output(String text){var value=SkyRuClient.npcChat(new GuiMessage(0,Component.literal(text),null,GuiMessageSource.SYSTEM_SERVER,null)).content();var b=new StringBuilder();StringDecomposer.iterateFormatted(value,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);Files.createDirectories(root);var db=new OfflineDictionary(root.resolve("config"));db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);flag("chatNotifications",new ChatNotificationTranslator(db));flag("npcDialogues",new NpcDialogueTranslator());flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("winterDialogues",NpcDialogueTranslator.winterDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());flag("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());
        var inputs=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("EXPANSION_PROBE_0.15.114.json"))).getAsJsonObject().getAsJsonArray("records");var records=new JsonArray();
        for(var raw:inputs){var r=raw.getAsJsonObject();var out=new JsonObject();String source=r.get("source").getAsString();out.addProperty("source",source);out.addProperty("flat",output(source));var layouts=new JsonArray();for(var value:r.getAsJsonArray("layouts")){var row=new JsonObject();row.addProperty("legacy",value.getAsString());row.addProperty("output",output(value.getAsString()));layouts.add(row);}out.add("layouts",layouts);records.add(out);}
        var jar=Path.of(NpcDialogueTranslator.class.getProtectionDomain().getCodeSource().getLocation().toURI());var out=new JsonObject();out.addProperty("runtime_jar_sha256",HexFormat.of().withUpperCase().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))));out.add("records",records);Files.writeString(root.resolve("SNAPSHOTS.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out));System.out.println("EXPANSION114 snapshots="+records.size()+";not game FPS/quality proof");
    }
}
