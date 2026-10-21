import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

public class TestMiningInstructions {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,c)->{b.appendCodePoint(c);return true;});return b.toString();}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        Style prose=Style.EMPTY.withColor(0xffffff).withClickEvent(new ClickEvent.RunCommand("/commissions"));
        Style number=Style.EMPTY.withColor(0x55ff55).withInsertion("original count");
        Style material=Style.EMPTY.withColor(0x55ffff).withHoverEvent(new HoverEvent.ShowText(Component.literal("original material")));
        String[][] cases={{"Mine ","Mithril","Добудьте ","ед. мифрила"},{"Mine ","Titanium","Добудьте ","ед. титана"},
            {"Mine ","Hard Stone","Добудьте ","ед. твёрдого камня"},{"Collect ","Mithril","Соберите ","ед. мифрила"},
            {"Collect ","Titanium","Соберите ","ед. титана"},{"Kill ","Goblins","Убейте ","гоблинов"},
            {"Defeat ","Ice Walkers","Победите ","ледяных ходоков"}};
        for(int repeat=0;repeat<5;repeat++)for(String[] p:cases)for(String count:List.of("10","350","1,000"))for(String location:List.of(""," in the Dwarven Mines"))for(String dot:List.of("",".")){
            // Existing location forms are limited to the two minerals, not invented quests.
            if(!location.isEmpty()&&!(p[0].equals("Mine ")&&(p[1].equals("Mithril")||p[1].equals("Titanium"))))continue;
            String source=p[0]+count+" "+p[1]+location+dot,target=p[2]+count+" "+p[3]+(location.isEmpty()?"":" в Дварфских шахтах")+".";
            var seq=FormattedCharSequence.composite(FormattedCharSequence.forward(p[0],prose),FormattedCharSequence.forward(count,number),FormattedCharSequence.forward(" ",prose),FormattedCharSequence.forward(p[1],material),FormattedCharSequence.forward(location+dot,prose));
            var translated=SkyRuClient.sequence(seq);check(plain(translated).equals(target),source+" => "+plain(translated));
            check(plain(SkyRuClient.sequence(translated)).equals(target),"idempotence");check(plain(seq).equals(source),"source immutable");
            int[] at={0};translated.accept((i,s,c)->{int pos=at[0];at[0]+=Character.charCount(c);if(pos>=p[2].length()&&pos<p[2].length()+count.length())check(s.equals(number),"numeric style/action");return true;});
            var raw=Component.empty().append(Component.literal(p[0]).setStyle(prose)).append(Component.literal(count).setStyle(number)).append(Component.literal(" ").setStyle(prose)).append(Component.literal(p[1]).setStyle(material)).append(Component.literal(location+dot).setStyle(prose));
            check(SkyRuClient.component(raw).getString().equals(target),"component full instruction");check(raw.getString().equals(source),"component immutable");
        }
        check(db.translate("Mithril Miner").equals("Добыча мифрила"),"HUD label");
        check(db.translate("Titanium Miner").equals("Добыча титана"),"HUD label");
        check(db.translate("Mine 10 Totally Unknown ZX-99").equals("Mine 10 Totally Unknown ZX-99"),"unknown unchanged");
        System.out.println("PASS: mining instructions "+checks+" assertions; complete reviewed instructions, exact numeric copies, component/sequence, five repeats");
    }
}
