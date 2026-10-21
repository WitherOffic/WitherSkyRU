import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Isolated receipt only. New translations do unequal work before/after, not an FPS comparison. */
public final class BenchmarkDungeon98 {
    private static volatile int sink;
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String plain(Component c){var b=new StringBuilder();net.minecraft.util.StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static Component queued(Component c){return SkyRuClient.npcChat(new GuiMessage(0,c,null,GuiMessageSource.SYSTEM_SERVER,null)).content();}
    private static void batch(List<Component> rows){for(var row:rows)sink^=queued(row).getString().length();}
    private static double median(double[] values){var c=values.clone();Arrays.sort(c);return(c[1]+c[2])/2;}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[1]));db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);flag("npcDialogues",new NpcDialogueTranslator());flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("winterDialogues",NpcDialogueTranslator.winterDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());flag("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());flag("chatNotifications",new ChatNotificationTranslator(db));
        var inputs=new LinkedHashMap<String,List<String>>();
        inputs.put("existing_boss_flat",List.of("[BOSS] Goldor: Who dares trespass into my domain?","[BOSS] The Watcher: You have proven yourself. You may pass."));
        inputs.put("existing_boss_color",List.of("§4[BOSS] Goldor§c: Who dares trespass into my domain?","§c[BOSS] The Watcher§f: You have proven yourself. You may pass."));
        inputs.put("unknown_boss",List.of("[BOSS] Bonzo: Unpublished future sentence98.","§c[BOSS] The Watcher§f: Unpublished future sentence98."));
        inputs.put("new_boss_color_unequal_work",List.of("§4[BOSS] Maxor§c: WELL WELL WELL LOOK WHO'S HERE!","§c[BOSS] The Watcher§f: I have been watching you closely...","§4[BOSS] §cSadan§f: You've managed to kill a Giant! Good luck with the rest of them!"));
        boolean expansion=BenchmarkDungeon98.class.getResource("/data/witherskyru/dungeon-expansion98.json")!=null;
        for(var e:inputs.entrySet())for(var s:e.getValue()){
            var original=Component.literal(s);var result=queued(original);boolean translated=!plain(result).equals(plain(original));
            boolean expected=e.getKey().startsWith("existing_")||e.getKey().startsWith("new_")&&expansion;
            if(translated!=expected)throw new AssertionError(e.getKey()+": "+plain(result));
        }
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().threadId();var out=new JsonObject();
        for(var e:inputs.entrySet()){
            var rows=e.getValue().stream().map(s->(Component)Component.literal(s)).toList();for(int n=0;n<300000/rows.size();n++)batch(rows);
            double[] wall=new double[4],bytes=new double[4];int count=200000/rows.size();double operations=(double)count*rows.size();
            for(int round=0;round<4;round++){long allocation=bean.getThreadAllocatedBytes(thread),start=System.nanoTime();for(int n=0;n<count;n++)batch(rows);wall[round]=(System.nanoTime()-start)/operations;bytes[round]=(bean.getThreadAllocatedBytes(thread)-allocation)/operations;}
            var r=new JsonObject();r.addProperty("ns_per_message",median(wall));r.addProperty("bytes_per_message",median(bytes));r.addProperty("equal_useful_output",!e.getKey().startsWith("new_"));r.add("rounds_ns",new Gson().toJsonTree(wall));r.add("rounds_bytes",new Gson().toJsonTree(bytes));out.add(e.getKey(),r);
        }
        out.addProperty("scope","Four isolated receipt modes;three equal-work,one new unequal-work;300000 warmup/four200000-call rounds. Not FPS,frame times or zero-cost proof.");Files.writeString(Path.of(args[0]),new GsonBuilder().setPrettyPrinting().create().toJson(out));
    }
}
