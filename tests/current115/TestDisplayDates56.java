import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Received only; day/year values and glyph events survive reordering. */
public final class TestDisplayDates56{
    private static int checks;private static Method row,plain,numeric,styles,plan;
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String text(Component c)throws Exception{return(String)plain.invoke(null,c);}
    private static final String[][] MONTHS={{"January","Январь","января"},{"February","Февраль","февраля"},{"March","Март","марта"},{"April","Апрель","апреля"},{"May","Май","мая"},{"June","Июнь","июня"},{"July","Июль","июля"},{"August","Август","августа"},{"September","Сентябрь","сентября"},{"October","Октябрь","октября"},{"November","Ноябрь","ноября"},{"December","Декабрь","декабря"}};
    public static void main(String[] args)throws Exception{
        row=TestSacks55.class.getDeclaredMethod("row",String.class,int.class,int.class);row.setAccessible(true);plain=TestSacks55.class.getDeclaredMethod("plain",Component.class);plain.setAccessible(true);numeric=TestSacks55.class.getDeclaredMethod("numeric",List.class);numeric.setAccessible(true);styles=TestSacks55.class.getDeclaredMethod("styles",Component.class);styles.setAccessible(true);var type=Class.forName("ru.wither.skyru.DisplayDates");plan=type.getDeclaredMethod("plan",String.class);plan.setAccessible(true);
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        for(var m:MONTHS)for(String name:m)for(String prefix:List.of("Received","Received ","Received: ","Получено","Получено: "))for(String day:List.of("1","13","28")){
            String original=prefix+name+" "+day+", 2026",target="Получено: "+day+" "+m[2]+" 2026";
            check(db.translateForSearch(original).equals(target),"plain month/day order "+original);
            for(int mode=0;mode<4;mode++)for(int seed=0;seed<2;seed++){
                var c=(Component)row.invoke(null,"§7"+prefix+"§d"+name+" §a"+day+"§7, §b2026",mode,seed);var current=c;var sequence=c.getVisualOrderText();
                for(int repeat=0;repeat<5;repeat++){current=SkyRuClient.component(current);sequence=SkyRuClient.sequence(sequence,false);check(text(current).equals(target),"component date "+original);check(AuditMiningStyles.plain(sequence).equals(target),"sequence date");check(numeric.invoke(null,List.of(c)).equals(numeric.invoke(null,List.of(current))),"original day/year glyph events");}
                var a=(List<?>)styles.invoke(null,c);var b=(List<?>)styles.invoke(null,current);check(a.get(text(c).indexOf(name)).equals(b.get(target.indexOf(m[2]))),"month semantic style/action");check(text(c).equals(original),"source immutable");
                var signed=new GuiMessage(0,c,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed");var msg=new GuiMessage(0,c,null,GuiMessageSource.PLAYER,null);check(SkyRuClient.npcChat(msg)==msg,"player");
            }
        }
        for(String s:List.of("Received February 29, 1900","Received February 29, 2026","Received April 31, 2026","Received October 0, 2026","Received October 13, 26","Created October 13, 2026","Received October 13, 2026 UNKNOWN"))check(plan.invoke(null,s)==null,"invalid/unreviewed date is not reordered");
        check(db.translateForSearch("Received February 29, 2000").equals("Получено: 29 февраля 2000"),"century leap");check(db.translateForSearch("Received February 29, 2024").equals("Получено: 29 февраля 2024"),"leap");
        var c=(Component)row.invoke(null,"ReceivedOctober 13, 2026",3,8);var before=text(c);var broken=Component.empty().append(Component.literal("ReceivedO").withColor(0xff5555)).append(Component.literal("ctober 13, 2026").withColor(0x5555ff));check(SkyRuClient.component(broken)==broken,"unknown mixed month style declines whole reorder");
        for(String key:List.of("ReceivedOctober 13, 2026","Received","October")){var dir=root.resolve("override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой текст\n");flag("dictionary",new OfflineDictionary(dir));check(!text(SkyRuClient.component(c)).equals("Получено: 13 октября 2026"),"custom date/month veto");}
        flag("dictionary",db);flag("enabled",false);check(SkyRuClient.component(c)==c&&text(c).equals(before),"disabled");
        System.out.println("PASS: display dates56 "+checks+" assertions;12 months/EN+RU forms/5 prefixes/3 days,4 layouts/2 fresh actions/5 repeats, valid leap guards/per-glyph numeric copy, overrides/chat/mixed style;Received only,not all timestamp formats");
    }
}
