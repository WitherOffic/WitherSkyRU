import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Queue text on receipt;not frame,world,font,GPU or FPS. New positives do extra useful work. */
public final class BenchmarkNpc70 {
    private static volatile int consumed;
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static double median(double[] a){var b=a.clone();Arrays.sort(b);return(b[1]+b[2])/2;}
    private static void batch(List<Component> rows){for(var c:rows)consumed^=SkyRuClient.npcChat(new GuiMessage(0,c,null,GuiMessageSource.SYSTEM_SERVER,null)).content().getString().length();}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);flag("npcDialogues",new NpcDialogueTranslator());flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("winterDialogues",NpcDialogueTranslator.winterDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());flag("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());flag("chatNotifications",new ChatNotificationTranslator(db));
        var inputs=Map.of("unknown",Collections.nCopies(10,"[NPC] Unknown: An unrecorded public test dialogue."),"existing",Collections.nCopies(10,"[NPC] Kat: You can pick it up in 29 minutes 55 seconds."),"named",Collections.nCopies(5,List.of("[NPC] Maxwell: power, that's an excellent power!","[NPC] Maxwell: A wise choice, Player_70!")).stream().flatMap(List::stream).toList(),"duration",Collections.nCopies(5,List.of("[NPC] Kat: You can pick it up in 1 day 0 hours 1 minute 2 seconds.","[NPC] Kat: Come back in 2 days to pick it up!")).stream().flatMap(List::stream).toList(),"contest",Collections.nCopies(5,List.of("[NPC] Jacob: You placed in the SILVER bracket in the Carrot Contest!","[NPC] Jacob: PERSONAL BEST! You've surpassed your previous record of 73,468 items collected in the Carrot Contest!")).stream().flatMap(List::stream).toList());
        int warm=Integer.getInteger("witherskyru.benchmark.warmup",20000),measure=Integer.getInteger("witherskyru.benchmark.measure",4000);String selected=System.getProperty("witherskyru.benchmark.scenario","");var report=new JsonObject();report.addProperty("version",args[1]);report.addProperty("scope","Same inputs,isolated fresh JVM;unknown/existing same useful output;named/duration/contest produce additional reviewed output in70. Receipt only;not FPS.");
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();boolean alloc=bean.isThreadAllocatedMemorySupported();if(alloc&&!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);long tid=Thread.currentThread().threadId();
        for(String mode:List.of("unknown","existing","named","duration","contest")){if(!selected.isEmpty()&&!selected.equals(mode))continue;var rows=inputs.get(mode).stream().map(s->(Component)Component.literal(s)).toList();for(int i=0;i<warm;i++)batch(rows);double[] times=new double[4],bytes=new double[4];for(int r=0;r<4;r++){long mem=alloc?bean.getThreadAllocatedBytes(tid):0,start=System.nanoTime();for(int i=0;i<measure;i++)batch(rows);times[r]=(System.nanoTime()-start)/(measure*1e6);bytes[r]=alloc?(bean.getThreadAllocatedBytes(tid)-mem)/((double)measure):-1;}var row=new JsonObject();row.addProperty("ms_per10",median(times));row.addProperty("bytes_per10",median(bytes));row.add("rounds_ms",new Gson().toJsonTree(times));row.add("rounds_bytes",new Gson().toJsonTree(bytes));report.add(mode,row);System.out.printf(Locale.ROOT,"NPC70 %s %s %.6fms %.0fbytes per10;not FPS%n",args[1],mode,median(times),median(bytes));}
        Files.writeString(Path.of(args[2]),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
    }
}
