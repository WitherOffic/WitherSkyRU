import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.util.*;

/** Observed Garden document additions and the separate preset screenshot. */
public class TestGardenDocAdditions {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xaaaaaa).withInsertion("body");
    private static final Style TERM=Style.EMPTY.withColor(0x55ff55).withHoverEvent(new HoverEvent.ShowText(Component.literal("term")));
    private static final Style NUMBER=Style.EMPTY.withColor(0xffff55).withClickEvent(new ClickEvent.RunCommand("/number"));
    private static final String[][] CASES={
        {"Every 3 SkyBlock days, I hold a contest\nfor 3 Farming Collections.","Каждые 3 дня SkyBlock проходят состязания\nпо 3 фермерским коллекциям."},
        {"The contests last 1 SkyBlock day.\n(20 minutes).","Состязание длится 1 день SkyBlock.\n(20 минут)."},
        {"The top players who increase\ntheir collection the most earn\nJacob's Tickets and unique\nrewards!","Игроки с наибольшим приростом\nколлекции получают\nбилеты Джейкоба и уникальные\nнаграды!"},
        {"Co-ops do NOT pool their collection!","Результаты участников кооператива НЕ суммируются!"},
        {"You may participate in 1 collection\ncontest per event!","Можно соревноваться только по 1 коллекции\nза одно событие!"},
        {"Minions do not count in contests!","Ресурсы, добытые миньонами, не учитываются!"},
        {"Increase the maximum amount of\norganic matter that can be stored to\n40,000","Увеличивает максимальный запас\nорганического сырья до\n40,000"},
        {"Change the selected plot to this\npreset!","Примените к выбранному участку\nэтот шаблон!"}
    };
    private static final Map<String,String> TERMS=Map.of("Farming","фермерским","Jacob's Tickets","билеты Джейкоба","unique","уникальные","NOT","НЕ");
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);checks++;}
    private static void flag(String key,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,v);}
    private static String text(List<Component> rows){return String.join("\n",rows.stream().map(Component::getString).toList());}
    private static String plain(FormattedCharSequence seq){var b=new StringBuilder();seq.accept((i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static Component styled(String row){var c=Component.empty();for(int at=0;at<row.length();){
        String term=null;for(String key:TERMS.keySet())if(row.startsWith(key,at)){term=key;break;}
        if(term!=null){c.append(Component.literal(term).setStyle(TERM));at+=term.length();}
        else {char ch=row.charAt(at++);c.append(Component.literal(String.valueOf(ch)).setStyle(Character.isDigit(ch)?NUMBER:BODY));}
    }return c;}
    private static void styles(Component c){String line=c.getString();var expected=new ArrayList<Style>();
        for(char ch:line.toCharArray())expected.add(Character.isDigit(ch)?NUMBER:BODY);
        for(String term:TERMS.values()){int at=line.indexOf(term);if(at>=0)for(int i=at;i<at+term.length();i++)expected.set(i,TERM);}
        int[] pos={0};StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int k=0;k<Character.charCount(p);k++)check(s.equals(expected.get(pos[0]++)),"semantic event/style: "+line);return true;});
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        for(int n=0;n<CASES.length;n++){
            var source=Arrays.stream(CASES[n][0].split("\n")).map(TestGardenDocAdditions::styled).toList();
            var result=TooltipTranslation.components(source,false);
            for(int repeat=0;repeat<5;repeat++){
                check(text(result).equals(CASES[n][1]),"paragraph: "+text(result));result.forEach(TestGardenDocAdditions::styles);
                var seq=TooltipTranslation.styled(source.stream().map(Component::getVisualOrderText).toList(),false);
                check(String.join("\n",seq.stream().map(TestGardenDocAdditions::plain).toList()).equals(CASES[n][1]),"sequence paragraph");
                result=TooltipTranslation.components(result,false);
            }
            check(db.translate(CASES[n][0]).equals(CASES[n][1]),"plain paragraph: "+db.translate(CASES[n][0]));
            check(text(source).equals(CASES[n][0]),"source immutable");
            var custom=Path.of(args[0]).resolve("override-"+n);Files.createDirectories(custom);
            Files.writeString(custom.resolve("overrides.tsv"),CASES[n][0].split("\n")[0]+"\tМой текст\n");
            var own=new OfflineDictionary(custom);own.setRecordUnknown(false);flag("dictionary",own);
            check(!text(TooltipTranslation.components(source,false)).equals(CASES[n][1]),"user override");flag("dictionary",db);
        }
        for(var entry:Map.of("Organic Matter Cap","Вместимость органического сырья","Not enough Copper!","Недостаточно меди!","Click to open pasting options!","Нажмите, чтобы настроить применение шаблона!").entrySet()){
            check(db.translate(entry.getKey()).equals(entry.getValue()),"label: "+db.translate(entry.getKey()));
            check(SkyRuClient.component(Component.literal(entry.getKey())).getString().equals(entry.getValue()),"component label");
        }
        var notices=new ChatNotificationTranslator(db);
        for(String glyph:List.of(""," "," ")){
            String line="[NPC] Pesthunter Pamela: Let Phillip finish teaching you about "+glyph+"Pests, then come chat with me!";
            var input=Component.empty().append(Component.literal("[NPC] Pesthunter Pamela: Let ").setStyle(BODY))
                .append(Component.literal("Phillip").setStyle(TERM)).append(Component.literal(" finish teaching you about ").setStyle(BODY))
                .append(Component.literal(glyph+"Pests").setStyle(TERM)).append(Component.literal(", then come chat with me!").setStyle(BODY));
            var result=notices.translateNpcRequest(input,GuiMessageSource.SYSTEM_SERVER,false);
            check(result.getString().equals("[NPC] Pesthunter Pamela: Сначала Phillip должен закончить рассказывать вам о "+glyph+"вредителях; затем поговорите со мной!"),"Pamela condition: "+result.getString());
            for(int i=0;i<5;i++)check(notices.translateNpcRequest(result,GuiMessageSource.SYSTEM_SERVER,false)==result,"NPC idempotence");
            check(input.getString().equals(line),"NPC original immutable");
            check(notices.translateNpcRequest(input,GuiMessageSource.PLAYER,false)==input,"player text not translated");
            check(notices.translateNpcRequest(input,GuiMessageSource.SYSTEM_CLIENT,false)==input,"unverified client source");
            check(notices.translateNpcRequest(input,GuiMessageSource.SYSTEM_SERVER,true)==input,"signed text not translated");
            check(notices.translate(input,GuiMessageSource.SYSTEM_SERVER,false)==input,"separate NPC toggle");
            for(String name:List.of("Wither_101","another_name","pickaxe")){
                var other=Component.literal(line.replace("Phillip",name));check(notices.translateNpcRequest(other,GuiMessageSource.SYSTEM_SERVER,false)==other,"do not infer player identity");
            }
            var custom=Path.of(args[0]).resolve("npc-override-"+glyph.length());Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),"Pests\tМой термин\n");
            var own=new OfflineDictionary(custom);own.setRecordUnknown(false);check(new ChatNotificationTranslator(own).translateNpcRequest(input,GuiMessageSource.SYSTEM_SERVER,false)==input,"NPC custom override");
        }
        System.out.println("PASS: Garden document additions "+checks+" assertions; Jacob rules, composter capacity, preset, Pamela training condition, styles/counts/overrides and player identity");
    }
}
