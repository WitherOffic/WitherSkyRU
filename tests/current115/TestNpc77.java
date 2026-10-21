import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Authored expectations + independently frozen publication layouts; no game launch. */
public final class TestNpc77 {
    private static int checks;
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String plain(Component input){var out=new StringBuilder();StringDecomposer.iterateFormatted(input,Style.EMPTY,(i,s,p)->{out.appendCodePoint(p);return true;});return out.toString();}
    private static List<Style> styles(Component input){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(input,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static GuiMessage message(Component value,GuiMessageSource source){return new GuiMessage(0,value,null,source,null);}
    private static Component queued(Component input){return SkyRuClient.npcChat(message(input,GuiMessageSource.SYSTEM_SERVER)).content();}
    private static Style style(int seed,int span){return Style.EMPTY.withColor((0x9343*seed+0x3517*span)&0xffffff).withBold(seed%2==0).withItalic(seed%3==0).withShadowColor(seed+span).withInsertion("fresh77 "+seed+"/"+span).withClickEvent(new ClickEvent.SuggestCommand("/retained77 "+seed+"/"+span)).withHoverEvent(new HoverEvent.ShowText(Component.literal("current77 "+seed+"/"+span)));}
    private record Anchor(String source,String target,int from,int to,int span){}
    private record Row(String source,String target,List<Anchor> anchors,String legacy){}
    private static void append(MutableComponent out,String value,Style style,boolean fragments){for(int at=0;at<value.length();){int end=fragments?Math.min(at+7,value.length()):value.length();out.append(Component.literal(value.substring(at,end)).setStyle(style));at=end;}}
    private static Component fixture(Row row,int seed,boolean fragments,String time,String suffix){
        int prefix=row.source.indexOf(": ")+2;if(row.source.startsWith("✆ ",prefix))prefix+=2;
        var out=Component.empty();append(out,time+row.source.substring(0,prefix),style(seed,19),fragments);int at=prefix;
        for(var anchor:row.anchors){append(out,row.source.substring(at,anchor.from),style(seed,0),fragments);append(out,anchor.source,style(seed,anchor.span),fragments);at=anchor.from+anchor.source.length();}
        append(out,row.source.substring(at),style(seed,0),fragments);append(out,suffix,style(seed,20),fragments);return out;
    }
    private static void numbers(Component a,Component b){var x=java.util.regex.Pattern.compile("[0-9]+").matcher(plain(a));var y=java.util.regex.Pattern.compile("[0-9]+").matcher(plain(b));var xs=styles(a);var ys=styles(b);while(x.find()){check(y.find()&&x.group().equals(y.group()),"numeric values retained");check(xs.subList(x.start(),x.end()).equals(ys.subList(y.start(),y.end())),"numeric appearance/actions retained");}check(!y.find(),"no invented numbers");}
    private static void semantic(Row row,Component input,Component output,String time){var a=styles(input);var b=styles(output);for(var anchor:row.anchors){var expected=a.get(time.length()+anchor.from);for(int n=0;n<anchor.target.length();n++)check(b.get(time.length()+anchor.to+n).equals(expected),"semantic style/action retained: "+anchor.source+" in "+row.source);}}
    public static void main(String[] args)throws Exception{
        var config=Path.of(args[0]);var sourceRoot=Path.of(args[1]);var db=new OfflineDictionary(config);db.setRecordUnknown(false);var npc=NpcDialogueTranslator.storyDialogues();
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",false);flag("storyDialogues",npc);flag("npcDialogues",new NpcDialogueTranslator());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("chatNotifications",new ChatNotificationTranslator(db));
        var evidence=new HashMap<String,JsonObject>();for(var raw:JsonParser.parseString(Files.readString(sourceRoot.resolve("NPC_SOURCES_0.15.77.json"))).getAsJsonObject().getAsJsonArray("records")){var row=raw.getAsJsonObject();check(evidence.put(row.get("source").getAsString(),row)==null,"unique frozen source");}
        var packaged=new HashMap<String,JsonObject>();for(var raw:JsonParser.parseReader(new java.io.InputStreamReader(TestNpc77.class.getResourceAsStream("/data/witherskyru/npc-expansion77.json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("records")){var row=raw.getAsJsonObject();check(packaged.put(row.get("source").getAsString(),row)==null,"unique resource key");}
        var rows=new ArrayList<Row>();var speakers=new HashSet<String>();
        for(String line:Files.readAllLines(sourceRoot.resolve("NPC_0.15.77_REVIEWED.tsv"))){if(line.isEmpty()||line.startsWith("#"))continue;String[] fields=line.split("\\|",-1);String prefix="[NPC] "+fields[0]+": ",source=prefix+fields[1],target=prefix+fields[2];speakers.add(fields[0]);var reference=evidence.get(source);check(reference!=null,"source independent of targets");check(packaged.containsKey(source)&&packaged.get(source).get("translation").getAsString().equals(target),"packaged equals authored Russian");
            var anchors=new ArrayList<Anchor>();if(fields.length==4){var definitions=JsonParser.parseString(fields[3]).getAsJsonObject();int span=1;for(var entry:definitions.entrySet()){String term=entry.getKey(),translated=entry.getValue().getAsString();int from=source.indexOf(term,prefix.length()),to=target.indexOf(translated,prefix.length());check(from>=0&&to>=0&&source.indexOf(term,from+1)<0&&target.indexOf(translated,to+1)<0,"unique independently located anchor");anchors.add(new Anchor(term,translated,from,to,span++));}}anchors.sort(Comparator.comparingInt(Anchor::from));
            String legacy=reference.get("source_legacy_line").getAsString().strip();check(plain(Component.literal(legacy)).equals(source),"published legacy layout is complete exact source: "+source);rows.add(new Row(source,target,anchors,legacy));
        }
        check(rows.size()==137&&speakers.size()==25&&evidence.size()==137&&packaged.size()==137,"137 rows /25 speaker identities, no guessed dynamic values");check(npc.phraseCount()==10799,"1149 retained plus137 new story keys plus324 NPC78 additions");
        for(var row:rows){
            check(plain(queued(Component.literal(row.source))).equals(row.target),"flat dispatch: "+row.source);
            var published=Component.literal(row.legacy);var translated=queued(published);check(plain(translated).equals(row.target),"published legacy color layout: "+row.source+" => "+plain(translated));semantic(row,published,translated,"");numbers(published,translated);
            for(int seed=1;seed<=2;seed++)for(boolean fragments:List.of(false,true))for(String time:List.of("","[01:02:03] "))for(String suffix:List.of(""," (x17)"," (×2)")){
                var input=fixture(row,seed,fragments,time,suffix);var output=queued(input);check(plain(output).equals(time+row.target+suffix),"full dispatch: "+row.source+" => "+plain(output));check(plain(input).equals(time+row.source+suffix),"source immutable");check(queued(output)==output,"idempotent original object");semantic(row,input,output,time);numbers(input,output);
                int prefix=row.source.indexOf(": ")+2;if(row.source.startsWith("✆ ",prefix))prefix+=2;check(styles(input).subList(0,time.length()+prefix).equals(styles(output).subList(0,time.length()+prefix)),"timestamp identity phone styles/actions retained");
                if(!suffix.isEmpty())check(styles(input).subList(plain(input).length()-suffix.length(),plain(input).length()).equals(styles(output).subList(plain(output).length()-suffix.length(),plain(output).length())),"repeat counter current appearance");
                for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var m=message(input,channel);check(SkyRuClient.npcChat(m)==m,"protected channel");}
            }
            // Convert the published legacy layout to modern inherited styles, with fresh actions.
            for(int seed=3;seed<=4;seed++){var modern=Component.empty();final int actionSeed=seed;StringDecomposer.iterateFormatted(Component.literal(row.legacy),Style.EMPTY,(i,s,p)->{modern.append(Component.literal(new String(Character.toChars(p))).setStyle(s.withInsertion("current publication "+actionSeed).withHoverEvent(new HoverEvent.ShowText(Component.literal("hint "+actionSeed))).withClickEvent(new ClickEvent.RunCommand("/unchanged-publication77 "+actionSeed))));return true;});
                // Merge adjacent equal styles: vanilla trees need not have one sibling per character.
                var merged=Component.empty();var text=plain(modern);var appearance=styles(modern);for(int at=0;at<text.length();){int end=at+1;while(end<text.length()&&appearance.get(end).equals(appearance.get(at)))end++;merged.append(Component.literal(text.substring(at,end)).setStyle(appearance.get(at)));at=end;}
                var output=queued(merged);check(plain(output).equals(row.target),"modern published styles/actions: "+row.source);semantic(row,merged,output,"");numbers(merged,output);
            }
            var input=fixture(row,5,false,"","");var signed=new GuiMessage(0,input,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed source protected");flag("translateNpcChat",false);check(queued(input)==input,"NPC off switch");flag("translateNpcChat",true);
        }
        for(int n=0;n<rows.size();n+=13){var row=rows.get(n);var own=config.resolve("override77-"+n);Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),row.source+"\tМой перевод\n");var custom=new OfflineDictionary(own);custom.setRecordUnknown(false);var input=Component.literal(row.source);check(npc.translate(input,custom)==input,"full key override wins");}
        for(String source:List.of("[NPC] Duke: You seem new here. You should probably try to get some [crop] for me.","[NPC] Unknown77: Hello.","[NPC] Croesus: An unknown future sentence.","[BOSS] Mort: Hmm, what's this?","[NPC] Malik: ✆ Survive long enough to collect 9,999 Undead Essence, and then bring it to me.")){var input=Component.literal(source);check(npc.translate(input,db)==input,"unknowns not synthesized");}
        System.out.println("PASS: NPC77 "+checks+" assertions;137 independent published rows/25 speaker identities;authored targets, actual legacy/modern colors, fresh actions, numbers, timestamps, repeats, fragmentation, signed/player/client protections, overrides; no live FPS or all-branches claim");
    }
}
