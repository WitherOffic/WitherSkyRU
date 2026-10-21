import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;

/** Offline chat work only. Baseline leaves new quiz untranslated, so positive useful work differs. */
public final class BenchmarkSafetyQuiz66 {
    private static volatile int sink;
    private static void batch(ChatNotificationTranslator translator,List<Component> rows){int n=0;for(var row:rows)n+=translator.translate(row,GuiMessageSource.SYSTEM_SERVER,false).getString().length();sink=n;}
    private static double median(double[] values){var sorted=values.clone();Arrays.sort(sorted);return (sorted[1]+sorted[2])/2;}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);var translator=new ChatNotificationTranslator(db);
        var captured=JsonParser.parseString(Files.readString(Path.of(args[1]))).getAsJsonObject().getAsJsonArray("messages");var positives=new ArrayList<Component>();
        for(var raw:captured){String text=raw.getAsJsonObject().get("normalized_newlines").getAsString();if(!text.contains("[NPC]")&&!text.contains("RING..."))positives.add(Component.literal(text).setStyle(Style.EMPTY.withInsertion("fresh benchmark caller")));}
        if(positives.size()!=13)throw new AssertionError("All13 captured quiz messages required");
        var negatives=new ArrayList<Component>();for(int i=0;i<13;i++)negatives.add(Component.literal("[01:02:03] Question #"+(20+i)+" unknown tutorial content").setStyle(Style.EMPTY.withInsertion("unknown caller")));
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().threadId();var report=new JsonObject();
        for(boolean positive:List.of(true,false)){var rows=positive?positives:negatives;for(int i=0;i<10000;i++)batch(translator,rows);double[] times=new double[4],bytes=new double[4];for(int r=0;r<4;r++){long memory=bean.getThreadAllocatedBytes(thread),start=System.nanoTime();for(int i=0;i<4000;i++)batch(translator,rows);times[r]=(System.nanoTime()-start)/4e9;bytes[r]=(bean.getThreadAllocatedBytes(thread)-memory)/4000.0;}var result=new JsonObject();result.addProperty("ms_per_13_messages",median(times));result.addProperty("bytes_per_13_messages",median(bytes));result.add("rounds_ms",new Gson().toJsonTree(times));result.add("rounds_bytes",new Gson().toJsonTree(bytes));report.add(positive?"positive":"negative",result);}
        report.addProperty("warning","Offline13-message CPU/allocation benchmark, not FPS. Previous version leaves positive quiz untranslated; unequal useful work. No commands executed.");report.addProperty("runtime",SkyRuClient.class.getProtectionDomain().getCodeSource().getLocation().toString());Files.writeString(Path.of(args[2]),new GsonBuilder().setPrettyPrinting().create().toJson(report));System.out.println(report);
    }
}
