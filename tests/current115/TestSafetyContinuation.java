import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;
import net.minecraft.network.chat.contents.*;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;

/** Recorded continuation only, not unvisited quest answers or GUI quiz inventory. */
public class TestSafetyContinuation {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xffffff);
    private static final Style NAME=Style.EMPTY.withColor(0x55ffff).withClickEvent(new ClickEvent.SuggestCommand("/msg Wither_101 "));
    private static final String[][] CHOICES={
        {"I volunteered to be part of the Safety Squad.","Я добровольно вступил в отряд безопасности."},
        {"Wait, what?","Подождите, что?"},
        {"What just happened?","Что сейчас произошло?"},
        {"Yeah.","Да."},
        {"How does this scam work?","Как устроена эта афера?"},
        {"Yikes.","Жуть."},
        {"It was quite a hectic day.","День выдался очень насыщенным."},
        {"Okay!","Хорошо!"}
    };
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
    private static void eq(String a,String b){check(a.equals(b),a+" != "+b);}
    private static void flag(String k,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(k);f.setAccessible(true);f.set(null,v);}
    private static String plain(Component c){var out=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{out.appendCodePoint(p);return true;});return out.toString();}
    private static GuiMessage msg(Component c,GuiMessageSource s){return new GuiMessage(0,c,null,s,null);}
    private static Component queued(Component c){return SkyRuClient.npcChat(msg(c,GuiMessageSource.SYSTEM_SERVER)).content();}
    private static void guards(Component c){
        for(var ch:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var m=msg(c,ch);check(SkyRuClient.npcChat(m)==m,"whole user/client message protection");}
        var signed=new GuiMessage(0,c,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed protection");
    }
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);flag("npcDialogues",new NpcDialogueTranslator());flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("chatNotifications",new ChatNotificationTranslator(db));
        var translations=new HashMap<String,String>();
        for(String resource:List.of("story-dialogues.json","quest-objectives.json")){
            var stream=TestSafetyContinuation.class.getResourceAsStream("/data/witherskyru/"+resource);
            var rows=JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("records");
            for(var raw:rows){var r=raw.getAsJsonObject();translations.put(r.get("source").getAsString(),r.get("translation").getAsString());}
        }
        var observed=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("SAFETY_CONTINUATION_OBSERVED_0.15.47.json"))).getAsJsonObject().getAsJsonArray("messages");check(observed.size()==68,"observed count");
        for(var raw:observed){
            var original=Component.literal(raw.getAsString());String visible=plain(original),body=visible.substring(11),leading=body.substring(0,body.length()-body.stripLeading().length()),core=body.stripLeading();String target=translations.get(core);
            if(core.equals("[NPC] Brigette: ..."))target=core;
            for(var p:CHOICES)if(core.stripTrailing().equals("Select an option: ["+p[0]+"]"))target="Выберите ответ: ["+p[1]+"]"+core.substring(core.stripTrailing().length());
            if(core.equals("[NPC] Elizabeth: Hey [Wither_101 head]Wither_101!"))target="[NPC] Elizabeth: Привет, [Wither_101 head]Wither_101!";
            check(target!=null,"unreviewed captured message: "+core);
            var result=queued(original);eq(plain(result),visible.substring(0,11)+leading+target);guards(original);
            for(int pass=0;pass<5;pass++){check(queued(result)==result,"idempotence");result=queued(original);eq(plain(result),visible.substring(0,11)+leading+target);}
        }
        for(var p:CHOICES)for(String tail:List.of(""," ")){
            var button=BODY.withColor(0x55ff55).withClickEvent(new ClickEvent.RunCommand("/original-quiz-answer")).withHoverEvent(new HoverEvent.ShowText(Component.literal("original hover")));
            var original=Component.literal("Select an option: [").setStyle(BODY).append(Component.literal(p[0]).setStyle(button)).append(Component.literal("]"+tail).setStyle(BODY));var result=queued(original);
            eq(plain(result),"Выберите ответ: ["+p[1]+"]"+tail);int at=plain(result).indexOf(p[1]);var ss=styles(result);for(int i=0;i<p[1].length();i++)check(ss.get(at+i).equals(button),"button commands/hover retained");guards(original);
            Path custom=Path.of(args[0]).resolve("choice-"+checks);Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),p[0]+"\tМой ответ\n");var own=new ChatNotificationTranslator(new OfflineDictionary(custom));check(own.translate(original,GuiMessageSource.SYSTEM_SERVER,false)==original,"custom choice");
        }
        for(String nick:List.of("Wither_101","pickaxe","Z","ABC_123456789012"))for(boolean head:List.of(false,true)){
            var original=Component.literal("[NPC] Elizabeth: Hey ").setStyle(BODY);
            if(head){var object=new ObjectContents(new AtlasSprite(Identifier.parse("minecraft:gui"),Identifier.parse("minecraft:icon")),Optional.of(Component.literal("["+nick+" head]")));original.append(MutableComponent.create(object).setStyle(NAME));}
            original.append(Component.literal(nick).setStyle(NAME)).append(Component.literal("!").setStyle(BODY));var result=queued(original);
            eq(plain(result),"[NPC] Elizabeth: Привет, "+(head?"\uFFFC":"")+nick+"!");int at=plain(result).lastIndexOf(nick);var ss=styles(result);for(int i=0;i<nick.length();i++)check(ss.get(at+i).equals(NAME),"real name action/style");guards(original);if(head)check(result.getSiblings().stream().anyMatch(n->n.getContents() instanceof ObjectContents),"head preserved");
        }
        for(String[] p:new String[][]{{"Talk to the Security Clerk","Поговорите со служащим безопасности","Security Clerk","служащим безопасности"},{"Talk to Elizabeth","Поговорите с Элизабет","Elizabeth","Элизабет"}}){
            for(String prefix:List.of("","Objective: ","Цель: ")){
                int at=p[0].indexOf(p[2]);var original=Component.literal(prefix+p[0].substring(0,at)).setStyle(BODY).append(Component.literal(p[2]).setStyle(NAME));var result=SkyRuClient.component(original);eq(plain(result),(prefix.isEmpty()?"":"Цель: ")+p[1]);int now=plain(result).indexOf(p[3]);var ss=styles(result);for(int i=0;i<p[3].length();i++)check(ss.get(now+i).equals(NAME),"goal term style");guards(original);
            }
        }
        for(String source:List.of("[NPC] Elizabeth: Hey [Other head]Wither_101!","[NPC] Other: Hey Wither_101!","Select an option: [Okay!] [UNKNOWN]","Select an option: [Different quiz answer]")){
            var c=Component.literal(source);check(queued(c)==c,"unknown not inferred");guards(c);
        }
        var receipt=new ChatNotificationTranslator(db);
        for(String text:List.of("[Sacks] +123 items. (Last 5s.)","You reached a 10 Kill Combo!","Select an option: [Okay!]","Кошелёк: 555,222","pickaxe Wither_101")){
            var c=Component.literal(text);check(receipt.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"non-NPC fast exit");
            var player=msg(c,GuiMessageSource.PLAYER);check(SkyRuClient.npcChat(player)==player,"all player notices unchanged");
            var signed=new GuiMessage(0,c,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed non-NPC notice unchanged");
        }
        var notice=Component.literal("You reached a 10 Kill Combo!");var translatedNotice=receipt.translate(notice,GuiMessageSource.SYSTEM_SERVER,false);
        check(translatedNotice!=notice&&translatedNotice.getString().contains("10")&&!translatedNotice.getString().contains("You reached"),"notification route still works");
        var hello=Component.literal("[NPC] Elizabeth: Hey Wither_101!");flag("translateNpcChat",false);check(queued(hello)==hello,"NPC toggle");flag("translateNpcChat",true);
        Path dir=Path.of(args[0]).resolve("greeting-override");Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),hello.getString()+"\tМой текст\n");var own=new ChatNotificationTranslator(new OfflineDictionary(dir));check(own.translateNpcRequest(hello,GuiMessageSource.SYSTEM_SERVER,false)==hello,"full greeting override");
        flag("enabled",false);check(queued(hello)==hello,"disabled");flag("enabled",true);
        System.out.println("PASS: safety continuation "+checks+" assertions;68 actual messages through reward/8 answers/2 goals/current actions and head; no unvisited branches, quiz GUI or user form edits");
    }
}
