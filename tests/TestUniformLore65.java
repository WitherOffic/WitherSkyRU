import java.nio.file.*;
import java.net.*;
import java.lang.reflect.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** Byte-pinned unoptimized65 oracle: same translations, current glyph styles/actions. */
public final class TestUniformLore65 {
    private static int checks;
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
    private static final class Previous extends URLClassLoader {
        Previous(URL url){super(new URL[]{url},TestUniformLore65.class.getClassLoader());}
        protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException{if(!n.startsWith("ru.wither.skyru."))return super.loadClass(n,resolve);synchronized(getClassLoadingLock(n)){var c=findLoadedClass(n);if(c==null)c=findClass(n);if(resolve)resolveClass(c);return c;}}
        public URL getResource(String n){if(n.startsWith("data/witherskyru/")){var r=findResource(n);if(r!=null)return r;}return super.getResource(n);}
    }
    private static void flag(Class<?> c,String n,Object v)throws Exception{var f=c.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String text(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,style,cp)->{b.appendCodePoint(cp);return true;});return b.toString();}
    private static List<Style> styles(FormattedCharSequence s){var out=new ArrayList<Style>();s.accept((i,style,cp)->{out.add(style);return true;});return out;}
    private static List<Component> rows(List<String> text,int mode,int seed){var out=new ArrayList<Component>();for(int r=0;r<text.size();r++){var style=Style.EMPTY.withColor(0x100000+seed*91+r).withInsertion("fresh uniform65 "+seed+"/"+r).withClickEvent(new ClickEvent.RunCommand("/fresh65 "+seed+" "+r)).withHoverEvent(new HoverEvent.ShowText(Component.literal("fresh "+seed+"/"+r)));String s=text.get(r);if(mode==0){out.add(Component.literal(s).setStyle(style));continue;}var c=Component.empty();for(int at=0;at<s.length();){int end=at+Character.charCount(s.codePointAt(at));var current=style;if(mode==2)current=current.withColor(0x300000+at).withInsertion("glyph "+seed+"/"+at);c.append(Component.literal(s.substring(at,end)).setStyle(current));at=end;}out.add(c);}return out;}
    private static void compare(List<String> source,Method comp,Method seq,int seed)throws Exception{
        for(int mode=0;mode<3;mode++)for(boolean shift:List.of(false,true)){
            var input=rows(source,mode,seed);var sequences=input.stream().map(Component::getVisualOrderText).toList();var before=(List<Component>)comp.invoke(null,input,shift);var after=TooltipTranslation.components(input,shift);var oldSeq=(List<FormattedCharSequence>)seq.invoke(null,sequences,shift);var newSeq=TooltipTranslation.styled(sequences,shift);check(before.size()==after.size()&&oldSeq.size()==newSeq.size(),"same row counts");
            for(int r=0;r<after.size();r++){check(before.get(r).getString().equals(after.get(r).getString()),"same component text");check(styles(before.get(r).getVisualOrderText()).equals(styles(after.get(r).getVisualOrderText())),"same current component colors/fonts/actions");check(text(oldSeq.get(r)).equals(text(newSeq.get(r))),"same sequence text");check(styles(oldSeq.get(r)).equals(styles(newSeq.get(r))),"same current sequence colors/fonts/actions");if(!text(newSeq.get(r)).isEmpty())check(oldSeq.get(r).accept((i,s,cp)->false)==newSeq.get(r).accept((i,s,cp)->false),"early termination oracle row "+r+" mode "+mode+" shift "+shift+" source "+source);}
        }
    }
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var project=Path.of(args[1]).toAbsolutePath().getParent();var jar=project.resolve("build/prototypes/WitherSkyRU-0.15.65-pre-uniform-fastpath-65A1A374.jar");check(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))).equalsIgnoreCase("65A1A3743C168F48EBE74721EB14C8F048A97EEEB303430A8789005BB3AE87BC"),"pinned same-text oracle");
        var db=new OfflineDictionary(root.resolve("new"));db.setRecordUnknown(false);flag(SkyRuClient.class,"dictionary",db);flag(SkyRuClient.class,"enabled",true);flag(SkyRuClient.class,"onlyHypixel",false);
        try(var old=new Previous(jar.toUri().toURL())){
            var client=old.loadClass("ru.wither.skyru.SkyRuClient");var dict=old.loadClass("ru.wither.skyru.OfflineDictionary");var oldDb=dict.getConstructor(Path.class).newInstance(root.resolve("old"));dict.getMethod("setRecordUnknown",boolean.class).invoke(oldDb,false);flag(client,"dictionary",oldDb);flag(client,"enabled",true);flag(client,"onlyHypixel",false);var toolkit=old.loadClass("ru.wither.skyru.TooltipTranslation");var comp=toolkit.getMethod("components",List.class,boolean.class);var seq=toolkit.getMethod("styled",List.class,boolean.class);
            var type=Class.forName("ru.wither.skyru.ReviewedLoreStyles");var f=type.getDeclaredField("GROUPS");f.setAccessible(true);var allGroups=(List<List<?>>)f.get(null);check(allGroups.size()>=650,"retained650 historical groups; new68 groups independently checked");var groups=allGroups.subList(0,650);var oldType=old.loadClass("ru.wither.skyru.ReviewedLoreStyles");var oldGroupsField=oldType.getDeclaredField("GROUPS");oldGroupsField.setAccessible(true);var oldGroups=(List<List<?>>)oldGroupsField.get(null);check(oldGroups.size()==650,"pinned old registry650");for(int g=0;g<650;g++){check(groups.get(g).size()==oldGroups.get(g).size(),"retained group rows");for(int r=0;r<groups.get(g).size();r++){var a=groups.get(g).get(r).getClass().getDeclaredMethod("text");a.setAccessible(true);var b=oldGroups.get(g).get(r).getClass().getDeclaredMethod("text");b.setAccessible(true);check(a.invoke(groups.get(g).get(r)).equals(b.invoke(oldGroups.get(g).get(r))),"retained old registry prefix");}}int seed=0;
            for(var group:groups){var text=new ArrayList<String>();text.add("Unknown title");for(var row:group){var m=row.getClass().getDeclaredMethod("text");m.setAccessible(true);text.add(((String)m.invoke(row)).replace("{0}","12.5").replace("{1}","13"));}text.add("Unknown neighbour");if(Set.of("While in The Catacombs, consume 1⸕","Place an explosive trap that","Allows you to teleport to any teammate! Grants").contains(text.get(1)))continue;for(int repeat=0;repeat<3;repeat++)compare(text,comp,seq,seed++);var broken=new ArrayList<>(text);broken.set(1,broken.get(1)+" UNREVIEWED");compare(broken,comp,seq,seed++);}
            for(String key:List.of("Grants ","Turbo-Cacti I","Cactus Fortune","Requires Bronze in a Cactus Contest!")){var dir=root.resolve("override-"+seed);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой текст\n");var current=new OfflineDictionary(dir);current.setRecordUnknown(false);var previous=dict.getConstructor(Path.class).newInstance(dir);dict.getMethod("setRecordUnknown",boolean.class).invoke(previous,false);flag(SkyRuClient.class,"dictionary",current);flag(client,"dictionary",previous);compare(List.of("Enchanted Book","Turbo-Cacti I","Grants +5☘ Cactus Fortune.","Requires Bronze in a Cactus Contest!"),comp,seq,seed++);}
        }
        System.out.println("PASS: uniform lore65 "+checks+" assertions;650 historical source groups retained;647 unchanged groups vs pinned pre-fastpath65;three intentional Dungeon68 color-boundary fixes independently covered by TestDungeons68;uniform/fragmented/mixed/Shift/overrides/fresh styles;not FPS");
    }
}
