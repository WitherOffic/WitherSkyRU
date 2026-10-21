import java.util.*;
import java.nio.file.*;
import ru.wither.skyru.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.network.chat.*;

/** Queue allowlist plus drawing context and exact player-name boundary checks. */
public class TestPlayerTextProtection {
    private static int checks;
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); checks++; }
    private static void flag(String name, Object value) throws Exception {
        var f = SkyRuClient.class.getDeclaredField(name); f.setAccessible(true); f.set(null, value);
    }
    public static void main(String[] args) throws Exception {
        var db = new OfflineDictionary(Path.of(args[0])); db.setRecordUnknown(false);
        flag("dictionary", db); flag("enabled", true); flag("onlyHypixel", false);
        flag("npcDialogues", new NpcDialogueTranslator());
        flag("chatNotifications", new ChatNotificationTranslator(db));
        Set<String> names = Set.of("Wither_101", "Pickaxe", "Thor", "COMMON");
        for (int repeat = 0; repeat < 5; repeat++) {
            for (String frame : List.of("extractRenderState", "render", "lambda$extractRenderState$2"))
                check(UserInputText.protectedDisplayFrames(List.of("net.minecraft.client.gui.components.ChatComponent#" + frame)), "chat glyph drawing must bypass dictionary");
            check(UserInputText.protectedDisplayFrames(List.of("net.minecraft.client.gui.components.EditBox#extractWidgetRenderState")), "input protected");
            check(!UserInputText.protectedDisplayFrames(List.of("net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip#extractText")), "tooltip not suppressed");
            check(!UserInputText.protectedDisplayFrames(List.of("net.minecraft.client.gui.components.StringWidget#extractWidgetRenderState")), "ordinary label not suppressed");
            check(UserInputText.protectedDisplayFrames(List.of("net.minecraft.client.gui.components.PlayerTabOverlay#getNameForDisplay")), "tab name protected");
            for (String name : names) for (String text : List.of(name, "[MVP+] " + name, "§a" + name + "§r", "Name: " + name, name + "'s island"))
                check(PlayerNames.contains(text, names), "exact original player token " + text);
            for (String text : List.of("Thorns", "Wither_1010", "xWither_101", "Wither_101_x", "UNCOMMON", "Diamond PickaxeHolder"))
                check(!PlayerNames.contains(text, names), "not a player token " + text);
            for (String text : List.of("pickaxe", "Pickaxe", "Wither_101", "[MVP+] Wither_101: Pickaxe", "/msg Wither_101 Pickaxe")) {
                Component raw = Component.literal(text);
                for (GuiMessageSource channel : List.of(GuiMessageSource.PLAYER, GuiMessageSource.SYSTEM_SERVER)) {
                    var message = new GuiMessage(0, raw, null, channel, null);
                    check(SkyRuClient.npcChat(message) == message, "ordinary chat not translated " + text);
                    check(raw.getString().equals(text), "source immutable");
                }
            }
            var npc = new GuiMessage(0, Component.literal("[NPC] Jeff: These don't just sit pretty, they grow stronger the more you use 'em!"), null, GuiMessageSource.SYSTEM_SERVER, null);
            check(SkyRuClient.npcChat(npc).content().getString().contains("чем больше ими пользуешься"), "NPC queue translation retained");
            var notification = new GuiMessage(0, Component.literal("Teleported you to The Barn!"), null, GuiMessageSource.SYSTEM_SERVER, null);
            check(SkyRuClient.npcChat(notification).content().getString().equals("Перемещение: Амбар!"), "notification queue translation retained");
        }
        System.out.println("PASS: player text protection " + checks + " assertions; offline queue/context/name boundaries, not a live chat test");
    }
}
