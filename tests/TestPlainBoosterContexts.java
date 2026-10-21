import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;

/** New plain multiline dictionary/search surface, not a replay-only tooltip pass. */
public class TestPlainBoosterContexts {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static void flag(String name,Object value)throws Exception{var field=SkyRuClient.class.getDeclaredField(name);field.setAccessible(true);field.set(null,value);}
    private static String[][] fixture(String name)throws Exception{var field=TestBoosterCrossRows.class.getDeclaredField(name);field.setAccessible(true);return (String[][])field.get(null);}
    public static void main(String[] args)throws Exception{
        String[][] source=fixture("SOURCES"),target=fixture("TARGETS");var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        for(int n=0;n<source.length;n++){
            String original=String.join("\n",source[n]),expected=String.join("\n",target[n]);
            for(int repeat=0;repeat<5;repeat++){
                check(db.translate(original).equals(expected),"raw multiline prose: "+db.translate(original));
                check(db.translateForSearch(original).equals(expected),"quiet dictionary same meaning");
                check(db.translate(expected).equals(expected),"plain idempotence");
                Style root=Style.EMPTY.withColor(0xaaaaaa).withInsertion("uniform multiline").withClickEvent(new ClickEvent.RunCommand("/unchanged-multiline"));
                var literal=Component.literal(original).setStyle(root);var rendered=SkyRuClient.component(literal);
                check(rendered.getString().equals(expected),"whole multiline Component entry point");
                check(rendered.getStyle().equals(root)&&literal.getString().equals(original),"uniform Component metadata/source retained");
                var sequence=SkyRuClient.sequence(net.minecraft.util.FormattedCharSequence.forward(original,root));
                var text=new StringBuilder();sequence.accept((i,s,cp)->{text.appendCodePoint(cp);check(s.equals(root),"whole multiline sequence style/action retained");return true;});
                check(text.toString().equals(expected),"whole multiline sequence entry point");
                check(db.translate("CONTEXT_SENTINEL\n\n"+original+"\n\nUNCOMMON BOOSTER").equals("CONTEXT_SENTINEL\n\n"+expected+"\n\nUNCOMMON УСИЛИТЕЛЬ"),"neighbours/empty lines/English rarity unchanged");
                var components=Arrays.stream(source[n]).map(Component::literal).map(c->(Component)c).toList();
                check(String.join("\n",TooltipTranslation.components(components,false).stream().map(Component::getString).toList()).equals(expected),"plain and rich entry points agree");
            }
            for(int line=0;line<source[n].length;line++){
                var altered=source[n].clone();altered[line]="UNCONFIRMED_NEIGHBOUR";check(!db.translate(String.join("\n",altered)).equals(expected),"each missing source row prevents closed whole-paragraph rewrite");
            }
            String composed=original+"\nSEPARATOR_SENTINEL\n"+original;
            check(db.translate(composed).equals(expected+"\nSEPARATOR_SENTINEL\n"+expected),"two disjoint exact paragraphs use their own positions");
            for(String custom:List.of(source[n][2]+"\tМоя инструкция\n",(n==2?"Hunting":"Foraging")+"\tМой навык\n")){
                Path config=Path.of(args[0]).resolve("plain-booster-custom-"+checks);Files.createDirectories(config);Files.writeString(config.resolve("overrides.tsv"),custom);var override=new OfflineDictionary(config);override.setRecordUnknown(false);
                check(!override.translate(original).equals(expected),"whole-line/term override prevents authored plain rewrite");
                if(custom.startsWith(source[n][2]))check(override.translate(original).contains("Моя инструкция"),"custom full-row text retained");
            }
        }
        check(db.translate("Common").equals("Common"),"standalone rarity token unchanged");
        check(db.translate("UNCOMMON").equals("UNCOMMON"),"uppercase rarity token unchanged");
        check(db.translateForSearch("Uncommon Foraging Fortune Booster Upgrade").equals("Необычное улучшение усилителя удачи лесорубства"),"fortune upgrade name consistent with stat");
        check(db.translateForSearch("Uncommon Foraging Wisdom Booster Upgrade").equals("Необычное улучшение усилителя мудрости лесорубства"),"wisdom upgrade name consistent with stat");
        System.out.println("PASS: plain booster contexts "+checks+" assertions; three complete paragraphs/five repeats, direct/quiet/rich APIs, offsets/duplicates/each neighbour/custom/rarity and two canonical names");
    }
}
