import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Different counters: old record, contest collection increase, increase over old record. */
public class TestForestRecords {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static String text(Component c){var b=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component c){var a=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)a.add(s);return true;});return a;}
    private static void retained(Component from,Component to,String old,String target){int a=text(from).indexOf(old),b=text(to).indexOf(target);check(a>=0&&b>=0,"semantic token");var x=styles(from);var y=styles(to);for(int i=0;i<target.length();i++)check(y.get(b+i).equals(x.get(a+(old.equals(target)?i:0))),"effective semantic style/action");}
    private static void result(ChatNotificationTranslator chat,Component source,String expected,boolean npc,String... values){
        var result=npc?chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_SERVER,false):chat.translate(source,GuiMessageSource.SYSTEM_SERVER,false);check(text(result).equals(expected),"complete forest record: "+text(result));for(String value:values)retained(source,result,value,value);
        check((npc?chat.translateNpcRequest(result,GuiMessageSource.SYSTEM_SERVER,false):chat.translate(result,GuiMessageSource.SYSTEM_SERVER,false))==result,"idempotence");
        check((npc?chat.translateNpcRequest(source,GuiMessageSource.PLAYER,false):chat.translate(source,GuiMessageSource.PLAYER,false))==source,"PLAYER excluded");check((npc?chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_SERVER,true):chat.translate(source,GuiMessageSource.SYSTEM_SERVER,true))==source,"signed excluded");
        var timed=Component.literal("[03:04:05] ").append(source);check(text(npc?chat.translateNpcRequest(timed,GuiMessageSource.SYSTEM_SERVER,false):chat.translate(timed,GuiMessageSource.SYSTEM_SERVER,false)).equals("[03:04:05] "+expected),"timestamp");
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);var chat=new ChatNotificationTranslator(db);Style prose=Style.EMPTY.withColor(0xffffff).withInsertion("prose");Style term=Style.EMPTY.withColor(0x55ffff).withClickEvent(new ClickEvent.RunCommand("/unchanged-term"));Style number=Style.EMPTY.withColor(0x55ff55).withHoverEvent(new HoverEvent.ShowText(Component.literal("original count")));
        for(int repeat=0;repeat<5;repeat++)for(String wood:List.of("Fig","Mangrove")){
            String ru=db.translateForSearch(wood);
            for(String[] n:new String[][]{{"1","бревно"},{"2","бревна"},{"11","брёвен"},{"5,129","брёвен"}}){
                var source=Component.literal("[NPC] Agatha: PERSONAL BEST! You've surpassed your previous record of ").setStyle(prose).append(Component.literal(n[0]).setStyle(number)).append(Component.literal(" ").setStyle(prose)).append(Component.literal(wood).setStyle(term)).append(Component.literal(" logs collected in my Contest!").setStyle(prose));
                String original=source.getString();result(chat,source,"[NPC] Agatha: ЛИЧНЫЙ РЕКОРД! Ты побил свой прежний рекорд в моём соревновании: "+n[0]+" "+n[1]+" ("+ru+")!",true,"Agatha",n[0]);retained(source,chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_SERVER,false),wood,ru);check(source.getString().equals(original),"old record not new total/source unchanged");
            }
            for(String[] values:new String[][]{{"129","0"},{"5,129","1,001"},{"434,325,129","234,455,129"}}){
                var source=Component.literal("PERSONAL BEST: You increased your ").setStyle(prose).append(Component.literal(wood).setStyle(term)).append(Component.literal(" Collection by ").setStyle(prose)).append(Component.literal(values[0]).setStyle(number)).append(Component.literal(" during the contest! That's ").setStyle(prose)).append(Component.literal(values[1]).setStyle(term)).append(Component.literal(" more than your previous best!").setStyle(prose));
                result(chat,source,"ЛИЧНЫЙ РЕКОРД: За соревнование твоя коллекция «"+ru+"» выросла на "+values[0]+"! Это на "+values[1]+" больше твоего прежнего рекорда!",false,values[0],values[1]);retained(source,chat.translate(source,GuiMessageSource.SYSTEM_SERVER,false),wood,ru);
            }
        }
        for(String amount:List.of("0","1.28","10")){var source=Component.literal("Your total ").setStyle(prose).append(Component.literal("∮ Sweep").setStyle(term)).append(Component.literal(" is now increased by ").setStyle(prose)).append(Component.literal(amount+"%").setStyle(number)).append(Component.literal("!").setStyle(prose));result(chat,source,"Ваш общий ∮ Размах теперь увеличен на "+amount+"%!",false,"∮",amount+"%");retained(source,chat.translate(source,GuiMessageSource.SYSTEM_SERVER,false),"Sweep","Размах");}
        var legacy=Component.literal("§e[NPC] §bAgatha§f: §6§lPERSONAL BEST§f! You've surpassed your previous record of §e5,129 §fFig logs collected in my Contest§f!");result(chat,legacy,"[NPC] Agatha: ЛИЧНЫЙ РЕКОРД! Ты побил свой прежний рекорд в моём соревновании: 5,129 брёвен (Инжир)!",true,"Agatha","5,129");
        var notification=Component.literal("§6§lPERSONAL BEST§f: You increased your §bFig §fCollection by §b129 §fduring the contest! That's §a0 §fmore than your previous best!");result(chat,notification,"ЛИЧНЫЙ РЕКОРД: За соревнование твоя коллекция «Инжир» выросла на 129! Это на 0 больше твоего прежнего рекорда!",false,"129","0");
        for(String phrase:List.of("PERSONAL BEST: You increased your Unknown Collection by 100 during the contest! That's 10 more than your previous best!","Your total ∮ Sweep is now increased by NaN%!","[NPC] Wither_101: PERSONAL BEST! You've surpassed your previous record of 10 Fig logs collected in my Contest!")){var c=Component.literal(phrase);check(chat.translate(c,GuiMessageSource.SYSTEM_SERVER,false)==c&&chat.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"unknown not guessed");}
        for(String phrase:List.of("Your total ∮ Sweep is now increased by 10%!","PERSONAL BEST: You increased your Fig Collection by 100 during the contest! That's 10 more than your previous best!")){
            Path custom=Path.of(args[0]).resolve("forest-record-custom-"+checks);Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),phrase+"\tМой рекорд\n");var override=new ChatNotificationTranslator(new OfflineDictionary(custom));var c=Component.literal(phrase);check(override.translate(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"full custom override not displaced");
        }
        System.out.println("PASS: forest record meanings "+checks+" assertions; old-vs-new counters, two woods, five repeats, primary legacy samples, styles, unknown/player/signed/override guards");
    }
}
