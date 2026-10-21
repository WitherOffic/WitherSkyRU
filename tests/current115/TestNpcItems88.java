import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.StringDecomposer;
import ru.wither.skyru.*;

/** Authored item-name oracle, not translation output; tests existing title handling only. */
public final class TestNpcItems88 {
    private static int checks;
    private static void check(boolean x,String why){if(!x)throw new AssertionError(why);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String plain(Component c){var b=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    public static void main(String[] args)throws Exception{
        Path config=Path.of(args[0]),root=Path.of(args[1]);var db=new OfflineDictionary(config);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var names=new LinkedHashMap<String,String>();
        for(String line:Files.readAllLines(root.resolve("NPC88_ITEM_NAMES.tsv"))){if(line.isEmpty()||line.startsWith("#"))continue;var f=line.split("\t",-1);check(f.length==2&&names.put(f[0],f[1])==null,"unique independently authored pair");}
        check(names.size()==1,"one distinct name,not NPC-count inflation");
        var corpus=JsonParser.parseString(Files.readString(root.resolve("NPC_SOURCES_0.15.88.json"))).getAsJsonObject();var pinned=new HashSet<String>();
        for(var raw:corpus.getAsJsonArray("records"))pinned.add(raw.getAsJsonObject().get("source").getAsString());
        var packaged=JsonParser.parseReader(new java.io.InputStreamReader(TestNpcItems88.class.getResourceAsStream("/data/witherskyru/npc-expansion88.json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("records");
        for(var pair:names.entrySet()){
            String source=pair.getKey(),target=pair.getValue();check(db.translate(source).equals(target),"real JAR dictionary: "+source);
            String dialogue="[NPC] Elise: Congratulations on obtaining the "+source+"!";check(pinned.contains(dialogue),"published Elise item context");
            int linked=0;for(var raw:packaged){var r=raw.getAsJsonObject();if(r.get("source").getAsString().equals(dialogue)){check(r.get("translation").getAsString().contains(target),"chat/title terminology agrees");linked++;}}check(linked==1,"one exact name context");
            for(int seed=1;seed<=3;seed++){
                var style=Style.EMPTY.withColor(seed*0x234567).withItalic(seed%2==0).withBold(seed%2!=0).withInsertion("fresh88 "+seed).withShadowColor(seed).withClickEvent(new ClickEvent.SuggestCommand("/not-executed88 "+seed)).withHoverEvent(new HoverEvent.ShowText(Component.literal("hint88 "+seed)));
                var title=Component.literal(source).setStyle(style);var input=List.<Component>of(title,Component.literal("LEGENDARY"));var output=TooltipTranslation.components(input,false);
                check(plain(output.getFirst()).equals(target),"actual title handler: "+source);check(plain(title).equals(source),"source immutable");
                StringDecomposer.iterateFormatted(output.getFirst(),Style.EMPTY,(i,current,p)->{check(current.equals(style),"fresh title style/actions");return true;});
                check(plain(TooltipTranslation.components(input,true).getFirst()).equals(source),"Shift original");
            }
            var legacy=Component.literal("§6"+source);check(plain(TooltipTranslation.components(List.of(legacy),false).getFirst()).equals(target),"legacy-colored title");
            var own=config.resolve("custom88-"+names.keySet().stream().toList().indexOf(source));Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),source+"\tМоё название\n");check(new OfflineDictionary(own).translate(source).equals("Моё название"),"custom name wins");
        }
        System.out.println("PASS: NPCItems88 "+checks+" assertions;one exact item name;one source-backed Elise context;current colors,actions,hover,Shift,overrides;no entire-item-lore or FPS claim");
    }
}
