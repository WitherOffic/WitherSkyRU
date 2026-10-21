import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Dalir announces attendance/reward: never instructs the player to fetch Starfall. */
public class TestNpcMeetingReward {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static String text(Component c){var b=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static Style at(Component c,String word){int target=text(c).indexOf(word);int[] cursor={0};Style[] found={null};StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{if(cursor[0]==target)found[0]=s;cursor[0]+=Character.charCount(p);return true;});return found[0];}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);var chat=new ChatNotificationTranslator(db);
        Style prose=Style.EMPTY.withColor(0xffffff).withInsertion("prose");Style value=Style.EMPTY.withColor(0x55ffff).withHoverEvent(new HoverEvent.ShowText(Component.literal("original attendance")));
        Style cult=Style.EMPTY.withColor(0xffff55).withClickEvent(new ClickEvent.RunCommand("/unchanged-cult"));Style reward=Style.EMPTY.withColor(0xff55ff).withHoverEvent(new HoverEvent.ShowText(Component.literal("original reward")));
        String[][] values={{"1","встрече"},{"2","встречах"},{"11","встречах"},{"21","встрече"},{"101","встрече"},{"111","встречах"},{"999999999999","встречах"}};
        for(int repeat=0;repeat<5;repeat++)for(var row:values){String number=row[0];
            var source=Component.literal("[NPC] Dalir: You’ve now attended ").setStyle(prose).append(Component.literal(number).setStyle(value)).append(Component.literal(" meetings of the ").setStyle(prose)).append(Component.literal("Cult of the Fallen Star").setStyle(cult)).append(Component.literal("! As a reward, here's some ").setStyle(prose)).append(Component.literal("Starfall").setStyle(reward)).append(Component.literal("!").setStyle(prose));
            String original=text(source),expected="[NPC] Dalir: Ты уже побывал на "+number+" "+row[1]+" Культа Падшей звезды! В награду — Звездопад!";
            var result=chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_SERVER,false);check(text(result).equals(expected),"whole clear reward "+text(result));check(text(source).equals(original),"immutable source");
            for(String[] word:new String[][]{{number,number},{"Dalir","Dalir"},{"Cult of the Fallen Star","Культа Падшей звезды"},{"Starfall","Звездопад"}})check(at(source,word[0]).equals(at(result,word[1])),"semantic style/action retained");
            check(chat.translateNpcRequest(result,GuiMessageSource.SYSTEM_SERVER,false)==result,"idempotent");check(chat.translateNpcRequest(source,GuiMessageSource.PLAYER,false)==source,"all player chat excluded");
            check(chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_CLIENT,false)==source,"client excluded");check(chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_SERVER,true)==source,"signed excluded");
            check(chat.translate(source,GuiMessageSource.SYSTEM_SERVER,false)==source,"not ordinary notification");
            var timed=Component.literal("[02:03:04] ").append(source);check(text(chat.translateNpcRequest(timed,GuiMessageSource.SYSTEM_SERVER,false)).equals("[02:03:04] "+expected),"timestamp");
        }
        for(String number:List.of("1,001","-1","1.5","1000000000000","pickaxe","Wither_101")){
            var c=Component.literal("[NPC] Dalir: You’ve now attended "+number+" meetings of the Cult of the Fallen Star! As a reward, here's some Starfall!");check(chat.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"malformed/unknown not guessed");
        }
        String phrase="[NPC] Dalir: You’ve now attended 3 meetings of the Cult of the Fallen Star! As a reward, here's some Starfall!";
        for(String line:List.of(phrase+"\tМоя реплика\n","Cult of the Fallen Star\tМой культ\n")){
            Path custom=Path.of(args[0]).resolve("meeting-custom-"+checks);Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),line);
            var override=new ChatNotificationTranslator(new OfflineDictionary(custom));var c=Component.literal(phrase);check(override.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"full/inflected term override takes priority");
        }
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("npcDialogues",new NpcDialogueTranslator());flag("chatNotifications",chat);flag("translateNpcChat",true);flag("translateGameChat",false);
        var message=new GuiMessage(123,Component.literal(phrase),null,GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(message).content().getString().contains("В награду — Звездопад!"),"NPC toggle alone");
        flag("translateNpcChat",false);flag("translateGameChat",true);check(SkyRuClient.npcChat(message)==message,"NPC disabled no fallback");flag("translateNpcChat",true);flag("enabled",false);check(SkyRuClient.npcChat(message)==message,"mod disabled");
        System.out.println("PASS: NPC meeting reward "+checks+" assertions; primary Dalir grammar, seven synthetic counts, five repeats, styles/overrides/toggles; no invented reward quantity");
    }
}
