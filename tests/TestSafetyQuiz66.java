import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Independent exact screenshot/log expectations, not answers read back from runtime tables. */
public final class TestSafetyQuiz66 {
    private static int checks;
    private record Pair(String source,String target) {}
    private record Case(String source,String target,List<Pair> edits,boolean npc) {}
    private static final List<Pair> QUESTIONS=List.of(
        new Pair("Why is account security important?","Почему важна безопасность аккаунта?"),
        new Pair("If someone in your party asks you to join their Discord to","Если участник группы зовёт вас в Discord,"),
        new Pair("voice chat, and sends the invite code in chat without the","чтобы поговорить, и присылает код приглашения без"),
        new Pair("\"discord.gg\", what should you do?","«discord.gg», что следует сделать?"),
        new Pair("Your friend sends you a link telling you to go vote for their","Друг присылает ссылку и просит проголосовать за него."),
        new Pair("prize, You click the link, and it's asking you to log in with an","Вы переходите по ней, а сайт требует войти"),
        new Pair("account. What should you do?","в аккаунт. Что следует сделать?"));
    private static final List<List<Pair>> ANSWERS=List.of(
        List.of(new Pair("It can lead you to lose a lot of items and money.","Можно потерять много предметов и денег."),new Pair("It can lead you to lose access to your Minecraft account.","Можно потерять доступ к аккаунту Minecraft."),new Pair("It can lead you to lose your Hypixel SkyBlock profile.","Можно потерять профиль Hypixel SkyBlock.")),
        List.of(new Pair("Manually type the invite link in the browser and join the discord.","Вручную ввести ссылку-приглашение в браузере и зайти в Discord."),new Pair("Leave the party and report the player.","Покинуть группу и пожаловаться на игрока."),new Pair("Add the player on discord and ask for the invite link there.","Добавить игрока в Discord и попросить ссылку там.")),
        List.of(new Pair("Register for an account and log in.","Создать аккаунт и войти."),new Pair("Use a linked account (Google, Facebook etc.) to log in.","Войти через связанный аккаунт (Google, Facebook и т. д.)."),new Pair("Look for signs of your friend getting hacked, and don't click on that website any further.","Проверить, не взломали ли друга, и больше ничего не нажимать на этом сайте.")));
    private static void check(boolean ok,String note){if(!ok)throw new AssertionError(note);checks++;}
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    private static String plain(Component c){var s=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,style,cp)->{s.appendCodePoint(cp);return true;});return s.toString();}
    private static List<Style> styles(Component c){var result=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)result.add(s);return true;});return result;}
    private static GuiMessage msg(Component c,GuiMessageSource channel){return new GuiMessage(0,c,null,channel,null);}
    private static Component queued(Component c){return SkyRuClient.npcChat(msg(c,GuiMessageSource.SYSTEM_SERVER)).content();}
    private static Style style(int seed,int span){return Style.EMPTY.withColor((seed*0x10203+span*0x30507)&0xffffff).withInsertion("fresh66/"+seed+"/"+span).withClickEvent(new ClickEvent.RunCommand("/original-answer-"+seed+"-"+span)).withHoverEvent(new HoverEvent.ShowText(Component.literal("original hover "+seed+"/"+span)));}
    private static Component fixture(String text,List<Pair> edits,int mode,int seed){
        var result=Component.empty();int cursor=0,span=0;
        boolean npc=text.contains("[NPC]");
        for(var edit:edits){int at=text.indexOf(edit.source(),cursor);if(at<0)throw new AssertionError("fixture missing span");add(result,text.substring(cursor,at),style(seed,npc?0:span++),mode);boolean anchor=Set.of("Charlotte","not Salad","Salad").contains(edit.source());add(result,edit.source(),style(seed,npc&&!anchor?0:++span),mode);cursor=at+edit.source().length();}
        add(result,text.substring(cursor),style(seed,npc?0:span),mode);return result;
    }
    private static void add(MutableComponent out,String text,Style s,int mode){
        if(text.isEmpty())return;
        if(mode==0)out.append(Component.literal(text).setStyle(s));
        else if(mode==1)for(int i=0;i<text.length();i++)out.append(Component.literal(text.substring(i,i+1)).setStyle(s));
        else out.append(Component.literal("§l"+text).setStyle(s));
    }
    private static void semantic(Case test,Component source,Component result,String prefix,String suffix){
        var before=styles(source);var after=styles(result);check(before.subList(0,prefix.length()).equals(after.subList(0,prefix.length())),"timestamp/padding styles copied");
        int from=prefix.length(),to=prefix.length();
        for(var edit:test.edits()){
            int a=plain(source).indexOf(edit.source(),from),b=plain(result).indexOf(edit.target(),to);
            check(a>=0&&b>=0,"expected independent translated span");
            check(before.subList(from,a).equals(after.subList(to,b)),"unchanged separators/icons/name styles/actions copied");
            for(int i=b;i<b+edit.target().length();i++)check(after.get(i).equals(before.get(a)),"translated span current color/click/hover/font/insertion");
            from=a+edit.source().length();to=b+edit.target().length();
        }
        check(before.subList(from,before.size()).equals(after.subList(to,after.size())),"suffix/digits copied with original per-glyph actions");
    }
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("npcDialogues",new NpcDialogueTranslator());flag("chatNotifications",new ChatNotificationTranslator(db));
        var cases=new ArrayList<Case>();for(var p:QUESTIONS)cases.add(new Case(p.source(),p.target(),List.of(p),false));
        for(String n:List.of("1","2","3")){var p=new Pair("Question #","Вопрос №");cases.add(new Case(p.source()+n,p.target()+n,List.of(p),false));}
        for(var group:ANSWERS){String a="\n    @ "+String.join(" \n    @ ",group.stream().map(Pair::source).toList()),b="\n    @ "+String.join(" \n    @ ",group.stream().map(Pair::target).toList());cases.add(new Case(a,b,group,false));for(var p:group)cases.add(new Case("@ "+p.source(),"@ "+p.target(),List.of(p),false));}
        for(int n=1;n<=3;n++)for(String phone:List.of("","✆ ")){String a=phone+String.join(" ",Collections.nCopies(n,"RING...")),b=phone+String.join(" ",Collections.nCopies(n,"ЗВОНОК..."));cases.add(new Case(a,b,List.of(new Pair(a.substring(phone.length()),b.substring(phone.length()))),false));}
        cases.add(new Case("[NPC] Brigette: Thanks, Salad!","[NPC] Brigette: Спасибо, Салат!",List.of(new Pair("Thanks, ","Спасибо, "),new Pair("Salad","Салат")),true));
        // Independent semantic anchors use not+Salad together, as in the red source emphasis.
        cases.add(new Case("[NPC] Charlotte: It's Charlotte, not Salad!!","[NPC] Charlotte: Меня зовут Шарлотта, а не Салат!!",List.of(new Pair("It's ","Меня зовут "),new Pair("Charlotte","Шарлотта"),new Pair(", ",", а "),new Pair("not Salad","не Салат")),true));
        for(var test:cases)for(int mode=0;mode<3;mode++)for(int seed=1;seed<=4;seed++)for(String suffix:test.npc()?List.of(""):List.of(""," ")){
            String prefix="[01:02:03] "+(test.npc()||test.source().startsWith("\n")?"":"    ");var original=fixture(prefix+test.source()+suffix,test.edits(),mode,seed);var result=original;
            for(int repeat=0;repeat<5;repeat++){result=queued(result);check(plain(result).equals(prefix+test.target()+suffix),"complete translation: "+plain(result));semantic(test,original,result,prefix,suffix);check(plain(original).equals(prefix+test.source()+suffix),"input immutable");}
            for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var m=msg(original,channel);check(SkyRuClient.npcChat(m)==m,"player/client never translated");}
            var signed=new GuiMessage(0,original,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed protection");
            flag(test.npc()?"translateNpcChat":"translateGameChat",false);check(queued(original)==original,"respective toggle");flag(test.npc()?"translateNpcChat":"translateGameChat",true);
            flag("enabled",false);check(queued(original)==original,"disabled");flag("enabled",true);
        }
        var capture=com.google.gson.JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("SAFETY_QUIZ_OBSERVED_0.15.66.json"))).getAsJsonObject().getAsJsonArray("messages");check(capture.size()==18,"all18 scoped source observations retained");
        for(var raw:capture){String observed=raw.getAsJsonObject().get("normalized_newlines").getAsString();var original=Component.literal(observed).setStyle(style(66,0));String text=plain(original),trimmed=text.stripTrailing();var test=cases.stream().filter(c->trimmed.endsWith(c.source())).max(Comparator.comparingInt(c->c.source().length())).orElseThrow();int at=trimmed.length()-test.source().length();String expected=text.substring(0,at)+test.target()+text.substring(trimmed.length());for(int repeat=0;repeat<5;repeat++)check(plain(queued(original)).equals(expected),"captured question/answers/NPC/rings with original padding");}
        for(var test:cases){var custom=root.resolve("custom-"+checks);Files.createDirectories(custom);String key=test.npc()?test.source():test.source().contains("RING...")?"RING...":test.edits().getFirst().source();Files.writeString(custom.resolve("overrides.tsv"),key+"\tМоя строка\n");var own=new OfflineDictionary(custom);own.setRecordUnknown(false);flag("dictionary",own);flag("chatNotifications",new ChatNotificationTranslator(own));var source=fixture(test.source(),test.edits(),0,99);check(queued(source)==source,"custom override veto: "+test.source()+" => "+plain(queued(source)));flag("dictionary",db);flag("chatNotifications",new ChatNotificationTranslator(db));}
        for(String text:List.of("Question #4","Question #20","Question #2 UNSEEN","@ Leave the party and report the player. UNKNOWN","RING... RING... RING... RING...","RING... USER MESSAGE","[NPC] Other: It's Charlotte, not Salad!!","\n    @ Leave the party and report the player. \n    @ UNKNOWN")){var c=Component.literal(text);check(queued(c)==c,"unknown shape not guessed");}
        System.out.println("PASS: safety quiz66 "+checks+" assertions;3 question headings/7 complete question rows/9 answers/3 bundles/2 NPC lines/6 ring forms,3 layouts/4 fresh actions/5 repeats,padding/times/numbers/clicks/hover/source/player/toggles/overrides;not live packets or all alternate quizzes");
    }
}
