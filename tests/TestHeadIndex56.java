import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import ru.wither.skyru.*;

/** Exact/static-head index completeness, order and immutable template state. */
public final class TestHeadIndex56{
    private static int checks;
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    private static Object call(Object o,String n)throws Exception{var m=o.getClass().getDeclaredMethod(n);m.setAccessible(true);return m.invoke(o);}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var type=Class.forName("ru.wither.skyru.ReviewedLoreStyles");var f=type.getDeclaredField("GROUPS");f.setAccessible(true);var groups=(List<List<?>>)f.get(null);
        var fi=type.getDeclaredField("GROUP_INDEX");fi.setAccessible(true);var index=fi.get(null);var get=index.getClass().getDeclaredMethod("groups",String.class);get.setAccessible(true);
        var bind=groups.getFirst().getFirst().getClass().getDeclaredMethod("bind",String.class);bind.setAccessible(true);
        var heads=new LinkedHashSet<String>();for(var group:groups){String s=(String)call(group.getFirst(),"text");for(String number:List.of("{literal}","0","7","12.5","1,000","12345678.123456","-1","1..2","8e8")){var value=number.equals("{literal}")?s:s.replace("{0}",number).replace("{1}","13");heads.add(value);heads.add(value+" UNKNOWN");}}
        heads.addAll(List.of("","Holds various items obtained from UNKNOWN","Хранит различные предметы, полученные из UNKNOWN","? unknown","💠 unknown","A".repeat(401)));
        for(String head:heads){
            var candidates=(List<?>)get.invoke(index,head);var expected=new TreeSet<Integer>();for(int id=0;id<groups.size();id++)if(bind.invoke(groups.get(id).getFirst(),head)!=null)expected.add(id);
            var actual=new TreeSet<Integer>();int previous=-1;for(var candidate:candidates){int id=(int)call(candidate,"id");check(id>previous,"candidate order/duplicates");previous=id;if(bind.invoke(groups.get(id).getFirst(),head)!=null)actual.add(id);
                var first=groups.get(id).getFirst();if(call(first,"numeric")==null)check(call(first,"text").equals(head),"constant miss is not a candidate");
            }check(actual.equals(expected),"lost accepted head: "+head+" expected "+expected+" actual "+actual);
        }
        var constants=index.getClass().getDeclaredField("constantHeads");constants.setAccessible(true);var map=(Map<String,List<?>>)constants.get(index);int size=map.size();
        try{map.put("UNKNOWN",List.of());throw new AssertionError("mutable index");}catch(UnsupportedOperationException expected){checks++;}
        for(var list:map.values())try{((List<Object>)list).add(null);throw new AssertionError("mutable list");}catch(UnsupportedOperationException expected){checks++;}
        for(int n=0;n<1000;n++)for(var candidate:(List<?>)get.invoke(index,"Holds various items obtained from UNKNOWN"+n)){
            int id=(int)call(candidate,"id");check(call(groups.get(id).getFirst(),"numeric")!=null,"unknown Holds skips constant candidates");
        }
        check(map.size()==size,"no runtime string retention");
        System.out.println("PASS: head index56 "+checks+" assertions;"+groups.size()+" groups, exact/static plus numeric completeness, original id order, Unicode/long/invalid/unknown and immutable template state;not FPS");
    }
}
