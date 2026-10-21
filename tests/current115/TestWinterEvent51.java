import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import ru.wither.skyru.*;

/** Bounded actual winter log replay plus independent human targets; no game/actions executed. */
public final class TestWinterEvent51 {
    private static int checks;
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String clean(String s){return s.replaceAll("(?i)§[0-9a-fk-or]","");}
    private static String plain(Component c){return AuditMiningStyles.plain(c.getVisualOrderText());}
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();c.getVisualOrderText().accept((i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static void span(Component from,String old,Component to,String next){
        int a=plain(from).indexOf(old),b=plain(to).indexOf(next);check(a>=0&&b>=0,"missing preserved span "+old);var ss=styles(from);var tt=styles(to);
        if(old.equals(next)){for(int i=0;i<old.length();i++)check(ss.get(a+i).equals(tt.get(b+i)),"retained span styles "+old);}
        else check(ss.get(a).equals(tt.get(b)),"translated semantic style "+old);
    }
    private static Component row(String legacy,int mode,int seed){
        var inherited=Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/unchanged-winter-"+seed)).withHoverEvent(new HoverEvent.ShowText(Component.literal("original hint "+seed))).withInsertion("original "+seed);
        var parsed=Component.literal(legacy).setStyle(inherited);if(mode==0)return parsed;
        String text=plain(parsed);var ss=styles(parsed);var out=Component.empty();
        Style current=null;var run=new StringBuilder();
        for(int i=0;i<text.length();){int cp=text.codePointAt(i);Style style=ss.get(i);if(Character.isDigit(cp)||text.charAt(i)=='_')style=style.withColor(0x120000+i*0x101);if(current!=null&&!current.equals(style)){out.append(Component.literal(run.toString()).setStyle(current));run.setLength(0);}current=style;run.appendCodePoint(cp);i+=Character.charCount(cp);}
        if(!run.isEmpty())out.append(Component.literal(run.toString()).setStyle(current));
        return out;
    }
    private static GuiMessage msg(Component c,GuiMessageSource s){return new GuiMessage(7,c,null,s,null);}
    private static Component server(Component c,String expected){
        var message=msg(c,GuiMessageSource.SYSTEM_SERVER);var translated=SkyRuClient.npcChat(message);check(plain(translated.content()).equals(expected),plain(c)+" => "+plain(translated.content()));check(translated.addedTime()==7,"original added time");
        for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var protectedMessage=msg(c,channel);check(SkyRuClient.npcChat(protectedMessage)==protectedMessage,"non-server original identity");}
        var signed=new GuiMessage(7,c,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed identity");
        for(int n=0;n<5;n++)check(SkyRuClient.npcChat(msg(translated.content(),GuiMessageSource.SYSTEM_SERVER)).content()==translated.content(),"repeated display identity");return translated.content();
    }
    // Independent target oracle, not loaded from the production resource.
    private static final String[][] UTTERANCES={
        {"St. Jerry","You may have noticed some Gifts laying around.","Ты, наверное, уже заметил подарки вокруг.","Gifts","подарки"},
        {"St. Jerry","Each SkyBlock year, I place them throughout the village to spread joy and good tidings.","Каждый год в SkyBlock я раскладываю их по всей деревне, чтобы дарить радость и праздничное настроение.","SkyBlock","SkyBlock"},
        {"St. Jerry","Find them all and come back to me!","Найди их все и возвращайся ко мне!"},
        {"St. Jerry","Find all of the Gifts around the village and come back to me!","Найди все подарки в деревне и возвращайся ко мне!","Gifts","подарки"},
        {"Frosty","Welcome to Jerry's Workshop!","Добро пожаловать в Мастерскую Джерри!","Jerry's Workshop","Мастерскую Джерри"},
        {"Frosty","Here the Jerrys make and deliver Gifts to all of SkyBlock!","Здесь Джерри делают подарки и доставляют их по всему SkyBlock!","Jerrys","Джерри","Gifts","подарки","SkyBlock","SkyBlock"},
        {"Frosty","When it's time to deliver Gifts, Mount Jerry always erupts and the village is ravaged by heat-based enemies!","Когда приходит время раздавать подарки, вулкан Джерри извергается, а деревню атакуют огненные враги!","Gifts","подарки","Mount Jerry","вулкан Джерри"},
        {"Frosty","You'll need a Frosty the Snow Cannon to aid you in your defense against the Magma Grinches.","Тебе понадобится снежная пушка Фрости, чтобы защититься от магмовых Гринчей.","Frosty the Snow Cannon","снежная пушка Фрости","Magma Grinches","магмовых Гринчей"},
        {"Frosty","It holds and fires Snowballs, which are the best way the extinguish the enemy!","Она хранит снежки и стреляет ими — это лучший способ потушить врагов!","Snowballs","снежки"},
        {"Frosty","I can refill it at any time for a modest price!","Я могу пополнить её в любое время за небольшую плату!"}
    };
    private static String target(String visible){
        String time=visible.matches("^\\[[0-9:]{8}\\] .*" )?visible.substring(0,11):"";String body=visible.substring(time.length());
        if(body.startsWith("[NPC] St. Jerry: 'Tis the Season of Jerry, "))return time+body.replace("'Tis the Season of Jerry","Наступил сезон Джерри");
        for(var r:UTTERANCES){String prefix="[NPC] "+r[0]+": ";String source=prefix+r[1];if(body.equals(source)||body.matches(java.util.regex.Pattern.quote(source)+" \\((?:x|×)[0-9]{1,6}\\)"))return time+prefix+r[2]+body.substring(source.length());}
        throw new AssertionError("unreviewed observed text "+visible);
    }
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);var winter=NpcDialogueTranslator.winterDialogues();check(winter.phraseCount()==10,"ten exact utterances");
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);flag("winterDialogues",winter);flag("chatNotifications",new ChatNotificationTranslator(db));
        var observed=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("WINTER_OBSERVED_0.15.51.json"))).getAsJsonObject().getAsJsonArray("records");check(observed.size()==15,"15 actual observed receipts");
        for(var raw:observed)for(int mode=0;mode<2;mode++)for(int seed=0;seed<5;seed++){
            String legacy=raw.getAsJsonObject().get("source_legacy").getAsString();var c=row(legacy,mode,seed);String old=plain(c);var translated=server(c,target(old));check(plain(c).equals(old),"source immutable");
            String prefix=old.substring(0,old.indexOf(": ",11)+2);span(c,prefix,translated,prefix);
            for(var r:UTTERANCES)if(old.contains("[NPC] "+r[0]+": "+r[1]))for(int n=3;n<r.length;n+=2)span(c,r[n],translated,r[n+1]);
            int counter=old.lastIndexOf(" (x");if(counter>=0)span(c,old.substring(counter),translated,old.substring(counter));
            if(old.contains("Wither_101"))span(c,"Wither_101",translated,"Wither_101");
        }
        for(var r:UTTERANCES)for(String suffix:List.of(""," (x1)"," (×999999)"))for(int seed=0;seed<5;seed++){
            String prefix="[NPC] "+r[0]+": ";var c=row(prefix+r[1]+suffix,1,seed);var translated=server(c,prefix+r[2]+suffix);span(c,prefix,translated,prefix);if(!suffix.isEmpty())span(c,suffix,translated,suffix);
            for(int n=3;n<r.length;n+=2)span(c,r[n],translated,r[n+1]);
        }
        for(String name:List.of("A","Test_001","Abcdefghijklmnop"))for(String suffix:List.of(""," (x5)"," (×12345)")){
            String prefix="[02:38:32] [NPC] St. Jerry: ";var c=row(prefix+"'Tis the Season of Jerry, "+name+"!"+suffix,1,9);var translated=server(c,prefix+"Наступил сезон Джерри, "+name+"!"+suffix);span(c,name,translated,name);if(!suffix.isEmpty())span(c,suffix,translated,suffix);
        }
        for(String bad:List.of("[NPC] Unknown: Welcome to Jerry's Workshop!","[NPC] Frosty: UNKNOWN","[NPC] St. Jerry: 'Tis the Season of Jerry, a-b!","[NPC] St. Jerry: 'Tis the Season of Jerry, Abcdefghijklmnopq!","[NPC] Frosty: Welcome to Jerry's Workshop! (x1000000)")){
            var m=msg(Component.literal(bad),GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(m)==m,"unknown grammar identity");
        }
        var excessive=Component.empty();String excessiveText="[02:38:57] [NPC] Frosty: "+UTTERANCES[6][1];check(excessiveText.length()>128,"fixture exceeds bounded node count");for(char c:excessiveText.toCharArray())excessive.append(Component.literal(String.valueOf(c)));check(winter.translate(excessive)==excessive,"unsupported >128 literal nodes safely unchanged");
        for(String key:List.of("[NPC] Frosty: Welcome to Jerry's Workshop!","Jerry's Workshop","[NPC] St. Jerry: 'Tis the Season of Jerry, Test_001!","Season of Jerry")){
            var dir=root.resolve("override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой текст\n");flag("dictionary",new OfflineDictionary(dir));flag("chatNotifications",new ChatNotificationTranslator(new OfflineDictionary(dir)));
            String source=key.contains("Season of Jerry")?"[NPC] St. Jerry: 'Tis the Season of Jerry, Test_001!":"[NPC] Frosty: Welcome to Jerry's Workshop!";var m=msg(Component.literal(source),GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(m)==m,"custom override yields");
        }
        flag("dictionary",db);flag("chatNotifications",new ChatNotificationTranslator(db));
        for(int spaces=0;spaces<=6;spaces++)for(String count:List.of("0","9","1,234","999999"))for(int seed=0;seed<5;seed++){
            String pad=" ".repeat(spaces);var c=row(pad+"North Stars: "+count,1,seed);var translated=SkyRuClient.component(c);check(plain(translated).equals(pad+"Полярные звёзды: "+count),"winter HUD counter => "+plain(translated));span(c,count,translated,count);
            check(AuditMiningStyles.plain(SkyRuClient.sequence(c.getVisualOrderText())).equals(pad+"Полярные звёзды: "+count),"HUD sequence");for(int n=0;n<5;n++){translated=SkyRuClient.component(translated);check(plain(translated).equals(pad+"Полярные звёзды: "+count),"HUD repeat");}
        }
        var lead=Style.EMPTY.withColor(0xffff55).withInsertion("goal");var name=Style.EMPTY.withColor(0xffffff).withInsertion("npc");var goal=Component.empty().append(Component.literal("Talk to ").setStyle(lead)).append(Component.literal("Frosty").setStyle(name));var translated=SkyRuClient.component(goal);check(plain(translated).equals("Поговорите с Фрости"),"fragmented winter goal");span(goal,"Talk to",translated,"Поговорите с");span(goal,"Frosty",translated,"Фрости");
        flag("translateNpcChat",false);var m=msg(Component.literal("[NPC] Frosty: Welcome to Jerry's Workshop!"),GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(m)==m,"NPC toggle");flag("translateNpcChat",true);flag("enabled",false);check(SkyRuClient.npcChat(m)==m,"disabled identity");check(SkyRuClient.component(goal)==goal,"disabled GUI identity");
        System.out.println("PASS: winter event51 "+checks+" assertions;15 observed receipts/10 exact cores/dynamic greeting/HUD counters/fresh styles/actions/chat/overrides; not all winter branches");
    }
}
