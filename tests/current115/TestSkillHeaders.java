import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import net.minecraft.client.multiplayer.chat.*;

public class TestSkillHeaders {
    private static int checks;
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component c){var a=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int k=0;k<Character.charCount(p);k++)a.add(s);return true;});return a;}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        String[] en={"Alchemy","Carpentry","Combat","Enchanting","Farming","Fishing","Foraging","Mining","Runecrafting","Social","Taming","Hunting","Catacombs","Dungeoneering"};
        String[] ru={"Алхимия","Плотницкое дело","Бой","Зачарование","Фермерство","Рыбалка","Лесорубство","Горное дело","Создание рун","Общение","Приручение","Охота","Катакомбы","Прохождение подземелий"};
        Style label=Style.EMPTY.withColor(0x55ff55).withInsertion("skill label");
        for(int n=0;n<en.length;n++)for(String level:List.of("I","XIX","XX","L","0","19","50","100")){
            String source=en[n]+" "+level,target=ru[n]+" "+level;var c=Component.literal(en[n]+" ").setStyle(label);
            for(int k=0;k<level.length();k++)c.append(Component.literal(level.substring(k,k+1)).setStyle(Style.EMPTY.withColor(0x550000+k*32).withInsertion("level "+k)));
            check(db.translate(source).equals(target),"plain "+source);var next=SkyRuClient.component(c);
            for(int repeat=0;repeat<5;repeat++){check(next.getString().equals(target),"joined heading "+next.getString());next=SkyRuClient.component(next);}
            check(plain(SkyRuClient.sequence(c.getVisualOrderText())).equals(target),"sequence heading");
            var a=styles(c);var z=styles(next);for(int k=0;k<level.length();k++)check(a.get(source.length()-level.length()+k).equals(z.get(target.length()-level.length()+k)),"each level glyph untouched");
            check(z.getFirst().equals(label),"label style/action");check(c.getString().equals(source),"immutable");
            var msg=new GuiMessage(1,c,null,GuiMessageSource.PLAYER,null);check(SkyRuClient.npcChat(msg)==msg,"player message unchanged");
        }
        for(String border:List.of("","༺ ","✦ ")){
            var c=Component.literal(border).setStyle(Style.EMPTY.withColor(0x999999)).append(Component.literal("Your Skills").setStyle(label)).append(Component.literal(" ✦").setStyle(Style.EMPTY.withColor(0x999999)));
            check(SkyRuClient.component(c).getString().equals(border+"Ваши навыки ✦"),"decorated title");
        }
        for(String key:List.of("Carpentry XIX","Carpentry")){
            var dir=Path.of(args[0]).resolve("custom-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой навык\n");var own=new OfflineDictionary(dir);flag("dictionary",own);
            check(own.translate(key).equals("Мой навык"),"override key");check(!SkyRuClient.component(Component.literal("Carpentry XIX")).getString().equals("Плотницкое дело XIX"),"authored rewrite yields");
        }
        flag("dictionary",db);flag("enabled",false);var input=Component.literal("Carpentry XIX");check(SkyRuClient.component(input)==input,"disabled");
        System.out.println("PASS: skill headings "+checks+" assertions; 14 skills, eight level forms, decorative title, per-glyph styles, repeated render, whole-player-chat and overrides");
    }
}
