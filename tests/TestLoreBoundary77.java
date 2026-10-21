import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;

/** A short reviewed rule may not silently translate a broken longer paragraph. */
public final class TestLoreBoundary77 {
    private static int checks;
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    private static String text(List<Component> rows){return String.join("\n",rows.stream().map(Component::getString).toList());}
    private static void eq(String actual,String expected){if(!actual.equals(expected))throw new AssertionError(actual+" != "+expected);checks++;}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        for(String value:List.of("0.5","1.25","12"))for(int seed=1;seed<=2;seed++){
            var style=Style.EMPTY.withColor(0x123450+seed).withInsertion("current77 "+seed).withClickEvent(new ClickEvent.SuggestCommand("/unchanged77"));
            var source=List.<Component>of(Component.literal("Regen +"+value+" Vitality every time you").setStyle(style),Component.literal("hit a mob.").setStyle(style));
            eq(text(TooltipTranslation.components(source,false)),"Даёт +"+value+" жизненной силы при каждом\nпопадании по мобу.");
            for(String tail:List.of("(1s Cooldown). UNKNOWN","unpublished continuation","(unexpected cooldown)")){
                var broken=new ArrayList<Component>(source);broken.add(Component.literal(tail).setStyle(style));
                eq(text(TooltipTranslation.components(broken,false)),text(broken.stream().map(SkyRuClient::component).toList()));
            }
            var terminated=new ArrayList<Component>(source);terminated.add(Component.literal(""));terminated.add(Component.literal("UNKNOWN NEXT PARAGRAPH"));
            var result=TooltipTranslation.components(terminated,false);eq(text(result.subList(0,2)),"Даёт +"+value+" жизненной силы при каждом\nпопадании по мобу.");eq(result.get(3).getString(),"UNKNOWN NEXT PARAGRAPH");
            eq(text(source),"Regen +"+value+" Vitality every time you\nhit a mob.");
        }
        System.out.println("PASS: LoreBoundary77 "+checks+" checks; short complete/blank-terminated paragraphs allowed, broken continuations fall back; source immutable");
    }
}
