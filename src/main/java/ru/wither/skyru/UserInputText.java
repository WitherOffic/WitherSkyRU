package ru.wither.skyru;

import java.util.List;

/** Preserve input and already-filtered chat; global drawing must not translate player text. */
public final class UserInputText {
    private UserInputText() {}
    public static boolean rendering() {
        return TechnicalText.active()||RenderContext.current().protectedText();
    }
    public static boolean protectedDisplayFrames(List<String> frames) {
        if (editingFrames(frames)) return true;
        // NPC/notification translation happens once in the display queue. Translating
        // chat glyph runs again can change ordinary words, usernames and commands.
        return frames.stream().anyMatch(frame -> frame.startsWith("net.minecraft.client.gui.components.ChatComponent#")
            || frame.startsWith("net.minecraft.client.gui.components.PlayerTabOverlay#getNameForDisplay")
            || frame.startsWith("net.minecraft.client.gui.components.DebugScreenOverlay#")
            || frame.startsWith("net.minecraft.client.gui.components.debugchart."));
    }
    public static boolean editingFrames(List<String> frames) {
        for (String frame : frames) {
            if (frame.startsWith("net.minecraft.client.gui.components.EditBox#")
                || frame.startsWith("net.minecraft.client.gui.components.MultiLineEditBox#")
                || frame.startsWith("net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen#extract")) return true;
        }
        return false;
    }
}
