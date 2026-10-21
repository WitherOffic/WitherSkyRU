import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Independent reviewed targets and published sources; synthetic styles,not live packet proof. */
public final class TestNpc75 {
    private static int checks;
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String plain(Component value){var out=new StringBuilder();StringDecomposer.iterateFormatted(value,Style.EMPTY,(i,s,p)->{out.appendCodePoint(p);return true;});return out.toString();}
    private static List<Style> styles(Component value){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(value,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static GuiMessage message(Component value,GuiMessageSource source){return new GuiMessage(0,value,null,source,null);}
    private static Component queued(Component value){return SkyRuClient.npcChat(message(value,GuiMessageSource.SYSTEM_SERVER)).content();}
    private static Style style(int seed,int span){return Style.EMPTY.withColor((0x13579*seed+0x11307*span)&0xffffff).withBold(seed%2==0).withItalic(seed%3==0).withShadowColor(seed+span).withInsertion("fresh75 "+seed+"/"+span).withClickEvent(new ClickEvent.SuggestCommand("/unchanged75 "+seed+"/"+span)).withHoverEvent(new HoverEvent.ShowText(Component.literal("current hint75 "+seed+"/"+span)));}
    private static void append(MutableComponent out,String text,Style style,boolean fragments){if(!fragments)out.append(Component.literal(text).setStyle(style));else for(int n=0;n<text.length();n+=3)out.append(Component.literal(text.substring(n,Math.min(n+3,text.length()))).setStyle(style));}
    private record Anchor(String source,String target,int start,int destination,int span){}
    private record Row(String speaker,String source,String target,List<Anchor> anchors){}
    private static Component fixture(Row row,String time,String suffix,int seed,boolean fragments){
        var out=Component.empty();int at=row.source.indexOf(": ")+2;append(out,time+row.source.substring(0,at),style(seed,9),fragments);
        for(var anchor:row.anchors){append(out,row.source.substring(at,anchor.start),style(seed,0),fragments);append(out,anchor.source,style(seed,anchor.span),fragments);at=anchor.start+anchor.source.length();}
        append(out,row.source.substring(at),style(seed,0),fragments);append(out,suffix,style(seed,10),fragments);return out;
    }
    private static void numeric(Component before,Component after){
        var a=java.util.regex.Pattern.compile("[0-9]+").matcher(plain(before));var b=java.util.regex.Pattern.compile("[0-9]+").matcher(plain(after));var x=styles(before);var y=styles(after);
        while(a.find()){check(b.find()&&a.group().equals(b.group()),"current numeric value copied");check(x.subList(a.start(),a.end()).equals(y.subList(b.start(),b.end())),"numeric styles/actions copied");}check(!b.find(),"no invented numbers");
    }
    private static int independentPosition(String value,String term,int prefix,boolean whole,int ordinal){
        String boundary=whole?"(?U)(?<![\\p{L}\\p{N}_])":"";String tail=whole?"(?![\\p{L}\\p{N}_])":"";var found=java.util.regex.Pattern.compile(boundary+java.util.regex.Pattern.quote(term)+tail).matcher(value);found.region(prefix,value.length());var positions=new ArrayList<Integer>();while(found.find())positions.add(found.start());return ordinal>0?(positions.size()>=ordinal?positions.get(ordinal-1):-1):(positions.size()==1?positions.getFirst():-1);
    }
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var sourceRoot=Path.of(args[1]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);var npc=NpcDialogueTranslator.storyDialogues();
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",false);flag("storyDialogues",npc);flag("npcDialogues",new NpcDialogueTranslator());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("chatNotifications",new ChatNotificationTranslator(db));
        var evidence=JsonParser.parseString(Files.readString(sourceRoot.resolve("NPC_SOURCES_0.15.75.json"))).getAsJsonObject();var published=new HashMap<String,String>();for(String file:List.of("NPC_SOURCES_0.15.74.json","NPC_SOURCES_0.15.75.json"))for(var raw:JsonParser.parseString(Files.readString(sourceRoot.resolve(file))).getAsJsonObject().getAsJsonArray("pages")){var p=raw.getAsJsonObject();published.put(p.get("name").getAsString(),p.get("section").getAsString());}
        var resource=JsonParser.parseReader(new java.io.InputStreamReader(TestNpc75.class.getResourceAsStream("/data/witherskyru/npc-expansion75.json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("records");
        var retained=JsonParser.parseString(Files.readString(sourceRoot.resolve("NPC_RETAINED_0.15.75.json"))).getAsJsonObject().getAsJsonArray("records");var skipped=new HashSet<String>();for(var row:retained)skipped.add(row.getAsJsonObject().get("source").getAsString());
        var bySource=new HashMap<String,JsonObject>();for(var raw:resource){var row=raw.getAsJsonObject();check(bySource.put(row.get("source").getAsString(),row)==null,"unique new source");}
        var rows=new ArrayList<Row>();var speakers=new HashSet<String>();
        for(String line:Files.readAllLines(sourceRoot.resolve("NPC_0.15.75_REVIEWED.tsv"))){if(line.isEmpty()||line.startsWith("#"))continue;String[] fields=line.split("\\|",-1);String prefix="[NPC] "+fields[0]+": ",source=prefix+fields[1],target=prefix+fields[2];if(skipped.contains(source))continue;
            check(published.get(fields[0]).contains(source),"independent published exact source,not synthesized from translation");var packaged=bySource.get(source);check(packaged!=null&&packaged.get("translation").getAsString().equals(target),"packaged target equals reviewed target");
            var anchors=new ArrayList<Anchor>();if(fields.length==4){var spec=JsonParser.parseString(fields[3]);var definitions=new ArrayList<JsonObject>();if(spec.isJsonArray())for(var item:spec.getAsJsonArray())definitions.add(item.getAsJsonObject());else for(var entry:spec.getAsJsonObject().entrySet()){var definition=new JsonObject();definition.addProperty("source",entry.getKey());definition.addProperty("target",entry.getValue().getAsString());definitions.add(definition);}int span=1;
                for(var definition:definitions){String term=definition.get("source").getAsString(),translated=definition.get("target").getAsString();boolean whole=definition.has("whole_word")&&definition.get("whole_word").getAsBoolean();int a=independentPosition(source,term,prefix.length(),whole,definition.has("source_occurrence")?definition.get("source_occurrence").getAsInt():0),b=independentPosition(target,translated,prefix.length(),whole,definition.has("target_occurrence")?definition.get("target_occurrence").getAsInt():0);check(a>=0&&b>=0,"independent semantic occurrence");anchors.add(new Anchor(term,translated,a,b,span++));}
            }anchors.sort(Comparator.comparingInt(Anchor::start));rows.add(new Row(fields[0],source,target,anchors));speakers.add(fields[0]);
        }
        check(rows.size()==100&&resource.size()==100&&speakers.size()==11&&skipped.isEmpty(),"100 independently reviewed sources/11 speakers,all old resources unchanged");check(npc.phraseCount()==10799,"820 retained story entries plus100 NPC75 and229 independently checked NPC76 additions plus137 NPC77 additions plus324 NPC78 additions");
        var mismatches=new ArrayList<String>();for(var row:rows){String actual=plain(queued(Component.literal(row.source)));if(!actual.equals(row.target))mismatches.add(row.source+" => "+actual+";reviewed="+row.target);}check(mismatches.isEmpty(),"flat dispatch preflight: "+mismatches);
        for(var pair:Map.of("Treecapitator","Древолом","Silex","Кремень Silex","Spider Slayer","Охота на пауков","Contacts Directory","Справочник контактов","Carpentry Table","Стол столяра","Fish Bait","Рыбная наживка","Fishing Bag","Сумка для рыбалки").entrySet())check(db.translate(pair.getKey()).equals(pair.getValue()),"reviewed canonical term "+pair.getKey()+" => "+db.translate(pair.getKey()));
        for(var row:rows){
            for(int seed=1;seed<=2;seed++)for(boolean fragments:List.of(false,true))for(String time:List.of("","[01:02:03] "))for(String suffix:List.of(""," (x17)"," (×2)")){
                var input=fixture(row,time,suffix,seed,fragments);var output=queued(input);check(plain(output).equals(time+row.target+suffix),"complete dispatched NPC75 text: "+row.source+" => "+plain(output));check(plain(input).equals(time+row.source+suffix),"input not mutated");check(queued(output)==output,"already translated object unchanged");numeric(input,output);
                int prefix=row.source.indexOf(": ")+2;check(styles(output).subList(0,time.length()+prefix).equals(styles(input).subList(0,time.length()+prefix)),"timestamp/speaker styles and actions retained");
                var outputStyles=styles(output);for(var anchor:row.anchors)for(int n=time.length()+anchor.destination;n<time.length()+anchor.destination+anchor.target.length();n++)check(outputStyles.get(n).equals(style(seed,anchor.span)),"current semantic color/hover/click/shadow/insertion copied");
                if(!suffix.isEmpty())check(outputStyles.subList(outputStyles.size()-suffix.length(),outputStyles.size()).equals(styles(input).subList(styles(input).size()-suffix.length(),styles(input).size())),"repeat counter appearance copied");
                for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var m=message(input,channel);check(SkyRuClient.npcChat(m)==m,"no player/client chat mutation");}
            }
            var literal=Component.literal("§d[01:02:03] §b[NPC] "+row.speaker+": §f"+row.source.substring(row.source.indexOf(": ")+2));check(plain(npc.translate(literal,db)).equals("[01:02:03] "+row.target),"legacy color parser");
            var input=fixture(row,"","",3,false);flag("translateNpcChat",false);check(queued(input)==input,"NPC off switch");flag("translateNpcChat",true);
            var signed=new GuiMessage(0,input,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed source protected");
        }
        for(int n=0;n<rows.size();n+=30){var row=rows.get(n);var own=root.resolve("override-"+n);Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),row.source+"\tМой диалог\n");var dictionary=new OfflineDictionary(own);dictionary.setRecordUnknown(false);var input=fixture(row,"","",5,false);check(npc.translate(input,dictionary)==input,"full user override wins");if(!row.anchors.isEmpty()){Files.writeString(own.resolve("overrides.tsv"),row.anchors.getFirst().source+"\tМой термин\n");dictionary=new OfflineDictionary(own);dictionary.setRecordUnknown(false);check(npc.translate(input,dictionary)==input,"semantic user override wins");}}
        var choiceRows=JsonParser.parseString(Files.readString(sourceRoot.resolve("NPC_CHOICES_0.15.75.json"))).getAsJsonObject().getAsJsonArray("records");check(choiceRows.size()==5,"five reviewed published layouts");
        for(var raw:choiceRows){
            var row=raw.getAsJsonObject();var labels=row.getAsJsonArray("source");var targets=row.getAsJsonArray("target");boolean arrow=row.get("arrow").getAsBoolean();String speaker=row.get("speaker").getAsString();
            for(var label:labels)check(published.get(speaker).contains("["+label.getAsString()+"]"),"independent published option");
            for(int seed=1;seed<=2;seed++)for(String gap:List.of(""," ","  "))for(String time:List.of("","[01:02:03] "))for(String tail:List.of(""," "))for(String counter:List.of(""," (x17)")){
                var input=Component.literal(time+"Select an option:"+gap).setStyle(style(seed,0));var expected=new StringBuilder(time+"Выберите ответ:"+gap);var buttonStyles=new ArrayList<Style>();
                for(int n=0;n<labels.size();n++){if(n>0){input.append(Component.literal(gap).setStyle(style(seed,0)));expected.append(gap);}if(arrow){input.append(Component.literal("➜"+gap).setStyle(style(seed,0)));expected.append("➜"+gap);}var current=style(seed,n+1).withClickEvent(new ClickEvent.RunCommand("/current-choice75 "+seed+"/"+n));buttonStyles.add(current);input.append(Component.literal("[").setStyle(style(seed,0))).append(Component.literal(labels.get(n).getAsString()).setStyle(current)).append(Component.literal("]").setStyle(style(seed,0)));expected.append("["+targets.get(n).getAsString()+"]");}
                input.append(Component.literal(tail+counter).setStyle(style(seed,10)));expected.append(tail+counter);flag("translateGameChat",false);check(queued(input)==input,"labels obey existing game-chat switch");flag("translateGameChat",true);var output=queued(input);check(plain(output).equals(expected.toString()),"whole option layout: "+plain(input)+" => "+plain(output));check(queued(output)==output,"option idempotence");
                for(int n=0;n<targets.size();n++){String target=targets.get(n).getAsString();int at=plain(output).indexOf(target);check(at>=0,"translated option exists");for(int p=at;p<at+target.length();p++)check(styles(output).get(p).equals(buttonStyles.get(n)),"current command/color/hover/insertion copied,not executed");}
                for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var m=message(input,channel);check(SkyRuClient.npcChat(m)==m,"option player/client guards");}var signed=new GuiMessage(0,input,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed option protected");
            }
        }
        for(String bad:List.of("[NPC] Alda: Welcome to Abiphones & Co., <Username>!","[NPC] Alixer: Splish...BUFF! You have gained [potion effect]! Press TAB or type /effects to view your active effects!","Select an option: ➜ [How are perks chosen?] ➜ [Unknown future answer]","Select an option: [I guess.] [Future]","Select an option: [I guess.]\n","[NPC] Apprentice (Crimson Isle): For a price, you can buy it here!")){var input=Component.literal(bad);check(queued(input)==input,"unknown,placeholder,Rift/faction speaker or layout not guessed");}
        var overrideRoot=root.resolve("choice-override");Files.createDirectories(overrideRoot);Files.writeString(overrideRoot.resolve("overrides.tsv"),"What's an Abiphone?\tМоя кнопка\n");var custom=new OfflineDictionary(overrideRoot);custom.setRecordUnknown(false);flag("dictionary",custom);flag("chatNotifications",new ChatNotificationTranslator(custom));var overridden=Component.literal("Select an option: [What's an Abiphone?]");check(queued(overridden)==overridden,"choice override wins");flag("dictionary",db);flag("chatNotifications",new ChatNotificationTranslator(db));
        for(var raw:retained){var row=raw.getAsJsonObject();var input=Component.literal(row.get("source").getAsString());check(plain(queued(input)).equals(row.get("existing_target").getAsString()),"retained source still uses original target");}
        System.out.println("PASS: NPC75 "+checks+" assertions;100 independently reviewed published sources/11 NPCs,all prior resources retained,timestamps/repeats/fragmented/current semantic styles/actions/legacy/overrides/channel/signed/toggles and five reviewed Alda/Seraphine choices;not every NPC,all live branches or FPS proof");
    }
}
