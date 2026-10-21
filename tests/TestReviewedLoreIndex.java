import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** Independent original ordered scan oracle; no performance assertion from timing. */
public class TestReviewedLoreIndex {
    private static int checks;private static Method bind,reviewed,allowed;
    private static List<List<?>> groups;
    private static Object call(Object o,String method)throws Exception{var m=o.getClass().getDeclaredMethod(method);m.setAccessible(true);return m.invoke(o);}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
    @SuppressWarnings("unchecked") private static List<String> sources(Object row)throws Exception{return(List<String>)call(row,"source");}
    private static String signature(Object row)throws Exception{return row==null?"NONE":call(row,"text")+" / "+sources(row)+" / "+call(row,"target");}
    private static Object oracle(List<String> rows,int index)throws Exception{
        for(var group:groups)for(int start=Math.max(0,index-group.size()+1);start<=index;start++){
            // Intentional77 safety correction, independently specified from the
            // published short variant, not read from the production Group flag.
            // Its two rows are a complete paragraph, never a prefix of an
            // unknown cooldown. TestLoreBoundary77 freezes the expected fallback.
            if(group.size()==2&&call(group.getFirst(),"text").equals("Regen +{0} Vitality every time you")
                &&call(group.get(1),"text").equals("hit a mob.")&&start+2<rows.size()
                &&!rows.get(start+2).isBlank())continue;
            if(start+group.size()>rows.size())continue;var first=bind.invoke(group.getFirst(),rows.get(start));if(first==null)continue;
            boolean match=true;var bound=new ArrayList<Object>();bound.add(first);
            for(int i=1;i<group.size();i++){var next=bind.invoke(group.get(i),rows.get(start+i));if(next==null){match=false;break;}bound.add(next);}
            if(!match)continue;var keys=new ArrayList<String>();
            for(var row:group){String text=(String)call(row,"text");keys.add(text);keys.addAll(sources(row));
                // Independent compatibility oracle; not read from the production map.
                var aliases=Map.ofEntries(Map.entry("Защита","Defense"),Map.entry("Повторение","Repeat"),Map.entry("и","and"),Map.entry("Суперпары","Superpairs"),Map.entry("Сад","Garden"),Map.entry("Культуры","Crops"),Map.entry("игроки","players"),Map.entry("Игроки","players"),Map.entry("Увеличивает скорость работы","Increases the speed of"),Map.entry("часов","hours"),Map.entry("минут","minutes"));
                for(String piece:sources(row)){String alias=aliases.get(piece);if(alias!=null){keys.add(alias);keys.add(text.replace(piece,alias));}}}
            for(var row:bound){keys.add((String)call(row,"text"));keys.addAll(sources(row));}
            return(Boolean)allowed.invoke(null,keys)?bound.get(index-start):null;
        }return null;
    }
    private static void compare(List<String> rows)throws Exception{
        for(int index=0;index<rows.size();index++){var actual=reviewed.invoke(null,rows,index);Object row=actual==null?null:call(actual,"row");check(signature(row).equals(signature(oracle(rows,index))),"ordered candidate/override precedence differs at "+index+": "+rows);}
    }
    private static List<Style> styles(Component c){var list=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)list.add(s);return true;});return list;}
    @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var type=Class.forName("ru.wither.skyru.ReviewedLoreStyles");var field=type.getDeclaredField("GROUPS");field.setAccessible(true);groups=(List<List<?>>)field.get(null);
        bind=groups.getFirst().getFirst().getClass().getDeclaredMethod("bind",String.class);bind.setAccessible(true);reviewed=type.getDeclaredMethod("reviewed",List.class,int.class);reviewed.setAccessible(true);allowed=SkyRuClient.class.getDeclaredMethod("tooltipPowerAllowed",List.class);allowed.setAccessible(true);
        // Independent full regex oracle: the new suffix gate cannot drop valid rows.
        for(var group:groups)for(var row:group)for(String number:List.of("0","7","12.5","1,000","12345678.123456","-1","1..2","8e8")){
            String text=(String)call(row,"text"),actual=text.replace("{0}",number).replace("{1}","13");
            var pattern=(java.util.regex.Pattern)call(row,"numeric");
            boolean expected=text.equals(actual)||actual.length()<=400&&pattern!=null&&actual.startsWith((String)call(row,"prefix"))&&pattern.matcher(actual).matches();
            check((bind.invoke(row,actual)!=null)==expected,"prefix/suffix rejection changed full-regex acceptance: "+actual);
            check(bind.invoke(row,actual+" UNREVIEWED")==null,"unknown suffix accepted");
        }
        for(var group:groups)for(String number:List.of("{literal}","7","12.5")){
            var rows=new ArrayList<String>();rows.add("Unreviewed title");rows.add("");
            for(var row:group){String text=(String)call(row,"text");rows.add(number.equals("{literal}")?text:text.replace("{0}",number).replace("{1}","13"));}
            rows.add("Unreviewed neighbour");compare(rows);
            var doubled=new ArrayList<String>(rows);doubled.addAll(rows);compare(doubled);
            for(int i=2;i<2+group.size();i++){var broken=new ArrayList<>(rows);broken.set(i,broken.get(i)+" UNREVIEWED");compare(broken);}
        }
        compare(List.of("Кошелёк: 100","Nothing here","","9% chance to not consume","bait."));
        for(String key:List.of("{0}","7","Saves arrows 7% of the time","Grants ")){
            Path dir=Path.of(args[0]).resolve("override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой текст\n");flag("dictionary",new OfflineDictionary(dir));
            for(var group:groups){var rows=new ArrayList<String>();for(var row:group)rows.add(((String)call(row,"text")).replace("{0}","7").replace("{1}","13"));compare(rows);}
        }
        flag("dictionary",db);
        var body=Style.EMPTY.withColor(0xaaaaaa).withClickEvent(new ClickEvent.RunCommand("/body-original"));
        for(int repeat=0;repeat<5;repeat++){
            var a=Style.EMPTY.withColor(0x100000+repeat).withInsertion("first "+repeat);var b=Style.EMPTY.withColor(0x200000+repeat).withClickEvent(new ClickEvent.RunCommand("/digit-"+repeat));
            var first=Component.literal("Saves arrows ").setStyle(body).append(Component.literal("1").setStyle(a)).append(Component.literal("2").setStyle(b)).append(Component.literal("% of the time").setStyle(body));
            var rows=List.<Component>of(first,Component.literal("when you fire your bow.").setStyle(body));var actual=TooltipTranslation.components(rows,false);
            check(actual.getFirst().getString().equals("С шансом 12% сохраняет стрелы"),"copy-only numeric pieces should preserve gradient without skipping prose");int at=actual.getFirst().getString().indexOf("12"),before=first.getString().indexOf("12");var ss=styles(actual.getFirst());var originalStyles=styles(first);check(ss.get(at).equals(originalStyles.get(before))&&ss.get(at+1).equals(originalStyles.get(before+1)),"fresh effective per-digit actions/styles");
            check(first.getString().equals("Saves arrows 12% of the time"),"immutable source");
            var styled=TooltipTranslation.styled(rows.stream().map(Component::getVisualOrderText).toList(),false);check(AuditMiningStyles.plain(styled.getFirst()).equals(actual.getFirst().getString()),"sequence numeric gradient");
        }
        var bad=Component.literal("Saves ").setStyle(body).append(Component.literal("arrows ").setStyle(body.withColor(0xff5555))).append(Component.literal("12% of the time").setStyle(body));
        var original=List.<Component>of(bad,Component.literal("when you fire your bow.").setStyle(body));check(TooltipTranslation.components(original,false).getFirst()==bad,"changed semantic span with unknown mixed style stays original");
        check(reviewed.invoke(null,List.of(),0)==null,"empty list guard");
        System.out.println("PASS: reviewed lore index "+checks+" assertions; "+groups.size()+" template groups vs original scan, precedence/unknown/overrides/gradient/fresh actions; no live game");
    }
}
