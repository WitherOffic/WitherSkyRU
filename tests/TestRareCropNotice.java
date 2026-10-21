import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Screenshot + synthetic coloured/deduplicated rare-crop notices, not a live capture. */
public class TestRareCropNotice {
    private static int checks,variants;
    private static final Style TIME=Style.EMPTY.withColor(0xff55ff).withInsertion("timestamp");
    private static final Style RARE=Style.EMPTY.withColor(0xffaa00).withBold(true).withInsertion("rare header");
    private static final Style CROP=Style.EMPTY.withColor(0xffff55).withClickEvent(new ClickEvent.RunCommand("/original-header"));
    private static final Style ITEM=Style.EMPTY.withColor(0x5555ff).withHoverEvent(new HoverEvent.ShowText(Component.literal("original item hover")));
    private static final Style VALUE=Style.EMPTY.withColor(0xaa5500).withInsertion("original value");
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    private static void flag(String k,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(k);f.setAccessible(true);f.set(null,v);}
    private static GuiMessage message(Component c,GuiMessageSource s){return new GuiMessage(0,c,null,s,null);}
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static void span(Component c,String word,Style wanted){int at=c.getString().indexOf(word);check(at>=0,"missing "+word);var ss=styles(c);for(int n=at;n<at+word.length();n++)check(ss.get(n).equals(wanted),"style/action "+word);}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);var chat=new ChatNotificationTranslator(db);
        flag("dictionary",db);flag("chatNotifications",chat);flag("enabled",true);flag("onlyHypixel",false);flag("translateGameChat",true);
        var names=List.of("Aggourdian","Botroot","Burrowing Spores","Cactus Flower","Cane Knot","Carrot Zest","Tasty Cheese","Compost","Cornucopia","Cropie","Crystalized Moonlight","Deepfries","Designer Coffee Beans","Dung","Feastfungus","Fermento","Floral Gelatin","Helianthus","Honey Jar","Jelly","Melon Juice","Plant Matter","Rarefinder Chip","Salted Sunflower Seeds","Seasoning","Squash","Warty");
        for(String name:names)for(String prefix:List.of("","VERY "))for(String bonus:List.of(""," (+1.5%)"," (+6.5%)"))for(String suffix:List.of(""," (x17)"," (×2)")){
            String source="[22:54:12] "+prefix+"RARE CROP! "+name+bonus+suffix,target="[22:54:12] "+(prefix.isEmpty()?"":"ОЧЕНЬ ")+"РЕДКИЙ УРОЖАЙ! "+db.translateForSearch(name)+bonus+suffix;
            var c=Component.empty().append(Component.literal("[22:54:12] ").setStyle(TIME));if(!prefix.isEmpty())c.append(Component.literal(prefix).setStyle(TIME));
            c.append(Component.literal("RARE").setStyle(RARE)).append(Component.literal(" ").setStyle(TIME)).append(Component.literal("CROP!").setStyle(CROP)).append(Component.literal(" ").setStyle(ITEM)).append(Component.literal(name).setStyle(ITEM));
            for(int i=0;i<(bonus+suffix).length();i++)c.append(Component.literal((bonus+suffix).substring(i,i+1)).setStyle(VALUE.withColor(0x100000+i)));
            var server=message(c,GuiMessageSource.SYSTEM_SERVER);var result=SkyRuClient.npcChat(server).content();check(result.getString().equals(target),source+" => "+result.getString());check(c.getString().equals(source),"immutable");
            span(result,"[22:54:12] ",TIME);span(result,"РЕДКИЙ",RARE);span(result,"УРОЖАЙ!",CROP);span(result,db.translateForSearch(name),ITEM);
            var old=styles(c);var next=styles(result);int oldAt=source.length()-bonus.length()-suffix.length(),newAt=target.length()-bonus.length()-suffix.length();for(int i=0;i<(bonus+suffix).length();i++)check(old.get(oldAt+i).equals(next.get(newAt+i)),"each bonus/counter glyph and action");
            for(int n=0;n<5;n++)check(SkyRuClient.npcChat(message(result,GuiMessageSource.SYSTEM_SERVER)).content()==result,"five repeats");
            for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var p=message(c,channel);check(SkyRuClient.npcChat(p)==p,"whole chat protected");}
            var signed=new GuiMessage(0,c,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed protected");variants++;
        }
        var literal=Component.literal("[22:54:12] RARE CROP! Rarefinder Chip (+1.5%)");check(chat.translate(literal,GuiMessageSource.SYSTEM_SERVER,false).getString().equals("[22:54:12] РЕДКИЙ УРОЖАЙ! Чип «Искатель редкостей» (+1.5%)"),"independent screenshot wording");
        var legacy=Component.literal("§d[22:54:12] §6RARE §eCROP! §9Rarefinder Chip §6(+§e1§a.§b5§6%)");check(chat.translate(legacy,GuiMessageSource.SYSTEM_SERVER,false).getString().equals("[22:54:12] РЕДКИЙ УРОЖАЙ! Чип «Искатель редкостей» (+1.5%)"),"legacy screenshot colours");
        for(String value:List.of("RARE CROP! Wither_101 (+1.5%)","RARE CROP! Rarefinder Chip (+banana%)","RARE DROP! Rarefinder Chip (+1.5%)","RARE CROP! Rarefinder Chip (×banana)","RARE CROP! Rarefinder Chip (+1.5%) trailing words")){var c=message(Component.literal(value),GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(c)==c,"unknown/malformed not guessed");}
        for(String key:List.of("Rarefinder Chip","RARE CROP!","RARE CROP! Rarefinder Chip (+1.5%)")){
            var dir=Path.of(args[0]).resolve("override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой текст\n");var own=new OfflineDictionary(dir);var c=Component.literal("[22:54:12] RARE CROP! Rarefinder Chip (+1.5%) (x17)");check(new ChatNotificationTranslator(own).translate(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"full/base/term override");
        }
        flag("translateGameChat",false);var c=message(literal,GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(c)==c,"game notification toggle");flag("translateGameChat",true);
        System.out.println("PASS: rare-crop notice "+checks+" assertions; "+names.size()+" known names / "+variants+" synthetic variants, styled words/values/counters, screenshot/legacy/overrides/chat guards; no live game");
    }
}
