import java.nio.file.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;
import com.google.gson.*;

/** Identical standalone workload for old and new JARs. Not actual game FPS or complete GUI drawing. */
public class BenchmarkRenderPerformance {
    private static volatile int consumed;
    private static final int BATCHES=200;
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    private static int count(FormattedCharSequence seq){int[] length={0};seq.accept((i,s,p)->{length[0]++;return true;});return length[0];}
    private static double median(double[] values){var sorted=values.clone();Arrays.sort(sorted);return (sorted[1]+sorted[2])/2;}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var strings=List.of("Кошелёк: 165,813","Биты: 650/0","Сад","Конец осени, 15","Ур. 75 (32.9%)","09:10am ☀","Normal","Sawdust: 5,011 (+2,283)","Farming Level:","www.hypixel.net");
        var components=strings.stream().map(s->Component.literal(s).setStyle(Style.EMPTY.withColor(0x55ffff))).toList();
        var sequences=components.stream().map(Component::getVisualOrderText).toList();
        for(int i=0;i<250;i++){for(var c:components)consumed^=SkyRuClient.component(c).getString().length();for(var s:sequences)consumed^=count(SkyRuClient.sequence(s,true));}
        var management=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
        boolean allocation=management.isThreadAllocatedMemorySupported();if(allocation&&!management.isThreadAllocatedMemoryEnabled())management.setThreadAllocatedMemoryEnabled(true);
        long thread=Thread.currentThread().threadId();double[] componentMs=new double[4],sequenceMs=new double[4],componentBytes=new double[4],sequenceBytes=new double[4];
        for(int round=0;round<4;round++){
            long memoryA=allocation?management.getThreadAllocatedBytes(thread):0,a=System.nanoTime();
            for(int i=0;i<BATCHES;i++)for(var c:components)consumed^=SkyRuClient.component(c).getString().length();
            long b=System.nanoTime(),memoryB=allocation?management.getThreadAllocatedBytes(thread):0;
            for(int i=0;i<BATCHES;i++)for(var s:sequences)consumed^=count(SkyRuClient.sequence(s,true));
            long c=System.nanoTime(),memoryC=allocation?management.getThreadAllocatedBytes(thread):0;
            componentMs[round]=(b-a)/(BATCHES*1e6);sequenceMs[round]=(c-b)/(BATCHES*1e6);
            componentBytes[round]=(memoryB-memoryA)/(double)BATCHES;sequenceBytes[round]=(memoryC-memoryB)/(double)BATCHES;
        }
        long begin=System.nanoTime();int dynamicBatches=100;
        for(int batch=0;batch<dynamicBatches;batch++)for(int row=0;row<10;row++){
            var source=Component.literal("Кошелёк: "+(batch*10+row)+" (+"+row+")").setStyle(Style.EMPTY.withColor(0x55ffff));
            consumed^=SkyRuClient.component(source).getString().length();consumed^=count(SkyRuClient.sequence(source.getVisualOrderText(),true));
        }
        double dynamicMs=(System.nanoTime()-begin)/(dynamicBatches*1e6);
        var report=new JsonObject();report.addProperty("label",args[1]);report.addProperty("scope","Standalone synthetic 10-row text APIs, not actual game FPS/font/panel extraction. Four round medians, 250 warm batches, 200 measured batches per round. Dynamic test uses new full values every row.");
        report.addProperty("loaded_client_source",SkyRuClient.class.getProtectionDomain().getCodeSource().getLocation().toString());
        report.addProperty("component_ms_per_10_rows",median(componentMs));report.addProperty("sequence_ms_per_10_rows",median(sequenceMs));
        report.addProperty("component_allocated_bytes_per_10_rows",allocation?median(componentBytes):-1);report.addProperty("sequence_allocated_bytes_per_10_rows",allocation?median(sequenceBytes):-1);
        report.addProperty("dynamic_component_plus_sequence_ms_per_10_rows",dynamicMs);report.add("component_rounds",new Gson().toJsonTree(componentMs));report.add("sequence_rounds",new Gson().toJsonTree(sequenceMs));
        report.addProperty("consumed",consumed);
        Files.writeString(Path.of(args[2]),new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(report)+"\n");
        System.out.printf(Locale.ROOT,"PERF %s: component %.4f ms / %.0f bytes; sequence %.4f ms / %.0f bytes; changing values both APIs %.4f ms per10 rows; standalone, not FPS%n",args[1],median(componentMs),median(componentBytes),median(sequenceMs),median(sequenceBytes),dynamicMs);
    }
}
