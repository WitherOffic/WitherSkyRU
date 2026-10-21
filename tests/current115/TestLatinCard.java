import java.nio.file.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Three complete reviewed paragraphs; no global Latin-to-Russian guessing. */
public class TestLatinCard {
    private static int checks;
    private static void eq(String got,String expected){if(!got.equals(expected))throw new AssertionError(got+"; expected "+expected);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String plain(FormattedCharSequence source){var b=new StringBuilder();source.accept((i,s,cp)->{b.appendCodePoint(cp);return true;});return b.toString();}
    private static void paragraph(OfflineDictionary db,String source,String target)throws Exception{
        eq(db.translate(source),target);var original=new ArrayList<Component>();int n=0;
        for(String row:source.split("\n",-1))original.add(Component.literal(row).setStyle(Style.EMPTY.withColor(0x110000+(++n)*0x1100).withClickEvent(new ClickEvent.RunCommand("/row-"+n)).withHoverEvent(new HoverEvent.ShowText(Component.literal("hover-"+n))).withInsertion("insert-"+n)));
        var translated=TooltipTranslation.components(original,false);var sequences=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);
        for(int repeat=0;repeat<5;repeat++){
            eq(String.join("\n",translated.stream().map(Component::getString).toList()),target);eq(String.join("\n",sequences.stream().map(TestLatinCard::plain).toList()),target);eq(db.translate(target),target);
            for(int i=0;i<original.size();i++){Style expected=original.get(i).getStyle();StringDecomposer.iterateFormatted(translated.get(i),Style.EMPTY,(at,s,cp)->{if(!s.equals(expected))throw new AssertionError("row Style/action changed");checks++;return true;});sequences.get(i).accept((at,s,cp)->{if(!s.equals(expected))throw new AssertionError("sequence Style/action changed");checks++;return true;});}
            translated=TooltipTranslation.components(translated,false);sequences=TooltipTranslation.styled(sequences,false);
        }
        eq(String.join("\n",original.stream().map(Component::getString).toList()),source);
        var titled=new ArrayList<Component>();titled.add(Component.literal("Original Item Title"));titled.addAll(original);eq(TooltipTranslation.components(titled,true).get(0).getString(),"Original Item Title");
        flag("enabled",false);eq(String.join("\n",TooltipTranslation.components(original,false).stream().map(Component::getString).toList()),source);flag("enabled",true);
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        paragraph(db,"The very first pickaxe model!\nInvented by the famous Thomas\nPickson.","Самая первая модель кирки!\nЕё изобрёл знаменитый Thomas\nPickson.");
        paragraph(db,"Suprema inter retia, nulla praeda\nelabitur.","Лучшая из сетей — ни одна добыча\nне ускользает.");
        paragraph(db,"Traps can be placed near the spawn\nlocation of a Shard creature, with\ntime it will eventually catch it.","Ловушки можно ставить рядом с местом\nпоявления существа-осколка; со временем\nловушка поймает его.");
        eq(db.translate("Reduces the time to capture by 50%."),"Сокращает время поимки на 50%.");eq(db.translate("Pickson."),"Pickson.");
        for(String rarity:List.of("COMMON","RARE","UNCOMMON","EPIC","LEGENDARY","SPECIAL"))eq(db.translate(rarity),rarity);
        Path custom=Path.of(args[0]).resolve("custom");Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),"elabitur.\tСВОЯ СТРОКА\n");var override=new OfflineDictionary(custom);override.setRecordUnknown(false);flag("dictionary",override);eq(TooltipTranslation.components(List.of(Component.literal("elabitur.")),false).get(0).getString(),"СВОЯ СТРОКА");flag("dictionary",db);
        eq(db.translate("Unobserved Latin quote ZX-891!"),"Unobserved Latin quote ZX-891!");
        System.out.println("PASS: Latin/inventor/trap prose "+checks+" assertions; three complete paragraphs / five repeats / row styles and actions / overrides / Shift / disabled / names and rarities");
    }
}
