import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;
import ru.wither.skyru.*;

/** Offline diagnostic only: changes are not a proof of editorial correctness. */
public final class Coverage117 {
    static final Gson JSON=new GsonBuilder().disableHtmlEscaping().create();
    static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    static String plain(Component value){var b=new StringBuilder();StringDecomposer.iterateFormatted(value,Style.EMPTY,(i,s,c)->{b.appendCodePoint(c);return true;});return b.toString();}
    static Object field(Object value,String name)throws Exception{var f=value.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(value);}
    public static void main(String[] args)throws Exception{
        Path out=Path.of(args[0]), config=Path.of(args[1]);Files.createDirectories(config);
        var dictionary=new OfflineDictionary(config);dictionary.setRecordUnknown(false);
        flag("dictionary",dictionary);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);
        var providers=new LinkedHashMap<String,NpcDialogueTranslator>();
        providers.put("npcDialogues",new NpcDialogueTranslator());providers.put("gardenDialogues",NpcDialogueTranslator.gardenDialogues());
        providers.put("questDialogues",NpcDialogueTranslator.questDialogues());providers.put("storyDialogues",NpcDialogueTranslator.storyDialogues());
        providers.put("winterDialogues",NpcDialogueTranslator.winterDialogues());providers.put("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());
        providers.put("questObjectives",NpcDialogueTranslator.questObjectives());providers.put("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());
        for(var e:providers.entrySet())flag(e.getKey(),e.getValue());flag("chatNotifications",new ChatNotificationTranslator(dictionary));
        try(var w=Files.newBufferedWriter(out.resolve("runtime-inventory.jsonl"),StandardCharsets.UTF_8)){
            var exact=(Map<?,?>)field(dictionary,"exact");
            for(var e:new TreeMap<>(exact).entrySet()){w.write(JSON.toJson(Map.of("source",e.getKey(),"translation",e.getValue(),"provider","OfflineDictionary.exact")));w.newLine();}
            for(var p:providers.entrySet())for(var e:new TreeMap<>((Map<String,?>)field(p.getValue(),"entries")).entrySet()){
                var method=e.getValue().getClass().getDeclaredMethod("target");method.setAccessible(true);
                w.write(JSON.toJson(Map.of("source",e.getKey(),"translation",method.invoke(e.getValue()),"provider",p.getKey())));w.newLine();
            }
        }
        try(var w=Files.newBufferedWriter(out.resolve("runtime-patterns.jsonl"),StandardCharsets.UTF_8)){
            for(Object rule:(List<?>)field(dictionary,"rules")){
                w.write(JSON.toJson(Map.of("pattern",field(rule,"pattern").toString(),"replacement",field(rule,"replacement"),"provider","OfflineDictionary.rules")));w.newLine();
            }
        }
        int groups=0,count=0;
        try(var reader=Files.newBufferedReader(out.resolve("probes.jsonl"),StandardCharsets.UTF_8);var writer=Files.newBufferedWriter(out.resolve("runtime-probes.jsonl"),StandardCharsets.UTF_8)){
            for(String line;(line=reader.readLine())!=null;){
                var r=JsonParser.parseString(line).getAsJsonObject();String surface=r.get("surface").getAsString();
                var lines=new ArrayList<Component>();for(var text:r.getAsJsonArray("lines"))lines.add(Component.literal(text.getAsString()));
                List<Component> translated;
                if(surface.equals("tooltip"))translated=TooltipTranslation.components(lines,false);
                else if(surface.equals("chat"))translated=List.of(SkyRuClient.npcChat(new GuiMessage(0,lines.getFirst(),null,GuiMessageSource.SYSTEM_SERVER,null)).content());
                else translated=List.of(SkyRuClient.component(lines.getFirst()));
                if(translated.size()!=lines.size())throw new AssertionError("tooltip row count");
                var ids=r.getAsJsonArray("ids");for(int n=0;n<ids.size();n++)if(!ids.get(n).isJsonNull()){
                    writer.write(JSON.toJson(Map.of("id",ids.get(n).getAsString(),"surface",surface,"output",plain(translated.get(n)))));writer.newLine();count++;
                }
                groups++;if(groups%5000==0)System.out.println("groups="+groups+" rows="+count);
            }
        }
        System.out.println("DONE groups="+groups+" rows="+count+" exact="+dictionary.phraseCount()+" regex="+dictionary.patternCount());
    }
}
