package ru.wither.skyru;

import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** Delivered96 oracle; scratch reuse must not retain live render objects or cross recursive calls. */
public final class TestOptimization97 {
    private static int checks;
    private static synchronized void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
    private static Object value(Class<?> cls,Object owner,String name)throws Exception{var f=cls.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    private static Method method(Class<?> cls,String name,Class<?>... args)throws Exception{var m=cls.getDeclaredMethod(name,args);m.setAccessible(true);return m;}
    private static final class Previous extends URLClassLoader {
        Previous(URL jar){super(new URL[]{jar},TestOptimization97.class.getClassLoader());}
        @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
            if(!name.startsWith("ru.wither.skyru."))return super.loadClass(name,resolve);
            synchronized(getClassLoadingLock(name)){var cls=findLoadedClass(name);if(cls==null)cls=findClass(name);if(resolve)resolveClass(cls);return cls;}
        }
    }
    private record RunView(int start,int end,Style style){}
    private record Snapshot(String text,List<RunView> runs){}
    private static Snapshot snapshot(Object object)throws Exception{
        var cls=object.getClass();String text=(String)value(cls,object,"text");var runs=new ArrayList<RunView>();
        for(Object run:(List<?>)value(cls,object,"runs")){var type=run.getClass();runs.add(new RunView((int)method(type,"start").invoke(run),(int)method(type,"end").invoke(run),(Style)method(type,"style").invoke(run)));}
        return new Snapshot(text,List.copyOf(runs));
    }
    private static Object pool()throws Exception{return ((ThreadLocal<?>)value(StyledRuns.class,null,"SCRATCH")).get();}
    private static void clean()throws Exception{
        var owner=pool();check(!(boolean)value(owner.getClass(),owner,"busy"),"pool idle after every completion");
        for(var field:owner.getClass().getDeclaredFields())check(field.getType()==StringBuilder.class||field.getType()==boolean.class,"pool has only primitive text storage and ownership;no collector/style/list/source/component");
        var text=(StringBuilder)value(owner.getClass(),owner,"text");check(text==null||text.isEmpty(),"no live text length retained");
        check(text==null||text.capacity()<=(int)value(StyledRuns.class,null,"MAX_RETAINED_CHARS"),"bounded primitive character buffer");
    }
    private static Style style(int n){return Style.EMPTY.withColor(n).withBold((n&1)==0).withInsertion("draw "+n).withClickEvent(new ClickEvent.RunCommand("/current "+n)).withHoverEvent(new HoverEvent.ShowText(Component.literal("hint "+n)));}
    private static FormattedCharSequence input(String text,int n){
        var parts=new ArrayList<FormattedCharSequence>();var base=style(n);
        if(n%4==0)return FormattedCharSequence.forward(text,base);
        int at=0;while(at<text.length()){
            int end=n%4==1?Math.min(text.length(),at+1):Math.min(text.length(),at+Math.max(1,text.length()/3));
            var current=n%4==1?style(n+at):n%4==2?base:style(n);
            parts.add(FormattedCharSequence.forward(text.substring(at,end),current));at=end;
        }
        return FormattedCharSequence.composite(parts);
    }
    private static void compare(Method old,FormattedCharSequence source,int limit)throws Exception{
        var expected=snapshot(old.invoke(null,source,limit));var actual=StyledRuns.collect(source,limit);
        check(expected.equals(snapshot(actual)),"fresh text/run/style oracle at limit "+limit);
        for(int i=0;i<actual.runs.size();i++)check(actual.runs.get(i).style()==expected.runs().get(i).style(),"exact current Style reference, not a retained or copied style");
        if(!actual.runs.isEmpty()){
            check(actual.styleAt(0).equals(expected.runs().getFirst().style()),"fresh styleAt");
            try{actual.runs.clear();throw new AssertionError("mutable result list");}catch(UnsupportedOperationException expectedImmutable){checks++;}
        }
        clean();
    }
    public static void main(String[] args)throws Exception{
        var project=Path.of(args[1]).toAbsolutePath().getParent();var jar=project.resolve("build/libs/WitherSkyRU-26.1.2-fabric-0.15.96-dev.jar");
        check(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))).equalsIgnoreCase("97B80C1105385C72DD3D04F713CD211540646EB97E266D8F077305DA36BD5BF1"),"frozen delivered96");
        try(var previous=new Previous(jar.toUri().toURL())){
            var old=method(previous.loadClass("ru.wither.skyru.StyledRuns"),"collect",FormattedCharSequence.class,int.class);
            var examples=List.of("","Gems: 75","Самоцветы: 75","§aLegacy","😀e\u0301👩‍🌾","   ","αβγ","я".repeat(300),"a".repeat(1024),"x".repeat(65536));
            int seed=0;
            for(String text:examples)for(int mode=0;mode<4;mode++)for(int limit:new int[]{-1,0,1,2,15,255,256,400,512,Integer.MAX_VALUE})compare(old,input(text,seed++),limit);
            var db=new OfflineDictionary(Path.of(args[0],"dictionary"));db.setRecordUnknown(false);
            var keys=(Map<String,String>)value(OfflineDictionary.class,db,"exact");
            for(String key:keys.keySet())compare(old,input(key,seed++),400);
            // Non-standard source indices and early termination remain the old collector's policy.
            var shared=style(42);FormattedCharSequence unusual=s->{if(!s.accept(900,shared,'A'))return false;if(!s.accept(-5,shared,0x1F600))return false;return s.accept(2,shared,'Z');};
            for(int limit:new int[]{0,1,2,5,Integer.MAX_VALUE})compare(old,unusual,limit);
            var retained=new ArrayList<StyledRuns>();var retainedSnapshots=new ArrayList<Snapshot>();
            for(int n=0;n<30;n++){var result=StyledRuns.collect(input("Gems: "+n,n));retained.add(result);retainedSnapshots.add(snapshot(result));}
            for(int n=0;n<2000;n++)StyledRuns.collect(input(n%2==0?"Русский 😀":"English next "+n,n));
            for(int n=0;n<retained.size();n++)check(snapshot(retained.get(n)).equals(retainedSnapshots.get(n)),"later reuse never mutates a returned snapshot");clean();
            // Inner collection owns a fresh fallback and cannot reset the active outer collector.
            var first=style(91);var last=style(92);FormattedCharSequence recursive=s->{
                if(!s.accept(0,first,'A'))return false;
                try{
                    var owner=pool();check((boolean)value(owner.getClass(),owner,"busy"),"outer marked busy");var buffer=(StringBuilder)value(owner.getClass(),owner,"text");
                    var nested=StyledRuns.collect(input("Внутренний 😀",12));check(nested.text.equals("Внутренний 😀"),"nested own result");
                    check((boolean)value(owner.getClass(),owner,"busy"),"inner preserves outer busy flag");check(buffer.toString().equals("A"),"inner preserves outer buffer");
                    try{StyledRuns.collect(inner->{inner.accept(0,style(888),'E');throw new IllegalStateException("inner failure");});throw new AssertionError("missing nested exception");}catch(IllegalStateException expected){check(expected.getMessage().equals("inner failure"),"nested exception propagated");}
                    check(buffer.toString().equals("A"),"failed inner never clears outer buffer");
                }catch(RuntimeException e){throw e;}catch(Exception e){throw new AssertionError(e);}
                return s.accept(1,last,'Z');
            };
            var nestedResult=StyledRuns.collect(recursive);check(nestedResult.text.equals("AZ")&&nestedResult.runs.getFirst().style()==first&&nestedResult.runs.getLast().style()==last,"outer fresh result after nested success/failure");clean();
            var late=new FormattedCharSink[1];var firstSnapshot=StyledRuns.collect(s->{late[0]=s;return s.accept(0,style(19),'A');});
            check(!late[0].accept(1,style(20),'X'),"a completed sink rejects delayed writes");
            check(value(late[0].getClass(),late[0],"current")==null&&value(late[0].getClass(),late[0],"runs")==null,"completed collector releases styles even if source retained its sink");
            var later=StyledRuns.collect(s->{s.accept(0,style(21),'B');check(!late[0].accept(2,style(22),'Y'),"old sink cannot corrupt active later draw");return s.accept(1,style(23),'C');});
            check(firstSnapshot.text.equals("A")&&later.text.equals("BC"),"late callback cannot mutate immutable snapshots or a later buffer");clean();
            for(int n=0;n<40;n++){
                int row=n;try{StyledRuns.collect(s->{s.accept(0,style(row),'X');throw new IllegalArgumentException("failure "+row);},n);throw new AssertionError("missing source failure");}catch(IllegalArgumentException expected){check(expected.getMessage().equals("failure "+n),"same source exception");}clean();compare(old,input("After failure "+n,n),400);
            }
            for(int n=0;n<10;n++){StyledRuns.collect(input("Large 😀".repeat(1500),n));clean();StyledRuns.collect(input("Gems: 75",n));clean();}
            for(Error error:List.of(new AssertionError("simulated error"),new OutOfMemoryError("simulated allocation error"))){try{StyledRuns.collect(s->{s.accept(0,style(71),'X');throw error;});throw new AssertionError("missing Error");}catch(Error caught){check(caught==error,"same Error propagated");}clean();}
            var owners=new ConcurrentLinkedQueue<Object>();var errors=new ConcurrentLinkedQueue<Throwable>();var threads=new ArrayList<Thread>();
            for(int t=0;t<6;t++){int worker=t;var thread=new Thread(()->{try{for(int n=0;n<250;n++){var result=StyledRuns.collect(input("Thread "+worker+"/"+n,n));check(result.text.equals("Thread "+worker+"/"+n),"independent concurrent output");clean();}owners.add(pool());}catch(Throwable error){errors.add(error);}});threads.add(thread);thread.start();}
            for(var thread:threads)thread.join();check(errors.isEmpty(),"parallel collector errors: "+errors);var list=new ArrayList<>(owners);for(int a=0;a<list.size();a++)for(int b=a+1;b<list.size();b++)check(list.get(a)!=list.get(b),"distinct thread-local scratch");
            clean();System.out.println("PASS: optimization97 "+checks+" assertions;all "+keys.size()+" dictionary keys plus Unicode/limits/large inputs;delivered96 run/style oracle;snapshot independence;nested success/failure;exception cleanup;bounded idle buffer;6 independent threads;not live FPS");
        }
    }
}
