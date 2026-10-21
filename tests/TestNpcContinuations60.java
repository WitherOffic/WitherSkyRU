import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/**45 actual user-log lines; independent TSV expectations, source glyph actions and explicit repeats. */
public final class TestNpcContinuations60{
    private static int checks;
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String plain(Component c){return AuditMiningStyles.plain(c.getVisualOrderText());}
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();c.getVisualOrderText().accept((i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static final Pattern NUM=Pattern.compile("[0-9]+(?:[.,][0-9]+)?"),TIME=Pattern.compile("^\\[\\d{2}:\\d{2}:\\d{2}\\] ");
    private static List<Style> digits(Component c){var out=new ArrayList<Style>();var all=styles(c);var m=NUM.matcher(plain(c));while(m.find())out.addAll(all.subList(m.start(),m.end()));return out;}
    private static int pos(String text,JsonObject a,String side,int begin,int end){String word=a.get(side).getAsString();boolean whole=a.has("whole_word")&&a.get("whole_word").getAsBoolean();String pattern=(whole?"(?<![\\p{L}\\p{N}_])":"")+Pattern.quote(word)+(whole?"(?![\\p{L}\\p{N}_])":"");var m=Pattern.compile(pattern).matcher(text);var found=new ArrayList<Integer>();while(m.find())if(m.start()>=begin&&m.end()<=end)found.add(m.start());int nth=a.has(side+"_occurrence")?a.get(side+"_occurrence").getAsInt():0;check(nth==0?found.size()==1:nth>0&&nth<=found.size(),"independent bounded occurrence");return found.get(nth==0?0:nth-1);}
    private static GuiMessage msg(Component c,GuiMessageSource ch){return new GuiMessage(0,c,null,ch,null);}
    private static Component queued(Component c){return SkyRuClient.npcChat(msg(c,GuiMessageSource.SYSTEM_SERVER)).content();}
    private static Component input(String raw,int mode,int seed,JsonObject row){
        var base=Style.EMPTY.withInsertion("frame "+seed).withClickEvent(new ClickEvent.RunCommand("/original-npc60-"+seed)).withHoverEvent(new HoverEvent.ShowText(Component.literal("current original hint "+seed)));
        var legacy=Component.literal(raw).setStyle(base);if(mode==0)return legacy;String text=plain(legacy);if(mode==2)return Component.literal(text).setStyle(base);var ss=styles(legacy);
        if(mode==3)for(int i=0;i<text.length();i++)if(Character.isDigit(text.charAt(i)))ss.set(i,ss.get(i).withColor(0x110000+i*0x101).withInsertion("digit "+seed+"/"+i).withClickEvent(new ClickEvent.RunCommand("/digit-"+seed+"-"+i)));
        if(mode==4&&row.has("style_anchors")){String source=row.get("source").getAsString();int offset=text.indexOf(source);int n=0;for(var a:row.getAsJsonArray("style_anchors")){var anchor=a.getAsJsonObject();int at=offset+pos(source,anchor,"source",source.indexOf(": ")+2,source.length());for(int i=at;i<at+anchor.get("source").getAsString().length();i++)ss.set(i,ss.get(i).withColor(0x120000+n*0x101).withInsertion("anchor "+seed+"/"+n).withClickEvent(new ClickEvent.RunCommand("/anchor-"+seed+"-"+n)));n++;}}
        var out=Component.empty();for(int i=0;i<text.length();){int end=i+1;while(end<text.length()&&end<i+3&&ss.get(end).equals(ss.get(i)))end++;out.append(Component.literal(text.substring(i,end)).setStyle(ss.get(i)));i=end;}return out;
    }
    private static void invalidMetadata(Path root)throws Exception{
        var type=NpcDialogueTranslator.class;var occurrence=type.getDeclaredMethod("occurrence",JsonObject.class,String.class);occurrence.setAccessible(true);
        for(String value:List.of("0","-1","17","1.5","\"1\"","null","true")){var anchor=JsonParser.parseString("{\"source_occurrence\":"+value+"}").getAsJsonObject();try{occurrence.invoke(null,anchor,"source_occurrence");throw new AssertionError("invalid occurrence accepted");}catch(InvocationTargetException expected){check(expected.getCause() instanceof java.io.IOException,"invalid occurrence rejected");}}
        var cp=Path.of(TestNpcContinuations60.class.getProtectionDomain().getCodeSource().getLocation().toURI());String folder="npc60-invalid-"+UUID.randomUUID();var fixtures=cp.resolve(folder);Files.createDirectories(fixtures);
        var ctor=type.getDeclaredConstructor(String.class,Set.class,boolean.class,boolean.class);ctor.setAccessible(true);
        String[] bad={"{\"source\":\"animals\",\"target\":\"животные\"}","{\"source\":\"animals\",\"target\":\"животные\",\"source_occurrence\":3}","{\"source\":\"animals\",\"target\":\"животные\",\"source_occurrence\":1},{\"source\":\"animals\",\"target\":\"животными\",\"source_occurrence\":1}"};
        for(int i=0;i<bad.length;i++){String json="{\"records\":[{\"speaker\":\"Test\",\"source\":\"[NPC] Test: animals and animals\",\"translation\":\"[NPC] Test: животные с животными\",\"style_anchors\":["+bad[i]+"]}]}";Files.writeString(fixtures.resolve(i+".json"),json);try{ctor.newInstance("/"+folder+"/"+i+".json",Set.of("NPC"),true,false);throw new AssertionError("ambiguous/overlapping source accepted");}catch(InvocationTargetException expected){check(expected.getCause() instanceof java.io.IOException,"bad anchor ranges rejected");}}
    }
    public static void main(String[] args)throws Exception{
        var dir=Path.of(args[1]);var expected=new LinkedHashMap<String,String>();for(String line:Files.readAllLines(dir.resolve("NPC_CONTINUATIONS_REVIEWED_0.15.60.tsv")))if(!line.isBlank()&&!line.startsWith("#")){String[] pair=line.split("\t",2);check(pair.length==2&&expected.putIfAbsent(pair[0],pair[1])==null,"unique independent expectation");}
        var capture=JsonParser.parseString(Files.readString(dir.resolve("NPC_GAPS_OBSERVED_0.15.60.json"))).getAsJsonObject();var samples=new HashMap<String,JsonObject>();for(var r:capture.getAsJsonArray("records")){var v=r.getAsJsonObject();samples.put(v.get("source").getAsString(),v);}
        var runtime=JsonParser.parseReader(new java.io.InputStreamReader(TestNpcContinuations60.class.getResourceAsStream("/data/witherskyru/story-dialogues.json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();var rows=new HashMap<String,JsonObject>();for(var r:runtime.getAsJsonArray("records")){var v=r.getAsJsonObject();if(v.get("source_key").getAsString().startsWith("npc.observed60."))rows.put(v.get("source").getAsString(),v);}
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);var story=NpcDialogueTranslator.storyDialogues();flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);flag("storyDialogues",story);flag("npcDialogues",new NpcDialogueTranslator());flag("chatNotifications",new ChatNotificationTranslator(db));
        for(var pair:expected.entrySet()){
            String source=pair.getKey(),target=pair.getValue();var row=rows.get(source);check(row!=null&&row.get("translation").getAsString().equals(target),"authored expectation vs runtime");var observation=samples.get(source);check(observation!=null,"actual complete source observation");String raw=observation.get("sample").getAsString();var time=TIME.matcher(raw.replaceAll("(?i)§[0-9a-fk-or]",""));String prefix=time.find()?time.group():"";int nameEnd=source.indexOf(": ")+2;
            for(int mode=0;mode<5;mode++)for(int seed=0;seed<5;seed++)for(String suffix:List.of(""," (x17)"," (×2)")){
                var original=input(raw+suffix,mode,seed,row);var current=original;
                for(int repeat=0;repeat<5;repeat++){
                    current=queued(current);check(plain(current).equals(prefix+target+suffix),"NPC60 translated "+row.get("source_key")+" mode"+mode+" => "+plain(current));check(digits(original).equals(digits(current)),"every number/time/counter glyph copied");check(plain(original).equals(prefix+source+suffix),"original immutable");
                    var a=styles(original);var b=styles(current);check(a.subList(0,prefix.length()+nameEnd).equals(b.subList(0,prefix.length()+nameEnd)),"timestamp/speaker exact glyphs");if(!suffix.isEmpty())check(a.subList(a.size()-suffix.length(),a.size()).equals(b.subList(b.size()-suffix.length(),b.size())),"counter glyphs unchanged");
                    if(row.has("style_anchors"))for(var anchorRaw:row.getAsJsonArray("style_anchors")){var anchor=anchorRaw.getAsJsonObject();String s=anchor.get("source").getAsString(),t=anchor.get("target").getAsString();int from=prefix.length()+pos(source,anchor,"source",nameEnd,source.length()),to=prefix.length()+pos(target,anchor,"target",nameEnd,target.length());if(s.equals(t))check(a.subList(from,from+s.length()).equals(b.subList(to,to+t.length())),"identity occurrence/current styles");else for(int n=to;n<to+t.length();n++)check(b.get(n).equals(a.get(from)),"changed occurrence semantic action/color");}
                }
                for(var ch:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var m=msg(original,ch);check(SkyRuClient.npcChat(m)==m,"whole player/client original");}var signed=new GuiMessage(0,original,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed original");
            }
            var original=input(raw,1,9,row);var unknown=Component.literal(plain(original)+" UNVISITED");check(queued(unknown)==unknown,"unknown branches not guessed");flag("translateNpcChat",false);check(queued(original)==original,"NPC toggle");flag("translateNpcChat",true);flag("translateGameChat",false);check(plain(queued(original)).equals(prefix+target),"NPC independent of game notices");flag("translateGameChat",true);
            var custom=root.resolve("override-"+rows.get(source).get("source_key").getAsString());Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),source+"\tМой текст\n");var own=new OfflineDictionary(custom);check(story.translate(original,own)==original,"full override");if(row.has("style_anchors"))for(var anchor:row.getAsJsonArray("style_anchors")){Files.writeString(custom.resolve("overrides.tsv"),anchor.getAsJsonObject().get("source").getAsString()+"\tМой термин\n");check(story.translate(original,new OfflineDictionary(custom))==original,"semantic override");}
        }
        check(expected.size()==45&&rows.size()==45,"45 independent observed NPC lines");invalidMetadata(root);flag("enabled",false);var off=Component.literal(expected.keySet().iterator().next());check(queued(off)==off,"disabled");
        System.out.println("PASS: NPC continuations60 "+checks+" assertions;45 observed lines/5 speakers,5 layouts/5 fresh actions/3 counters/5 repeats,explicit repeated semantics/number glyphs/independent TSV/chat/overrides/invalid metadata;not all unvisited branches/live channels");
    }
}
