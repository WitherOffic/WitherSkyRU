import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Local miss strings vs authored expectations; unknown adjacency is not a tooltip. */
public final class TestExperimentDisplay57{
    private static int checks;private static Method row,plain,numeric,styles;
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String text(Component c)throws Exception{return(String)plain.invoke(null,c);}
    private static final Map<String,String> LABELS=Map.ofEntries(
        Map.entry("Add-on Experiment","Дополнительный эксперимент"),Map.entry("Add-on locked!","Дополнительная игра недоступна!"),Map.entry("Clicks:","Ходы:"),Map.entry("Enchant items to earn Enchanting EXP!","Зачаровывайте предметы, чтобы получать опыт зачарования!"),Map.entry("Enchanting Exp","Опыт зачарования"),Map.entry("Enchanting level!","уровень зачарования!"),Map.entry("Enchanting level too low!","Недостаточный уровень зачарования!"),Map.entry("Experiment over","Эксперимент завершён"),Map.entry("Experiment Over","Эксперимент завершён"),Map.entry("Experimental XP:","Опыт экспериментов:"),Map.entry("Experimentation Table RNG","Случайная добыча стола экспериментов"),Map.entry("Experimentation Table RNG Meter","Счётчик случайной добычи стола экспериментов"),Map.entry("Experiments, Bazaar","Эксперименты, Базар"),Map.entry("Grid Size:","Размер поля:"),Map.entry("Main Experiment","Основной эксперимент"),Map.entry("Possible pairs:","Возможные пары:"),Map.entry("Rare chance to find VII books!","Малая вероятность найти книги VII!"),Map.entry("Renew Experiments","Добавить попытку экспериментов"),Map.entry("Required Enchanting:","Требуется уровень зачарования:"),Map.entry("Starting cost:","Стоимость запуска:"),Map.entry("Stored Experimental XP:","Накопленный опыт экспериментов:"),Map.entry("To Experimentation Table","К столу экспериментов"),Map.entry("Unknown experiment!","Неизвестный эксперимент!"),Map.entry("Unlock this minigame with a higher","Для этой мини-игры нужен более высокий"),Map.entry("Click any button!","Нажмите на любую ячейку!"),Map.entry("Click a second button!","Нажмите на вторую ячейку!"),Map.entry("Next button is instantly rewarded!","Следующая ячейка сразу даёт награду!"),Map.entry("Superpairs Powerup","Усиление Суперпар"),Map.entry("Remember the pattern!","Запомните последовательность!"),Map.entry("XX ULTRA-RARE BOOK! XX","XX ОЧЕНЬ РЕДКАЯ КНИГА! XX"));
    private static String expected(String s){
        if(LABELS.containsKey(s))return LABELS.get(s);
        for(String[] p:new String[][]{{"Superpairs","Суперпары"},{"Chronomatron","Хрономатрон"},{"Ultrasequencer","Ультрасеквенсор"}})if(s.startsWith(p[0]+" ")){String tail=s.substring(p[0].length());if(tail.equals(" Rewards"))return p[1]+" — награды";for(String[] t:new String[][]{{"Beginner","Начальный"},{"High","Продвинутый"},{"Grand","Большой"},{"Supreme","Высший"},{"Transcendent","Трансцендентный"},{"Metaphysical","Метафизический"}})if(tail.equals(" ("+t[0]+")"))return p[1]+" ("+t[1]+")";}
        if(s.startsWith("("))return s.replace("Experimentation Table RNG","Случайная добыча стола экспериментов");
        if(s.contains("Enchanting Exp"))return s.replace("Enchanting Exp","опыта зачарования").replace("(Stakes)","(сложность)").replace("(Pairs)","(пары)");
        if(s.endsWith("XP Levels"))return s.replace("XP Levels","уровней опыта");
        if(s.endsWith("Grand Experience Bottles"))return s.replace("Grand Experience Bottles","больших пузырьков опыта");
        if(s.endsWith("Titanic Experience Bottles"))return s.replace("Titanic Experience Bottles","титанических пузырьков опыта");
        if(s.endsWith("Experience Bottles"))return s.replace("Experience Bottles","пузырьков опыта");
        if(s.endsWith("Enchanted Books"))return s.replace("Enchanted Books","зачарованных книг");
        if(s.startsWith("Stakes: "))return s.replace("Stakes:","Сложность:").replace("Metaphysical","Метафизический");
        for(String[] p:new String[][]{{"Remaining Clicks:","Осталось ходов:"},{"Round:","Раунд:"},{"Experimental XP:","Опыт экспериментов:"},{"Stored Experimental XP:","Накопленный опыт экспериментов:"}})if(s.startsWith(p[0]+" "))return s.replace(p[0],p[1]);
        if(s.startsWith("Timer: "))return s.replace("Timer:","Таймер:").replace("s"," с");throw new AssertionError("unreviewed "+s);
    }
    private static void test(String s,String target)throws Exception{
        for(int mode=0;mode<4;mode++)for(int seed=0;seed<5;seed++){
            var c=(Component)row.invoke(null,s,mode,seed);var current=c;var seq=c.getVisualOrderText();
            for(int n=0;n<5;n++){current=SkyRuClient.component(current);seq=SkyRuClient.sequence(seq,false);check(text(current).equals(target),s+" mode"+mode+" => "+text(current));check(AuditMiningStyles.plain(seq).equals(target),"sequence");check(numeric.invoke(null,List.of(c)).equals(numeric.invoke(null,List.of(current))),"digits/styles/actions unchanged");}
            check(text(c).equals(s.replaceAll("(?i)§[0-9a-fk-or]","")),"source immutable");var signed=new GuiMessage(0,c,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed");var msg=new GuiMessage(0,c,null,GuiMessageSource.PLAYER,null);check(SkyRuClient.npcChat(msg)==msg,"player");
        }
    }
    public static void main(String[] args)throws Exception{
        row=TestSacks55.class.getDeclaredMethod("row",String.class,int.class,int.class);row.setAccessible(true);plain=TestSacks55.class.getDeclaredMethod("plain",Component.class);plain.setAccessible(true);numeric=TestSacks55.class.getDeclaredMethod("numeric",List.class);numeric.setAccessible(true);styles=TestSacks55.class.getDeclaredMethod("styles",Component.class);styles.setAccessible(true);
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);int observed=0,deferred=0;
        var capture=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("EXPERIMENT_MISSES_0.15.57.json"))).getAsJsonObject().getAsJsonArray("records");
        for(var r:capture){String s=r.getAsJsonObject().get("source").getAsString();if(s.equals("the Experimentation Table.")){deferred++;continue;}test(s,expected(s));observed++;}
        check(observed==59&&deferred==1,"59 observed strings plus1 ambiguous fragment deferred");
        for(String s:List.of("Chronomatron (Metaphysical)","Ultrasequencer (Metaphysical)","Experiment Over","Stakes: Metaphysical","Remaining Clicks: 22","Remaining Clicks: 0","Round: 1","Round: 2","Timer: 3s","Timer: 10s","Experimental XP: 131k","Stored Experimental XP: 42,000"))test(s,expected(s));
        for(var p:LABELS.entrySet())test(p.getKey(),p.getValue());
        var original=(Component)row.invoke(null,"§7Stakes: §dMetaphysical",2,7);var next=SkyRuClient.component(original);var a=(List<?>)styles.invoke(null,original);var b=(List<?>)styles.invoke(null,next);check(a.get(text(original).indexOf("Metaphysical")).equals(b.get(text(next).indexOf("Метафизический"))),"difficulty semantic action");
        for(String key:List.of("25k-150k Enchanting Exp","Enchanting Exp","Superpairs","Metaphysical","XP Levels")){var dir=root.resolve("custom-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой текст\n");flag("dictionary",new OfflineDictionary(dir));for(String s:List.of("25k-150k Enchanting Exp","Superpairs (Metaphysical)","50 XP Levels"))if(s.contains(key))check(!SkyRuClient.component(Component.literal(s)).getString().equals(expected(s)),"custom veto");}flag("dictionary",db);
        var type=Class.forName("ru.wither.skyru.ExperimentDisplay");var edits=type.getDeclaredMethod("edits",String.class);edits.setAccessible(true);for(String bad:List.of("50 XP Levels UNKNOWN","-1 XP Levels","1..2 Enchanting Exp","(4/3) Experimentation Table RNG","Timer: 8e8s","Superpairs (UNKNOWN)","Remaining Clicks: 12.5k"))check(((List<?>)edits.invoke(null,bad)).isEmpty(),"invalid shape not authored");
        flag("enabled",false);var c=Component.literal("Main Experiment");check(SkyRuClient.component(c)==c,"disabled");
        System.out.println("PASS: experiment display57 "+checks+" assertions;59 local miss strings/1 deferred fragment,source-backed phase/title/count shapes,4 layouts/5 fresh actions/5 repeats,values/chat/overrides;not full tooltip adjacency/live GUI");
    }
}
