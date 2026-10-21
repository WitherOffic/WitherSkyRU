import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import java.util.regex.*;
import java.lang.invoke.*;
import ru.wither.skyru.*;

/** Numeric-prefix completeness vs full regex, order and zero-allocation lookup. */
public final class TestNumericHeadIndex59{
    private static int checks;
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
    private static Object call(Object o,String n)throws Exception{var m=o.getClass().getDeclaredMethod(n);m.setAccessible(true);return m.invoke(o);}
    private static int inspect(Object node)throws Exception{
        var type=node.getClass();for(var f:type.getDeclaredFields())check(Modifier.isFinal(f.getModifiers())&&Modifier.isPrivate(f.getModifiers()),"frozen private node fields");
        var children=type.getDeclaredField("children");children.setAccessible(true);var chars=type.getDeclaredField("chars");chars.setAccessible(true);var candidates=type.getDeclaredField("candidates");candidates.setAccessible(true);
        var letters=(char[])chars.get(node);var nodes=(Object[])children.get(node);check(letters.length==nodes.length,"edges align");for(int i=1;i<letters.length;i++)check(letters[i-1]<letters[i],"sorted distinct edges");
        var list=(List<?>)candidates.get(node);int prev=-1;for(var group:list){int id=(int)call(group,"id");check(id>prev,"candidate precedence");prev=id;}
        try{((List<Object>)list).add(null);throw new AssertionError("mutable candidates");}catch(UnsupportedOperationException expected){checks++;}
        int total=1;for(var child:nodes)total+=inspect(child);return total;
    }
    public static void main(String[] args)throws Throwable{
        var type=Class.forName("ru.wither.skyru.ReviewedLoreStyles");var fg=type.getDeclaredField("GROUPS");fg.setAccessible(true);var groups=(List<List<?>>)fg.get(null);
        var fi=type.getDeclaredField("GROUP_INDEX");fi.setAccessible(true);var index=fi.get(null);var get=index.getClass().getDeclaredMethod("groups",String.class);get.setAccessible(true);
        var fn=index.getClass().getDeclaredField("numericHeads");fn.setAccessible(true);var numeric=fn.get(index);int nodes=inspect(numeric);
        var probes=new LinkedHashSet<String>();var regex=new ArrayList<Pattern>();var heads=new ArrayList<String>();
        for(var group:groups){var first=group.getFirst();String text=(String)call(first,"text");heads.add(text);Pattern pattern=(Pattern)call(first,"numeric");regex.add(pattern);
            for(String n:List.of("{literal}","0","7","12.5","1,000","12345678.123456","-1","8e8")){String actual=n.equals("{literal}")?text:text.replace("{0}",n).replace("{1}","13");probes.add(actual);probes.add(actual+" UNKNOWN");}
            String prefix=(String)call(first,"prefix");for(int end=0;end<prefix.length();end++){probes.add(prefix.substring(0,end));probes.add(prefix.substring(0,end)+"? UNREVIEWED");}
        }
        probes.addAll(List.of("","? unknown","💠 unknown","Я неизвестный","٧%","７%","Holds various items obtained from UNKNOWN","Increases damage dealt by 1.2%","A".repeat(401)));
        long candidateCount=0;
        for(String actual:probes){
            var expected=new TreeSet<Integer>();for(int i=0;i<groups.size();i++){var pattern=regex.get(i);if(heads.get(i).equals(actual)||actual.length()<=400&&pattern!=null&&pattern.matcher(actual).matches())expected.add(i);}
            var accepted=new TreeSet<Integer>();int prev=-1;for(var group:(List<?>)get.invoke(index,actual)){
                candidateCount++;int id=(int)call(group,"id");check(id>prev,"ordered candidate ids");prev=id;var pattern=regex.get(id);if(heads.get(id).equals(actual)||actual.length()<=400&&pattern!=null&&pattern.matcher(actual).matches())accepted.add(id);
            }check(accepted.equals(expected),"lost full-regex head "+actual+" expected="+expected+" actual="+accepted);
        }
        var m=numeric.getClass().getDeclaredMethod("groups",String.class);m.setAccessible(true);var handle=MethodHandles.lookup().unreflect(m).bindTo(numeric).asType(MethodType.methodType(List.class,String.class));
        var samples=List.of("Increases damage dealt by 1.2%","Increases  Crit Damage by 100%.","Every 3 hits on a monster,","Holds various items obtained from UNKNOWN","７%","? unknown");
        int sink=0;for(int n=0;n<20000;n++)for(int j=0;j<samples.size();j++)sink+=((List<?>)handle.invokeExact(samples.get(j))).size();
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();if(bean.isThreadAllocatedMemorySupported()){
            if(!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().threadId(),before=bean.getThreadAllocatedBytes(thread);
            for(int n=0;n<20000;n++)for(int j=0;j<samples.size();j++)sink+=((List<?>)handle.invokeExact(samples.get(j))).size();
            long bytes=bean.getThreadAllocatedBytes(thread)-before;check(bytes<4096,"numeric lookup allocates per query: "+bytes);
        }
        check(inspect(numeric)==nodes,"no runtime nodes or current-string retention");check(sink>=0,"consumed lookup");
        System.out.println("PASS: numeric head index59 "+checks+" assertions;"+groups.size()+" groups/"+probes.size()+" probes/"+candidateCount+" candidates/"+nodes+" fixed nodes,full-regex completeness/order/prefix cuts/Unicode/long/immutable/no per-query allocation;not FPS");
    }
}
