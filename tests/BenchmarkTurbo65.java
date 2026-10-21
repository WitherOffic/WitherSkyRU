import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;

/** Ten complete pinned cards; previous version leaves some text untranslated. Not FPS. */
public final class BenchmarkTurbo65 {
    private static volatile int consumed;
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    private static void batch(List<List<Component>> cards,boolean sequence){int n=0;for(var card:cards){if(sequence){for(var row:TooltipTranslation.styled(card.stream().map(Component::getVisualOrderText).toList(),false)){int[] count={0};row.accept((i,s,cp)->{count[0]++;return true;});n+=count[0];}}else for(var row:TooltipTranslation.components(card,false))n+=row.getString().length();}consumed=n;}
    private static double median(double[] values){var sorted=values.clone();Arrays.sort(sorted);return (sorted[1]+sorted[2])/2;}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var ids=Set.of("TURBO_CACTUS;1","TURBO_CANE;4","TURBO_WHEAT;7","TURBO_MELON;6","TURBO_COCO;5","TURBO_WARTS;4","TURBO_SUNFLOWER;5","TURBO_MOONFLOWER;7","TURBO_GOURD","ENCHANTED_TURBO_GOURD");
        var cards=new ArrayList<List<Component>>();for(var raw:JsonParser.parseString(Files.readString(Path.of(args[1]))).getAsJsonObject().getAsJsonArray("records")){var item=raw.getAsJsonObject();if(!ids.contains(item.get("id").getAsString()))continue;var card=new ArrayList<Component>();card.add(Component.literal(item.get("name_legacy").getAsString()).setStyle(Style.EMPTY.withInsertion("current title")));for(var row:item.getAsJsonArray("lore_legacy"))card.add(Component.literal(row.getAsString()).setStyle(Style.EMPTY.withInsertion("current lore")));cards.add(card);}
        if(cards.size()!=10)throw new AssertionError("Expected10 full captured cards");
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().threadId();var report=new JsonObject();
        for(boolean sequence:List.of(false,true)){for(int i=0;i<10000;i++)batch(cards,sequence);double[] times=new double[4],bytes=new double[4];for(int r=0;r<4;r++){long memory=bean.getThreadAllocatedBytes(thread),start=System.nanoTime();for(int i=0;i<4000;i++)batch(cards,sequence);times[r]=(System.nanoTime()-start)/4e12;bytes[r]=(bean.getThreadAllocatedBytes(thread)-memory)/4000.0;}var result=new JsonObject();result.addProperty("ms_per_10_full_tooltips",median(times)*1000);result.addProperty("bytes_per_10_full_tooltips",median(bytes));result.add("seconds_per_batch_rounds",new Gson().toJsonTree(times));report.add(sequence?"sequence":"component",result);}
        report.addProperty("warning","Synthetic offline CPU/allocation comparison, not FPS. Previous version translates less useful text; unequal output work. CPU rounds retained; allocation reports median only.");report.addProperty("runtime",SkyRuClient.class.getProtectionDomain().getCodeSource().getLocation().toString());Files.writeString(Path.of(args[2]),new GsonBuilder().setPrettyPrinting().create().toJson(report));System.out.println(report);
    }
}
