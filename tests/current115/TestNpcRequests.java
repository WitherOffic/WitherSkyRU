import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Source-backed Pablo request grammar; synthetic values do not claim live quest coverage. */
public class TestNpcRequests {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static void flag(String n,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,value);}
    private static Style at(Component c,String word){int target=c.getString().indexOf(word);int[] offset={0};Style[] out={null};StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{if(offset[0]==target)out[0]=s;offset[0]+=Character.charCount(p);return true;});return out[0];}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);var chat=new ChatNotificationTranslator(db);
        Style prefix=Style.EMPTY.withColor(0xffaa00).withInsertion("original Pablo");
        Style body=Style.EMPTY.withColor(0xffffff).withHoverEvent(new HoverEvent.ShowText(Component.literal("original request")));
        Style flower=Style.EMPTY.withColor(0x55ffff).withClickEvent(new ClickEvent.RunCommand("/unchanged"));
        String[][] forms={
            {"Are you available? I desperately need a "," today.","Ты свободен? Сегодня мне очень нужен этот цветок: ","."},
            {"Bring me that "," as soon as you can!","Принеси мне этот цветок как можно скорее: ","!"},
            {"Could you bring me an ","?","Можешь принести мне этот цветок: ","?"},
            {"I really need a "," today, do you have one you could spare?","Сегодня мне очень нужен этот цветок: ",". У тебя найдётся один для меня?"}};
        List<String> flowers=List.of("Dandelion","Poppy","Blue Orchid","Allium","Azure Bluet","Red Tulip","Orange Tulip","White Tulip","Pink Tulip","Oxeye Daisy","Sunflower","Lilac","Rose Bush","Peony");
        for(int repeat=0;repeat<5;repeat++)for(var form:forms)for(String item:flowers)for(String phone:List.of("","✆ ")){
            String translatedItem=db.translateForSearch(item);check(!translatedItem.equals(item),"known translated flower");
            String head="[NPC] Pablo: "+phone,target=head+form[2]+translatedItem+form[3];
            var source=Component.literal(head).setStyle(prefix).append(Component.literal(form[0]).setStyle(body)).append(Component.literal(item).setStyle(flower)).append(Component.literal(form[1]).setStyle(body));
            String originalText=source.getString();var result=chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_SERVER,false);
            check(result.getString().equals(target),"complete request "+result.getString());
            check(source.getString().equals(originalText),"immutable source");check(at(result,translatedItem).equals(at(source,item)),"effective flower style/action");
            check(at(result,"Pablo").equals(at(source,"Pablo")),"speaker retained");check(at(result,form[2]).equals(at(source,form[0])),"effective prose hover retained");
            check(chat.translateNpcRequest(result,GuiMessageSource.SYSTEM_SERVER,false)==result,"idempotence");
            check(chat.translateNpcRequest(source,GuiMessageSource.PLAYER,false)==source,"player excluded");
            check(chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_CLIENT,false)==source,"client chat excluded");
            check(chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_SERVER,true)==source,"signed excluded");
            check(chat.translate(source,GuiMessageSource.SYSTEM_SERVER,false)==source,"ordinary notifications do not rewrite NPC requests");
            var timed=Component.literal("[00:00:00] ").setStyle(body).append(source);check(chat.translateNpcRequest(timed,GuiMessageSource.SYSTEM_SERVER,false).getString().equals("[00:00:00] "+target),"timestamp retained");
        }
        for(String unknown:List.of("[NPC] Pablo: Bring me that Unknown Flower as soon as you can!","[NPC] Pablo: Bring me that Hyperion as soon as you can!","[NPC] Pablo: Bring me 10 Dandelion as soon as you can!","[NPC] Wither_101: Bring me that Dandelion as soon as you can!","[VIP] Player: [NPC] Pablo: Bring me that Dandelion as soon as you can!")){
            var c=Component.literal(unknown);check(chat.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"unknown never guessed");
        }
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("npcDialogues",new NpcDialogueTranslator());flag("chatNotifications",chat);flag("translateGameChat",false);flag("translateNpcChat",true);
        var content=Component.literal("[NPC] Pablo: Could you bring me a Dandelion?");var message=new GuiMessage(123,content,null,GuiMessageSource.SYSTEM_SERVER,null);
        check(SkyRuClient.npcChat(message).content().getString().equals("[NPC] Pablo: Можешь принести мне этот цветок: Одуванчик?"),"NPC toggle alone supports request");
        flag("translateNpcChat",false);flag("translateGameChat",true);check(SkyRuClient.npcChat(message)==message,"NPC disabled remains original even with notifications on");
        flag("translateNpcChat",true);var player=new GuiMessage(123,content,null,GuiMessageSource.PLAYER,null);check(SkyRuClient.npcChat(player)==player,"queue player excluded");
        check(chat.npcRequestFamilyCount()==4,"four closed grammars");
        System.out.println("PASS: NPC requests "+checks+" assertions; four primary grammars / 14 known flowers / two phone forms / five repeats / original nicknames and input");
    }
}
