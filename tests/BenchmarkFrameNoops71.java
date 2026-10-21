import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Same public entry points on both executable versions; no reflection in measured loops. */
public final class BenchmarkFrameNoops71 {
    private static volatile Object sink;
    private static final Runnable VENDOR_DRAW=()->sink=OverhaulTextBudget.available(net.minecraft.client.gui.TextAlignment.LEFT,20,30);
    private static final FormattedCharSequence[] NUMBERS={FormattedCharSequence.forward("1,234/25,000 (+12.5%)",Style.EMPTY),FormattedCharSequence.forward("❤ 1234/5678",Style.EMPTY),FormattedCharSequence.forward("(85.9%)",Style.EMPTY)};
    private static final FormattedCharSequence[] MIXED={NUMBERS[0],FormattedCharSequence.forward("Gems: 75",Style.EMPTY),NUMBERS[1],FormattedCharSequence.forward("Normal",Style.EMPTY),FormattedCharSequence.forward("Wait for Beth to revisit your Garden again",Style.EMPTY)};
    private static final FormattedCharSequence[] PROSE={MIXED[1],MIXED[3],MIXED[4],FormattedCharSequence.forward("Foreign unknown HUD line",Style.EMPTY)};
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static void batch(String mode){for(int n=0;n<10;n++){
        if(mode.equals("technical_scope")){try(var scope=TechnicalText.open()){sink=TechnicalText.active()?Boolean.TRUE:Boolean.FALSE;}}
        else if(mode.equals("vendor_scope")){OverhaulTextBudget.within(10,100,VENDOR_DRAW);}
        else{var rows=mode.equals("numeric_sequence")?NUMBERS:mode.equals("prose_sequence")?PROSE:MIXED;sink=SkyRuClient.sequenceOutsideHud(rows[n%rows.length]);}
    }}
    private static double median(double[] values){var sorted=values.clone();Arrays.sort(sorted);return (sorted[1]+sorted[2])/2;}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[1]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().threadId();var out=new JsonObject();
        for(String mode:List.of("numeric_sequence","mixed_sequence","prose_sequence","technical_scope","vendor_scope")){
            for(int i=0;i<20000;i++)batch(mode);double[] nanos=new double[4],bytes=new double[4];
            for(int r=0;r<4;r++){long memory=bean.getThreadAllocatedBytes(thread),start=System.nanoTime();for(int i=0;i<10000;i++)batch(mode);nanos[r]=(System.nanoTime()-start)/100000.0;bytes[r]=(bean.getThreadAllocatedBytes(thread)-memory)/100000.0;}
            var row=new JsonObject();row.addProperty("ns_per_operation",median(nanos));row.addProperty("bytes_per_operation",median(bytes));row.add("rounds_ns",new Gson().toJsonTree(nanos));row.add("rounds_bytes",new Gson().toJsonTree(bytes));out.add(mode,row);
        }
        out.addProperty("scope","Microbenchmark of real sequenceOutsideHud API and TechnicalText scopes. 20k warm batches; 4x10k measured batches of 10 calls. Same inputs/output semantics. Not game FPS, draw-count frequency, 0.1% lows or shader/GPU time.");
        Files.writeString(Path.of(args[0]),new GsonBuilder().setPrettyPrinting().create().toJson(out));System.out.println(out);
    }
}
