import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Source-backed totals/daily status: no invented fetch quest, reward or reset time. */
public class TestNpcPurchaseTotals {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xffffff).withInsertion("original prose");
    private static final Style PLACE=Style.EMPTY.withColor(0xff55ff).withClickEvent(new ClickEvent.RunCommand("/unchanged-place"));
    private static final Style VALUE=Style.EMPTY.withColor(0x55ffff).withHoverEvent(new HoverEvent.ShowText(Component.literal("original count")));
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String text(Component c){var b=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component c){var output=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int j=0;j<Character.charCount(p);j++)output.add(s);return true;});return output;}
    private static void retained(Component before,Component after,String from,String to){
        int oldAt=text(before).indexOf(from),newAt=text(after).indexOf(to);check(oldAt>=0&&newAt>=0,"semantic span present");
        var oldStyles=styles(before);var next=styles(after);for(int j=0;j<to.length();j++)check(oldStyles.get(oldAt).equals(next.get(newAt+j)),"semantic colour/action");
    }
    private static Component purchased(String count){
        var result=Component.literal("[NPC] Lucius: You've purchased ").setStyle(BODY);
        for(int i=0;i<count.length();i++)result.append(Component.literal(count.substring(i,i+1)).setStyle(VALUE.withColor(0x550000+i)));
        return result.append(Component.literal(" items from the ").setStyle(BODY)).append(Component.literal("Dark Auction").setStyle(PLACE)).append(Component.literal(".").setStyle(BODY));
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);var chat=new ChatNotificationTranslator(db);var npc=new NpcDialogueTranslator();
        String[][] counts={{"0","предметов"},{"1","предмет"},{"2","предмета"},{"11","предметов"},{"21","предмет"},{"101","предмет"},{"00021","предмет"},{"999999999999","предметов"}};
        for(int repeat=0;repeat<5;repeat++)for(var row:counts){
            var original=purchased(row[0]);var translated=chat.translateNpcRequest(original,GuiMessageSource.SYSTEM_SERVER,false);
            String expected="[NPC] Lucius: Ты купил "+row[0]+" "+row[1]+" на Тёмном аукционе.";
            check(text(translated).equals(expected),"purchase total, not instruction: "+text(translated));
            retained(original,translated,"Lucius","Lucius");retained(original,translated,"Dark Auction","Тёмном аукционе");
            int oldAt=text(original).indexOf(row[0]),newAt=text(translated).indexOf(row[0]);var oldStyles=styles(original);var next=styles(translated);
            for(int j=0;j<row[0].length();j++)check(oldStyles.get(oldAt+j).equals(next.get(newAt+j)),"each numeric glyph unchanged");
            check(chat.translateNpcRequest(translated,GuiMessageSource.SYSTEM_SERVER,false)==translated,"idempotent");
            check(chat.translateNpcRequest(original,GuiMessageSource.PLAYER,false)==original,"all player text excluded");
            check(chat.translateNpcRequest(original,GuiMessageSource.SYSTEM_CLIENT,false)==original,"client NPC imitation excluded");
            check(chat.translateNpcRequest(original,GuiMessageSource.SYSTEM_SERVER,true)==original,"signed message excluded");
            check(chat.translate(original,GuiMessageSource.SYSTEM_SERVER,false)==original,"not notification toggle");
            var timed=Component.literal("[02:03:04] ").append(original);check(text(chat.translateNpcRequest(timed,GuiMessageSource.SYSTEM_SERVER,false)).equals("[02:03:04] "+expected),"timestamp retained");
        }
        for(String count:List.of("1,001","-1","1.5","1000000000000","pickaxe","Wither_101")){
            var original=purchased(count);check(chat.translateNpcRequest(original,GuiMessageSource.SYSTEM_SERVER,false)==original,"unsupported count not guessed");
        }
        String originalCarnival="[NPC] Carnival Leader: You've already claimed your Carnival Tickets for today, but I'm happy to answer any questions you might have.";
        String targetCarnival="[NPC] Carnival Leader: Ты уже забрал билеты карнавала на сегодня. Но можешь задать мне вопросы — с радостью отвечу!";
        var carnival=Component.literal("[NPC] Carnival Leader: You've already claimed your ").setStyle(BODY)
            .append(Component.literal("Carnival Tickets").setStyle(PLACE)).append(Component.literal(" for ").setStyle(BODY))
            .append(Component.literal("today").setStyle(VALUE)).append(Component.literal(", but I'm happy to answer any questions you might have.").setStyle(BODY));
        for(int repeat=0;repeat<5;repeat++){
            var translated=npc.translate(carnival);check(text(translated).equals(targetCarnival),"daily tickets already received");
            retained(carnival,translated,"Carnival Leader","Carnival Leader");retained(carnival,translated,"Carnival Tickets","билеты карнавала");retained(carnival,translated,"today","сегодня");
            check(npc.translate(translated)==translated,"daily status idempotent");check(carnival.getString().equals(originalCarnival),"immutable carnival source");
        }
        String legacy="§e[NPC] §aCarnival Leader§f: §rYou've already claimed your §aCarnival Tickets §ffor §btoday§f, but I'm happy to answer any questions you might have.";
        var legacyOriginal=Component.literal(legacy).setStyle(BODY);var legacyResult=npc.translate(legacyOriginal);check(text(legacyResult).equals(targetCarnival),"cached primary legacy sample");
        retained(legacyOriginal,legacyResult,"Carnival Leader","Carnival Leader");retained(legacyOriginal,legacyResult,"Carnival Tickets","билеты карнавала");retained(legacyOriginal,legacyResult,"today","сегодня");
        var missing=Component.literal(originalCarnival.replace("today","tomorrow"));check(npc.translate(missing)==missing,"unknown day not guessed");
        for(String row:List.of(purchased("3").getString()+"\tМоя реплика\n","Dark Auction\tМой аукцион\n")){
            Path custom=Path.of(args[0]).resolve("purchase-custom-"+checks);Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),row);
            var c=purchased("3");check(new ChatNotificationTranslator(new OfflineDictionary(custom)).translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"whole/inflected place override respected");
        }
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("npcDialogues",npc);flag("chatNotifications",chat);flag("translateNpcChat",true);flag("translateGameChat",false);
        for(var c:List.of(purchased("3"),carnival)){
            var system=new GuiMessage(1,c,null,GuiMessageSource.SYSTEM_SERVER,null);check(!SkyRuClient.npcChat(system).content().getString().equals(c.getString()),"NPC toggle alone");
            var player=new GuiMessage(1,c,null,GuiMessageSource.PLAYER,null);check(SkyRuClient.npcChat(player)==player,"whole player message not translated");
            flag("translateNpcChat",false);flag("translateGameChat",true);check(SkyRuClient.npcChat(system)==system,"NPC disabled no ordinary-chat fallback");flag("translateNpcChat",true);flag("translateGameChat",false);
        }
        flag("enabled",false);var system=new GuiMessage(1,carnival,null,GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(system)==system,"mod disabled");
        System.out.println("PASS: NPC purchase/daily status "+checks+" assertions; source-backed total, original per-glyph counts, eight synthetic counts/five repeats, cached Carnival sample, no invented requirement/reset");
    }
}
