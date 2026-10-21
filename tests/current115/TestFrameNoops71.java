import java.nio.file.*;
import java.net.*;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

public final class TestFrameNoops71 {
    private static int checks;
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    private static final class Old extends URLClassLoader {
        Old(URL jar){super(new URL[]{jar},TestFrameNoops71.class.getClassLoader());}
        protected Class<?> loadClass(String n,boolean r)throws ClassNotFoundException{if(!n.startsWith("ru.wither.skyru."))return super.loadClass(n,r);synchronized(getClassLoadingLock(n)){var c=findLoadedClass(n);if(c==null)c=findClass(n);if(r)resolveClass(c);return c;}}
    }
    private static void flag(Class<?> type,String name,Object value)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static List<Style> styles(FormattedCharSequence text){var out=new ArrayList<Style>();text.accept((i,s,cp)->{out.add(s);return true;});return out;}
    public static void main(String[] args)throws Exception{
        var project=Path.of(args[1]).toAbsolutePath().getParent();var jar=project.resolve("build/libs/WitherSkyRU-26.1.2-fabric-0.15.70-dev.jar");
        check(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))).equalsIgnoreCase("32AFF41415A452DCF90AE7D02956F7BC33F0F96725206C13CDDEFB0F66CF3DA5"),"frozen delivered70");
        var style=Style.EMPTY.withColor(0x123456).withBold(true).withInsertion("unchanged").withClickEvent(new ClickEvent.RunCommand("/original")).withHoverEvent(new HoverEvent.ShowText(Component.literal("original hint")));
        try(var old=new Old(jar.toUri().toURL())){
            var type=old.loadClass("ru.wither.skyru.DisplayNoop");var numbers=type.getDeclaredMethod("numbers",String.class);numbers.setAccessible(true);
            var db=new OfflineDictionary(Path.of(args[0]).resolve("current"));db.setRecordUnknown(false);
            var oldDbType=old.loadClass("ru.wither.skyru.OfflineDictionary");var oldDb=oldDbType.getConstructor(Path.class).newInstance(Path.of(args[0]).resolve("previous"));oldDbType.getMethod("setRecordUnknown",boolean.class).invoke(oldDb,false);
            var oldClient=old.loadClass("ru.wither.skyru.SkyRuClient");
            for(Class<?> client:List.of(SkyRuClient.class,oldClient)){flag(client,"dictionary",client==SkyRuClient.class?db:oldDb);flag(client,"enabled",true);flag(client,"onlyHypixel",false);}
            var exact=OfflineDictionary.class.getDeclaredField("exact");exact.setAccessible(true);var oldExact=oldDbType.getDeclaredField("exact");oldExact.setAccessible(true);
            check(exact.get(db).equals(oldExact.get(oldDb)),"no dictionary changes vs delivered70");
            var oldSequence=oldClient.getMethod("sequenceOutsideHud",FormattedCharSequence.class);
            for(String key:((Map<String,String>)exact.get(db)).keySet()){
                var source=FormattedCharSequence.forward(key,style);var before=(FormattedCharSequence)oldSequence.invoke(null,source);var after=SkyRuClient.sequenceOutsideHud(source);
                check(AuditMiningStyles.plain(before).equals(AuditMiningStyles.plain(after)),"delivered70 displayed text: "+key);
                check(styles(before).equals(styles(after)),"delivered70 styles/actions: "+key);
                check((before==source)==(after==source),"delivered70 identity decision: "+key);
            }
            for(int cp=0;cp<=0x10ffff;cp++){
                if(cp>=0xd800&&cp<=0xdfff)continue;
                String text=new String(Character.toChars(cp));boolean expected=(boolean)numbers.invoke(null,text);
                check(SkyRuClient.numericSequence(FormattedCharSequence.forward(text,style))==expected,"delivered70 code point policy: "+cp);
            }
            var random=new Random(71);for(int n=0;n<20000;n++){
                String text=n%2==0?String.format(Locale.ROOT,"❤ %d/%d (+%.2f%%)",n,random.nextInt(1000000),random.nextDouble()*100):List.of("Normal","Gems: 75","1,234.5 coins","0.1% Low","Русский текст","SKYBLOCK","150 FPS","1s","1§a2").get(n%9);
                var pieces=new ArrayList<FormattedCharSequence>();for(int i=0;i<text.length();i++)pieces.add(FormattedCharSequence.forward(text.substring(i,i+1),style.withColor(i).withInsertion("glyph"+i)));
                var sequence=FormattedCharSequence.composite(pieces);
                check(SkyRuClient.numericSequence(sequence)==(boolean)numbers.invoke(null,text),"fragmented live actions/numbers "+n);
                check(AuditMiningStyles.plain(sequence).equals(text),"probe does not change source");
            }
        }
        check(SkyRuClient.numericSequence(FormattedCharSequence.EMPTY),"empty no-op");
        check(!SkyRuClient.numericSequence(null),"null probe safe");
        check(!SkyRuClient.numericSequence(sink->{throw new IllegalStateException("foreign sequence");}),"foreign failure falls through, not crash");
        var left=net.minecraft.client.gui.TextAlignment.LEFT;
        for(int n=0;n<1000;n++){
            check(OverhaulTextBudget.available(left,20,30)==30,"no prior vendor bounds");
            OverhaulTextBudget.within(10,100,()->{
                check(OverhaulTextBudget.available(left,20,30)==90,"outer vendor bounds");
                OverhaulTextBudget.within(0,0,()->check(OverhaulTextBudget.available(left,20,30)==30,"invalid nested bounds clear only nested scope"));
                check(OverhaulTextBudget.available(left,20,30)==90,"vendor outer restored");
                try{OverhaulTextBudget.within(0,80,()->{throw new IllegalArgumentException("foreign draw");});}catch(IllegalArgumentException expected){}
                check(OverhaulTextBudget.available(left,20,30)==90,"vendor exception restored");
            });
            check(OverhaulTextBudget.available(left,20,30)==30,"no vendor bounds retained");
        }
        for(int i=0;i<10000;i++){
            check(!TechnicalText.active(),"technical inactive");try(var a=TechnicalText.open()){check(TechnicalText.active(),"technical outer");try(var b=TechnicalText.open()){check(TechnicalText.active(),"technical nested");}check(TechnicalText.active(),"outer restored");a.close();check(!TechnicalText.active(),"manual close");}check(!TechnicalText.active(),"idempotent close");
            try(var scope=RenderContext.open()){check(RenderContext.current()!=null,"fresh context");}var f=RenderContext.class.getDeclaredField("CURRENT");f.setAccessible(true);check(((ThreadLocal<?>)f.get(null)).get()==null,"no snapshot retained between draws");
        }
        System.out.println("PASS: frame noops71 "+checks+" assertions; frozen70 Unicode policy,20000 fragmented styles/actions,foreign failures,10000 nested/idempotent scopes; no live frame-time improvement claim");
    }
}
