import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;
import java.util.regex.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;

/** Differential lookup/render oracle from the delivered60 JAR, not new code expectations. */
public final class TestNpcLookup61 {
    private static int checks;
    private static void check(boolean ok,String note){if(!ok)throw new AssertionError(note);checks++;}
    private static final class Previous extends URLClassLoader {
        Previous(URL[] urls){super(urls,TestNpcLookup61.class.getClassLoader());}
        protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
            if(!name.startsWith("ru.wither.skyru."))return super.loadClass(name,resolve);
            synchronized(getClassLoadingLock(name)){var c=findLoadedClass(name);if(c==null)c=findClass(name);if(resolve)resolveClass(c);return c;}
        }
        public URL getResource(String name){if(name.startsWith("data/witherskyru/")){var url=findResource(name);if(url!=null)return url;}return super.getResource(name);}
    }
    private static String plain(Component c){return AuditMiningStyles.plain(c.getVisualOrderText());}
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();c.getVisualOrderText().accept((i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static void compare(NpcDialogueTranslator current,Object previous,Method oldPlain,Method oldDb,Object oldDictionary,OfflineDictionary db,Component input)throws Exception{
        var old=(Component)oldPlain.invoke(previous,input);var now=current.translate(input);
        check(plain(old).equals(plain(now)),"plain oracle mismatch: "+plain(input));check(styles(old).equals(styles(now)),"plain styles/actions oracle");check((old==input)==(now==input),"same identity/decline decision");
        old=(Component)oldDb.invoke(previous,input,oldDictionary);now=current.translate(input,db);
        check(plain(old).equals(plain(now)),"dictionary oracle mismatch: "+plain(input));check(styles(old).equals(styles(now)),"dictionary styles/actions oracle");check((old==input)==(now==input),"same dictionary decline decision");
    }
    public static void main(String[] args)throws Exception{
        // Explicit105 editorial supersession: delivered60 cannot be the wording
        // oracle for these eleven rows. TestNpcWording105 independently checks
        // their authored primary/published layouts and current actions/styles.
        var revisedTargets=new HashMap<String,String>();
        var revisions=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("NPC_WORDING_SOURCES_0.15.105.json"))).getAsJsonObject().getAsJsonArray("records");
        for(var raw:revisions){var row=raw.getAsJsonObject();check(revisedTargets.put(row.get("source").getAsString(),row.get("translation").getAsString())==null,"unique explicit105 wording revision");}
        check(revisedTargets.size()==11,"exact eleven-row editorial exception,not a blanket oracle bypass");
        var project=Path.of(args[1]).toAbsolutePath().getParent();var oldJar=project.resolve("build/libs/WitherSkyRU-26.1.2-fabric-0.15.60-dev.jar");check(Files.isRegularFile(oldJar),"preserved delivered60 JAR exists");
        check(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(oldJar))).equalsIgnoreCase("3A5645142C8C9CBFBDF16E781B1D8D44B9EAE11FF356C93BEF7D80E7AB0AE748"),"golden delivered60 hash unchanged");
        var cp=Path.of(TestNpcLookup61.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        try(var oldLoader=new Previous(new URL[]{oldJar.toUri().toURL(),cp.toUri().toURL()})){
            var type=oldLoader.loadClass("ru.wither.skyru.NpcDialogueTranslator");var dictType=oldLoader.loadClass("ru.wither.skyru.OfflineDictionary");var oldPlain=type.getMethod("translate",Component.class);var oldDb=type.getMethod("translate",Component.class,dictType);
            var root=Path.of(args[0]);var db=new OfflineDictionary(root.resolve("new"));db.setRecordUnknown(false);var oldDictionary=dictType.getConstructor(Path.class).newInstance(root.resolve("old"));dictType.getMethod("setRecordUnknown",boolean.class).invoke(oldDictionary,false);
            String[][] providers={{"","npc-dialogues"},{"gardenDialogues","garden-dialogues"},{"questDialogues","quest-dialogues"},{"storyDialogues","story-dialogues"},{"winterDialogues","winter-dialogues"},{"questObjectives","quest-objectives"},{"dungeonDialogues","dungeon-dialogues"},{"dungeonNotifications","dungeon-notifications"}};
            var base=Style.EMPTY.withColor(0xaaaaaa).withInsertion("current caller").withClickEvent(new ClickEvent.RunCommand("/current-original")).withHoverEvent(new HoverEvent.ShowText(Component.literal("current hover")));
            for(var provider:providers){
                var current=provider[0].isEmpty()?new NpcDialogueTranslator():(NpcDialogueTranslator)NpcDialogueTranslator.class.getMethod(provider[0]).invoke(null);
                var previous=provider[0].isEmpty()?type.getConstructor().newInstance():type.getMethod(provider[0]).invoke(null);
                var records=JsonParser.parseReader(new java.io.InputStreamReader(TestNpcLookup61.class.getResourceAsStream("/data/witherskyru/"+provider[1]+".json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("records");
                for(var raw:records){String source=raw.getAsJsonObject().get("source").getAsString();
                    if(revisedTargets.containsKey(source)){
                        check(plain(current.translate(Component.literal(source).setStyle(base))).equals(revisedTargets.get(source)),"explicit105 revised target replaces old60 wording");
                        continue;
                    }
                    // Two intentional66 editorial changes have an independent screenshot test.
                    // They cannot use delivered60's old/untranslated text as the expected output.
                    if(provider[1].equals("story-dialogues")&&(source.equals("[NPC] Charlotte: It's Charlotte, not Salad!!")||source.equals("[NPC] Brigette: Thanks, Salad!")))continue;
                    // NPC70 intentionally translates the old literal BRONZE result.
                    // TestNpc70 plus the retained15 observed pass10 samples check its new target/styles.
                    if(provider[1].equals("npc-dialogues")&&source.equals("[NPC] Jacob: You placed in the BRONZE bracket in the Carrot Contest!"))continue;
                    for(String prefix:List.of("","[04:05:06] ","[99:99:99] ","[０4:05:06] ","[04:5:06] ","  ","\u2003"))for(String suffix:List.of(""," (x17)"," (×2)"," UNKNOWN")){
                        compare(current,previous,oldPlain,oldDb,oldDictionary,db,Component.literal(prefix+source+suffix).setStyle(base));
                        compare(current,previous,oldPlain,oldDb,oldDictionary,db,Component.literal("§e"+prefix+"§f"+source+suffix).setStyle(base));
                    }
                    compare(current,previous,oldPlain,oldDb,oldDictionary,db,Component.translatable("unknown.lookup61",source));
                }
                for(int i=0;i<100;i++)compare(current,previous,oldPlain,oldDb,oldDictionary,db,Component.literal("[NPC] Unknown: Unvisited "+i).setStyle(base));
            }
            var timestamp=NpcDialogueTranslator.class.getDeclaredMethod("timestampLength",String.class);timestamp.setAccessible(true);var regex=Pattern.compile("^\\[\\d{2}:\\d{2}:\\d{2}\\] ");
            var random=new Random(61);for(int i=0;i<10000;i++){String s=i%2==0?String.format(Locale.ROOT,"[%02d:%02d:%02d] body",random.nextInt(100),random.nextInt(100),random.nextInt(100)):"[04:05:06] body";if(i%3==0){int at=random.nextInt(11);s=s.substring(0,at)+(char)(random.nextInt(120)+20)+s.substring(at+1);}var match=regex.matcher(s);check((int)timestamp.invoke(null,s)==(match.find()?match.end():0),"timestamp regex equivalence");}
            // Literal counter records must still honour an override of their base message.
            String fixture="npc-lookup61-"+UUID.randomUUID()+".json",source="[NPC] Test: Known (x2)",target="[NPC] Test: Известно (x2)";
            Files.writeString(cp.resolve(fixture),"{\"records\":[{\"speaker\":\"Test\",\"source\":\""+source+"\",\"translation\":\""+target+"\",\"preserve_suffix\":\" (x2)\"}]}");
            var constructor=NpcDialogueTranslator.class.getDeclaredConstructor(String.class,Set.class,boolean.class,boolean.class);constructor.setAccessible(true);var oldConstructor=type.getDeclaredConstructor(String.class,Set.class,boolean.class,boolean.class);oldConstructor.setAccessible(true);
            var current=(NpcDialogueTranslator)constructor.newInstance("/"+fixture,Set.of("NPC"),true,false);var previous=oldConstructor.newInstance("/"+fixture,Set.of("NPC"),true,false);
            var custom=root.resolve("override");Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),"[NPC] Test: Known\tМой текст\n");db=new OfflineDictionary(custom);oldDictionary=dictType.getConstructor(Path.class).newInstance(custom);
            compare(current,previous,oldPlain,oldDb,oldDictionary,db,Component.literal(source).setStyle(base));check(current.translate(Component.literal(source),db).getString().equals(source),"literal counter base override veto");
        }
        System.out.println("PASS: NPC lookup61 "+checks+" assertions;all8 providers vs delivered60 JAR,known/unknown/legacy/time/padding/counter/translatable/style/action/identity decisions,10000 timestamp probes,base override;no live FPS claim");
    }
}
