import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Independently authored targets + pinned publication layouts, not a runtime self-oracle. */
public final class TestNpc91 {
    private static int checks;
    private static String newlyReviewed111(String source){return switch(source){
        case "[NPC] Drakuu: You might be someone worth taking to, but the time isn't right." -> "[NPC] Drakuu: Возможно, с тобой стоит поговорить, но время ещё не пришло.";
        case "[NPC] Drakuu: I could talk to you, but the conditions don't seem right." -> "[NPC] Drakuu: Я мог бы с тобой поговорить, но условия, похоже, неподходящие.";
        case "[NPC] Simon: I've already given you a Sloth Hat for this year! Happy Anniversary!" -> "[NPC] Simon: Я уже дал тебе шапку ленивца в этом году! С годовщиной!";
        case "[NPC] Simon: The century celebration is over." -> "[NPC] Simon: Празднование столетия закончилось.";
        default -> null;
    };}

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
        int prefix=time.length()+prefix(row.source);check(a.subList(0,prefix).equals(b.subList(0,prefix)),"timestamp/identity styles/actions");
        if(!suffix.isEmpty())check(a.subList(plain(input).length()-suffix.length(),plain(input).length()).equals(b.subList(plain(output).length()-suffix.length(),plain(output).length())),"counter unchanged");
        String numericInput=plain(input);if(row.source.equals("[NPC] Fossil Muncher: come back l8r"))numericInput=numericInput.replace("l8r","l_r"); // rebus later,not eight;positions preserved
        var x=Pattern.compile("[0-9]+").matcher(numericInput);var y=Pattern.compile("[0-9]+").matcher(plain(output));
        while(x.find()){check(y.find()&&x.group().equals(y.group()),"number unchanged: "+row.source);check(a.subList(x.start(),x.end()).equals(b.subList(y.start(),y.end())),"number style/actions unchanged: "+row.source+" number="+x.group()+" before="+a.subList(x.start(),x.end())+" after="+b.subList(y.start(),y.end()));}check(!y.find(),"no invented numbers");
    }
    public static void main(String[] args)throws Exception{
        var config=Path.of(args[0]);var root=Path.of(args[1]);var db=new OfflineDictionary(config);db.setRecordUnknown(false);var story=NpcDialogueTranslator.storyDialogues();
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",false);flag("storyDialogues",story);flag("npcDialogues",new NpcDialogueTranslator());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("chatNotifications",new ChatNotificationTranslator(db));
        var reference=JsonParser.parseString(Files.readString(root.resolve("NPC_SOURCES_0.15.91.json"))).getAsJsonObject();
        var evidence=new HashMap<String,JsonObject>();for(var raw:reference.getAsJsonArray("records")){var r=raw.getAsJsonObject();check(evidence.put(r.get("source").getAsString(),r)==null,"unique pinned source");}
        var blocks=new HashMap<String,JsonObject>();for(var raw:reference.getAsJsonArray("blocks")){var b=raw.getAsJsonObject();check(blocks.put(b.get("block_id").getAsString(),b)==null,"unique full block");}
        var packaged=new HashMap<String,JsonObject>();try(var reader=new java.io.InputStreamReader(TestNpc91.class.getResourceAsStream("/data/witherskyru/npc-expansion91.json"),java.nio.charset.StandardCharsets.UTF_8)){for(var raw:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("records")){var r=raw.getAsJsonObject();check(packaged.put(r.get("source").getAsString(),r)==null,"unique executable key");}}
        var rows=new ArrayList<Row>();var seen=new HashSet<String>();var counts=new HashMap<String,Integer>();int historical=0;
        for(String line:Files.readAllLines(root.resolve("NPC_0.15.91_REVIEWED.tsv"))){
            if(line.isEmpty()||line.startsWith("#"))continue;String[] f=line.split("\\|",-1);String prefix="[NPC] "+f[0]+": ",source=prefix+f[1],target=prefix+f[2];
            check(seen.add(source)&&evidence.containsKey(source),"authored source pinned independently");check(packaged.get(source).get("translation").getAsString().equals(target),"authored target verbatim in JAR");
            var frozen=evidence.get(source);var published=frozen.getAsJsonObject("published_row");check(frozen.get("runtime_registration").getAsBoolean(),"literal,not pending");
            if(published.get("historical_heading").getAsBoolean())historical++;
            String legacy=published.get("source_legacy_line").getAsString().strip();check(plain(Component.literal(legacy)).equals(source),"exact published layout");
            for(var raw:frozen.getAsJsonArray("all_occurrences")){var occurrence=raw.getAsJsonObject();var block=blocks.get(occurrence.get("block_id").getAsString());check(block!=null&&block.has("condition_context_raw")&&block.has("template_raw"),"original conditions and block");check(block.get("revision").equals(occurrence.get("revision")),"revision matched");}
            var anchors=new ArrayList<Anchor>();if(f.length==4){int part=1;for(var raw:definitions(f[3])){var a=raw.getAsJsonObject();anchors.add(new Anchor(a.get("source").getAsString(),a.get("target").getAsString(),position(source,a,"source",prefix.length()),position(target,a,"target",prefix.length()),part++));}}
            anchors.sort(Comparator.comparingInt(Anchor::from));rows.add(new Row(source,target,legacy,anchors));counts.merge(f[0],1,Integer::sum);
        }
        check(rows.size()==649&&packaged.keySet().equals(seen),"all649 new literals");check(evidence.size()==706&&reference.getAsJsonArray("pending_variables").size()==35,"complete706 row partition");
        var expectedCounts=Map.ofEntries(Map.entry("Simon",21),Map.entry("Arbadak",21),Map.entry("Argofay Threebrother",21),Map.entry("Kuudra Believer",21),Map.entry("Lasea",21),Map.entry("Melody ♫",21),Map.entry("Nuvian",21),Map.entry("Plenk",21),Map.entry("Safari Receptionist",21),Map.entry("An",20),Map.entry("Queen Nyx",20),Map.entry("Colore",20),Map.entry("Creed",20),Map.entry("Jotraeline Greatforge",20),Map.entry("Kelly",20),Map.entry("Ävaeìkx",20),Map.entry("Mazakala",20),Map.entry("Yoink",20),Map.entry("Barbarian Emissary",19),Map.entry("Chihai",19),Map.entry("Drakuu",19),Map.entry("Hattie",19),Map.entry("Hungry Hiker",19),Map.entry("Jacob",19),Map.entry("Jine",19),Map.entry("Kuudra Loremaster",19),Map.entry("Marigold",19),Map.entry("Pearl Dealer",19),Map.entry("Auryon",18),Map.entry("Button",18),Map.entry("Coral",18),Map.entry("Fossil Muncher",18),Map.entry("Inverted Sirius",18));
        check(counts.equals(expectedCounts)&&historical==0,"33 counted labels;selected canonical rows not historical-tagged;not live certification");check(story.phraseCount()==10799,"7261 retained plus649 authored literals");
        var removedPages=new HashSet<String>();for(var raw:reference.getAsJsonArray("raw_archive_marker_candidates_not_live_certification")){var r=raw.getAsJsonObject();if(r.get("removed_table_or_heading").getAsBoolean()||!r.getAsJsonArray("page_historical_templates").isEmpty())removedPages.add(r.get("page").getAsString());}
        check(removedPages.equals(Set.of("Simon","Nuvian","Drakuu"))&&reference.getAsJsonArray("source_pages").size()==41,"page-level historical markers retained,not silently live-certified");
        check(packaged.get("[NPC] Fossil Muncher: come back l8r").get("translation").getAsString().equals("[NPC] Fossil Muncher: приходи позже"),"l8r rebus translated,not treated as quantity8");
        check(packaged.get("[NPC] Button: If you say \"Help Wizardman!\" I am sure they will come save you.").get("translation").getAsString().contains("Help Wizardman!"),"literal server trigger retained with Russian explanation");
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
        check(reference.getAsJsonArray("retained_originals").size()==17,"17 old authored keys retained");
        for(var raw:reference.getAsJsonArray("pending_variables")){String source=raw.getAsJsonObject().get("source").getAsString();var literal=Component.literal(source);check(!packaged.containsKey(source)&&(newlyReviewed111(source)==null?story.translate(literal,db)==literal:plain(story.translate(literal,db)).equals(newlyReviewed111(source))),"111 explicitly reviewed literal/archive coverage;all other negatives retained");}
        check(reference.getAsJsonArray("neutral_unchanged").size()==1,"one neutral source not inflated as translation");
        for(var raw:reference.getAsJsonArray("neutral_unchanged")){String source=raw.getAsJsonObject().get("source").getAsString();var literal=Component.literal(source);check(!packaged.containsKey(source)&&story.translate(literal,db)==literal,"neutral original object unchanged");}
        check(reference.getAsJsonArray("unsafe_excluded").size()==4,"four Removed-only keys remain excluded");
        for(var raw:reference.getAsJsonArray("unsafe_excluded")){var excluded=raw.getAsJsonObject();String source=excluded.get("source").getAsString();check(excluded.get("historical_heading").getAsBoolean()&&!excluded.get("alpha_page").getAsBoolean()&&excluded.getAsJsonArray("headings").asList().stream().anyMatch(h->h.getAsString().startsWith("Removed")),"explicit Removed heading,not an alpha-page inference");var literal=Component.literal(source);check(!packaged.containsKey(source)&&(newlyReviewed111(source)==null?story.translate(literal,db)==literal:plain(story.translate(literal,db)).equals(newlyReviewed111(source))),"111 explicitly reviewed literal/archive coverage;all other negatives retained");}
        check(reference.getAsJsonArray("runtime_retained").size()==0,"no redundant old dynamic results counted");
        flag("translateGameChat",true);
        for(var raw:reference.getAsJsonArray("runtime_retained")){var r=raw.getAsJsonObject();String source=r.get("source").getAsString();check(!packaged.containsKey(source),"no unreachable redundant key");check(plain(dispatch(Component.literal(source))).equals(r.get("before_flat").getAsString()),"old dynamic flat output retained");check(plain(dispatch(Component.literal(r.getAsJsonObject("published_row").get("source_legacy_line").getAsString().strip()))).equals(r.get("before_colored").getAsString()),"old dynamic colored output retained");}
        flag("translateGameChat",false);
        for(int n=0;n<rows.size();n+=11){var row=rows.get(n);var own=config.resolve("custom90-"+n);Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),row.source+"\tМой перевод\n");var input=Component.literal(row.source);check(story.translate(input,new OfflineDictionary(own))==input,"full override wins");}
        var sample=rows.stream().filter(r->!r.anchors.isEmpty()).findFirst().orElseThrow();var own=config.resolve("semantic90");Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),sample.anchors.getFirst().source+"\tМой термин\n");var input=publication(sample,5,false,"","");check(story.translate(input,new OfflineDictionary(own))==input,"semantic override wins");
        for(String source:List.of("[NPC] Kat: An unpublished future sentence.","[BOSS] Elizabeth: Hello! Welcome to SkyBlock!","[NPC] Unknown90: Hello!")){var original=Component.literal(source);check(story.translate(original,db)==original,"unknown/wrong tag not guessed");}
        var unsupported=Component.literal("[NPC] Kat: ").append(Component.literal("Take my ").setStyle(style(7,1))).append(Component.literal("vacuum cleaner.").setStyle(style(7,2)));check(story.translate(unsupported,db)==unsupported,"unreviewed semantic split declined");
        check(layouts==656,"all656 published legacy strings");
        System.out.println("PASS: NPC91 "+checks+" assertions;649 new authored literals/656 published layouts;33 speaker labels;35 structured sources pending;one neutral row;17 old keys retained;four Removed-only keys excluded;l8r rebus and literal server trigger;colors,styles,fresh actions,fragmentation,timestamps,counters,numbers,overrides,channels;not FPS or live completeness");
    }
}
