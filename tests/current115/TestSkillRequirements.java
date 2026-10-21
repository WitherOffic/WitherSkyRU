import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Observed requirement variants, independent Russian expectations and styled fragments. */
public class TestSkillRequirements {
    private static int checks;
    private static void check(String name,boolean ok){if(!ok)throw new AssertionError(name);checks++;}
    private static void eq(String got,String expected){check(got+"; expected "+expected,got.equals(expected));}
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,style,cp)->{b.appendCodePoint(cp);return true;});return b.toString();}
    private static Style at(Component row,String token){int at=row.getString().indexOf(token);check("found "+token,at>=0);int[] pos={0};Style[] style={null};StringDecomposer.iterateFormatted(row,Style.EMPTY,(i,s,cp)->{if(pos[0]==at)style[0]=s;pos[0]+=Character.charCount(cp);return true;});return style[0];}
    public static void main(String[] args)throws Exception {
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var labels=Map.of("Catacombs","Катакомбы","Combat","Бой","Farming","Фермерство","Fishing","Рыбалка","Foraging","Лесорубство","Hunting","Охота","Mining","Горное дело","Social","Общение");
        var pattern=Pattern.compile("^❣ Requires (.+) Skill ([0-9]+)\\.$");
        var observed=new TreeSet<String>();
        var items=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("item-context.json"))).getAsJsonArray();
        for(var raw:items)for(var line:raw.getAsJsonObject().getAsJsonArray("lore"))if(pattern.matcher(line.getAsString()).matches())observed.add(line.getAsString());
        check("fixed observed variant count",observed.size()==120);
        for(String source:observed) {
            var m=pattern.matcher(source);check("known family",m.matches()&&labels.containsKey(m.group(1)));
            String expected="❣ Требуется уровень навыка «"+labels.get(m.group(1))+"» "+m.group(2)+".";
            eq(db.translate(source),expected);
            Component original=Component.literal(source),translated=SkyRuClient.component(original);
            var sequence=SkyRuClient.sequence(original.getVisualOrderText());
            for(int pass=0;pass<5;pass++) {
                eq(translated.getString(),expected);eq(plain(sequence),expected);eq(db.translate(expected),expected);
                translated=SkyRuClient.component(translated);sequence=SkyRuClient.sequence(sequence);
            }
            eq(original.getString(),source);
            check("not item-name semantics",db.itemTitleEdits(source).isEmpty());
        }
        Style glyph=Style.EMPTY.withColor(0xff5555),prose=Style.EMPTY.withColor(0xaaaaaa).withClickEvent(new ClickEvent.RunCommand("/unchanged")),skill=Style.EMPTY.withColor(0x55ffff).withBold(true),number=Style.EMPTY.withColor(0x55ff55).withHoverEvent(new HoverEvent.ShowText(Component.literal("unchanged")));
        for(var pair:List.of(new String[]{"Catacombs","Катакомбы","12"},new String[]{"Combat","Бой","20"},new String[]{"Farming","Фермерство","7"},new String[]{"Fishing","Рыбалка","22"},new String[]{"Foraging","Лесорубство","30"},new String[]{"Hunting","Охота","10"},new String[]{"Mining","Горное дело","30"},new String[]{"Social","Общение","7"})) {
            Component input=Component.literal("❣ ").setStyle(glyph).append(Component.literal("Requires ").setStyle(prose)).append(Component.literal(pair[0]).setStyle(skill)).append(Component.literal(" Skill ").setStyle(prose)).append(Component.literal(pair[2]+".").setStyle(number));
            String expected="❣ Требуется уровень навыка «"+pair[1]+"» "+pair[2]+".";
            var output=SkyRuClient.component(input);eq(output.getString(),expected);
            check("alert glyph retained",at(output,"❣").equals(glyph));check("prose/action retained",at(output,"Требуется").equals(prose));
            check("skill retained",at(output,pair[1]).equals(skill));check("level/hover retained",at(output,pair[2]).equals(number));
            var sequence=SkyRuClient.sequence(input.getVisualOrderText());eq(plain(sequence),expected);
            for(int pass=0;pass<5;pass++){output=SkyRuClient.component(output);sequence=SkyRuClient.sequence(sequence);eq(output.getString(),expected);eq(plain(sequence),expected);}
            eq(input.getString(),"❣ Requires "+pair[0]+" Skill "+pair[2]+".");
            var tooltip=TooltipTranslation.components(List.of(Component.literal("RARE Test title"),input),true);
            eq(tooltip.getFirst().getString(),"RARE Test title");eq(tooltip.get(1).getString(),expected);
        }
        // Existing generic dictionary behaviour for an unknown label stays unchanged.
        eq(db.translate("❣ Requires Invented Skill 999."),"❣ Требуется навык «Invented» 999.");
        Component legacy=Component.literal("§c❣ §7Requires §bFarming§7 Skill §a7.");
        eq(SkyRuClient.component(legacy).getString(),"❣ Требуется уровень навыка «Фермерство» 7.");
        eq(legacy.getString(),"§c❣ §7Requires §bFarming§7 Skill §a7.");
        Component unsupported=Component.literal("❣ Requires ").setStyle(prose)
            .append(Component.literal("Farm").setStyle(skill)).append(Component.literal("ing").setStyle(glyph))
            .append(Component.literal(" Skill 7.").setStyle(number));
        var unsupportedRu=SkyRuClient.component(unsupported);
        check("mixed styles inside a skill fail closed",!unsupportedRu.getString().contains("«Фермерство»"));
        check("first half not flattened",at(unsupportedRu,"Farm").equals(at(unsupported,"Farm")));
        check("second half not flattened",at(unsupportedRu,"ing").equals(at(unsupported,"ing")));
        check("sequence early termination retained",!SkyRuClient.sequence(Component.literal("❣ Requires Farming Skill 7.").getVisualOrderText()).accept((i,s,cp)->false));
        // Prefix/suffix lookalikes must not invoke full-line span rewriting.
        check("unknown syntax not a reviewed title",db.itemTitleEdits("❣ Requires Farming Skill 7. extra").isEmpty());
        Path custom=Path.of(args[0]).resolve("custom");Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),"❣ Requires Farming Skill 7.\tМой уровень семь\n");
        var overridden=new OfflineDictionary(custom);overridden.setRecordUnknown(false);flag("dictionary",overridden);
        eq(overridden.translate("❣ Requires Farming Skill 7."),"Мой уровень семь");eq(SkyRuClient.component(Component.literal("❣ Requires Farming Skill 7.")).getString(),"Мой уровень семь");
        flag("dictionary",db);flag("enabled",false);eq(SkyRuClient.component(Component.literal("❣ Requires Farming Skill 7.")).getString(),"❣ Requires Farming Skill 7.");
        System.out.println("PASS: skill requirements "+checks+" assertions; 120 observed variants / 8 styled families / five repeat passes");
    }
}
