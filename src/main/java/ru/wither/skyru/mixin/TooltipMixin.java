package ru.wither.skyru.mixin;

import ru.wither.skyru.SkyRuClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.List;
import java.util.ArrayList;

/** Item lore/hover text; never mutates the actual ItemStack or click handlers. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class TooltipMixin {
    @ModifyVariable(
        method={
            "setTooltipForNextFrame(Ljava/util/List;II)V",
            "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/resources/Identifier;)V",
            "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;IIZLnet/minecraft/resources/Identifier;)V"
        },
        at=@At("HEAD"), argsOnly=true, ordinal=0, require=3)
    private List<net.minecraft.util.FormattedCharSequence> witherskyru$styledTooltips(List<net.minecraft.util.FormattedCharSequence> text) {
        if(text==null || !SkyRuClient.active()) return text;
        List<net.minecraft.util.FormattedCharSequence> out=new ArrayList<>(text.size());
        boolean changed=false;
        for(var row:text) { var next=SkyRuClient.sequence(row); out.add(next); changed |= row!=next; }
        return changed ? out : text;
    }

    @ModifyVariable(
        method={
            "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;)V",
            "setComponentTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/resources/Identifier;)V"
        },
        at=@At("HEAD"), argsOnly=true, ordinal=0, require=2)
    private List<Component> witherskyru$tooltips(List<Component> text) {
        if(text==null || !SkyRuClient.active()) return text;
        List<Component> out=new ArrayList<>(text.size());
        boolean changed=false;
        for(Component row:text) { Component newRow=SkyRuClient.component(row); out.add(newRow); if(row!=newRow) changed=true; }
        return changed ? out : text;
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
