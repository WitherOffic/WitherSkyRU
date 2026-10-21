package ru.wither.skyru;

import java.nio.file.*;
import java.net.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

/** Frozen72 nickname protection, exact collisions and bounded lookup fallback. */
public final class TestNames73 {
    private static int checks;
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;}
    private static final class Old extends URLClassLoader {
        Old(URL jar){super(new URL[]{jar},TestNames73.class.getClassLoader());}
        @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
            if(!name.startsWith("ru.wither.skyru."))return super.loadClass(name,resolve);
            synchronized(getClassLoadingLock(name)){var found=findLoadedClass(name);if(found==null)found=findClass(name);if(resolve)resolveClass(found);return found;}
        }
    }
    private static Predicate<String> prepare(Method method,Set<String> names)throws Exception{return(Predicate<String>)method.invoke(null,names);}
    private static String[] table(Predicate<String> index)throws Exception{var field=index.getClass().getDeclaredField("table");field.setAccessible(true);return(String[])field.get(index);}
    private static void compare(Predicate<String> current,Predicate<String> old,String text){check(current.test(text)==old.test(text),"same72 exact-case token/formatting policy: "+text);}
    public static void main(String[] args)throws Exception{
        var project=Path.of(args[1]).toAbsolutePath().getParent();var jar=project.resolve("build/libs/WitherSkyRU-26.1.2-fabric-0.15.72-dev.jar");
        check(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))).equalsIgnoreCase("1645060A5C13D085B44AFE3666009F88F31BD55CD8C6ED2E0AF13B76EFD48E5D"),"pinned72 oracle");
        var currentPrepare=PlayerNames.class.getDeclaredMethod("prepare",Set.class);currentPrepare.setAccessible(true);
        try(var loader=new Old(jar.toUri().toURL())){
            var oldType=loader.loadClass("ru.wither.skyru.PlayerNames");var oldPrepare=oldType.getDeclaredMethod("prepare",Set.class);oldPrepare.setAccessible(true);
            var sample=new HashSet<String>(List.of("Wither_101","Thor","NORMAL","12","a-b","Имя","Player Name","😀",""));sample.add(null);
            var current=prepare(currentPrepare,sample);var previous=prepare(oldPrepare,sample);
            compare(current,previous,null);compare(current,previous,"");
            for(String name:sample)if(name!=null)for(String before:List.of("","[MVP+] ","x","_","§a","😀","Русский ","\uD800"))for(String after:List.of("",": hi","x","_","😀"," §r","\uDC00"))compare(current,previous,before+name+after);
            for(String text:List.of("Thorns","Thor_","xThor","thor","NORMAL","normal","Not a Player Name here","§aWither_101§r","§xWither_101","12,345","112","Player\tName"))compare(current,previous,text);
            for(int size:new int[]{0,1,80,128,512,513,1024}){
                var names=new HashSet<String>();for(int n=0;n<size;n++)names.add("Player"+n);
                var now=prepare(currentPrepare,names);var old=prepare(oldPrepare,names);var slots=table(now);
                check(size<=512?slots!=null&&slots.length<=1024&&(slots.length&(slots.length-1))==0:slots==null,"bounded1024 table,oversized old-path fallback");
                if(slots!=null){int count=0;for(String name:slots)if(name!=null){check(names.contains(name),"table retains roster names only");count++;}check(count==size,"every standard name inserted exactly once");}
                for(int n=0;n<1100;n++)for(String before:List.of("","x","[VIP] "))compare(now,old,before+"Player"+n+": hello");
            }
            // All Aa/BB concatenations have the same String hash, including misses.
            var collisions=new ArrayList<String>();for(int mask=0;mask<32;mask++){var name=new StringBuilder();for(int bit=0;bit<5;bit++)name.append((mask&(1<<bit))==0?"Aa":"BB");collisions.add(name.toString());}
            check(collisions.stream().map(String::hashCode).distinct().count()==1,"real32-way String hash collision fixture");
            var collisionNames=new HashSet<>(collisions.subList(0,20));var colliding=prepare(currentPrepare,collisionNames);var oldColliding=prepare(oldPrepare,collisionNames);
            for(int repeat=0;repeat<30;repeat++)for(String name:collisions){compare(colliding,oldColliding,"[VIP] "+name+": hello");compare(colliding,oldColliding,"x"+name);}
            var random=new Random(73);var names=new HashSet<String>(sample);for(int n=0;n<80;n++)names.add("User_"+n);var stable=prepare(currentPrepare,names);var oldStable=prepare(oldPrepare,names);
            String alphabet="AbcXYZ_019 -:§a😀Имя";
            for(int n=0;n<50000;n++){
                var text=new StringBuilder();for(int p=0,len=random.nextInt(100);p<len;p++)text.append(alphabet.charAt(random.nextInt(alphabet.length())));
                if(n%3==0)text.insert(random.nextInt(text.length()+1),"User_"+random.nextInt(100));
                compare(stable,oldStable,text.toString());
            }
            names.add("NewArrival");var updated=prepare(currentPrepare,names);check(updated.test("NewArrival")&&!stable.test("NewArrival"),"roster rebuild changes protection,old immutable index unchanged");
            var errors=new ConcurrentLinkedQueue<Throwable>();var threads=new ArrayList<Thread>();var start=new CountDownLatch(1);
            for(int t=0;t<4;t++){var thread=new Thread(()->{try{start.await();for(int n=0;n<10000;n++){String text=n%2==0?"[VIP] User_17: hello":"Thorns and other items 75";if(stable.test(text)!=oldStable.test(text))throw new AssertionError("concurrent result");}}catch(Throwable error){errors.add(error);}});threads.add(thread);thread.start();}
            start.countDown();for(var thread:threads)thread.join();check(errors.isEmpty(),"immutable index safe for4 concurrent readers: "+errors);
        }
        System.out.println("PASS: names73 "+checks+" assertions;frozen72 exact nickname/Unicode/formatting policy,50000 randomized inputs,32 hash collisions,8-probe fallback,1024-slot bound,oversized fallback,roster rebuild,4 concurrent readers;not live FPS proof");
    }
}
