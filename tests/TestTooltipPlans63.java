import java.nio.file.*;
import java.net.*;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;
import ru.wither.skyru.*;

/** Compare tooltip text, effective styles/actions, identity and rollback with delivered62. */
public final class TestTooltipPlans63 {
    private static int checks;
    private static void check(boolean ok,String note){if(!ok)throw new AssertionError(note);checks++;}
    private static final class Previous extends URLClassLoader {
        Previous(URL url){super(new URL[]{url},TestTooltipPlans63.class.getClassLoader());}
        protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException{
            if(!n.startsWith("ru.wither.skyru."))return super.loadClass(n,resolve);
            synchronized(getClassLoadingLock(n)){var c=findLoadedClass(n);if(c==null)c=findClass(n);if(resolve)resolveClass(c);return c;}
        }
        public URL getResource(String n){if(n.startsWith("data/witherskyru/")){var r=findResource(n);if(r!=null)return r;}return super.getResource(n);}
    }
    private static void flag(Class<?> c,String name,Object value)throws Exception{var f=c.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static List<Style> styles(FormattedCharSequence seq){var out=new ArrayList<Style>();seq.accept((i,s,p)->{out.add(s);return true;});return out;}
    private static List<Component> rows(List<String> text,int seed,boolean fragmented){
        var out=new ArrayList<Component>();
        for(int row=0;row<text.size();row++){
            var style=Style.EMPTY.withColor(0x200000+seed*99+row).withInsertion("fresh:"+seed+":"+row)
                .withClickEvent(new ClickEvent.RunCommand("/tooltip63 "+seed+" "+row))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("hint "+seed+"/"+row)));
            String s=text.get(row);if(!fragmented){out.add(Component.literal(s).setStyle(style));continue;}
            var c=Component.empty();for(int at=0;at<s.length();){int end=at+Character.charCount(s.codePointAt(at));c.append(Component.literal(s.substring(at,end)).setStyle(style.withColor(0x300000+at)));at=end;}out.add(c);
        }return out;
    }
    private static void compare(List<String> text,Method oldComp,Method oldSeq,int seed)throws Exception{
        for(boolean fragmented:List.of(false,true))for(boolean title:List.of(false,true)){
            var input=rows(text,seed,fragmented);var seq=input.stream().map(Component::getVisualOrderText).toList();
            for(int repeat=0;repeat<2;repeat++){
                var before=(List<Component>)oldComp.invoke(null,input,title);var after=TooltipTranslation.components(input,title);
                check(before.size()==after.size(),"component rows");check((before==input)==(after==input),"original component-list decision");
                var a=(List<FormattedCharSequence>)oldSeq.invoke(null,seq,title);var b=TooltipTranslation.styled(seq,title);
                check(a.size()==b.size(),"sequence rows");check((a==seq)==(b==seq),"original sequence-list decision");
                for(int i=0;i<after.size();i++){
                    check(before.get(i).getString().equals(after.get(i).getString()),"component text: "+text);
                    check(styles(before.get(i).getVisualOrderText()).equals(styles(after.get(i).getVisualOrderText())),"fresh component actions/colors");
                    check((before.get(i)==input.get(i))==(after.get(i)==input.get(i)),"original row decision");
                    check(AuditMiningStyles.plain(a.get(i)).equals(AuditMiningStyles.plain(b.get(i))),"sequence text");
                    check(styles(a.get(i)).equals(styles(b.get(i))),"fresh sequence actions/colors");
                }
            }
        }
    }
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var project=Path.of(args[1]).toAbsolutePath().getParent();
        var jar=project.resolve("build/libs/WitherSkyRU-26.1.2-fabric-0.15.62-dev.jar");
        check(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))).equalsIgnoreCase("07D7632DD8C5772E1E54BCEF0A8E9B882BA87F30E1AB7569686224CDEFBAC4C3"),"delivered62 oracle hash");
        var db=new OfflineDictionary(root.resolve("new"));db.setRecordUnknown(false);
        flag(SkyRuClient.class,"dictionary",db);flag(SkyRuClient.class,"enabled",true);flag(SkyRuClient.class,"onlyHypixel",false);
        try(var old=new Previous(jar.toUri().toURL())){
            var client=old.loadClass("ru.wither.skyru.SkyRuClient");var dict=old.loadClass("ru.wither.skyru.OfflineDictionary");
            var oldDb=dict.getConstructor(Path.class).newInstance(root.resolve("old"));dict.getMethod("setRecordUnknown",boolean.class).invoke(oldDb,false);
            flag(client,"dictionary",oldDb);flag(client,"enabled",true);flag(client,"onlyHypixel",false);
            var toolkit=old.loadClass("ru.wither.skyru.TooltipTranslation");var comp=toolkit.getMethod("components",List.class,boolean.class);var seq=toolkit.getMethod("styled",List.class,boolean.class);
            var lore=Class.forName("ru.wither.skyru.ReviewedLoreStyles");var groups=lore.getDeclaredField("GROUPS");groups.setAccessible(true);int seed=0;
            for(var group:(List<List<?>>)groups.get(null)){
                var text=new ArrayList<String>();text.add("Unknown tooltip title");for(var row:group){var m=row.getClass().getDeclaredMethod("text");m.setAccessible(true);text.add(((String)m.invoke(row)).replace("{0}","12.5").replace("{1}","13"));}text.add("Unknown neighbour");
                check(db.contextEdits(text).equals(EditorialContext.edits(text)),"editorial oracle");
                compare(text,comp,seq,seed++);compare(text,comp,seq,seed++);
                var wrong=new ArrayList<>(text);wrong.set(1,wrong.get(1)+" UNVISITED");compare(wrong,comp,seq,seed++);
            }
            for(var text:List.of(List.of("Items are sold for 50% of their","selling price."),List.of("The longer you hold this in your","inventory, the stronger you'll","become for 2 minutes after removing","it from your inventory! Max +50.","","Time Held: 12 Minutes"),List.of("Unknown","Unknown")))compare(text,comp,seq,seed++);
        }
        try{OfflineDictionary.class.getDeclaredField("contextCache");throw new AssertionError("Rejected cache shipped");}catch(NoSuchFieldException expected){check(true,"no new retained context cache");}
        var mutable=new ArrayList<>(List.of("Items are sold for 50% of their","selling price."));var saved=List.copyOf(mutable);var originalEdits=db.contextEdits(mutable);mutable.set(0,"Changed title");
        check(originalEdits.equals(EditorialContext.edits(saved)),"input mutation does not change existing edits");check(db.contextEdits(mutable).equals(EditorialContext.edits(mutable)),"mutation never reuses stale edits");
        for(int i=0;i<1000;i++){var source=List.of("Dynamic "+i,"selling price.");check(db.contextEdits(source).equals(EditorialContext.edits(source)),"dynamic context oracle");}
        for(var oversized:List.of(Collections.nCopies(33,"row"),List.of("x".repeat(401)),Collections.nCopies(11,"x".repeat(399))))check(db.contextEdits(oversized).equals(EditorialContext.edits(oversized)),"oversized full path");
        var custom=root.resolve("custom");Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),saved.getFirst()+"\tМоя строка\n");var overridden=new OfflineDictionary(custom);
        check(overridden.contextEdits(saved).isEmpty(),"override veto");
        System.out.println("PASS: tooltip plans63 "+checks+" assertions; delivered62 oracle,285 groups/fresh styles/actions/fragmented input/original-title/rollback,mutations/churn/oversize/overrides,rejected context cache absent;not FPS");
    }
}
