package ru.wither.skyru;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** Real entry points on71 and72; no reflection or reporting in measured loops. */
public final class BenchmarkPrepared72 {
    private static volatile Object sink;private static volatile long consumed;private static int next;private static long glyphSum;
    private static final FormattedCharSink CONSUME=(i,s,cp)->{glyphSum+=i+cp+(s.getColor()==null?0:s.getColor().getValue());return true;};
    private static final FormattedCharSequence[] ROWS=List.of("Gems: 75","Normal","Sawdust: 5,011 (+2,283)","Pet: Rabbit","Talk to Susan","Cleanup: 52.1%","Mining Fiesta","Foreign stable HUD text").stream().map(s->FormattedCharSequence.forward(s,Style.EMPTY)).toArray(FormattedCharSequence[]::new);
    private static final FormattedCharSequence FRAGMENTED;
    static{var parts=new ArrayList<FormattedCharSequence>();String text="abcdefghijklmnop";for(int i=0;i<16;i++)parts.add(FormattedCharSequence.forward(text.substring(i,i+1),Style.EMPTY.withColor(i)));FRAGMENTED=FormattedCharSequence.composite(parts);}
    private static void flag(String name,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,v);}
    private static void batch(String mode,boolean consume){for(int i=0;i<10;i++){
        switch(mode){
            case "stable_hud"->sink=SkyRuClient.sequence(ROWS[i%ROWS.length],true);
            case "stable_outside_fresh_context"->sink=SkyRuClient.sequenceOutsideHud(ROWS[i%ROWS.length]);
            case "dynamic_counter"->sink=SkyRuClient.sequence(FormattedCharSequence.forward("Gems: "+next++,Style.EMPTY),true);
            case "fresh_colors_actions"->{var style=Style.EMPTY.withColor(next++&0xffffff).withClickEvent(new ClickEvent.RunCommand("/current"+next)).withHoverEvent(new HoverEvent.ShowText(Component.literal("current hint"+next)));sink=SkyRuClient.sequence(FormattedCharSequence.forward("Gems: 75",style),true);}
            case "fragmented16"->sink=SkyRuClient.sequence(FRAGMENTED,true);
            default->sink=SkyRuClient.sequence(FormattedCharSequence.forward("я".repeat(300),Style.EMPTY),true);
        }
        if(consume)((FormattedCharSequence)sink).accept(CONSUME);
    }if(consume)consumed=glyphSum;}
    private static double median(double[] a){var copy=a.clone();Arrays.sort(copy);return(copy[1]+copy[2])/2;}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[1]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var f=RenderContext.class.getDeclaredField("CURRENT");f.setAccessible(true);((ThreadLocal<Object>)f.get(null)).set(new RenderContext.Snapshot(false,true,false,false));
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().threadId();var out=new JsonObject();
        if(bean.isThreadCpuTimeSupported()&&!bean.isThreadCpuTimeEnabled())bean.setThreadCpuTimeEnabled(true);
        for(String mode:List.of("stable_hud","stable_outside_fresh_context","dynamic_counter","fresh_colors_actions","fragmented16","over_limit_uncached","stable_hud_consume","dynamic_counter_consume","fresh_colors_actions_consume","fragmented16_consume")){
            boolean consume=mode.endsWith("_consume");String base=consume?mode.substring(0,mode.length()-8):mode;
            for(int i=0;i<20000;i++)batch(base,consume);double[] nanos=new double[4],bytes=new double[4],cpu=new double[4];int batches=base.equals("dynamic_counter")?20000:4000;double operations=batches*10.0;
            for(int r=0;r<4;r++){long memory=bean.getThreadAllocatedBytes(thread),startCpu=bean.getCurrentThreadCpuTime(),start=System.nanoTime();for(int i=0;i<batches;i++)batch(base,consume);nanos[r]=(System.nanoTime()-start)/operations;cpu[r]=(bean.getCurrentThreadCpuTime()-startCpu)/operations;bytes[r]=(bean.getThreadAllocatedBytes(thread)-memory)/operations;}
            var row=new JsonObject();row.addProperty("measured_operations_per_round",operations);row.addProperty("ns_per_operation",median(nanos));row.addProperty("thread_cpu_ns_per_operation",median(cpu));row.addProperty("bytes_per_operation",median(bytes));row.add("rounds_ns",new Gson().toJsonTree(nanos));row.add("rounds_thread_cpu_ns",new Gson().toJsonTree(cpu));row.add("rounds_bytes",new Gson().toJsonTree(bytes));out.add(mode,row);
        }
        out.addProperty("scope","Same sequence/sequenceOutsideHud API on71/72.20k warm batches;4 measured rounds of10-call batches:20k batches for dynamic counters,4k otherwise. Wall nanoTime plus calling-thread CPU counters. CPU clock granularity can distort short modes; only long dynamic-counter rounds used for focused CPU interpretation. Direct HUD modes use fixed caller snapshot;outside includes fresh StackWalker. Source construction included equally. Not game FPS,0.1%low,GPU,draw frequencies or full-frame timing.");
        Files.writeString(Path.of(args[0]),new GsonBuilder().setPrettyPrinting().create().toJson(out));System.out.println(out);
    }
}
