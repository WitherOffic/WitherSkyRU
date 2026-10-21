import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import jdk.jfr.consumer.*;

/** Read-only offline analysis of a bounded local JFR, no JVM attach or game mutation. */
public final class AnalyzeGameplayProfile67 {
    private static boolean owned(String name){return name.startsWith("ru.wither.skyru.")||name.contains("$witherskyru");}
    private static void add(Map<String,Long> map,String key,long weight){map.merge(key,weight,Long::sum);}
    private static JsonArray top(Map<String,Long> map,int limit){var out=new JsonArray();map.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).limit(limit).forEach(e->{var r=new JsonObject();r.addProperty("name",e.getKey());r.addProperty("count_or_weight",e.getValue());out.add(r);});return out;}
    public static void main(String[] args)throws Exception{
        var kinds=new TreeMap<String,Long>();var threads=new HashMap<String,Long>();var self=new HashMap<String,Long>();var inclusive=new HashMap<String,Long>();var ownSelf=new HashMap<String,Long>();var ownInclusive=new HashMap<String,Long>();var allocation=new HashMap<String,Long>();
        long samples=0,renderSamples=0,renderMod=0,modAny=0,allocWeight=0,modAlloc=0;double pauseTotal=0,pauseMax=0;long pauses=0;
        var renderStacks=new HashMap<String,Long>();var boundaries=new HashMap<String,Long>();
        try(var file=new RecordingFile(Path.of(args[0]))){while(file.hasMoreEvents()){
            var e=file.readEvent();String kind=e.getEventType().getName();add(kinds,kind,1);
            boolean sample=kind.equals("jdk.ExecutionSample")||kind.equals("jdk.NativeMethodSample"),alloc=kind.equals("jdk.ObjectAllocationSample");
            if(kind.equals("jdk.GCPhasePause")){double ms=e.getDuration().toNanos()/1e6;pauseTotal+=ms;pauseMax=Math.max(pauseMax,ms);pauses++;}
            if(!sample&&!alloc)continue;var stack=e.getStackTrace();if(stack==null||stack.getFrames().isEmpty())continue;
            var names=new ArrayList<String>();for(var f:stack.getFrames()){var m=f.getMethod();names.add(m.getType().getName()+"."+m.getName());}
            boolean own=names.stream().anyMatch(AnalyzeGameplayProfile67::owned);
            if(alloc){long weight=e.getLong("weight");allocWeight+=weight;if(own){modAlloc+=weight;for(String n:new LinkedHashSet<>(names))if(owned(n))add(allocation,n,weight);}continue;}
            samples++;var thread=e.getThread("sampledThread");String threadName=thread==null?"unknown":thread.getJavaName();add(threads,threadName,1);if(own)modAny++;
            if(threadName.equals("Render thread")){
                renderSamples++;if(own){renderMod++;add(renderStacks,String.join(" <- ",names.stream().limit(12).toList()),1);}
                add(self,names.getFirst(),1);for(String n:new LinkedHashSet<>(names))add(inclusive,n,1);
                if(owned(names.getFirst()))add(ownSelf,names.getFirst(),1);
                for(String n:new LinkedHashSet<>(names))if(owned(n))add(ownInclusive,n,1);
                if(own){String next=names.stream().filter(n->!owned(n)&&!n.startsWith("java.")&&!n.startsWith("jdk.")).findFirst().orElse("none");add(boundaries,next,1);}
            }
        }}
        var report=new JsonObject();report.addProperty("scope","30-second observational local gameplay JFR; sampled CPU stacks and statistical allocation weights, not measured FPS or without-mod A/B. Own attribution includes injected witherskyru handlers under Minecraft class names. Inclusive shares overlap; JFR and uncontrolled gameplay/background load can affect samples. Allocation weights have not been validated as absolute bytes allocated during this interval and must not be converted to MB/s. Raw JFR kept locally only.");report.addProperty("sample_events",samples);report.addProperty("all_thread_samples_with_mod_stack",modAny);report.addProperty("render_thread_samples",renderSamples);report.addProperty("render_samples_with_mod_stack",renderMod);report.addProperty("render_mod_inclusive_sample_percent",renderSamples==0?0:100.0*renderMod/renderSamples);report.addProperty("sampled_allocation_weight_all_threads",allocWeight);report.addProperty("sampled_allocation_weight_with_mod_on_stack",modAlloc);report.addProperty("gc_pause_count",pauses);report.addProperty("gc_pause_total_ms",pauseTotal);report.addProperty("gc_pause_max_ms",pauseMax);report.add("event_kinds",top(kinds,100));report.add("threads",top(threads,20));report.add("render_top_self",top(self,25));report.add("render_top_inclusive",top(inclusive,35));report.add("render_mod_self",top(ownSelf,30));report.add("render_mod_inclusive",top(ownInclusive,35));report.add("mod_allocation_inclusive_weights",top(allocation,25));report.add("mod_render_stack_examples",top(renderStacks,12));report.add("mod_first_non_jdk_or_mod_frames",top(boundaries,15));
        Files.writeString(Path.of(args[1]),new GsonBuilder().setPrettyPrinting().create().toJson(report));System.out.println(report);
    }
}
