import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Observed collection requirements, unchanged Roman levels, styled terms and nearby prose. */
public class TestCollectionRequirements {
    private static int checks;
    private static void eq(String got,String expected){if(!got.equals(expected))throw new AssertionError(got+"; expected "+expected);checks++;}
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,st,cp)->{b.appendCodePoint(cp);return true;});return b.toString();}
    private static FormattedCharSequence sequence(Component c){var pieces=new ArrayList<FormattedCharSequence>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,cp)->{pieces.add(FormattedCharSequence.forward(new String(Character.toChars(cp)),s));return true;});return FormattedCharSequence.composite(pieces);}
    private static void tokenStyle(FormattedCharSequence c,String token,Style expected){int at=plain(c).indexOf(token);if(at<0)throw new AssertionError("missing token "+token);int[] cursor={0};c.accept((n,s,cp)->{int i=cursor[0];cursor[0]+=Character.charCount(cp);if(i>=at&&i<at+token.length()){if(!s.equals(expected))throw new AssertionError("style/action lost for "+token);checks++;}return true;});}
    private static void paragraph(OfflineDictionary db,String source,String expected){
        Style style=Style.EMPTY.withColor(0xaaaaaa).withClickEvent(new ClickEvent.RunCommand("/keep"));
        var originals=Arrays.stream(source.split("\n",-1)).map(s->(Component)Component.literal(s).setStyle(style)).toList();
        var components=TooltipTranslation.components(originals,false);var sequences=TooltipTranslation.styled(originals.stream().map(Component::getVisualOrderText).toList(),false);
        for(int pass=0;pass<5;pass++){
            eq(db.translate(source),expected);eq(db.translate(expected),expected);
            eq(String.join("\n",components.stream().map(Component::getString).toList()),expected);
            eq(String.join("\n",sequences.stream().map(TestCollectionRequirements::plain).toList()),expected);
            for(var row:components)sequence(row).accept((i,s,cp)->{if(!s.equals(style))throw new AssertionError("prose style/action lost");checks++;return true;});
            components=TooltipTranslation.components(components,false);sequences=TooltipTranslation.styled(sequences,false);
        }
        eq(String.join("\n",originals.stream().map(Component::getString).toList()),source);
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var labels=Map.of("Cactus","Кактус","Coal","Уголь","Gold Ingot","Золотой слиток","Hard Stone","Твёрдый камень","Honeycomb","Медовые соты","Ice","Лёд","Lotus","Лотос","Sand","Песок","Slimeball","Слизь","Umber","Умбра");
        var pattern=Pattern.compile("^❣ Requires (.+) Collection ([IVXLCDM]+)\\.$");
        var observed=new TreeSet<String>();
        for(var raw:JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("item-context.json"))).getAsJsonArray())
            for(var line:raw.getAsJsonObject().getAsJsonArray("lore"))if(pattern.matcher(line.getAsString()).matches())observed.add(line.getAsString());
        if(observed.size()!=15)throw new AssertionError("fixed observed count changed");checks++;
        Style glyph=Style.EMPTY.withColor(0xff5555),prose=Style.EMPTY.withColor(0xaaaaaa).withClickEvent(new ClickEvent.RunCommand("/unchanged")),
            name=Style.EMPTY.withColor(0x55ffff).withBold(true),level=Style.EMPTY.withColor(0x55ff55).withHoverEvent(new HoverEvent.ShowText(Component.literal("keep")));
        for(String source:observed){
            var match=pattern.matcher(source);if(!match.matches()||!labels.containsKey(match.group(1)))throw new AssertionError("unknown observed family");checks++;
            String label=labels.get(match.group(1)),roman=match.group(2),expected="❣ Требуется уровень коллекции «"+label+"» "+roman+".";
            eq(db.translate(source),expected);if(!db.itemTitleEdits(source).isEmpty())throw new AssertionError("requirement entered item-name API");checks++;
            Component original=Component.literal("❣ ").setStyle(glyph).append(Component.literal("Requires ").setStyle(prose))
                .append(Component.literal(match.group(1)).setStyle(name)).append(Component.literal(" Collection ").setStyle(prose))
                .append(Component.literal(roman).setStyle(level)).append(Component.literal(".").setStyle(prose));
            var component=SkyRuClient.component(original);var styled=SkyRuClient.sequence(original.getVisualOrderText());
            for(int pass=0;pass<5;pass++){
                eq(component.getString(),expected);eq(plain(styled),expected);eq(db.translate(expected),expected);
                tokenStyle(sequence(component),label,name);tokenStyle(styled,label,name);tokenStyle(sequence(component),roman,level);tokenStyle(styled,roman,level);
                tokenStyle(sequence(component),"Требуется",prose);tokenStyle(sequence(component),"❣",glyph);
                component=SkyRuClient.component(component);styled=SkyRuClient.sequence(styled);
            }
            eq(original.getString(),source);
            var title=List.<Component>of(Component.literal("Original name"),original);
            eq(TooltipTranslation.components(title,true).get(0).getString(),"Original name");eq(TooltipTranslation.components(title,true).get(1).getString(),expected);
        }
        paragraph(db,"Impacts your Minions, Chocolate\nFactory, Aging Items, Kat, Fann,\nGreenhouse, and The Forge.","Влияет на ваших миньонов, шоколадную\nфабрику, выдерживаемые предметы, Кэт, Fann,\nтеплицу и кузницу.");
        paragraph(db,"This arrow explodes after 2s or on\nimpact.","Эта стрела взрывается через 2с или при\nпопадании.");
        paragraph(db,"The explosion deals the arrow's\ndamage split to mobs within 2.5 blocks.","Взрыв распределяет урон стрелы\nмежду мобами в пределах 2.5 блоков.");
        eq(db.translate("impact."),"попадания.");
        Component legacy=Component.literal("§c❣ §7Requires §bLotus§7 Collection §aIX§7.");eq(SkyRuClient.component(legacy).getString(),"❣ Требуется уровень коллекции «Лотос» IX.");
        Component unsupported=Component.literal("❣ Requires ").setStyle(prose).append(Component.literal("Hard ").setStyle(name))
            .append(Component.literal("Stone").setStyle(level)).append(Component.literal(" Collection V.").setStyle(prose));
        eq(SkyRuClient.component(unsupported).getString(),unsupported.getString());eq(plain(SkyRuClient.sequence(unsupported.getVisualOrderText())),unsupported.getString());
        if(!db.itemTitleEdits("❣ Requires Invented Collection XI.").isEmpty())throw new AssertionError("unknown label guessed");checks++;
        Path custom=Path.of(args[0]).resolve("custom");Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),"❣ Requires Lotus Collection IX.\tМой уровень\n");
        var override=new OfflineDictionary(custom);override.setRecordUnknown(false);flag("dictionary",override);
        eq(SkyRuClient.component(Component.literal("❣ Requires Lotus Collection IX.")).getString(),"Мой уровень");
        Path fragments=Path.of(args[0]).resolve("fragments");Files.createDirectories(fragments);Files.writeString(fragments.resolve("overrides.tsv"),"Lotus\tМой лотос\n");
        var fragmentOverride=new OfflineDictionary(fragments);fragmentOverride.setRecordUnknown(false);flag("dictionary",fragmentOverride);
        Component fragment=Component.literal("❣ Requires ").setStyle(prose).append(Component.literal("Lotus").setStyle(name)).append(Component.literal(" Collection IX.").setStyle(prose));
        if(!SkyRuClient.component(fragment).getString().contains("Мой лотос"))throw new AssertionError("fragment override overwritten");checks++;
        flag("dictionary",db);flag("enabled",false);eq(SkyRuClient.component(Component.literal("❣ Requires Lotus Collection IX.")).getString(),"❣ Requires Lotus Collection IX.");
        System.out.println("PASS: collection requirements "+checks+" assertions; 15 observed levels / 10 collections / five repeats / nearby prose");
    }
}
