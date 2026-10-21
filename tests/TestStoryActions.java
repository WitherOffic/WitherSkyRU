import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Authored synthetic interactive components, not network submission/live NPC screenshots. */
public class TestStoryActions {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xffffff);
    private static final Style A=BODY.withColor(0x55ff55).withClickEvent(new ClickEvent.RunCommand("/original-choice-one"));
    private static final Style B=BODY.withColor(0xff5555).withClickEvent(new ClickEvent.RunCommand("/original-choice-two"));
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    private static List<Style> styles(Component c){var ss=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)ss.add(s);return true;});return ss;}
    private static void span(Component c,String text,Style wanted){int at=c.getString().lastIndexOf(text);check(at>=0,"missing "+text);var ss=styles(c);for(int i=at;i<at+text.length();i++)check(ss.get(i).equals(wanted),"changed original action/style "+text);}
    private static GuiMessage msg(Component c,GuiMessageSource channel){return new GuiMessage(0,c,null,channel,null);}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);var story=NpcDialogueTranslator.storyDialogues();flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("storyDialogues",story);flag("chatNotifications",new ChatNotificationTranslator(db));
        var stream=TestStoryActions.class.getResourceAsStream("/data/witherskyru/story-dialogues.json");var rows=JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("records");var recovery=java.util.stream.StreamSupport.stream(rows.spliterator(),false).map(JsonElement::getAsJsonObject).filter(r->r.get("source_key").getAsString().equals("story.6.1")).findFirst().orElseThrow();
        String source=recovery.get("source").getAsString(),target=recovery.get("translation").getAsString();int click=source.indexOf("[CLICK]",15);
        for(int repeat=0;repeat<5;repeat++)for(boolean split:List.of(false,true)){
            var input=Component.empty().append(Component.literal(source.substring(0,click)).setStyle(BODY));
            if(split)input.append(Component.literal("[").setStyle(A)).append(Component.literal("CLICK").setStyle(B)).append(Component.literal("]").setStyle(A));else input.append(Component.literal("[CLICK]").setStyle(A));
            var result=SkyRuClient.npcChat(msg(input,GuiMessageSource.SYSTEM_SERVER)).content();check(result.getString().equals(target),"merged/separate action anchors");span(result,"ВОССТАНОВИТЬ",split?B:A);span(result,"[",A);span(result,"]",A);check(input.getString().equals(source),"source recovery immutable");check(story.translate(result,db)==result,"recovery repeat");
            var contaminated=Component.empty().append(Component.literal(source.substring(0,source.indexOf("expenses"))).setStyle(BODY)).append(Component.literal("expenses").setStyle(B)).append(Component.literal(source.substring(source.indexOf("expenses")+8,click)).setStyle(BODY)).append(Component.literal("[CLICK]").setStyle(A));
            check(story.translate(contaminated,db)==contaminated,"unmapped coloured/action word remains original");
        }
        int forms=0;
        for(String[] pair:new String[][]{{"NO","YES","НЕТ","ДА"},{"BARBARIANS","MAGES","ВАРВАРЫ","МАГИ"}})for(String gap:List.of(""," ","  "))for(boolean decorated:List.of(false,true)){
            forms++;String before=decorated?"[01:02:03]   ":"",left="Select an option:"+gap,right="["+pair[0]+"]"+gap+"["+pair[1]+"]";
            var input=Component.empty().append(Component.literal(before+left+"[").setStyle(BODY)).append(Component.literal(pair[0]).setStyle(A)).append(Component.literal("]"+gap+"[").setStyle(BODY)).append(Component.literal(pair[1]).setStyle(B)).append(Component.literal("]").setStyle(BODY));
            var server=msg(input,GuiMessageSource.SYSTEM_SERVER);var result=SkyRuClient.npcChat(server).content();check(result.getString().equals(before+(pair[0].equals("NO")?"Выберите ответ:":"Выберите фракцию:")+gap+"["+pair[2]+"]"+gap+"["+pair[3]+"]"),"both story choices");span(result,pair[2],A);span(result,pair[3],B);check(input.getString().equals(before+left+right),"source choices immutable");
            for(int n=0;n<5;n++)check(SkyRuClient.npcChat(msg(result,GuiMessageSource.SYSTEM_SERVER)).content()==result,"choice repeat");
            for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var other=msg(input,channel);check(SkyRuClient.npcChat(other)==other,"whole user/client choice protected");}
            var signed=new GuiMessage(0,input,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed choice protected");
            flag("translateGameChat",false);check(SkyRuClient.npcChat(server)==server,"game-notification toggle");flag("translateGameChat",true);flag("translateNpcChat",false);check(SkyRuClient.npcChat(server).content().getString().equals(result.getString()),"choice independent of NPC toggle");flag("translateNpcChat",true);
            var ownPath=Path.of(args[0]).resolve("choice-"+forms);Files.createDirectories(ownPath);Files.writeString(ownPath.resolve("overrides.tsv"),left+right+"\tМой ответ\n");var own=new OfflineDictionary(ownPath);own.setRecordUnknown(false);check(new ChatNotificationTranslator(own).translate(input,GuiMessageSource.SYSTEM_SERVER,false)==input,"full choice override");Files.writeString(ownPath.resolve("overrides.tsv"),pair[0]+"\tМоя кнопка\n");own=new OfflineDictionary(ownPath);own.setRecordUnknown(false);check(new ChatNotificationTranslator(own).translate(input,GuiMessageSource.SYSTEM_SERVER,false)==input,"button override");
        }
        // YES/NO is now an independently sourced Kaus/Rollim layout checked by TestNpcChoices84.
        for(String text:List.of("Select an option: [CUSTOM] [YES]","Select an option: [YES] [MAYBE]","[NPC] Unknown: Select an option: [NO] [YES]","[VIP] Wither_101: Select an option: [NO] [YES]")){var input=msg(Component.literal(text),GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(input)==input,"unknown choice not guessed");}
        System.out.println("PASS: Story actions "+checks+" assertions; recovery merged/split semantic anchors, "+forms+" synthetic choice layouts; actions retained, not executed/live-tested");
    }
}
