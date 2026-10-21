import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Independently authored targets + pinned publication layouts, not a runtime self-oracle. */
public final class TestNpc89 {
    private static int checks;
    private static void check(boolean v,String why){if(!v)throw new AssertionError(why);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String plain(Component input){var b=new StringBuilder();StringDecomposer.iterateFormatted(input,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component input){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(input,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static GuiMessage message(Component c,GuiMessageSource channel){return new GuiMessage(0,c,null,channel,null);}
    private static Component dispatch(Component c){return SkyRuClient.npcChat(message(c,GuiMessageSource.SYSTEM_SERVER)).content();}
    private static Style style(int seed,int part){return Style.EMPTY.withColor((seed*0x5291+part*0x2739)&0xffffff).withBold(seed%2==0).withItalic(seed%3==0).withShadowColor(seed+part).withInsertion("fresh89 "+seed+"/"+part).withClickEvent(new ClickEvent.SuggestCommand("/unchanged89 "+seed+"/"+part)).withHoverEvent(new HoverEvent.ShowText(Component.literal("hint89 "+seed+"/"+part)));}
    private record Anchor(String source,String target,int from,int to,int part){}
    private record Row(String source,String target,String legacy,List<Anchor> anchors){}
    private static int prefix(String text){int n=text.indexOf(": ")+2;return text.startsWith("✆ ",n)?n+2:n;}
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
        int prefix=prefix(row.source);var out=Component.empty();append(out,time+row.source.substring(0,prefix),style(seed,19),fragments);int at=prefix;
        for(var a:row.anchors){append(out,row.source.substring(at,a.from),style(seed,0),fragments);append(out,a.source,style(seed,a.part),fragments);at=a.from+a.source.length();}
        append(out,row.source.substring(at),style(seed,0),fragments);append(out,suffix,style(seed,20),fragments);return out;
    }
    private static Component publication(Row row,int seed,boolean fragments,String time,String suffix){
        var legacy=Component.literal(row.legacy);String text=plain(legacy);var appearance=styles(legacy);var out=Component.empty();append(out,time,style(seed,19),fragments);int prefix=prefix(row.source);
        for(int at=0;at<text.length();){int end=at+1;while(end<text.length()&&appearance.get(end).equals(appearance.get(at)))end++;if(at<prefix&&end>prefix)end=prefix;
            var current=appearance.get(at).withInsertion("publication89 "+seed).withShadowColor(seed).withClickEvent(new ClickEvent.RunCommand("/retained-publication89 "+seed)).withHoverEvent(new HoverEvent.ShowText(Component.literal("publication hint89 "+seed)));
            append(out,text.substring(at,end),current,fragments);at=end;}
        append(out,suffix,style(seed,20),fragments);return out;
    }
    private static void result(Row row,Component input,String time,String suffix){
        var output=dispatch(input);check(plain(output).equals(time+row.target+suffix),"dispatch "+row.source+" => "+plain(output));
        check(plain(input).equals(time+row.source+suffix),"source immutable");check(dispatch(output)==output,"idempotent original object");
        var a=styles(input);var b=styles(output);
        for(var anchor:row.anchors)for(int n=0;n<anchor.target.length();n++)check(b.get(time.length()+anchor.to+n).equals(a.get(time.length()+anchor.from)),"fresh semantic style/actions: "+anchor.source+" in "+row.source);
        int prefix=time.length()+prefix(row.source);check(a.subList(0,prefix).equals(b.subList(0,prefix)),"timestamp/identity styles/actions");
        if(!suffix.isEmpty())check(a.subList(plain(input).length()-suffix.length(),plain(input).length()).equals(b.subList(plain(output).length()-suffix.length(),plain(output).length())),"counter unchanged");
        var x=Pattern.compile("[0-9]+").matcher(plain(input));var y=Pattern.compile("[0-9]+").matcher(plain(output));
        while(x.find()){check(y.find()&&x.group().equals(y.group()),"number unchanged: "+row.source);check(a.subList(x.start(),x.end()).equals(b.subList(y.start(),y.end())),"number style/actions unchanged: "+row.source+" number="+x.group()+" before="+a.subList(x.start(),x.end())+" after="+b.subList(y.start(),y.end()));}check(!y.find(),"no invented numbers");
    }
    public static void main(String[] args)throws Exception{
        var config=Path.of(args[0]);var root=Path.of(args[1]);var db=new OfflineDictionary(config);db.setRecordUnknown(false);var story=NpcDialogueTranslator.storyDialogues();
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",false);flag("storyDialogues",story);flag("npcDialogues",new NpcDialogueTranslator());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("chatNotifications",new ChatNotificationTranslator(db));
        var reference=JsonParser.parseString(Files.readString(root.resolve("NPC_SOURCES_0.15.89.json"))).getAsJsonObject();
        var evidence=new HashMap<String,JsonObject>();for(var raw:reference.getAsJsonArray("records")){var r=raw.getAsJsonObject();check(evidence.put(r.get("source").getAsString(),r)==null,"unique pinned source");}
        var blocks=new HashMap<String,JsonObject>();for(var raw:reference.getAsJsonArray("blocks")){var b=raw.getAsJsonObject();check(blocks.put(b.get("block_id").getAsString(),b)==null,"unique full block");}
        var packaged=new HashMap<String,JsonObject>();try(var reader=new java.io.InputStreamReader(TestNpc89.class.getResourceAsStream("/data/witherskyru/npc-expansion89.json"),java.nio.charset.StandardCharsets.UTF_8)){for(var raw:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("records")){var r=raw.getAsJsonObject();check(packaged.put(r.get("source").getAsString(),r)==null,"unique executable key");}}
        var rows=new ArrayList<Row>();var seen=new HashSet<String>();var counts=new HashMap<String,Integer>();int historical=0;
        for(String line:Files.readAllLines(root.resolve("NPC_0.15.89_REVIEWED.tsv"))){
            if(line.isEmpty()||line.startsWith("#"))continue;String[] f=line.split("\\|",-1);String prefix="[NPC] "+f[0]+": ",source=prefix+f[1],target=prefix+f[2];
            check(seen.add(source)&&evidence.containsKey(source),"authored source pinned independently");check(packaged.get(source).get("translation").getAsString().equals(target),"authored target verbatim in JAR");
            var frozen=evidence.get(source);var published=frozen.getAsJsonObject("published_row");check(frozen.get("runtime_registration").getAsBoolean(),"literal,not pending");
            if(published.get("historical_heading").getAsBoolean())historical++;
            String legacy=published.get("source_legacy_line").getAsString().strip();check(plain(Component.literal(legacy)).equals(source),"exact published layout");
            for(var raw:frozen.getAsJsonArray("all_occurrences")){var occurrence=raw.getAsJsonObject();var block=blocks.get(occurrence.get("block_id").getAsString());check(block!=null&&block.has("condition_context_raw")&&block.has("template_raw"),"original conditions and block");check(block.get("revision").equals(occurrence.get("revision")),"revision matched");}
            var anchors=new ArrayList<Anchor>();if(f.length==4){int part=1;for(var raw:definitions(f[3])){var a=raw.getAsJsonObject();anchors.add(new Anchor(a.get("source").getAsString(),a.get("target").getAsString(),position(source,a,"source",prefix.length()),position(target,a,"target",prefix.length()),part++));}}
            anchors.sort(Comparator.comparingInt(Anchor::from));rows.add(new Row(source,target,legacy,anchors));counts.merge(f[0],1,Integer::sum);
        }
        check(rows.size()==734&&packaged.keySet().equals(seen),"all734 new literals");check(evidence.size()==757&&reference.getAsJsonArray("pending_variables").size()==13,"complete757 row partition");
        check(counts.equals(Map.ofEntries(Map.entry("Alchemage",25),Map.entry("Aranya",24),Map.entry("Aura",27),Map.entry("Mayor Aura",32),Map.entry("Bruuh",32),Map.entry("Corm",25),Map.entry("Don Expresso",24),Map.entry("Fairylosopher",30),Map.entry("Gerald",26),Map.entry("Gemma",29),Map.entry("Grandpa Wolf",21),Map.entry("Gustave",24),Map.entry("Junker Joel",26),Map.entry("Nall",25),Map.entry("Marco",27),Map.entry("Molbert",30),Map.entry("Pesthunter Phillip",26),Map.entry("Rabbit Bro",26),Map.entry("Rabbit Daddy",24),Map.entry("Rabbit Dog",32),Map.entry("Rabbit Granny",21),Map.entry("Rabbit Sis",30),Map.entry("Rabbit Uncle",31),Map.entry("Rhys",30),Map.entry("Jacquelle",31),Map.entry("Spider Tamer",24),Map.entry("Suus",32)))&&historical==0,"27 counted labels;frozen flags miss page/table archival markers");check(story.phraseCount()==10525,"5813 retained plus734 corpus literals plus3 separately audited extras");
        var archival=JsonParser.parseString(Files.readString(root.resolve("NPC_ARCHIVAL_PROVENANCE_0.15.89.json"))).getAsJsonObject();
        check(archival.get("explicit_archived_or_removed_authored_literals").getAsInt()==53&&archival.getAsJsonArray("records").size()==53,"53 archived rows explicitly separated,not live certified");
        var archivedKeys=new HashSet<String>();for(var raw:archival.getAsJsonArray("records")){var r=raw.getAsJsonObject();String s=r.get("source").getAsString();check(seen.contains(s)&&archivedKeys.add(s),"unique archival authored key");check(!r.getAsJsonArray("all_archival_proofs").isEmpty(),"raw Historical/table evidence present");}
        int layouts=0;
        for(var row:rows){
            var unique=new HashSet<String>();for(var raw:evidence.get(row.source).getAsJsonArray("all_occurrences")){
                String legacy=raw.getAsJsonObject().get("source_legacy_line").getAsString().strip();check(plain(Component.literal(legacy)).equals(row.source),"every published occurrence exact");
                if(unique.add(legacy)){var variant=new Row(row.source,row.target,legacy,row.anchors);result(variant,Component.literal(legacy),"","");for(int seed=3;seed<=4;seed++)for(boolean fragments:List.of(false,true))result(variant,publication(variant,seed,fragments,"[03:04:05] "," (x23)"),"[03:04:05] "," (x23)");layouts++;}
            }
            result(row,Component.literal(row.source),"","");
            for(int seed=1;seed<=2;seed++)for(boolean fragments:List.of(false,true))for(String time:List.of("","[01:02:03] "))for(String suffix:List.of(""," (x17)"," (×2)")){
                result(row,fixture(row,seed,fragments,time,suffix),time,suffix);result(row,publication(row,seed,fragments,time,suffix),time,suffix);
            }
            var input=publication(row,4,false,"","");for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var m=message(input,channel);check(SkyRuClient.npcChat(m)==m,"player/client protected");}
            var signed=new GuiMessage(0,input,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed protected");flag("translateNpcChat",false);check(dispatch(input)==input,"toggle respected");flag("translateNpcChat",true);
        }
        for(var raw:reference.getAsJsonArray("retained_originals")){var retained=raw.getAsJsonObject();String source=retained.get("source").getAsString();var targets=new HashSet<String>();for(var original:retained.getAsJsonArray("original_resources"))targets.add(original.getAsJsonObject().get("translation").getAsString());check(targets.size()==1&&!packaged.containsKey(source),"old targets not overwritten");check(plain(dispatch(Component.literal(source))).equals(targets.iterator().next()),"old flat translation retained");}
        check(reference.getAsJsonArray("retained_originals").size()==4,"four old authored keys retained");
        for(var raw:reference.getAsJsonArray("pending_variables")){String source=raw.getAsJsonObject().get("source").getAsString();var literal=Component.literal(source);check(!packaged.containsKey(source)&&story.translate(literal,db)==literal,"placeholder never faked as literal coverage");}
        check(reference.getAsJsonArray("neutral_unchanged").size()==5,"five neutral sources not inflated as translations");
        for(var raw:reference.getAsJsonArray("neutral_unchanged")){String source=raw.getAsJsonObject().get("source").getAsString();var literal=Component.literal(source);check(!packaged.containsKey(source)&&story.translate(literal,db)==literal,"neutral original object unchanged");}
        check(reference.getAsJsonArray("unsafe_excluded").size()==0,"no frozen unsafe flags in selected branch;53 explicit archival rows reviewed separately");
        for(var raw:reference.getAsJsonArray("unsafe_excluded")){var excluded=raw.getAsJsonObject();String source=excluded.get("source").getAsString();check(excluded.get("historical_heading").getAsBoolean()&&!excluded.get("alpha_page").getAsBoolean()&&excluded.getAsJsonArray("headings").asList().stream().anyMatch(h->h.getAsString().equals("Removed")),"explicit Removed heading,not an alpha-page inference");var literal=Component.literal(source);check(!packaged.containsKey(source)&&story.translate(literal,db)==literal,"excluded old-layout source not silently registered");}
        check(reference.getAsJsonArray("runtime_retained").size()==1,"one old dynamic result not inflated as a new key");
        flag("translateGameChat",true);
        for(var raw:reference.getAsJsonArray("runtime_retained")){var r=raw.getAsJsonObject();String source=r.get("source").getAsString();check(!packaged.containsKey(source),"no unreachable redundant key");check(plain(dispatch(Component.literal(source))).equals(r.get("before_flat").getAsString()),"old dynamic flat output retained");check(plain(dispatch(Component.literal(r.getAsJsonObject("published_row").get("source_legacy_line").getAsString().strip()))).equals(r.get("before_colored").getAsString()),"old dynamic colored output retained");}
        flag("translateGameChat",false);
        for(int n=0;n<rows.size();n+=11){var row=rows.get(n);var own=config.resolve("custom89-"+n);Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),row.source+"\tМой перевод\n");var input=Component.literal(row.source);check(story.translate(input,new OfflineDictionary(own))==input,"full override wins");}
        var sample=rows.stream().filter(r->!r.anchors.isEmpty()).findFirst().orElseThrow();var own=config.resolve("semantic89");Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),sample.anchors.getFirst().source+"\tМой термин\n");var input=publication(sample,5,false,"","");check(story.translate(input,new OfflineDictionary(own))==input,"semantic override wins");
        for(String source:List.of("[NPC] Kat: An unpublished future sentence.","[BOSS] Elizabeth: Hello! Welcome to SkyBlock!","[NPC] Unknown89: Hello!")){var original=Component.literal(source);check(story.translate(original,db)==original,"unknown/wrong tag not guessed");}
        var unsupported=Component.literal("[NPC] Kat: ").append(Component.literal("Take my ").setStyle(style(7,1))).append(Component.literal("vacuum cleaner.").setStyle(style(7,2)));check(story.translate(unsupported,db)==unsupported,"unreviewed semantic split declined");
        check(layouts==736,"all736 published legacy strings including two alternate layouts");
        var extras=JsonParser.parseString(Files.readString(root.resolve("NPC_EXTRAS_0.15.89.json"))).getAsJsonObject().getAsJsonArray("records");check(extras.size()==3,"3 outside frozen denominator");
        var extraPackaged=new HashMap<String,String>();try(var reader=new java.io.InputStreamReader(TestNpc89.class.getResourceAsStream("/data/witherskyru/npc-extras89.json"),java.nio.charset.StandardCharsets.UTF_8)){for(var raw:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("records")){var r=raw.getAsJsonObject();extraPackaged.put(r.get("source").getAsString(),r.get("translation").getAsString());}}
        int extraCount=0;for(String line:Files.readAllLines(root.resolve("NPC89_EXTRAS_REVIEWED.tsv"))){
            if(line.isEmpty()||line.startsWith("#"))continue;String[] f=line.split("\\|",-1);String identity="[NPC] "+f[0]+": ",s=identity+f[1],t=identity+f[2];
            var proof=extras.asList().stream().map(JsonElement::getAsJsonObject).filter(r->r.get("source").getAsString().equals(s)).findFirst().orElseThrow();check(!proof.get("counted_as_frozen_corpus_exact_key").getAsBoolean()&&t.equals(extraPackaged.get(s))&&t.equals(proof.get("translation").getAsString()),"extra authored target source-backed and packaged");
            var aa=new ArrayList<Anchor>();int part=1;for(var raw:definitions(f[3])){var a=raw.getAsJsonObject();aa.add(new Anchor(a.get("source").getAsString(),a.get("target").getAsString(),position(s,a,"source",identity.length()),position(t,a,"target",identity.length()),part++));}aa.sort(Comparator.comparingInt(Anchor::from));
            var row=new Row(s,t,proof.get("source_legacy_line").getAsString(),aa);check(plain(Component.literal(row.legacy)).equals(s),"corrected raw extra visible source");result(row,Component.literal(row.legacy),"","");result(row,Component.literal(s),"","");
            for(int seed=1;seed<=3;seed++)for(boolean fragments:List.of(false,true))for(String time:List.of("","[01:02:03] "))for(String suffix:List.of(""," (x17)")){result(row,fixture(row,seed,fragments,time,suffix),time,suffix);result(row,publication(row,seed,fragments,time,suffix),time,suffix);}
            var ownExtra=config.resolve("extra-"+extraCount);Files.createDirectories(ownExtra);Files.writeString(ownExtra.resolve("overrides.tsv"),s+"\tМой перевод\n");var c=Component.literal(s);check(story.translate(c,new OfflineDictionary(ownExtra))==c,"extra override wins");extraCount++;
        }check(extraCount==3&&extraPackaged.size()==3,"all3 independent extras checked");
        System.out.println("PASS: NPC89 "+checks+" assertions;734 new authored literals/736 published layouts;27 speaker labels;53 explicit archival rows;3 separately counted extras;13 structured sources pending;5 neutral rows;4 old keys/one old dynamic result retained;colors,styles,fresh actions,fragmentation,timestamps,counters,numbers,overrides,channels;not FPS or live completeness");
    }
}
