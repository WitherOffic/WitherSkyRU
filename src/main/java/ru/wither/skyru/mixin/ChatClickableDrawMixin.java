package ru.wither.skyru.mixin;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.wither.skyru.TechnicalText;

/** Keep the native clickable text and commands; scope lasts for this exact call only. */
@Mixin(targets="net.minecraft.client.gui.components.ChatComponent$ClickableTextOnlyGraphicsAccess")
public abstract class ChatClickableDrawMixin {
    @Redirect(method="handleMessage(IFLnet/minecraft/util/FormattedCharSequence;)Z",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/util/FormattedCharSequence;)V"),require=1)
    private static void witherskyru$originalClickableChat(ActiveTextCollector collector,TextAlignment alignment,int x,int y,FormattedCharSequence source){
        try(var scope=TechnicalText.open()){collector.accept(alignment,x,y,source);}
    }
}
