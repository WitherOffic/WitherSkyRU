import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Independently authored targets + pinned publication layouts, not a runtime self-oracle. */
public final class TestNpc102 {
    private static int checks;
    private static String newlyReviewed111(String source){return switch(source){
        case "[NPC] [Eye]: Quit it!" -> "[NPC] [Eye]: Прекрати!";
        case "[NPC] Andrew: If you'd like to discuss SkyBlock with other players then check out the SkyBlock section of the forums!" -> "[NPC] Andrew: Хочешь обсудить SkyBlock с другими игроками? Загляни в раздел SkyBlock на форумах!";
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
        flag("winterDialogues",NpcDialogueTranslator.winterDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());flag("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());
        var reference=JsonParser.parseString(Files.readString(root.resolve("NPC_SOURCES_0.15.102.json"))).getAsJsonObject();
        var evidence=new HashMap<String,JsonObject>();for(var raw:reference.getAsJsonArray("records")){var r=raw.getAsJsonObject();check(evidence.put(r.get("source").getAsString(),r)==null,"unique pinned source");}
        var blocks=new HashMap<String,JsonObject>();for(var raw:reference.getAsJsonArray("blocks")){var b=raw.getAsJsonObject();check(blocks.put(b.get("block_id").getAsString(),b)==null,"unique full block");}
        var packaged=new HashMap<String,JsonObject>();try(var reader=new java.io.InputStreamReader(TestNpc102.class.getResourceAsStream("/data/witherskyru/npc-expansion102.json"),java.nio.charset.StandardCharsets.UTF_8)){for(var raw:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("records")){var r=raw.getAsJsonObject();check(packaged.put(r.get("source").getAsString(),r)==null,"unique executable key");}}
        var rows=new ArrayList<Row>();var seen=new HashSet<String>();var counts=new HashMap<String,Integer>();int historical=0;
        for(String line:Files.readAllLines(root.resolve("NPC_0.15.102_REVIEWED.tsv"))){
            if(line.isEmpty()||line.startsWith("#"))continue;String[] f=line.split("\\|",-1);String prefix="[NPC] "+f[0]+": ",source=prefix+f[1],target=prefix+f[2];
            check(seen.add(source)&&evidence.containsKey(source),"authored source pinned independently");check(packaged.get(source).get("translation").getAsString().equals(target),"authored target verbatim in JAR");
            var frozen=evidence.get(source);var published=frozen.getAsJsonObject("published_row");check(frozen.get("runtime_registration").getAsBoolean(),"literal,not pending");
            if(published.get("historical_heading").getAsBoolean())historical++;
            String legacy=published.get("source_legacy_line").getAsString().strip();check(plain(Component.literal(legacy)).equals(source),"exact published layout");
            for(var raw:frozen.getAsJsonArray("all_occurrences")){var occurrence=raw.getAsJsonObject();var block=blocks.get(occurrence.get("block_id").getAsString());check(block!=null&&block.has("condition_context_raw")&&block.has("template_raw"),"original conditions and block");check(block.get("revision").equals(occurrence.get("revision")),"revision matched");}
            var anchors=new ArrayList<Anchor>();if(f.length==4){int part=1;for(var raw:definitions(f[3])){var a=raw.getAsJsonObject();anchors.add(new Anchor(a.get("source").getAsString(),a.get("target").getAsString(),position(source,a,"source",prefix.length()),position(target,a,"target",prefix.length()),part++));}}
            anchors.sort(Comparator.comparingInt(Anchor::from));rows.add(new Row(source,target,legacy,anchors));counts.merge(f[0],1,Integer::sum);
        }
        check(rows.size()==316&&packaged.keySet().equals(seen),"all316 new literals");check(evidence.size()==917&&reference.getAsJsonArray("pending_variables").size()==43,"complete917 row partition");
        var expectedCounts=Map.ofEntries(Map.entry("Mayor Aatrox",3),Map.entry("Alabaster",3),Map.entry("Argofay Bugshopper",2),Map.entry("Argofay Doorholder",3),Map.entry("Argofay Serialbather",3),Map.entry("Argofay Tencounter",1),Map.entry("Argofay Trailblazer",3),Map.entry("Ashera",3),Map.entry("Autonull",3),Map.entry("Bartender",1),Map.entry("Belanor",1),Map.entry("Bingo",1),Map.entry("Blobbercyst",2),Map.entry("Bob",1),Map.entry("Bruto",1),Map.entry("Bubu",2),Map.entry("Bylma",1),Map.entry("Calvin",3),Map.entry("Campfire Initiate",3),Map.entry("Carly",3),Map.entry("Carpenter",1),Map.entry("Carrot King Avatar",3),Map.entry("Cashier",2),Map.entry("Chester",3),Map.entry("Chicken",2),Map.entry("Christopher",2),Map.entry("Mayor Cole",3),Map.entry("Counsellor",3),Map.entry("Cow",2),Map.entry("Cyndarin",2),Map.entry("Dackinoru",3),Map.entry("Dante Goon",1),Map.entry("Darr",1),Map.entry("Deer",2),Map.entry("Deng",1),Map.entry("Desperate Engineer",1),Map.entry("Mayor Diana",3),Map.entry("Mayor Diaz",3),Map.entry("Dr. Phear",1),Map.entry("----",1),Map.entry("Emissary Lissandra",3),Map.entry("Emissary Carlton",1),Map.entry("Emissary Ceanna",3),Map.entry("Emissary Fraiser",3),Map.entry("Emissary Sisko",2),Map.entry("Emissary Wilson",1),Map.entry("Finance Rabbit",3),Map.entry("Mayor Finnegan",3),Map.entry("Finplex",2),Map.entry("Fisherman Gerald",1),Map.entry("Forager Daniel",3),Map.entry("Forager Rowan",1),Map.entry("Forast",3),Map.entry("Forger",1),Map.entry("Mayor Foxy",3),Map.entry("Frozen Alex",2),Map.entry("Garlacius",3),Map.entry("Ghost of Armorsmith",3),Map.entry("Gladiator",2),Map.entry("Golden Dragon",3),Map.entry("Grandma",1),Map.entry("Gregory",2),Map.entry("Gwendolyn",1),Map.entry("Gwynnie",3),Map.entry("Hilda",3),Map.entry("Hoppity",1),Map.entry("Andrew",2),Map.entry("Hunter Ava",3),Map.entry("Hunter Jean",3),Map.entry("Hunter McNeil",3),Map.entry("Huntress Alissa",3),Map.entry("Husbandry Merchant",2),Map.entry("Iron Forger",2),Map.entry("Ivan",3),Map.entry("Ivy",2),Map.entry("Jeff",2),Map.entry("Jimmy",1),Map.entry("Kalhuiki Door Guardian",3),Map.entry("Kuudra Archaeologist",3),Map.entry("Lazarus",3),Map.entry("Lonely Philosopher",2),Map.entry("Lost Wizard",2),Map.entry("Ludleth",3),Map.entry("Maggie",1),Map.entry("Mayor Marina",3),Map.entry("Marthos",3),Map.entry("Martin",3),Map.entry("Maths Enjoyer",2),Map.entry("Maxim",3),Map.entry("Melancholic Viking",3),Map.entry("Mino",3),Map.entry("Motes Grubber",3),Map.entry("Mayor Nancy",2),Map.entry("Minister Nancy",2),Map.entry("Nene",1),Map.entry("Noway",3),Map.entry("Old Shaman Nyko",3),Map.entry("Mayor Paul",2),Map.entry("Pest Wrangler",3),Map.entry("Pest Wrangler?",3),Map.entry("Phillip",1),Map.entry("Phaser",3),Map.entry("Piglin Guard",1),Map.entry("Rabbit Security",3),Map.entry("Ravenous Rhino",3),Map.entry("Researcher Betty",3),Map.entry("Researcher Jade",3),Map.entry("Researcher Jessica",3),Map.entry("Researcher Kay",1),Map.entry("Researcher Tay",1),Map.entry("Researcher Timmy",3),Map.entry("Researcher Tina",3),Map.entry("Resident Neighbor",2),Map.entry("Resistance Leader",1),Map.entry("Rhanora Cat",3),Map.entry("Roger",3),Map.entry("Romero",1),Map.entry("Roosterzilla",2),Map.entry("Rusty",2),Map.entry("Safari Shill",2),Map.entry("Salesman",2),Map.entry("Salvage Old Attributes",3),Map.entry("Mayor Scorpius",2),Map.entry("Scout Scardius",3),Map.entry("Sheep",3),Map.entry("Sherry",3),Map.entry("Shifty",1),Map.entry("☣ Sick Farmer ☣",1),Map.entry("Yoshua",2),Map.entry(" St. Jerry",1),Map.entry("The Lotus Eater",1),Map.entry("Unbound Explorer",2),Map.entry("Uvia",2),Map.entry("Vanessa",2),Map.entry("Vargul",2),Map.entry("Vesuvius",2),Map.entry("Vinyl Collector",3),Map.entry("Vreike",3),Map.entry("Weaponsmith",3),Map.entry("Wool Weaver",1),Map.entry("yrreJ",3),Map.entry("Ziri",2));
        check(counts.equals(expectedCounts)&&historical==0,"142 counted labels;canonical rows not historical-tagged;not live certification");check(story.phraseCount()==10799,"all retained and new literal resources including3 current API additions");
        var removedPages=new HashSet<String>();for(var raw:reference.getAsJsonArray("raw_archive_marker_candidates_not_live_certification")){var r=raw.getAsJsonObject();if(r.get("removed_table_or_heading").getAsBoolean()||!r.getAsJsonArray("page_historical_templates").isEmpty())removedPages.add(r.get("page").getAsString());}
        check(removedPages.equals(Set.of("Bartender","Calvin","Dark Auction","Darr","Finnegan","Foxy","Golden Dragon","Hub Villagers","Mino","Resistance Leader","Vanessa"))&&reference.getAsJsonArray("source_pages").size()==148,"page-level historical markers retained,not silently live-certified");
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
        for(var raw:reference.getAsJsonArray("retained_originals")){var retained=raw.getAsJsonObject();String source=retained.get("source").getAsString();var targets=new HashSet<String>();for(var original:retained.getAsJsonArray("original_resources"))targets.add(original.getAsJsonObject().get("translation").getAsString());check(targets.size()==1&&!packaged.containsKey(source),"old targets not overwritten");check(plain(dispatch(Component.literal(source))).equals(targets.iterator().next()),"old flat translation retained: "+source+" => "+plain(dispatch(Component.literal(source))));}
        check(reference.getAsJsonArray("retained_originals").size()==530,"530 old authored keys retained");
        for(var raw:reference.getAsJsonArray("pending_variables")){String source=raw.getAsJsonObject().get("source").getAsString();var literal=Component.literal(source);check(!packaged.containsKey(source)&&(newlyReviewed111(source)==null?story.translate(literal,db)==literal:plain(story.translate(literal,db)).equals(newlyReviewed111(source))),"111 explicitly reviewed literal/archive coverage;all other negatives retained");}
        check(reference.getAsJsonArray("neutral_unchanged").size()==15,"15 neutral sources not inflated as translations");
        for(var raw:reference.getAsJsonArray("neutral_unchanged")){String source=raw.getAsJsonObject().get("source").getAsString();var literal=Component.literal(source);check(!packaged.containsKey(source)&&story.translate(literal,db)==literal,"neutral original object unchanged");}
        check(reference.getAsJsonArray("unsafe_excluded").size()==2,"2 Removed unique rows not newly registered");
        check(reference.getAsJsonArray("extraction_pending").size()==5,"5 malformed-layout source candidates are explicit,not silent literals");
        for(var raw:reference.getAsJsonArray("extraction_pending")){String s=raw.getAsJsonObject().get("source").getAsString();var c=Component.literal(s);if(s.equals("[NPC] Mayor Nancy: Hi|")){check(!packaged.containsKey(s)&&plain(story.translate(c,db)).equals("[NPC] Mayor Nancy: Привет|"),"115 source-backed nowiki pipe recovered");}else if(s.equals("[NPC] Generow: Click to claim your reward!")){check(!packaged.containsKey(s)&&plain(story.translate(c,db)).equals("[NPC] Generow: Нажми, чтобы забрать награду!"),"115 complete message recovered before next full speaker separator");}else check(!packaged.containsKey(s)&&story.translate(c,db)==c,"malformed/extraction candidate not registered: "+s);}
        for(var raw:reference.getAsJsonArray("unsafe_excluded")){var excluded=raw.getAsJsonObject();String source=excluded.get("source").getAsString();check(excluded.get("historical_heading").getAsBoolean()&&!excluded.get("alpha_page").getAsBoolean()&&excluded.getAsJsonArray("headings").asList().stream().anyMatch(h->h.getAsString().startsWith("Removed")),"explicit Removed heading,not an alpha-page inference");var literal=Component.literal(source);check(!packaged.containsKey(source)&&(newlyReviewed111(source)==null?story.translate(literal,db)==literal:plain(story.translate(literal,db)).equals(newlyReviewed111(source))),"111 explicitly reviewed literal/archive coverage;all other negatives retained");}
        check(reference.getAsJsonArray("runtime_retained").size()==6,"6 old dynamic results not newly counted");
        flag("translateGameChat",true);
        for(var raw:reference.getAsJsonArray("runtime_retained")){var r=raw.getAsJsonObject();String source=r.get("source").getAsString();check(!packaged.containsKey(source),"no unreachable redundant key");check(plain(dispatch(Component.literal(source))).equals(r.get("before_flat").getAsString()),"old dynamic flat output retained");check(plain(dispatch(Component.literal(r.getAsJsonObject("published_row").get("source_legacy_line").getAsString().strip()))).equals(r.get("before_colored").getAsString()),"old dynamic colored output retained");}
        flag("translateGameChat",false);
        for(int n=0;n<rows.size();n+=11){var row=rows.get(n);var own=config.resolve("custom90-"+n);Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),row.source+"\tМой перевод\n");var input=Component.literal(row.source);check(story.translate(input,new OfflineDictionary(own))==input,"full override wins");}
        var sample=rows.stream().filter(r->!r.anchors.isEmpty()).findFirst().orElseThrow();var own=config.resolve("semantic90");Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),sample.anchors.getFirst().source+"\tМой термин\n");var input=publication(sample,5,false,"","");check(story.translate(input,new OfflineDictionary(own))==input,"semantic override wins");
        for(String source:List.of("[NPC] Kat: An unpublished future sentence.","[BOSS] Elizabeth: Hello! Welcome to SkyBlock!","[NPC] Unknown90: Hello!")){var original=Component.literal(source);check(story.translate(original,db)==original,"unknown/wrong tag not guessed");}
        var unsupported=Component.literal("[NPC] Kat: ").append(Component.literal("Take my ").setStyle(style(7,1))).append(Component.literal("vacuum cleaner.").setStyle(style(7,2)));check(story.translate(unsupported,db)==unsupported,"unreviewed semantic split declined");
        check(layouts==317,"all317 published legacy strings");
        System.out.println("PASS: Npc102 "+checks+" assertions;316 independently authored literals/317 published layouts;every character/colors/actions/numbers/fragmentation/overrides/protected chat;not FPS/live completeness");
    }
}
