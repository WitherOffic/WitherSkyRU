import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import net.minecraft.client.multiplayer.chat.*;

public class TestDungeonUiLabels {
    private static int checks;
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component c){var a=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int k=0;k<Character.charCount(p);k++)a.add(s);return true;});return a;}
    private static void verify(Component source,String target){
        var result=SkyRuClient.component(source);check(result.getString().equals(target),"joined UI label: "+result.getString()+" != "+target);check(plain(SkyRuClient.sequence(source.getVisualOrderText())).equals(target),"sequence UI label");
        for(int repeat=0;repeat<5;repeat++){check(SkyRuClient.component(result).getString().equals(target),"idempotence");result=SkyRuClient.component(result);}
        var a=styles(source);var z=styles(result);String s=source.getString();int at=0;
        for(int k=0;k<s.length();k++)if(Character.isDigit(s.charAt(k))){at=target.indexOf(s.charAt(k),at);check(at>=0,"level/floor retained");check(a.get(k).equals(z.get(at)),"numeric style/action retained");at++;}
        var msg=new GuiMessage(1,source,null,GuiMessageSource.PLAYER,null);check(SkyRuClient.npcChat(msg)==msg,"whole player chat original");
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        String[] en={"Archer","Berserk","Healer","Mage","Tank"},ru={"Лучник","Берсерк","Целитель","Маг","Танк"};Style label=Style.EMPTY.withColor(0x55ff55).withInsertion("class label");
        for(int n=0;n<5;n++)for(int level=1;level<=50;level++){
            String number=Integer.toString(level),s=en[n]+" Level "+number,t=ru[n]+": уровень "+number;var c=Component.literal(en[n]+" Level ").setStyle(label);
            for(int k=0;k<number.length();k++)c.append(Component.literal(number.substring(k,k+1)).setStyle(Style.EMPTY.withColor(0x660000+k*16).withInsertion("tier "+k)));
            check(db.translate(s).equals(t),"plain class level");verify(c,t);check(styles(SkyRuClient.component(c)).getFirst().equals(label),"class semantic style");check(c.getString().equals(s),"immutable class label");
        }
        Style floor=Style.EMPTY.withColor(0xffaa00).withClickEvent(new ClickEvent.RunCommand("/unchanged-floor"));
        for(String mode:List.of("F","M"))for(int level=1;level<=7;level++){
            String id=mode+level,s="Catacombs ("+id+") RNG Meter",t="Катакомбы ("+id+") — счётчик гарантированной добычи";
            var c=Component.literal("Catacombs (").setStyle(label).append(Component.literal(id).setStyle(floor)).append(Component.literal(") RNG Meter").setStyle(label));
            check(db.translate(s).equals(t),"concrete source regex expansion: "+db.translate(s));verify(c,t);check(styles(SkyRuClient.component(c)).get(t.indexOf(id)).equals(styles(c).get(s.indexOf(id))),"F/M floor identifier/action with inherited parent style");
        }
        for(String key:List.of("Archer Level 10","Archer","Catacombs (F1) RNG Meter","Catacombs","RNG Meter")){
            var dir=Path.of(args[0]).resolve("custom-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой заголовок\n");var own=new OfflineDictionary(dir);flag("dictionary",own);String s=key.contains("Archer")?"Archer Level 10":"Catacombs (F1) RNG Meter";
            check(own.translate(key).equals("Мой заголовок"),"override final");check(SkyRuClient.component(Component.literal(s)).getString().equals(own.translate(s)),"formatter yields to override");
        }
        flag("dictionary",db);flag("enabled",false);var c=Component.literal("Archer Level 10");check(SkyRuClient.component(c)==c,"disabled");
        System.out.println("PASS: Dungeon UI labels "+checks+" assertions; 250 frozen class levels, 14 concrete floor-meter expansions (12 repository patterns, two extra synthetic), numeric and F/M styles, overrides/chat guards");
    }
}
