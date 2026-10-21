import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Independently authored targets + pinned publication layouts, not a runtime self-oracle. */
public final class TestNpc101 {
    private static int checks;
    private static void check(boolean v,String why){if(!v)throw new AssertionError(why);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String plain(Component input){var b=new StringBuilder();StringDecomposer.iterateFormatted(input,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component input){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(input,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static GuiMessage message(Component c,GuiMessageSource channel){return new GuiMessage(0,c,null,channel,null);}
    private static Component dispatch(Component c){return SkyRuClient.npcChat(message(c,GuiMessageSource.SYSTEM_SERVER)).content();}
    private static Style style(int seed,int part){return Style.EMPTY.withColor((seed*0x5291+part*0x2739)&0xffffff).withBold(seed%2==0).withItalic(seed%3==0).withShadowColor(seed+part).withInsertion("fresh90 "+seed+"/"+part).withClickEvent(new ClickEvent.SuggestCommand("/unchanged90 "+seed+"/"+part)).withHoverEvent(new HoverEvent.ShowText(Component.literal("hint90 "+seed+"/"+part)));}
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
            var current=appearance.get(at).withInsertion("publication90 "+seed).withShadowColor(seed).withClickEvent(new ClickEvent.RunCommand("/retained-publication90 "+seed)).withHoverEvent(new HoverEvent.ShowText(Component.literal("publication hint90 "+seed)));
            append(out,text.substring(at,end),current,fragments);at=end;}
        append(out,suffix,style(seed,20),fragments);return out;
    }
    private static void result(Row row,Component input,String time,String suffix){
        var output=dispatch(input);check(plain(output).equals(time+row.target+suffix),"dispatch "+row.source+" => "+plain(output));
        check(plain(input).equals(time+row.source+suffix),"source immutable");check(dispatch(output)==output,"idempotent original object");
        var a=styles(input);var b=styles(output);
        for(var anchor:row.anchors)for(int n=0;n<anchor.target.length();n++)check(b.get(time.length()+anchor.to+n).equals(a.get(time.length()+anchor.from)),"fresh semantic style/actions: "+anchor.source+" in "+row.source);
        String inputPlain=plain(input),outputPlain=plain(output);
        int sourceStart=time.length()+prefix(row.source),targetStart=time.length()+prefix(row.target);
        Style base=null;
        for(int n=sourceStart;n<inputPlain.length()-suffix.length();n++){
            if(Character.isWhitespace(inputPlain.charAt(n)))continue;
            boolean covered=false;for(var semantic:row.anchors)if(n>=time.length()+semantic.from&&n<time.length()+semantic.from+semantic.source.length()){covered=true;break;}
            if(!covered){base=a.get(n);break;}
        }
        if(base==null)base=a.get(sourceStart);
        for(int n=targetStart;n<outputPlain.length()-suffix.length();n++){
            Style expected=base;
            for(var semantic:row.anchors)if(n>=time.length()+semantic.to&&n<time.length()+semantic.to+semantic.target.length()){
                int original=time.length()+semantic.from+(semantic.source.equals(semantic.target)?n-time.length()-semantic.to:0);expected=a.get(original);break;
            }
            check(b.get(n).equals(expected),"every translated character keeps reviewed meaning/style/current actions: "+row.source);
        }
        int prefix=time.length()+prefix(row.source);check(a.subList(0,prefix).equals(b.subList(0,prefix)),"timestamp/identity styles/actions");
        if(!suffix.isEmpty())check(a.subList(plain(input).length()-suffix.length(),plain(input).length()).equals(b.subList(plain(output).length()-suffix.length(),plain(output).length())),"counter unchanged");
        var x=Pattern.compile("[0-9]+").matcher(plain(input));var y=Pattern.compile("[0-9]+").matcher(plain(output));
        while(x.find()){check(y.find()&&x.group().equals(y.group()),"number unchanged: "+row.source);check(a.subList(x.start(),x.end()).equals(b.subList(y.start(),y.end())),"number style/actions unchanged: "+row.source+" number="+x.group()+" before="+a.subList(x.start(),x.end())+" after="+b.subList(y.start(),y.end()));}check(!y.find(),"no invented numbers");
    }
    public static void main(String[] args)throws Exception{
        var config=Path.of(args[0]);var root=Path.of(args[1]);var db=new OfflineDictionary(config);db.setRecordUnknown(false);var story=NpcDialogueTranslator.storyDialogues();
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",false);flag("storyDialogues",story);flag("npcDialogues",new NpcDialogueTranslator());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("chatNotifications",new ChatNotificationTranslator(db));
        var reference=JsonParser.parseString(Files.readString(root.resolve("NPC_SOURCES_0.15.101.json"))).getAsJsonObject();
        var evidence=new HashMap<String,JsonObject>();for(var raw:reference.getAsJsonArray("records")){var r=raw.getAsJsonObject();check(evidence.put(r.get("source").getAsString(),r)==null,"unique pinned source");}
        var blocks=new HashMap<String,JsonObject>();for(var raw:reference.getAsJsonArray("blocks")){var b=raw.getAsJsonObject();check(blocks.put(b.get("block_id").getAsString(),b)==null,"unique full block");}
        var packaged=new HashMap<String,JsonObject>();try(var reader=new java.io.InputStreamReader(TestNpc101.class.getResourceAsStream("/data/witherskyru/npc-expansion101.json"),java.nio.charset.StandardCharsets.UTF_8)){for(var raw:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("records")){var r=raw.getAsJsonObject();check(packaged.put(r.get("source").getAsString(),r)==null,"unique executable key");}}
        var rows=new ArrayList<Row>();var seen=new HashSet<String>();var counts=new HashMap<String,Integer>();int historical=0;
        for(String line:Files.readAllLines(root.resolve("NPC_0.15.101_REVIEWED.tsv"))){
            if(line.isEmpty()||line.startsWith("#"))continue;String[] f=line.split("\\|",-1);String prefix="[NPC] "+f[0]+": ",source=prefix+f[1],target=prefix+f[2];
            check(seen.add(source)&&evidence.containsKey(source),"authored source pinned independently");check(packaged.get(source).get("translation").getAsString().equals(target),"authored target verbatim in JAR");
            var frozen=evidence.get(source);var published=frozen.getAsJsonObject("published_row");check(frozen.get("runtime_registration").getAsBoolean(),"literal,not pending");
            if(published.get("historical_heading").getAsBoolean())historical++;
            String legacy=published.get("source_legacy_line").getAsString().strip();check(plain(Component.literal(legacy)).equals(source),"exact published layout");
            for(var raw:frozen.getAsJsonArray("all_occurrences")){var occurrence=raw.getAsJsonObject();var block=blocks.get(occurrence.get("block_id").getAsString());check(block!=null&&block.has("condition_context_raw")&&block.has("template_raw"),"original conditions and block");check(block.get("revision").equals(occurrence.get("revision")),"revision matched");}
            var anchors=new ArrayList<Anchor>();if(f.length==4){int part=1;for(var raw:definitions(f[3])){var a=raw.getAsJsonObject();anchors.add(new Anchor(a.get("source").getAsString(),a.get("target").getAsString(),position(source,a,"source",prefix.length()),position(target,a,"target",prefix.length()),part++));}}
            anchors.sort(Comparator.comparingInt(Anchor::from));rows.add(new Row(source,target,legacy,anchors));counts.merge(f[0],1,Integer::sum);
        }
        check(rows.size()==385&&packaged.keySet().equals(seen),"all385 new literals");check(evidence.size()==544&&reference.getAsJsonArray("pending_variables").size()==16,"complete544 row partition");
        var expectedCounts=Map.ofEntries(Map.entry("Argofay Sonfather",6),Map.entry("Argofay Treemerger",6),Map.entry("Brigette",6),Map.entry("Dalbrek",6),Map.entry("Duncan",6),Map.entry("Feast Baker Scott",6),Map.entry("Grumblefoot",6),Map.entry("Gulliver",6),Map.entry("Hound",6),Map.entry("Hunter Harry",6),Map.entry("Hunter Jasper",6),Map.entry("Hunter Luigi",6),Map.entry("Hunter Reeves",6),Map.entry("Master Tactician Funk",6),Map.entry("Old Man Garry",6),Map.entry("Researcher Ribery",6),Map.entry("Sawyer",6),Map.entry("Shen Citrinitas",6),Map.entry("Skylark",6),Map.entry("Spike",6),Map.entry("Treehugger Timothy",6),Map.entry("Banker Broadjaw",5),Map.entry("Campfire Adept",5),Map.entry("Combat Merchant",5),Map.entry("Dr. Hibble",5),Map.entry("Fafnir",5),Map.entry("Gavin",5),Map.entry("Gold Forger",5),Map.entry("Krevius",5),Map.entry("Grelius",5),Map.entry("Hunter Billy",5),Map.entry("Hunter Lennard",5),Map.entry("Huntress Melissa",5),Map.entry("Ian",5),Map.entry("Raffle Box",5),Map.entry("Jade Dragon",5),Map.entry("Joey McPizza",5),Map.entry("Maddox",4),Map.entry("Mason",5),Map.entry("Mushroom Guy",5),Map.entry("Obo",5),Map.entry("Rescue Recruiter",5),Map.entry("Roy",5),Map.entry("Royal Guard",5),Map.entry("Scout Plinius",5),Map.entry("Tammy",5),Map.entry("Torrhus Canyon",5),Map.entry("Vaeri",5),Map.entry("Agnes",4),Map.entry("Albert",4),Map.entry("Alixer",4),Map.entry("Ant",4),Map.entry("Aviar",4),Map.entry("Bromm",4),Map.entry("Chunk",4),Map.entry("Curator",4),Map.entry("Ghost of Goon",4),Map.entry("Defunct Wizardman",4),Map.entry("Elmar",4),Map.entry("Eludore",4),Map.entry("Emissary Braum",4),Map.entry("Farmer Jon",4),Map.entry("Fragilis",5),Map.entry("Gnyl",4),Map.entry("Guard",4),Map.entry("Guildford",4),Map.entry("Guy",4),Map.entry("Untitled NPC",4),Map.entry("Jonesy",4),Map.entry("Karis",4),Map.entry("Lumina",4),Map.entry("Sunny",4),Map.entry("Puzzler",4),Map.entry("Researcher Beryl",4),Map.entry("Resident Snooty",4),Map.entry("Soul Campfire Initiate",4),Map.entry("Tilus",4),Map.entry("Tornora",4),Map.entry("Zarina",4));
        check(counts.equals(expectedCounts)&&historical==0,"79 counted labels;canonical rows not historical-tagged;not live certification");check(story.phraseCount()==10799,"9821 retained plus385 authored literals");
        var removedPages=new HashSet<String>();for(var raw:reference.getAsJsonArray("raw_archive_marker_candidates_not_live_certification")){var r=raw.getAsJsonObject();if(r.get("removed_table_or_heading").getAsBoolean()||!r.getAsJsonArray("page_historical_templates").isEmpty())removedPages.add(r.get("page").getAsString());}
        check(removedPages.equals(Set.of("Lumina"))&&reference.getAsJsonArray("source_pages").size()==83,"page-level historical markers retained,not silently live-certified");
        check(packaged.get("[NPC] Gavin: Don't tell mum this, but I just use /scg to open it.").get("translation").getAsString().contains("/scg"),"literal command preserved,never sent");
        String puzzle="[NPC] Puzzler: ▲▼triangle▼▶▲food◀▲";
        check(packaged.get(puzzle).get("translation").getAsString().equals("[NPC] Puzzler: ▲▼треугольник▼▶▲еда◀▲"),"puzzle glyph sequence preserved exactly");
        check(evidence.get("[NPC] Fragilis: Y'know now that we are getting deeper and deeper a lot of these blocks are getting harder to mine!").getAsJsonObject("published_row").get("variable_definition").getAsBoolean(),"reviewed classifier false positive retained as provenance,not hidden queue rewrite");
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
        check(reference.getAsJsonArray("retained_originals").size()==136,"136 old authored keys retained");
        for(var raw:reference.getAsJsonArray("pending_variables")){String source=raw.getAsJsonObject().get("source").getAsString();var literal=Component.literal(source);check(!packaged.containsKey(source)&&story.translate(literal,db)==literal,"placeholder never faked as literal coverage");}
        check(reference.getAsJsonArray("neutral_unchanged").size()==6,"six neutral sources not inflated as translations");
        for(var raw:reference.getAsJsonArray("neutral_unchanged")){String source=raw.getAsJsonObject().get("source").getAsString();var literal=Component.literal(source);check(!packaged.containsKey(source)&&story.translate(literal,db)==literal,"neutral original object unchanged");}
        check(reference.getAsJsonArray("unsafe_excluded").size()==0,"no Removed unique rows selected");
        check(reference.getAsJsonArray("extraction_pending").size()==0,"no malformed-layout source candidates are explicit,not silent literals");
        for(var raw:reference.getAsJsonArray("extraction_pending")){String s=raw.getAsJsonObject().get("source").getAsString();var c=Component.literal(s);check(!packaged.containsKey(s)&&story.translate(c,db)==c,"malformed/extraction candidate not registered");}
        for(var raw:reference.getAsJsonArray("unsafe_excluded")){var excluded=raw.getAsJsonObject();String source=excluded.get("source").getAsString();check(excluded.get("historical_heading").getAsBoolean()&&!excluded.get("alpha_page").getAsBoolean()&&excluded.getAsJsonArray("headings").asList().stream().anyMatch(h->h.getAsString().startsWith("Removed")),"explicit Removed heading,not an alpha-page inference");var literal=Component.literal(source);check(!packaged.containsKey(source)&&story.translate(literal,db)==literal,"excluded old-layout source not silently registered");}
        check(reference.getAsJsonArray("runtime_retained").size()==1,"one old dynamic result retained,not newly counted");
        flag("translateGameChat",true);
        for(var raw:reference.getAsJsonArray("runtime_retained")){var r=raw.getAsJsonObject();String source=r.get("source").getAsString();check(!packaged.containsKey(source),"no unreachable redundant key");check(plain(dispatch(Component.literal(source))).equals(r.get("before_flat").getAsString()),"old dynamic flat output retained");check(plain(dispatch(Component.literal(r.getAsJsonObject("published_row").get("source_legacy_line").getAsString().strip()))).equals(r.get("before_colored").getAsString()),"old dynamic colored output retained");}
        flag("translateGameChat",false);
        for(int n=0;n<rows.size();n+=11){var row=rows.get(n);var own=config.resolve("custom90-"+n);Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),row.source+"\tМой перевод\n");var input=Component.literal(row.source);check(story.translate(input,new OfflineDictionary(own))==input,"full override wins");}
        var sample=rows.stream().filter(r->!r.anchors.isEmpty()).findFirst().orElseThrow();var own=config.resolve("semantic90");Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),sample.anchors.getFirst().source+"\tМой термин\n");var input=publication(sample,5,false,"","");check(story.translate(input,new OfflineDictionary(own))==input,"semantic override wins");
        for(String source:List.of("[NPC] Kat: An unpublished future sentence.","[BOSS] Elizabeth: Hello! Welcome to SkyBlock!","[NPC] Unknown90: Hello!")){var original=Component.literal(source);check(story.translate(original,db)==original,"unknown/wrong tag not guessed");}
        var unsupported=Component.literal("[NPC] Kat: ").append(Component.literal("Take my ").setStyle(style(7,1))).append(Component.literal("vacuum cleaner.").setStyle(style(7,2)));check(story.translate(unsupported,db)==unsupported,"unreviewed semantic split declined");
        check(layouts==385,"all385 published legacy strings");
        System.out.println("PASS: NPC101 "+checks+" assertions;385 authored literals/385 published layouts;79 speaker labels;16 variable,6 neutral,1 previously dynamic row separate;every character/colors/actions/numbers/puzzle glyphs/commands/overrides/protected chat;not FPS/live completeness");
    }
}
