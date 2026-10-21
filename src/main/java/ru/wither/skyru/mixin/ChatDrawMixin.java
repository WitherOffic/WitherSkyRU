package ru.wither.skyru.mixin;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.wither.skyru.TechnicalText;

/** The existing protected chat policy, before generic per-line caller inspection. */
@Mixin(targets={"net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess",
    "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess"})
public abstract class ChatDrawMixin {
    @Redirect(method="handleMessage(IFLnet/minecraft/util/FormattedCharSequence;)Z",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V"),require=1)
    private static void witherskyru$originalChat(ActiveTextCollector collector,TextAlignment alignment,int x,int y,
        ActiveTextCollector.Parameters parameters,FormattedCharSequence source){
        try(var scope=TechnicalText.open()){collector.accept(alignment,x,y,parameters,source);}
    }
}
