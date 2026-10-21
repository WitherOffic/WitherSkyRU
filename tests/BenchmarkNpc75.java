import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Chat insertion only. New translated positives do more work than the old unchanged source. */
public final class BenchmarkNpc75 {
    private static volatile int sink;
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    private static void batch(List<Component> rows){for(int i=0;i<10;i++)sink^=SkyRuClient.npcChat(new GuiMessage(0,rows.get(i%rows.size()),null,GuiMessageSource.SYSTEM_SERVER,null)).content().getString().length();}
    private static double median(double[] values){var copy=values.clone();Arrays.sort(copy);return(copy[1]+copy[2])/2;}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[1]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);flag("npcDialogues",new NpcDialogueTranslator());flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("winterDialogues",NpcDialogueTranslator.winterDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());flag("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());flag("chatNotifications",new ChatNotificationTranslator(db));
        var inputs=Map.of("unknown",List.of("[NPC] Unknown: Future unpublished dialogue75."),"existing",List.of("[NPC] Kat: You can pick it up in 29 minutes 55 seconds.","[NPC] Maxwell: A wise choice, Player_75!"),"existing74",List.of("[NPC] Enchantment Nerd: There are many, many, many enchantments that exist in SkyBlock.","[NPC] Banker: Hello there!","[NPC] Alchemist: For now, you shouldn't let it get to your head."),"new_intro",List.of("[NPC] Alda: Are you here to buy an Abiphone?","[NPC] Angler Angus: Here, I've got some spare Fish Bait for ya!","[NPC] Apprentice: For a price, you can buy it here!"),"new_phone",List.of("[NPC] Bulvar: ✆ How about it?","[NPC] Alixer: ✆ Potions travel much faster by phone!"),"choice",List.of("Select an option: [What's an Abiphone?]","Select an option: ➜ [How are perks chosen?] ➜ [What's the deal with Ministers?] ➜ [What are Special Mayors?] ➜ [I think I'm good.]"));
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);if(!bean.isThreadCpuTimeEnabled())bean.setThreadCpuTimeEnabled(true);long thread=Thread.currentThread().threadId();var out=new JsonObject();
        for(String mode:List.of("unknown","existing","existing74","new_intro","new_phone","choice")){
            var rows=inputs.get(mode).stream().map(s->(Component)Component.literal(s).setStyle(Style.EMPTY.withColor(0x123456).withInsertion("unchanged").withClickEvent(new ClickEvent.SuggestCommand("/original")))).toList();for(int n=0;n<20000;n++)batch(rows);
            double[] wall=new double[4],cpu=new double[4],bytes=new double[4];double operations=40000;
            for(int r=0;r<4;r++){long allocation=bean.getThreadAllocatedBytes(thread),startedCpu=bean.getCurrentThreadCpuTime(),started=System.nanoTime();for(int n=0;n<4000;n++)batch(rows);wall[r]=(System.nanoTime()-started)/operations;cpu[r]=(bean.getCurrentThreadCpuTime()-startedCpu)/operations;bytes[r]=(bean.getThreadAllocatedBytes(thread)-allocation)/operations;}
            var result=new JsonObject();result.addProperty("ns_per_message",median(wall));result.addProperty("thread_cpu_ns_per_message",median(cpu));result.addProperty("bytes_per_message",median(bytes));result.addProperty("equal_useful_output",mode.equals("unknown")||mode.equals("existing")||mode.equals("existing74"));result.add("rounds_ns",new Gson().toJsonTree(wall));result.add("rounds_cpu_ns",new Gson().toJsonTree(cpu));result.add("rounds_bytes",new Gson().toJsonTree(bytes));out.add(mode,result);
        }
        out.addProperty("scope","Four rounds of40000 receipt calls/mode,200000 warmup calls;all provider setup outside timed regions. New positives translate additional content;not equal work. Calling-thread CPU is coarse on Windows. Not render/font/world/GPU/FPS/frame-time measurement.");Files.writeString(Path.of(args[0]),new GsonBuilder().setPrettyPrinting().create().toJson(out));System.out.println(out);
    }
}
