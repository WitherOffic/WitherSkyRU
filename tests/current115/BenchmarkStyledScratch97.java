package ru.wither.skyru;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** Same public frontend on96/97, including small mixed UTF16 input and real output consumption. */
public final class BenchmarkStyledScratch97 {
    private static volatile Object sink,innerSink;private static volatile long consumed;private static long sum;private static int next;
    private static final FormattedCharSequence ENGLISH=FormattedCharSequence.forward("Gems: 75",Style.EMPTY);
    private static final FormattedCharSequence RUSSIAN=FormattedCharSequence.forward("Самоцветы: 75",Style.EMPTY);
    private static final FormattedCharSequence LARGE=FormattedCharSequence.forward("Длинный текст 😀 ".repeat(60),Style.EMPTY);
    private static final FormattedCharSequence INNER=FormattedCharSequence.forward("Внутри 😀",Style.EMPTY);
    private static final FormattedCharSequence NESTED=s->{innerSink=StyledRuns.collect(INNER);return ENGLISH.accept(s);};
    private static final FormattedCharSink CONSUME=(i,s,p)->{sum+=i+p+(s.getColor()==null?0:s.getColor().getValue());return true;};
    private static void flag(String name,Object value)throws Exception{var field=SkyRuClient.class.getDeclaredField(name);field.setAccessible(true);field.set(null,value);}
    private static double median(double[] values){var copy=values.clone();Arrays.sort(copy);return(copy[1]+copy[2])/2;}
    private static FormattedCharSequence source(String mode,int n){return switch(mode){
        case "stable_english"->ENGLISH;
        case "stable_russian"->RUSSIAN;
        case "alternating_languages"->(n&1)==0?ENGLISH:RUSSIAN;
        case "nested_collect"->NESTED;
        case "large_text"->LARGE;
        default->{
            var first=Style.EMPTY.withColor(n).withClickEvent(new ClickEvent.RunCommand("/label "+n)).withHoverEvent(new HoverEvent.ShowText(Component.literal("now "+n)));
            yield FormattedCharSequence.composite(FormattedCharSequence.forward((n&1)==0?"Gems: ":"Самоцветы: ",first),FormattedCharSequence.forward("75",first.withColor(n+1).withClickEvent(new ClickEvent.SuggestCommand("/value "+n))));
        }
    };}
    private static void operation(String mode){var output=SkyRuClient.sequence(source(mode,next++),true);output.accept(CONSUME);sink=output;}
    private static List<String> snapshot(FormattedCharSequence source){var result=new ArrayList<String>();source.accept((i,s,p)->{result.add(i+":"+p+":"+s);return true;});return result;}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[1]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var field=RenderContext.class.getDeclaredField("CURRENT");field.setAccessible(true);((ThreadLocal<Object>)field.get(null)).set(new RenderContext.Snapshot(false,true,false,false));
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);if(!bean.isThreadCpuTimeEnabled())bean.setThreadCpuTimeEnabled(true);long thread=Thread.currentThread().threadId();var output=new JsonObject();
        for(String mode:List.of("stable_english","stable_russian","alternating_languages","fresh_fragment_styles","nested_collect","large_text")){
            var preflight=new ArrayList<List<String>>();for(int n=0;n<3;n++)preflight.add(snapshot(SkyRuClient.sequence(source(mode,n),true)));
            int warm=mode.equals("large_text")?20000:300000,calls=mode.equals("large_text")?20000:200000;
            for(int n=0;n<warm;n++)operation(mode);var wall=new double[4];var cpu=new double[4];var bytes=new double[4];
            for(int r=0;r<4;r++){
                long memory=bean.getThreadAllocatedBytes(thread),startCpu=bean.getCurrentThreadCpuTime(),start=System.nanoTime();
                for(int n=0;n<calls;n++)operation(mode);
                wall[r]=(System.nanoTime()-start)/(double)calls;cpu[r]=(bean.getCurrentThreadCpuTime()-startCpu)/(double)calls;bytes[r]=(bean.getThreadAllocatedBytes(thread)-memory)/(double)calls;consumed=sum;
            }
            var row=new JsonObject();row.addProperty("ns_per_operation",median(wall));row.addProperty("cpu_ns_per_operation",median(cpu));row.addProperty("bytes_per_operation",median(bytes));row.addProperty("calls_per_round",calls);row.add("rounds_ns",new Gson().toJsonTree(wall));row.add("rounds_cpu_ns",new Gson().toJsonTree(cpu));row.add("rounds_bytes",new Gson().toJsonTree(bytes));row.add("preflight_glyph_style_snapshot",new Gson().toJsonTree(preflight));output.add(mode,row);
        }
        output.addProperty("scope","Six96/97 equal-input sequence modes;all outputs consumed.300000 warmup/four200000-call rounds,large-text20000 warmup/round. Small English/Russian alternation,fresh actions,nested collector,oversized input. Input creation included equally. Fixed HUD context;not font/GPU/live FPS or frame-frequency measurement.");
        Files.writeString(Path.of(args[0]),new GsonBuilder().setPrettyPrinting().create().toJson(output));
    }
}
