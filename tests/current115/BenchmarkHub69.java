import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Same inputs in isolated JVMs; positive new translations do more useful work. Not game FPS. */
public final class BenchmarkHub69 {
    private static volatile int consumed;
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    private static double median(double[] a){var b=a.clone();Arrays.sort(b);return(b[1]+b[2])/2;}
    private static void batch(List<Component> rows,String mode){for(var c:rows){var out=mode.startsWith("world")?SkyRuClient.minionNameTag(c,mode.equals("world_player")):SkyRuClient.npcChat(new GuiMessage(0,c,null,GuiMessageSource.SYSTEM_SERVER,null)).content();consumed^=out.getString().length();}}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);
        flag("npcDialogues",new NpcDialogueTranslator());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("winterDialogues",NpcDialogueTranslator.winterDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());flag("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());flag("chatNotifications",new ChatNotificationTranslator(db));
        var inputs=new LinkedHashMap<String,List<String>>();
        inputs.put("world_unknown",List.of("Unknown label","Wither_101","Maxwell","Blacksmith","Unknown Creature","1,000 HP","Wheat","Community Center","Already Russian — Надпись","No label match"));
        inputs.put("world_player",List.of("Thaumaturgist","Maths Enjoyer","CLICK","Wither_101","Maxwell","Coal Minion 6","Blacksmith","1,000 HP","Unknown player","No label match"));
        inputs.put("world_minion",List.of("Coal Minion 6","Wheat Minion XII","Snow Minion XI","Clay Minion 10","Mithril Minion 12","Chicken Minion IV","Sheep Minion 7","Oak Minion I","Melon Minion VIII","Iron Minion 6"));
        inputs.put("world_new_roles",Collections.nCopies(2,List.of("Thaumaturgist","Maths Enjoyer","Enchantment Nerd","ENCHANTMENT GUIDE","CLICK")).stream().flatMap(List::stream).toList());
        inputs.put("npc_unknown",java.util.stream.IntStream.range(0,10).mapToObj(i->"[NPC] Unknown: Unrecorded public test "+i+".").toList());
        inputs.put("npc_existing",Collections.nCopies(10,"[NPC] Kat: You can pick it up in 29 minutes 55 seconds."));
        inputs.put("npc_new",List.of("[NPC] Maxwell: Accessories are X magical X pieces of gear.","[NPC] Maxwell: To truly harness their power, collect as many as possible and store them in your Accessory Bag!","[NPC] Maxwell: Check it out!","[NPC] Maxwell: Even better, YOU choose what stats you get!","[NPC] Maxwell: No, it's magic!","[NPC] Maxwell: On top of their existing abilities, each accessory makes your Accessory Bag more powerful!","[NPC] Mort: Don't forget your Dungeon Orb!","[NPC] Mort: Time to make friends.","[NPC] Mort: I wish you the best of luck.","[NPC] Security Clerk: The Security Hall is only for authorized personnel."));
        int warm=Integer.getInteger("witherskyru.benchmark.warmup",20000),measure=Integer.getInteger("witherskyru.benchmark.measure",4000);
        String selected=System.getProperty("witherskyru.benchmark.scenario","");var report=new JsonObject();report.addProperty("version",args[1]);report.addProperty("scope","7 isolated text API scenarios;per10 inputs. New roles/NPC positive rows perform more useful output translation than68. CPU elapsed loop timings, no world/font/GPU/FPS. Allocation rounds retained.");report.addProperty("warmup_batches",warm);report.addProperty("measured_batches",measure);
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();boolean memory=bean.isThreadAllocatedMemorySupported();if(memory&&!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);long tid=Thread.currentThread().threadId();
        for(var entry:inputs.entrySet()){String mode=entry.getKey();if(!selected.isEmpty()&&!selected.equals(mode))continue;var rows=entry.getValue().stream().map(s->(Component)Component.literal(s).setStyle(Style.EMPTY.withColor(0xffffff))).toList();
            for(int i=0;i<warm;i++)batch(rows,mode);double[] times=new double[4],bytes=new double[4];
            for(int round=0;round<4;round++){long startBytes=memory?bean.getThreadAllocatedBytes(tid):0,start=System.nanoTime();for(int i=0;i<measure;i++)batch(rows,mode);times[round]=(System.nanoTime()-start)/(measure*1e6);bytes[round]=memory?(bean.getThreadAllocatedBytes(tid)-startBytes)/(double)measure:-1;}
            var r=new JsonObject();r.addProperty("ms_per10",median(times));r.addProperty("bytes_per10",median(bytes));r.add("rounds_ms",new Gson().toJsonTree(times));r.add("rounds_bytes",new Gson().toJsonTree(bytes));report.add(mode,r);System.out.printf(Locale.ROOT,"HUB69 %s %s: %.6fms %.1fbytes per10;not FPS%n",args[1],mode,median(times),median(bytes));
        }
        Files.writeString(Path.of(args[2]),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
    }
}
