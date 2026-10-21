import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** Five closed reviewed paragraphs in eleven full forest cards. */
public class TestForestBoosterCards {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xaaaaaa).withInsertion("original prose");
    private static final Style TERM=Style.EMPTY.withColor(0x55ffff).withHoverEvent(new HoverEvent.ShowText(Component.literal("original term")));
    private static final Style VALUE=Style.EMPTY.withColor(0x55ff55).withClickEvent(new ClickEvent.RunCommand("/unchanged-value"));
    private static final String[][] CASES={{"Gains 1 Sweep every 2,000 Logs\ncut on Galatea. (max 20)","Получает 1 размаха за каждые 2,000 брёвен,\nсрубленных на Галатее. (максимум 20)"},{"Applies the Groovy reforge when\ncombined with Armor.","Накладывает перековку «Зажигательный» при\nобъединении с бронёй."},{"Applies the ꕮ Sweep booster when\ncombined with Foraging Armor,\nEquipment, or Axes.","При объединении добавляет усилитель ꕮ размаха для\nлесорубской брони,\nэкипировки или топоров."},{"Grants  Sweep.","Даёт  размах."},{"Apply to an item with a Common Sweep\nBooster to upgrade it to an\nUncommon Sweep Booster, which\nincreases the amount of  Sweep it\ngives.","Примените к предмету с обычным усилителем размаха,\nчтобы улучшить этот усилитель до\nнеобычного усилителя размаха, который\nувеличивает количество  размаха,\nкоторое даёт этот усилитель."}};
    private static final String[][] EN={{"Sweep","Logs","Galatea"},{"Groovy","Armor"},{"Sweep","Foraging Armor","Equipment","Axes"},{"Sweep"},{"Common Sweep","Booster","Uncommon Sweep Booster","Sweep"}};
    private static final String[][] RU={{"размаха","брёвен","Галатее"},{"Зажигательный","бронёй"},{"размаха","лесорубской брони","экипировки","топоров"},{"размах"},{"обычным усилителем размаха","усилитель","необычного усилителя размаха","размаха"}};
    private static final String[][] NUM={{"1","","2,000","20"},{},{"ꕮ"},{""},{""}};
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String text(List<Component> rows){return String.join("\n",rows.stream().map(Component::getString).toList());}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Component> source(int n){var output=new ArrayList<Component>();for(String row:CASES[n][0].split("\\n")){
        var styles=new ArrayList<Style>(Collections.nCopies(row.length(),BODY));var occupied=new BitSet(row.length());
        var tokens=new ArrayList<String>();tokens.addAll(List.of(EN[n]));tokens.sort(Comparator.comparingInt(String::length).reversed());
        for(String token:tokens)for(int at=row.indexOf(token);at>=0;at=row.indexOf(token,at+token.length())){
            if(occupied.nextSetBit(at)>=0&&occupied.nextSetBit(at)<at+token.length())continue;
            for(int i=at;i<at+token.length();i++)styles.set(i,TERM);occupied.set(at,at+token.length());
        }
        for(String token:NUM[n])for(int at=row.indexOf(token);at>=0;at=row.indexOf(token,at+token.length()))
            for(int i=at;i<at+token.length();i++)styles.set(i,VALUE);
        var c=Component.empty();for(int i=0;i<row.length();){int end=i+1;while(end<row.length()&&styles.get(i).equals(styles.get(end)))end++;
            c.append(Component.literal(row.substring(i,end)).setStyle(styles.get(i)));i=end;}output.add(c);
    }return output;}
    private static void styles(List<Component> rows,int n){
        String[] inputs=CASES[n][0].split("\\n");
        for(int rowIndex=0;rowIndex<rows.size();rowIndex++){
            var component=rows.get(rowIndex);String full=component.getString();
            var expected=new ArrayList<Style>(Collections.nCopies(full.length(),BODY));var occupied=new BitSet(full.length());
            var tokens=new ArrayList<String>();
            for(int term=0;term<EN[n].length;term++)if(inputs[rowIndex].contains(EN[n][term]))tokens.add(RU[n][term]);
            tokens.sort(Comparator.comparingInt(String::length).reversed());
            for(String token:tokens)for(int at=full.indexOf(token);at>=0;at=full.indexOf(token,at+token.length())){
                if(occupied.nextSetBit(at)>=0&&occupied.nextSetBit(at)<at+token.length())continue;
                for(int i=at;i<at+token.length();i++)expected.set(i,TERM);occupied.set(at,at+token.length());
            }
            for(String token:NUM[n])for(int at=full.indexOf(token);at>=0;at=full.indexOf(token,at+token.length()))
                for(int i=at;i<at+token.length();i++)expected.set(i,VALUE);
            int[] offset={0};StringDecomposer.iterateFormatted(component,Style.EMPTY,(i,s,p)->{
                for(int k=0;k<Character.charCount(p);k++)check(s.equals(expected.get(offset[0]++)),"effective style/action row "+full);
                return true;});
        }
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        for(int n=0;n<CASES.length;n++){
            var original=source(n);var translated=TooltipTranslation.components(original,false);for(int repeat=0;repeat<5;repeat++){
                check(text(translated).equals(CASES[n][1]),"complete forest paragraph: "+text(translated));styles(translated,n);translated=TooltipTranslation.components(translated,false);
                var seq=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);check(String.join("\n",seq.stream().map(TestForestBoosterCards::plain).toList()).equals(CASES[n][1]),"styled paragraph");
            }
            check(text(original).equals(CASES[n][0]),"source immutable");var titled=new ArrayList<Component>();titled.add(Component.literal("Fig Hew"));titled.addAll(original);check(TooltipTranslation.components(titled,true).getFirst().getString().equals("Fig Hew"),"Shift title original");check(text(TooltipTranslation.components(titled,true).subList(1,titled.size())).equals(CASES[n][1]),"Shift description readable");
        }
        for(int n:new int[]{2,4}){
            String rarity=n==2?"COMMON":"UNCOMMON";var card=new ArrayList<Component>();card.add(Component.literal("Common Sweep Booster"));card.addAll(source(n));card.add(Component.literal(rarity+" BOOSTER"));
            check(TooltipTranslation.components(card,false).getLast().getString().equals(rarity+" УСИЛИТЕЛЬ"),"English rarity tag kept");
        }
        Path custom=Path.of(args[0]).resolve("forest-booster-custom");Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),"Gains 1 Sweep every 2,000 Logs\tМоя инструкция\n");var override=new OfflineDictionary(custom);override.setRecordUnknown(false);flag("dictionary",override);
        var uniform=Arrays.stream(CASES[0][0].split("\n")).map(Component::literal).map(c->(Component)c).toList();check(text(TooltipTranslation.components(uniform,false)).contains("Моя инструкция"),"uniform override wins");check(!text(TooltipTranslation.components(source(0),false)).equals(CASES[0][1]),"authored multicolour rewrite yields to override");
        flag("dictionary",db);flag("enabled",false);check(text(TooltipTranslation.components(source(1),false)).equals(CASES[1][0]),"disabled unchanged");
        System.out.println("PASS: Forest booster cards "+checks+" assertions; five whole paragraphs, five repeats, semantic styles, Shift, overrides, unchanged English rarity");
    }
}
