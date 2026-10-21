import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** Six complete lore paragraphs read in seven hunting/cult cards. */
public class TestNpcRelatedCards {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xaaaaaa).withInsertion("original prose");
    private static final Style TERM=Style.EMPTY.withColor(0x55ffff).withHoverEvent(new HoverEvent.ShowText(Component.literal("original term")));
    private static final Style VALUE=Style.EMPTY.withColor(0x55ff55).withClickEvent(new ClickEvent.RunCommand("/unchanged-value"));
    private static final String[][] CASES={
        {"Wear this hat to a Cult of the Fallen\nStar meeting.","Наденьте этот шлем на встречу Культа Падшей\nзвезды."},
        {"Gain +30 Speed while in the\nMushroom Desert.","Даёт +30 скорости в\nГрибной пустыне."},
        {"Earn +1 pelt from tracker quests.","Даёт +1 шкуру за задания охотника."},
        {"Earn x1.1 Pelts from tracker quests.","Количество шкур за задания охотника: x1.1."},
        {"Collect 100 Pelts to upgrade this\naccessory to UNCOMMON.","Соберите 100 шкур, чтобы улучшить\nаксессуар до редкости UNCOMMON."},
        {"Indicates the approximate height difference\nand angle between you and your tracked mob.","Показывает примерный перепад высот\nи угол между вами и отслеживаемой целью."}};
    private static final String[][] EN={{"Cult of the Fallen","Star"},{"Speed","Mushroom Desert"},{"pelt"},{"Pelts"},{"Pelts","UNCOMMON"},{"height difference","angle","tracked mob"}};
    private static final String[][] RU={{"Культа Падшей","звезды"},{"скорости","Грибной пустыне"},{"шкуру"},{"Количество шкур"},{"шкур","UNCOMMON"},{"перепад высот","угол","отслеживаемой целью"}};
    private static final String[][] NUM={{},{"+30"},{"+1"},{"x1.1"},{"100"},{}};
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String text(List<Component> rows){return String.join("\n",rows.stream().map(Component::getString).toList());}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Component> source(int n){var output=new ArrayList<Component>();for(String row:CASES[n][0].split("\n")){
        var styles=new ArrayList<Style>(Collections.nCopies(row.length(),BODY));for(int group=0;group<2;group++)for(String token:group==0?EN[n]:NUM[n]){int at=row.indexOf(token);if(at>=0)for(int i=at;i<at+token.length();i++)styles.set(i,group==0?TERM:VALUE);}
        var c=Component.empty();for(int i=0;i<row.length();){int end=i+1;while(end<row.length()&&styles.get(i).equals(styles.get(end)))end++;c.append(Component.literal(row.substring(i,end)).setStyle(styles.get(i)));i=end;}output.add(c);
    }return output;}
    private static void styles(List<Component> rows,int n){String full=text(rows);var expected=new ArrayList<Style>(Collections.nCopies(full.length(),BODY));for(int group=0;group<2;group++)for(String token:group==0?RU[n]:NUM[n]){int at=full.indexOf(token);check(at>=0,"target semantic span");for(int i=at;i<at+token.length();i++)expected.set(i,group==0?TERM:VALUE);}
        int[] offset={0};for(var c:rows){StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int k=0;k<Character.charCount(p);k++)check(s.equals(expected.get(offset[0]++)),"effective style/action");return true;});offset[0]++;}
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        for(int n=0;n<CASES.length;n++){
            var original=source(n);var translated=TooltipTranslation.components(original,false);for(int repeat=0;repeat<5;repeat++){
                check(text(translated).equals(CASES[n][1]),"complete NPC-related paragraph: "+text(translated));styles(translated,n);translated=TooltipTranslation.components(translated,false);
                var seq=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);check(String.join("\n",seq.stream().map(TestNpcRelatedCards::plain).toList()).equals(CASES[n][1]),"styled paragraph");
            }
            check(text(original).equals(CASES[n][0]),"source immutable");var titled=new ArrayList<Component>();titled.add(Component.literal("Fallen Star Helmet"));titled.addAll(original);check(TooltipTranslation.components(titled,true).getFirst().getString().equals("Fallen Star Helmet"),"Shift title original");check(text(TooltipTranslation.components(titled,true).subList(1,titled.size())).equals(CASES[n][1]),"Shift description readable");
        }
        Path custom=Path.of(args[0]).resolve("related-cards-custom");Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),"Wear this hat to a Cult of the Fallen\tМоя инструкция\n");var override=new OfflineDictionary(custom);override.setRecordUnknown(false);flag("dictionary",override);
        var uniform=Arrays.stream(CASES[0][0].split("\n")).map(Component::literal).map(c->(Component)c).toList();check(text(TooltipTranslation.components(uniform,false)).contains("Моя инструкция"),"uniform override wins");check(!text(TooltipTranslation.components(source(0),false)).equals(CASES[0][1]),"authored multicolour rewrite yields to override");
        flag("dictionary",db);flag("enabled",false);check(text(TooltipTranslation.components(source(1),false)).equals(CASES[1][0]),"disabled unchanged");
        System.out.println("PASS: NPC related cards "+checks+" assertions; six whole paragraphs, five repeats, semantic styles, Shift, overrides, unchanged English rarity");
    }
}
