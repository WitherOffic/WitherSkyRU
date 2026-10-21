package ru.wither.skyru;

import java.lang.reflect.Field;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.layouts.LayoutElement;

/** Read-only optional bridge: live inner layout bounds, never modifies the other mod. */
final class CustomScoreboardBudget {
    private static volatile boolean attempted;
    private static Field display;
    private CustomScoreboardBudget() {}
    static int available(TextAlignment alignment, int x, int originalWidth, org.joml.Matrix3x2fc pose) {
        try {
            if (!attempted) init();
            if (display != null && display.get(null) instanceof LayoutElement layout)
                return HudTextFit.posedPanelBudget(layout.getX(), layout.getWidth(), x, alignment, originalWidth, pose);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unsupported) {
            // API/version mismatch: keep the existing bounded readable policy.
        }
        return originalWidth;
    }
    private static synchronized void init() {
        if (attempted) return;
        try {
            boolean supported = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("customscoreboard")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString().equals("1.12.14-2")).orElse(false);
            if (!supported) return;
            var type = Class.forName("me.owdding.customscoreboard.core.CustomScoreboardRenderer", false,
                CustomScoreboardBudget.class.getClassLoader());
            var field = type.getDeclaredField("display");
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && field.trySetAccessible()) display = field;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unsupported) {
            display = null;
        } finally { attempted = true; }
    }
}
