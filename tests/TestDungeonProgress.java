import java.nio.file.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;
import ru.wither.skyru.*;

public class TestDungeonProgress {
    private static int checks;private static ChatNotificationTranslator chat;
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);checks++;}
    private static String text(Component c){var b=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component c){var a=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)a.add(s);return true;});return a;}
    private static void retained(Component a,Component b,String word){
        int x=text(a).indexOf(word),y=text(b).indexOf(word);check(x>=0&&y>=0,"missing retained token");
        var old=styles(a);var now=styles(b);for(int i=0;i<word.length();i++)check(old.get(x+i).equals(now.get(y+i)),"token style/action: "+word);
    }
    private static Component result(Component source,String expected,String... retained){
        Component output=chat.translate(source,GuiMessageSource.SYSTEM_SERVER,false);check(text(output).equals(expected),"dynamic: "+text(output)+" vs "+expected);
        for(String token:retained)retained(source,output,token);
        check(chat.translate(output,GuiMessageSource.SYSTEM_SERVER,false)==output,"idempotence");
        for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT))
            check(chat.translate(source,channel,false)==source,"non-server whole chat untouched");
        check(chat.translate(source,GuiMessageSource.SYSTEM_SERVER,true)==source,"signed untouched");
        check(chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_SERVER,false)==source,"not NPC dialogue");
        var timed=Component.literal("[12:34:56] ").append(source);var before=timed.getString();
        var after=chat.translate(timed,GuiMessageSource.SYSTEM_SERVER,false);
        check(text(after).equals("[12:34:56] "+expected),"timestamp");retained(timed,after,"[12:34:56] ");check(timed.getString().equals(before),"input immutable");
        return output;
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);chat=new ChatNotificationTranslator(db);
        check(chat.familyCount()==36&&chat.dungeonFamilyCount()==6,"separate family counts");
        Style body=Style.EMPTY.withColor(0x55ff55).withInsertion("body"),name=Style.EMPTY.withColor(0x55ffff).withClickEvent(new ClickEvent.SuggestCommand("/msg Wither_101 ")),number=Style.EMPTY.withColor(0xff5555).withHoverEvent(new HoverEvent.ShowText(Component.literal("unchanged count")));
        for(int repeat=0;repeat<5;repeat++){
            for(String player:List.of("Wither_101","martimavocado","Pickaxe","COMMON","A","Player123456789_")){
                var ready=Component.empty().append(Component.literal(player).setStyle(name)).append(Component.literal(" is now ready!").setStyle(body));
                result(ready,player+" готов!",player);
                var countdown=Component.empty().append(Component.literal(player).setStyle(name))
                    .append(Component.literal(" has started the dungeon countdown. The dungeon will begin in ").setStyle(body))
                    .append(Component.literal("1").setStyle(number)).append(Component.literal(" minute.").setStyle(body));
                var timedResult=result(countdown,player+" запустил обратный отсчёт. Подземелье начнётся через 1 минуту.",player);
                check(styles(countdown).get(text(countdown).lastIndexOf("1")).equals(styles(timedResult).get(text(timedResult).lastIndexOf("1"))),"count distinct from nickname digits");
                for(String[] type:new String[][]{{"activated","lever","активировал рычаг"},{"activated","terminal","активировал терминал"},{"completed","device","завершил работу с устройством"}})
                    for(int total=1;total<=9;total++)for(int current=0;current<=total;current++){
                        String a=""+current,b=""+total;
                        var source=Component.empty().append(Component.literal(player).setStyle(name))
                            .append(Component.literal(" "+type[0]+" a "+type[1]+"! (").setStyle(body))
                            .append(Component.literal(a).setStyle(number)).append(Component.literal("/"+b+")").setStyle(body));
                        String original=source.getString();var output=result(source,player+" "+type[2]+"! ("+a+"/"+b+")",player);
                        // Locate the whole progress suffix, avoiding digits already present in a nickname.
                        retained(source,output,"("+a+"/"+b+")");
                        check(source.getString().equals(original),"source immutable");
                    }
            }
            for(int count=2;count<=250;count++){
                String n=""+count;int tail=count%100,last=count%10;
                String word=tail>=11&&tail<=14?"секунд":last==1?"секунду":last>=2&&last<=4?"секунды":"секунд";
                var source=Component.empty().append(Component.literal("Dungeon starts in ").setStyle(body));
                for(int i=0;i<n.length();i++)source.append(Component.literal(n.substring(i,i+1)).setStyle(number.withInsertion("digit-"+i)));
                source.append(Component.literal(" seconds.").setStyle(body));
                result(source,"Подземелье начнётся через "+n+" "+word+".",n);
            }
        }
        for(String[] pair:new String[][]{
            {"§bmartimavocado§r§a activated a lever! (§r§c7§r§a/7)","martimavocado активировал рычаг! (7/7)"},
            {"§bmartimavocado§r§a completed a device! (§r§c3§r§a/8)","martimavocado завершил работу с устройством! (3/8)"},
            {"§bmartimavocado§r§a activated a terminal! (§r§c4§r§a/7)","martimavocado активировал терминал! (4/7)"}
        })result(Component.literal(pair[0]),pair[1],"martimavocado");
        for(String bad:List.of("Player activated a device! (1/7)","Player completed a lever! (1/7)","Player activated a terminal! (8/7)","Player activated a terminal! (10/11)","Player activated a lever! (0/0)","Player is now ready! extra","NameThatIsFarTooLong is now ready!","Dungeon starts in 0 seconds.","Dungeon starts in 1000 seconds.","[VIP] Wither_101: Dungeon starts in 2 seconds.","Wither_101: pickaxe")){
            var c=Component.literal(bad);check(chat.translate(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"unsupported form not guessed");
        }
        var ambiguous=Component.literal("Wither_101 is now ").append(Component.literal("ready!").withStyle(Style.EMPTY.withBold(true)));
        check(chat.translate(ambiguous,GuiMessageSource.SYSTEM_SERVER,false)==ambiguous,"different prose styles fail closed");
        Path custom=Path.of(args[0]).resolve("custom-dungeon-progress");Files.createDirectories(custom);String key="Wither_101 is now ready!";
        Files.writeString(custom.resolve("overrides.tsv"),key+"\tМоя строка\n");var override=new ChatNotificationTranslator(new OfflineDictionary(custom));
        var c=Component.literal(key);check(override.translate(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"override priority");
        System.out.println("PASS: Dungeon progress "+checks+" assertions; six closed server-only families / three frozen Goldor samples / synthetic nicknames/counts / five repeats; no player chat or live-server test");
    }
}
