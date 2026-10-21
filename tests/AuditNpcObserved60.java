import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Replay filtered user observations; unknown channels/styles are not a live-server census. */
public final class AuditNpcObserved60{
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String plain(Component c){return AuditMiningStyles.plain(c.getVisualOrderText());}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);
        flag("npcDialogues",new NpcDialogueTranslator());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("winterDialogues",NpcDialogueTranslator.winterDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());flag("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());flag("chatNotifications",new ChatNotificationTranslator(db));
        var capture=JsonParser.parseString(Files.readString(Path.of(args[1]))).getAsJsonObject();var records=new JsonArray();int unchanged=0,changed=0,guards=0;var speakers=new TreeMap<String,Integer>();
        for(var raw:capture.getAsJsonArray("records")){
            var source=raw.getAsJsonObject();var input=Component.literal(source.get("sample").getAsString());var result=SkyRuClient.npcChat(new GuiMessage(0,input,null,GuiMessageSource.SYSTEM_SERVER,null)).content();String before=plain(input),after=plain(result);boolean same=before.equals(after);if(same){unchanged++;speakers.merge(source.get("speaker").getAsString(),1,Integer::sum);}else changed++;
            for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var msg=new GuiMessage(0,input,null,channel,null);if(SkyRuClient.npcChat(msg)!=msg)throw new AssertionError("ordinary chat changed");guards++;}
            var signed=new GuiMessage(0,input,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);if(SkyRuClient.npcChat(signed)!=signed)throw new AssertionError("signed changed");guards++;
            var row=source.deepCopy();row.addProperty("unchanged_plain",same);row.addProperty("current_result",after);row.addProperty("literal_backslash_escape_present",before.contains("\\n")||before.contains("\\r"));records.add(row);
        }
        var report=new JsonObject();report.addProperty("scope","Filtered local NPC display lines under synthetic unsigned SYSTEM_SERVER assumption; changed may be partial, not live channel/full branches/confirmed error count. PLAYER/client/signed verified unchanged.");report.addProperty("unchanged",unchanged);report.addProperty("changed_not_proof_of_complete_translation",changed);report.addProperty("chat_guard_assertions",guards);report.add("records",records);Files.writeString(Path.of(args[2]),new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(report)+"\n");
        System.out.println("NPC60 SCREEN: unchanged="+unchanged+" changed/possibly partial="+changed+" chatguards="+guards);speakers.forEach((s,n)->System.out.println(s+":"+n));
    }
}
