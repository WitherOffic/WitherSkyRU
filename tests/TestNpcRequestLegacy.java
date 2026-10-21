import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Additional legacy/opaque/custom cases, not captured live phone conversations. */
public class TestNpcRequestLegacy {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static String text(Component c){var b=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component c){var a=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)a.add(s);return true;});return a;}
    private static void retained(Component before,Component after,String from,String to){int a=text(before).indexOf(from),b=text(after).indexOf(to);check(a>=0&&b>=0,"semantic word");var x=styles(before);var y=styles(after);for(int i=0;i<to.length();i++)check(y.get(b+i).equals(x.get(a+(from.equals(to)?i:0))),"semantic actions/styles retained");}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);var chat=new ChatNotificationTranslator(db);
        Style inherited=Style.EMPTY.withBold(true).withInsertion("original inheritance").withClickEvent(new ClickEvent.RunCommand("/unchanged"));
        String[][] forms={{"Are you available? I desperately need a "," today.","Ты свободен? Сегодня мне очень нужен этот цветок: ","."},{"Bring me that "," as soon as you can!","Принеси мне этот цветок как можно скорее: ","!"},{"Could you bring me a ","?","Можешь принести мне этот цветок: ","?"},{"I really need a "," today, do you have one you could spare?","Сегодня мне очень нужен этот цветок: ",". У тебя найдётся один для меня?"}};
        for(int repeat=0;repeat<5;repeat++)for(var form:forms)for(String item:List.of("Blue Orchid","Peony","Rose Bush"))for(String phone:List.of("","✆ ")){
            String flower=db.translateForSearch(item),head="[NPC] Pablo: "+phone;
            var source=Component.literal("§e[NPC] §bPablo§f: "+phone+form[0]+"§a"+item+"§f"+form[1]).setStyle(inherited);
            String old=source.getString();var result=chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_SERVER,false);
            check(text(result).equals(head+form[2]+flower+form[3]),"whole request, timing preserved");check(source.getString().equals(old),"legacy source immutable");
            retained(source,result,"Pablo","Pablo");retained(source,result,item,flower);retained(source,result,form[0].strip(),form[2].strip());
            if(!phone.isEmpty())retained(source,result,"✆ ","✆ ");
            check(chat.translateNpcRequest(result,GuiMessageSource.SYSTEM_SERVER,false)==result,"idempotence");
            var timed=Component.literal("§d[04:05:06] ").append(source);var timedResult=chat.translateNpcRequest(timed,GuiMessageSource.SYSTEM_SERVER,false);
            check(text(timedResult).equals("[04:05:06] "+head+form[2]+flower+form[3]),"legacy timestamp");retained(timed,timedResult,"[04:05:06] ","[04:05:06] ");
        }
        String phrase="[NPC] Pablo: Bring me that Blue Orchid as soon as you can!";
        Path custom=Path.of(args[0]).resolve("request-legacy-custom");Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),phrase+"\tМоя просьба\n");
        var override=new ChatNotificationTranslator(new OfflineDictionary(custom));var c=Component.literal(phrase);check(override.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"full override never displaced");
        Path flowerCustom=Path.of(args[0]).resolve("request-legacy-flower");Files.createDirectories(flowerCustom);Files.writeString(flowerCustom.resolve("overrides.tsv"),"Blue Orchid\tМой цветок\n");
        var customFlower=new ChatNotificationTranslator(new OfflineDictionary(flowerCustom));check(text(customFlower.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false)).equals("[NPC] Pablo: Принеси мне этот цветок как можно скорее: Мой цветок!"),"flower override honoured");
        var opaque=Component.literal("[NPC] Pablo: Bring me that ").append(Component.translatable("Blue Orchid")).append(" as soon as you can!");check(chat.translateNpcRequest(opaque,GuiMessageSource.SYSTEM_SERVER,false)==opaque,"nonliteral flower untouched");
        var mixed=Component.literal("[NPC] Pablo: Bring me that §aBlue §bOrchid§f as soon as you can!");check(chat.translateNpcRequest(mixed,GuiMessageSource.SYSTEM_SERVER,false)==mixed,"internal mixed term not flattened");
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("npcDialogues",new NpcDialogueTranslator());flag("chatNotifications",chat);flag("translateNpcChat",true);flag("translateGameChat",true);
        for(String player:List.of("pickaxe","Dandelion","Mining Speed","Wither_101","[VIP] Wither_101: pickaxe","[NPC] Pablo: Bring me that Blue Orchid as soon as you can!")){
            var content=Component.literal(player);var message=new GuiMessage(123,content,null,GuiMessageSource.PLAYER,null);check(SkyRuClient.npcChat(message)==message,"entire player message excluded, not word exception");
        }
        System.out.println("PASS: NPC request legacy "+checks+" assertions; four request forms, three flowers, two phone forms, inherited events, custom/opaque guards and whole player messages");
    }
}
