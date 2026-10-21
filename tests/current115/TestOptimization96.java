package ru.wither.skyru;

import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** Delivered95 oracle: exact parser boundaries, fresh glyphs/actions and deterministic local plans. */
public final class TestOptimization96 {
    private static int checks;
    private static final List<String> NAMES=List.of("Sawdust","Cleanup","Gems","North Stars","Coins/h","Farming XP/h","Farming Level","Yaw","Pitch");
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
    private static void flag(Class<?> cls,String name,Object v)throws Exception{var f=cls.getDeclaredField(name);f.setAccessible(true);f.set(null,v);}
    private static Object value(Class<?> cls,Object owner,String name)throws Exception{var f=cls.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    private static Method method(Class<?> cls,String name,Class<?>... args)throws Exception{var m=cls.getDeclaredMethod(name,args);m.setAccessible(true);return m;}
    private static final class Previous extends URLClassLoader {
        Previous(URL jar){super(new URL[]{jar},TestOptimization96.class.getClassLoader());}
        @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
            if(!name.startsWith("ru.wither.skyru."))return super.loadClass(name,resolve);
            synchronized(getClassLoadingLock(name)){var cls=findLoadedClass(name);if(cls==null)cls=findClass(name);if(resolve)resolveClass(cls);return cls;}
        }
        @Override public URL getResource(String name){if(name.startsWith("data/witherskyru/")){var url=findResource(name);if(url!=null)return url;}return super.getResource(name);}
    }
    private record Glyph(int index,int point,Style style){}
    private static List<Glyph> glyphs(FormattedCharSequence s){var out=new ArrayList<Glyph>();s.accept((i,style,cp)->{out.add(new Glyph(i,cp,style));return true;});return out;}
    private static List<String> edits(Object object)throws Exception{
        var result=new ArrayList<String>();for(var e:(List<?>)object){var cls=e.getClass();result.add(cls.getMethod("start").invoke(e)+":"+cls.getMethod("end").invoke(e)+":"+cls.getMethod("replacement").invoke(e));}return result;
    }
    private static FormattedCharSequence source(String text,int seed){
        var style=Style.EMPTY.withColor(seed).withInsertion("fresh "+seed).withClickEvent(new ClickEvent.SuggestCommand("/fresh "+seed)).withHoverEvent(new HoverEvent.ShowText(Component.literal("current "+seed)));
        if(seed%3==0||text.isEmpty())return FormattedCharSequence.forward(text,style);
        int at=seed%3==1?Math.max(1,text.indexOf(':')+1):Math.max(1,text.length()/2);
        return FormattedCharSequence.composite(FormattedCharSequence.forward(text.substring(0,at),style),FormattedCharSequence.forward(text.substring(at),style.withColor(seed+1).withClickEvent(new ClickEvent.RunCommand("/other "+seed))));
    }
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var project=Path.of(args[1]).toAbsolutePath().getParent();var jar=project.resolve("build/libs/WitherSkyRU-26.1.2-fabric-0.15.95-dev.jar");
        check(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))).equalsIgnoreCase("E5D397D17E7F7ADF9EAD2C9BF7E607B134F2412EF89CB4D951CB78944825BCBF"),"frozen delivered95");
        var db=new OfflineDictionary(root.resolve("plain"));db.setRecordUnknown(false);
        var data=(Map<String,String>)value(OfflineDictionary.class,db,"exact");
        var rows=new ArrayList<String>(data.keySet());
        var boundaries=List.of("","+","-","0","01","+1","-1","1.2",".2","1/2","1 (+12%)","1 (+999%)","1 (+1000%)","1x","1X","1-","1 ","1 2","1\t2","1\n","1\r\n","١2","1١","1😀","1\u2028","1"+"9".repeat(47),"1"+"9".repeat(48),"1"+"9".repeat(49),"1"+"x".repeat(48),"1"+"x".repeat(49));
        for(String name:NAMES)for(int padding=0;padding<=8;padding++)for(String number:boundaries){String row=" ".repeat(padding)+name+": "+number;rows.add(row);rows.add(row.toLowerCase(Locale.ROOT));rows.add("§a"+row);}
        var random=new Random(96);String alphabet="0123456789,./%+() x-\t\r\nABC😀";
        for(int n=0;n<18000;n++){var b=new StringBuilder();int length=random.nextInt(57);for(int p=0;p<length;p++)b.append(alphabet.charAt(random.nextInt(alphabet.length())));rows.add(" ".repeat(random.nextInt(9))+NAMES.get(n%NAMES.size())+": "+b);}
        rows.addAll(List.of("", " ", "      ", "       ", "Capacity: 2,240 of each item","Sawdust: 1 (+12%)","Sawdust: 1.234, (+999%)","Plot - Carrot","Pet: Rabbit","Collected 123k","Chapter III","Vol: 1K/2K","Progress to Wheat IV: 25%"));
        var local=(ThreadLocal<Object>)value(RenderContext.class,null,"CURRENT");local.set(new RenderContext.Snapshot(false,true,false,false));
        flag(SkyRuClient.class,"dictionary",db);flag(SkyRuClient.class,"enabled",true);flag(SkyRuClient.class,"onlyHypixel",false);
        try(var previous=new Previous(jar.toUri().toURL())){
            var old=previous.loadClass("ru.wither.skyru.InterfaceDisplay");var oldKeys=method(old,"keys",String.class);var oldEdits=method(old,"edits",String.class,Function.class);var oldTranslate=method(old,"translate",String.class,Function.class);var oldShape=method(old,"hudShape",String.class);
            var pattern=(Pattern)value(old,null,"HUD_VALUE");var fast=method(InterfaceDisplay.class,"hudValue",String.class);
            Function<String,String> lookup=k->switch(k){case "Sawdust:"->"Опилки:";case "Gems:"->"Самоцветы:";case "Cleanup:"->"Уборка:";case "Pet:"->"Питомец:";case "Rabbit"->"Кролик";case "Plot"->"Участок";default->"Перевод «"+k+"»";};
            for(String row:rows){
                check((fast.invoke(null,row)!=null)==pattern.matcher(row).matches(),"prefix gate retains full original regex boundary");
                check(InterfaceDisplay.keys(row).equals(oldKeys.invoke(null,row)),"exact override keys: "+row);
                check(InterfaceDisplay.hudShape(row)==(boolean)oldShape.invoke(null,row),"shape oracle: "+row);
                var plan=InterfaceDisplay.edits(row,lookup);
                check(edits(plan).equals(edits(oldEdits.invoke(null,row,lookup))),"semantic span oracle: "+row);
                var expected=oldTranslate.invoke(null,row,lookup);
                check(Objects.equals(InterfaceDisplay.translate(row,lookup),expected),"plain oracle: "+row);
                check(Objects.equals(InterfaceDisplay.applyEdits(row,plan),expected),"local validated-plan reuse: "+row);
            }
            for(String row:rows.subList(data.size(),rows.size()))for(Function<String,String> special:List.<Function<String,String>>of(k->null,Function.identity())){
                check(edits(InterfaceDisplay.edits(row,special)).equals(edits(oldEdits.invoke(null,row,special))),"null/identity label plans");
                check(Objects.equals(InterfaceDisplay.translate(row,special),oldTranslate.invoke(null,row,special)),"null/identity fallback");
            }
            var oldDbType=previous.loadClass("ru.wither.skyru.OfflineDictionary");var oldClient=previous.loadClass("ru.wither.skyru.SkyRuClient");var oldSequence=oldClient.getMethod("sequence",FormattedCharSequence.class,boolean.class);
            flag(oldClient,"enabled",true);flag(oldClient,"onlyHypixel",false);
            var oldLocal=(ThreadLocal<Object>)value(previous.loadClass("ru.wither.skyru.RenderContext"),null,"CURRENT");oldLocal.set(previous.loadClass("ru.wither.skyru.RenderContext$Snapshot").getConstructor(boolean.class,boolean.class,boolean.class,boolean.class).newInstance(false,true,false,false));
            for(String mode:List.of("empty","unrelated","label","whole")){
                var directory=root.resolve(mode);Files.createDirectories(directory);
                if(!mode.equals("empty"))Files.writeString(directory.resolve("overrides.tsv"),switch(mode){case "unrelated"->"Unrelated caption\tМоя подпись\n";case "label"->"Gems:\tМои камни:\nSawdust:\tМои опилки:\n";default->"Gems: 42\tМои 42 камня\nSawdust: 42 (+12%)\tОсобый бонус\n";});
                db=new OfflineDictionary(directory);db.setRecordUnknown(false);var oldDb=oldDbType.getConstructor(Path.class).newInstance(directory);oldDbType.getMethod("setRecordUnknown",boolean.class).invoke(oldDb,false);flag(SkyRuClient.class,"dictionary",db);flag(oldClient,"dictionary",oldDb);
                for(int n=0;n<1800;n++){
                    String name=NAMES.get(n%NAMES.size()),row=" ".repeat(n%8)+name+": "+(n%3==0?42:n)+(name.equals("Sawdust")?" (+12%)":"");var input=source(row,n);boolean compact=(n&1)==0;
                    var expected=(FormattedCharSequence)oldSequence.invoke(null,input,compact);var actual=SkyRuClient.sequence(input,compact);
                    check(glyphs(actual).equals(glyphs(expected)),"live styles/actions/indices "+mode);
                    check((actual==input)==(expected==input),"original sequence identity "+mode);
                    check(db.translate(row).equals(oldDbType.getMethod("translate",String.class).invoke(oldDb,row)),"dictionary precedence "+mode);
                }
            }
            oldLocal.remove();
        }finally{local.remove();}
        System.out.println("PASS: optimization96 "+checks+" assertions;"+rows.size()+" parser rows including all "+data.size()+" exact keys;delivered95 regex/keys/spans/plain/local-plan oracle;7200 fresh styled sequences;empty/unrelated/label/whole overrides;not live FPS");
    }
}
