import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** Confirmed whole paragraphs from thirteen newly read Dungeon utility/healing cards. */
public class TestDungeonUtilityProse {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xaaaaaa).withInsertion("original body");
    private static final Style TERM=Style.EMPTY.withColor(0x55ffff).withHoverEvent(new HoverEvent.ShowText(Component.literal("original term")));
    private static final Style VALUE=Style.EMPTY.withColor(0x55ff55).withClickEvent(new ClickEvent.RunCommand("/original-value"));
    private static final String[][] CASES={{"Deals double damage when coming\nback. Pierces up to 10 foes.","Наносит двойной урон при\nвозвращении. Пронзает до 10 врагов."},
{"Can be used to defuse Dungeon\ntraps. Successful defusing of traps\nmay increase your  Intelligence!","Позволяет обезвреживать ловушки подземелий.\nУспешное обезвреживание ловушек\nможет повысить ваш  интеллект!"},
{"Slash in a huge arc, dealing 135% melee\ndamage to all enemies hit!","Рубит по широкой дуге, нанося 135% урона ближнего боя\nвсем задетым врагам!"},
{"Heal for 500❤ and heal players within 8\nblocks for 100❤.","Восстанавливает вам 500❤; игрокам в радиусе 8\nблоков — 100❤."},
{"Heal for 420❤ and heal players within 7\nblocks for 84❤.","Восстанавливает вам 420❤; игрокам в радиусе 7\nблоков — 84❤."},
{"Heal for 320❤ and heal players within 7\nblocks for 64❤.","Восстанавливает вам 320❤; игрокам в радиусе 7\nблоков — 64❤."},
{"Produces a cone of ice in front of the caster\nthat deals 17,000 damage to mobs and freezes\nthem in place for 5 seconds! Frozen mobs\ntake 10% increased damage!","Создаёт перед вами конус льда,\nкоторый наносит мобам 17,000 урона и замораживает\nих на месте на 5 секунд! Замороженные мобы\nполучают на 10% больше урона!"},
{"Produces a cone of ice in front of the caster\nthat deals 19,114.1 damage to mobs and\nfreezes them in place for 5 seconds! Frozen\nmobs take 10% increased damage!","Создаёт перед вами конус льда,\nкоторый наносит мобам 19,114.1 урона и\nзамораживает их на месте на 5 секунд! Замороженные\nмобы получают на 10% больше урона!"},
{"The longer you hold this in your\ninventory, the stronger you'll\nbecome for 2 minutes after removing\nit from your inventory! Max +50.\n\nTime Held: 0 Minutes","Чем дольше утяжелители находятся в вашем\nинвентаре, тем больше силы вы\nполучите на 2 минуты после удаления\nих из инвентаря! Максимум +50.\n\nВремя хранения: 0 минут"}};
    private static final String[][] EN={{},{"Dungeon","Intelligence"},{},{},{},{},{},{},{}};
    private static final String[][] RU={{},{"подземелий","интеллект"},{},{},{},{},{},{},{}};
    private static final String[][] NUM={{"10"},{},{"135%"},{"500","8","100"},{"420","7","84"},{"320","7","64"},{"17,000","5","10%"},{"19,114.1","5","10%"},{"2","+50"}};
    private static final String[][] NUM_RU={{"10"},{},{"135%"},{"500","8","100"},{"420","7","84"},{"320","7","64"},{"17,000","5","10%"},{"19,114.1","5","10%"},{"2","+50"}};
    private static void check(boolean v,String message){if(!v)throw new AssertionError(message);checks++;}
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    private static String text(List<Component> rows){return String.join("\n",rows.stream().map(Component::getString).toList());}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Component> source(int n){var result=new ArrayList<Component>();
        for(String row:CASES[n][0].split("\n")){
            var c=Component.empty();int at=0;
            while(at<row.length()){
                String found=null;Style style=BODY;
                for(String t:EN[n])if(row.startsWith(t,at)){found=t;style=TERM;break;}
                for(String t:NUM[n])if(row.startsWith(t,at)){found=t;style=VALUE;break;}
                if(found==null)found=row.substring(at,at+1);
                c.append(Component.literal(found).setStyle(style));at+=found.length();
            }result.add(c);
        }return result;
    }
    private static void styles(List<Component> rows,int n){
        for(var row:rows){String line=row.getString();var expected=new ArrayList<Style>(Collections.nCopies(line.length(),BODY));
            for(String t:RU[n]){int at=-1;while((at=line.indexOf(t,at+1))>=0)for(int i=at;i<at+t.length();i++)expected.set(i,TERM);}
            for(String t:NUM_RU[n]){int at=-1;while((at=line.indexOf(t,at+1))>=0)for(int i=at;i<at+t.length();i++)expected.set(i,VALUE);}
            int[] at={0};StringDecomposer.iterateFormatted(row,Style.EMPTY,(i,s,p)->{for(int k=0;k<Character.charCount(p);k++)check(s.equals(expected.get(at[0]++)),"style/action: "+line);return true;});
        }
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        for(int n=0;n<CASES.length;n++){
            var original=source(n);var result=TooltipTranslation.components(original,false);
            for(int repeat=0;repeat<5;repeat++){
                check(text(result).equals(CASES[n][1]),"whole paragraph: "+text(result));styles(result,n);
                var seq=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);
                check(String.join("\n",seq.stream().map(TestDungeonUtilityProse::plain).toList()).equals(CASES[n][1]),"sequence paragraph");
                result=TooltipTranslation.components(result,false);
            }
            check(text(original).equals(CASES[n][0]),"source immutable");
            check(db.translate(CASES[n][0]).equals(CASES[n][1]),"plain full paragraph: "+db.translate(CASES[n][0]));
            var uniform=Arrays.stream(CASES[n][0].split("\n")).map(Component::literal).map(c->(Component)c).toList();
            check(text(TooltipTranslation.components(uniform,false)).equals(CASES[n][1]),"uniform paragraph");
            var card=new ArrayList<Component>();card.add(Component.literal("Dungeon Orb"));card.addAll(original);card.add(Component.literal("LEGENDARY DUNGEON ITEM"));
            var shifted=TooltipTranslation.components(card,true);check(shifted.getFirst().getString().equals("Dungeon Orb"),"Shift-original title");
            check(text(shifted.subList(1,1+original.size())).equals(CASES[n][1]),"Shift prose");check(shifted.getLast().getString().equals("LEGENDARY ПРЕДМЕТ ПОДЗЕМЕЛИЙ"),"English rarity");
            var custom=Path.of(args[0]).resolve("custom-"+n);Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),CASES[n][0].split("\n")[0]+"\tМоя фраза\n");
            var own=new OfflineDictionary(custom);own.setRecordUnknown(false);flag("dictionary",own);
            check(text(TooltipTranslation.components(uniform,false)).contains("Моя фраза"),"uniform override wins");
            check(!text(TooltipTranslation.components(original,false)).equals(CASES[n][1]),"semantic renderer yields to override");flag("dictionary",db);
        }
        for(String count:List.of("0","1","2","15","60","120")){
            var timer=source(8);timer.set(timer.size()-1,Component.empty().append(Component.literal("Time Held: ").setStyle(BODY))
                .append(Component.literal(count).setStyle(VALUE)).append(Component.literal(" Minutes").setStyle(BODY)));
            var result=TooltipTranslation.components(timer,false);
            check(result.getLast().getString().equals("Время хранения: "+count+" минут"),"inventory timer, not hands");
            var label=result.getLast();StringDecomposer.iterateFormatted(label,Style.EMPTY,(i,s,p)->{
                check(s.equals(Character.isDigit(p)?VALUE:BODY),"original timer glyph action/style");return true;
            });
            check(db.translate(text(timer)).endsWith("Время хранения: "+count+" минут"),"plain inventory timer");
        }
        var isolated=TooltipTranslation.components(List.of(Component.literal("Time Held: 0 Minutes")),false);
        check(!isolated.getFirst().getString().contains("Время хранения"),"no guessing unrelated timer");
        flag("enabled",false);check(text(TooltipTranslation.components(source(0),false)).equals(CASES[0][0]),"disabled unchanged");
        System.out.println("PASS: Dungeon utility prose "+checks+" assertions; nine paragraphs, five repeats, semantic events/numbers, Shift, overrides, English rarity");
    }
}
