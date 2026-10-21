package ru.wither.skyru.mixin;

import ru.wither.skyru.SkyRuClient;
import ru.wither.skyru.TooltipTextFit;
import ru.wither.skyru.TooltipTranslation;
import ru.wither.skyru.OriginalItemTitle;
import ru.wither.skyru.TooltipFont;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Unique;
import java.util.List;

/** Item lore/hover text; never mutates the actual ItemStack or click handlers. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class TooltipMixin {
    @Unique private boolean witherskyru$layingOutTooltip;

    @Inject(method="tooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;)V",
        at=@At("HEAD"), cancellable=true, require=1)
    private void witherskyru$readableTooltip(Font font, List<ClientTooltipComponent> rows,
        int x, int y, ClientTooltipPositioner positioner, Identifier background, CallbackInfo ci) {
        if (witherskyru$layingOutTooltip || rows == null || !(SkyRuClient.tooltipTextFitActive() || TooltipFont.active())) return;
        GuiGraphicsExtractor graphics = (GuiGraphicsExtractor)(Object)this;
        var prepared = TooltipTextFit.prepare(font, rows, graphics.guiWidth());
        if (prepared == rows) return;
        witherskyru$layingOutTooltip = true;
        try { graphics.tooltip(font, prepared, x, y, positioner, background); }
        finally { witherskyru$layingOutTooltip = false; }
        ci.cancel();
    }
    @ModifyVariable(
        method={
            "setTooltipForNextFrame(Ljava/util/List;II)V",
            "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/resources/Identifier;)V",
            "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;IIZLnet/minecraft/resources/Identifier;)V"
        },
        at=@At("HEAD"), argsOnly=true, ordinal=0, require=3)
    private List<net.minecraft.util.FormattedCharSequence> witherskyru$styledTooltips(List<net.minecraft.util.FormattedCharSequence> text) {
        return TooltipTranslation.styled(text, OriginalItemTitle.held());
    }

    @ModifyVariable(
        method={
            "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;)V",
            "setComponentTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/resources/Identifier;)V"
        },
        at=@At("HEAD"), argsOnly=true, ordinal=0, require=2)
    private List<Component> witherskyru$tooltips(List<Component> text) {
        return TooltipTranslation.components(text, OriginalItemTitle.held());
    }

    @ModifyVariable(
        method={
            "setTooltipForNextFrame(Lnet/minecraft/network/chat/Component;II)V",
            "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IILnet/minecraft/resources/Identifier;)V"
        },
        at=@At("HEAD"), argsOnly=true, ordinal=0, require=2)
    private Component witherskyru$singleTooltip(Component text) {
        return SkyRuClient.component(text);
    }
}
