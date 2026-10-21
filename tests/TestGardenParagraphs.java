import java.nio.file.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Independent expectations for the second human-reviewed Garden selection. */
public class TestGardenParagraphs {
    private static int checks;
    private static void eq(String got,String expected) { if(!got.equals(expected))throw new AssertionError(got+"; expected "+expected);checks++; }
    private static void flag(String name,Object value)throws Exception {var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,style,cp)->{b.appendCodePoint(cp);return true;});return b.toString();}
    private static void paragraph(OfflineDictionary db,String source,String target) {
        eq(db.translate(source),target);
        var original=Arrays.stream(source.split("\n",-1)).map(Component::literal).map(c->(Component)c).toList();
        var translated=TooltipTranslation.components(original,false);
        var styled=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);
        for(int pass=0;pass<5;pass++) {
            eq(String.join("\n",translated.stream().map(Component::getString).toList()),target);
            eq(String.join("\n",styled.stream().map(TestGardenParagraphs::plain).toList()),target);
            eq(db.translate(target),target);
            translated=TooltipTranslation.components(translated,false);styled=TooltipTranslation.styled(styled,false);
        }
        eq(String.join("\n",original.stream().map(Component::getString).toList()),source);
    }
    public static void main(String[] args)throws Exception {
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        paragraph(db,"Upon breaking crops, nether wart,\ncocoa, wild rose, or sunflower,\nautomatically replant from materials\nin your inventory.","После сбора культур, адского нароста,\nкакао, дикой розы или подсолнуха,\nавтоматически высаживает их заново, используя материалы\nв вашем инвентаре.");
        // The older three-crop wording shares its opening; check that comma too.
        paragraph(db,"Upon breaking crops, nether wart,\nor cocoa, automatically replant from\nmaterials in your inventory.","После сбора культур, адского нароста,\nили какао-бобов автоматически сажает их снова,\nиспользуя материалы из вашего инвентаря.");
        paragraph(db,"Adds +3,000 Fuel to a refuelable\nmachine.","Добавляет +3,000 топлива в заправляемое\nустройство.");
        paragraph(db,"Cleans a 5x5 area of grass, leaves\nand flowers.","Очищает область 5x5 от травы, листьев\nи цветов.");
        for(var pair:List.of(new String[]{"Compost","компост"},new String[]{"Dung","навоз"},new String[]{"Honey Jar","банку мёда"},new String[]{"Plant Matter","растительное вещество"},new String[]{"Tasty Cheese","вкусный сыр"}))
            paragraph(db,"Garden Visitors have a +0.25%\nchance to bring "+pair[0]+".","Посетители Сада получают +0.25%\nк шансу принести "+pair[1]+".");
        for(int level=2;level<=12;level++)eq(db.translate("❣ Requires Garden Level "+level+"."),"❣ Требуется уровень Сада "+level+".");
        for(int wisdom=1;wisdom<=10;wisdom++)paragraph(db,"Gain +"+wisdom+"☯ Farming Wisdom and\n+"+(wisdom*2)+"☘ Farming Fortune.","Даёт +"+wisdom+"☯ мудрости фермерства и\n+"+(wisdom*2)+"☘ удачи фермерства.");
        eq(db.translate("Compost Speed I"),"Скорость производства компоста I");
        eq(db.translate("Ability: Nether Wart Storage  LEFT CLICK"),"Способность: Хранилище адского нароста — ЛКМ");
        eq(db.translate("Sprays your selected material onto"),"Распыляет выбранный материал");
        eq(db.translate("and flowers."),"и цветы."); // No global genitive rewrite of unrelated text.
        Style green=Style.EMPTY.withColor(0x55ff55).withClickEvent(new ClickEvent.RunCommand("/unchanged"));
        var stat=List.<Component>of(Component.literal("Gain +1☯ Farming Wisdom and"),Component.literal("+2☘ Farming Fortune.").setStyle(green));
        var statRu=TooltipTranslation.components(stat,false);
        eq(statRu.get(1).getString(),"+2☘ удачи фермерства.");
        StringDecomposer.iterateFormatted(statRu.get(1),Style.EMPTY,(i,s,cp)->{if(!s.equals(green))throw new AssertionError("stat style/event lost");checks++;return true;});
        var statSequence=TooltipTranslation.styled(stat.stream().map(Component::getVisualOrderText).toList(),false);
        eq(plain(statSequence.get(1)),"+2☘ удачи фермерства.");
        eq(stat.get(1).getString(),"+2☘ Farming Fortune.");
        Path custom=Path.of(args[0]).resolve("custom");Files.createDirectories(custom);
        Files.writeString(custom.resolve("overrides.tsv"),"and flowers.\tМои цветы\n");
        var overridden=new OfflineDictionary(custom);overridden.setRecordUnknown(false);
        eq(overridden.translate("Cleans a 5x5 area of grass, leaves\nand flowers."),"Очищает область 5x5 от травы, листьев\nМои цветы");
        System.out.println("PASS: Garden proofreading "+checks+" assertions; five repeat passes");
    }
}
