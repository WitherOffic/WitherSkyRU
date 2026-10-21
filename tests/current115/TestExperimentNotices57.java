import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Exact source implementation examples plus explicitly synthetic variants. */
public final class TestExperimentNotices57{
    private static int checks;private static Method row,plain,numeric,styles;
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String text(Component c)throws Exception{return(String)plain.invoke(null,c);}
    private static String clean(String s){return s.replaceAll("(?i)§[0-9a-fk-or]","");}
    private static String expected(String source){String s=clean(source);if(s.startsWith("You claimed the "))return "Вы получили награды за "+s.substring(16).replace(" rewards!","!").replace("Superpairs","Суперпары").replace("Chronomatron","Хрономатрон").replace("Ultrasequencer","Ультрасеквенсор");if(s.startsWith("☕"))return s.replace("You bought a bonus charge for the Experimentation Table!","Вы купили дополнительную попытку для стола экспериментов!");if(s.startsWith("+"))return s.replace("Enchanting Exp","опыта зачарования");return s.replace("ULTRA-RARE BOOK!","ОЧЕНЬ РЕДКАЯ КНИГА!");}
    private static void test(String raw,ChatNotificationTranslator notices)throws Exception{
        String target=expected(raw);for(int mode=0;mode<4;mode++)for(int seed=0;seed<5;seed++)for(String suffix:List.of(""," (x27)")){
            var original=(Component)row.invoke(null,"[05:12:34] "+raw+suffix,mode,seed);var message=new GuiMessage(0,original,null,GuiMessageSource.SYSTEM_SERVER,null);var result=SkyRuClient.npcChat(message);
            for(int n=0;n<5;n++){check(text(result.content()).equals("[05:12:34] "+target+suffix),raw+" mode"+mode+" => "+text(result.content()));check(numeric.invoke(null,List.of(original)).equals(numeric.invoke(null,List.of(result.content()))),"timestamp/value/counter glyph actions");result=SkyRuClient.npcChat(result);}
            check(text(original).equals("[05:12:34] "+clean(raw)+suffix),"source immutable");
            for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var other=new GuiMessage(0,original,null,channel,null);check(SkyRuClient.npcChat(other)==other,"ordinary/client message stays original");}
            var signed=new GuiMessage(0,original,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed");
            if(clean(raw).contains("ULTRA-RARE BOOK!")){int at=text(original).indexOf("XX"),next=text(result.content()).indexOf("XX");var a=(List<?>)styles.invoke(null,original);var b=(List<?>)styles.invoke(null,result.content());check(a.subList(at,at+2).equals(b.subList(next,next+2)),"obfuscated decoration unchanged");}
        }
    }
    public static void main(String[] args)throws Exception{
        row=TestSacks55.class.getDeclaredMethod("row",String.class,int.class,int.class);row.setAccessible(true);plain=TestSacks55.class.getDeclaredMethod("plain",Component.class);plain.setAccessible(true);numeric=TestSacks55.class.getDeclaredMethod("numeric",List.class);numeric.setAccessible(true);styles=TestSacks55.class.getDeclaredMethod("styles",Component.class);styles.setAccessible(true);
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateGameChat",true);var notices=new ChatNotificationTranslator(db);flag("chatNotifications",notices);
        var capture=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("EXPERIMENT_PRIMARY_0.15.57.json"))).getAsJsonObject().getAsJsonArray("records");var examples=new LinkedHashSet<String>();
        for(var r:capture){var o=r.getAsJsonObject();if(!o.has("source_code"))continue;for(String line:o.get("source_code").getAsString().split("\n")){int at=line.indexOf("REGEX-TEST: ");if(at<0||line.contains("WRAPPED-"))continue;String s=line.substring(at+12).strip();String p=clean(s);if(p.startsWith("You claimed the ")||p.startsWith("☕ You bought a bonus charge")||p.equals("XX ULTRA-RARE BOOK! XX"))examples.add(s);}}
        check(examples.size()==5,"five exact implementation examples, not actual game capture");for(String s:examples)test(s,notices);
        for(String s:List.of("You claimed the Chronomatron rewards!","☕ You bought a bonus charge for the Experimentation Table! (2/3)","☕ You bought a bonus charge for the Experimentation Table! (3/3)","+6.5k Enchanting Exp","+42,000 Enchanting Exp"))test(s,notices);
        check(notices.experimentNotificationFamilyCount()==4,"four closed families");for(String s:List.of("☕ You bought a bonus charge for the Experimentation Table! (4/3)","You claimed the UNKNOWN rewards!","+8e8 Enchanting Exp","+10 Unknown XP","You claimed the Superpairs rewards! UNKNOWN")){var c=Component.literal(s);check(notices.translate(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"unknown/invalid notice");}
        for(String key:List.of("Experimentation Table","Superpairs","Enchanting Exp","You claimed the Superpairs rewards!")){var dir=root.resolve("custom-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой текст\n");var custom=new ChatNotificationTranslator(new OfflineDictionary(dir));String s=key.equals("Superpairs")||key.startsWith("You claimed")?"You claimed the Superpairs rewards!":key.equals("Enchanting Exp")?"+6.5k Enchanting Exp":"☕ You bought a bonus charge for the Experimentation Table! (1/3)";var c=Component.literal(s);check(custom.translate(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"override");}
        flag("translateGameChat",false);var c=Component.literal("You claimed the Superpairs rewards!");var msg=new GuiMessage(0,c,null,GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(msg)==msg,"game toggle");flag("translateGameChat",true);flag("enabled",false);check(SkyRuClient.npcChat(msg)==msg,"disabled");
        System.out.println("PASS: experiment notices57 "+checks+" assertions;5 exact mod matcher examples +5 synthetic variants/4 families,4 layouts/5 fresh actions/5 repeats/counters, original values/decorations/actions/channel/toggles/overrides;not all rewards/live capture");
    }
}
