import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** Confirmed whole paragraphs from armor-stone cards reviewed in passes 26–27 and 35. */
public class TestArmorStoneReadability {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xaaaaaa).withInsertion("original body");
    private static final Style TERM=Style.EMPTY.withColor(0x55ffff).withHoverEvent(new HoverEvent.ShowText(Component.literal("original term")));
    private static final Style VALUE=Style.EMPTY.withColor(0x55ff55).withClickEvent(new ClickEvent.RunCommand("/original-value"));
    private static final String[][] CASES={
{"Applies the Renowned reforge when\ncombined with Armor.","Накладывает перековку «Прославленный» при\nобъединении с бронёй."},
{"Applies the Spiked reforge when\ncombined with Armor.","Накладывает перековку «Шипастый» при\nобъединении с бронёй."},
{"A cold and hardened heart that was\nonce brimming with eager love.","Холодное окаменевшее сердце, которое\nкогда-то было полно пылкой любви."},
{"Mobs are 1% more likely to drop\ncandy during the Spooky Festival.","Мобы на 1% чаще оставляют\nконфеты во время Жуткого фестиваля."},
{"Grants +5 Speed for 5s after\nusing a Transmission ability.","Даёт +5 скорости на 5 с после\nиспользования способности перемещения."},
{"The center of Magma Cubes is the\nhottest part...also the hardest. Giant\nones can even create stones like\nthis one.","Сердцевина магмовых кубов — это\nсамая горячая и самая твёрдая часть. Гигантские\nкубы могут даже создавать такие камни, как\nэтот."},
{"Rewarded to every Necromancer who\npasses the final exam.","Выдаётся каждому некроманту, который\nсдаст выпускной экзамен."},
{"Applies the Ancient reforge when\ncombined with Armor.","Накладывает перековку «Древний» при\nобъединении с бронёй."},
{"The Zombie it came from was\nprobably a great warrior before its\ndeath.","Зомби, которому принадлежала эта плоть,\nвероятно, был великим воином до своей\nсмерти."},
{"Take -2%  Damage from  Undead\nmobs.","-2%  к получаемому урону от  нежити.\n"},
{"Grants +10 Mending while in\n Dungeons.","Даёт +10 силы исцеления в\n подземельях."}
    };
    private static final String[][] EN={{"Renowned","Armor"},{"Spiked","Armor"},{},{"candy","Spooky Festival"},{"Speed","Transmission"},{"Magma Cubes"},{"Necromancer"},{"Ancient","Armor"},{"Zombie"},{"Damage","Undead"},{"Mending","Dungeons"}};
    private static final String[][] RU={{"Прославленный","бронёй"},{"Шипастый","бронёй"},{},{"конфеты","Жуткого фестиваля"},{"скорости","перемещения"},{"магмовых кубов"},{"некроманту"},{"Древний","бронёй"},{"Зомби"},{"урону","нежити"},{"силы исцеления","подземельях"}};
    private static final String[][] NUM={{},{},{},{"1%"},{"+5","5s"},{},{},{},{},{"-2%"},{"+10"}};
    private static final String[][] NUM_RU={{},{},{},{"1%"},{"+5","5 с"},{},{},{},{},{"-2%"},{"+10"}};
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
            for(String t:RU[n]){int at=line.indexOf(t);if(at>=0)for(int i=at;i<at+t.length();i++)expected.set(i,TERM);}
            for(String t:NUM_RU[n]){int at=line.indexOf(t);if(at>=0)for(int i=at;i<at+t.length();i++)expected.set(i,VALUE);}
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
                check(String.join("\n",seq.stream().map(TestArmorStoneReadability::plain).toList()).equals(CASES[n][1]),"sequence paragraph");
                result=TooltipTranslation.components(result,false);
            }
            check(text(original).equals(CASES[n][0]),"source immutable");
            if(n>=7)check(db.translate(CASES[n][0]).equals(CASES[n][1]),"new plain whole paragraph: "+db.translate(CASES[n][0]));
            var uniform=Arrays.stream(CASES[n][0].split("\n")).map(Component::literal).map(c->(Component)c).toList();
            check(text(TooltipTranslation.components(uniform,false)).equals(CASES[n][1]),"uniform paragraph");
            var card=new ArrayList<Component>();card.add(Component.literal("Dragon Horn"));card.addAll(original);card.add(Component.literal("EPIC REFORGE STONE"));
            var shifted=TooltipTranslation.components(card,true);check(shifted.getFirst().getString().equals("Dragon Horn"),"Shift-original title");
            check(text(shifted.subList(1,1+original.size())).equals(CASES[n][1]),"Shift prose");check(shifted.getLast().getString().equals("EPIC КАМЕНЬ ПЕРЕКОВКИ"),"English rarity");
            var custom=Path.of(args[0]).resolve("custom-"+n);Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),CASES[n][0].split("\n")[0]+"\tМоя фраза\n");
            var own=new OfflineDictionary(custom);own.setRecordUnknown(false);flag("dictionary",own);
            check(text(TooltipTranslation.components(uniform,false)).contains("Моя фраза"),"uniform override wins");
            check(!text(TooltipTranslation.components(original,false)).equals(CASES[n][1]),"semantic renderer yields to override");flag("dictionary",db);
        }
        flag("enabled",false);check(text(TooltipTranslation.components(source(0),false)).equals(CASES[0][0]),"disabled unchanged");
        System.out.println("PASS: armor-stone readability "+checks+" assertions; eleven paragraphs, five repeats, semantic events/numbers, Shift, overrides, English rarity");
    }
}
