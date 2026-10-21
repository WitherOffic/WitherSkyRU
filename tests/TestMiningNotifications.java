import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.util.StringDecomposer;

/** Cached primary regex grammars, synthetic Components; not a captured live channel. */
public class TestMiningNotifications {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static Style at(Component c,String word){int target=c.getString().indexOf(word);int[] offset={0};Style[] out={null};StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{if(offset[0]==target)out[0]=s;offset[0]+=Character.charCount(p);return true;});return out[0];}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);
        var chat=new ChatNotificationTranslator(db);
        Style name=Style.EMPTY.withColor(0x55ff55).withBold(true).withInsertion("original title");
        Style prose=Style.EMPTY.withColor(0xffff55).withHoverEvent(new HoverEvent.ShowText(Component.literal("original hover")));
        Style king=Style.EMPTY.withColor(0xffaa00).withClickEvent(new ClickEvent.RunCommand("/commissions"));
        String[][] titles={{"Mithril Miner","Добыча мифрила"},{"Titanium Miner","Добыча титана"},{"Goblin Slayer","Истребление гоблинов"},{"Ice Walker Slayer","Истребление ледяных ходоков"},{"Elusive Goblin Slayer","Истребление неуловимых гоблинов"}};
        for(int repeat=0;repeat<5;repeat++)for(var p:titles)for(String timestamp:List.of("","[23:59:59] ")){
            var original=Component.literal(timestamp).setStyle(prose).append(Component.literal(p[0]).setStyle(name))
                .append(Component.literal(" Commission Complete! Visit the ").setStyle(prose))
                .append(Component.literal("King").setStyle(king)).append(Component.literal(" to claim your rewards!").setStyle(prose));
            String source=original.getString(),target=timestamp+p[1]+" — задание выполнено! Поговорите с королём, чтобы забрать награды!";
            var result=chat.translate(original,GuiMessageSource.SYSTEM_SERVER,false);
            check(result.getString().equals(target),"full commission "+result.getString());
            check(original.getString().equals(source),"source immutable");
            check(at(result,p[1]).equals(at(original,p[0])),"effective title action/style");check(at(result,"королём").equals(at(original,"King")),"effective king action/style");
            check(chat.translate(result,GuiMessageSource.SYSTEM_SERVER,false)==result,"idempotence");
            check(chat.translate(original,GuiMessageSource.PLAYER,false)==original,"player excluded");
            check(chat.translate(original,GuiMessageSource.SYSTEM_SERVER,true)==original,"signed excluded");
            var legacy=Component.literal(timestamp+"§a§l"+p[0]+" §r§eCommission Complete! Visit the King §r§eto claim your rewards!");
            check(chat.translate(legacy,GuiMessageSource.SYSTEM_SERVER,false).getString().equals(target),"primary legacy colours");
        }
        String[][] mayhem={
            {"Your Pickaxe Ability cooldown was reduced from your Mineshaft Mayhem perk!","Способность вашей кирки перезаряжается быстрее благодаря перку «Хаос в штольнях»!"},
            {"You received a  Cold Resistance buff from your Mineshaft Mayhem perk!","Перк «Хаос в штольнях» повысил вашу  устойчивость к холоду!"},
            {"You received a  Mining Fortune buff from your Mineshaft Mayhem perk!","Перк «Хаос в штольнях» повысил вашу  удачу добычи!"},
            {"You received a  Mining Speed buff from your Mineshaft Mayhem perk!","Перк «Хаос в штольнях» повысил вашу  скорость добычи!"},
            {"Your Suspicious Scrap chance was buffed by your Mineshaft Mayhem perk!","Перк «Хаос в штольнях» повысил шанс получить подозрительный обломок!"}};
        for(int repeat=0;repeat<5;repeat++)for(var p:mayhem){var original=Component.literal(p[0]).setStyle(prose);var result=chat.translate(original,GuiMessageSource.SYSTEM_SERVER,false);
            check(result.getString().equals(p[1]),"Mayhem complete");check(original.getString().equals(p[0]),"Mayhem immutable");
            StringDecomposer.iterateFormatted(result,Style.EMPTY,(i,s,c)->{check(s.equals(prose),"Mayhem style/actions");return true;});
            check(chat.translate(original,GuiMessageSource.PLAYER,false)==original,"Mayhem player excluded");
            check(chat.translate(original,GuiMessageSource.SYSTEM_SERVER,true)==original,"Mayhem signed excluded");
            check(chat.translate(result,GuiMessageSource.SYSTEM_SERVER,false)==result,"Mayhem idempotent");
        }
        for(String text:List.of("Wither_101 Commission Complete! Visit the King to claim your rewards!","[VIP] Player: Mithril Miner Commission Complete! Visit the King to claim your rewards!","Unknown Task Commission Complete! Visit the King to claim your rewards!","You received an unknown buff from your Mineshaft Mayhem perk!")){
            var original=Component.literal(text);check(chat.translate(original,GuiMessageSource.SYSTEM_SERVER,false)==original,"unknown unchanged");
        }
        var unsupported=Component.literal("Your Pickaxe Ability cooldown was ").setStyle(name).append(Component.literal("reduced from your Mineshaft Mayhem perk!").setStyle(prose));
        check(chat.translate(unsupported,GuiMessageSource.SYSTEM_SERVER,false)==unsupported,"unmapped mixed clause fail closed");
        check(chat.familyCount()==36,"family count including two new forest notifications");
        System.out.println("PASS: mining notifications "+checks+" assertions; five named commissions, five exact Mayhem messages, five repeats, primary legacy pattern");
    }
}
