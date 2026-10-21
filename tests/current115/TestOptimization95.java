package ru.wither.skyru;

import java.net.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** Delivered94 oracle; no translation/data changes; fresh styles and immutable override decisions. */
public final class TestOptimization95 {
    private static int checks;
    private static void check(boolean ok,String note){if(!ok)throw new AssertionError(note);checks++;}
    private static void flag(Class<?> type,String name,Object value)throws Exception{var field=type.getDeclaredField(name);field.setAccessible(true);field.set(null,value);}
    private static Object value(Class<?> type,Object owner,String name)throws Exception{var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(owner);}
    private static Method method(Class<?> type,String name,Class<?>... args)throws Exception{var m=type.getDeclaredMethod(name,args);m.setAccessible(true);return m;}
    private static final class Previous extends URLClassLoader {
        Previous(URL jar){super(new URL[]{jar},TestOptimization95.class.getClassLoader());}
        @Override protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException{
            if(!n.startsWith("ru.wither.skyru."))return super.loadClass(n,resolve);
            synchronized(getClassLoadingLock(n)){var type=findLoadedClass(n);if(type==null)type=findClass(n);if(resolve)resolveClass(type);return type;}
        }
        @Override public URL getResource(String name){if(name.startsWith("data/witherskyru/")){var url=findResource(name);if(url!=null)return url;}return super.getResource(name);}
    }
    private record Glyph(int index,int point,Style style){}
    private static List<Glyph> glyphs(FormattedCharSequence source){var result=new ArrayList<Glyph>();source.accept((i,s,cp)->{result.add(new Glyph(i,cp,s));return true;});return result;}
    private static List<String> edits(Object object)throws Exception{
        var result=new ArrayList<String>();for(var edit:(List<?>)object){var type=edit.getClass();result.add(type.getMethod("start").invoke(edit)+":"+type.getMethod("end").invoke(edit)+":"+type.getMethod("replacement").invoke(edit));}return result;
    }
    private static Style style(int n){return Style.EMPTY.withColor(n).withBold((n&1)==0).withInsertion("current "+n).withClickEvent(new ClickEvent.RunCommand("/current "+n)).withHoverEvent(new HoverEvent.ShowText(Component.literal("hint "+n)));}
    private static FormattedCharSequence sequence(String text,int n){
        if(text.isEmpty()||n%3==0)return FormattedCharSequence.forward(text,style(n));
        var parts=new ArrayList<FormattedCharSequence>();int split=Math.max(1,text.length()/2);
        parts.add(FormattedCharSequence.forward(text.substring(0,split),style(n)));parts.add(FormattedCharSequence.forward(text.substring(split),style(n+1)));return FormattedCharSequence.composite(parts);
    }
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var project=Path.of(args[1]).toAbsolutePath().getParent();var jar=project.resolve("build/libs/WitherSkyRU-26.1.2-fabric-0.15.94-dev.jar");
        check(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))).equalsIgnoreCase("A784B75D65EFBCB8246C72CEE0AE7B60525EBDA9AA07B664127BC39A547BAC00"),"frozen delivered94");
        flag(SkyRuClient.class,"enabled",true);flag(SkyRuClient.class,"onlyHypixel",false);
        var local=(ThreadLocal<Object>)value(RenderContext.class,null,"CURRENT");local.set(new RenderContext.Snapshot(false,true,false,false));
        int keyCount=0;
        try(var previous=new Previous(jar.toUri().toURL())){
            var oldType=previous.loadClass("ru.wither.skyru.OfflineDictionary");var oldClient=previous.loadClass("ru.wither.skyru.SkyRuClient");
            flag(oldClient,"enabled",true);flag(oldClient,"onlyHypixel",false);
            var oldContext=previous.loadClass("ru.wither.skyru.RenderContext");var oldLocal=(ThreadLocal<Object>)value(oldContext,null,"CURRENT");
            oldLocal.set(previous.loadClass("ru.wither.skyru.RenderContext$Snapshot").getConstructor(boolean.class,boolean.class,boolean.class,boolean.class).newInstance(false,true,false,false));
            var oldEdits=method(oldType,"displayEdits",String.class);var oldPolicy=method(oldType,"preserveRequirementOnStyleFailure",String.class);var oldSequence=oldClient.getMethod("sequence",FormattedCharSequence.class,boolean.class);var oldNarrow=oldClient.getMethod("narrowHud",FormattedCharSequence.class,FormattedCharSequence.class);
            for(String mode:List.of("empty","unrelated","selected")){
                var directory=root.resolve(mode);Files.createDirectories(directory);
                if(!mode.equals("empty"))Files.writeString(directory.resolve("overrides.tsv"),mode.equals("unrelated")?"Unrelated caption\tМоя подпись\n":"Gems:\tМои самоцветы:\nNormal\tМой режим\nPet:\tМой питомец:\nRabbit\tЗаяц\nBestiary Milestone\tМой этап\n");
                var db=new OfflineDictionary(directory);db.setRecordUnknown(false);var oldDb=oldType.getConstructor(Path.class).newInstance(directory);oldType.getMethod("setRecordUnknown",boolean.class).invoke(oldDb,false);
                check(db.hasCustomOverrides()!=mode.equals("empty"),"per dictionary empty flag");
                flag(SkyRuClient.class,"dictionary",db);flag(oldClient,"dictionary",oldDb);
                var keys=(Map<String,String>)value(OfflineDictionary.class,db,"exact");check(keys.equals(value(oldType,oldDb,"exact")),"exact data unchanged");
                var rows=new ArrayList<String>();
                if(mode.equals("empty")){rows.addAll(keys.keySet());keyCount=keys.size();}
                for(int n=0;n<300;n++)for(String row:List.of("Gems: "+n,"  Cleanup: "+n+".25%","Sawdust: "+n+" (+12%)","North Stars: "+n,"Collected "+n+"k","Pet: Rabbit","Normal","Farming XP/h: "+n,"Unknown dynamic "+n,"Capacity: "+n+" of each item","Expires in: "+n+" seconds","☠ Voidgloom Seraph","Быстрая проверка "+n,"Museum (Dungeons): ✖ Not donated"))rows.add(row);
                for(String row:rows){
                    check(db.translate(row).equals(oldType.getMethod("translate",String.class).invoke(oldDb,row)),"plain oracle "+mode+"/"+row);
                    check(edits(db.displayEdits(row)).equals(edits(oldEdits.invoke(oldDb,row))),"display span oracle "+mode+"/"+row);
                    check(db.preserveRequirementOnStyleFailure(row)==(boolean)oldPolicy.invoke(oldDb,row),"style failure oracle "+mode+"/"+row);
                }
                for(int n=0;n<900;n++){
                    String row=rows.get((n*47)%rows.size());var source=sequence(row,n);boolean compact=(n&1)==0;
                    var expected=(FormattedCharSequence)oldSequence.invoke(null,source,compact);var actual=SkyRuClient.sequence(source,compact);
                    check(glyphs(expected).equals(glyphs(actual)),"fresh sequence styles/actions/indices "+mode);
                    check((expected==source)==(actual==source),"source identity "+mode);
                }
                for(int n=0;n<30;n++)for(String[] pair:List.of(new String[]{"Normal","Обычный режим"},new String[]{"Updating ranking...","Обновление места…"},new String[]{"Gems: 75","Самоцветы: 75"},new String[]{"- Benediction","- Благословение"})){
                    var source=sequence(pair[0],n);var translated=sequence(pair[1],n+1);int[] visits={0};FormattedCharSequence counted=s->{visits[0]++;return source.accept(s);};
                    var expected=(FormattedCharSequence)oldNarrow.invoke(null,source,translated);var actual=SkyRuClient.narrowHud(counted,translated);
                    check(glyphs(expected).equals(glyphs(actual)),"narrow exact live glyph oracle "+mode);
                    check((expected==translated)==(actual==translated),"narrow original identity "+mode);
                    check(visits[0]==(mode.equals("empty")?0:1),"no source parsing only for truly empty overrides");
                }
                // An already-loaded dictionary is immutable; only explicit reconstruction loads a file change.
                if(mode.equals("empty")){
                    Files.writeString(directory.resolve("overrides.tsv"),"Normal\tНовый режим\n");
                    check(!db.hasCustomOverrides(),"file edits cannot mutate current decision");
                    var replacement=new OfflineDictionary(directory);replacement.setRecordUnknown(false);check(replacement.hasCustomOverrides(),"reconstructed dictionary sees new override");
                    flag(SkyRuClient.class,"dictionary",replacement);var translated=FormattedCharSequence.forward("Обычный режим",Style.EMPTY);check(SkyRuClient.narrowHud(FormattedCharSequence.forward("Normal",Style.EMPTY),translated)==translated,"replacement override prevents compaction");flag(SkyRuClient.class,"dictionary",db);
                }
                var guardSource=FormattedCharSequence.forward("Normal",style(998));var untouched=FormattedCharSequence.forward("Обычный режим",style(999));flag(SkyRuClient.class,"enabled",false);check(SkyRuClient.narrowHud(guardSource,untouched)==untouched,"disabled bypass");flag(SkyRuClient.class,"enabled",true);
                try(var technical=TechnicalText.open()){check(SkyRuClient.sequence(guardSource,true)==guardSource,"F3 still bypassed");}
            }
            oldLocal.remove();
        }finally{local.remove();}
        System.out.println("PASS: optimization95 "+checks+" assertions;all "+keyCount+" exact keys plus 12600 dynamic/context rows;delivered94 oracle;empty/unrelated/selected overrides;fresh styles/actions/indices;no empty-set source traversal;not live FPS");
    }
}
