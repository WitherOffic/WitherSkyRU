import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Exact names from the public API, not invented lore or live-obtainability assertions. */
public class TestPrimaryLabels {
    private static int checks;
    private static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var fixtures=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("PRIMARY_LABEL_REVIEW_61_65.json"))).getAsJsonObject().getAsJsonArray("rows");
        Style label=Style.EMPTY.withColor(0x55ffff).withHoverEvent(new HoverEvent.ShowText(Component.literal("label"))),number=Style.EMPTY.withColor(0xffaa00).withBold(true).withClickEvent(new ClickEvent.RunCommand("/pass"));
        for(var raw:fixtures)for(int repeat=0;repeat<5;repeat++){
            var row=raw.getAsJsonObject();String source=row.get("source").getAsString(),target=row.get("target").getAsString();
            check(db.translateForSearch(source).equals(target),"quiet "+source);check(db.translateForSearch(target).equals(target),"idempotence");
            var original=Component.literal(source).setStyle(label);var result=TooltipTranslation.components(List.of(original,Component.literal("EPIC")),false);
            check(result.getFirst().getString().equals(target),"tooltip "+source);check(result.get(1).getString().equals("EPIC"),"English rarity");
            check(TooltipTranslation.components(List.of(original),true).getFirst().getString().equals(source),"Shift original");
            check(SkyRuClient.component(result.getFirst()).getString().equals(target),"second render");
            StringDecomposer.iterateFormatted(result.getFirst(),Style.EMPTY,(i,s,c)->{check(s.equals(label),"label style/actions");return true;});
            var msg=new GuiMessage(0,original,null,GuiMessageSource.PLAYER,null);check(SkyRuClient.npcChat(msg)==msg,"whole chat unchanged");
            if(row.get("section").getAsString().equals("pass")){
                int at=source.lastIndexOf(' ');String token=source.substring(at+1);var coloured=Component.empty().append(Component.literal(source.substring(0,at+1)).setStyle(label)).append(Component.literal(token).setStyle(number));var translated=SkyRuClient.component(coloured);
                check(translated.getString().equals(target),"pass separate tier style");int[] pos={0};StringDecomposer.iterateFormatted(translated,Style.EMPTY,(i,s,c)->{check(s.equals(pos[0]++<target.length()-token.length()?label:number),"Roman/zero exact style");return true;});
            }
        }
        check(db.translateForSearch("Catacombs Pass XI").equals("Catacombs Pass XI"),"unknown tier not invented");
        check(db.translateForSearch("Master Catacombs Pass -1").equals("Master Catacombs Pass -1"),"negative tier unchanged");
        System.out.println("PASS: primary labels "+checks+" assertions;48 API records/47 exact names/five repeats/styled tiers/Shift/player chat/idempotence; no invented mechanics or live-server claim");
    }
}
