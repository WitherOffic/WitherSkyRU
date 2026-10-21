import java.nio.file.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Exact synthetic multicolour semantic runs: no statistical coverage claim. */
public class TestMiningStyles {
    private static int checks;
    private static void eq(String got,String expected){if(!got.equals(expected))throw new AssertionError(got+"; expected "+expected);checks++;}
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    private static FormattedCharSequence sequence(Component c){var out=new ArrayList<FormattedCharSequence>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,cp)->{out.add(FormattedCharSequence.forward(new String(Character.toChars(cp)),s));return true;});return FormattedCharSequence.composite(out);}
    private static final String[][] TERMS={
        {"Пронзающая стрела","Насквозь"},{"Насквозь","Пронзающей стрелой"},{"широты добычи","Коула","Горная фиеста"},
        {"широты добычи","блоков"},{"скорости","удачи добычи","блоков"},{"скорости"},{"драгоценный камень"}};
    private static final String[][] VALUES={{},{"+7.5%","5"},{"+25"},{"+50%"},{"{1}","100","+1"},{},{"{0}","x2"}};
    private static void style(FormattedCharSequence row,int kind){
        String text=AuditMiningStyles.plain(row);var expected=new ArrayList<Style>(Collections.nCopies(text.length(),AuditMiningStyles.PROSE));
        for(int family=0;family<2;family++)for(String token:(family==0?TERMS[kind]:VALUES[kind])){
            int at=text.indexOf(token);while(at>=0){for(int i=at;i<at+token.length();i++)expected.set(i,family==0?AuditMiningStyles.TERM:AuditMiningStyles.VALUE);at=text.indexOf(token,at+token.length());}
        }
        int[] cursor={0};row.accept((local,s,cp)->{for(int n=0;n<Character.charCount(cp);n++){
            if(!s.equals(expected.get(cursor[0]++)))throw new AssertionError("semantic style/click/hover changed: "+text);checks++;
        }return true;});
    }
    private static List<Component> originals(int index){return Arrays.stream(AuditMiningStyles.CASES[index][0].split("\n")).map(AuditMiningStyles::row).toList();}
    private static String componentText(List<Component> rows){return String.join("\n",rows.stream().map(Component::getString).toList());}
    private static String styledText(List<FormattedCharSequence> rows){return String.join("\n",rows.stream().map(AuditMiningStyles::plain).toList());}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        for(int index=0;index<AuditMiningStyles.CASES.length;index++){
            var original=originals(index);String target=AuditMiningStyles.CASES[index][1];
            var components=TooltipTranslation.components(original,false);
            var styled=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);
            for(int pass=0;pass<5;pass++){
                eq(componentText(components),target);eq(styledText(styled),target);eq(db.translateForSearch(target),target);
                for(var row:components)style(sequence(row),index);for(var row:styled)style(row,index);
                components=TooltipTranslation.components(components,false);styled=TooltipTranslation.styled(styled,false);
            }
            eq(componentText(original),AuditMiningStyles.CASES[index][0]);
            var withTitle=new ArrayList<Component>();withTitle.add(Component.literal("Enchanted Book"));withTitle.addAll(original);
            var protectedRows=TooltipTranslation.components(withTitle,true);eq(protectedRows.get(0).getString(),"Enchanted Book");eq(componentText(protectedRows.subList(1,protectedRows.size())),target);
            var protectedStyled=TooltipTranslation.styled(withTitle.stream().map(Component::getVisualOrderText).toList(),true);
            eq(AuditMiningStyles.plain(protectedStyled.get(0)),"Enchanted Book");eq(styledText(protectedStyled.subList(1,protectedStyled.size())),target);
            eq(TooltipTranslation.components(withTitle,false).get(0).getString(),"Зачарованная книга");
            var legacy=new ArrayList<Component>();
            for(var row:original){var b=new StringBuilder();sequence(row).accept((i,s,cp)->{b.append(s.equals(AuditMiningStyles.TERM)?"§b":s.equals(AuditMiningStyles.VALUE)?"§a":"§7").appendCodePoint(cp);return true;});legacy.add(Component.literal(b.toString()));}
            eq(componentText(TooltipTranslation.components(legacy,false)),target);
        }
        // Different styles inside Piercing cannot be silently flattened.
        var broken=new ArrayList<Component>(originals(0));broken.set(1,Component.literal("the ").setStyle(AuditMiningStyles.PROSE)
            .append(Component.literal("Pier").setStyle(AuditMiningStyles.TERM)).append(Component.literal("cing").setStyle(AuditMiningStyles.VALUE))
            .append(Component.literal(" enchant to ").setStyle(AuditMiningStyles.PROSE)).append(Component.literal("Skewer").setStyle(AuditMiningStyles.TERM)).append(Component.literal(".").setStyle(AuditMiningStyles.PROSE)));
        eq(TooltipTranslation.components(broken,false).get(1).getString(),broken.get(1).getString());
        eq(AuditMiningStyles.plain(TooltipTranslation.styled(broken.stream().map(Component::getVisualOrderText).toList(),false).get(1)),broken.get(1).getString());
        // This exact row alone is not the reviewed Nansorb paragraph.
        var isolated=List.of(originals(0).get(1));eq(componentText(TooltipTranslation.components(isolated,false)),SkyRuClient.component(isolated.get(0)).getString());
        for(String key:List.of("the Piercing enchant to Skewer.","Piercing")){
            Path custom=Path.of(args[0]).resolve("custom-"+checks);Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),key+"\tМой перевод\n");
            var override=new OfflineDictionary(custom);override.setRecordUnknown(false);flag("dictionary",override);
            var rows=key.equals("Piercing")?originals(0):List.<Component>of(Component.literal("Replaces the damage reduction from"),Component.literal(key));
            if(!componentText(TooltipTranslation.components(rows,false)).contains("Мой перевод"))throw new AssertionError("override lost");checks++;
            if(!styledText(TooltipTranslation.styled(rows.stream().map(Component::getVisualOrderText).toList(),false)).contains("Мой перевод"))throw new AssertionError("styled override lost");checks++;
        }
        flag("dictionary",db);
        if(!UserInputText.editingFrames(List.of("net.minecraft.client.gui.components.EditBox#renderWidget")))throw new AssertionError("input frame guard lost");checks++;
        if(!UserInputText.editingFrames(List.of("net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen#extractRenderState")))throw new AssertionError("sign frame guard lost");checks++;
        flag("enabled",false);eq(componentText(TooltipTranslation.components(originals(2),false)),AuditMiningStyles.CASES[2][0]);
        eq(styledText(TooltipTranslation.styled(originals(2).stream().map(Component::getVisualOrderText).toList(),false)),AuditMiningStyles.CASES[2][0]);
        System.out.println("PASS: multicolour reviewed lore "+checks+" assertions; 7 synthetic paragraphs / five repeats / semantic actions / legacy / fail closed / overrides / input / Shift");
    }
}
