import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Sirius primary examples, SkyHanni's appended item, original nicknames and price glyphs. */
public class TestNpcAuctionClarity {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xffffff).withInsertion("original prose");
    private static final Style NAME=Style.EMPTY.withColor(0x55ffff).withHoverEvent(new HoverEvent.ShowText(Component.literal("original player")));
    private static final Style ITEM=Style.EMPTY.withColor(0xff55ff).withClickEvent(new ClickEvent.RunCommand("/unchanged-item"));
    private static final Style MONEY=Style.EMPTY.withColor(0xffff55).withHoverEvent(new HoverEvent.ShowText(Component.literal("original price")));
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String text(Component c){var b=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component c){var output=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int j=0;j<Character.charCount(p);j++)output.add(s);return true;});return output;}
    private static void retained(Component before,Component after,String from,String to,boolean copy){
        int oldAt=text(before).indexOf(from),newAt=text(after).indexOf(to);check(oldAt>=0&&newAt>=0,"semantic span present");
        var oldStyles=styles(before);var next=styles(after);for(int j=0;j<to.length();j++)check(oldStyles.get(oldAt+(copy?j:0)).equals(next.get(newAt+j)),"semantic colour/action for "+from);
    }
    private static Component amount(Component target,String count){for(int i=0;i<count.length();i++)((MutableComponent)target).append(Component.literal(count.substring(i,i+1)).setStyle(MONEY.withColor(0x770000+i)));return target;}
    private static Component lot(String first,String item,String count){
        var c=Component.literal("[NPC] Sirius: ").setStyle(BODY).append(Component.literal(first).setStyle(NAME)).append(Component.literal(" up we have "+(item.startsWith("Ender")?"an ":"a ")).setStyle(BODY))
            .append(Component.literal(item).setStyle(ITEM)).append(Component.literal(", the starting bid is ").setStyle(BODY));amount(c,count);return c.append(Component.literal(" ").setStyle(BODY)).append(Component.literal("Coins").setStyle(MONEY)).append(Component.literal("!").setStyle(BODY));
    }
    private static Component highest(String nick,String count,String item){
        var c=Component.literal("[NPC] Sirius: The highest bidder was ").setStyle(BODY).append(Component.literal(nick).setStyle(NAME)).append(Component.literal(" with a bid of ").setStyle(BODY));
        amount(c,count);c.append(Component.literal(" ").setStyle(BODY)).append(Component.literal("Coins").setStyle(MONEY)).append(Component.literal("!").setStyle(BODY));
        if(item!=null)c.append(Component.literal(" (").setStyle(BODY)).append(Component.literal(item).setStyle(ITEM)).append(Component.literal(")").setStyle(BODY));return c;
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);var chat=new ChatNotificationTranslator(db);
        String[][] items={{"Nether Artifact","Артефакт Незера"},{"Protection VII Book","Книга «Защита VII»"},{"Ender Artifact","Эндерский артефакт"}};
        String[][] prices={{"1","монета"},{"2","монеты"},{"11","монет"},{"21","монета"},{"50,000","монет"},{"25,950,000","монет"}};
        for(int repeat=0;repeat<5;repeat++)for(var item:items)for(var price:prices){
            for(String first:List.of("First","Next")){
                var c=lot(first,item[0],price[0]);String original=text(c);var got=chat.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false);
                String expected="[NPC] Sirius: "+(first.equals("First")?"Первый":"Следующий")+" лот: "+item[1]+". Начальная ставка — "+price[0]+" "+price[1]+"!";
                check(text(got).equals(expected),"starting bid not sale price: "+text(got));retained(c,got,first,first.equals("First")?"Первый лот":"Следующий лот",false);
                retained(c,got,item[0],item[1],false);retained(c,got,price[0],price[0],true);retained(c,got,"Coins",price[1],false);
                check(chat.translateNpcRequest(got,GuiMessageSource.SYSTEM_SERVER,false)==got,"lot idempotent");check(text(c).equals(original),"lot source immutable");
            }
            for(String nick:List.of("qtLuna","Wither_101","pickaxe"))for(String suffix:new String[]{null,item[0]}){
                var c=highest(nick,price[0],suffix);var got=chat.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false);
                String expected="[NPC] Sirius: Наибольшую ставку сделал "+nick+" — "+price[0]+" "+price[1]+"!"+(suffix==null?"":" ("+item[1]+")");
                check(text(got).equals(expected),"highest bidder not charged/winner: "+text(got));retained(c,got,nick,nick,true);retained(c,got,price[0],price[0],true);retained(c,got,"Coins",price[1],false);
                if(suffix!=null)retained(c,got,item[0],item[1],false);check(chat.translateNpcRequest(got,GuiMessageSource.SYSTEM_SERVER,false)==got,"highest idempotent");
            }
        }
        for(String tail:List.of(""," Let's move on to the next item."))for(var item:items){
            var c=Component.literal("[NPC] Sirius: No one made any bids!"+tail+" (").setStyle(BODY).append(Component.literal(item[0]).setStyle(ITEM)).append(Component.literal(")").setStyle(BODY));
            var got=chat.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false);check(text(got).equals("[NPC] Sirius: Никто не сделал ставку!"+(tail.isEmpty()?"":" Перейдём к следующему лоту.")+" ("+item[1]+")"),"no bids with confirmed continuation/item");retained(c,got,item[0],item[1],false);
            check(chat.translateNpcRequest(got,GuiMessageSource.SYSTEM_SERVER,false)==got,"no-bid idempotent");
        }
        for(String source:List.of("[NPC] Sirius: No one made any bids!","[NPC] Sirius: No one made any bids! Let's move on to the next item."))check(!text(chat.translateNpcRequest(Component.literal(source),GuiMessageSource.SYSTEM_SERVER,false)).equals(source),"raw primary no-bid sample");
        var multicolour=Component.literal("[NPC] Sirius: Next up we have a ").setStyle(BODY)
            .append(Component.literal("Protection").setStyle(ITEM)).append(Component.literal(" ").setStyle(BODY))
            .append(Component.literal("V").setStyle(MONEY.withColor(0x110000))).append(Component.literal("I").setStyle(MONEY.withColor(0x220000))).append(Component.literal("I").setStyle(MONEY.withColor(0x330000)))
            .append(Component.literal(" ").setStyle(BODY)).append(Component.literal("Book").setStyle(NAME))
            .append(Component.literal(", the starting bid is 100,000 ").setStyle(BODY)).append(Component.literal("Coins").setStyle(MONEY)).append(Component.literal("!").setStyle(BODY));
        var bookResult=chat.translateNpcRequest(multicolour,GuiMessageSource.SYSTEM_SERVER,false);
        check(text(bookResult).equals("[NPC] Sirius: Следующий лот: Книга «Защита VII». Начальная ставка — 100,000 монет!"),"split book title uses semantic spans, not flat replacement");
        retained(multicolour,bookResult,"Protection","Защита",false);retained(multicolour,bookResult,"Book","Книга «",false);retained(multicolour,bookResult,"VII","VII",true);
        var c=highest("Wither_101","25,950,000","Nether Artifact");
        for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT))check(chat.translateNpcRequest(c,channel,false)==c,"entire non-server/player message excluded");
        check(chat.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,true)==c,"signed unchanged");check(chat.translate(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"not ordinary-notification toggle");
        var timed=Component.literal("[02:03:04] ").append(c);check(text(chat.translateNpcRequest(timed,GuiMessageSource.SYSTEM_SERVER,false)).startsWith("[02:03:04] [NPC] Sirius: Наибольшую ставку сделал Wither_101"),"time and nick unchanged");
        for(String bad:List.of(c.getString().replace("25,950,000","25,95,000"),c.getString().replace("25,950,000","-1"),c.getString().replace("Sirius","Other NPC"),"[NPC] Sirius: No one made any bids! Unconfirmed extra requirement.","Wither_101: pickaxe",c.getString()+" unconfirmed tail")){
            var raw=Component.literal(bad);check(chat.translateNpcRequest(raw,GuiMessageSource.SYSTEM_SERVER,false)==raw,"unknown shape/input never guessed");
        }
        Path nickConfig=Path.of(args[0]).resolve("auction-nick-custom");Files.createDirectories(nickConfig);Files.writeString(nickConfig.resolve("overrides.tsv"),"Wither_101\tВитхер_101\npickaxe\tКирка\n");
        var nickChat=new ChatNotificationTranslator(new OfflineDictionary(nickConfig));check(text(nickChat.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false)).contains("Wither_101 —"),"nickname never looked up in dictionary even if explicit key exists");
        for(String custom:List.of(c.getString()+"\tМоя реплика\n","Coins\tМои монеты\n","Nether Artifact\tМой артефакт\n","Protection\tМоё зачарование\n")){
            Path config=Path.of(args[0]).resolve("auction-custom-"+checks);Files.createDirectories(config);Files.writeString(config.resolve("overrides.tsv"),custom);var override=new ChatNotificationTranslator(new OfflineDictionary(config));check(override.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"closed authored translation yields to custom phrase/term");
        }
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("npcDialogues",new NpcDialogueTranslator());flag("chatNotifications",chat);flag("translateNpcChat",true);flag("translateGameChat",false);
        var message=new GuiMessage(1,c,null,GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(message).content().getString().contains("Наибольшую ставку сделал Wither_101"),"NPC toggle and original nick");
        flag("translateNpcChat",false);flag("translateGameChat",true);check(SkyRuClient.npcChat(message)==message,"NPC disabled no fallback");flag("translateNpcChat",true);flag("enabled",false);check(SkyRuClient.npcChat(message)==message,"disabled unchanged");
        System.out.println("PASS: NPC auction clarity "+checks+" assertions; three primary shapes/three primary items, five repeats, raw/known SkyHanni suffix, original nick and numeric glyphs, no invented winner/action");
    }
}
