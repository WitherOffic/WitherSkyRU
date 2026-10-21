import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Independent reviewed targets and published sources; synthetic styles,not live packet proof. */
public final class TestNpc74 {
    private static int checks;
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String plain(Component value){var out=new StringBuilder();StringDecomposer.iterateFormatted(value,Style.EMPTY,(i,s,p)->{out.appendCodePoint(p);return true;});return out.toString();}
    private static List<Style> styles(Component value){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(value,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static GuiMessage message(Component value,GuiMessageSource source){return new GuiMessage(0,value,null,source,null);}
    private static Component queued(Component value){return SkyRuClient.npcChat(message(value,GuiMessageSource.SYSTEM_SERVER)).content();}
    private static Style style(int seed,int span){return Style.EMPTY.withColor((0x13579*seed+0x11307*span)&0xffffff).withBold(seed%2==0).withItalic(seed%3==0).withShadowColor(seed+span).withInsertion("fresh74 "+seed+"/"+span).withClickEvent(new ClickEvent.SuggestCommand("/unchanged74 "+seed+"/"+span)).withHoverEvent(new HoverEvent.ShowText(Component.literal("current hint74 "+seed+"/"+span)));}
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
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var sourceRoot=Path.of(args[1]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);var npc=NpcDialogueTranslator.storyDialogues();
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",false);flag("storyDialogues",npc);flag("npcDialogues",new NpcDialogueTranslator());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("chatNotifications",new ChatNotificationTranslator(db));
        var evidence=JsonParser.parseString(Files.readString(sourceRoot.resolve("NPC_SOURCES_0.15.74.json"))).getAsJsonObject();var published=new HashMap<String,String>();for(var raw:evidence.getAsJsonArray("pages")){var p=raw.getAsJsonObject();published.put(p.get("name").getAsString(),p.get("section").getAsString());}
        var resource=JsonParser.parseReader(new java.io.InputStreamReader(TestNpc74.class.getResourceAsStream("/data/witherskyru/npc-expansion74.json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("records");
        var retained=JsonParser.parseString(Files.readString(sourceRoot.resolve("NPC_RETAINED_0.15.74.json"))).getAsJsonObject().getAsJsonArray("records");var skipped=new HashSet<String>();for(var row:retained)skipped.add(row.getAsJsonObject().get("source").getAsString());
        var bySource=new HashMap<String,JsonObject>();for(var raw:resource){var row=raw.getAsJsonObject();check(bySource.put(row.get("source").getAsString(),row)==null,"unique new source");}
        var rows=new ArrayList<Row>();var speakers=new HashSet<String>();
        for(String line:Files.readAllLines(sourceRoot.resolve("NPC_0.15.74_REVIEWED.tsv"))){if(line.isEmpty()||line.startsWith("#"))continue;String[] fields=line.split("\\|",-1);String prefix="[NPC] "+fields[0]+": ",source=prefix+fields[1],target=prefix+fields[2];if(skipped.contains(source))continue;
            check(published.get(fields[0]).contains(source),"independent published exact source,not synthesized from translation");var packaged=bySource.get(source);check(packaged!=null&&packaged.get("translation").getAsString().equals(target),"packaged target equals reviewed target");
            var anchors=new ArrayList<Anchor>();if(fields.length==4){var maps=JsonParser.parseString(fields[3]).getAsJsonObject();int span=1;for(var entry:maps.entrySet()){
                String term=entry.getKey(),translated=entry.getValue().getAsString();int a=source.indexOf(term,prefix.length()),b=target.indexOf(translated,prefix.length());check(a>=0&&b>=0&&source.indexOf(term,a+1)<0&&target.indexOf(translated,b+1)<0,"unambiguous independent semantic anchor");anchors.add(new Anchor(term,translated,a,b,span++));
            }}anchors.sort(Comparator.comparingInt(Anchor::start));rows.add(new Row(fields[0],source,target,anchors));speakers.add(fields[0]);
        }
        check(rows.size()==239&&resource.size()==239&&speakers.size()==31&&skipped.size()==7,"239 independently reviewed new sources/31 speakers,7 retained old targets");check(npc.phraseCount()==10799,"820 retained story entries plus100 NPC75 and229 independently checked NPC76 additions plus137 NPC77 additions plus324 NPC78 additions");
        var mismatches=new ArrayList<String>();for(var row:rows){String actual=plain(queued(Component.literal(row.source)));if(!actual.equals(row.target))mismatches.add(row.source+" => "+actual+";reviewed="+row.target);}check(mismatches.isEmpty(),"flat dispatch preflight: "+mismatches);
        check(db.translate("Foraging").equals("Лесорубство"),"active skill glossary,not the older shadowed phrase entry");check(db.translate("Promising Axe").equals("Перспективный топор"),"active item glossary");check(db.translate("Rune Sack").equals("Мешок рун"),"active sack glossary");
        for(var row:rows){if(row.source.contains("Foraging"))check(row.target.contains("лесорубств"),"NPC skill term agrees with active glossary");if(row.source.contains("Promising Axe"))check(row.target.contains("перспективный топор"),"reward item agrees with inventory name");if(row.source.contains("Rune Sack"))check(row.target.contains("мешок рун"),"sack term agrees with inventory name");if(row.source.contains("Safety Advocate"))check(row.target.contains("представител")&&row.target.contains("отряда безопасности"),"Safety title consistent with retained training chain");}
        for(var row:rows){
            for(int seed=1;seed<=2;seed++)for(boolean fragments:List.of(false,true))for(String time:List.of("","[01:02:03] "))for(String suffix:List.of(""," (x17)"," (×2)")){
                var input=fixture(row,time,suffix,seed,fragments);var output=queued(input);check(plain(output).equals(time+row.target+suffix),"complete dispatched NPC74 text: "+row.source+" => "+plain(output));check(plain(input).equals(time+row.source+suffix),"input not mutated");check(queued(output)==output,"already translated object unchanged");numeric(input,output);
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
        var source=Component.literal("Select an option: ").setStyle(style(1,0)).append(Component.literal("[Sure]").setStyle(style(1,1))).append(Component.literal(" [Nah, I'm good]").setStyle(style(1,2)));
        check(queued(source)==source,"existing game-notification switch still controls option labels");flag("translateGameChat",true);
        var choice=queued(source);check(plain(choice).equals("Выберите ответ: [Конечно] [Нет, спасибо]"),"reviewed Lumber Jack option layout");for(String target:List.of("Конечно","Нет, спасибо")){int at=plain(choice).indexOf(target);for(int n=at;n<at+target.length();n++)check(styles(choice).get(n).equals(style(1,target.equals("Конечно")?1:2)),"button action/style preserved,not executed");}
        for(String bad:List.of("[NPC] Enchantment Nerd: Future unrecorded lesson.","[NPC] Stranger: Water troubles? No problem!","Select an option: [Sure] [Future answer]","[NPC] Alchemist: There is a darkness in you, [player].","[NPC] Rosetta: Hey <player name>!!!")){var input=Component.literal(bad);check(queued(input)==input,"unknown speaker/layout/placeholders not guessed");}
        for(var raw:retained){var row=raw.getAsJsonObject();var input=Component.literal(row.get("source").getAsString());check(plain(queued(input)).equals(row.get("existing_target").getAsString()),"retained source still uses original target");}
        System.out.println("PASS: NPC74 "+checks+" assertions;239 independently reviewed published sources/31 NPCs,7 retained targets,timestamps/repeats/fragmented/current semantic styles/actions/legacy/overrides/channel/signed/toggles and reviewed Lumber Jack choices;not every NPC,all live branches or FPS proof");
    }
}
