package ru.wither.skyru;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** Identical compaction results; isolates conditional source/override work, not native rendering. */
public final class BenchmarkNarrow95 {
    private static volatile Object sink;
    private static int next;
    private static void flag(String name,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,v);}
    private static double median(double[] values){var a=values.clone();Arrays.sort(a);return(a[1]+a[2])/2;}
    private static FormattedCharSequence source=FormattedCharSequence.forward("Normal",Style.EMPTY);
    private static FormattedCharSequence translated=FormattedCharSequence.forward("Обычный режим",Style.EMPTY);
    private static FormattedCharSequence operation(boolean dynamic){
        if(!dynamic)return SkyRuClient.narrowHud(source,translated);
        int n=next++;var style=Style.EMPTY.withColor(n).withClickEvent(new ClickEvent.SuggestCommand("/current "+n)).withHoverEvent(new HoverEvent.ShowText(Component.literal("current "+n)));
        return SkyRuClient.narrowHud(FormattedCharSequence.forward("Sawdust: "+n,style),FormattedCharSequence.forward("Опилки: "+n,style));
    }
    private static String text(FormattedCharSequence s){var out=new StringBuilder();s.accept((i,style,cp)->{out.appendCodePoint(cp);return true;});return out.toString();}
    public static void main(String[] args)throws Exception{
        var output=new JsonObject();boolean optimized=Boolean.parseBoolean(args[2]);
        flag("enabled",true);flag("onlyHypixel",false);
        var field=RenderContext.class.getDeclaredField("CURRENT");field.setAccessible(true);((ThreadLocal<Object>)field.get(null)).set(new RenderContext.Snapshot(false,true,false,false));
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);if(!bean.isThreadCpuTimeEnabled())bean.setThreadCpuTimeEnabled(true);long thread=Thread.currentThread().threadId();
        for(String mode:List.of("no_overrides","dynamic_no_overrides","matching_override","unrelated_override")){
            var directory=Path.of(args[1],mode);Files.createDirectories(directory);
            if(mode.endsWith("_override"))Files.writeString(directory.resolve("overrides.tsv"),mode.equals("matching_override")?"Normal\tОбычный режим\n":"Unrelated caption\tСвоя подпись\n");
            var db=new OfflineDictionary(directory);db.setRecordUnknown(false);flag("dictionary",db);
            boolean dynamic=mode.startsWith("dynamic");int[] visits={0};FormattedCharSequence counted=s->{visits[0]++;return source.accept(s);};
            var result=SkyRuClient.narrowHud(counted,translated);
            if(!text(result).equals(mode.equals("matching_override")?"Обычный режим":"Обычный"))throw new AssertionError("different useful output");
            if(visits[0]!=(optimized&&!mode.endsWith("_override")?0:1))throw new AssertionError("unexpected source visits");
            for(int n=0;n<200000;n++)sink=operation(dynamic);
            var wall=new double[4];var cpu=new double[4];var bytes=new double[4];int calls=100000;
            for(int r=0;r<4;r++){
                long memory=bean.getThreadAllocatedBytes(thread),startCpu=bean.getCurrentThreadCpuTime(),start=System.nanoTime();
                for(int n=0;n<calls;n++)sink=operation(dynamic);
                wall[r]=(System.nanoTime()-start)/(double)calls;cpu[r]=(bean.getCurrentThreadCpuTime()-startCpu)/(double)calls;bytes[r]=(bean.getThreadAllocatedBytes(thread)-memory)/(double)calls;
            }
            var row=new JsonObject();row.addProperty("equal_useful_output",true);row.addProperty("ns_per_operation",median(wall));row.addProperty("cpu_ns_per_operation",median(cpu));row.addProperty("bytes_per_operation",median(bytes));row.add("rounds_ns",new Gson().toJsonTree(wall));row.add("rounds_cpu_ns",new Gson().toJsonTree(cpu));row.add("rounds_bytes",new Gson().toJsonTree(bytes));output.add(mode,row);
        }
        output.addProperty("scope","Direct narrowHud API with fixed HUD caller context;200k warmup/4x100k calls;source construction included equally for dynamic mode;not native rendering/FPS. Existing custom overrides retained;no override settings modified in game.");
        Files.writeString(Path.of(args[0]),new GsonBuilder().setPrettyPrinting().create().toJson(output));
    }
}
