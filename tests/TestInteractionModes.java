import java.nio.file.Path;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;

/** Shift affects only item title; editable text must never be translated. */
public class TestInteractionModes {
    private static int checks;
    private static void check(String label, boolean ok) { if (!ok) throw new AssertionError(label); checks++; }
    private static String plain(FormattedCharSequence row) {
        StringBuilder out = new StringBuilder(); row.accept((i, s, cp) -> { out.appendCodePoint(cp); return true; }); return out.toString();
    }
    private static void flag(String name, Object value) throws Exception {
        var field = SkyRuClient.class.getDeclaredField(name); field.setAccessible(true); field.set(null, value);
    }
    public static void main(String[] args) throws Exception {
        var db = new OfflineDictionary(Path.of(args[0])); db.setRecordUnknown(false);
        flag("dictionary", db); flag("enabled", true); flag("onlyHypixel", false);
        Style titleStyle = Style.EMPTY.withColor(0xaa00aa).withItalic(true)
            .withClickEvent(new ClickEvent.RunCommand("/viewrecipe END_CHESTPLATE"));
        var title = FormattedCharSequence.forward("Light Ender Chestplate", titleStyle);
        var lore = FormattedCharSequence.forward("Health: +120 (+15)", Style.EMPTY);
        List<FormattedCharSequence> original = List.of(title, lore);
        for (int i = 0; i < 100; i++) {
            var held = TooltipTranslation.styled(original, true);
            check("held title original", plain(held.getFirst()).equals("Light Ender Chestplate"));
            check("lore stays translated", plain(held.get(1)).equals("Здоровье: +120 (+15)"));
            check("draw hook cannot translate protected title", SkyRuClient.sequence(held.getFirst()) == held.getFirst());
            var secondPass = TooltipTranslation.styled(held, true);
            check("overload chaining keeps original title", plain(secondPass.getFirst()).equals("Light Ender Chestplate"));
            var released = TooltipTranslation.styled(held, false);
            check("release restores Russian", plain(released.getFirst()).equals("Лёгкий Эндерский нагрудник"));
            check("input list never changed", original.getFirst() == title && original.get(1) == lore);
        }
        var protectedTitle = OriginalItemTitle.protect(title);
        protectedTitle.accept((i, style, cp) -> { check("original styles remain", style.equals(titleStyle)); return true; });
        check("protect idempotent", OriginalItemTitle.protect(protectedTitle) == protectedTitle);
        check("unwrap exact original", OriginalItemTitle.unwrap(protectedTitle) == title);
        var componentTitle = Component.literal("Epic Void Sword").setStyle(titleStyle);
        var componentLore = Component.literal("Health: +120 (+15)");
        var components = TooltipTranslation.components(List.of(componentTitle, componentLore), true);
        check("component title original identity", components.getFirst() == componentTitle);
        check("component lore translated", components.get(1).getString().equals("Здоровье: +120 (+15)"));
        check("normal component title Russian", TooltipTranslation.components(List.of(componentTitle, componentLore), false)
            .getFirst().getString().equals("Эпический Меч Пустоты"));
        check("empty list supported", TooltipTranslation.styled(List.of(), true).isEmpty());
        check("null supported", TooltipTranslation.styled(null, true) == null);
        check("chat input frame protected", UserInputText.editingFrames(List.of("net.minecraft.client.gui.components.EditBox#extractWidgetRenderState")));
        check("RRV input uses same base", UserInputText.editingFrames(List.of("cc.cassian.rrv.common.overlay.itemlist.view.SearchBar#lambda$new$0", "net.minecraft.client.gui.components.EditBox#extractWidgetRenderState")));
        check("multiline input protected", UserInputText.editingFrames(List.of("net.minecraft.client.gui.components.MultiLineEditBox#extractWidgetRenderState")));
        check("sign input protected", UserInputText.editingFrames(List.of("net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen#extractSignText")));
        check("sign helper shows English originals", UserInputText.editingFrames(List.of("net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen#extractRenderState")));
        check("received chat still translates", !UserInputText.editingFrames(List.of("net.minecraft.client.gui.components.ChatComponent#render")));
        check("regular labels still translate", !UserInputText.editingFrames(List.of("net.minecraft.client.gui.components.StringWidget#extractWidgetRenderState")));
        check("tooltip still translates", !UserInputText.editingFrames(List.of("net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip#extractText")));
        flag("enabled", false);
        check("disabled leaves list intact", TooltipTranslation.styled(original, true) == original);
        System.out.println("PASS: input/Shift modes " + checks + " assertions");
    }
}
