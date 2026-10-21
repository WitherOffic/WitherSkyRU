import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Seven reviewed families, frozen corpus + observed alternative wrap; not all enchants. */
public class TestEnchantmentParagraphs {
    private static int checks;
    private static final Set<String> FAMILIES=Set.of("INFINITE_QUIVER","EXPERIENCE","SCAVENGER","SHARPNESS","POWER","GROWTH","PROTECTION");
    private static final Pattern VALUE=Pattern.compile("[+]?[0-9][0-9,.]*(?:%)?");
    private static final Style PROSE=Style.EMPTY.withColor(0xaaaaaa).withClickEvent(new ClickEvent.RunCommand("/keep"));
    private static final Style NUMBER=Style.EMPTY.withColor(0x55ff55).withHoverEvent(new HoverEvent.ShowText(Component.literal("keep number")));
    private static final Style TERM=Style.EMPTY.withColor(0x55ffff).withBold(true);
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
    private static void eq(String a,String b){check(a.equals(b),a+" != "+b);}
    private static Component row(String text,int mode){
        if(mode==0)return Component.literal(text).setStyle(PROSE);
        var styles=new ArrayList<Style>(Collections.nCopies(text.length(),PROSE));
        var m=VALUE.matcher(text);while(m.find())for(int i=m.start();i<m.end();i++)styles.set(i,NUMBER);
        if(mode==2)for(String term:List.of("Coins","Health","Defense")){
            int at=text.indexOf(term);if(at>=0)for(int i=at;i<at+term.length();i++)styles.set(i,TERM);
        }
        var out=Component.empty();for(int i=0;i<text.length();i++)out.append(Component.literal(text.substring(i,i+1)).setStyle(styles.get(i)));
        return out;
    }
    private static FormattedCharSequence sequence(Component c){return sink->StringDecomposer.iterateFormatted(c,Style.EMPTY,sink);}
    private static String joined(List<Component> c){return String.join("\n",c.stream().map(Component::getString).toList());}
    private static String expected(String family,String source){
        var m=VALUE.matcher(source);check(m.find(),"missing numeric value");String n=m.group();
        return switch(family){
            case "INFINITE_QUIVER"->"С шансом "+n+" сохраняет стрелы\nпри выстреле из лука.";
            case "EXPERIENCE"->source.contains("chance for mobs")?"С шансом "+n+" существа\nи руда дают двойной\nопыт.":"С шансом "+n+"\nсущества и руда дают двойной\nопыт.";
            case "SCAVENGER"->"За убийство даёт монеты: "+n+" за каждый\nуровень убитого существа.";
            case "SHARPNESS"->"Увеличивает урон в ближнем бою на\n"+n;
            case "POWER"->"Увеличивает урон лука на "+n+".";
            case "GROWTH"->"Даёт "+n+" ❤ здоровья.";
            case "PROTECTION"->"Даёт "+n+" ❈ защиты.";
            default->throw new AssertionError(family);
        };
    }
    private static void test(String family,String source,OfflineDictionary db){
        String target=expected(family,source);
        for(int mode=0;mode<3;mode++){
            final int styleMode=mode;
            var original=Arrays.stream(source.split("\n")).map(s->row(s,styleMode)).toList();
            var components=TooltipTranslation.components(original,false);
            var styled=TooltipTranslation.styled(original.stream().map(TestEnchantmentParagraphs::sequence).toList(),false);
            for(int pass=0;pass<5;pass++){
                eq(joined(components),target);eq(String.join("\n",styled.stream().map(AuditMiningStyles::plain).toList()),target);
                for(var c:components){String text=c.getString();int[] at={0};sequence(c).accept((i,s,cp)->{
                    boolean number=Character.isDigit(cp)||cp=='+'||cp=='%'||((cp=='.'||cp==',')&&at[0]>0&&Character.isDigit(text.charAt(at[0]-1)));
                    if(styleMode>0&&number)check(s.equals(NUMBER),"current numeric style/action lost");
                    at[0]+=Character.charCount(cp);return true;
                });}
                if(mode==2){String term=switch(family){case "SCAVENGER"->"монеты";case "GROWTH"->"здоровья";case "PROTECTION"->"защиты";default->null;};
                    if(term!=null){Component c=components.getFirst();String text=c.getString();int start=text.indexOf(term);int[] at={0};sequence(c).accept((i,s,cp)->{if(at[0]>=start&&at[0]<start+term.length())check(s.equals(TERM),"semantic term style lost");at[0]+=Character.charCount(cp);return true;});}
                }
                components=TooltipTranslation.components(components,false);styled=TooltipTranslation.styled(styled,false);
            }
            eq(joined(original),source);eq(db.translateForSearch(target),target);
        }
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        int cards=0;var items=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("item-context.json"))).getAsJsonArray();
        for(var raw:items){var item=raw.getAsJsonObject();String id=item.get("id").getAsString();if(!id.contains(";"))continue;String family=id.split(";")[0];if(!FAMILIES.contains(family)||!item.get("name").getAsString().equals("Enchanted Book"))continue;
            var body=new ArrayList<String>();var lore=item.getAsJsonArray("lore");for(int i=3;i<lore.size()&&!lore.get(i).getAsString().isBlank();i++)body.add(lore.get(i).getAsString());
            test(family,String.join("\n",body),db);cards++;
        }
        check(cards==49,"frozen corpus changed: "+cards);
        test("INFINITE_QUIVER","Saves arrows 3% of the time when\nyou fire your bow.",db);
        var source=List.<Component>of(row("Saves arrows 3% of the time",1),row("when you fire your bow.",1));
        var broken=List.<Component>of(source.getFirst(),row("when you fire your bow. NOT REVIEWED",1));
        eq(joined(TooltipTranslation.components(broken,false)),joined(broken.stream().map(SkyRuClient::component).toList()));
        for(String key:List.of("Saves arrows 3% of the time","3","{0}")){
            Path custom=Path.of(args[0]).resolve("override-"+checks);Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),key+"\tМой текст\n");flag("dictionary",new OfflineDictionary(custom));
            eq(joined(TooltipTranslation.components(source,false)),joined(source.stream().map(SkyRuClient::component).toList()));
        }
        flag("dictionary",db);flag("enabled",false);check(TooltipTranslation.components(source,false)==source,"disabled identity");
        flag("enabled",true);var shift=TooltipTranslation.components(source,true);eq(shift.getFirst().getString(),source.getFirst().getString());
        System.out.println("PASS: enchantment paragraphs "+checks+" assertions;49 cards/7 families + observed wrap;3 styles/five repeats/numbers/actions/terms/neighbours/overrides/disabled/Shift; not full server audit");
    }
}
