import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.*;

/** Full cooldown labels only; no rewriting the user's text or duration value. */
public class TestDurationLabels {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String text(Component c){var b=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component c){var a=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)a.add(s);return true;});return a;}
    private static String expected(String source){var m=java.util.regex.Pattern.compile("^Cooldown: ([0-9.]+)(s|m|h| hours)$").matcher(source);if(!m.matches())throw new AssertionError("new source form "+source);return "Перезарядка: "+m.group(1)+" "+switch(m.group(2)){case "s"->"с";case "m"->"мин";default->"ч";};}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var cards=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("item-context.json"))).getAsJsonArray();int snapshotRows=0;Set<String> forms=new HashSet<>(),items=new HashSet<>();
        for(var card:cards)for(var raw:card.getAsJsonObject().getAsJsonArray("lore")){String source=raw.getAsString();if(!source.startsWith("Cooldown:"))continue;snapshotRows++;forms.add(source);items.add(card.getAsJsonObject().get("id").getAsString());
            check(db.translate(source).equals(expected(source)),"known exact/pattern label");var tooltip=TooltipTranslation.components(List.of(Component.literal("Talbot's Theodolite"),Component.literal(source)),true);check(tooltip.get(1).getString().equals(expected(source)),"known label under Shift");
        }
        check(snapshotRows==160&&items.size()==157&&forms.size()==19,"current frozen duration-label scope (three cards have two cooldowns)");
        Style label=Style.EMPTY.withColor(0xaaaaaa).withHoverEvent(new HoverEvent.ShowText(Component.literal("label hover")));
        Style unit=Style.EMPTY.withColor(0xffaa00).withClickEvent(new ClickEvent.RunCommand("/original-unit"));
        for(int repeat=0;repeat<5;repeat++)for(String number:List.of("0","0.5","1","10","120","999999.999999"))for(String enUnit:List.of("s","m","h"," hours")){
            String sourceText="Cooldown: "+number+enUnit;var source=Component.literal("Cooldown: ").setStyle(label);
            for(int i=0;i<number.length();i++)source.append(Component.literal(number.substring(i,i+1)).setStyle(Style.EMPTY.withColor(0x550000+i*0x100).withInsertion("number "+i)));
            if(enUnit.startsWith(" "))source.append(Component.literal(" ").setStyle(label));source.append(Component.literal(enUnit.strip()).setStyle(unit));
            var translated=SkyRuClient.component(source);check(text(translated).equals(expected(sourceText)),"component complete duration: "+text(translated));check(source.getString().equals(sourceText),"source unchanged");
            var a=styles(source);var b=styles(translated);int before=sourceText.indexOf(number),after=text(translated).indexOf(number);for(int i=0;i<number.length();i++)check(a.get(before+i).equals(b.get(after+i)),"each numeric glyph/action unchanged");
            check(b.get(0).equals(a.get(0)),"label event");check(b.get(b.size()-1).equals(a.get(a.size()-1)),"effective unit event with parent inheritance");check(text(SkyRuClient.component(translated)).equals(expected(sourceText)),"idempotence");
            var seq=SkyRuClient.sequence(source.getVisualOrderText());check(plain(seq).equals(expected(sourceText)),"sequence same result");
        }
        var legacy=Component.literal("§7Cooldown: §a0.5§cs");check(text(SkyRuClient.component(legacy)).equals("Перезарядка: 0.5 с"),"legacy label/value/unit colours");
        var mixed=Component.literal("Cool").setStyle(label).append(Component.literal("down: 10s").setStyle(unit));check(text(SkyRuClient.component(mixed)).equals("Cooldown: 10s"),"unreviewed mixed label fails closed");
        for(String source:List.of("Cooldown: -1s","Cooldown: NaNs","Cooldown: Wither_101","Cooldown: 10 parsecs"))check(db.translate(source).substring(db.translate(source).indexOf(':')+1).equals(source.substring(source.indexOf(':')+1)),"unrecognized value/unit not replaced");
        for(String overrideKey:List.of("Cooldown: 10s","Cooldown")){Path custom=Path.of(args[0]).resolve("duration-custom-"+checks);Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),overrideKey+"\tМоя перезарядка\n");var customDb=new OfflineDictionary(custom);flag("dictionary",customDb);var translated=SkyRuClient.component(Component.literal("Cooldown: 10s"));
            check(text(translated).equals(customDb.translate("Cooldown: 10s")),"closed renderer yields to existing whole-line override/lookup policy");
            check(customDb.translate(overrideKey).equals("Моя перезарядка"),"the overridden key itself remains final");
            if(overrideKey.contains(":"))check(text(translated).equals("Моя перезарядка"),"full-line user override wins");
        }
        flag("dictionary",db);flag("translateNpcChat",true);flag("translateGameChat",true);
        for(String input:List.of("Cooldown: 10s","pickaxe","s","m","h","Wither_101")){var message=new GuiMessage(123,Component.literal(input),null,GuiMessageSource.PLAYER,null);check(SkyRuClient.npcChat(message)==message,"whole player message unchanged");}
        flag("enabled",false);var c=Component.literal("Cooldown: 10s");check(SkyRuClient.component(c)==c,"disabled unchanged");
        System.out.println("PASS: duration labels "+checks+" assertions; 160 frozen rows / 157 items / 19 unique forms, numeric per-glyph styles, units, Shift/overrides/whole-chat guards");
    }
}
