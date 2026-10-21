import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.RenderContext;

/** Actual fresh StackWalker scopes plus fixed/churning metadata, no reused caller snapshot. */
public final class BenchmarkContext67 {
    private static volatile int sink;
    private static int fresh(int depth){if(depth>0)return fresh(depth-1);try(var scope=RenderContext.open()){var current=RenderContext.current();return current.protectedText()?1:current.hud()?2:current.tooltip()?4:0;}}
    private static double median(double[] values){var sorted=values.clone();Arrays.sort(sorted);return (sorted[1]+sorted[2])/2;}
    private static final List<String> DEEP=Collections.nCopies(64,"at.hannibal2.skyhanni.Widget#render");
    private static final List<List<String>> CHURN=java.util.stream.IntStream.range(0,300).mapToObj(i->List.of("foreign.Churn"+i+"#method"+(i%40))).toList();
    private static int next;
    private static void batch(String mode){int out=0;for(int i=0;i<10;i++)out+=switch(mode){case "fresh_shallow"->fresh(0);case "fresh_deep32"->fresh(32);case "warm_metadata64"->RenderContext.classify(DEEP).hud()?1:0;default->RenderContext.classify(CHURN.get(next++%300)).hud()?1:0;};sink=out;}
    public static void main(String[] args)throws Exception{
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().threadId();var report=new JsonObject();
        for(String mode:List.of("fresh_shallow","fresh_deep32","warm_metadata64","churn_metadata300")){for(int i=0;i<20000;i++)batch(mode);double[] times=new double[4],bytes=new double[4];for(int r=0;r<4;r++){long memory=bean.getThreadAllocatedBytes(thread),start=System.nanoTime();for(int i=0;i<10000;i++)batch(mode);times[r]=(System.nanoTime()-start)/1e10;bytes[r]=(bean.getThreadAllocatedBytes(thread)-memory)/10000.0;}var result=new JsonObject();result.addProperty("ms_per10",median(times));result.addProperty("bytes_per10",median(bytes));result.add("rounds_ms",new Gson().toJsonTree(times));result.add("rounds_bytes",new Gson().toJsonTree(bytes));report.add(mode,result);}
        report.addProperty("scope","Fresh context API scopes and metadata only;20k warm batches,4x10k measured batches of10 operations. Not FPS, not GPU or real HUD draw count. All CPU/allocation rounds retained.");Files.writeString(Path.of(args[0]),new GsonBuilder().setPrettyPrinting().create().toJson(report));System.out.println(report);
    }
}
