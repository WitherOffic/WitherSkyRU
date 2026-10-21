import java.nio.file.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Closed observed mining/arrow paragraphs: conditions, numbers, repeats, styles, overrides. */
public class TestMiningArrowProse {
    private static int checks;
    private static void eq(String got,String expected){if(!got.equals(expected))throw new AssertionError(got+"; expected "+expected);checks++;}
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,st,cp)->{b.appendCodePoint(cp);return true;});return b.toString();}
    private static FormattedCharSequence sequence(Component c){var pieces=new ArrayList<FormattedCharSequence>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,cp)->{pieces.add(FormattedCharSequence.forward(new String(Character.toChars(cp)),s));return true;});return FormattedCharSequence.composite(pieces);}
    private static void paragraph(OfflineDictionary db,String source,String expected){
        var styles=List.of(Style.EMPTY.withColor(0xaaaaaa).withClickEvent(new ClickEvent.RunCommand("/keep")),
            Style.EMPTY.withColor(0x55ffff).withBold(true).withHoverEvent(new HoverEvent.ShowText(Component.literal("keep"))));
        var originals=new ArrayList<Component>();int n=0;
        for(var row:source.split("\n",-1))originals.add(Component.literal(row).setStyle(styles.get(n++%2)));
        var components=TooltipTranslation.components(originals,false);var sequences=TooltipTranslation.styled(originals.stream().map(Component::getVisualOrderText).toList(),false);
        for(int pass=0;pass<5;pass++){
            eq(db.translate(source),expected);eq(db.translate(expected),expected);
            eq(String.join("\n",components.stream().map(Component::getString).toList()),expected);
            eq(String.join("\n",sequences.stream().map(TestMiningArrowProse::plain).toList()),expected);
            for(int row=0;row<components.size();row++){
                Style expectedStyle=styles.get(row%2);
                sequence(components.get(row)).accept((i,s,cp)->{if(!s.equals(expectedStyle))throw new AssertionError("component style/action lost");checks++;return true;});
                sequences.get(row).accept((i,s,cp)->{if(!s.equals(expectedStyle))throw new AssertionError("sequence style/action lost");checks++;return true;});
            }
            components=TooltipTranslation.components(components,false);sequences=TooltipTranslation.styled(sequences,false);
        }
        eq(String.join("\n",originals.stream().map(Component::getString).toList()),source);
        var titled=new ArrayList<Component>();titled.add(Component.literal("Original item"));titled.addAll(originals);
        eq(TooltipTranslation.components(titled,true).get(0).getString(),"Original item");
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        paragraph(db,"Replaces the damage reduction from\nthe Piercing enchant to Skewer.","Заменяет снижение урона от\nзачарования «Пронзающая стрела» эффектом «Насквозь».");
        paragraph(db,"Passive: Skewer\nHits through Piercing deal +7.5%\ndamage than the previous one, up to\n5 victims.","Пассивный эффект: Насквозь\nПопадания с «Пронзающей стрелой» наносят на +7.5%\nбольше урона, чем предыдущее; не более\n5 целей.");
        paragraph(db,"Grants +25 Mining Spread while\nMayor Cole's Mining Fiesta perk is\nactive.","Даёт +25 широты добычи,\nпока бонус мэра Коула «Горная фиеста»\nактивен.");
        paragraph(db,"Grants +50%  Mining Spread\ntowards Blocks.","Даёт +50%  широты добычи\nпри добыче блоков.");
        paragraph(db,"Convert every {1} Speed you\nhave above 100 into +1 Block\nFortune.","Преобразует каждые {1} скорости\nсверх 100 в +1 удачи добычи\nблоков.");
        paragraph(db,"Right-click your summoned pet to\nride it! Moves faster based on your\n Speed.","Нажмите ПКМ по призванному питомцу, чтобы\nоседлать его! Скорость передвижения зависит от вашей\n скорости.");
        paragraph(db,"Every {0} seconds, the next\nGemstone you mine gives 2x drops","Каждые {0} с следующий добытый\nдрагоценный камень даёт добычу x2.");
        eq(db.translate("towards Blocks."),"к блокам.");eq(db.translate("Fortune."),"удачи.");
        eq(db.translate("Piercing I"),"Пронзающая стрела I");
        Path custom=Path.of(args[0]).resolve("custom");Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),"towards Blocks.\tМои блоки.\n");
        var override=new OfflineDictionary(custom);override.setRecordUnknown(false);flag("dictionary",override);
        var customRows=List.<Component>of(Component.literal("Grants +50%  Mining Spread"),Component.literal("towards Blocks."));
        eq(TooltipTranslation.components(customRows,false).get(1).getString(),"Мои блоки.");
        eq(plain(TooltipTranslation.styled(customRows.stream().map(Component::getVisualOrderText).toList(),false).get(1)),"Мои блоки.");
        flag("dictionary",db);flag("enabled",false);
        eq(String.join("\n",TooltipTranslation.components(customRows,false).stream().map(Component::getString).toList()),"Grants +50%  Mining Spread\ntowards Blocks.");
        System.out.println("PASS: mining/arrow prose "+checks+" assertions; 7 paragraphs / five repeats / styles / overrides / Shift / disabled");
    }
}
