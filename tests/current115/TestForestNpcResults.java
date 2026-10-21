import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Source-backed forest NPC clauses, including actual legacy REGEX-TEST samples. */
public class TestForestNpcResults {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String text(Component c){var b=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static void retained(Component a,Component b,String source,String target){int old=text(a).indexOf(source),now=text(b).indexOf(target);check(old>=0&&now>=0,"semantic token");var x=styles(a);var y=styles(b);for(int i=0;i<target.length();i++)check(y.get(now+i).equals(x.get(old+(source.equals(target)?i:0))),"semantic style/actions");}
    private static void result(ChatNotificationTranslator chat,Component source,String target,String... tokens){String old=source.getString();var translated=chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_SERVER,false);check(text(translated).equals(target),"whole forest reply: "+text(translated));check(source.getString().equals(old),"source immutable");for(String token:tokens)retained(source,translated,token,token);
        check(chat.translateNpcRequest(translated,GuiMessageSource.SYSTEM_SERVER,false)==translated,"idempotence");check(chat.translateNpcRequest(source,GuiMessageSource.PLAYER,false)==source,"whole player chat excluded");check(chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_SERVER,true)==source,"signed excluded");check(chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_CLIENT,false)==source,"client excluded");check(chat.translate(source,GuiMessageSource.SYSTEM_SERVER,false)==source,"not ordinary notifications");
        var timed=Component.literal("[02:03:04] ").append(source);check(text(chat.translateNpcRequest(timed,GuiMessageSource.SYSTEM_SERVER,false)).equals("[02:03:04] "+target),"timestamp");
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);var chat=new ChatNotificationTranslator(db);Style body=Style.EMPTY.withColor(0xffffff).withInsertion("prose");Style value=Style.EMPTY.withColor(0x55ffff).withClickEvent(new ClickEvent.RunCommand("/unchanged-value"));Style record=Style.EMPTY.withColor(0xff55ff).withBold(true).withHoverEvent(new HoverEvent.ShowText(Component.literal("original record")));
        for(int repeat=0;repeat<5;repeat++)for(String speaker:List.of("Agatha","Miria")){
            String head="[NPC] "+speaker+": ";
            for(String bracket:List.of("COMMON","UNCOMMON","RARE","EPIC","LEGENDARY")){var source=Component.literal(head+"You reached the ").setStyle(body).append(Component.literal(bracket).setStyle(record)).append(Component.literal(" Bracket in my contest!").setStyle(body));result(chat,source,head+"Ты попал в категорию "+bracket+" в моём соревновании!",speaker,bracket);}
            for(String[] n:new String[][]{{"1","очко"},{"2","очка"},{"11","очков"},{"21","очко"},{"2,506","очков"},{"123,700","очков"}})for(boolean best:List.of(false,true)){
                var source=Component.literal(head+"You earned a total of ").setStyle(body).append(Component.literal(n[0]).setStyle(value)).append(Component.literal(" points!"+(best?" That's a new ":"")).setStyle(body));if(best)source.append(Component.literal("PERSONAL BEST").setStyle(record)).append(Component.literal("!").setStyle(body));
                result(chat,source,head+"Ты набрал "+n[0]+" "+n[1]+"!"+(best?" Это новый ЛИЧНЫЙ РЕКОРД!":""),speaker,n[0]);if(best)retained(source,chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_SERVER,false),"PERSONAL BEST","ЛИЧНЫЙ РЕКОРД");
            }
            var previous=Component.literal(head+"Your previous Personal Best was ").setStyle(body).append(Component.literal("6,487").setStyle(value)).append(Component.literal(".").setStyle(body));result(chat,previous,head+"Твой предыдущий личный рекорд — 6,487.",speaker,"6,487");
            var claim=Component.literal(head+"Come see me at ").setStyle(body).append(Component.literal("Murkwater Loch").setStyle(record)).append(Component.literal(" to claim your rewards!").setStyle(body));var claimResult=chat.translateNpcRequest(claim,GuiMessageSource.SYSTEM_SERVER,false);result(chat,claim,head+"Приходи ко мне в локацию «Тёмное озеро», чтобы забрать награды!",speaker);retained(claim,claimResult,"Murkwater Loch","Тёмное озеро");
            result(chat,Component.literal(head+"Keep it up!").setStyle(body),head+"Так держать!",speaker);
        }
        var legacy=Component.literal("§e[NPC] §bAgatha§f: §rYou earned a total of §b700 §fpoints! That's a new §d§lPERSONAL BEST§f!");result(chat,legacy,"[NPC] Agatha: Ты набрал 700 очков! Это новый ЛИЧНЫЙ РЕКОРД!","Agatha","700");retained(legacy,chat.translateNpcRequest(legacy,GuiMessageSource.SYSTEM_SERVER,false),"PERSONAL BEST","ЛИЧНЫЙ РЕКОРД");
        var location=Component.literal("§e[NPC] §bAgatha§f: §rCome see me at §2Murkwater Loch §fto claim your rewards!");result(chat,location,"[NPC] Agatha: Приходи ко мне в локацию «Тёмное озеро», чтобы забрать награды!","Agatha");
        for(String unknown:List.of("[NPC] Wither_101: Keep it up!","[NPC] Placeholder Name: Keep it up!","[NPC] Agatha: You earned a total of 1,,001 points!","[NPC] Agatha: You reached the UNKNOWN Bracket in my contest!","[NPC] Agatha: Come see me at Unknown Zone to claim your rewards!","[VIP] Player: [NPC] Agatha: Keep it up!")){var c=Component.literal(unknown);check(chat.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"unknown never guessed");}
        Path custom=Path.of(args[0]).resolve("forest-npc-custom");Files.createDirectories(custom);String phrase="[NPC] Agatha: Keep it up!";Files.writeString(custom.resolve("overrides.tsv"),phrase+"\tМоя фраза\n");var override=new ChatNotificationTranslator(new OfflineDictionary(custom));var c=Component.literal(phrase);check(override.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"full override priority");
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("npcDialogues",new NpcDialogueTranslator());flag("chatNotifications",chat);flag("translateGameChat",false);flag("translateNpcChat",true);var message=new GuiMessage(123,c,null,GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(message).content().getString().equals("[NPC] Agatha: Так держать!"),"NPC toggle alone");flag("translateNpcChat",false);flag("translateGameChat",true);check(SkyRuClient.npcChat(message)==message,"NPC disabled");flag("translateNpcChat",true);flag("enabled",false);check(SkyRuClient.npcChat(message)==message,"mod disabled");
        System.out.println("PASS: forest NPC results "+checks+" assertions; five grammars, two closed speakers, five repeats, legacy samples; Miria combinations synthetic, not captured live");
    }
}
