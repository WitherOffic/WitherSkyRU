import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;
import java.util.regex.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** Differential rendering oracle from delivered61; no mutable outputs cached. */
public final class TestRenderFastPaths62 {
    private static int checks;
    private static void check(boolean ok,String note){if(!ok)throw new AssertionError(note);checks++;}
    private static final class Previous extends URLClassLoader {
        Previous(URL url){super(new URL[]{url},TestRenderFastPaths62.class.getClassLoader());}
        protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
            if(!name.startsWith("ru.wither.skyru."))return super.loadClass(name,resolve);
            synchronized(getClassLoadingLock(name)){var c=findLoadedClass(name);if(c==null)c=findClass(name);if(resolve)resolveClass(c);return c;}
        }
        public URL getResource(String name){if(name.startsWith("data/witherskyru/")){var result=findResource(name);if(result!=null)return result;}return super.getResource(name);}
    }
    private static void flag(Class<?> type,String n,Object v)throws Exception{var f=type.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static Object field(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    private static List<Style> styles(FormattedCharSequence s){var out=new ArrayList<Style>();s.accept((i,style,cp)->{for(int n=0;n<Character.charCount(cp);n++)out.add(style);return true;});return out;}
    private static String plain(FormattedCharSequence s){return AuditMiningStyles.plain(s);}
    private static void compare(Component source,Method oldComp,Method oldSeq)throws Exception{
        var before=(Component)oldComp.invoke(null,source);var after=SkyRuClient.component(source);
        check(plain(before.getVisualOrderText()).equals(plain(after.getVisualOrderText())),"component text oracle: "+source.getString());
        check(styles(before.getVisualOrderText()).equals(styles(after.getVisualOrderText())),"component style/actions oracle");
        check((before==source)==(after==source),"component original identity decision");
        var seq=source.getVisualOrderText();var a=(FormattedCharSequence)oldSeq.invoke(null,seq,false);var b=SkyRuClient.sequence(seq,false);
        check(plain(a).equals(plain(b)),"sequence text oracle: "+source.getString());check(styles(a).equals(styles(b)),"sequence style/actions oracle");check((a==seq)==(b==seq),"sequence identity decision");
    }
    private static ThreadLocal<Object> context(Class<?> type,Object value)throws Exception{var f=type.getDeclaredField("CURRENT");f.setAccessible(true);var local=(ThreadLocal<Object>)f.get(null);local.set(value);return local;}
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var project=Path.of(args[1]).toAbsolutePath().getParent();var oldJar=project.resolve("build/libs/WitherSkyRU-26.1.2-fabric-0.15.61-dev.jar");
        check(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(oldJar))).equalsIgnoreCase("A9129DFC982BB1854390456C027D08AAE6722DF889163E369D4D2E0A66D12771"),"delivered61 golden hash");
        var numeric=Class.forName("ru.wither.skyru.DisplayNoop").getDeclaredMethod("numbers",String.class);numeric.setAccessible(true);var safeField=OfflineDictionary.class.getDeclaredField("SAFE");safeField.setAccessible(true);var safe=(Pattern)safeField.get(null);
        for(int c=0;c<=65535;c++){String s=String.valueOf((char)c);check((boolean)numeric.invoke(null,s)==safe.matcher(s).matches(),"numeric ASCII/Unicode set oracle "+c);}
        var db=new OfflineDictionary(root.resolve("new"));db.setRecordUnknown(false);flag(SkyRuClient.class,"dictionary",db);flag(SkyRuClient.class,"enabled",true);flag(SkyRuClient.class,"onlyHypixel",false);
        try(var previous=new Previous(oldJar.toUri().toURL())){
            var oldClient=previous.loadClass("ru.wither.skyru.SkyRuClient");var oldDict=previous.loadClass("ru.wither.skyru.OfflineDictionary");var own=oldDict.getConstructor(Path.class).newInstance(root.resolve("old"));oldDict.getMethod("setRecordUnknown",boolean.class).invoke(own,false);
            flag(oldClient,"dictionary",own);flag(oldClient,"enabled",true);flag(oldClient,"onlyHypixel",false);
            var oldComp=oldClient.getMethod("component",Component.class);var oldSeq=oldClient.getMethod("sequence",FormattedCharSequence.class,boolean.class);
            var oldContext=previous.loadClass("ru.wither.skyru.RenderContext");var oldSnapshot=previous.loadClass("ru.wither.skyru.RenderContext$Snapshot").getConstructor(boolean.class,boolean.class,boolean.class,boolean.class);
            var newer=context(RenderContext.class,new RenderContext.Snapshot(false,false,false,false));var older=context(oldContext,oldSnapshot.newInstance(false,false,false,false));
            try{
                var exact=(Map<String,String>)field(db,"exact");check(exact.equals(field(own,"exact")),"no translation data changed");
                var style=Style.EMPTY.withColor(0x123456).withInsertion("current default").withClickEvent(new ClickEvent.RunCommand("/original-perf62")).withHoverEvent(new HoverEvent.ShowText(Component.literal("current hint")));
                for(String source:new TreeSet<>(exact.keySet()))compare(Component.literal(source).setStyle(style),oldComp,oldSeq);
                var random=new Random(62);for(int n=0;n<10000;n++){
                    String source=n%2==0?String.format(Locale.ROOT,"❤ %d/%d (+%.2f%%)",n,random.nextInt(100000),random.nextDouble()*100):"Gems: "+n;
                    var c=Component.empty();for(int i=0;i<source.length();i++)c.append(Component.literal(source.substring(i,i+1)).setStyle(style.withColor(0x100000+i).withInsertion("glyph "+n+"/"+i)));compare(c,oldComp,oldSeq);
                }
                for(boolean protect:List.of(false,true))for(boolean hud:List.of(false,true)){
                    newer.set(new RenderContext.Snapshot(protect,hud,false,false));older.set(oldSnapshot.newInstance(protect,hud,false,false));
                    for(String source:List.of("Normal","1,200/2,500 (+15%)","Gems: 75","SKYBLOCK","ПолученоOctober 13, 2026","A new unknown line","Обычный режим","www.hypixel.net"))compare(Component.literal(source).setStyle(style),oldComp,oldSeq);
                }
                newer.set(new RenderContext.Snapshot(false,false,false,false));older.set(oldSnapshot.newInstance(false,false,false,false));
                var oldEditorial=previous.loadClass("ru.wither.skyru.EditorialContext").getMethod("edits",List.class);
                var lore=Class.forName("ru.wither.skyru.ReviewedLoreStyles");var groupsField=lore.getDeclaredField("GROUPS");groupsField.setAccessible(true);
                for(var group:(List<List<?>>)groupsField.get(null)){
                    var source=new ArrayList<String>();source.add("Unknown title");for(var row:group){var method=row.getClass().getDeclaredMethod("text");method.setAccessible(true);source.add(((String)method.invoke(row)).replace("{0}","12.5").replace("{1}","13"));}source.add("Unknown neighbour");
                    for(int n=0;n<source.size();n++){var sample=new ArrayList<>(source);if(n>0)sample.set(n,sample.get(n)+" UNVISITED");check(((List<?>)oldEditorial.invoke(null,sample)).toString().equals(EditorialContext.edits(sample).toString()),"context edits/order oracle");}
                }
                var custom=root.resolve("custom");Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),"Normal\tМой режим\nGems:\tМои камни:\n123\tНЕ ЧИСЛО\n");db=new OfflineDictionary(custom);own=oldDict.getConstructor(Path.class).newInstance(custom);flag(SkyRuClient.class,"dictionary",db);flag(oldClient,"dictionary",own);
                for(String text:List.of("Normal","Gems: 123","123","Обычный режим","Diamond Pickaxe"))compare(Component.literal(text).setStyle(style),oldComp,oldSeq);
            }finally{newer.remove();older.remove();}
        }
        flag(SkyRuClient.class,"enabled",false);var off=Component.literal("Normal");check(SkyRuClient.componentOutsideHud(off)==off,"disabled original");
        System.out.println("PASS: render fast paths62 "+checks+" assertions;36032 exact keys vs delivered61,10000 dynamic fragmented fresh glyph/action cases,BMP numeric-set oracle,contexts/overrides/identity/editorial-order;not live FPS");
    }
}
