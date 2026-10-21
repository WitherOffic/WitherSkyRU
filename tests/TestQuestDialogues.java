import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Five observed quest sections: exact prose, original legacy styles, not a live-server census. */
public class TestQuestDialogues {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xffffff);
    private static final Style PREFIX=BODY.withColor(0x55ffff).withInsertion("original-speaker");
    private static final Style ANCHOR=BODY.withColor(0x55ff55).withHoverEvent(new HoverEvent.ShowText(Component.literal("original-term")));
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static GuiMessage msg(Component c,GuiMessageSource channel){return new GuiMessage(0,c,null,channel,null);}
    private static List<Style> styles(Component c){var result=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)result.add(s);return true;});return result;}
    private static void span(Component c,String needle,Style wanted){int begin=needle.contains("[NPC]")?0:Math.max(0,c.getString().indexOf(": ")+2);int at=c.getString().indexOf(needle,begin);check(at>=0,"missing span: "+needle);var ss=styles(c);for(int n=at;n<at+needle.length();n++)check(ss.get(n).equals(wanted),"style/action changed: "+needle);}
    private static List<String> numbers(String text){var m=Pattern.compile("[0-9]+").matcher(text);var out=new ArrayList<String>();while(m.find())out.add(m.group());Collections.sort(out);return out;}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);var quest=NpcDialogueTranslator.questDialogues();
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("npcDialogues",new NpcDialogueTranslator());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",quest);flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("chatNotifications",new ChatNotificationTranslator(db));
        var stream=TestQuestDialogues.class.getResourceAsStream("/data/witherskyru/quest-dialogues.json");
        var rows=JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("records");
        check(rows.size()==quest.phraseCount(),"loaded quest count");var lookup=new HashMap<String,JsonObject>();var passes=new HashSet<Integer>();
        for(var raw:rows){
            var row=raw.getAsJsonObject();String source=row.get("source").getAsString(),target=row.get("translation").getAsString();lookup.put(source,row);passes.add(row.get("pass").getAsInt());
            String prefix="[NPC] "+row.get("speaker").getAsString()+": ";check(target.startsWith(prefix),"original NPC identity");check(numbers(source).equals(numbers(target)),"quantity changed: "+source);
            var anchors=new ArrayList<String[]>();if(row.has("style_anchors"))for(var a:row.getAsJsonArray("style_anchors"))anchors.add(new String[]{a.getAsJsonObject().get("source").getAsString(),a.getAsJsonObject().get("target").getAsString()});anchors.sort(Comparator.comparingInt(a->source.indexOf(a[0],prefix.length())));
            var input=Component.empty().append(Component.literal(prefix).setStyle(PREFIX));int cursor=prefix.length();
            for(var a:anchors){int at=source.indexOf(a[0],cursor);check(at>=cursor,"invalid anchor ordering");input.append(Component.literal(source.substring(cursor,at)).setStyle(BODY));input.append(Component.literal(a[0]).setStyle(ANCHOR));cursor=at+a[0].length();}
            input.append(Component.literal(source.substring(cursor)).setStyle(BODY));
            var result=quest.translate(input,db);check(result.getString().equals(target),"synthetic quest: "+source);span(result,prefix,PREFIX);for(var a:anchors)span(result,a[1],ANCHOR);
            check(input.getString().equals(source),"source mutated");for(int n=0;n<5;n++)check(quest.translate(result,db)==result,"idempotence");
            var queued=msg(input,GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(queued).content().getString().equals(target),"actual queue branch");
            for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var other=msg(input,channel);check(SkyRuClient.npcChat(other)==other,"ordinary player/client text translated");}
            var repeated=input.copy().append(Component.literal(" (x17)").setStyle(PREFIX));var repeatedResult=quest.translate(repeated,db);check(repeatedResult.getString().equals(target+" (x17)"),"duplicate counter");span(repeatedResult," (x17)",PREFIX);
            flag("translateNpcChat",false);check(SkyRuClient.npcChat(queued)==queued,"NPC toggle");flag("translateNpcChat",true);flag("translateGameChat",false);check(SkyRuClient.npcChat(queued).content().getString().equals(target),"game toggle independent");flag("translateGameChat",true);
            var custom=Path.of(args[0]).resolve(row.get("source_key").getAsString());Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),source+"\tМой текст\n");var own=new OfflineDictionary(custom);own.setRecordUnknown(false);check(quest.translate(input,own)==input,"full override");
            for(var a:anchors){Files.writeString(custom.resolve("overrides.tsv"),a[0]+"\tМой термин\n");own=new OfflineDictionary(custom);own.setRecordUnknown(false);check(quest.translate(input,own)==input,"semantic override");}
        }
        int legacy=0;var frozen=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("QUEST_0.15.35_OBSERVED_SAMPLES.json"))).getAsJsonObject().getAsJsonArray("samples");
        for(var raw:frozen){var sample=raw.getAsJsonObject();String source=sample.get("source").getAsString();if(!lookup.containsKey(source))continue;
            String inputText=sample.get("sample").getAsString();String plain=inputText.replaceAll("(?i)§[0-9a-fk-or]","");var tm=Pattern.compile("^\\[\\d{2}:\\d{2}:\\d{2}\\] ").matcher(plain);String timestamp=tm.find()?tm.group():"";String suffix=plain.substring(timestamp.length()+source.length());
            var input=Component.literal(inputText);var queued=msg(input,GuiMessageSource.SYSTEM_SERVER);var result=SkyRuClient.npcChat(queued).content();check(result.getString().equals(timestamp+lookup.get(source).get("translation").getAsString()+suffix),"LEGACY QUEST MISSING: "+source);legacy++;
            var before=styles(input);var after=styles(result);String prefix=timestamp+"[NPC] "+sample.get("speaker").getAsString()+": ";for(int i=0;i<prefix.length();i++)check(before.get(i).equals(after.get(i)),"raw prefix style");
            var row=lookup.get(source);if(row.has("style_anchors"))for(var a:row.getAsJsonArray("style_anchors")){String original=a.getAsJsonObject().get("source").getAsString(),translated=a.getAsJsonObject().get("target").getAsString();int start=plain.indexOf(original,prefix.length());span(result,translated,before.get(start));}
        }
        check(legacy==rows.size(),"one observed sample per reviewed record");if(rows.size()==132)check(passes.size()==5,"five substantive quest sections");
        var objectiveTranslator=NpcDialogueTranslator.questObjectives();
        var goalStream=TestQuestDialogues.class.getResourceAsStream("/data/witherskyru/quest-objectives.json");var goals=JsonParser.parseReader(new java.io.InputStreamReader(goalStream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("records");check(goals.size()==29,"all27 previous goals plusClerk/Elizabeth");
        for(var raw:goals){var row=raw.getAsJsonObject();String source=row.get("source").getAsString(),target=row.get("translation").getAsString();check(numbers(source).equals(numbers(target)),"objective quantity");
            var anchors=new ArrayList<String[]>();for(var a:row.getAsJsonArray("style_anchors"))anchors.add(new String[]{a.getAsJsonObject().get("source").getAsString(),a.getAsJsonObject().get("target").getAsString()});anchors.sort(Comparator.comparingInt(a->source.indexOf(a[0])));
            var input=Component.empty().append(Component.literal("[01:02:03]   ").setStyle(PREFIX));int at=0;for(var a:anchors){int next=source.indexOf(a[0],at);input.append(Component.literal(source.substring(at,next)).setStyle(BODY));input.append(Component.literal(a[0]).setStyle(ANCHOR));at=next+a[0].length();}input.append(Component.literal(source.substring(at)).setStyle(BODY));
            var result=objectiveTranslator.translate(input,db);check(result.getString().equals("[01:02:03]   "+target),"styled objective: "+source);var ss=styles(result);for(var a:anchors){int start=result.getString().indexOf(a[1],13);check(start>=0,"goal anchor target");for(int i=start;i<start+a[1].length();i++)check(ss.get(i).equals(ANCHOR),"goal semantic styles/actions");}
            for(int n=0;n<5;n++)check(objectiveTranslator.translate(result,db)==result,"goal repeat");check(db.translate(source).equals(target),"GUI goal dictionary");
            var queued=msg(input,GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(queued).content().getString().equals(result.getString()),"server objective");flag("translateNpcChat",false);check(SkyRuClient.npcChat(queued).content().getString().equals(result.getString()),"objective independent of NPC toggle");flag("translateNpcChat",true);flag("translateGameChat",false);check(SkyRuClient.npcChat(queued)==queued,"objective game toggle");flag("translateGameChat",true);
            for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var other=msg(input,channel);check(SkyRuClient.npcChat(other)==other,"whole goal player/client text protected");}
            var custom=Path.of(args[0]).resolve(row.get("source_key").getAsString());Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),source+"\tМоя цель\n");var own=new OfflineDictionary(custom);own.setRecordUnknown(false);check(objectiveTranslator.translate(input,own)==input,"full objective override");for(var a:anchors){Files.writeString(custom.resolve("overrides.tsv"),a[0]+"\tМой термин\n");own=new OfflineDictionary(custom);own.setRecordUnknown(false);check(objectiveTranslator.translate(input,own)==input,"goal semantic override");}
        }
        int observedGoals=0;var frozenGoals=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("QUEST_0.15.35_OBJECTIVES.json"))).getAsJsonObject().getAsJsonArray("samples");for(var raw:frozenGoals){var row=raw.getAsJsonObject();var input=msg(Component.literal(row.get("sample").getAsString()),GuiMessageSource.SYSTEM_SERVER);var result=SkyRuClient.npcChat(input);check(result!=input,"LEGACY OBJECTIVE MISSING: "+row.get("source").getAsString());observedGoals++;}check(observedGoals==35,"35 observed goal bodies");
        var unknown=msg(Component.literal("[NPC] Sam: A never observed unknown sentence."),GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(unknown)==unknown,"unknown NPC not guessed");
        for(String text:List.of("pickaxe mining speed Wither_101","[NPC] Moby: So listen.","Bring 8 Glowing Mushroom")){var player=msg(Component.literal(text),GuiMessageSource.PLAYER);check(SkyRuClient.npcChat(player)==player,"whole player message protected");}
        System.out.println("PASS: Quest dialogue "+checks+" assertions; "+rows.size()+" reviewed records, "+legacy+" observed legacy samples, "+passes.size()+" sections, "+goals.size()+" additional goals /35 frozen old goals; not all server quests or live-tested");
    }
}
