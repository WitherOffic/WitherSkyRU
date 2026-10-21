import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;
import ru.wither.skyru.*;

/** Fourteen primary shapes; expanded values are synthetic, not invented server captures. */
public class TestDungeonCombat {
    private static int checks;private static ChatNotificationTranslator chat;
    private static void check(boolean x,String why){if(!x)throw new AssertionError(why);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String text(Component c){var b=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static List<String> numbers(String s){return Pattern.compile("[0-9]+(?:[,.][0-9]+)*").matcher(s).results().map(x->x.group()).toList();}
    private static void anchor(Component old,Component now,String source,String target,boolean exact){
        int a=text(old).indexOf(source),b=text(now).indexOf(target);check(a>=0&&b>=0,"missing "+source+" -> "+target);
        var x=styles(old);var y=styles(now);for(int i=0;i<target.length();i++)check(y.get(b+i).equals(x.get(a+(exact?i:0))),"semantic/count/name action "+source);
    }
    private record Token(int start,int end,boolean numeric){}
    private static Component fragmented(JsonObject row,Style body,Style semantic){
        String source=row.get("source").getAsString();var tokens=new ArrayList<Token>();
        for(var v:row.getAsJsonArray("retained")){String word=v.getAsString();int at=source.indexOf(word);check(at>=0,"token position");tokens.add(new Token(at,at+word.length(),Character.isDigit(word.charAt(0))));}
        for(var raw:row.getAsJsonArray("style_anchors")){String word=raw.getAsJsonObject().get("source").getAsString();int at=source.indexOf(word);tokens.add(new Token(at,at+word.length(),false));}
        tokens.sort(Comparator.comparingInt(Token::start));var result=Component.empty();int cursor=0;
        for(var token:tokens){check(token.start()>=cursor,"nonoverlapping test anchors");if(cursor<token.start())result.append(Component.literal(source.substring(cursor,token.start())).setStyle(body));
            if(token.numeric())for(int i=token.start();i<token.end();i++)result.append(Component.literal(source.substring(i,i+1)).setStyle(semantic.withInsertion("digit-"+i)));
            else result.append(Component.literal(source.substring(token.start(),token.end())).setStyle(semantic.withInsertion("token-"+token.start())));
            cursor=token.end();
        }if(cursor<source.length())result.append(Component.literal(source.substring(cursor)).setStyle(body));return result;
    }
    private static void replay(JsonObject row,Component original,boolean semantic)throws Exception{
        String target=row.get("target").getAsString(),source=row.get("source").getAsString(),unchanged=original.getString();
        Component result=chat.translate(original,GuiMessageSource.SYSTEM_SERVER,false);
        check(text(result).equals(target),"whole clause "+row.get("id")+": "+text(result)+" expected "+target);
        check(numbers(source).equals(numbers(target)),"original numeric multiset");
        for(int repeat=0;repeat<5;repeat++){
            check(chat.translate(result,GuiMessageSource.SYSTEM_SERVER,false)==result,"idempotence identity");
            check(original.getString().equals(unchanged),"source immutable");
            for(var token:row.getAsJsonArray("retained"))anchor(original,result,token.getAsString(),token.getAsString(),true);
            if(semantic)for(var raw:row.getAsJsonArray("style_anchors")){
                var a=raw.getAsJsonObject();anchor(original,result,a.get("source").getAsString(),a.get("target").getAsString(),false);
            }
        }
        for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){
            check(chat.translate(original,channel,false)==original,"whole player/client unchanged");
            var m=new GuiMessage(7,original,null,channel,null);check(SkyRuClient.npcChat(m)==m,"queue excluded");
        }
        check(chat.translate(original,GuiMessageSource.SYSTEM_SERVER,true)==original,"signed unchanged");
        check(chat.translateNpcRequest(original,GuiMessageSource.SYSTEM_SERVER,false)==original,"not NPC request");
        var timed=Component.literal("[03:04:05] ").append(original);
        var timedRu=chat.translate(timed,GuiMessageSource.SYSTEM_SERVER,false);
        check(text(timedRu).equals("[03:04:05] "+target),"timestamp");
        anchor(timed,timedRu,"[03:04:05] ","[03:04:05] ",true);
        flag("translateNpcChat",false);flag("translateGameChat",true);
        var m=new GuiMessage(7,original,null,GuiMessageSource.SYSTEM_SERVER,null);
        check(text(SkyRuClient.npcChat(m).content()).equals(target),"game flag alone");
        flag("translateNpcChat",true);flag("translateGameChat",false);
        check(SkyRuClient.npcChat(m)==m,"NPC flag does not translate game notices");
        flag("translateGameChat",true);
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);chat=new ChatNotificationTranslator(db);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("chatNotifications",chat);
        flag("npcDialogues",new NpcDialogueTranslator());flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());flag("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());
        check(chat.familyCount()==36&&chat.dungeonFamilyCount()==6&&chat.dungeonCombatFamilyCount()==14,"separate documented counts");
        var root=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("DUNGEON_COMBAT_PRIMARY_SHAPES.json"))).getAsJsonObject();
        var rows=root.getAsJsonArray("cases");check(rows.size()==14,"fixed 14 shapes");
        Style body=Style.EMPTY.withColor(0xffffff).withClickEvent(new ClickEvent.RunCommand("/original")).withHoverEvent(new HoverEvent.ShowText(Component.literal("original hover"))).withInsertion("body");
        Style token=body.withColor(0x55ffff).withClickEvent(new ClickEvent.SuggestCommand("/original-token "));
        Set<String> seen=new HashSet<>();
        for(var raw:rows){var row=raw.getAsJsonObject();check(seen.add(row.get("id").getAsString()),"unique case");
            String sample=row.get("sample").getAsString(),pattern=row.get("pattern").getAsString(),source=row.get("source").getAsString();
            if(row.get("kind").getAsString().startsWith("PRIMARY_TEMPLATE"))
                check(Pattern.compile(pattern).matcher(sample).matches(),"synthetic sample satisfies actual primary regex");
            else check(sample.equals(pattern),"frozen literal/REGEX-TEST copied");
            check(sample.replaceAll("(?i)§[0-9a-fk-or]","").equals(source),"visible source");
            replay(row,Component.literal(source).setStyle(body),false);
            replay(row,Component.literal(sample).setStyle(body),true);
            replay(row,fragmented(row,body,token),true);
        }
        for(int n=1;n<=250;n++){
            int tail=n%100,last=n%10;String word=tail>=11&&tail<=14?"врагов":last==1?"врага":"врагов";
            String raw="Your Guided Sheep hit "+n+" enemy for 1,250 damage.";
            check(text(chat.translate(Component.literal(raw),GuiMessageSource.SYSTEM_SERVER,false)).equals("Способность «Управляемая овца» задела "+n+" "+word+", нанеся 1,250 урона."),"enemy plural");
        }
        for(String malformed:List.of("You need at least 1,,250 mana to activate this!","This ability is on cooldown for -1s.","Your Unknown Spell hit 2 enemy for 250 damage.","Your Implosion hit 2 enemy for 250 damage. extra","Your fairy healed PlayerWithTooLongNickname for 250 health!","You gained 250 HP worth of absorption for 4s from Wither_101!","Wither_101 granted you 50 strength for 30 seconds!","[Berserk] Melee Damage 48 -> 88","[Berserk] Walk Speed 38% -> 68%","[Mage] Melee Damage 48% -> 88%","[MVP+] Wither_101: You need at least 250 mana to activate this!","pickaxe","banana","Wither_101")){
            var c=Component.literal(malformed);check(chat.translate(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"unknown/ordinary/player not guessed: "+malformed);
        }
        var split=Component.literal("You need ").setStyle(body).append(Component.literal("at least ").setStyle(token)).append(Component.literal("250 mana to activate this!").setStyle(body));
        check(chat.translate(split,GuiMessageSource.SYSTEM_SERVER,false)==split,"mixed prose actions not flattened");
        Path custom=Path.of(args[0]).resolve("custom-dungeon-combat");Files.createDirectories(custom);
        String key="You need at least 250 mana to activate this!";
        Files.writeString(custom.resolve("overrides.tsv"),key+"\tМоя фраза\nImplosion\tМой термин\nHealing V\tМой эффект\nBerserk\tМой класс\n");
        var override=new ChatNotificationTranslator(new OfflineDictionary(custom));
        for(String s:List.of(key,"Your Implosion hit 2 enemy for 250 damage.","BUFF! You have gained Healing V!","[Berserk] Melee Damage 48% -> 88%")){
            var c=Component.literal(s);check(override.translate(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"custom override priority");
        }
        System.out.println("PASS: Dungeon combat "+checks+" assertions; 14 primary shapes /11 synthetic template expansions /one literal /two frozen class samples; amounts/nicks/styles/actions/timestamps, five repeats, no player/client/signed translation; not live-tested");
    }
}
