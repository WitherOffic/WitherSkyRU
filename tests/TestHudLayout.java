import ru.wither.skyru.HudTextFit;
import java.util.List;

/** Pure policy tests: no Minecraft/Fabric initialization or server connection. */
public class TestHudLayout {
    private static int checks;
    private static void check(boolean value) { if (!value) throw new AssertionError("check " + checks); checks++; }
    public static void main(String[] args) {
        check(HudTextFit.compact("Драгоценный камень: 1,276 (+22)").equals("Самоцвет: 1,276 (+22)"));
        check(HudTextFit.compact("Пыль драгоценных камней: 0").equals("Пыль самоцветов: 0"));
        check(HudTextFit.compact("Шахтёрские задания").equals("Шахт. задания"));
        check(HudTextFit.compact("Титан на жилах у скалы 0.0%").equals("Титан на жилах у скалы 0.0%"));
        check(HudTextFit.scale(0, 50) == 1f);
        check(HudTextFit.eligibleFrames(List.of("de.hysky.skyblocker.skyblock.tabhud.widget.PowderWidget#render")));
        check(HudTextFit.eligibleFrames(List.of("me.owdding.customscoreboard.core.CustomScoreboardRenderer#onRender")));
        check(HudTextFit.eligibleFrames(List.of("at.hannibal2.skyhanni.utils.render.RenderUtils#draw")));
        check(!HudTextFit.eligibleFrames(List.of("net.minecraft.client.gui.ChatComponent#render", "at.hannibal2.skyhanni.utils.render.RenderUtils#draw")));
        check(!HudTextFit.eligibleFrames(List.of("net.minecraft.client.gui.screens.inventory.InventoryScreen#render", "at.hannibal2.skyhanni.utils.render.RenderUtils#draw")));
        check(!HudTextFit.eligibleFrames(List.of("at.hannibal2.skyhanni.utils.render.RenderUtils#drawTooltip")));
        check(!HudTextFit.eligibleFrames(List.of("other.mod.Renderer#draw")));
        for (int oldWidth = 1; oldWidth <= 500; oldWidth++) {
            for (int newWidth : new int[]{0, 1, oldWidth, oldWidth + 1, oldWidth * 3}) {
                float scale = HudTextFit.scale(oldWidth, newWidth);
                check(scale > 0 && scale <= 1);
                check(newWidth * scale <= oldWidth + 0.001f);
                if (newWidth <= oldWidth) check(scale == 1f);
            }
        }
        System.out.println("PASS: HUD policy " + checks + " assertions");
    }
}
