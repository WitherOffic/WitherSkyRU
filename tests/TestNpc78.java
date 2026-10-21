import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Independent authored targets + frozen complete324 public branch, not private scripts. */
public final class TestNpc78 {
    private static int checks;
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);checks++;}
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    private static String plain(Component input){var out=new StringBuilder();StringDecomposer.iterateFormatted(input,Style.EMPTY,(i,s,p)->{out.appendCodePoint(p);return true;});return out.toString();}
    private static List<Style> styles(Component input){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(input,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static GuiMessage message(Component value,GuiMessageSource channel){return new GuiMessage(0,value,null,channel,null);}
    private static Component queued(Component input){return SkyRuClient.npcChat(message(input,GuiMessageSource.SYSTEM_SERVER)).content();}
    private static Style style(int seed,int part){return Style.EMPTY.withColor((0x12349*seed+0x2327*part)&0xffffff).withBold(seed%2==0).withItalic(seed%3==0).withShadowColor(seed+part).withInsertion("current78 "+seed+"/"+part).withClickEvent(new ClickEvent.SuggestCommand("/unchanged78 "+seed+"/"+part)).withHoverEvent(new HoverEvent.ShowText(Component.literal("hint78 "+seed+"/"+part)));}
    private record Anchor(String source,String target,int from,int to,int part){}
    private record Row(String source,String target,String legacy,List<Anchor> anchors){}
    private static void append(MutableComponent out,String text,Style style,boolean fragments){for(int at=0;at<text.length();){int end=fragments?Math.min(at+7,text.length()):text.length();out.append(Component.literal(text.substring(at,end)).setStyle(style));at=end;}}
    private static Component fixture(Row row,int seed,boolean fragments,String time,String suffix){int prefix=row.source.indexOf(": ")+2;var out=Component.empty();append(out,time+row.source.substring(0,prefix),style(seed,19),fragments);int at=prefix;for(var a:row.anchors){append(out,row.source.substring(at,a.from),style(seed,0),fragments);append(out,a.source,style(seed,a.part),fragments);at=a.from+a.source.length();}append(out,row.source.substring(at),style(seed,0),fragments);append(out,suffix,style(seed,20),fragments);return out;}
    private static void numbers(Component source,Component target){var a=java.util.regex.Pattern.compile("[0-9]+").matcher(plain(source));var b=java.util.regex.Pattern.compile("[0-9]+").matcher(plain(target));var x=styles(source);var y=styles(target);while(a.find()){check(b.find()&&a.group().equals(b.group()),"same numbers");check(x.subList(a.start(),a.end()).equals(y.subList(b.start(),b.end())),"numeric styles/actions");}check(!b.find(),"no invented numeric values");}
    private static void semantic(Row row,Component source,Component target,String time){var a=styles(source);var b=styles(target);for(var anchor:row.anchors)for(int n=0;n<anchor.target.length();n++)check(b.get(time.length()+anchor.to+n).equals(a.get(time.length()+anchor.from)),"current semantic appearance/action: "+row.source);}
    public static void main(String[] args)throws Exception{
        var config=Path.of(args[0]);var sourceRoot=Path.of(args[1]);var db=new OfflineDictionary(config);db.setRecordUnknown(false);var npc=NpcDialogueTranslator.storyDialogues();
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",false);flag("storyDialogues",npc);flag("npcDialogues",new NpcDialogueTranslator());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("chatNotifications",new ChatNotificationTranslator(db));
        var reference=JsonParser.parseString(Files.readString(sourceRoot.resolve("NPC_SOURCES_0.15.78.json"))).getAsJsonObject();var evidence=new HashMap<String,JsonObject>();var talSources=new HashSet<String>();
        for(var raw:reference.getAsJsonArray("records")){var row=raw.getAsJsonObject();String source=row.get("source").getAsString();check(evidence.put(source,row)==null,"unique frozen source");if(row.getAsJsonObject("published_row").get("speaker").getAsString().equals("Tal Ker"))talSources.add(source);}
        var blocks=new HashMap<String,JsonObject>();for(var raw:reference.getAsJsonArray("blocks")){var block=raw.getAsJsonObject();check(blocks.put(block.get("block_id").getAsString(),block)==null,"unique full block");}
        var packaged=new HashMap<String,JsonObject>();for(var raw:JsonParser.parseReader(new java.io.InputStreamReader(TestNpc78.class.getResourceAsStream("/data/witherskyru/npc-expansion78.json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("records")){var row=raw.getAsJsonObject();check(packaged.put(row.get("source").getAsString(),row)==null,"unique runtime key");}
        var rows=new ArrayList<Row>();var sections=new HashMap<String,Integer>();
        for(String line:Files.readAllLines(sourceRoot.resolve("NPC_0.15.78_REVIEWED.tsv"))){if(line.isEmpty()||line.startsWith("#"))continue;String[] f=line.split("\\|",-1);String prefix="[NPC] "+f[0]+": ",source=prefix+f[1],target=prefix+f[2];check(f[0].equals("Tal Ker")&&evidence.containsKey(source),"authored source in frozen publication");check(packaged.get(source).get("translation").getAsString().equals(target),"authored target packaged verbatim");var frozen=evidence.get(source);var published=frozen.getAsJsonObject("published_row");String legacy=published.get("source_legacy_line").getAsString().strip();check(plain(Component.literal(legacy)).equals(source),"complete exact legacy source");
            sections.merge(published.getAsJsonArray("headings").toString(),1,Integer::sum);
            for(var raw:frozen.getAsJsonArray("all_occurrences")){var occurrence=raw.getAsJsonObject();var block=blocks.get(occurrence.get("block_id").getAsString());check(block!=null&&block.has("condition_context_raw")&&block.has("template_raw"),"full occurrence block/conditions retained");check(block.get("revision").equals(occurrence.get("revision")),"revision matched");}
            var anchors=new ArrayList<Anchor>();if(f.length==4){int part=1;for(var entry:JsonParser.parseString(f[3]).getAsJsonObject().entrySet()){String a=entry.getKey(),b=entry.getValue().getAsString();int from=source.indexOf(a,prefix.length()),to=target.indexOf(b,prefix.length());check(from>=0&&to>=0&&source.indexOf(a,from+1)<0&&target.indexOf(b,to+1)<0,"independent semantic position");anchors.add(new Anchor(a,b,from,to,part++));}}anchors.sort(Comparator.comparingInt(Anchor::from));rows.add(new Row(source,target,legacy,anchors));
        }
        check(rows.size()==324&&packaged.size()==324&&talSources.size()==324&&packaged.keySet().equals(talSources),"all324 captured Tal Ker rows covered, not selected snippets");check(sections.size()==4,"Garden,First Time,Completed First Dialogue,Second Time");check(npc.phraseCount()==10525,"1286 retained plus324 new source keys");
        for(var row:rows){
            var legacy=Component.literal(row.legacy);var translated=queued(legacy);check(plain(translated).equals(row.target),"published layout: "+row.source+" => "+plain(translated));semantic(row,legacy,translated,"");numbers(legacy,translated);
            for(int seed=1;seed<=2;seed++)for(boolean fragments:List.of(false,true))for(String time:List.of("","[01:02:03] "))for(String suffix:List.of(""," (x17)"," (×2)")){
                var input=fixture(row,seed,fragments,time,suffix);var output=queued(input);check(plain(output).equals(time+row.target+suffix),"authored full dispatch: "+row.source);check(plain(input).equals(time+row.source+suffix),"source immutable");check(queued(output)==output,"idempotent original object");numbers(input,output);semantic(row,input,output,time);int prefix=time.length()+row.source.indexOf(": ")+2;check(styles(input).subList(0,prefix).equals(styles(output).subList(0,prefix)),"identity and timestamp appearance/actions retained");
                if(!suffix.isEmpty())check(styles(input).subList(plain(input).length()-suffix.length(),plain(input).length()).equals(styles(output).subList(plain(output).length()-suffix.length(),plain(output).length())),"counter appearance");
                for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var message=message(input,channel);check(SkyRuClient.npcChat(message)==message,"protected channel");}
            }
            var input=fixture(row,3,false,"","");var signed=new GuiMessage(0,input,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed source untouched");flag("translateNpcChat",false);check(queued(input)==input,"NPC switch respected");flag("translateNpcChat",true);
        }
        for(int n=0;n<rows.size();n+=31){var row=rows.get(n);var own=config.resolve("override78-"+n);Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),row.source+"\tМой текст\n");var custom=new OfflineDictionary(own);custom.setRecordUnknown(false);var input=Component.literal(row.source);check(npc.translate(input,custom)==input,"full user override wins");}
        for(String source:List.of("[NPC] Tal Ker: An unpublished future line.","[NPC] Tal Ker: Hello <player>!","[BOSS] Tal Ker: Hey!","[NPC] Tal Ker: I'm the guy that never stops talking UNKNOWN")){var input=Component.literal(source);check(npc.translate(input,db)==input,"unknowns/placeholders/wrong tags not guessed");}
        var guarded=Component.literal("[NPC] Tal Ker: ").append(Component.literal("I'm the guy that ").setStyle(style(4,1))).append(Component.literal("never stops talking").setStyle(style(4,2)));check(npc.translate(guarded,db)==guarded,"unreviewed mixed semantic style declined");
        System.out.println("PASS: NPC78 "+checks+" assertions;all324 frozen Tal Ker rows/four sections,exact targets,source conditions,current colors/actions,numbers,counters,timestamps,fragmentation,overrides,protected channels;not private/live completeness or FPS");
    }
}
