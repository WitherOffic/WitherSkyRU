import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** Five complete source-backed item descriptions; instructions are not literal player speech. */
public class TestNpcLoreClarity {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static void flag(String n,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,value);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,c)->{b.appendCodePoint(c);return true;});return b.toString();}
    private static String text(List<Component> rows){return String.join("\n",rows.stream().map(Component::getString).toList());}
    private static final Style PROSE=Style.EMPTY.withColor(0xaaaaaa).withClickEvent(new ClickEvent.RunCommand("/original-prose"));
    private static final Style TERM=Style.EMPTY.withColor(0x55ffff).withHoverEvent(new HoverEvent.ShowText(Component.literal("original term")));
    private static final Style VALUE=Style.EMPTY.withColor(0x55ff55).withInsertion("original number");
    private static final String[][] CASES={
        {"Increases Coin gain from Griffin\nBurrows by +2.5%.","Увеличивает количество монет из грифоньих\nнор на +2.5%."},
        {"Can be gifted to any eligible NPC to\ninvite them to visit your Garden.","Можно подарить любому подходящему персонажу,\nчтобы пригласить его посетить ваш Сад."},
        {"Reach out to the King and open the\nCommissions menu.","Связывается с королём и открывает\nменю шахтёрских заданий."},
        {"Grants +5  Mining Speed and +2.5\n Ore Fortune while in the Mines of\nDivan.","Даёт +5  скорости добычи и +2.5\n удачи добычи руды в шахтах\nДивана."},
        {"Commissions grant +10 HOTM Exp.","Шахтёрские задания дают +10 опыта Сердца горы."}};
    private static final String[][] EN_TERMS={{"Coin","Griffin","Burrows"},{"NPC","Garden"},{"King","Commissions"},{"Mining Speed","Ore Fortune","Divan"},{"Commissions","HOTM Exp"}};
    private static final String[][] RU_TERMS={{"количество монет","грифоньих","нор"},{"персонажу","Сад"},{"королём","меню шахтёрских заданий"},{"скорости добычи","удачи добычи руды","Дивана"},{"Шахтёрские задания","опыта Сердца горы"}};
    private static final String[][] VALUES={{"+2.5%"},{},{},{"+5","+2.5"},{"+10"}};
    private static List<Component> source(int kind){var result=new ArrayList<Component>();for(String row:CASES[kind][0].split("\n")){
        var styles=new ArrayList<Style>(Collections.nCopies(row.length(),PROSE));
        for(int category=0;category<2;category++)for(String word:category==0?EN_TERMS[kind]:VALUES[kind]){int at=row.indexOf(word);if(at>=0)for(int i=at;i<at+word.length();i++)styles.set(i,category==0?TERM:VALUE);}
        var c=Component.empty();for(int i=0;i<row.length();){int stop=i+1;while(stop<row.length()&&styles.get(i).equals(styles.get(stop)))stop++;c.append(Component.literal(row.substring(i,stop)).setStyle(styles.get(i)));i=stop;}result.add(c);
    }return result;}
    private static void styles(List<Component> translated,int kind){String full=text(translated);var expected=new ArrayList<Style>(Collections.nCopies(full.length(),PROSE));
        for(int category=0;category<2;category++)for(String word:category==0?RU_TERMS[kind]:VALUES[kind]){int at=full.indexOf(word);check(at>=0,"target semantic token");for(int i=at;i<at+word.length();i++)expected.set(i,category==0?TERM:VALUE);}
        int[] offset={0};for(var c:translated){StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)check(s.equals(expected.get(offset[0]++)),"semantic styles/actions");return true;});offset[0]++;}
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        for(int kind=0;kind<CASES.length;kind++){
            var original=source(kind);var translated=TooltipTranslation.components(original,false);
            for(int repeat=0;repeat<5;repeat++){
                check(text(translated).equals(CASES[kind][1]),"complete paragraph "+text(translated));styles(translated,kind);
                translated=TooltipTranslation.components(translated,false);
                var seq=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);
                check(String.join("\n",seq.stream().map(TestNpcLoreClarity::plain).toList()).equals(CASES[kind][1]),"styled complete paragraph");
            }
            check(text(original).equals(CASES[kind][0]),"source immutable");
            var withTitle=new ArrayList<Component>();withTitle.add(Component.literal("Royal Pigeon"));withTitle.addAll(original);
            check(TooltipTranslation.components(withTitle,true).getFirst().getString().equals("Royal Pigeon"),"Shift original title");
            check(text(TooltipTranslation.components(withTitle,true).subList(1,withTitle.size())).equals(CASES[kind][1]),"Shift descriptions still readable");
        }
        Path custom=Path.of(args[0]).resolve("custom-description");Files.createDirectories(custom);
        Files.writeString(custom.resolve("overrides.tsv"),"Reach out to the King and open the\tМоё описание\n");
        var override=new OfflineDictionary(custom);override.setRecordUnknown(false);flag("dictionary",override);
        var plainRows=Arrays.stream(CASES[2][0].split("\n")).map(Component::literal).map(c->(Component)c).toList();
        check(text(TooltipTranslation.components(plainRows,false)).contains("Моё описание"),"full-line override takes priority on uniform text");
        check(!text(TooltipTranslation.components(source(2),false)).equals(CASES[2][1]),"authored multicolour rewrite does not replace custom override");
        flag("dictionary",db);flag("enabled",false);check(text(TooltipTranslation.components(source(0),false)).equals(CASES[0][0]),"disabled unchanged");
        System.out.println("PASS: NPC/item instruction clarity "+checks+" assertions; five complete paragraphs, five repeats, semantic styles, Shift, overrides");
    }
}
