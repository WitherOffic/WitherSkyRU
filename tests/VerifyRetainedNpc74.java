import java.nio.file.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Same retained numeric ownership fixture run directly on delivered73 and final74. */
public final class VerifyRetainedNpc74 {
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);var translator=new ChatNotificationTranslator(db);
        var records=JsonParser.parseString(Files.readString(Path.of(args[1],"NPC_RETAINED_RUNTIME_0.15.74.json"))).getAsJsonObject().getAsJsonArray("records");
        for(var raw:records){var row=raw.getAsJsonObject();var input=Component.literal(row.get("source").getAsString());var result=translator.translateNpcRequest(input,GuiMessageSource.SYSTEM_SERVER,false);if(!result.getString().equals(row.get("existing_target").getAsString()))throw new AssertionError("retained source no longer owned by existing numeric handler");}
        System.out.println("PASS: retained NPC74 numeric ownership "+records.size()+" exact source/target;story expansion not involved");
    }
}
