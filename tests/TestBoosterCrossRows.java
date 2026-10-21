import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** Distinct per-term/per-row actions prove cross-line words keep their actual source style. */
public class TestBoosterCrossRows {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xaaaaaa).withInsertion("original prose");
    private static final String[][] SOURCES={
        {"Apply to an item with a Common","Foraging Fortune Booster to","upgrade it to an Uncommon Foraging","Fortune Booster, which increases","the amount of  Foraging Fortune it","gives."},
        {"Apply to an item with a Common","Foraging Wisdom Booster to upgrade","it to an Uncommon Foraging Wisdom","Booster, which increases the amount","of ☯ Foraging Wisdom it gives."},
        {"Apply to an item with a Common","Hunting Wisdom Booster to upgrade it","to an Uncommon Hunting Wisdom","Booster, which increases the amount","of ☯ Hunting Wisdom it gives."}};
    private static final String[][] TARGETS={
        {"Примените к предмету с обычным","усилителем удачи лесорубства, чтобы","улучшить его до необычного усилителя","удачи лесорубства, который увеличивает","количество  удачи лесорубства,","которое даёт этот усилитель."},
        {"Примените к предмету с обычным","усилителем мудрости лесорубства, чтобы улучшить","его до необычного усилителя мудрости лесорубства,","который увеличивает количество","☯ мудрости лесорубства, которую даёт."},
        {"Примените к предмету с обычным","усилителем мудрости охоты, чтобы улучшить его","до необычного усилителя мудрости охоты,","который увеличивает количество","☯ мудрости охоты, которую даёт."}};
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String text(List<Component> rows){return String.join("\n",rows.stream().map(Component::getString).toList());}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int j=0;j<Character.charCount(p);j++)out.add(s);return true;});return out;}
    private static List<Component> source(int kind){var out=new ArrayList<Component>();int row=0;
        for(String value:SOURCES[kind]){var styles=new ArrayList<Style>(Collections.nCopies(value.length(),BODY));
            for(String token:List.of("Common","Uncommon","Foraging","Hunting","Wisdom","Fortune","Booster","","☯")){
                int at=value.indexOf(token);if(at<0)continue;var style=Style.EMPTY.withColor(0x220000+row*1000+token.length()).withInsertion(row+":"+token)
                    .withClickEvent(new ClickEvent.RunCommand("/unchanged-"+row+"-"+token.length())).withHoverEvent(new HoverEvent.ShowText(Component.literal("row "+row+" term "+token)));
                for(int j=at;j<at+token.length();j++)styles.set(j,style);
            }
            var c=Component.empty();for(int i=0;i<value.length();i++){c.append(Component.literal(value.substring(i,i+1)).setStyle(styles.get(i)));}out.add(c);row++;
        }return out;
    }
    private static void span(List<Component> before,List<Component> after,int oldRow,String from,int newRow,String to){
        int a=before.get(oldRow).getString().indexOf(from),b=after.get(newRow).getString().indexOf(to);check(a>=0&&b>=0,"span present");
        var old=styles(before.get(oldRow));var next=styles(after.get(newRow));for(int i=b;i<b+to.length();i++)check(old.get(a).equals(next.get(i)),"cross-row style/action: "+from+" row "+oldRow+" => "+to+" row "+newRow);
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        for(int n=0;n<SOURCES.length;n++){
            var original=source(n);var got=TooltipTranslation.components(original,false);String expected=String.join("\n",TARGETS[n]);
            for(int repeat=0;repeat<5;repeat++){
                check(text(got).equals(expected),"whole readable upgrade: "+text(got));
                span(original,got,0,"Common",0,"обычным");span(original,got,2,"Uncommon",2,"необычного");
                span(original,got,1,"Booster",1,"усилителем");span(original,got,3,"Booster",2,"усилителя");
                span(original,got,1,n==2?"Hunting":"Foraging",1,n==2?"охоты":"лесорубства");
                span(original,got,2,n==2?"Hunting":"Foraging",n==0?3:2,n==2?"охоты":"лесорубства");
                span(original,got,n==0?3:1,n==0?"Fortune":"Wisdom",n==0?3:1,n==0?"удачи":"мудрости");
                span(original,got,4,n==0?"":"☯",4,n==0?"":"☯");
                var seq=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);
                check(String.join("\n",seq.stream().map(TestBoosterCrossRows::plain).toList()).equals(expected),"formatted overload same full paragraph");
                got=TooltipTranslation.components(got,false);check(text(original).equals(String.join("\n",SOURCES[n])),"original immutable");
            }
            var card=new ArrayList<Component>();card.add(Component.literal("Uncommon Foraging Fortune Booster Upgrade"));card.add(Component.literal(""));card.addAll(original);card.add(Component.literal("UNCOMMON BOOSTER"));
            var shifted=TooltipTranslation.components(card,true);check(shifted.getFirst().getString().equals(card.getFirst().getString()),"Shift only keeps original title");
            check(text(shifted.subList(2,2+original.size())).equals(expected),"shifted lore still translated at actual offset");check(shifted.getLast().getString().equals("UNCOMMON УСИЛИТЕЛЬ"),"English rarity tag");
            var incomplete=new ArrayList<Component>(original);incomplete.set(3,Component.literal("Unconfirmed replacement line."));
            check(!text(TooltipTranslation.components(incomplete,false)).equals(expected),"unconfirmed neighbour not guessed");
        }
        var opaque=source(0);opaque.set(3,Component.translatableWithFallback("unknown.key",SOURCES[0][3]));
        check(text(TooltipTranslation.components(opaque,false)).equals(String.join("\n",SOURCES[0])),"matching opaque group fails closed as whole paragraph");
        var ambiguous=source(0);ambiguous.set(2,Component.literal("upgrade it to an Un").setStyle(BODY).append(Component.literal("common Foraging").setStyle(Style.EMPTY.withColor(0xff0000))));
        check(text(TooltipTranslation.components(ambiguous,false)).equals(String.join("\n",SOURCES[0])),"split semantic token fails closed for whole paragraph");
        for(String row:List.of(SOURCES[0][2]+"\tМоя инструкция\n","Foraging\tМой навык\n")){
            Path config=Path.of(args[0]).resolve("crossrow-custom-"+checks);Files.createDirectories(config);Files.writeString(config.resolve("overrides.tsv"),row);flag("dictionary",new OfflineDictionary(config));
            check(!text(TooltipTranslation.components(source(0),false)).equals(String.join("\n",TARGETS[0])),"authored cross-row rewrite yields to override");
        }
        flag("dictionary",db);flag("enabled",false);check(text(TooltipTranslation.components(source(1),false)).equals(String.join("\n",SOURCES[1])),"disabled unchanged");
        System.out.println("PASS: booster cross-row prose "+checks+" assertions; three complete paragraphs/five repeats, actual per-row term events, components/sequences, Shift/rarity/custom/opaque/ambiguous guards");
    }
}
