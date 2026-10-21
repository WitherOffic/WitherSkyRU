import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.*;
import ru.wither.skyru.*;

/** Deliberately block the diagnostic writer; rendering never waits for disk capacity. */
public final class TestUnknownWriter62 {
    private static int checks;
    private static void check(boolean ok,String note){if(!ok)throw new AssertionError(note);checks++;}
    private static Object field(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);var writer=(ThreadPoolExecutor)field(db,"unknownWriter");var queue=(BlockingQueue<?>)field(db,"unknownQueue");var misses=(Set<?>)field(db,"misses");var dropped=(java.util.concurrent.atomic.AtomicLong)field(db,"unknownDropped");
        var started=new CountDownLatch(1);var release=new CountDownLatch(1);writer.execute(()->{started.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}});check(started.await(5,TimeUnit.SECONDS),"writer fixture started");
        var log=OfflineDictionary.class.getDeclaredMethod("logUnknown",String.class);log.setAccessible(true);
        try{
            for(int n=0;n<16000;n++){log.invoke(db,"BoundedUnknownSample"+n);check(queue.size()<=256,"bounded pending strings");check(misses.size()<=15000,"check capacity before insertion");check(writer.getQueue().size()<=1,"at most one scheduled writer task");}
            check(misses.size()==15000&&queue.size()==256&&dropped.get()==14744,"bounded best-effort overflow, not a render-time disk wait");
            int count=misses.size();for(int n=0;n<1000;n++)log.invoke(db,"BoundedUnknownSample0");check(misses.size()==count&&queue.size()==256,"duplicate stays quiet at limit");
            db.setRecordUnknown(false);log.invoke(db,"DisabledUnknownSample");check(!misses.contains("DisabledUnknownSample"),"disabled recorder");
        }finally{release.countDown();}
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(((java.util.concurrent.atomic.AtomicBoolean)field(db,"unknownScheduled")).get()&&System.nanoTime()<deadline)Thread.sleep(5);
        check(queue.isEmpty()&&!((java.util.concurrent.atomic.AtomicBoolean)field(db,"unknownScheduled")).get(),"background drained and scope reset");
        var lines=Files.readAllLines(root.resolve("unknown.tsv"));check(lines.size()==256&&new HashSet<>(lines).size()==256,"all accepted lines written once/in batches");check(lines.getFirst().equals("BoundedUnknownSample0"),"original log order retained");writer.shutdown();check(writer.awaitTermination(5,TimeUnit.SECONDS),"test writer stopped");
        var quiet=new OfflineDictionary(root.resolve("quiet"));quiet.translateForSearch("QuietUnknownSample");check(((Set<?>)field(quiet,"misses")).isEmpty(),"quiet search not recorded");
        ((ExecutorService)field(quiet,"unknownWriter")).shutdown();
        System.out.println("PASS: unknown writer62 "+checks+" assertions;16000-event blocked-I/O burst,15000 unique cap/256 queue/1 task/64-line batches,dedup/disabled/quiet;diagnostics may drop excess,translations unaffected");
    }
}
