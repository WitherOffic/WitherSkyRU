import java.util.List;
import ru.wither.skyru.*;

/** API-independent policy regressions introduced by the 26.2 GUI/HUD split. */
public final class TestMigration262 {
    private static int checks;
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }
    public static void main(String[] args) {
        String hud = "net.minecraft.client.gui.Hud#displayScoreboardSidebar";
        check(RenderContext.classify(List.of(hud)).hud(), "new vanilla sidebar recognized");
        check(HudTextFit.eligibleFrames(List.of(hud)), "new vanilla sidebar fit policy");
        for (String protectedFrame : List.of(
                "net.minecraft.client.gui.components.ChatComponent#extractRenderState",
                "net.minecraft.client.gui.components.EditBox#extractWidgetRenderState",
                "net.minecraft.client.gui.components.DebugScreenOverlay#extractLines")) {
            check(RenderContext.classify(List.of(protectedFrame, hud)).protectedText(), "protection: " + protectedFrame);
        }
        check(!RenderContext.classify(List.of("net.minecraft.client.gui.screens.inventory.InventoryScreen#extractRenderState",hud)).hud(), "inventory not fixed-width HUD");
        check(!RenderContext.classify(List.of("net.minecraft.client.gui.components.ChatComponent#extractRenderState",hud)).hud(), "chat not fixed-width HUD");
        check(!RenderContext.classify(List.of("net.minecraft.client.gui.Hud#extractHotbar")).hud(), "not all HUD methods classified as sidebar");
        check(CompatibilityMixinPlugin.supportedRrvVersion("8.11.1+26.2"), "inspected RRV enabled");
        check(!CompatibilityMixinPlugin.supportedRrvVersion("8.11.2+26.2"), "unknown RRV not guessed");
        check(!CompatibilityMixinPlugin.supportedRrvVersion("8.11.1+26.3"), "wrong Minecraft not guessed");
        check(CompatibilityMixinPlugin.supportedSkyRecipesVersion("0.5.13"), "SkyRecipes index retained");
        check(CompatibilityMixinPlugin.supportedSkyblockerVersion("6.11.0+26.2"), "inspected Skyblocker enabled");
        check(CompatibilityMixinPlugin.supportedOverhaul151Version("1.5.1+mc26.2"), "inspected Overhaul enabled");
        check(CompatibilityMixinPlugin.supportedCustomScoreboardVersion("1.13.2"), "inspected CustomScoreboard enabled");
        check(!CompatibilityMixinPlugin.supportedCustomScoreboardVersion("1.13.3"), "unknown layout not guessed");
        System.out.println("PASS: migration262 " + checks + " GUI/HUD classification and bounded compatibility checks; not a game test");
    }
}
