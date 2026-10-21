import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import net.minecraft.client.multiplayer.chat.*;

public class TestCalendarLabels {
    private static int checks;
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component c){var a=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int k=0;k<Character.charCount(p);k++)a.add(s);return true;});return a;}
    private static void verify(Component c,String target){
        var next=SkyRuClient.component(c);check(next.getString().equals(target),"calendar component: "+next.getString());
        check(plain(SkyRuClient.sequence(c.getVisualOrderText())).equals(target),"calendar sequence");
        for(int repeat=0;repeat<5;repeat++){next=SkyRuClient.component(next);check(next.getString().equals(target),"repeat");}
        var a=styles(c);var z=styles(next);var source=c.getString();int at=0;
        for(int k=0;k<source.length();k++)if(Character.isDigit(source.charAt(k))){at=target.indexOf(source.charAt(k),at);check(at>=0,"numeric glyph retained");check(a.get(k).equals(z.get(at)),"numeric glyph style/action retained");at++;}
        var msg=new GuiMessage(1,c,null,GuiMessageSource.PLAYER,null);check(SkyRuClient.npcChat(msg)==msg,"whole player chat untouched");
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        Style label=Style.EMPTY.withColor(0x55ffff).withInsertion("calendar label");
        for(String event:List.of(" Election Over!"," Election Booth Opens"," New Year Celebration"," Hoppity's Hunt"," Spooky Festival"," Season of Jerry"))for(String count:List.of("1","2","3","11","432","999999"))for(String suffix:List.of("st","nd","rd","th")){
            var c=Component.empty();for(int k=0;k<count.length();k++)c.append(Component.literal(count.substring(k,k+1)).setStyle(Style.EMPTY.withColor(0x550000+k*32).withInsertion("digit "+k)));
            c.append(Component.literal(suffix).setStyle(label));c.append(Component.literal(event).setStyle(label));String source=c.getString(),ordinal=switch(event){case " Hoppity's Hunt"->"-я";case " Spooky Festival"," Season of Jerry"->"-й";default->"-е";};String target=count+ordinal+switch(event){case " Election Booth Opens"->" выборы: начало голосования";case " New Year Celebration"->" празднование Нового года";case " Hoppity's Hunt"->" охота Хоппити";case " Spooky Festival"->" Жуткий фестиваль";case " Season of Jerry"->" сезон Джерри";default->" выборы завершены!";};
            check(db.translate(source).equals(target),"ordinal");verify(c,target);check(c.getString().equals(source),"immutable title");
        }
        for(String start:List.of("Starts in: ","Ends in: "))for(String time:List.of("3d 20h 58m 43s","0s","12h 3s","999999d 2m")){
            var c=Component.literal(start).setStyle(label);StringBuilder target=new StringBuilder(start.startsWith("Starts")?"Начнётся через: ":"Завершится через: ");
            int digit=0;for(String part:time.split(" ")){if(target.charAt(target.length()-1)!=' ')target.append(' ');String number=part.substring(0,part.length()-1),unit=part.substring(part.length()-1);
                for(int k=0;k<number.length();k++)c.append(Component.literal(number.substring(k,k+1)).setStyle(Style.EMPTY.withColor(0x440000+(digit++)*8).withInsertion("time digit "+digit)));
                c.append(Component.literal(unit).setStyle(Style.EMPTY.withColor(0xffaa00).withClickEvent(new ClickEvent.RunCommand("/unit-"+unit))));
                target.append(number).append(switch(unit){case "d"->" дн";case "h"->" ч";case "m"->" мин";default->" с";});if(!part.equals(time.substring(time.lastIndexOf(' ')+1)))c.append(Component.literal(" ").setStyle(label));
            }
            check(c.getString().equals(start+time),"fixture spacing");verify(c,target.toString());check(db.translate(c.getString()).equals(target.toString()),"plain timer");
            var z=styles(SkyRuClient.component(c));check(z.getFirst().equals(label),"label action");check(z.getLast().equals(styles(c).getLast()),"unit action");
        }
        var mayor=List.<Component>of(Component.literal("Calendar"),Component.literal("The ").append(Component.literal("Mayor").setStyle(label)).append(Component.literal(" is selected until the next")),Component.literal("election ends!"));
        var ru=TooltipTranslation.components(mayor,false);check(ru.get(1).getString().equals("Выбранный мэр остаётся в должности"),"mayor first row");check(ru.get(2).getString().equals("до завершения следующих выборов!"),"mayor last row");
        check(styles(ru.get(1)).get(ru.get(1).getString().indexOf("мэр")).equals(label),"mayor semantic style");
        var seq=TooltipTranslation.styled(mayor.stream().map(Component::getVisualOrderText).toList(),false);check(plain(seq.get(1)).equals(ru.get(1).getString()),"mayor sequence");
        for(String source:List.of("Starts in: 2h 3d","Starts in: 2s 3s","Starts in: 1w","Thu Oct 15 2026 18:15:00","2026-10-15 18:15:04 GMT+07:00"))check(db.translate(source).equals(source),"invalid timer/timestamp preserved: "+db.translate(source));
        for(String key:List.of("432nd Election Over!","Election Over!","Starts in: 3d 20h 58m 43s","Starts in:")){
            var dir=Path.of(args[0]).resolve("custom-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой календарь\n");var own=new OfflineDictionary(dir);flag("dictionary",own);
            check(own.translate(key).equals("Мой календарь"),"override final");String source=key.contains("Election")?"432nd Election Over!":"Starts in: 3d 20h 58m 43s";
            check(SkyRuClient.component(Component.literal(source)).getString().equals(own.translate(source)),"renderer yields to override policy");
        }
        flag("dictionary",db);flag("enabled",false);var c=Component.literal("432nd Election Over!");check(SkyRuClient.component(c)==c,"disabled");
        System.out.println("PASS: calendar labels "+checks+" assertions; screenshot title/timer/mayor, synthetic ordinal/unit styles, idempotence, overrides, whole player chat and dates");
    }
}
