import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Isolated chat-receipt work only. This is not FPS, GPU, font or frame timing. */
public final class BenchmarkNpc77 {
    private static volatile int sink;
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static void batch(List<Component> rows){for(int n=0;n<10;n++)sink^=SkyRuClient.npcChat(new GuiMessage(0,rows.get(n%rows.size()),null,GuiMessageSource.SYSTEM_SERVER,null)).content().getString().length();}
    private static double median(double[] values){var copy=values.clone();Arrays.sort(copy);return(copy[1]+copy[2])/2;}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[1]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);flag("npcDialogues",new NpcDialogueTranslator());flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("winterDialogues",NpcDialogueTranslator.winterDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());flag("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());flag("chatNotifications",new ChatNotificationTranslator(db));
        var inputs=new LinkedHashMap<String,List<String>>();
        inputs.put("unknown",List.of("[NPC] Unknown77: Future unpublished dialogue.","[NPC] Croesus: Unpublished future line77."));
        inputs.put("existing_dynamic",List.of("[NPC] Kat: You can pick it up in 29 minutes 55 seconds.","[NPC] Maxwell: A wise choice, Player_77!"));
        inputs.put("existing_exact",List.of("[NPC] Baker: I made one especially for you, here you go.","[NPC] Bingo: It's time to play SkyBlock Bingo!"));
        inputs.put("new_exact",List.of("[NPC] Croesus: I wonder if they'll ever open more doors in here.","[NPC] Sam: Our customer service is great!"));
        inputs.put("new_phone",List.of("[NPC] Malik: ✆ Here's my contact.","[NPC] Trinity: ✆ You want to know the secret of my potions?"));
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);if(!bean.isThreadCpuTimeEnabled())bean.setThreadCpuTimeEnabled(true);long thread=Thread.currentThread().threadId();var out=new JsonObject();
        for(var entry:inputs.entrySet()){
            var rows=entry.getValue().stream().map(s->(Component)Component.literal(s).setStyle(Style.EMPTY.withColor(0x123456).withInsertion("fresh").withClickEvent(new ClickEvent.SuggestCommand("/unchanged77")))).toList();for(int n=0;n<20000;n++)batch(rows);
            double[] wall=new double[4],cpu=new double[4],bytes=new double[4];double operations=40000;
            for(int r=0;r<4;r++){long allocation=bean.getThreadAllocatedBytes(thread),startCpu=bean.getCurrentThreadCpuTime(),start=System.nanoTime();for(int n=0;n<4000;n++)batch(rows);wall[r]=(System.nanoTime()-start)/operations;cpu[r]=(bean.getCurrentThreadCpuTime()-startCpu)/operations;bytes[r]=(bean.getThreadAllocatedBytes(thread)-allocation)/operations;}
            var result=new JsonObject();result.addProperty("ns_per_message",median(wall));result.addProperty("cpu_ns_per_message",median(cpu));result.addProperty("bytes_per_message",median(bytes));result.addProperty("equal_useful_output",entry.getKey().startsWith("existing")||entry.getKey().equals("unknown"));result.add("rounds_ns",new Gson().toJsonTree(wall));result.add("rounds_cpu_ns",new Gson().toJsonTree(cpu));result.add("rounds_bytes",new Gson().toJsonTree(bytes));out.add(entry.getKey(),result);
        }
        out.addProperty("scope","Chat receipt only; four 40000-call rounds after 200000 warmup calls/mode; setup outside timing; new positives do extra translation work, not equal output. Calling-thread CPU coarse on Windows. Not FPS or 0.1% lows.");Files.writeString(Path.of(args[0]),new GsonBuilder().setPrettyPrinting().create().toJson(out));
    }
}
