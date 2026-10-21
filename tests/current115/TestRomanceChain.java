import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Complete reviewed public dialogue tables, independent of runtime resource enumeration. */
public class TestRomanceChain {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xffffff);
    private static final Style OLD=Style.EMPTY.withColor(0x5555ff).withClickEvent(new ClickEvent.RunCommand("/original-old"));
    private static final Style NEXT=Style.EMPTY.withColor(0x55ffff).withInsertion("original reward");
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    private static void flag(String k,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(k);f.setAccessible(true);f.set(null,v);}
    private static GuiMessage message(Component c,GuiMessageSource channel){return new GuiMessage(0,c,null,channel,null);}
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static void span(Component c,String value,Style wanted){int at=c.getString().indexOf(value);check(at>=0,"missing "+value);var ss=styles(c);for(int i=at;i<at+value.length();i++)check(ss.get(i).equals(wanted),"original reward style/action");}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);
        flag("npcDialogues",new NpcDialogueTranslator());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("chatNotifications",new ChatNotificationTranslator(db));
        var audit=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("ROMANCE_SOURCE_AUDIT_0.15.41.json"))).getAsJsonObject();int translated=0,punctuation=0;
        for(var raw:audit.getAsJsonArray("records")){
            var row=raw.getAsJsonObject();String source=row.get("source").getAsString().replace("<player>","Wither_101").replace("<Player>","Wither_101");var c=Component.literal("[01:02:03] "+source).setStyle(BODY);var m=message(c,GuiMessageSource.SYSTEM_SERVER);var result=SkyRuClient.npcChat(m).content();
            if(row.get("classification").getAsString().equals("punctuation_unchanged")){check(result==c,"punctuation unchanged, not fake new translation");punctuation++;}
            else {check(!result.getString().equals(c.getString()),"public dialogue still unsupported: "+source);translated++;}
            check(result.getString().startsWith("[01:02:03] [NPC] "+row.get("speaker").getAsString()+": "),"speaker/time intact");
            for(int n=0;n<5;n++)check(SkyRuClient.npcChat(message(result,GuiMessageSource.SYSTEM_SERVER)).content()==result,"idempotent whole dialogue");
            for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var p=message(c,channel);check(SkyRuClient.npcChat(p)==p,"whole player/client chat protected");}
        }
        check(translated==187&&punctuation==2,"189 public dialogue shapes: 187 translated incl three nick families, two safe punctuation forms");
        int upgrades=0;
        for(var raw:audit.getAsJsonArray("upgrade_notices")){
            String source=raw.getAsJsonObject().get("source").getAsString();int a=source.indexOf(" was upgraded to ");String old=source.substring(5,a),next=source.substring(a+17,source.length()-1);
            for(String suffix:List.of(""," (x17)"," (×2)")){
                var c=Component.empty().append(Component.literal("[01:02:03] Your ").setStyle(BODY)).append(Component.literal(old).setStyle(OLD)).append(Component.literal(" was upgraded to ").setStyle(BODY)).append(Component.literal(next).setStyle(NEXT)).append(Component.literal("!"+suffix).setStyle(BODY));
                String target="[01:02:03] Ваш предмет «"+db.translateForSearch(old)+"» улучшен до «"+db.translateForSearch(next)+"»!"+suffix;var result=SkyRuClient.npcChat(message(c,GuiMessageSource.SYSTEM_SERVER)).content();check(result.getString().equals(target),"actual ring upgrade "+source+" => "+result.getString());span(result,db.translateForSearch(old),OLD);span(result,db.translateForSearch(next),NEXT);
                for(int n=0;n<5;n++)check(SkyRuClient.npcChat(message(result,GuiMessageSource.SYSTEM_SERVER)).content()==result,"ring repeat");
                for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var p=message(c,channel);check(SkyRuClient.npcChat(p)==p,"reward does not enable ordinary chat translation");}
                upgrades++;
            }
        }
        check(upgrades==21,"seven known transitions with three counter variants");
        for(String source:List.of("Your Shiny Yellow Rock was upgraded to Exquisite Ring of Love!","Your Wither_101 was upgraded to Yellow Rock of Love!","[NPC] Romero: Unknown future dialogue.")){var m=message(Component.literal(source),GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(m)==m,"unknown/wrong reward transition left original");}
        var ownPath=Path.of(args[0]).resolve("custom");Files.createDirectories(ownPath);Files.writeString(ownPath.resolve("overrides.tsv"),"Shiny Yellow Rock\tМой камень\n");var own=new OfflineDictionary(ownPath);var c=Component.literal("Your Shiny Yellow Rock was upgraded to Yellow Rock of Love!");check(new ChatNotificationTranslator(own).translate(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"ring-name override");
        System.out.println("PASS: romance chain "+checks+" assertions; 189 public shapes, seven known reward transitions/21 synthetic counters; not full server or live-tested");
    }
}
