import java.nio.file.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Independent expectations for reviewed, neighbouring-line grammatical fixes. */
public class TestNeighbourProse {
    private static int checks;
    private static void eq(String got,String expected){if(!got.equals(expected))throw new AssertionError(got+"; expected "+expected);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,st,cp)->{b.appendCodePoint(cp);return true;});return b.toString();}
    private static void paragraph(OfflineDictionary db,String source,String target){
        eq(db.translate(source),target);
        var originals=new ArrayList<Component>();
        var expected=target.split("\n",-1);
        int row=0;
        for(String line:source.split("\n",-1)){
            Style style=Style.EMPTY.withColor(0x550000+row*0x001111).withClickEvent(new ClickEvent.RunCommand("/row"+row));
            originals.add(Component.literal(line).setStyle(style));row++;
        }
        var components=TooltipTranslation.components(originals,false);
        var sequences=TooltipTranslation.styled(originals.stream().map(Component::getVisualOrderText).toList(),false);
        for(int pass=0;pass<5;pass++){
            eq(String.join("\n",components.stream().map(Component::getString).toList()),target);
            eq(String.join("\n",sequences.stream().map(TestNeighbourProse::plain).toList()),target);
            eq(db.translate(target),target);
            for(int i=0;i<components.size();i++){
                eq(components.get(i).getString(),expected[i]);
                Style wanted=originals.get(i).getStyle();
                StringDecomposer.iterateFormatted(components.get(i),Style.EMPTY,(n,s,cp)->{if(!s.equals(wanted))throw new AssertionError("row style/action lost");checks++;return true;});
                sequences.get(i).accept((n,s,cp)->{if(!s.equals(wanted))throw new AssertionError("sequence style/action lost");checks++;return true;});
            }
            components=TooltipTranslation.components(components,false);sequences=TooltipTranslation.styled(sequences,false);
        }
        eq(String.join("\n",originals.stream().map(Component::getString).toList()),source);
        var withTitle=new ArrayList<Component>();withTitle.add(Component.literal("Original Title"));withTitle.addAll(originals);
        eq(TooltipTranslation.components(withTitle,true).get(0).getString(),"Original Title");
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        paragraph(db,"Applies the Ambered reforge when\ncombined with a Mining Tool.","Накладывает перековку «Янтарный» при\nприменении к инструменту для добычи.");
        paragraph(db,"Put a bunch of Amber together and\nit'll make more Amber - Throin the\nForger.","Соедините несколько кусков янтаря, и\nполучится больше янтаря — Throin,\nкузнец.");
        paragraph(db,"Find Goblins from mining 40% more\noften.","При добыче находите гоблинов на 40%\nчаще.");
        paragraph(db,"Combine x9 of this item at the\nThaumaturgist in the Hub to unlock the\nForceful power.","Объедините 9 таких предметов у\nтауматурга в Хабе, чтобы открыть\nНапористую силу.");
        paragraph(db,"Grants +1 extra Accessory Bag\nslots.","Увеличивает на +1 вместимость сумки аксессуаров\nв ячейках.");
        paragraph(db,"Use on a Tree with an active Tree\nProtection Order to lather it with\nHoneycomb!","Используйте на дереве с действующим\nприказом о защите дерева, чтобы покрыть его\nмедовыми сотами!");
        paragraph(db,"Upon use, a unique  Critter will\nINSTANTLY be attracted to the\nHoneycomb!","При использовании уникальный  зверёк\nМГНОВЕННО придёт к\nмедовым сотам!");
        for(String time:List.of("15","30","60")){
            String first="After "+time+"m, a unique  Critter will be";
            paragraph(db,first+"\nattracted to the Honeycomb!","Через "+time+" мин уникальный  зверёк\nпридёт к медовым сотам!");
        }
        paragraph(db,"Click another player to initiate a\ngame of Four in a Row!","Нажмите на другого игрока, чтобы начать\nигру «Четыре в ряд»!");
        for(String amount:List.of("0","1","2.5","5","7.5","10")){
            String first="Grants +"+amount+" Treasure Chance while";
            paragraph(db,first+"\nunderwater.","Даёт +"+amount+" шанса сокровищ\nпод водой.");
        }
        eq(db.translate("Honeycomb!"),"Медовые соты!");eq(db.translate("Forceful power."),"Напористая сила.");
        eq(db.translate("often."),"часто.");eq(db.translate("slots."),"ячейки.");eq(db.translate("Forger."),"кузнеца.");
        Style honey=Style.EMPTY.withColor(0xffaa00).withClickEvent(new ClickEvent.RunCommand("/honey"));
        Style punctuation=Style.EMPTY.withColor(0xaaaaaa);
        var honeyRows=List.<Component>of(Component.literal("Use on a Tree with an active Tree"),
            Component.literal("Protection Order to lather it with"),
            Component.empty().append(Component.literal("Honeycomb").setStyle(honey)).append(Component.literal("!").setStyle(punctuation)));
        var honeyRu=TooltipTranslation.components(honeyRows,false);
        eq(honeyRu.get(2).getString(),"медовыми сотами!");
        StringDecomposer.iterateFormatted(honeyRu.get(2),Style.EMPTY,(i,s,cp)->{if(!s.equals(cp=='!'?punctuation:honey))throw new AssertionError("honey term/punctuation style lost");checks++;return true;});
        var honeySequence=TooltipTranslation.styled(honeyRows.stream().map(Component::getVisualOrderText).toList(),false);
        eq(plain(honeySequence.get(2)),"медовыми сотами!");
        honeySequence.get(2).accept((i,s,cp)->{if(!s.equals(cp=='!'?punctuation:honey))throw new AssertionError("styled honey term/punctuation lost");checks++;return true;});
        var slotRows=List.<Component>of(Component.empty().append(Component.literal("Grants ").setStyle(punctuation))
            .append(Component.literal("+1").setStyle(honey)).append(Component.literal(" extra ").setStyle(punctuation))
            .append(Component.literal("Accessory Bag").setStyle(Style.EMPTY.withColor(0xaa00aa))),Component.literal("slots."));
        eq(String.join("\n",TooltipTranslation.components(slotRows,false).stream().map(Component::getString).toList()),"Увеличивает на +1 вместимость сумки аксессуаров\nв ячейках.");
        eq(String.join("\n",TooltipTranslation.styled(slotRows.stream().map(Component::getVisualOrderText).toList(),false).stream().map(TestNeighbourProse::plain).toList()),"Увеличивает на +1 вместимость сумки аксессуаров\nв ячейках.");
        var slotRu=TooltipTranslation.components(slotRows,false);
        String slotText=slotRu.get(0).getString();
        int numberAt=slotText.indexOf("+1"),bagAt=slotText.indexOf("сумки аксессуаров");
        int[] cursor={0};
        StringDecomposer.iterateFormatted(slotRu.get(0),Style.EMPTY,(localIndex,s,cp)->{
            int i=cursor[0];cursor[0]+=Character.charCount(cp);
            if(i>=numberAt && i<numberAt+2 && !s.equals(honey))throw new AssertionError("slot number style/action lost");
            if(i>=bagAt && !s.equals(Style.EMPTY.withColor(0xaa00aa)))throw new AssertionError("bag term colour lost");
            checks++;return true;
        });
        for(int pass=0;pass<5;pass++){
            eq(String.join("\n",TooltipTranslation.components(slotRu,false).stream().map(Component::getString).toList()),"Увеличивает на +1 вместимость сумки аксессуаров\nв ячейках.");
            eq(TooltipTranslation.components(honeyRu,false).get(2).getString(),"медовыми сотами!");
        }
        eq(slotRows.get(0).getString(),"Grants +1 extra Accessory Bag");
        if(!EditorialContext.edits(List.of("Unknown context","Honeycomb!")).isEmpty())throw new AssertionError("unrelated context rewritten");checks++;
        Path custom=Path.of(args[0]).resolve("custom");Files.createDirectories(custom);
        Files.writeString(custom.resolve("overrides.tsv"),"Honeycomb!\tМои соты\n");
        var override=new OfflineDictionary(custom);override.setRecordUnknown(false);
        eq(override.translate("Use on a Tree with an active Tree\nProtection Order to lather it with\nHoneycomb!"),"Используйте на дереве с действующим\nприказом о защите дерева, чтобы покрыть его\nМои соты");
        System.out.println("PASS: neighbour prose "+checks+" assertions; five repeat passes; original standalone terms preserved");
    }
}
