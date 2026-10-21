import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;

/** Ten fixed complete dungeon tooltip inputs; previous version does less translation. Not FPS. */
public final class BenchmarkDungeons68 {
    private static volatile int consumed;
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static void batch(List<List<Component>> cards,boolean sequence){int out=0;for(var card:cards){if(sequence){for(var row:TooltipTranslation.styled(card.stream().map(Component::getVisualOrderText).toList(),false)){int[] count={0};row.accept((i,s,p)->{count[0]++;return true;});out+=count[0];}}else for(var row:TooltipTranslation.components(card,false))out+=row.getString().length();}consumed=out;}
    private static double median(double[] a){var b=a.clone();Arrays.sort(b);return (b[1]+b[2])/2;}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var sources=Path.of(args[1]);var cards=new ArrayList<List<Component>>();var ids=Set.of("DUNGEON_TRAP","DUNGEONBREAKER","MACHINE_GUN_BOW","SPIRIT_LEAP","INFINITE_SPIRIT_LEAP","LESSER_ORB_OF_HEALING","TANK_DUNGEON_ABILITY_1","REVIVE_STONE");
        for(var raw:JsonParser.parseString(Files.readString(sources.resolve("DUNGEONS_PRIMARY_0.15.68.json"))).getAsJsonObject().getAsJsonArray("records")){var item=raw.getAsJsonObject();if(!ids.contains(item.get("id").getAsString()))continue;var card=new ArrayList<Component>();card.add(Component.literal(item.get("name_legacy").getAsString()));for(var s:item.getAsJsonArray("lore_legacy"))card.add(Component.literal(s.getAsString()));cards.add(card);}
        for(var raw:JsonParser.parseString(Files.readString(sources.resolve("DUNGEON_ORB_OBSERVED_0.15.68.json"))).getAsJsonObject().getAsJsonArray("records")){var item=raw.getAsJsonObject();var card=new ArrayList<Component>();card.add(Component.literal("Dungeon Orb"));for(var s:item.getAsJsonArray("source_rows"))card.add(Component.literal(s.getAsString()));cards.add(card);}
        if(cards.size()!=10)throw new AssertionError("ten complete fixed inputs");
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().threadId();var report=new JsonObject();
        for(boolean sequence:List.of(false,true)){for(int i=0;i<10000;i++)batch(cards,sequence);double[] times=new double[4],bytes=new double[4];for(int r=0;r<4;r++){long mem=bean.getThreadAllocatedBytes(thread),start=System.nanoTime();for(int i=0;i<4000;i++)batch(cards,sequence);times[r]=(System.nanoTime()-start)/4e9;bytes[r]=(bean.getThreadAllocatedBytes(thread)-mem)/4000.0;}var result=new JsonObject();result.addProperty("ms_per10_full_inputs",median(times));result.addProperty("bytes_per10_full_inputs",median(bytes));result.add("rounds_ms",new Gson().toJsonTree(times));result.add("rounds_bytes",new Gson().toJsonTree(bytes));report.add(sequence?"sequence":"component",result);}
        report.addProperty("scope","10 fixed complete inputs,10000 warm batches,4x4000 measured. CPU only. Previous runtime translates less useful text, not equal output work. All CPU/allocation rounds retained. Not FPS.");report.addProperty("runtime",SkyRuClient.class.getProtectionDomain().getCodeSource().getLocation().toString());Files.writeString(Path.of(args[2]),new GsonBuilder().setPrettyPrinting().create().toJson(report));System.out.println(report);
    }
}
