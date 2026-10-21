import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Synthetic receipt-only CPU cost; not per-frame FPS or a network/chat integration test. */
public class BenchmarkNpcRequests {
    private static volatile int consumed;
    private static double median(double[] v){var a=v.clone();Arrays.sort(a);return(a[1]+a[2])/2;}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);var chat=new ChatNotificationTranslator(db);var report=new JsonObject();report.addProperty("label",args[1]);report.addProperty("scope","NPC request API on receipt only;10 fixed messages, four rounds after warmup; separate JVMs. Not FPS or entire queue.");
        var inputs=Map.of(
            "non_npc_server",List.of("[Sacks] +123 items. (Last 5s.)","Кошелёк: 555,222","RARE DROP! Rarefinder Chip (+1.5%)","LEVEL UP! Your Cactus Knife Mk. I just leveled up to Level 7!","Normal","Gems: 75","Take a seat in the Briefing Room","www.hypixel.net","You reached a 10 Kill Combo!","Select an option: [Okay!]"),
            "existing_npc",Collections.nCopies(10,"[NPC] Kat: You can pick it up in 29 minutes 55 seconds."),
            "unknown_npc",Collections.nCopies(10,"[NPC] Unknown: Unreviewed statement.")
        );
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long tid=Thread.currentThread().threadId();boolean memory=bean.isThreadAllocatedMemorySupported();if(memory&&!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);
        for(String name:List.of("non_npc_server","existing_npc","unknown_npc")){
            var rows=inputs.get(name).stream().map(s->(Component)Component.literal(s)).toList();
            for(int warm=0;warm<1000;warm++)for(var row:rows)consumed^=chat.translateNpcRequest(row,GuiMessageSource.SYSTEM_SERVER,false).getString().length();
            double[] times=new double[4],bytes=new double[4];
            for(int round=0;round<4;round++){long startBytes=memory?bean.getThreadAllocatedBytes(tid):0,start=System.nanoTime();for(int batch=0;batch<1000;batch++)for(var row:rows)consumed^=chat.translateNpcRequest(row,GuiMessageSource.SYSTEM_SERVER,false).getString().length();times[round]=(System.nanoTime()-start)/1e9;bytes[round]=memory?(bean.getThreadAllocatedBytes(tid)-startBytes)/1000.0:-1;}
            var item=new JsonObject();item.addProperty("ms_per10",median(times));item.addProperty("bytes_per10",median(bytes));item.add("rounds_ms",new Gson().toJsonTree(times));report.add(name,item);System.out.printf(Locale.ROOT,"NPC %s %s: %.6fms /%.0fbytes per10; not FPS%n",args[1],name,median(times),median(bytes));
        }
        Files.writeString(Path.of(args[2]),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
    }
}
