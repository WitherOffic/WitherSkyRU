import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;
import net.minecraft.network.chat.contents.*;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;

/** Frozen observed safety branch only; original commands/icons tested without executing them. */
public class TestSafetyScreenshot {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xffffff);
    private static final Style PREFIX=Style.EMPTY.withColor(0x55ffff);
    private static final String[][][] CHOICES={
        {new String[]{"Absolutely!","Tell me more about the Safety Squad.","Not right now."},new String[]{"Конечно!","Расскажите об отряде безопасности.","Не сейчас."}},
        {new String[]{"Sounds good with me!","Give me a moment."},new String[]{"Согласен!","Дайте мне минуту."}},
        {new String[]{"Who, me?"},new String[]{"Кто, я?"}},
        {new String[]{"What is a safety-related incident anyway?"},new String[]{"Что вы называете опасным происшествием?"}},
        {new String[]{"What's the point of Account Safety?"},new String[]{"Зачем нужна безопасность аккаунта?"}},
        {new String[]{"I don't want to lose my account!"},new String[]{"Я не хочу потерять свой аккаунт!"}},
        {new String[]{"That doesn't sound good."},new String[]{"Звучит неприятно."}},
        {new String[]{"Stranger danger, understood."},new String[]{"Незнакомцам не доверять. Понял."}},
        {new String[]{"I'm not stupid enough to open a random link!"},new String[]{"Я не настолько глуп, чтобы открыть случайную ссылку!"}},
        {new String[]{"C'mon, I'm sure it's not thaaat complicated."},new String[]{"Да ладно, не может быть всё настолько сложно."}},
        {new String[]{"Brigette, you're quite scary."},new String[]{"Brigette, вы меня пугаете."}},
        {new String[]{"The /report command. Noted."},new String[]{"Команда /report. Запомнил."}},
        {new String[]{"Links for SkyBlock outside SkyBlock? Like what?"},new String[]{"Ссылки на SkyBlock вне игры? Например?"}},
        {new String[]{"I will be careful when clicking on links, I promise!"},new String[]{"Обещаю осторожно переходить по ссылкам!"}},
        {new String[]{"I'm good, thanks."},new String[]{"Я в порядке, спасибо."}},
        {new String[]{"Hello, Jimmy."},new String[]{"Привет, Jimmy."}},
        {new String[]{"Security is indeed very important."},new String[]{"Безопасность и правда очень важна."}},
        {new String[]{"Cool."},new String[]{"Здорово."}},
        {new String[]{"Deal."},new String[]{"Договорились."}}
    };
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    private static void eq(String a,String b){check(a.equals(b),a+" != "+b);}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String plain(Component c){var s=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,st,p)->{s.appendCodePoint(p);return true;});return s.toString();}
    private static List<Style> styles(Component c){var s=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,st,p)->{for(int n=0;n<Character.charCount(p);n++)s.add(st);return true;});return s;}
    private static GuiMessage msg(Component c,GuiMessageSource source){return new GuiMessage(0,c,null,source,null);}
    private static Component queued(Component c){return SkyRuClient.npcChat(msg(c,GuiMessageSource.SYSTEM_SERVER)).content();}
    private static void retain(Component a,Component b,String token){
        int old=plain(a).lastIndexOf(token),now=plain(b).lastIndexOf(token);check(old>=0&&now>=0,"missing retained "+token);var x=styles(a);var y=styles(b);
        for(int i=0;i<token.length();i++)check(x.get(old+i).equals(y.get(now+i)),"retained styles/actions "+token);
    }
    private static void span(Component c,String token,Style wanted){int at=plain(c).indexOf(token);check(at>=0,"missing button");var s=styles(c);for(int i=at;i<at+token.length();i++)check(s.get(i).equals(wanted),"button action lost");}
    private static void guards(Component c){
        for(var ch:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var m=msg(c,ch);check(SkyRuClient.npcChat(m)==m,"whole player/client text unchanged");}
        var signed=new GuiMessage(0,c,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed protection");
    }
    private static Component digits(String value){var c=Component.empty();for(int i=0;i<value.length();i++)c.append(Component.literal(value.substring(i,i+1)).setStyle(BODY.withColor(0x220000+i*0x1000).withInsertion("digit "+i)));return c;}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);
        flag("npcDialogues",new NpcDialogueTranslator());flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("chatNotifications",new ChatNotificationTranslator(db));
        var records=JsonParser.parseReader(new java.io.InputStreamReader(TestSafetyScreenshot.class.getResourceAsStream("/data/witherskyru/story-dialogues.json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("records");
        var translations=new HashMap<String,String>();for(var raw:records){var r=raw.getAsJsonObject();translations.put(r.get("source").getAsString(),r.get("translation").getAsString());}
        var observed=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("SAFETY_OBSERVED_0.15.46.json"))).getAsJsonObject().getAsJsonArray("messages");check(observed.size()==101,"frozen observed count");
        for(var raw:observed){
            String text=raw.getAsString();Component original=Component.literal(text);String visible=plain(original),body=visible.substring(11),base=body.replaceFirst(" \\((?:x|×)[0-9]+\\)$","");String suffix=body.substring(base.length()),target=translations.get(base);
            if(target==null&&base.startsWith("Select an option:"))for(var p:CHOICES){String en="Select an option: "+String.join(" ",Arrays.stream(p[0]).map(s->"["+s+"]").toList());if(base.stripTrailing().equals(en))target="Выберите ответ: "+String.join(" ",Arrays.stream(p[1]).map(s->"["+s+"]").toList())+base.substring(base.stripTrailing().length());}
            if(base.equals("[NPC] Kat: You can pick it up in 29 minutes 55 seconds."))target="[NPC] Kat: Питомца можно забрать через 29 мин 55 с.";
            if(base.equals("[NPC] Susan: Awesome! You're our 286218th volunteer!"))target="[NPC] Susan: Отлично! Вы наш доброволец №286218!";
            if(base.startsWith("[NPC] Brigette: Sorry [Wither_101 head]Wither_101,"))target="[NPC] Brigette: Извините, [Wither_101 head]Wither_101, я быстро принесу стул и попрошу кого-нибудь убрать этот беспорядок.";
            check(target!=null,"unreviewed observed message: "+base);
            var result=queued(original);eq(plain(result),visible.substring(0,11)+target+suffix);guards(original);
            for(int n=0;n<5;n++){check(queued(result)==result,"observed idempotence");result=queued(original);eq(plain(result),visible.substring(0,11)+target+suffix);}
        }
        for(var p:CHOICES)for(String gap:List.of(""," ","  "))for(String tail:List.of(""," ")){
            var c=Component.literal("[01:02:03] Select an option:"+gap).setStyle(BODY);var buttonStyles=new ArrayList<Style>();
            for(int i=0;i<p[0].length;i++){var st=BODY.withColor(0x110000+i*0x111111).withClickEvent(new ClickEvent.RunCommand("/original-choice-"+i)).withHoverEvent(new HoverEvent.ShowText(Component.literal("keep original hover")));buttonStyles.add(st);if(i>0)c.append(Component.literal(gap).setStyle(BODY));c.append(Component.literal("[").setStyle(BODY)).append(Component.literal(p[0][i]).setStyle(st)).append(Component.literal("]").setStyle(BODY));}c.append(Component.literal(tail).setStyle(BODY));
            var result=queued(c);eq(plain(result),"[01:02:03] Выберите ответ:"+gap+String.join(gap,Arrays.stream(p[1]).map(s->"["+s+"]").toList())+tail);
            for(int i=0;i<p[1].length;i++)span(result,p[1][i],buttonStyles.get(i));guards(c);
        }
        for(String m:List.of("0","1","29","9999"))for(String s:List.of("0","1","55","59")){
            var c=Component.literal("[NPC] Kat: You can pick it up in ").setStyle(BODY).append(digits(m)).append(Component.literal(m.equals("1")?" minute ":" minutes ").setStyle(BODY)).append(digits(s)).append(Component.literal(s.equals("1")?" second.":" seconds.").setStyle(BODY));
            var result=queued(c);eq(plain(result),"[NPC] Kat: Питомца можно забрать через "+m+" мин "+s+" с.");retain(c,result,m);guards(c);
            // Check second's per-character style even if both values have equal spellings.
            int old=plain(c).lastIndexOf(s),now=plain(result).lastIndexOf(s);var x=styles(c);var y=styles(result);for(int i=0;i<s.length();i++)check(x.get(old+i).equals(y.get(now+i)),"seconds not restyled");
        }
        for(String rank:List.of("1","2","3","286218","999999999999"))for(String ending:List.of("st","nd","rd","th")){
            var c=Component.literal("[NPC] Susan: Awesome! You're our ").setStyle(BODY).append(digits(rank)).append(Component.literal(ending).setStyle(PREFIX)).append(Component.literal(" volunteer!").setStyle(BODY));
            var result=queued(c);eq(plain(result),"[NPC] Susan: Отлично! Вы наш доброволец №"+rank+"!");retain(c,result,rank);guards(c);
        }
        for(String nick:List.of("Wither_101","pickaxe","Z","ABC_123456789012"))for(boolean icon:List.of(false,true)){
            var c=Component.literal("[NPC] Brigette: Sorry ").setStyle(BODY);
            if(icon){var obj=new ObjectContents(new AtlasSprite(Identifier.parse("minecraft:gui"),Identifier.parse("minecraft:icon")),Optional.of(Component.literal("["+nick+" head]")));c.append(MutableComponent.create(obj).setStyle(PREFIX));}
            c.append(Component.literal(nick).setStyle(PREFIX.withClickEvent(new ClickEvent.SuggestCommand("/msg "+nick+" ")))).append(Component.literal(", I'll go grab a chair real quick, and get someone to clean up this mess.").setStyle(BODY));
            var result=queued(c);check(plain(result).contains("Извините, "),"nickname sentence");retain(c,result,nick);guards(c);
            if(icon){check(result.getSiblings().stream().anyMatch(n->n.getContents() instanceof ObjectContents),"head object retained");}
        }
        for(String prefix:List.of("","Objective: ","Цель: ")){
            var c=Component.literal(prefix+"Take a seat in the ").setStyle(BODY).append(Component.literal("Briefing Room").setStyle(PREFIX));var result=SkyRuClient.component(c);
            eq(plain(result),(prefix.isEmpty()?"":"Цель: ")+"Займите место в комнате инструктажа");span(result,"комнате инструктажа",PREFIX);eq(plain(SkyRuClient.component(result)),plain(result));
        }
        var shortGoal=Component.literal("Take a seat in the ").setStyle(BODY).append(Component.literal("Briefin").setStyle(PREFIX));eq(plain(SkyRuClient.component(shortGoal)),"Займите место в комнате инструктажа");
        for(String text:List.of("[NPC] Kat: You can pick it up in 10 minutes 60 seconds.","[NPC] Other: You can pick it up in 29 minutes 55 seconds.","Select an option: [Who, me?] [CUSTOM]","Select an option: [CUSTOM]","[VIP] Wither_101: Select an option: [Who, me?]")){
            var c=Component.literal(text);check(queued(c)==c,"unknown shape protected");guards(c);
        }
        var timer=Component.literal("[NPC] Kat: You can pick it up in 29 minutes 55 seconds.");flag("translateNpcChat",false);check(queued(timer)==timer,"NPC toggle");flag("translateNpcChat",true);
        var option=Component.literal("Select an option: [Who, me?] ");flag("translateGameChat",false);check(queued(option)==option,"option notification toggle");flag("translateGameChat",true);
        for(String key:List.of("Who, me?","Select an option: [Who, me?]","Safety Squad")){
            var dir=Path.of(args[0]).resolve("custom-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой текст\n");flag("dictionary",new OfflineDictionary(dir));flag("chatNotifications",new ChatNotificationTranslator(new OfflineDictionary(dir)));check(queued(option)==option,"custom choice override");
        }
        flag("dictionary",db);flag("chatNotifications",new ChatNotificationTranslator(db));flag("enabled",false);check(queued(timer)==timer,"disabled");flag("enabled",true);
        System.out.println("PASS: safety screenshots "+checks+" assertions;101 actual legacy messages/19 closed choice layouts/3 dynamic shapes/heads/nicknames/goals/commands/toggles/overrides; partial branch, no live game test");
    }
}
