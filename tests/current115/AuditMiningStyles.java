import java.nio.file.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Synthetic semantic boundaries, not a claim of observed server component structure. */
public class AuditMiningStyles {
    public static final String[][] CASES={
        {"Replaces the damage reduction from\nthe Piercing enchant to Skewer.","Заменяет снижение урона от\nзачарования «Пронзающая стрела» эффектом «Насквозь»."},
        {"Passive: Skewer\nHits through Piercing deal +7.5%\ndamage than the previous one, up to\n5 victims.","Пассивный эффект: Насквозь\nПопадания с «Пронзающей стрелой» наносят на +7.5%\nбольше урона, чем предыдущее; не более\n5 целей."},
        {"Grants +25 Mining Spread while\nMayor Cole's Mining Fiesta perk is\nactive.","Даёт +25 широты добычи,\nпока бонус мэра Коула «Горная фиеста»\nактивен."},
        {"Grants +50%  Mining Spread\ntowards Blocks.","Даёт +50%  широты добычи\nпри добыче блоков."},
        {"Convert every {1} Speed you\nhave above 100 into +1 Block\nFortune.","Преобразует каждые {1} скорости\nсверх 100 в +1 удачи добычи\nблоков."},
        {"Right-click your summoned pet to\nride it! Moves faster based on your\n Speed.","Нажмите ПКМ по призванному питомцу, чтобы\nоседлать его! Скорость передвижения зависит от вашей\n скорости."},
        {"Every {0} seconds, the next\nGemstone you mine gives 2x drops","Каждые {0} с следующий добытый\nдрагоценный камень даёт добычу x2."}
    };
    public static final Style PROSE=Style.EMPTY.withColor(0xaaaaaa).withClickEvent(new ClickEvent.RunCommand("/unchanged"));
    public static final Style TERM=Style.EMPTY.withColor(0x55ffff).withBold(true).withHoverEvent(new HoverEvent.ShowText(Component.literal("keep term")));
    public static final Style VALUE=Style.EMPTY.withColor(0x55ff55).withHoverEvent(new HoverEvent.ShowText(Component.literal("keep value")));
    private static final List<String> TERMS=List.of("Mining Spread","Mining Fiesta","Mayor Cole","Piercing","Skewer","Blocks","Block","Fortune","Gemstone","Speed");
    private static final List<String> VALUES=List.of("+7.5%","+50%","+25","{0}","{1}","100","+1","2x","5");
    public static Component row(String text){
        var out=Component.empty();int i=0;
        while(i<text.length()){
            int stop=i;String token=null;Style style=PROSE;
            for(String term:TERMS)if(text.startsWith(term,i)){token=term;style=TERM;break;}
            if(token==null)for(String value:VALUES)if(text.startsWith(value,i)){token=value;style=VALUE;break;}
            if(token!=null)stop=i+token.length();
            else {stop=i+1;while(stop<text.length()){
                final int offset=stop;if(TERMS.stream().anyMatch(t->text.startsWith(t,offset))||VALUES.stream().anyMatch(t->text.startsWith(t,offset)))break;stop++;
            }}
            out.append(Component.literal(text.substring(i,stop)).setStyle(style));i=stop;
        }
        return out;
    }
    public static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,st,cp)->{b.appendCodePoint(cp);return true;});return b.toString();}
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        int differences=0;for(int i=0;i<CASES.length;i++){
            var original=Arrays.stream(CASES[i][0].split("\n")).map(AuditMiningStyles::row).toList();
            String component=String.join("\n",TooltipTranslation.components(original,false).stream().map(Component::getString).toList());
            String styled=String.join("\n",TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false).stream().map(AuditMiningStyles::plain).toList());
            if(!component.equals(CASES[i][1])||!styled.equals(CASES[i][1]))differences++;
            System.out.println("CASE "+(i+1)+"\nEN:\n"+CASES[i][0]+"\nCOMPONENT:\n"+component+"\nSTYLED:\n"+styled+"\n");
        }
        System.out.println("DIFFERENT PARAGRAPHS: "+differences+" / "+CASES.length);
    }
}
