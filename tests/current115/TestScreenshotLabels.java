import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import net.minecraft.client.multiplayer.chat.*;

public class TestScreenshotLabels {
    private static int checks;
    private static final Style LABEL=Style.EMPTY.withColor(0xffaa00).withInsertion("label");
    private static final Style TERM=Style.EMPTY.withColor(0x55ffff).withClickEvent(new ClickEvent.RunCommand("/warp-original"));
    private static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);checks++;}
    private static void flag(String key,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,v);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component c){var a=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int k=0;k<Character.charCount(p);k++)a.add(s);return true;});return a;}
    private static void verify(Component source,String target){
        var c=SkyRuClient.component(source);check(c.getString().equals(target),source.getString()+" => "+c.getString());
        check(plain(SkyRuClient.sequence(source.getVisualOrderText())).equals(target),"sequence");
        for(int n=0;n<5;n++){c=SkyRuClient.component(c);check(c.getString().equals(target),"repeat");}
        var player=new GuiMessage(1,source,null,GuiMessageSource.PLAYER,null);check(SkyRuClient.npcChat(player)==player,"whole chat unchanged");
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        String[] en={"Early Spring","Spring","Late Spring","Early Summer","Summer","Late Summer","Early Autumn","Autumn","Late Autumn","Early Winter","Winter","Late Winter"};
        String[] ru={"Начало весны","Весна","Конец весны","Начало лета","Лето","Конец лета","Начало осени","Осень","Конец осени","Начало зимы","Зима","Конец зимы"};
        for(int n=0;n<en.length;n++)for(int day=1;day<=31;day++){
            String digits=""+day,suffix=day>=11&&day<=13?"th":switch(day%10){case 1->"st";case 2->"nd";case 3->"rd";default->"th";};
            var source=Component.literal(en[n]+" ").setStyle(LABEL);
            for(int k=0;k<digits.length();k++)source.append(Component.literal(digits.substring(k,k+1)).setStyle(TERM.withInsertion("digit-"+k)));
            source.append(Component.literal(suffix).setStyle(LABEL));String target=ru[n]+", "+digits;
            verify(source,target);check(db.translate(source.getString()).equals(target),"plain date");
            var s=styles(SkyRuClient.component(source));int at=target.indexOf(digits);
            for(int k=0;k<digits.length();k++)check(s.get(at+k).equals(TERM.withInsertion("digit-"+k)),"numeric date style/action");
        }
        var talk=Component.empty().append(Component.literal("Talk to ").setStyle(LABEL)).append(Component.literal("Lone Adventurer").setStyle(TERM));
        verify(talk,"Поговорите с Одиноким искателем приключений");
        check(styles(SkyRuClient.component(talk)).get(13).equals(TERM),"quest target original action");
        var fiesta=Component.empty().append(Component.literal("Mining").setStyle(LABEL)).append(Component.literal(" ").setStyle(LABEL)).append(Component.literal("Fiesta").setStyle(TERM));
        verify(fiesta,"Горнодобывающая фиеста");check(styles(SkyRuClient.component(fiesta)).getLast().equals(TERM),"event style/action");
        for(String[] pair:new String[][]{{"Normal.","Обычный режим."},{"Gems:","Самоцветы:"},{"Marauder","Мародёр"},{"Benediction","Благословение"},
            {"Nest","Гнездо"},{"Void","Пустота"},{"Skull","Череп"},{"Tomb","Гробница"},{"Dwarven","Дварфские"},{"Hollows","Кристальные пещеры"},{"Trapper","Охотник"},{"Top","Вершина"},{"Springs","Источники"},{"Murkwater","Тёмное озеро"},{"Cave","Пещера"},{"Home","Дом"}}){
            var source=Component.literal(pair[0]).setStyle(TERM);verify(source,pair[1]);check(styles(SkyRuClient.component(source)).stream().allMatch(TERM::equals),"warp action kept");
        }
        for(String[] pair:new String[][]{{"True Dwarth","Истинный дворф"},{"Park Enjoyer","Любитель Парка"},{"Park Enthusiast","Знаток Парка"}}){
            var c=Component.empty().append(Component.literal("Ability: ").setStyle(LABEL)).append(Component.literal(pair[0]).setStyle(TERM));verify(c,"Способность: "+pair[1]);
            check(styles(SkyRuClient.component(c)).getLast().equals(TERM),"ability style");
        }
        var gems=Component.empty().append(Component.literal("Gems: ").setStyle(LABEL)).append(Component.literal("75").setStyle(TERM));verify(gems,"Самоцветы: 75");check(styles(SkyRuClient.component(gems)).getLast().equals(TERM),"currency count");
        for(String value:List.of("2026-10-11 21:48:43 GMT+07:00","Sun 11.10.2026 21:48:43","/warp dwarven","Late Summer 0th","Late Summer 32nd"))check(db.translate(value).equals(value),"timestamp/ID/bounds: "+db.translate(value));
        // Nickname safety belongs to the player/GUI context, not bare dictionary lookup.
        for(String value:List.of("Wither_101","Pickaxe","Mining Fiesta","Talk to Lone Adventurer","Home","/msg Wither_101 pickaxe")){
            var message=new GuiMessage(1,Component.literal(value),null,GuiMessageSource.PLAYER,null);
            check(SkyRuClient.npcChat(message)==message,"all ordinary player chat unchanged");
        }
        check(PlayerNames.contains("[MVP+] Wither_101",Set.of("Wither_101")),"original nick token");
        for(String[] pair:new String[][]{{"True Dwarth","Истинный дворф"},{"Throwing Axe","Метательный топор"}}){
            var c=Component.empty().append(Component.literal("Ability: ").setStyle(LABEL)).append(Component.literal(pair[0]).setStyle(TERM)).append(Component.literal("  ").setStyle(LABEL)).append(Component.literal("RIGHT CLICK").setStyle(TERM.withBold(true)));
            verify(c,"Способность: "+pair[1]+" — ПКМ");check(styles(SkyRuClient.component(c)).getLast().equals(TERM.withBold(true)),"original click-label style");
        }
        var dir=Path.of(args[0]).resolve("custom");Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),"Talk to Lone Adventurer\tМоя цель\nLate Summer 29th\tМоя дата\n");
        flag("dictionary",new OfflineDictionary(dir));verify(Component.literal("Talk to Lone Adventurer"),"Моя цель");verify(Component.literal("Late Summer 29th"),"Моя дата");
        flag("dictionary",db);flag("enabled",false);var off=Component.literal("Late Summer 29th");check(SkyRuClient.component(off)==off,"disabled");
        System.out.println("PASS: Screenshot labels "+checks+" assertions; 372 synthetic season/day dates, quest/event/warp labels, original commands/chat/timestamps, overrides and styles");
    }
}
