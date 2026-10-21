import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** Confirmed whole paragraphs from twelve newly read Wither/mage Dungeon cards. */
public class TestDungeonAbilityProse {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xaaaaaa).withInsertion("original body");
    private static final Style TERM=Style.EMPTY.withColor(0x55ffff).withHoverEvent(new HoverEvent.ShowText(Component.literal("original term")));
    private static final Style VALUE=Style.EMPTY.withColor(0x55ff55).withClickEvent(new ClickEvent.RunCommand("/original-value"));
    private static final String[][] CASES={{"Shoots a guided spirit bat, following your aim\nand exploding for 2,000 damage.","Выпускает призрачную летучую мышь, управляемую вашим прицелом\nи взрывающуюся, нанося 2,000 урона."},
{"➤ The damage blocked scales with\nyour Dungeon Stat Boost while in The\nCatacombs.","➤ Блокируемый урон зависит от\nвашего усиления характеристик подземелий в\nКатакомбах."}};
    private static final String[][] EN={{"spirit bat"},{"Dungeon Stat Boost","Catacombs"}};
    private static final String[][] RU={{"призрачную летучую мышь"},{"усиления характеристик подземелий","Катакомбах"}};
    private static final String[][] NUM={{"2,000"},{}};
    private static final String[][] NUM_RU={{"2,000"},{}};
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
                check(String.join("\n",seq.stream().map(TestDungeonAbilityProse::plain).toList()).equals(CASES[n][1]),"sequence paragraph");
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
        flag("enabled",false);check(text(TooltipTranslation.components(source(0),false)).equals(CASES[0][0]),"disabled unchanged");
        System.out.println("PASS: Dungeon ability prose "+checks+" assertions; two paragraphs, five repeats, semantic events/numbers, Shift, overrides, English rarity");
    }
}
