import java.nio.file.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Independent paragraph expectations, styles and repeat passes; no game launch. */
public class TestReadableParagraphs {
    private static int checks;
    private static void check(String name, boolean ok) { if (!ok) throw new AssertionError(name); checks++; }
    private static void flag(String name, Object value) throws Exception {
        var f=SkyRuClient.class.getDeclaredField(name); f.setAccessible(true); f.set(null,value);
    }
    private static String plain(FormattedCharSequence row) {
        var b=new StringBuilder(); row.accept((i,s,cp)->{b.appendCodePoint(cp); return true;}); return b.toString();
    }
    private static void eq(String name, String got, String expected) { check(name+": "+got+"; expected "+expected,got.equals(expected)); }
    private static Style styleAt(Component row,String token) {
        int at=row.getString().indexOf(token); check("token found "+token,at>=0); Style[] out={null};
        int[] position={0};
        StringDecomposer.iterateFormatted(row,Style.EMPTY,(i,s,cp)->{if(position[0]==at)out[0]=s;position[0]+=Character.charCount(cp);return true;}); return out[0];
    }
    private static final String SOURCE="Fill your composter with Crops, like\nWheat or Enchanted Potato, to turn\nthem into Organic Matter. Organic\nmatter is used to create Compost.";
    private static final String TARGET="Заполните компостер культурами, например\nпшеницей или зачарованным картофелем, чтобы превратить\nих в органическое сырьё.\nИз него производится компост.";
    private static void paragraph(OfflineDictionary db,String source,String target) {
        eq("plain paragraph",db.translate(source),target);
        List<Component> rows=new ArrayList<>(); rows.add(Component.literal("COMMON Test title"));
        for(var line:source.split("\n",-1))rows.add(Component.literal(line));
        var translated=TooltipTranslation.components(rows,true);
        eq("components paragraph",String.join("\n",translated.subList(1,translated.size()).stream().map(Component::getString).toList()),target);
        eq("Shift title",translated.getFirst().getString(),"COMMON Test title");
        var styled=TooltipTranslation.styled(rows.stream().map(Component::getVisualOrderText).toList(),true);
        eq("styled paragraph",String.join("\n",styled.subList(1,styled.size()).stream().map(TestReadableParagraphs::plain).toList()),target);
        for(int pass=0;pass<5;pass++) {
            translated=TooltipTranslation.components(translated,true); styled=TooltipTranslation.styled(styled,true);
            eq("repeat plain",db.translate(target),target);
            eq("repeat components",String.join("\n",translated.subList(1,translated.size()).stream().map(Component::getString).toList()),target);
            eq("repeat styled",String.join("\n",styled.subList(1,styled.size()).stream().map(TestReadableParagraphs::plain).toList()),target);
        }
        eq("source intact",String.join("\n",rows.subList(1,rows.size()).stream().map(Component::getString).toList()),source);
    }
    public static void main(String[] args) throws Exception {
        var db=new OfflineDictionary(Path.of(args[0])); db.setRecordUnknown(false);
        flag("dictionary",db); flag("enabled",true); flag("onlyHypixel",false);
        paragraph(db,SOURCE,TARGET);
        for(var n:List.of("4,000","4000","4.000","12,345","42"))
            paragraph(db,"The composter must have "+n+"\norganic matter stored to start\ncreating compost.","Компостеру нужно "+n+"\nед. органического сырья для запуска\nпроизводства компоста.");
        paragraph(db,"A key component needed by the\nProfessor to repair Automaton Prime\nin the Lost Precursor City!","Важная деталь, необходимая\nПрофессору для ремонта Automaton Prime\nв Затерянном городе предтеч!");
        paragraph(db,"  /etable, /potionbag, and /quiver","  /etable, /potionbag и /quiver");
        paragraph(db,"Mass-produced for miners who\nparticipated in the Many More Major\nand Minor Modifications Meticulously\nMade to Modernize Mining alpha test.","Массово выпускается для шахтёров, участвовавших\nв альфа-тесте обновления Many More Major\nand Minor Modifications Meticulously\nMade to Modernize Mining.");
        eq("heading Crops",db.translate("Crops"),"Культуры"); eq("heading Compost",db.translate("Compost"),"Компост");
        eq("no blind connector deletion",db.translate("A or or B"),"A or or B");
        Style grey=Style.EMPTY.withColor(0xaaaaaa), green=Style.EMPTY.withColor(0x55ff55).withBold(true)
            .withClickEvent(new ClickEvent.RunCommand("/unchanged")), gold=Style.EMPTY.withColor(0xffaa00);
        var rows=List.<Component>of(Component.literal("COMMON Test title"),
            Component.literal("Fill your composter with ").setStyle(grey).append(Component.literal("Crops").setStyle(green)).append(Component.literal(", like").setStyle(grey)),
            Component.literal("Wheat").setStyle(grey).append(Component.literal(" or ").setStyle(grey)).append(Component.literal("Enchanted Potato").setStyle(green)).append(Component.literal(", to turn").setStyle(grey)),
            Component.literal("them into ").setStyle(grey).append(Component.literal("Organic Matter").setStyle(gold)).append(Component.literal(". Organic").setStyle(grey)),
            Component.literal("matter is used to create ").setStyle(grey).append(Component.literal("Compost").setStyle(gold)).append(Component.literal(".").setStyle(grey)));
        var translated=TooltipTranslation.components(rows,true);
        eq("coloured component prose",String.join("\n",translated.subList(1,5).stream().map(Component::getString).toList()),TARGET);
        check("Crops style and click",styleAt(translated.get(1),"культурами").equals(green));
        check("Potato style and click",styleAt(translated.get(2),"зачарованным картофелем").equals(green));
        check("Organic Matter style",styleAt(translated.get(3),"органическое сырьё").equals(gold));
        check("Compost style",styleAt(translated.get(4),"компост").equals(gold));
        var sequence=TooltipTranslation.styled(rows.stream().map(Component::getVisualOrderText).toList(),true);
        eq("coloured sequence prose",String.join("\n",sequence.subList(1,5).stream().map(TestReadableParagraphs::plain).toList()),TARGET);
        for(int pass=0;pass<5;pass++) {
            translated=TooltipTranslation.components(translated,true); sequence=TooltipTranslation.styled(sequence,true);
            eq("repeat coloured components",String.join("\n",translated.subList(1,5).stream().map(Component::getString).toList()),TARGET);
            eq("repeat coloured sequences",String.join("\n",sequence.subList(1,5).stream().map(TestReadableParagraphs::plain).toList()),TARGET);
        }
        Style cyan=Style.EMPTY.withColor(0x55ffff).withHoverEvent(new HoverEvent.ShowText(Component.literal("unchanged number")));
        var numeric=List.<Component>of(Component.literal("COMMON Test title"),
            Component.literal("The composter must have ").setStyle(grey).append(Component.literal("4,000").setStyle(cyan)),
            Component.literal("organic matter stored to start").setStyle(grey),Component.literal("creating compost.").setStyle(grey));
        var numericRu=TooltipTranslation.components(numeric,true);
        eq("numeric coloured prefix",numericRu.get(1).getString(),"Компостеру нужно 4,000");
        check("numeric style and hover",styleAt(numericRu.get(1),"4,000").equals(cyan));
        var numericSequence=TooltipTranslation.styled(numeric.stream().map(Component::getVisualOrderText).toList(),true);
        eq("numeric coloured sequence",plain(numericSequence.get(1)),"Компостеру нужно 4,000");
        eq("numeric genitive continuation",plain(numericSequence.get(3)),"производства компоста.");
        eq("numeric source intact",numeric.get(1).getString(),"The composter must have 4,000");
        Path custom=Path.of(args[0]).resolve("custom"); Files.createDirectories(custom);
        Files.writeString(custom.resolve("overrides.tsv"),"Fill your composter with Crops, like\tМой заголовок\nCrops\tМои культуры\n");
        var overrides=new OfflineDictionary(custom); overrides.setRecordUnknown(false);
        check("custom full row takes priority",overrides.translate(SOURCE).startsWith("Мой заголовок\n"));
        eq("custom term",overrides.translate("Crops"),"Мои культуры");
        System.out.println("PASS: readable paragraphs "+checks+" assertions; five repeat passes");
    }
}
