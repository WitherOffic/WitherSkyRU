package ru.wither.skyru;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** All nine existing numeric HUD labels; same live inputs and glyph consumption on95/96. */
public final class BenchmarkDynamicHud96 {
    private static final String[] NAMES={"Sawdust","Cleanup","Gems","North Stars","Coins/h","Farming XP/h","Farming Level","Yaw","Pitch"};
    private static volatile Object sink;private static volatile long consumed;private static long sum;private static int next;
    private static final FormattedCharSink CONSUME=(i,s,p)->{sum+=i+p+(s.getColor()==null?0:s.getColor().getValue());return true;};
    private static void flag(String name,Object value)throws Exception{var field=SkyRuClient.class.getDeclaredField(name);field.setAccessible(true);field.set(null,value);}
    private static double median(double[] values){var copy=values.clone();Arrays.sort(copy);return(copy[1]+copy[2])/2;}
    private static FormattedCharSequence input(String mode,int n){
        String name=NAMES[n%NAMES.length],label=name+": ",number=n+(name.equals("Sawdust")?" (+12%)":name.equals("Cleanup")?".25%":"");
        if(mode.equals("padding6_consume"))label="      "+label;
        if(mode.equals("invalid_boundary_consume"))label="       "+label;
        if(mode.equals("fresh_labels_actions_consume")){
            var style=Style.EMPTY.withColor(n).withClickEvent(new ClickEvent.SuggestCommand("/label "+n)).withHoverEvent(new HoverEvent.ShowText(Component.literal("current "+n)));
            return FormattedCharSequence.composite(FormattedCharSequence.forward(label,style),FormattedCharSequence.forward(number,style.withColor(n+1).withClickEvent(new ClickEvent.RunCommand("/value "+n))));
        }
        return FormattedCharSequence.forward(label+number,Style.EMPTY);
    }
    private static void operation(String mode){var output=SkyRuClient.sequence(input(mode,next++),true);output.accept(CONSUME);sink=output;}
    private static List<String> snapshot(FormattedCharSequence source){var result=new ArrayList<String>();source.accept((i,s,p)->{result.add(i+":"+p+":"+s);return true;});return result;}
    public static void main(String[] args)throws Exception{
        flag("enabled",true);flag("onlyHypixel",false);
        var field=RenderContext.class.getDeclaredField("CURRENT");field.setAccessible(true);((ThreadLocal<Object>)field.get(null)).set(new RenderContext.Snapshot(false,true,false,false));
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);if(!bean.isThreadCpuTimeEnabled())bean.setThreadCpuTimeEnabled(true);long thread=Thread.currentThread().threadId();var output=new JsonObject();
        for(String mode:List.of("numeric_uniform_consume","fresh_labels_actions_consume","padding6_consume","invalid_boundary_consume","unrelated_overrides_consume")){
            var directory=Path.of(args[1],mode);Files.createDirectories(directory);if(mode.equals("unrelated_overrides_consume"))Files.writeString(directory.resolve("overrides.tsv"),"Unrelated caption\tМоя подпись\n");var db=new OfflineDictionary(directory);db.setRecordUnknown(false);flag("dictionary",db);
            // Retain a deterministic preflight snapshot for cross-version validation outside measured loops.
            var preflight=new ArrayList<List<String>>();for(int n=0;n<9;n++)preflight.add(snapshot(SkyRuClient.sequence(input(mode,n+42),true)));
            for(int n=0;n<200000;n++)operation(mode);var wall=new double[4];var cpu=new double[4];var bytes=new double[4];int calls=100000;
            for(int r=0;r<4;r++){
                long memory=bean.getThreadAllocatedBytes(thread),startCpu=bean.getCurrentThreadCpuTime(),start=System.nanoTime();
                for(int n=0;n<calls;n++)operation(mode);
                wall[r]=(System.nanoTime()-start)/(double)calls;cpu[r]=(bean.getCurrentThreadCpuTime()-startCpu)/(double)calls;bytes[r]=(bean.getThreadAllocatedBytes(thread)-memory)/(double)calls;consumed=sum;
            }
            var row=new JsonObject();row.addProperty("ns_per_operation",median(wall));row.addProperty("cpu_ns_per_operation",median(cpu));row.addProperty("bytes_per_operation",median(bytes));row.add("rounds_ns",new Gson().toJsonTree(wall));row.add("rounds_cpu_ns",new Gson().toJsonTree(cpu));row.add("rounds_bytes",new Gson().toJsonTree(bytes));row.add("preflight_glyph_style_snapshot",new Gson().toJsonTree(preflight));output.add(mode,row);
        }
        output.addProperty("scope","Five mixed dynamic HUD modes,cycling all nine existing numeric labels.200000 warmup/four100000-call rounds;identical new source construction and glyph consumption on95/96;fixed HUD context. Preflight snapshots retained for exact comparison. Includes fresh label/value actions,padding6/7 and unrelated overrides. No native font/GPU/render frequency/game FPS measurement.");
        Files.writeString(Path.of(args[0]),new GsonBuilder().setPrettyPrinting().create().toJson(output));
    }
}
