import java.nio.file.*;
import java.time.*;
import java.util.*;
import com.google.gson.*;
import jdk.jfr.consumer.*;

/** Sanitized local-only JFR timeline. No chat, paths, process arguments or URLs. */
public final class AnalyzeFramePacing71 {
    public static void main(String[] args)throws Exception{
        var fps=new JsonArray();var pauses=new ArrayList<JsonObject>();var waits=new ArrayList<JsonObject>();
        Instant start=Instant.MAX;
        // Chunks are not chronological; use the earliest event as a shared origin.
        try(var origin=new RecordingFile(Path.of(args[0]))){while(origin.hasMoreEvents()){var t=origin.readEvent().getStartTime();if(t.isBefore(start))start=t;}}
        long allocationFirst=-1,allocationLast=-1;Instant allocationStart=null,allocationEnd=null;
        try(var file=new RecordingFile(Path.of(args[0]))){while(file.hasMoreEvents()){
            var e=file.readEvent();String kind=e.getEventType().getName();
            double elapsed=Duration.between(start,e.getStartTime()).toNanos()/1e9;
            if(kind.equals("minecraft.ClientFps")){var row=new JsonObject();row.addProperty("elapsed_seconds",elapsed);row.addProperty("fps",e.getInt("fps"));fps.add(row);}
            if(kind.equals("jdk.ThreadAllocationStatistics")){
                var thread=e.getThread("thread");if(thread!=null&&"Render thread".equals(thread.getJavaName())){
                    long bytes=e.getLong("allocated");if(allocationStart==null||e.getStartTime().isBefore(allocationStart)){allocationFirst=bytes;allocationStart=e.getStartTime();}if(allocationEnd==null||e.getStartTime().isAfter(allocationEnd)){allocationLast=bytes;allocationEnd=e.getStartTime();}
                }
            }
            double ms=e.getDuration().toNanos()/1e6;
            boolean pause=kind.equals("jdk.GCPhasePause")||kind.equals("jdk.ExecuteVMOperation")&&e.getBoolean("safepoint");
            var thread=e.getThread();boolean render=thread!=null&&"Render thread".equals(thread.getJavaName());
            if(ms<1||!pause&&!render)continue;
            var row=new JsonObject();row.addProperty("kind",kind);row.addProperty("elapsed_seconds",elapsed);row.addProperty("duration_ms",ms);
            if(kind.equals("jdk.ExecuteVMOperation"))row.addProperty("vm_operation",e.getString("operation"));
            var frames=new JsonArray();if(e.getStackTrace()!=null)for(var f:e.getStackTrace().getFrames().stream().limit(16).toList())frames.add(f.getMethod().getType().getName()+"."+f.getMethod().getName());
            row.add("stack_methods",frames);(pause?pauses:waits).add(row);
        }}
        var out=new JsonObject();out.addProperty("scope","Observed game with 0.15.70-dev; one-second FPS events are not frame times or 0.1% lows. No without-mod control. JVM safepoint events can overlap GC; do not sum them. Profiling startup can itself pause the game. Allocation delta is entire render thread, not mod allocation.");
        out.add("fps_once_per_second",fps);out.addProperty("render_thread_cumulative_allocated_delta_bytes",allocationLast-allocationFirst);
        out.addProperty("allocation_observed_seconds",allocationStart==null?0:Duration.between(allocationStart,allocationEnd).toNanos()/1e9);
        pauses.sort(Comparator.comparingDouble((JsonObject o)->o.get("duration_ms").getAsDouble()).reversed());
        waits.sort(Comparator.comparingDouble((JsonObject o)->o.get("duration_ms").getAsDouble()).reversed());
        out.add("safepoint_and_gc_events_ms_at_least1",new Gson().toJsonTree(pauses));out.add("render_thread_duration_events_ms_at_least1",new Gson().toJsonTree(waits.stream().limit(60).toList()));
        Files.writeString(Path.of(args[1]),new GsonBuilder().setPrettyPrinting().create().toJson(out));
        System.out.println("Sanitized timeline saved; fps events="+fps.size()+", pauses="+pauses.size()+", render duration events="+waits.size());
    }
}
