package ru.wither.skyru;

import java.util.List;

/** Presentation-only HUD policy: no changes to Font metrics or server data. */
public final class HudTextFit {
    private static final StackWalker WALKER = StackWalker.getInstance();
    private HudTextFit() {}

    public static boolean currentPanel() {
        if (!SkyRuClient.hudTextFitActive()) return false;
        // Called only for drawn text. Never retained between frames or worlds.
        return WALKER.walk(frames -> eligibleFrames(frames.limit(48)
            .map(f -> f.getClassName() + "#" + f.getMethodName()).toList()));
    }

    public static boolean eligibleFrames(List<String> frames) {
        boolean panel = false;
        for (String frame : frames) {
            String lower = frame.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("chatcomponent#") || lower.contains(".chat.")
                || lower.contains("tooltip") || lower.contains(".screens.")) return false;
            panel |= frame.startsWith("me.owdding.customscoreboard.core.")
                || frame.startsWith("de.hysky.skyblocker.skyblock.tabhud.")
                || frame.startsWith("at.hannibal2.skyhanni.")
                || (frame.startsWith("net.minecraft.client.gui.Gui#")
                    && (lower.contains("scoreboard") || lower.contains("sidebar")))
                || lower.contains("scoreboardoverhaul");
        }
        return panel;
    }

    public static float scale(int originalWidth, int translatedWidth) {
        if (originalWidth <= 0 || translatedWidth <= originalWidth) return 1f;
        // Small margin covers rounding and shadow; never enlarges short lines.
        return Math.min(1f, Math.max(1, originalWidth - 1) / (float) translatedWidth);
    }

    /** Short HUD terminology only; digits, percentages and arbitrary prose stay intact. */
    public static String compact(String text) {
        return text.replace("Пыль драгоценных камней", "Пыль самоцветов")
            .replace("пыль драгоценных камней", "пыль самоцветов")
            .replace("Драгоценные камни", "Самоцветы")
            .replace("драгоценные камни", "самоцветы")
            .replace("Драгоценный камень", "Самоцвет")
            .replace("драгоценный камень", "самоцвет")
            .replace("Шахтёрские задания", "Шахт. задания")
            .replace("шахтёрские задания", "шахт. задания");
    }
}
