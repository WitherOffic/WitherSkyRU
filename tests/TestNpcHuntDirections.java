import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Trevor primary-source whitelist, not a live test of all hunting states. */
public class TestNpcHuntDirections {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static String text(Component c){var b=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static void retained(Component from,Component to,String old,String translated,boolean perChar){int a=text(from).indexOf(old),b=text(to).indexOf(translated);check(a>=0&&b>=0,"semantic text");var x=styles(from);var y=styles(to);for(int i=0;i<translated.length();i++)check(y.get(b+i).equals(x.get(a+(perChar?i:0))),"effective styles/events");}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);var chat=new ChatNotificationTranslator(db);
        Style body=Style.EMPTY.withColor(0xffffff).withHoverEvent(new HoverEvent.ShowText(Component.literal("prose")));
        Style place=Style.EMPTY.withColor(0xffff55).withClickEvent(new ClickEvent.RunCommand("/original-location"));
        List<String> locations=List.of("Oasis","Mushroom Gorge","Overgrown Mushroom Cave","Desert Settlement","Glowing Mushroom Cave","Desert Mountain");
        for(int repeat=0;repeat<5;repeat++)for(String rarity:List.of("TRACKABLE","UNTRACKABLE","UNDETECTED","ENDANGERED","ELUSIVE"))for(String location:locations){
            var source=Component.literal("[NPC] Trevor: You can find your ").setStyle(body);
            for(int i=0;i<rarity.length();i++)source.append(Component.literal(rarity.substring(i,i+1)).setStyle(Style.EMPTY.withColor(0x550000+i*0x100).withInsertion("rarity "+i)));
            source.append(Component.literal(" animal near the ").setStyle(body)).append(Component.literal(location).setStyle(place)).append(Component.literal(".").setStyle(body));
            String old=text(source),ru=db.translateForSearch(location),expected="[NPC] Trevor: Ищи животное категории «"+rarity+"» возле локации «"+ru+"».";
            var result=chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_SERVER,false);check(text(result).equals(expected),"clear whole direction: "+text(result));check(text(source).equals(old),"source immutable");
            retained(source,result,rarity,rarity,true);retained(source,result,location,ru,false);retained(source,result,"Trevor","Trevor",true);
            check(chat.translateNpcRequest(result,GuiMessageSource.SYSTEM_SERVER,false)==result,"idempotent");
            check(chat.translateNpcRequest(source,GuiMessageSource.PLAYER,false)==source,"player excluded");check(chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_SERVER,true)==source,"signed excluded");
            check(chat.translateNpcRequest(source,GuiMessageSource.SYSTEM_CLIENT,false)==source,"client excluded");
            check(chat.translate(source,GuiMessageSource.SYSTEM_SERVER,false)==source,"ordinary notifications do not rewrite direction");
            var timed=Component.literal("[03:04:05] ").setStyle(place).append(source);check(text(chat.translateNpcRequest(timed,GuiMessageSource.SYSTEM_SERVER,false)).equals("[03:04:05] "+expected),"timestamp retained");
        }
        // Exact primary REGEX-TEST includes a legacy-coloured location.
        var legacy=Component.literal("[NPC] Trevor: You can find your TRACKABLE animal near the §eDesert Mountain.").setStyle(body);
        var result=chat.translateNpcRequest(legacy,GuiMessageSource.SYSTEM_SERVER,false);check(text(result).equals("[NPC] Trevor: Ищи животное категории «TRACKABLE» возле локации «Пустынная гора»."),"primary legacy sample");retained(legacy,result,"Desert Mountain","Пустынная гора",false);
        for(String unknown:List.of("[NPC] Trevor: You can find your RARE animal near the Desert Mountain.","[NPC] Trevor: You can find your TRACKABLE animal near the Wither_101.","[NPC] Wither_101: You can find your TRACKABLE animal near the Desert Mountain.","[VIP] Player: [NPC] Trevor: You can find your TRACKABLE animal near the Desert Mountain.","[NPC] Trevor: You can find your TRACKABLE animal near the Desert Mountain. Kill 10!")){
            var c=Component.literal(unknown);check(chat.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"unknown direction not invented");
        }
        Path custom=Path.of(args[0]).resolve("hunt-custom");Files.createDirectories(custom);String source="[NPC] Trevor: You can find your TRACKABLE animal near the Desert Mountain.";Files.writeString(custom.resolve("overrides.tsv"),source+"\tМои указания\n");
        var override=new ChatNotificationTranslator(new OfflineDictionary(custom));var c=Component.literal(source);check(override.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"user override preserved");
        var mixed=Component.literal("[NPC] Trevor: You can ").setStyle(body).append(Component.literal("find your TRACKABLE animal near the Desert Mountain.").setStyle(place));check(chat.translateNpcRequest(mixed,GuiMessageSource.SYSTEM_SERVER,false)==mixed,"unreviewed prose styles fail closed");
        System.out.println("PASS: NPC hunt directions "+checks+" assertions; five known categories / six primary areas / five repeats / legacy sample / no invented count or action");
    }
}
