package ru.wither.skyru;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.ChatFormatting;
import ru.wither.skyru.mixin.ContainerScreenAccessor;

/** Hold Shift: preserve only the original item title, not the lore or stored stack. */
public final class OriginalItemTitle {
    private OriginalItemTitle() {}

    public static boolean held() {
        if (!SkyRuClient.active()) return false;
        try {
            Minecraft mc = Minecraft.getInstance();
            return mc != null && mc.screen instanceof AbstractContainerScreen<?>
                && (InputConstants.isKeyDown(mc.getWindow(), InputConstants.KEY_LSHIFT)
                    || InputConstants.isKeyDown(mc.getWindow(), InputConstants.KEY_RSHIFT));
        } catch (RuntimeException ignored) { return false; }
    }

    private record OriginalSequence(FormattedCharSequence source) implements FormattedCharSequence {
        @Override public boolean accept(FormattedCharSink sink) { return source.accept(sink); }
    }

    public static boolean protectedSequence(FormattedCharSequence source) { return source instanceof OriginalSequence; }
    public static FormattedCharSequence protect(FormattedCharSequence source) {
        return source == null || protectedSequence(source) ? source : new OriginalSequence(source);
    }
    public static FormattedCharSequence unwrap(FormattedCharSequence source) {
        return source instanceof OriginalSequence original ? original.source() : source;
    }

    /** Direct custom tooltip renderers may bypass the standard tooltip list hooks. */
    public static boolean hoveredName(String source) {
        if (source == null || !held()) return false;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (!(mc.screen instanceof ContainerScreenAccessor access)) return false;
            var slot = access.witherskyru$hoveredSlot();
            return slot != null && slot.hasItem()
                && java.util.Objects.equals(ChatFormatting.stripFormatting(source),
                    ChatFormatting.stripFormatting(slot.getItem().getHoverName().getString()));
        } catch (RuntimeException ignored) { return false; }
    }

    public static boolean hoveredName(FormattedCharSequence source) {
        if (!held()) return false;
        StringBuilder plain = new StringBuilder();
        source.accept((index, style, point) -> { plain.appendCodePoint(point); return true; });
        return hoveredName(plain.toString());
    }
}
