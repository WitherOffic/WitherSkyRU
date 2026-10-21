import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.util.*;

/** Observed updated document plus labelled synthetic style/identity/boundary cases. */
public class TestDocumentUpdate {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xaaaaaa).withInsertion("body");
    private static final Style TERM=Style.EMPTY.withColor(0x55ffff).withHoverEvent(new HoverEvent.ShowText(Component.literal("term")));
    private static final Style NUMBER=Style.EMPTY.withColor(0xffff55).withClickEvent(new ClickEvent.RunCommand("/number"));
    private static final String[][] PARAGRAPHS={
        {"Consume to apply a pungent smell\nupon yourself, confusing The\nMatriarch in the Crimson Isle into\nreleasing new pearls.","Используйте духи: резкий запах\nсобьёт с толку\nМатриарха на Багровом острове, заставив\nвыпустить новые жемчужины."},
        {"You can expand your Accessory Bag\nby doing any of the following:","Расширить сумку аксессуаров\nможно следующими способами:"},
        {"Increasing your Redstone Collection.","Повышайте уровень коллекции редстоуна."},
        {"Purchasing slots for Coins from\nJacobus in the Combat Settlement in\nthe Hub.","Покупайте ячейки за монеты у\nЯкобуса в Поселении воинов\nв Хабе."},
        {"Unlocking slots from Elizabeth in the\nCommunity Center in the Hub.","Открывайте ячейки у Элизабет в\nОбщественном центре в Хабе."}
    };
    private static final Map<String,String> TERMS=Map.ofEntries(Map.entry("The",""),Map.entry("Matriarch","Матриарха"),
        Map.entry("Crimson Isle","Багровом острове"),Map.entry("Redstone Collection","коллекции редстоуна"),
        Map.entry("Coins","монеты"),Map.entry("Jacobus","Якобуса"),Map.entry("Combat Settlement","Поселении воинов"),
        Map.entry("Elizabeth","Элизабет"),Map.entry("Community Center","Общественном центре"),Map.entry("Hub","Хабе"));
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);checks++;}
    private static void flag(String key,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,v);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static String text(List<Component> rows){return String.join("\n",rows.stream().map(Component::getString).toList());}
    private static Component styled(String row){var c=Component.empty();int at=0;while(at<row.length()){
        String term=null;for(String key:TERMS.keySet())if(row.startsWith(key,at)){term=key;break;}
        if(term!=null){c.append(Component.literal(term).setStyle(TERM));at+=term.length();}
        else {c.append(Component.literal(row.substring(at,at+1)).setStyle(BODY));at++;}
    }return c;}
    private static void semanticStyles(List<Component> rows){for(var row:rows){var line=row.getString();var expected=new ArrayList<Style>(Collections.nCopies(line.length(),BODY));
        for(String word:TERMS.values()){if(word.isEmpty())continue;int at=-1;while((at=line.indexOf(word,at+1))>=0)for(int i=at;i<at+word.length();i++)expected.set(i,TERM);}
        int[] pos={0};StringDecomposer.iterateFormatted(row,Style.EMPTY,(i,s,p)->{for(int k=0;k<Character.charCount(p);k++)check(s.equals(expected.get(pos[0]++)),"semantic style: "+line);return true;});
    }}
    private static void span(Component c,String text,Style wanted){var styles=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)styles.add(s);return true;});
        int at=c.getString().indexOf(text);check(at>=0,"missing span: "+text);for(int i=at;i<at+text.length();i++)check(styles.get(i).equals(wanted),"span event/style: "+text);}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        for(int n=0;n<PARAGRAPHS.length;n++){
            var source=Arrays.stream(PARAGRAPHS[n][0].split("\n")).map(TestDocumentUpdate::styled).toList();
            var result=TooltipTranslation.components(source,false);
            for(int repeat=0;repeat<5;repeat++){
                check(text(result).equals(PARAGRAPHS[n][1]),"styled full paragraph: "+text(result));semanticStyles(result);
                var sequence=TooltipTranslation.styled(source.stream().map(Component::getVisualOrderText).toList(),false);
                check(String.join("\n",sequence.stream().map(TestDocumentUpdate::plain).toList()).equals(PARAGRAPHS[n][1]),"sequence paragraph");
                result=TooltipTranslation.components(result,false);
            }
            check(text(source).equals(PARAGRAPHS[n][0]),"source immutable");
            check(db.translate(PARAGRAPHS[n][0]).equals(PARAGRAPHS[n][1]),"plain paragraph: "+db.translate(PARAGRAPHS[n][0]));
            var custom=Path.of(args[0]).resolve("paragraph-override-"+n);Files.createDirectories(custom);
            Files.writeString(custom.resolve("overrides.tsv"),PARAGRAPHS[n][0].split("\n")[0]+"\tМоя фраза\n");
            var own=new OfflineDictionary(custom);own.setRecordUnknown(false);flag("dictionary",own);
            check(!text(TooltipTranslation.components(source,false)).equals(PARAGRAPHS[n][1]),"override yields");flag("dictionary",db);
        }
        var ratio=Component.empty().append(Component.literal("Bits Cost: ").setStyle(TERM)).append(Component.literal("833").setStyle(NUMBER)).append(Component.literal(" Coins per: bit").setStyle(BODY));
        var ratioResult=SkyRuClient.component(ratio);check(ratioResult.getString().equals("Монет за бит: 833"),"ratio is not number of Bits: "+ratioResult.getString());span(ratioResult,"833",NUMBER);span(ratioResult,"Монет за бит:",TERM);
        for(String[] price:List.of(new String[]{"1,200 Bits Coins per bit: 833.3","1,200 битов Монет за бит: 833.3"},new String[]{"3 Day Avg. Price: 1,104,541.4 Coins","Средняя цена за 3 дня: 1,104,541.4 монет"})){
            check(db.translate(price[0]).equals(price[1]),"price plain: "+db.translate(price[0]));check(SkyRuClient.component(Component.literal(price[0])).getString().equals(price[1]),"price component");
        }
        var notices=new ChatNotificationTranslator(db);check(notices.documentNotificationFamilyCount()==5,"notification families");
        var examples=new LinkedHashMap<String,String>();
        examples.put("RARE CROP! Cactus Flower (+6.5%)","РЕДКИЙ УРОЖАЙ! Цветок кактуса (+6.5%)");
        examples.put("Cultivating II on your Euclid's Wheat Sickle Mk. I was upgraded to Cultivating III!","Возделывание II на предмете «Пшеничный серп Евклида, модификация I» повышено до Возделывание III!");
        examples.put("EWW! 2  Pest have spawned in Plot - 4!","ФУ! 2  вредителя появились на участке № 4!");
        examples.put("You don't have enough of the required item! (x2)","Не хватает нужного предмета! (x2)");
        examples.put("You earned 506 GEXP + 1,518 Event EXP from playing SkyBlock!","Вы получили 506 опыта гильдии + 1,518 опыта события за игру в SkyBlock!");
        for(var e:examples.entrySet()){
            var source=Component.literal("[03:46:45] "+e.getKey()).setStyle(BODY);
            var result=notices.translate(source,GuiMessageSource.SYSTEM_SERVER,false);check(result.getString().equals("[03:46:45] "+e.getValue()),"notification: "+result.getString());
            for(int i=0;i<5;i++){check(notices.translate(result,GuiMessageSource.SYSTEM_SERVER,false)==result,"notification idempotent");
                check(notices.translate(source,GuiMessageSource.PLAYER,false)==source,"player identity");check(notices.translate(source,GuiMessageSource.SYSTEM_SERVER,true)==source,"signed identity");check(notices.translate(source,GuiMessageSource.SYSTEM_CLIENT,false)==source,"client unknown origin untouched");}
            check(source.getString().equals("[03:46:45] "+e.getKey()),"notification original unchanged");
        }
        var exp=Component.empty().append(Component.literal("You earned ").setStyle(BODY)).append(Component.literal("506").setStyle(NUMBER))
            .append(Component.literal(" GEXP").setStyle(TERM)).append(Component.literal(" + ").setStyle(BODY)).append(Component.literal("1,518").setStyle(NUMBER))
            .append(Component.literal(" Event EXP").setStyle(TERM)).append(Component.literal(" from playing ").setStyle(BODY)).append(Component.literal("SkyBlock!").setStyle(TERM));
        var translated=notices.translate(exp,GuiMessageSource.SYSTEM_SERVER,false);check(translated.getString().equals(examples.get(exp.getString())),"styled exp");span(translated,"506",NUMBER);span(translated,"1,518",NUMBER);span(translated,"опыта гильдии",TERM);span(translated,"SkyBlock",TERM);
        for(int level=0;level<9;level++)for(String tool:List.of("Cactus Knife Mk. I","Euclid's Wheat Sickle Mk. III","Blessed Advanced Gardening Hoe","Diamond Pickaxe")){
            String[] roman={"I","II","III","IV","V","VI","VII","VIII","IX","X"};String line="Cultivating "+roman[level]+" on your "+tool+" was upgraded to Cultivating "+roman[level+1]+"!";
            var source=Component.literal(line);check(notices.translate(source,GuiMessageSource.SYSTEM_SERVER,false).getString().equals("Возделывание "+roman[level]+" на предмете «"+db.translateForSearch(tool)+"» повышено до Возделывание "+roman[level+1]+"!"),"all known tool levels: "+line);
        }
        for(String line:List.of("Cultivating II on your unknown user nick was upgraded to Cultivating III!","Cultivating IX on your Diamond Pickaxe was upgraded to Cultivating II!","RARE CROP! Wither_101 (+6.5%)","EWW! 0  Pests have spawned in Plot - 4!","Wither_101: pickaxe")){
            var c=Component.literal(line);check(notices.translate(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"malformed/unobserved/player untouched: "+line);}
        var custom=Path.of(args[0]).resolve("notification-override");Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),"Cultivating\tМой термин\n");
        var own=new OfflineDictionary(custom);own.setRecordUnknown(false);var c=Component.literal(examples.keySet().stream().filter(s->s.startsWith("Cultivating")).findFirst().orElseThrow());
        check(new ChatNotificationTranslator(own).translate(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"notification override");
        flag("enabled",false);check(TooltipTranslation.components(List.of(styled(PARAGRAPHS[0][0].split("\n")[0])),false).getFirst().getString().startsWith("Consume"),"disabled");
        System.out.println("PASS: Updated document "+checks+" assertions; five paragraph groups, prices, five server notice families, style/events/counts, overrides and player protection");
    }
}
