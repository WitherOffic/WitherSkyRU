package ru.wither.skyru;

import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;
import com.google.gson.*;
import net.minecraft.network.chat.Style;
import net.minecraft.util.*;

/** Prepared roster and real sequence API. No reflection/source construction in timed name lookups. */
public final class BenchmarkNames73 {
    private static volatile Object sink;private static volatile int matched;private static int next;
    private static Predicate<String> index;
    private static final List<String> NEGATIVE=List.of("Normal","Gems: 75","Sawdust: 5,011 (+2,283)","Pet: Rabbit","Talk to Susan","Cleanup: 52.1%","Mining Fiesta","Your item grants bonus Combat experience.");
    private static final List<String> POSITIVE=List.of("[VIP] PerfUser17: hello","[MVP+] Wither_101: Thorns","Thor","the player PerfUser79 is visiting your Garden");
    private static final List<String> LEGACY=List.of("§a[MVP+] §bPerfUser17: hello","§eGems: §b75","§aYour item grants bonus Combat experience.");
    private static final FormattedCharSequence[] ROWS=NEGATIVE.stream().map(s->FormattedCharSequence.forward(s,Style.EMPTY)).toArray(FormattedCharSequence[]::new);
    private static void flag(Class<?> type,String name,Object value)throws Exception{var field=type.getDeclaredField(name);field.setAccessible(true);field.set(null,value);}
    private static void batch(String mode){int count=0;for(int i=0;i<10;i++){
        switch(mode){
            case "index_positive"->count+=index.test(POSITIVE.get(i%POSITIVE.size()))?1:0;
            case "index_legacy"->count+=index.test(LEGACY.get(i%LEGACY.size()))?1:0;
            case "sequence_stable_roster"->sink=SkyRuClient.sequence(ROWS[i%ROWS.length],true);
            case "sequence_dynamic_roster"->sink=SkyRuClient.sequence(FormattedCharSequence.forward("Gems: "+next++,Style.EMPTY),true);
            default->count+=index.test(NEGATIVE.get(i%NEGATIVE.size()))?1:0;
        }
    }matched=count;}
    private static double median(double[] values){var sorted=values.clone();Arrays.sort(sorted);return(sorted[1]+sorted[2])/2;}
    public static void main(String[] args)throws Exception{
        var names=new HashSet<String>();for(int n=0;n<80;n++)names.add("PerfUser"+n);names.add("Wither_101");names.add("Thor");
        var prepare=PlayerNames.class.getDeclaredMethod("prepare",Set.class);prepare.setAccessible(true);index=(Predicate<String>)prepare.invoke(null,Set.copyOf(names));
        flag(PlayerNames.class,"names",Set.copyOf(names));flag(PlayerNames.class,"connection",null);flag(PlayerNames.class,"checkedAt",System.nanoTime()+3_600_000_000_000L);
        var db=new OfflineDictionary(Path.of(args[1]));db.setRecordUnknown(false);flag(SkyRuClient.class,"dictionary",db);flag(SkyRuClient.class,"enabled",true);flag(SkyRuClient.class,"onlyHypixel",false);
        var context=RenderContext.class.getDeclaredField("CURRENT");context.setAccessible(true);((ThreadLocal<Object>)context.get(null)).set(new RenderContext.Snapshot(false,true,false,false));
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);if(!bean.isThreadCpuTimeEnabled())bean.setThreadCpuTimeEnabled(true);long thread=Thread.currentThread().threadId();var out=new JsonObject();
        for(String mode:List.of("index_negative","index_positive","index_legacy","sequence_stable_roster","sequence_dynamic_roster")){
            for(int n=0;n<50000;n++)batch(mode);double[] wall=new double[4],bytes=new double[4],cpu=new double[4];int batches=mode.equals("sequence_dynamic_roster")?20000:50000;double operations=batches*10.0;
            for(int r=0;r<4;r++){long allocation=bean.getThreadAllocatedBytes(thread),startedCpu=bean.getCurrentThreadCpuTime(),started=System.nanoTime();for(int n=0;n<batches;n++)batch(mode);wall[r]=(System.nanoTime()-started)/operations;cpu[r]=(bean.getCurrentThreadCpuTime()-startedCpu)/operations;bytes[r]=(bean.getThreadAllocatedBytes(thread)-allocation)/operations;}
            var result=new JsonObject();result.addProperty("operations_per_round",operations);result.addProperty("ns_per_operation",median(wall));result.addProperty("thread_cpu_ns_per_operation",median(cpu));result.addProperty("bytes_per_operation",median(bytes));result.add("rounds_ns",new Gson().toJsonTree(wall));result.add("rounds_cpu_ns",new Gson().toJsonTree(cpu));result.add("rounds_bytes",new Gson().toJsonTree(bytes));out.add(mode,result);
        }
        out.addProperty("scope","82-name fixed roster. Warm500k operations/mode,4 measured rounds of500k operations except200k dynamic sequences. Name index directly and real sequence API under a fixed caller snapshot. Legacy-format mode keeps original regex stripping. Thread CPU clock quantized on Windows. Not FPS,0.1%low,GPU,frame time,live roster refresh or complete GUI draw.");
        Files.writeString(Path.of(args[0]),new GsonBuilder().setPrettyPrinting().create().toJson(out));System.out.println(out);
    }
}
