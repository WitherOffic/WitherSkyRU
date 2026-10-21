import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Closed, source-backed NPC clauses: synthetic numbers are not live dialogue coverage. */
public class TestNpcDynamicInstructions {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xffffff).withHoverEvent(new HoverEvent.ShowText(Component.literal("original body")));
    private static final Style TERM=Style.EMPTY.withColor(0xffaa00).withClickEvent(new ClickEvent.RunCommand("/original-term"));
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String text(Component c){var b=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static void retained(Component before,Component after,String from,String to){
        int old=text(before).indexOf(from),now=text(after).indexOf(to);check(old>=0&&now>=0,"semantic word present");
        var a=styles(before);var b=styles(after);for(int i=0;i<to.length();i++)check(b.get(now+i).equals(a.get(old+(from.equals(to)?i:0))),"effective semantic styles/actions");
    }
    private static Component digits(String number){var c=Component.empty();for(int i=0;i<number.length();i++)c.append(Component.literal(number.substring(i,i+1)).setStyle(Style.EMPTY.withColor(0x220000+i*0x1000).withInsertion("number char "+i)));return c;}
    private static void result(ChatNotificationTranslator chat,Component source,String expected,String... unchanged){
        String old=text(source);var translated=chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_SERVER,false);
        check(text(translated).equals(expected),"full NPC result: "+text(translated));check(text(source).equals(old),"source immutable");
        for(String word:unchanged)retained(source,translated,word,word);
        for(int repeat=0;repeat<5;repeat++)check(chat.translateNpcRequest(translated,GuiMessageSource.SYSTEM_SERVER,false)==translated,"idempotence");
        check(chat.translateNpcRequest(source,GuiMessageSource.PLAYER,false)==source,"all player messages excluded");
        check(chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_CLIENT,false)==source,"client excluded");
        check(chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_SERVER,true)==source,"signed excluded");
        check(chat.translate(source,GuiMessageSource.SYSTEM_SERVER,false)==source,"NPC not ordinary notifications");
        var timed=Component.literal("[01:02:03] ").setStyle(TERM).append(source);
        check(text(chat.translateNpcRequest(timed,GuiMessageSource.SYSTEM_SERVER,false)).equals("[01:02:03] "+expected),"timestamp");
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);var chat=new ChatNotificationTranslator(db);
        String[] numbers={"0","1","2","4","5","11","12","14","21","22","25","101","1,001","9,999","42,602","999,999,999,999","999999999999"};
        String[] nouns={"предметов","предмет","предмета","предмета","предметов","предметов","предметов","предметов","предмет","предмета","предметов","предмет","предмет","предметов","предмета","предметов","предметов"};
        for(int k=0;k<numbers.length;k++)for(String record:List.of(""," PERSONAL BEST!")){
            String number=numbers[k];var source=Component.literal("[NPC] Jacob: ").setStyle(TERM).append(Component.literal("You collected ").setStyle(BODY)).append(digits(number)).append(Component.literal(" items!"+record).setStyle(BODY));
            result(chat,source,"[NPC] Jacob: Ты собрал "+number+" "+nouns[k]+"!"+(record.isEmpty()?"":" ЛИЧНЫЙ РЕКОРД!"),"Jacob",number);
            retained(source,chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_SERVER,false),"You collected","Ты собрал");
        }
        for(String number:numbers){var source=Component.literal("[NPC] Jacob: Your previous Personal Best was ").setStyle(BODY).append(digits(number)).append(Component.literal(".").setStyle(BODY));
            result(chat,source,"[NPC] Jacob: Твой предыдущий личный рекорд — "+number+".","Jacob",number);
        }
        for(String crop:List.of("Wheat","Carrot","Potato","Nether Wart","Sugar Cane","Mushroom","Cocoa Beans","Cactus","Pumpkin","Melon"))for(String number:List.of("+0","+10","+12.5")){
            var source=Component.literal("[NPC] Jacob: Your ").setStyle(BODY).append(Component.literal("Personal Bests").setStyle(TERM)).append(Component.literal(" perk is now granting you ").setStyle(BODY)).append(digits(number)).append(Component.literal(" "+crop+" Fortune").setStyle(TERM)).append(Component.literal("!").setStyle(BODY));
            String ru=db.translateForSearch(crop);var translated=chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_SERVER,false);
            result(chat,source,"[NPC] Jacob: Бонус «Личные рекорды» теперь даёт "+number+" к удаче при сборе культуры «"+ru+"»!","Jacob",number);
            retained(source,translated,crop,ru);retained(source,translated,"Personal Bests","Личные рекорды");
        }
        var legacy=Component.literal("§e[NPC] Jacob§f: §rYour §6Personal Bests §fperk is now granting you §6+10 Wheat Fortune§f!");
        result(chat,legacy,"[NPC] Jacob: Бонус «Личные рекорды» теперь даёт +10 к удаче при сборе культуры «Пшеница»!","Jacob","+10");
        var translated=chat.translateNpcRequest(legacy,GuiMessageSource.SYSTEM_SERVER,false);retained(legacy,translated,"Wheat","Пшеница");
        for(String value:List.of("1,,001","1,01",",10","-1","1.5","1000000000000","1,000,000,000,000","10 pickaxe","Wither_101")){
            var c=Component.literal("[NPC] Jacob: You collected "+value+" items! PERSONAL BEST!");check(chat.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"malformed count unchanged");
        }
        for(String value:List.of("[NPC] Jacob: Your Personal Bests perk is now granting you +10 Unknown Crop Fortune!","[NPC] Jacob: Your Personal Bests perk is now granting you +Infinity Wheat Fortune!","[NPC] Wither_101: You collected 12 items!","[VIP] Player: [NPC] Jacob: You collected 12 items!","pickaxe","Wither_101")){
            var c=Component.literal(value);check(chat.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"unknown/player prose unchanged");
        }
        var mixed=Component.literal("[NPC] Jacob: You ").setStyle(BODY).append(Component.literal("collected 3 items!").setStyle(TERM));check(chat.translateNpcRequest(mixed,GuiMessageSource.SYSTEM_SERVER,false)==mixed,"unexpected mixed prose fails closed");
        Path custom=Path.of(args[0]).resolve("dynamic-custom");Files.createDirectories(custom);String phrase="[NPC] Jacob: You collected 3 items!";Files.writeString(custom.resolve("overrides.tsv"),phrase+"\tМой перевод\n");
        var override=new ChatNotificationTranslator(new OfflineDictionary(custom));var c=Component.literal(phrase);check(override.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"full user override not displaced by closed template");
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("npcDialogues",new NpcDialogueTranslator());flag("chatNotifications",chat);flag("translateGameChat",false);flag("translateNpcChat",true);
        var message=new GuiMessage(123,Component.literal(phrase),null,GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(message).content().getString().equals("[NPC] Jacob: Ты собрал 3 предмета!"),"queue NPC toggle alone");
        flag("translateNpcChat",false);flag("translateGameChat",true);check(SkyRuClient.npcChat(message)==message,"NPC disabled no fallback");flag("translateNpcChat",true);flag("enabled",false);check(SkyRuClient.npcChat(message)==message,"mod disabled");
        check(chat.npcTemplateFamilyCount()==19,"four requests, three Jacob grammars, Trevor, Dalir, six forest-result grammars, Lucius purchase total, three Sirius auction shapes");
        System.out.println("PASS: dynamic NPC instructions "+checks+" assertions; Jacob counters, ten crops, legacy colours, semantic events, malformed values, whole-player-chat protection");
    }
}
