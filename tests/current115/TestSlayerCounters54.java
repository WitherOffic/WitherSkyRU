import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import ru.wither.skyru.*;

/** Counter variants built around the independently reviewed16 observed messages; not live captures. */
public final class TestSlayerCounters54 {
    private static int checks;
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String plain(Component c){return AuditMiningStyles.plain(c.getVisualOrderText());}
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();c.getVisualOrderText().accept((i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateGameChat",true);flag("translateNpcChat",true);flag("chatNotifications",new ChatNotificationTranslator(db));
        var oracle=TestSlayerScreenshot53.class.getDeclaredMethod("expected",String.class);oracle.setAccessible(true);var row=TestSlayerScreenshot53.class.getDeclaredMethod("row",String.class,int.class,int.class);row.setAccessible(true);
        var records=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("SLAYER_OBSERVED_0.15.53.json"))).getAsJsonObject().getAsJsonArray("records");int forms=0;
        for(var record:records)for(String suffix:List.of(" (x1)"," (x27)"," (×999999)"))for(int mode=0;mode<2;mode++)for(int seed=0;seed<5;seed++){
            forms++;String raw=record.getAsJsonObject().get("source_legacy").getAsString().stripTrailing();var original=(Component)row.invoke(null,raw,mode,seed);String baseTarget=((String)oracle.invoke(null,raw)).stripTrailing();
            var c=Component.empty().append(original);for(int i=0;i<suffix.length();i++)c.append(Component.literal(suffix.substring(i,i+1)).setStyle(Style.EMPTY.withColor(0x100000+i*0x101).withInsertion("counter "+seed+"/"+i).withHoverEvent(new HoverEvent.ShowText(Component.literal("original counter hint")))));
            var m=new GuiMessage(8,c,null,GuiMessageSource.SYSTEM_SERVER,null);var result=SkyRuClient.npcChat(m);check(plain(result.content()).equals(baseTarget+suffix),"counter result => "+plain(result.content()));check(plain(c).endsWith(suffix),"original counter immutable");
            var old=styles(c);var next=styles(result.content());for(int i=0;i<suffix.length();i++)check(old.get(old.size()-suffix.length()+i).equals(next.get(next.size()-suffix.length()+i)),"counter per-glyph style/action");
            for(int n=0;n<5;n++)check(SkyRuClient.npcChat(new GuiMessage(8,result.content(),null,GuiMessageSource.SYSTEM_SERVER,null)).content()==result.content(),"repeat identity");
            var player=new GuiMessage(8,c,null,GuiMessageSource.PLAYER,null);check(SkyRuClient.npcChat(player)==player,"PLAYER original");var signed=new GuiMessage(8,c,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed original");
            String visible=plain(original);boolean mod=visible.contains("[SkyHanni]")||visible.contains("[Skyblocker]");var client=new GuiMessage(8,c,null,GuiMessageSource.SYSTEM_CLIENT,null);if(mod)check(plain(SkyRuClient.npcChat(client).content()).equals(baseTarget+suffix),"closed mod local channel");else check(SkyRuClient.npcChat(client)==client,"game/NPC server channel only");
        }check(forms==480,"480 counter/style/frame combinations");
        for(var record:records){String raw=record.getAsJsonObject().get("source_legacy").getAsString().replaceAll("(?i)§[0-9a-fk-or]","").stripTrailing();String body=raw.substring(11).stripLeading();
            for(String bad:List.of(" (x1000000)"," (x-1)"," (x1.2)"," (x5) UNKNOWN")){var c=Component.literal(raw+bad);var m=new GuiMessage(0,c,null,GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(m)==m,"invalid counter original");}
            for(String key:List.of(body,body+" (x27)")){var dir=root.resolve("override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой текст\n");var own=new OfflineDictionary(dir);flag("dictionary",own);flag("chatNotifications",new ChatNotificationTranslator(own));var c=Component.literal(raw+" (x27)");var m=new GuiMessage(0,c,null,GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(m)==m,"full/core override yields");}
            flag("dictionary",db);flag("chatNotifications",new ChatNotificationTranslator(db));
        }
        flag("enabled",false);var c=Component.literal("SLAYER QUEST COMPLETE! (x5)");var m=new GuiMessage(0,c,null,GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(m)==m,"disabled identity");
        System.out.println("PASS: Slayer counters54 "+checks+" assertions;480 synthetic counter/style/frame cases around16 source-backed messages;invalid/channel/override/idempotence guards;not actual counter capture/all quests");
    }
}
