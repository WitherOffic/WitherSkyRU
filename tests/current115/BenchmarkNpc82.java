import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Isolated chat receipt, not game FPS, GPU, frame time, or a zero-cost claim. */
public final class BenchmarkNpc82 {
    private static volatile int sink;
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static void batch(List<Component> rows){for(int n=0;n<10;n++)sink^=SkyRuClient.npcChat(new GuiMessage(0,rows.get(n%rows.size()),null,GuiMessageSource.SYSTEM_SERVER,null)).content().getString().length();}
    private static double median(double[] values){var copy=values.clone();Arrays.sort(copy);return(copy[1]+copy[2])/2;}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[1]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);flag("npcDialogues",new NpcDialogueTranslator());flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("winterDialogues",NpcDialogueTranslator.winterDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());flag("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());flag("chatNotifications",new ChatNotificationTranslator(db));
        var inputs=new LinkedHashMap<String,List<String>>();
        inputs.put("unknown",List.of("[NPC] Unknown81: Future unpublished dialogue.","[NPC] Croesus: Unpublished future line80."));
        inputs.put("existing_dynamic",List.of("[NPC] Kat: You can pick it up in 29 minutes 55 seconds.","[NPC] Maxwell: A wise choice, Player_77!"));
        inputs.put("existing_exact",List.of("[NPC] Baker: I made one especially for you, here you go.","[NPC] Bingo: It's time to play SkyBlock Bingo!"));
        inputs.put("existing_style",List.of("§e[NPC] §6Baker§f: My §9Wheat Minions§f produce the §afinest flour§f around, but they can't keep up with today's demand..."));
        inputs.put("existing_tal",List.of("[NPC] Tal Ker: I'm the guy that never stops talking","[NPC] Tal Ker: I love talking, but I also think a lot!"));
        inputs.put("existing_wizard",List.of("[NPC] Wizard: Greetings, traveler!","§e[NPC] Wizardman§f: The first to deplete the other's §c❤ Hearts §fshall be declared the winner."));
        inputs.put("existing_tyashoi",List.of("[NPC] Tyashoi Alchemist: I did it!","§e[NPC] §5Tyashoi Alchemist§f: Do you really have feet instead of hands? How hard can it be to get §e5 §aDecayed Bats§f?"));
        inputs.put("existing_elle",List.of("[NPC] Elle: Oh, you already got everything?","§e[NPC] §cElle§f: We need to find the fuel for the Ballista, it §cmust §chave fallen in the lava§f with the rest of my supplies!"));
        inputs.put("new_kat_elizabeth",List.of("[NPC] Kat: Take my vacuum cleaner.","§e[NPC] §dElizabeth§f: §b✆ §fOoh, you got yourself an §3Abiphone§f!","§e[NPC] §5Kat§f: §fMake sure to empty the vacuum in the §aVermin Bin§f!"));
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);if(!bean.isThreadCpuTimeEnabled())bean.setThreadCpuTimeEnabled(true);long thread=Thread.currentThread().threadId();var out=new JsonObject();
        for(var entry:inputs.entrySet()){
            var rows=entry.getValue().stream().map(s->(Component)Component.literal(s).setStyle(Style.EMPTY.withColor(0x123456).withInsertion("fresh").withClickEvent(new ClickEvent.SuggestCommand("/unchanged82")))).toList();for(int n=0;n<20000;n++)batch(rows);
            double[] wall=new double[4],cpu=new double[4],bytes=new double[4];double operations=40000;
            for(int r=0;r<4;r++){long allocation=bean.getThreadAllocatedBytes(thread),startCpu=bean.getCurrentThreadCpuTime(),start=System.nanoTime();for(int n=0;n<4000;n++)batch(rows);wall[r]=(System.nanoTime()-start)/operations;cpu[r]=(bean.getCurrentThreadCpuTime()-startCpu)/operations;bytes[r]=(bean.getThreadAllocatedBytes(thread)-allocation)/operations;}
            var result=new JsonObject();result.addProperty("ns_per_message",median(wall));result.addProperty("cpu_ns_per_message",median(cpu));result.addProperty("bytes_per_message",median(bytes));result.addProperty("equal_useful_output",!entry.getKey().startsWith("new_"));result.add("rounds_ns",new Gson().toJsonTree(wall));result.add("rounds_cpu_ns",new Gson().toJsonTree(cpu));result.add("rounds_bytes",new Gson().toJsonTree(bytes));out.add(entry.getKey(),result);
        }
        out.addProperty("scope","Chat receipt only; four40000-call rounds after200000 warmup calls/mode; setup excluded. New Kat and Elizabeth positives do additional translation work, not equal output. Windows calling-thread CPU coarse; not FPS/0.1% lows.");Files.writeString(Path.of(args[0]),new GsonBuilder().setPrettyPrinting().create().toJson(out));
    }
}
