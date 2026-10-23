import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.StringDecomposer;
import ru.wither.skyru.*;

/** Screen candidates against existing exact/pattern/display/context routes before authoring. */
public final class Bulk117002Probe {
    static final Gson JSON=new GsonBuilder().disableHtmlEscaping().create();
    static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    static String plain(Component v){var b=new StringBuilder();StringDecomposer.iterateFormatted(v,Style.EMPTY,(i,s,c)->{b.appendCodePoint(c);return true;});return b.toString();}
    public static void main(String[] args)throws Exception{
        Path out=Path.of(args[0]);var config=out.resolve("baseline-config");Files.createDirectories(config);
        var d=new OfflineDictionary(config);d.setRecordUnknown(false);flag("dictionary",d);flag("enabled",true);flag("onlyHypixel",false);
        int groups=0,count=0;
        try(var reader=Files.newBufferedReader(out.resolve("probes.jsonl"),StandardCharsets.UTF_8);var writer=Files.newBufferedWriter(out.resolve("baseline.jsonl"),StandardCharsets.UTF_8)){
            for(String line;(line=reader.readLine())!=null;){
                var r=JsonParser.parseString(line).getAsJsonObject();var inputs=new ArrayList<Component>();for(var v:r.getAsJsonArray("lines"))inputs.add(Component.literal(v.getAsString()));
                var translated=TooltipTranslation.components(inputs,false);if(translated.size()!=inputs.size())throw new AssertionError("row count");
                var ids=r.getAsJsonArray("ids");for(int i=0;i<ids.size();i++)if(!ids.get(i).isJsonNull()){
                    String source=plain(inputs.get(i)).strip();
                    writer.write(JSON.toJson(Map.of("id",ids.get(i).getAsString(),"source",source,"dictionary",d.translateForSearch(source),"component",plain(SkyRuClient.component(inputs.get(i))).strip(),"tooltip",plain(translated.get(i)).strip())));writer.newLine();count++;
                }groups++;
            }
        }System.out.println("SCREENED groups="+groups+" unique sources="+count);
    }
}
