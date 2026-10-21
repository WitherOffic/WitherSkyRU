import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import ru.wither.skyru.*;

/** Diagnostic of reviewed power-stone lines split into coloured runs. */
public class AuditPowerStyles {
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);
        for(var pair:Map.of("dictionary",(Object)db,"enabled",true,"onlyHypixel",false).entrySet()){
            var f=SkyRuClient.class.getDeclaredField(pair.getKey());f.setAccessible(true);f.set(null,pair.getValue());
        }
        var items=JsonParser.parseString(Files.readString(Path.of(args[1]))).getAsJsonArray();
        for(var raw:items){
            var item=raw.getAsJsonObject();String power=item.getAsJsonArray("lore").get(2).getAsString();
            String name=power.substring(0,power.indexOf(" power."));
            var rows=List.<Component>of(Component.literal("Combine x9 of this item at the"),Component.literal("Thaumaturgist in the Hub to unlock the"),
                Component.empty().append(Component.literal(name).setStyle(Style.EMPTY.withColor(0xaa00aa)))
                    .append(Component.literal(" power.").setStyle(Style.EMPTY.withColor(0xaaaaaa))));
            System.out.println(name+" | "+db.translate(name)+" | "+TooltipTranslation.components(rows,false).get(2).getString());
        }
    }
}
