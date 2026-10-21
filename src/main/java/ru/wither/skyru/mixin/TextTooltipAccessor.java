package ru.wither.skyru.mixin;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read a display-only tooltip copy, never ItemStack or server components. */
@Mixin(ClientTextTooltip.class)
public interface TextTooltipAccessor {
    @Accessor("text") FormattedCharSequence witherskyru$text();
}
