import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

public class TestReorderedTitles {
    private static int checks;
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); checks++; }
    private static String plain(FormattedCharSequence row) { var b=new StringBuilder();row.accept((i,s,c)->{b.appendCodePoint(c);return true;});return b.toString(); }
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        Style modifier=Style.EMPTY.withColor(0x55ff55).withClickEvent(new ClickEvent.RunCommand("/reforges"));
        Style base=Style.EMPTY.withColor(0x55ffff).withHoverEvent(new HoverEvent.ShowText(Component.literal("base")));
        Style gap=Style.EMPTY.withColor(0xffffff),stars=Style.EMPTY.withColor(0xffaa00).withBold(true);
        String[][] pairs={{"Lumberjack's","Advanced Gardening Axe","Продвинутый садовый топор","лесоруба"},
            {"Peasant's","Advanced Gardening Hoe","Продвинутая садовая мотыга","крестьянина"},
            {"Prospector's","Diamond Pickaxe","Алмазная кирка","старателя"},
            {"Green Thumb","Advanced Gardening Hoe","Продвинутая садовая мотыга","«Зелёные пальцы»"},
            {"Greater Spook","Diamond Sword","Алмазный меч","великого ужаса"},
            {"Jerry's","Diamond Sword","Алмазный меч","Джерри"},
            {"Moonglade","Diamond Sword","Алмазный меч","лунной поляны"}};
        for(int repeat=0;repeat<5;repeat++)for(String[] p:pairs)for(String mark:List.of(""," ✪✪✪✪✪"," ➊➋➌➍➎"," [10]"," ★")){
            String expected=p[2]+" "+p[3]+mark;
            var source=Component.empty().append(Component.literal(p[0]).setStyle(modifier)).append(Component.literal(" ").setStyle(gap))
                .append(Component.literal(p[1]).setStyle(base)).append(Component.literal(mark).setStyle(stars));
            Component result=SkyRuClient.component(source);
            check(result.getString().equals(expected),"suffix component "+source.getString()+" => "+result.getString());
            int[] at={0};StringDecomposer.iterateFormatted(result,Style.EMPTY,(i,s,c)->{
                int pos=at[0];at[0]+=Character.charCount(c);
                Style target=pos<p[2].length()?base:pos==p[2].length()?gap:pos<p[2].length()+1+p[3].length()?modifier:stars;
                check(s.equals(target),"semantic suffix style/actions");return true;});
            var seq=FormattedCharSequence.composite(FormattedCharSequence.forward(p[0],modifier),FormattedCharSequence.forward(" ",gap),FormattedCharSequence.forward(p[1],base),FormattedCharSequence.forward(mark,stars));
            check(plain(SkyRuClient.sequence(seq)).equals(expected),"suffix sequence");
            check(plain(SkyRuClient.sequence(SkyRuClient.sequence(seq))).equals(expected),"idempotence");
            check(source.getString().equals(p[0]+" "+p[1]+mark),"original immutable");
            check(TooltipTranslation.components(List.of(source),true).getFirst()==source,"Shift original");
            check(SkyRuClient.searchName(source,"мотыга").getString().contains(p[2]),"search still contains base");
        }
        var mixed=Component.literal("Lumberjack's").setStyle(modifier).append(Component.literal(" ").setStyle(gap))
            .append(Component.literal("Advanced ").setStyle(base)).append(Component.literal("Gardening Axe").setStyle(stars));
        check(SkyRuClient.component(mixed).getString().equals(mixed.getString()),"mixed base not flattened or partially translated");
        var unknown=Component.literal("Lumberjack's ZX-Unknown-99").setStyle(base);
        check(SkyRuClient.component(unknown).getString().equals(unknown.getString()),"unknown item not guessed");
        Path custom=Path.of(args[0]).resolve("custom-full-title");Files.createDirectories(custom);
        Files.writeString(custom.resolve("overrides.tsv"),"Lumberjack's Advanced Gardening Axe\tМой топор\n");
        var overrides=new OfflineDictionary(custom);overrides.setRecordUnknown(false);flag("dictionary",overrides);
        check(SkyRuClient.component(Component.literal("Lumberjack's Advanced Gardening Axe").setStyle(base)).getString().equals("Мой топор"),"full title override wins");
        flag("dictionary",db);flag("enabled",false);
        check(SkyRuClient.component(mixed)==mixed,"disabled retains original");
        System.out.println("PASS: reordered item titles "+checks+" assertions; all seven reviewed suffixes, five decorations, five repeats, semantic actions and guards");
    }
}
