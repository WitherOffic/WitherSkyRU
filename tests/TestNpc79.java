import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Authored TSV expectations, independent pinned publication, current actions and channel guards. */
public final class TestNpc79 {
    private static int checks;
    private static void check(boolean v,String why){if(!v)throw new AssertionError(why);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String plain(Component input){var b=new StringBuilder();StringDecomposer.iterateFormatted(input,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component input){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(input,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static GuiMessage message(Component c,GuiMessageSource channel){return new GuiMessage(0,c,null,channel,null);}
    private static Component dispatch(Component c){return SkyRuClient.npcChat(message(c,GuiMessageSource.SYSTEM_SERVER)).content();}
    private static Style style(int seed,int part){return Style.EMPTY.withColor((seed*0x5291+part*0x2739)&0xffffff).withBold(seed%2==0).withItalic(seed%3==0).withShadowColor(seed+part).withInsertion("fresh79 "+seed+"/"+part).withClickEvent(new ClickEvent.SuggestCommand("/unchanged79 "+seed+"/"+part)).withHoverEvent(new HoverEvent.ShowText(Component.literal("hint79 "+seed+"/"+part)));}
    private record Anchor(String source,String target,int from,int to,int part){}
    private record Row(String source,String target,String legacy,List<Anchor> anchors){}
    private static int position(String text,JsonObject a,String side,int prefix){
        String term=a.get(side).getAsString();boolean whole=a.has("whole_word")&&a.get("whole_word").getAsBoolean();
        var m=Pattern.compile((whole?"(?U)(?<![\\p{L}\\p{N}_])":"")+Pattern.quote(term)+(whole?"(?![\\p{L}\\p{N}_])":"")).matcher(text);m.region(prefix,text.length());
        var found=new ArrayList<Integer>();while(m.find())found.add(m.start());
        int ordinal=a.has(side+"_occurrence")?a.get(side+"_occurrence").getAsInt():0;
        check(ordinal>0?found.size()>=ordinal:found.size()==1,"independent anchor occurrence: "+term);
        return ordinal>0?found.get(ordinal-1):found.getFirst();
    }
    private static JsonArray definitions(String value){var p=JsonParser.parseString(value);if(p.isJsonArray())return p.getAsJsonArray();var out=new JsonArray();for(var e:p.getAsJsonObject().entrySet()){var a=new JsonObject();a.addProperty("source",e.getKey());a.addProperty("target",e.getValue().getAsString());out.add(a);}return out;}
    private static void append(MutableComponent out,String text,Style style,boolean fragments){for(int at=0;at<text.length();){int end=fragments?Math.min(at+7,text.length()):text.length();out.append(Component.literal(text.substring(at,end)).setStyle(style));at=end;}}
    private static Component fixture(Row row,int seed,boolean fragments,String time,String suffix){
        int prefix=row.source.indexOf(": ")+2;var out=Component.empty();append(out,time+row.source.substring(0,prefix),style(seed,19),fragments);int at=prefix;
        for(var a:row.anchors){append(out,row.source.substring(at,a.from),style(seed,0),fragments);append(out,a.source,style(seed,a.part),fragments);at=a.from+a.source.length();}
        append(out,row.source.substring(at),style(seed,0),fragments);append(out,suffix,style(seed,20),fragments);return out;
    }
    private static Component publication(Row row,int seed,boolean fragments,String time,String suffix){
        var legacy=Component.literal(row.legacy);String text=plain(legacy);var appearance=styles(legacy);var out=Component.empty();append(out,time,style(seed,19),fragments);int prefix=row.source.indexOf(": ")+2;
        for(int at=0;at<text.length();){int end=at+1;while(end<text.length()&&appearance.get(end).equals(appearance.get(at)))end++;if(at<prefix&&end>prefix)end=prefix;
            var current=appearance.get(at).withInsertion("publication79 "+seed).withShadowColor(seed).withClickEvent(new ClickEvent.RunCommand("/retained-publication79 "+seed)).withHoverEvent(new HoverEvent.ShowText(Component.literal("publication hint "+seed)));
            append(out,text.substring(at,end),current,fragments);at=end;}
        append(out,suffix,style(seed,20),fragments);return out;
    }
    private static void numeric(Component input,Component output){var x=Pattern.compile("[0-9]+").matcher(plain(input));var y=Pattern.compile("[0-9]+").matcher(plain(output));var a=styles(input);var b=styles(output);while(x.find()){check(y.find()&&x.group().equals(y.group()),"numeric value retained");check(a.subList(x.start(),x.end()).equals(b.subList(y.start(),y.end())),"numeric appearance/actions retained");}check(!y.find(),"no invented numbers");}
    private static void semantics(Row row,Component input,Component output,String time){var a=styles(input);var b=styles(output);for(var anchor:row.anchors)for(int n=0;n<anchor.target.length();n++)check(b.get(time.length()+anchor.to+n).equals(a.get(time.length()+anchor.from)),"current semantic styles/actions: "+anchor.source+" in "+row.source);}
    private static void result(Row row,Component input,String time,String suffix){var output=dispatch(input);check(plain(output).equals(time+row.target+suffix),"dispatch "+row.source+" => "+plain(output));check(plain(input).equals(time+row.source+suffix),"input immutable");check(dispatch(output)==output,"idempotent original object");numeric(input,output);semantics(row,input,output,time);int prefix=time.length()+row.source.indexOf(": ")+2;check(styles(input).subList(0,prefix).equals(styles(output).subList(0,prefix)),"timestamp/NPC identity styles/actions");if(!suffix.isEmpty())check(styles(input).subList(plain(input).length()-suffix.length(),plain(input).length()).equals(styles(output).subList(plain(output).length()-suffix.length(),plain(output).length())),"current counter styles/actions");}
    public static void main(String[] args)throws Exception{
        var config=Path.of(args[0]);var root=Path.of(args[1]);var db=new OfflineDictionary(config);db.setRecordUnknown(false);var story=NpcDialogueTranslator.storyDialogues();
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",false);flag("storyDialogues",story);flag("npcDialogues",new NpcDialogueTranslator());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("chatNotifications",new ChatNotificationTranslator(db));
        var reference=JsonParser.parseString(Files.readString(root.resolve("NPC_SOURCES_0.15.79.json"))).getAsJsonObject();
        var evidence=new HashMap<String,JsonObject>();for(var raw:reference.getAsJsonArray("records")){var r=raw.getAsJsonObject();check(evidence.put(r.get("source").getAsString(),r)==null,"unique pinned source");}
        var blocks=new HashMap<String,JsonObject>();for(var raw:reference.getAsJsonArray("blocks")){var b=raw.getAsJsonObject();check(blocks.put(b.get("block_id").getAsString(),b)==null,"unique full source block");}
        var packaged=new HashMap<String,JsonObject>();try(var reader=new java.io.InputStreamReader(TestNpc79.class.getResourceAsStream("/data/witherskyru/npc-expansion79.json"),java.nio.charset.StandardCharsets.UTF_8)){for(var raw:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("records")){var r=raw.getAsJsonObject();check(packaged.put(r.get("source").getAsString(),r)==null,"unique packaged key");}}
        var rows=new ArrayList<Row>();var counts=new HashMap<String,Integer>();var seen=new HashSet<String>();
        for(String line:Files.readAllLines(root.resolve("NPC_0.15.79_REVIEWED.tsv"))){
            if(line.isEmpty()||line.startsWith("#"))continue;String[] f=line.split("\\|",-1);String prefix="[NPC] "+f[0]+": ",source=prefix+f[1],target=prefix+f[2];
            check(seen.add(source)&&evidence.containsKey(source),"authored source pinned independently");check(packaged.get(source).get("translation").getAsString().equals(target),"authored target verbatim in executable");
            var frozen=evidence.get(source);var published=frozen.getAsJsonObject("published_row");check(frozen.get("runtime_registration").getAsBoolean(),"literal not pending");
            String legacy=published.get("source_legacy_line").getAsString().strip();check(plain(Component.literal(legacy)).equals(source),"published layout exact source: "+source);
            for(var raw:frozen.getAsJsonArray("all_occurrences")){var occurrence=raw.getAsJsonObject();var block=blocks.get(occurrence.get("block_id").getAsString());check(block!=null&&block.has("condition_context_raw")&&block.has("template_raw"),"complete conditions and original block");check(block.get("revision").equals(occurrence.get("revision")),"revision matched");}
            var anchors=new ArrayList<Anchor>();if(f.length==4){int part=1;for(var raw:definitions(f[3])){var a=raw.getAsJsonObject();anchors.add(new Anchor(a.get("source").getAsString(),a.get("target").getAsString(),position(source,a,"source",prefix.length()),position(target,a,"target",prefix.length()),part++));}}
            anchors.sort(Comparator.comparingInt(Anchor::from));rows.add(new Row(source,target,legacy,anchors));counts.merge(f[0],1,Integer::sum);
        }
        check(rows.size()==244&&packaged.size()==244&&packaged.keySet().equals(seen),"all244 authored literals packaged");check(evidence.size()==251&&reference.getAsJsonArray("pending_variables").size()==7,"seven placeholders explicitly pending");check(counts.equals(Map.of("Wizard",115,"Wizardman",129)),"both bounded published branches");check(story.phraseCount()==10525,"1610 old plus244 new keys");
        for(var row:rows){
            result(row,Component.literal(row.source),"","");result(row,Component.literal(row.legacy),"","");
            for(int seed=1;seed<=2;seed++)for(boolean fragments:List.of(false,true))for(String time:List.of("","[01:02:03] "))for(String suffix:List.of(""," (x17)"," (×2)")){
                result(row,fixture(row,seed,fragments,time,suffix),time,suffix);
                result(row,publication(row,seed,fragments,time,suffix),time,suffix);
            }
            var input=publication(row,4,false,"","");for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var m=message(input,channel);check(SkyRuClient.npcChat(m)==m,"protected player/client channel");}
            var signed=new GuiMessage(0,input,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed message protected");flag("translateNpcChat",false);check(dispatch(input)==input,"NPC switch respected");flag("translateNpcChat",true);
        }
        for(var raw:reference.getAsJsonArray("pending_variables")){var source=raw.getAsJsonObject().get("source").getAsString();check(!packaged.containsKey(source),"placeholder not a fabricated literal translation");}
        for(int n=0;n<rows.size();n+=37){var row=rows.get(n);var own=config.resolve("custom79-"+n);Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),row.source+"\tМой перевод\n");var input=Component.literal(row.source);check(story.translate(input,new OfflineDictionary(own))==input,"full source override wins");}
        var sample=rows.stream().filter(r->!r.anchors.isEmpty()).findFirst().orElseThrow();var a=sample.anchors.getFirst();var own=config.resolve("semantic79");Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),a.source+"\tМой термин\n");var input=publication(sample,5,false,"","");check(story.translate(input,new OfflineDictionary(own))==input,"semantic override wins");
        for(String source:List.of("[NPC] Wizard: An unpublished future sentence.","[NPC] Wizardman: An unpublished future sentence.","[BOSS] Wizardman: I am Wizardman!")){var unknown=Component.literal(source);check(story.translate(unknown,db)==unknown,"unknown/wrong tag not guessed");}
        var unsupported=Component.literal("[NPC] Wizard: ").append(Component.literal("Oh").setStyle(style(7,1))).append(Component.literal("...").setStyle(style(7,2)));check(story.translate(unsupported,db)==unsupported,"unreviewed mixed semantic layout declined");
        System.out.println("PASS: NPC79 "+checks+" assertions;244 authored literals/two bounded branches;seven placeholders pending;actual legacy/modern colors,fresh actions,fragmentation,timestamps,counters,numbers,overrides,channel and identity guards;not FPS or private/live completeness");
    }
}
