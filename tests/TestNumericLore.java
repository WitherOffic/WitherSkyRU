import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Synthetic concrete substitutions: display copied literally, never stats calculated. */
public class TestNumericLore {
    private static int checks;
    private static void eq(String got,String expected){if(!got.equals(expected))throw new AssertionError(got+"; expected "+expected);checks++;}
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    private static FormattedCharSequence sequence(Component c){var out=new ArrayList<FormattedCharSequence>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,cp)->{out.add(FormattedCharSequence.forward(new String(Character.toChars(cp)),s));return true;});return FormattedCharSequence.composite(out);}
    private static String componentText(List<Component> rows){return String.join("\n",rows.stream().map(Component::getString).toList());}
    private static String styledText(List<FormattedCharSequence> rows){return String.join("\n",rows.stream().map(AuditMiningStyles::plain).toList());}
    private static List<Component> rows(int kind,String value){
        String parameter=kind==4?"{1}":"{0}";var rows=new ArrayList<Component>();
        for(String line:AuditMiningStyles.CASES[kind][0].split("\n")){
            int at=line.indexOf(parameter);rows.add(at<0?AuditMiningStyles.row(line):Component.empty()
                .append(AuditMiningStyles.row(line.substring(0,at))).append(Component.literal(value).setStyle(AuditMiningStyles.VALUE))
                .append(AuditMiningStyles.row(line.substring(at+parameter.length()))));
        }return rows;
    }
    private static void styles(FormattedCharSequence actual,String template,String parameter,String value,int kind){
        String expected=template.replace(parameter,value);eq(AuditMiningStyles.plain(actual),expected);
        var colours=new ArrayList<Style>(Collections.nCopies(template.length(),AuditMiningStyles.PROSE));
        for(String term:kind==4?List.of("скорости","удачи добычи","блоков"):List.of("драгоценный камень")){
            int at=template.indexOf(term);if(at>=0)for(int i=at;i<at+term.length();i++)colours.set(i,AuditMiningStyles.TERM);
        }
        for(String number:kind==4?List.of("100","+1"):List.of("x2")){
            int at=template.indexOf(number);if(at>=0)for(int i=at;i<at+number.length();i++)colours.set(i,AuditMiningStyles.VALUE);
        }
        int at=template.indexOf(parameter);
        if(at>=0){colours.subList(at,at+parameter.length()).clear();colours.addAll(at,Collections.nCopies(value.length(),AuditMiningStyles.VALUE));}
        int[] cursor={0};actual.accept((local,s,cp)->{for(int n=0;n<Character.charCount(cp);n++){
            if(!s.equals(colours.get(cursor[0]++)))throw new AssertionError("numeric/semantic style lost: "+expected);checks++;
        }return true;});
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        boolean audit=args.length>1&&args[1].equals("audit-only");int differing=0;
        for(int kind:List.of(4,6))for(String value:List.of("0","1","1.5","7","10","100","12,5","0.000001","99999999")){
            String parameter=kind==4?"{1}":"{0}",target=AuditMiningStyles.CASES[kind][1].replace(parameter,value);
            var original=rows(kind,value);var components=TooltipTranslation.components(original,false);
            var styled=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);
            if(!componentText(components).equals(target)||!styledText(styled).equals(target))differing++;
            if(audit)System.out.println("CASE "+kind+" / "+value+"\nCOMPONENT:\n"+componentText(components)+"\nSTYLED:\n"+styledText(styled));
            if(audit)continue;
            for(int pass=0;pass<5;pass++){
                eq(componentText(components),target);eq(styledText(styled),target);eq(db.translateForSearch(target),target);
                String[] templates=AuditMiningStyles.CASES[kind][1].split("\n");
                for(int i=0;i<templates.length;i++){styles(sequence(components.get(i)),templates[i],parameter,value,kind);styles(styled.get(i),templates[i],parameter,value,kind);}
                components=TooltipTranslation.components(components,false);styled=TooltipTranslation.styled(styled,false);
            }
            eq(componentText(original),AuditMiningStyles.CASES[kind][0].replace(parameter,value));
        }
        System.out.println("DIFFERENT CONCRETE PARAGRAPHS: "+differing+" / 18");if(audit)return;
        for(String invalid:List.of("-1","NaN","1e2","100000000","1.1234567"))for(int kind:List.of(4,6)){
            var original=rows(kind,invalid);eq(componentText(TooltipTranslation.components(original,false)),componentText(original.stream().map(SkyRuClient::component).toList()));
        }
        for(String key:List.of("Convert every 7 Speed you","{1}","7")){
            Path custom=Path.of(args[0]).resolve("custom-"+checks);Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),key+"\tМой перевод\n");
            flag("dictionary",new OfflineDictionary(custom));
            var original=key.startsWith("Convert")?List.<Component>of(Component.literal(key),AuditMiningStyles.row("have above 100 into +1 Block"),AuditMiningStyles.row("Fortune.")):rows(4,"7");
            if(key.equals("{1}")||key.equals("7")){eq(componentText(TooltipTranslation.components(original,false)),componentText(original.stream().map(SkyRuClient::component).toList()));}
            else {if(!componentText(TooltipTranslation.components(original,false)).contains("Мой перевод"))throw new AssertionError("numeric override lost");checks++;}
        }
        flag("dictionary",db);flag("enabled",false);eq(componentText(TooltipTranslation.components(rows(4,"7"),false)),componentText(rows(4,"7")));
        System.out.println("PASS: concrete numeric lore "+checks+" assertions; 18 synthetic paragraphs / five repeats / semantic values / invalid numbers / overrides / disabled");
    }
}
