import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Independently authored targets + pinned publication layouts, not a runtime self-oracle. */
public final class TestNpc87 {
    private static int checks;
    private static String newlyReviewed111(String source){return switch(source){
        case "[NPC] Detective Amog: Mmh..." -> "[NPC] Detective Amog: Хм...";
        case "[NPC] Detective Amog: The developer creating this didn't have time to implement a proper barricade breaking sequence." -> "[NPC] Detective Amog: Разработчику, который это делал, не хватило времени на полноценную сцену разрушения баррикады.";
        case "[NPC] Detective Amog: But since we already broke the 4th wall, maybe we can use this opportunity to break this other wall." -> "[NPC] Detective Amog: Но раз уж мы сломали 4-ю стену, может, заодно сломаем и эту.";
        case "[NPC] Detective Amog: Tada!" -> "[NPC] Detective Amog: Та-дам!";
        case "[NPC] Detective Amog: Let's get inside." -> "[NPC] Detective Amog: Пойдём внутрь.";
        default -> null;
    };}

    private static void check(boolean v,String why){if(!v)throw new AssertionError(why);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String plain(Component input){var b=new StringBuilder();StringDecomposer.iterateFormatted(input,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component input){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(input,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static GuiMessage message(Component c,GuiMessageSource channel){return new GuiMessage(0,c,null,channel,null);}
    private static Component dispatch(Component c){return SkyRuClient.npcChat(message(c,GuiMessageSource.SYSTEM_SERVER)).content();}
    private static Style style(int seed,int part){return Style.EMPTY.withColor((seed*0x5291+part*0x2739)&0xffffff).withBold(seed%2==0).withItalic(seed%3==0).withShadowColor(seed+part).withInsertion("fresh87 "+seed+"/"+part).withClickEvent(new ClickEvent.SuggestCommand("/unchanged87 "+seed+"/"+part)).withHoverEvent(new HoverEvent.ShowText(Component.literal("hint87 "+seed+"/"+part)));}
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
            var current=appearance.get(at).withInsertion("publication87 "+seed).withShadowColor(seed).withClickEvent(new ClickEvent.RunCommand("/retained-publication87 "+seed)).withHoverEvent(new HoverEvent.ShowText(Component.literal("publication hint87 "+seed)));
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
        while(x.find()){check(y.find()&&x.group().equals(y.group()),"number unchanged");check(a.subList(x.start(),x.end()).equals(b.subList(y.start(),y.end())),"number style/actions unchanged");}check(!y.find(),"no invented numbers");
    }
    public static void main(String[] args)throws Exception{
        var config=Path.of(args[0]);var root=Path.of(args[1]);var db=new OfflineDictionary(config);db.setRecordUnknown(false);var story=NpcDialogueTranslator.storyDialogues();
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",false);flag("storyDialogues",story);flag("npcDialogues",new NpcDialogueTranslator());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("chatNotifications",new ChatNotificationTranslator(db));
        var reference=JsonParser.parseString(Files.readString(root.resolve("NPC_SOURCES_0.15.87.json"))).getAsJsonObject();
        var evidence=new HashMap<String,JsonObject>();for(var raw:reference.getAsJsonArray("records")){var r=raw.getAsJsonObject();check(evidence.put(r.get("source").getAsString(),r)==null,"unique pinned source");}
        var blocks=new HashMap<String,JsonObject>();for(var raw:reference.getAsJsonArray("blocks")){var b=raw.getAsJsonObject();check(blocks.put(b.get("block_id").getAsString(),b)==null,"unique full block");}
        var packaged=new HashMap<String,JsonObject>();try(var reader=new java.io.InputStreamReader(TestNpc87.class.getResourceAsStream("/data/witherskyru/npc-expansion87.json"),java.nio.charset.StandardCharsets.UTF_8)){for(var raw:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("records")){var r=raw.getAsJsonObject();check(packaged.put(r.get("source").getAsString(),r)==null,"unique executable key");}}
        var rows=new ArrayList<Row>();var seen=new HashSet<String>();var counts=new HashMap<String,Integer>();int historical=0;
        for(String line:Files.readAllLines(root.resolve("NPC_0.15.87_REVIEWED.tsv"))){
            if(line.isEmpty()||line.startsWith("#"))continue;String[] f=line.split("\\|",-1);String prefix="[NPC] "+f[0]+": ",source=prefix+f[1],target=prefix+f[2];
            check(seen.add(source)&&evidence.containsKey(source),"authored source pinned independently");check(packaged.get(source).get("translation").getAsString().equals(target),"authored target verbatim in JAR");
            var frozen=evidence.get(source);var published=frozen.getAsJsonObject("published_row");check(frozen.get("runtime_registration").getAsBoolean(),"literal,not pending");
            if(published.get("historical_heading").getAsBoolean())historical++;
            String legacy=published.get("source_legacy_line").getAsString().strip();check(plain(Component.literal(legacy)).equals(source),"exact published layout");
            for(var raw:frozen.getAsJsonArray("all_occurrences")){var occurrence=raw.getAsJsonObject();var block=blocks.get(occurrence.get("block_id").getAsString());check(block!=null&&block.has("condition_context_raw")&&block.has("template_raw"),"original conditions and block");check(block.get("revision").equals(occurrence.get("revision")),"revision matched");}
            var anchors=new ArrayList<Anchor>();if(f.length==4){int part=1;for(var raw:definitions(f[3])){var a=raw.getAsJsonObject();anchors.add(new Anchor(a.get("source").getAsString(),a.get("target").getAsString(),position(source,a,"source",prefix.length()),position(target,a,"target",prefix.length()),part++));}}
            anchors.sort(Comparator.comparingInt(Anchor::from));rows.add(new Row(source,target,legacy,anchors));counts.merge(f[0],1,Integer::sum);
        }
        check(rows.size()==673&&packaged.keySet().equals(seen),"all673 new literals");check(evidence.size()==707&&reference.getAsJsonArray("pending_variables").size()==28,"complete707 row partition");
        check(counts.equals(Map.ofEntries(Map.entry("Detective Amog",156),Map.entry("Oruo The Almighty",56),Map.entry("David Hunterborough",56),Map.entry("Argofay Bughunter",57),Map.entry("Sirih",48),Map.entry("Kysha",47),Map.entry("Loras",47),Map.entry("Guber",35),Map.entry("Master Tao",35),Map.entry("Odger",36),Map.entry("Sorcerer Okron",36),Map.entry("Velyna",36),Map.entry("Cowboy Nick",28)))&&historical==0,"13 independently counted speaker labels; parser flags are not live availability");check(story.phraseCount()==10799,"4448 retained plus673 new keys");
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
        check(reference.getAsJsonArray("retained_originals").size()==0,"all new branch keys; old resources checked separately");
        for(var raw:reference.getAsJsonArray("pending_variables")){String source=raw.getAsJsonObject().get("source").getAsString();var literal=Component.literal(source);check(!packaged.containsKey(source)&&(newlyReviewed111(source)==null?story.translate(literal,db)==literal:plain(story.translate(literal,db)).equals(newlyReviewed111(source))),"111 explicitly reviewed literal/archive coverage;all other negatives retained");}
        check(reference.getAsJsonArray("neutral_unchanged").size()==1,"one neutral sources not inflated as translations");
        for(var raw:reference.getAsJsonArray("neutral_unchanged")){String source=raw.getAsJsonObject().get("source").getAsString();var literal=Component.literal(source);check(!packaged.containsKey(source)&&story.translate(literal,db)==literal,"neutral original object unchanged");}
        check(reference.getAsJsonArray("unsafe_excluded").size()==5,"five removed historical sources excluded");
        for(var raw:reference.getAsJsonArray("unsafe_excluded")){String source=raw.getAsJsonObject().get("source").getAsString();var literal=Component.literal(source);check(!packaged.containsKey(source)&&(newlyReviewed111(source)==null?story.translate(literal,db)==literal:plain(story.translate(literal,db)).equals(newlyReviewed111(source))),"111 explicitly reviewed literal/archive coverage;all other negatives retained");}
        check(reference.getAsJsonArray("runtime_retained").size()==0,"no old dynamic results not inflated as new keys");
        flag("translateGameChat",true);
        for(var raw:reference.getAsJsonArray("runtime_retained")){var r=raw.getAsJsonObject();String source=r.get("source").getAsString();check(!packaged.containsKey(source),"no unreachable redundant key");check(plain(dispatch(Component.literal(source))).equals(r.get("before_flat").getAsString()),"old dynamic flat output retained");check(plain(dispatch(Component.literal(r.getAsJsonObject("published_row").get("source_legacy_line").getAsString().strip()))).equals(r.get("before_colored").getAsString()),"old dynamic colored output retained");}
        flag("translateGameChat",false);
        for(int n=0;n<rows.size();n+=11){var row=rows.get(n);var own=config.resolve("custom87-"+n);Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),row.source+"\tМой перевод\n");var input=Component.literal(row.source);check(story.translate(input,new OfflineDictionary(own))==input,"full override wins");}
        var sample=rows.stream().filter(r->!r.anchors.isEmpty()).findFirst().orElseThrow();var own=config.resolve("semantic87");Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),sample.anchors.getFirst().source+"\tМой термин\n");var input=publication(sample,5,false,"","");check(story.translate(input,new OfflineDictionary(own))==input,"semantic override wins");
        for(String source:List.of("[NPC] Kat: An unpublished future sentence.","[BOSS] Elizabeth: Hello! Welcome to SkyBlock!","[NPC] Unknown87: Hello!")){var original=Component.literal(source);check(story.translate(original,db)==original,"unknown/wrong tag not guessed");}
        var unsupported=Component.literal("[NPC] Kat: ").append(Component.literal("Take my ").setStyle(style(7,1))).append(Component.literal("vacuum cleaner.").setStyle(style(7,2)));check(story.translate(unsupported,db)==unsupported,"unreviewed semantic split declined");
        check(layouts==688,"all688 distinct published legacy strings including15 alternate layouts");
        System.out.println("PASS: NPC87 "+checks+" assertions;673 new authored literals/688 published layouts;13 speaker labels;28 structured sources excluded;one neutral row;five removed historical rows excluded;colors,styles,fresh actions,fragmentation,timestamps,counters,numbers,overrides,channels;not FPS or live completeness");
    }
}
