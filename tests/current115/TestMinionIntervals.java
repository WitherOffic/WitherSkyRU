import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;
import ru.wither.skyru.*;

public class TestMinionIntervals {
    private static int checks;
    private static void check(boolean v,String why){if(!v)throw new AssertionError(why);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String text(Component c){var b=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static String plain(net.minecraft.util.FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var all=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("item-context.json"))).getAsJsonArray();int cards=0;Set<String> forms=new HashSet<>();
        var pattern=Pattern.compile("^Time Between Actions: ([0-9,.]+)s$");
        for(var raw:all){var card=raw.getAsJsonObject();if(!card.get("name").getAsString().matches("^.+ Minion [IVX]+$"))continue;
            int times=0;for(var l:card.getAsJsonArray("lore")){String source=l.getAsString();var m=pattern.matcher(source);if(!m.matches())continue;times++;forms.add(source);
                String expected="Интервал действий: "+m.group(1)+" с";check(db.translate(source).equals(expected),"actual interval: "+source);
                List<Component> lines=List.of(Component.literal(card.get("name").getAsString()),Component.literal(source),Component.literal("RARE"));
                var translated=TooltipTranslation.components(lines,true);check(translated.get(0).getString().equals(card.get("name").getAsString()),"Shift-original actual minion");
                check(translated.get(1).getString().equals(expected),"actual interval under Shift");check(translated.get(2).getString().equals("RARE"),"English rarity");
            }check(times==1,"one interval per frozen minion");cards++;
        }check(cards==719&&forms.size()==82,"719 frozen cards /82 exact intervals, including 1,013s");
        Style label=Style.EMPTY.withColor(0xaaaaaa).withHoverEvent(new HoverEvent.ShowText(Component.literal("original interval")));
        Style unit=Style.EMPTY.withColor(0xffaa00).withClickEvent(new ClickEvent.RunCommand("/original-unit"));
        for(int repeat=0;repeat<5;repeat++)for(String number:List.of("0","0.5","1","75","1,013","123,456.7","999999.999999"))for(String enUnit:List.of("s"," seconds")){
            String input="Time Between Actions: "+number+enUnit,expected="Интервал действий: "+number+" с";
            var source=Component.literal("Time Between Actions: ").setStyle(label);
            for(int i=0;i<number.length();i++)source.append(Component.literal(number.substring(i,i+1)).setStyle(Style.EMPTY.withColor(0x550000+i*0x100).withInsertion("digit-"+i)));
            if(enUnit.startsWith(" "))source.append(Component.literal(" ").setStyle(label));source.append(Component.literal(enUnit.strip()).setStyle(unit));
            var translated=SkyRuClient.component(source);check(text(translated).equals(expected),"styled interval: "+text(translated));
            var old=styles(source);var now=styles(translated);int before=input.indexOf(number),after=text(translated).indexOf(number);
            for(int i=0;i<number.length();i++)check(old.get(before+i).equals(now.get(after+i)),"original numeric glyph/action");
            check(now.get(0).equals(old.get(0)),"label actions");check(now.getLast().equals(old.getLast()),"unit action");
            check(source.getString().equals(input),"source immutable");check(text(SkyRuClient.component(translated)).equals(expected),"idempotence");
            check(plain(SkyRuClient.sequence(source.getVisualOrderText())).equals(expected),"styled sequence interval");
        }
        var legacy=Component.literal("§7Time Between Actions: §a1,013§cs");check(text(SkyRuClient.component(legacy)).equals("Интервал действий: 1,013 с"),"actual grouped value/legacy colours");
        for(String standalone:List.of("s","Time Between Actions: -1s","Time Between Actions: NaNs","Time Between Actions: 1,,013s")){
            var c=Component.literal(standalone);check(!text(SkyRuClient.component(c)).matches("Интервал действий: [0-9,.]+ с"),"invalid not accepted as valid interval");
            var msg=new GuiMessage(0,c,null,GuiMessageSource.PLAYER,null);check(SkyRuClient.npcChat(msg)==msg,"whole player chat unchanged");
        }
        for(String key:List.of("Time Between Actions: 75s","Time Between Actions")){
            Path custom=Path.of(args[0]).resolve("interval-custom-"+checks);Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),key+"\tМой интервал\n");
            var own=new OfflineDictionary(custom);flag("dictionary",own);var input=Component.literal("Time Between Actions: 75s");
            check(text(SkyRuClient.component(input)).equals(own.translate("Time Between Actions: 75s")),"existing override policy retained");check(own.translate(key).equals("Мой интервал"),"key override intact");
        }
        flag("dictionary",db);
        for(String last:List.of("upgrade.","form.","selling price.","level."))check(db.translate(last).equals(switch(last){case "upgrade."->"Улучшить.";case "form."->"форму.";case "selling price."->"цену продажи.";default->"уровня.";}),"standalone grammar fragment not globally changed");
        check(db.translate("Requires an open area to place\nCobblestone. Minions also work when\nare offline!").startsWith("Для установки требуется свободное место\n"),"tree-specific growth wording not assigned to stone");
        flag("enabled",false);var c=Component.literal("Time Between Actions: 75s");check(SkyRuClient.component(c)==c,"disabled");
        System.out.println("PASS: Minion intervals "+checks+" assertions;719 real cards /82 intervals /decimal and grouped counts/per-glyph actions/five repeats/Shift/rarities/overrides/chat and non-tree context guards; not live-tested");
    }
}
