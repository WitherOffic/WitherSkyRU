package smoke;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import ru.wither.skyru.*;

/** Test-only game driver. Not packaged in WitherSkyRU. No accounts, worlds or servers. */
public final class ClientSmoke262 {
    private static int stage, frames, textFrames;
    private static long started = System.nanoTime();
    private static boolean finished;
    private static boolean translatedDraw;
    private static boolean translatedTooltip;
    public static void observe(net.minecraft.util.FormattedCharSequence source, int x, int y) {
        if (stage != 1) return;
        var plain=new StringBuilder(); source.accept((i,s,c)->{plain.appendCodePoint(c);return true;});
        if (plain.toString().contains("кирка")) { translatedDraw=true; if(x!=20 || y!=30) translatedTooltip=true; }
    }
    private static void check(boolean value, String why) { if (!value) throw new AssertionError(why); }
    private static Button button(Screen screen, String label) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
            .filter(b -> b.getMessage().getString().equals(label)).findFirst().orElseThrow();
    }
    public static void frame() {
        if (finished) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (System.nanoTime()-started > 180_000_000_000L) throw new AssertionError("Client smoke timed out at stage " + stage);
            Screen screen = mc.gui.screen();
            if (stage == 0 && screen != null && screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) {
                screen.onClose(); return;
            }
            if (stage == 0 && screen instanceof TitleScreen && mc.gui.overlay() == null) {
                check(SkyRuClient.active(), "offline smoke settings activated translation");
                check(SkyRuClient.component(Component.literal("Diamond Pickaxe")).getString().contains("кирка"), "real dictionary loaded");
                mc.gui.setScreen(new ProbeScreen()); stage=1; frames=0;
            } else if (stage == 1 && ++frames > 30) {
                check(textFrames >= 10, "actual GUI frames rendered");
                check(translatedDraw, "actual text draw received translated glyph sequence");
                check(translatedTooltip, "actual tooltip draw received translated glyph sequence");
                verifyCustomScoreboardLayout();
                mc.gui.setScreen(new OptionsScreen(screen, mc.options, false)); stage=2; frames=0;
            } else if (stage == 2 && ++frames > 5) {
                button(screen, "WitherSkyRU").onPress(null); stage=3; frames=0;
            } else if (stage == 3 && ++frames > 5) {
                check(screen instanceof SkyRuSettingsScreen, "injected options button opened real settings");
                Button font = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(b -> b.getMessage().getString().startsWith("Шрифт подсказок:")).findFirst().orElseThrow();
                font.onPress(null);
                button(screen,"Сохранить").onPress(null);
                check(TooltipFont.choice() == TooltipFont.Choice.DEFAULT, "saved setting applied immediately");
                check(mc.gui.screen() instanceof OptionsScreen, "settings returned to parent via new GUI API");
                stage=4; frames=0;
            } else if (stage == 4 && ++frames > 5) {
                button(screen,"WitherSkyRU").onPress(null); stage=5; frames=0;
            } else if (stage == 5 && ++frames > 5) {
                check(screen instanceof SkyRuSettingsScreen, "settings reopened");
                screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(b -> b.getMessage().getString().startsWith("Шрифт подсказок:")).findFirst().orElseThrow().onPress(null);
                button(screen,"Отмена").onPress(null);
                check(TooltipFont.choice() == TooltipFont.Choice.DEFAULT, "cancel retains committed settings");
                check(!TechnicalText.active(), "technical render scope cleaned");
                finish(mc, true, "Real 26.2 OpenGL client: title, translated GUI/tooltip frames, options injection, settings save/reopen/cancel. Not multiplayer, world rendering, Vulkan or FPS.");
            }
        } catch(Throwable error) { error.printStackTrace(); finish(mc,false,error.toString()); }
    }
    private static void verifyCustomScoreboardLayout() throws Exception {
        if(!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("customscoreboard"))return;
        var type=Class.forName("me.owdding.customscoreboard.core.CustomScoreboardRenderer");
        var field=type.getDeclaredField("display"); field.setAccessible(true); Object original=field.get(null);
        var layout=net.minecraft.client.gui.layouts.LayoutElement.class;
        Object probe=java.lang.reflect.Proxy.newProxyInstance(layout.getClassLoader(),new Class<?>[]{layout},(p,m,a)->switch(m.getName()){
            case "getX" -> 100; case "getWidth" -> 200; case "getY" -> 0; case "getHeight" -> 20; default -> null;
        });
        try {
            field.set(null,probe);
            var bridge=Class.forName("ru.wither.skyru.CustomScoreboardBudget").getDeclaredMethod("available",net.minecraft.client.gui.TextAlignment.class,int.class,int.class,org.joml.Matrix3x2fc.class);
            bridge.setAccessible(true);
            check((Integer)bridge.invoke(null,net.minecraft.client.gui.TextAlignment.LEFT,120,20,new org.joml.Matrix3x2f())==180,"actual CustomScoreboard1.13.2 field and bounds bridge");
            System.out.println("WITHERSKYRU_CUSTOM_SCOREBOARD262_LAYOUT_PASS; synthetic bounds, original live field restored");
        } finally { field.set(null,original); }
    }
    private static void finish(Minecraft mc, boolean success, String detail) {
        finished=true;
        try { Files.writeString(Path.of("client-smoke-result.txt"), (success?"PASS":"FAIL")+"\n"+detail+"\nrendered_probe_frames="+textFrames+"\n"); }
        catch(Exception ex) { ex.printStackTrace(); }
        System.out.println("WITHERSKYRU_CLIENT_SMOKE_262 " + (success?"PASS ":"FAIL ") + detail);
        mc.stop();
    }
    private static final class ProbeScreen extends Screen {
        ProbeScreen(){super(Component.literal("WitherSkyRU 26.2 test-only screen"));}
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
            super.extractRenderState(graphics,mouseX,mouseY,tick);
            graphics.text(font, Component.literal("Diamond Pickaxe").withStyle(Style.EMPTY.withColor(0x55ffff)),20,30,-1);
            graphics.text(font, "Normal",20,45,-1);
            graphics.setComponentTooltipForNextFrame(font, java.util.List.of(Component.literal("Diamond Pickaxe"),Component.literal("Click to view!")),60,70);
            textFrames++;
        }
    }
}
