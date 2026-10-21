package ru.wither.skyru.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import ru.wither.skyru.TechnicalText;

/** Exact 26.2 technical line draw; retain original source, metrics and colours. */
@Mixin(DebugScreenOverlay.class)
public abstract class DebugTextMixin {
    @Redirect(method="extractLines(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Ljava/util/List;Z)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V"),require=1)
    private static void witherskyru$debugText(GuiGraphicsExtractor graphics,Font font,String text,int x,int y,int color,boolean shadow){
        try(var context=TechnicalText.open()){graphics.text(font,text,x,y,color,shadow);}
    }
}
