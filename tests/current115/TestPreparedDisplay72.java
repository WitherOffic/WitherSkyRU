package ru.wither.skyru;

import java.nio.file.*;
import java.net.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** Frozen71 oracle; warmed plans, fresh current styles/actions, exact glyph indices. */
public final class TestPreparedDisplay72 {
    private static int checks;
    private static void check(boolean ok,String note){if(!ok)throw new AssertionError(note);checks++;}
    private static void flag(Class<?> type,String name,Object value)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static Object value(Object owner,String name)throws Exception{var f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    private static final class Old extends URLClassLoader {
        Old(URL jar){super(new URL[]{jar},TestPreparedDisplay72.class.getClassLoader());}
        protected Class<?> loadClass(String n,boolean r)throws ClassNotFoundException{if(!n.startsWith("ru.wither.skyru."))return super.loadClass(n,r);synchronized(getClassLoadingLock(n)){var c=findLoadedClass(n);if(c==null)c=findClass(n);if(r)resolveClass(c);return c;}}
    }
    private record Glyph(int index,int point,Style style){}
    private static List<Glyph> glyphs(FormattedCharSequence source){var out=new ArrayList<Glyph>();source.accept((i,s,cp)->{out.add(new Glyph(i,cp,s));return true;});return out;}
    private static FormattedCharSequence forward(String s,Style style){return FormattedCharSequence.forward(s,style);}
    private static String plain(FormattedCharSequence source){var out=new StringBuilder();source.accept((i,s,cp)->{out.appendCodePoint(cp);return true;});return out.toString();}
    private static ThreadLocal<Object> context(Class<?> type,Object snapshot)throws Exception{var f=type.getDeclaredField("CURRENT");f.setAccessible(true);var local=(ThreadLocal<Object>)f.get(null);local.set(snapshot);return local;}
    private static void compare(Method old,FormattedCharSequence source,boolean hud)throws Exception{
        var expected=(FormattedCharSequence)old.invoke(null,source,hud);var actual=SkyRuClient.sequence(source,hud);
        check(glyphs(expected).equals(glyphs(actual)),"exact displayed glyph/style/action/index oracle: "+plain(source));
        check((expected==source)==(actual==source),"original identity oracle");
    }
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var project=Path.of(args[1]).toAbsolutePath().getParent();var jar=project.resolve("build/libs/WitherSkyRU-26.1.2-fabric-0.15.71-dev.jar");
        check(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))).equalsIgnoreCase("0BAA651B0D612D95AC0D3F486278094C0B16497CD6618914791C8BA21316CA46"),"frozen delivered71");
        var db=new OfflineDictionary(root.resolve("current"));db.setRecordUnknown(false);flag(SkyRuClient.class,"dictionary",db);flag(SkyRuClient.class,"enabled",true);flag(SkyRuClient.class,"onlyHypixel",false);
        var local=context(RenderContext.class,new RenderContext.Snapshot(false,true,false,false));
        try(var old=new Old(jar.toUri().toURL())){
            var oldDbType=old.loadClass("ru.wither.skyru.OfflineDictionary");var oldDb=oldDbType.getConstructor(Path.class).newInstance(root.resolve("previous"));oldDbType.getMethod("setRecordUnknown",boolean.class).invoke(oldDb,false);
            var oldClient=old.loadClass("ru.wither.skyru.SkyRuClient");flag(oldClient,"dictionary",oldDb);flag(oldClient,"enabled",true);flag(oldClient,"onlyHypixel",false);
            var oldContext=old.loadClass("ru.wither.skyru.RenderContext");var snapshot=old.loadClass("ru.wither.skyru.RenderContext$Snapshot").getConstructor(boolean.class,boolean.class,boolean.class,boolean.class);
            var oldLocal=context(oldContext,snapshot.newInstance(false,true,false,false));var oldSeq=oldClient.getMethod("sequence",FormattedCharSequence.class,boolean.class);
            var exact=OfflineDictionary.class.getDeclaredField("exact");exact.setAccessible(true);var oldExact=oldDbType.getDeclaredField("exact");oldExact.setAccessible(true);check(exact.get(db).equals(oldExact.get(oldDb)),"all dictionary data unchanged");
            for(String key:((Map<String,String>)exact.get(db)).keySet())for(int draw=0;draw<4;draw++){
                var style=Style.EMPTY.withColor(0x123450+draw).withBold(draw%2==0).withInsertion("current"+draw).withClickEvent(new ClickEvent.RunCommand("/current"+draw)).withHoverEvent(new HoverEvent.ShowText(Component.literal("hint"+draw)));
                compare(oldSeq,forward(key,style),true);
            }
            var random=new Random(72);for(int n=0;n<3000;n++){
                String text=List.of("Gems: 75","Pet: Rabbit","Enchanted Diamond Pickaxe","Talk to Susan","Updating ranking...","Cleanup: 52.1%","Sawdust: 5,011 (+2,283)","Normal").get(n%8);
                int split=Math.max(1,random.nextInt(text.length()));var first=Style.EMPTY.withColor(n).withHoverEvent(new HoverEvent.ShowText(Component.literal("fresh"+n)));var second=Style.EMPTY.withColor(n+1).withClickEvent(new ClickEvent.RunCommand("/draw"+n));
                for(int draw=0;draw<4;draw++)compare(oldSeq,FormattedCharSequence.composite(forward(text.substring(0,split),first),forward(text.substring(split),second)),draw%2==0);
            }
            for(boolean protect:List.of(true,false)){
                local.set(new RenderContext.Snapshot(protect,false,false,false));oldLocal.set(snapshot.newInstance(protect,false,false,false));
                for(int n=0;n<8;n++)compare(oldSeq,forward("Gems: 75",Style.EMPTY),false);
            }
            local.set(new RenderContext.Snapshot(false,false,false,false));oldLocal.set(snapshot.newInstance(false,false,false,false));
            try(var technical=TechnicalText.open()){var raw=forward("Gems: 75",Style.EMPTY);check(SkyRuClient.sequence(raw,true)==raw,"F3 bypass before prepared cache");}
            var protectedTitle=OriginalItemTitle.protect(forward("Diamond Pickaxe",Style.EMPTY));check(SkyRuClient.sequence(protectedTitle,true)==protectedTitle,"Shift title before cache");
            var custom=root.resolve("custom");Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),"Normal\tМой новый режим\nGems:\tМои кристаллы:\n");
            var customDb=new OfflineDictionary(custom);customDb.setRecordUnknown(false);var oldCustom=oldDbType.getConstructor(Path.class).newInstance(custom);flag(SkyRuClient.class,"dictionary",customDb);flag(oldClient,"dictionary",oldCustom);
            for(int n=0;n<8;n++)for(String s:List.of("Normal","Gems: 75"))compare(oldSeq,forward(s,Style.EMPTY),true);
            for(int phase=0;phase<3;phase++){
                var names=phase==1?Set.of("Normal"):Set.<String>of();
                for(Class<?> player:List.of(PlayerNames.class,old.loadClass("ru.wither.skyru.PlayerNames"))){flag(player,"names",names);flag(player,"checkedAt",System.nanoTime());flag(player,"connection",null);}
                for(int n=0;n<5;n++)compare(oldSeq,FormattedCharSequence.composite(forward("Normal",Style.EMPTY.withColor(1)),forward("ly",Style.EMPTY.withColor(2))),false);
            }
            for(int n=0;n<16;n++){
                var style=Style.EMPTY.withColor(n+300).withShadowColor(n).withItalic(n%2==0).withUnderlined(n%3==0).withStrikethrough(n%5==0).withObfuscated(n%7==0);
                compare(oldSeq,forward("Gems: 75",style),true);
            }
            oldLocal.remove();flag(SkyRuClient.class,"dictionary",db);
        }
        var calls=new AtomicInteger();PreparedDisplay.Renderer renderer=(source,current,dictionary,compact)->{
            calls.incrementAndGet();return FormattedCharSequence.composite(forward("Готовый 😀",current.styleAt(0)),forward("!",current.styleAt(current.text.length()-1)));
        };
        for(int n=0;n<40;n++){
            var hover=Component.literal("tooltip"+n);var a=Style.EMPTY.withColor(n).withHoverEvent(new HoverEvent.ShowText(hover));var b=Style.EMPTY.withColor(n+100).withClickEvent(new ClickEvent.RunCommand("/action"+n));
            var source=FormattedCharSequence.composite(forward("memo",a),forward(" text",b));var current=StyledRuns.collect(source);var result=PreparedDisplay.render(source,current,db,false,renderer);
            check(plain(result).equals("Готовый 😀!"),"prepared glyphs intact");var glyph=glyphs(result);check(glyph.getFirst().style()==a&&glyph.getLast().style()==b,"bind current styles by reference, not previous actions");hover.append(" updated");check(((HoverEvent.ShowText)glyph.getFirst().style().getHoverEvent()).value().getString().endsWith(" updated"),"current mutable hover content not frozen");
            if(n>=2){check(calls.get()==2,"stable text does not retranslate after admission");check(glyph.getLast().index()==0,"composite glyph index preserved");}
        }
        for(int n=0;n<1000;n++){
            var a=Style.EMPTY.withColor(1);var b=Style.EMPTY.withColor(2);var c=n%2==0?a:Style.EMPTY.withColor(3);
            var source=FormattedCharSequence.composite(forward("a",a),forward("b",b),forward("c",c));var current=StyledRuns.collect(source);
            PreparedDisplay.Renderer copy=(s,r,d,compact)->FormattedCharSequence.composite(forward("A",r.runs.get(0).style()),forward("C",r.runs.get(2).style()));
            var result=PreparedDisplay.render(source,current,db,false,copy);check(glyphs(result).getLast().style()==c,"nonadjacent equal-style partition invalidation");
        }
        var noChange=new AtomicInteger();for(int n=0;n<10;n++){var source=forward("Unchanged memo text",Style.EMPTY);check(PreparedDisplay.render(source,StyledRuns.collect(source),db,false,(s,r,d,c)->{noChange.incrementAndGet();return s;})==source,"unchanged plan returns current original");}check(noChange.get()==2,"negative result memoized without retaining original");
        var changed=new AtomicInteger();for(int n=0;n<10000;n++){
            String text="Dynamic counter "+n;var source=forward(text,Style.EMPTY);var result=PreparedDisplay.render(source,StyledRuns.collect(source),db,false,(s,r,d,c)->{changed.incrementAndGet();return forward("Новое "+r.text,r.runs.getFirst().style());});check(plain(result).equals("Новое "+text),"changing input never freezes");
        }check(changed.get()==10000,"unique dynamic inputs translated once, not stale hits");
        check("Aa".hashCode()=="BB".hashCode(),"real String hash collision fixture");
        for(int n=0;n<30;n++){String text=n%2==0?"Aa":"BB";var source=forward(text,Style.EMPTY);var result=PreparedDisplay.render(source,StyledRuns.collect(source),db,false,(s,r,d,c)->forward("Текст "+r.text,r.runs.getFirst().style()));check(plain(result).equals("Текст "+text),"hash collision never returns another entry's glyphs");}
        for(int n=0;n<6;n++){
            var source=forward("Aborted sink memo",Style.EMPTY);var result=PreparedDisplay.render(source,StyledRuns.collect(source),db,false,(s,r,d,c)->forward("АБВГД",r.runs.getFirst().style()));var count=new AtomicInteger();
            check(!result.accept((i,s,cp)->count.incrementAndGet()<3)&&count.get()==3,"prepared accept stops exactly when requested");
        }
        for(int n=0;n<20;n++){
            var outer=forward("Outer recursive memo",Style.EMPTY);
            var result=PreparedDisplay.render(outer,StyledRuns.collect(outer),db,false,(s,r,d,c)->{
                var inner=forward("Inner recursive memo",Style.EMPTY);var inside=PreparedDisplay.render(inner,StyledRuns.collect(inner),d,c,(is,ir,id,ic)->forward("Внутри",ir.runs.getFirst().style()));
                check(plain(inside).equals("Внутри"),"nested cache keeps inner glyphs");return forward("Снаружи",r.runs.getFirst().style());
            });check(plain(result).equals("Снаружи"),"nested lookup never commits another call's glyphs");
        }
        for(int n=0;n<6;n++){
            var source=forward("Derived unsupported style",Style.EMPTY);var result=PreparedDisplay.render(source,StyledRuns.collect(source),db,false,(s,r,d,c)->forward("Производный",Style.EMPTY.withBold(true)));
            check(glyphs(result).getFirst().style().isBold(),"unmappable derived style falls through unchanged");
        }
        var cacheField=PreparedDisplay.class.getDeclaredField("CACHE");cacheField.setAccessible(true);var cacheLocal=(ThreadLocal<?>)cacheField.get(null);var cache=cacheLocal.get();var entries=(Object[])value(cache,"entries");check(entries.length==128,"fixed slot limit");check((int)value(cache,"glyphs")<=PreparedDisplay.MAX_GLYPHS,"glyph budget");
        for(int n=0;n<500;n++){
            var source=forward("Budget memo "+n,Style.EMPTY);
            for(int draw=0;draw<4;draw++)PreparedDisplay.render(source,StyledRuns.collect(source),db,false,(s,r,d,c)->forward("я".repeat(500),r.runs.getFirst().style()));
            int total=0;for(Object e:(Object[])value(cache,"entries"))if(e!=null){var plan=value(e,"plan");if(plan!=null){var points=(int[])value(plan,"points");if(points!=null)total+=points.length;}}
            check(total==(int)value(cache,"glyphs")&&total<=PreparedDisplay.MAX_GLYPHS,"aggregate glyph budget / eviction accounting");
        }
        var oversized=new AtomicInteger();for(int n=0;n<6;n++){var source=forward("Oversized output memo",Style.EMPTY);var result=PreparedDisplay.render(source,StyledRuns.collect(source),db,false,(s,r,d,c)->{oversized.incrementAndGet();return forward("я".repeat(600),r.runs.getFirst().style());});check(plain(result).length()==600,"oversized plan retains ordinary rendering");}check(oversized.get()==6,"oversized output not retained as a plan");
        for(Object entry:entries)if(entry!=null)for(var field:entry.getClass().getDeclaredFields())check(!Style.class.isAssignableFrom(field.getType())&&!Component.class.isAssignableFrom(field.getType())&&!FormattedCharSequence.class.isAssignableFrom(field.getType())&&!StyledRuns.class.isAssignableFrom(field.getType()),"entry has no render object fields");
        var maps=Collections.synchronizedList(new ArrayList<Object>());var errors=new ConcurrentLinkedQueue<Throwable>();var threads=new ArrayList<Thread>();
        for(int t=0;t<4;t++){var thread=new Thread(()->{try{var source=forward("Thread memo",Style.EMPTY);for(int n=0;n<20;n++)PreparedDisplay.render(source,StyledRuns.collect(source),db,false,(s,r,d,c)->forward("Готово",Style.EMPTY));maps.add(cacheLocal.get());}catch(Throwable error){errors.add(error);}});threads.add(thread);thread.start();}for(var thread:threads)thread.join();check(errors.isEmpty(),"parallel memoization: "+errors);for(int a=0;a<maps.size();a++)for(int b=a+1;b<maps.size();b++)check(maps.get(a)!=maps.get(b),"independent per-thread caches");
        local.remove();System.out.println("PASS: prepared display72 "+checks+" assertions; frozen71 36044-key warmed oracle, fresh actions/colors/hover/indices,partition/dynamic/overrides/guards/bounds/concurrency; not game FPS or0.1%low");
    }
}
