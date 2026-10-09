package ru.wither.skyru;

import java.util.List;

/** Fits only drawn tooltip copies. Font.width() and item data remain untouched. */
public final class TooltipTextFit {
    private static final StackWalker WALKER = StackWalker.getInstance();
    private TooltipTextFit() {}
    public static boolean currentPanel() {
        if (!SkyRuClient.tooltipTextFitActive()) return false;
        return WALKER.walk(frames -> eligibleFrames(frames.limit(48)
            .map(f -> f.getClassName() + "#" + f.getMethodName()).toList()));
    }
    public static boolean eligibleFrames(List<String> frames) {
        boolean tooltip = false;
        for (String frame : frames) {
            String lower = frame.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("chatcomponent#") || lower.contains(".chat.")) return false;
            String method = lower.substring(lower.lastIndexOf('#') + 1);
            tooltip |= lower.contains("tooltip") &&
                (method.contains("extract") || method.contains("draw") || method.contains("render"));
        }
        return tooltip;
    }
}
