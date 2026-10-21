import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.StringDecomposer;
import net.minecraft.client.multiplayer.chat.*;

/** Public-source short functional instructions, synthetic formatting; not live NPC captures. */
public class TestStoryDialogues {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xffffff);
    private static final Style PREFIX=BODY.withColor(0x55ffff).withInsertion("speaker");
    private static final Style TERM=BODY.withColor(0x55ff55).withHoverEvent(new HoverEvent.ShowText(Component.literal("original term")));
    private static final Style NICK=BODY.withColor(0xffaa00).withClickEvent(new ClickEvent.SuggestCommand("/msg Wither_101 ")).withInsertion("original player");
    private static Style term(int index){return TERM.withColor(0x110000+index*0x110).withInsertion("original semantic span "+index);}
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);checks++;}
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    private static GuiMessage message(Component c,GuiMessageSource channel){return new GuiMessage(0,c,null,channel,null);}
    private static List<Style> styles(Component c){var ss=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)ss.add(s);return true;});return ss;}
    private static void span(Component c,String text,Style style,int start){int at=c.getString().indexOf(text,start);check(at>=0,"missing "+text);var ss=styles(c);for(int i=at;i<at+text.length();i++)check(ss.get(i).equals(style),"style/action "+text);}
    private static List<String> numbers(String s){var m=java.util.regex.Pattern.compile("[0-9]+(?:,[0-9]{3})*").matcher(s);var ns=new ArrayList<String>();while(m.find())ns.add(m.group());return ns;}
    private static int position(String text,JsonObject a,String side,int begin){String word=a.get(side).getAsString();boolean whole=a.has("whole_word")&&a.get("whole_word").getAsBoolean();String regex=(whole?"(?<![\\p{L}\\p{N}_])":"")+java.util.regex.Pattern.quote(word)+(whole?"(?![\\p{L}\\p{N}_])":"");var m=java.util.regex.Pattern.compile(regex).matcher(text);var positions=new ArrayList<Integer>();while(m.find())if(m.start()>=begin)positions.add(m.start());int nth=a.has(side+"_occurrence")?a.get(side+"_occurrence").getAsInt():0;check(nth==0?positions.size()==1:nth>0&&nth<=positions.size(),"independent anchor position");return positions.get(nth==0?0:nth-1);}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);var story=NpcDialogueTranslator.storyDialogues();
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("storyDialogues",story);flag("npcDialogues",new NpcDialogueTranslator());flag("chatNotifications",new ChatNotificationTranslator(db));
        var stream=TestStoryDialogues.class.getResourceAsStream("/data/witherskyru/story-dialogues.json");var rows=JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("records");check(rows.size()==476&&story.phraseCount()==10799,"476 retained story utterances plus83 Hub69,22 NPC70 and239 NPC74 and100 separately checked NPC75 additions plus137 NPC77 additions plus324 NPC78 additions");
        for(var raw:rows){var row=raw.getAsJsonObject();String source=row.get("source").getAsString(),target=row.get("translation").getAsString(),prefix="[NPC] "+row.get("speaker").getAsString()+": ";check(numbers(source).equals(numbers(target)),"source quantity preserved");
            var anchors=new ArrayList<String[]>();if(row.has("style_anchors"))for(var a:row.getAsJsonArray("style_anchors"))anchors.add(new String[]{a.getAsJsonObject().get("source").getAsString(),a.getAsJsonObject().get("target").getAsString(),Integer.toString(position(source,a.getAsJsonObject(),"source",prefix.length())),Integer.toString(position(target,a.getAsJsonObject(),"target",prefix.length()))});anchors.sort(Comparator.comparingInt(a->Integer.parseInt(a[2])));
            var input=Component.empty().append(Component.literal("[01:02:03] "+prefix).setStyle(PREFIX));int at=prefix.length();
            int termIndex=0;for(var a:anchors){int next=Integer.parseInt(a[2]);input.append(Component.literal(source.substring(at,next)).setStyle(BODY)).append(Component.literal(a[0]).setStyle(term(termIndex++)));at=next+a[0].length();}input.append(Component.literal(source.substring(at)).setStyle(BODY));
            var queued=message(input,GuiMessageSource.SYSTEM_SERVER);var result=SkyRuClient.npcChat(queued).content();check(result.getString().equals("[01:02:03] "+target),"actual dispatch "+source);span(result,"[01:02:03] "+prefix,PREFIX,0);termIndex=0;for(var a:anchors){int begin=11+Integer.parseInt(a[3]);var ss=styles(result);for(int n=begin;n<begin+a[1].length();n++)check(ss.get(n).equals(term(termIndex)),"indexed semantic style");termIndex++;}
            check(input.getString().equals("[01:02:03] "+source),"original immutable");for(int n=0;n<5;n++)check(story.translate(result,db)==result,"idempotent story");
            var repeated=input.copy().append(Component.literal(" (x17)").setStyle(NICK));var dedup=story.translate(repeated,db);check(dedup.getString().equals("[01:02:03] "+target+" (x17)"),"counter suffix: "+source+" => "+dedup.getString());span(dedup," (x17)",NICK,0);
            flag("translateGameChat",false);check(SkyRuClient.npcChat(queued).content().getString().equals(result.getString()),"NPC independent of game notices");flag("translateGameChat",true);flag("translateNpcChat",false);check(SkyRuClient.npcChat(queued)==queued,"NPC toggle");flag("translateNpcChat",true);
            for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var other=message(input,channel);check(SkyRuClient.npcChat(other)==other,"whole player/client message protected");}
            // Independent legacy-colour fixture, not a captured server packet.
            var legacy=Component.literal("§d[01:02:03] §b"+prefix+"§f"+source.substring(prefix.length()));check(story.translate(legacy,db).getString().equals("[01:02:03] "+target),"raw section formatting");
            Path custom=Path.of(args[0]).resolve(row.get("source_key").getAsString());Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),source+"\tМой разговор\n");var own=new OfflineDictionary(custom);own.setRecordUnknown(false);check(story.translate(input,own)==input,"whole override");for(var a:anchors){Files.writeString(custom.resolve("overrides.tsv"),a[0]+"\tМой термин\n");own=new OfflineDictionary(custom);own.setRecordUnknown(false);check(story.translate(input,own)==input,"semantic override");}
        }
        String[][] greetings={{"[NPC] Romero: It's you, ","!","[NPC] Romero: Это вы, ","!"},{"[NPC] Juliette: You're the best, ","!","[NPC] Juliette: Вы лучше всех, ","!"},{"[NPC] Romero: ","! You're the best person to turn the day into something memorable!","[NPC] Romero: ","! Вы поможете сделать этот день незабываемым!"},{"[NPC] Farmer Rigby: Howdy, ","!","[NPC] Farmer Rigby: Здравствуйте, ","!"}};
        int dynamic=0;
        for(var p:greetings)for(String nick:List.of("Wither_101","pickaxe","Z","ABC_123456789012"))for(String suffix:List.of(""," (x12)"," (×2)")){
            dynamic++;String source=p[0]+nick+p[1]+suffix,target=p[2]+nick+p[3]+suffix;var input=Component.empty().append(Component.literal("[01:02:03] "+p[0]).setStyle(PREFIX)).append(Component.literal(nick).setStyle(NICK)).append(Component.literal(p[1]+suffix).setStyle(BODY));
            var server=message(input,GuiMessageSource.SYSTEM_SERVER);var result=SkyRuClient.npcChat(server).content();check(result.getString().equals("[01:02:03] "+target),"nickname form "+source);span(result,nick,NICK,11);check(input.getString().equals("[01:02:03] "+source),"dynamic immutable");
            for(int n=0;n<5;n++)check(SkyRuClient.npcChat(message(result,GuiMessageSource.SYSTEM_SERVER)).content()==result,"dynamic repeat");
            for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var other=message(input,channel);check(SkyRuClient.npcChat(other)==other,"no nick/word exceptions for player chat");}
            var signed=new GuiMessage(0,input,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed protection");
            flag("translateNpcChat",false);check(SkyRuClient.npcChat(server)==server,"dynamic NPC toggle");flag("translateNpcChat",true);
            var ownPath=Path.of(args[0]).resolve("dynamic-"+dynamic);Files.createDirectories(ownPath);Files.writeString(ownPath.resolve("overrides.tsv"),source+"\tМоё обращение\n");var own=new OfflineDictionary(ownPath);own.setRecordUnknown(false);check(new ChatNotificationTranslator(own).translateNpcRequest(input,GuiMessageSource.SYSTEM_SERVER,false)==input,"dynamic override");
        }
        check(story.translate(Component.literal("[NPC] Romero: I was thinking of a bouquet of roses.")).getString().equals("[NPC] Romero: Для букета мне нужны маки."),"actual Minecraft poppy, not a made-up rose item");
        check(story.translate(Component.literal("[NPC] Romero: EXCEPT... Could you enchant it?")).getString().equals("[NPC] Romero: Только... можете принести зачарованный вариант?"),"enchanted ingredient variant, not an instruction to use the enchanting table");
        check(story.translate(Component.literal("[NPC] Lapis Miner: First you'll need to reach Lapis Lazuli Collection III, and then you can craft it for me.")).getString().equals("[NPC] Lapis Miner: Сначала достигните уровня III в коллекции лазурита. Тогда вы сможете создать для меня кирку."),"collection III, not Mining III or a made-up iron pickaxe");
        check(story.translate(Component.literal("[NPC] Farmhand: Reach Farming Skill Level V to unlock access to the Mushroom Desert.")).getString().equals("[NPC] Farmhand: Достигните уровня V в навыке фермерства, чтобы попасть в Грибную пустыню."),"Farming V, not Garden level V");
        check(story.translate(Component.literal("[NPC] Farmhand: Hold your Chicken Pet in your hand and right-click to put it in your Pets Menu!")).getString().equals("[NPC] Farmhand: Возьмите питомца «Курица» в руку и нажмите ПКМ, чтобы добавить его в меню питомцев!"),"right click registers held pet, not left click summons it");
        for(String text:List.of("[NPC] Romero: It's you, name with spaces!","[NPC] Romero: It's you, ABC_1234567890123!","[NPC] Unknown: It's you, Wither_101!","[NPC] Elle: Future unknown instruction.","pickaxe mining speed Wither_101")){var msg=message(Component.literal(text),GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(msg)==msg,"unknown text not guessed");}
        System.out.println("PASS: Story dialogue "+checks+" assertions; "+rows.size()+" functional lines, "+dynamic+" synthetic named variants; not full NPC scripts/live-tested");
    }
}
