package ru.wither.skyru.mixin;

import ru.wither.skyru.SkyRuClient;
import ru.wither.skyru.HudTextFit;
import ru.wither.skyru.TooltipTextFit;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Unique;

/** Display-time hooks targeting the real Minecraft 26.1.2 API; dev hooks pending runtime QA. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiTextMixin {
    @Unique private boolean witherskyru$drawingTranslated;

    @Inject(
        method="text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
        at=@At("HEAD"), cancellable=true, require=1)
    private void witherskyru$styledText(net.minecraft.client.gui.Font font,
        net.minecraft.util.FormattedCharSequence source, int x, int y, int color,
        boolean shadow, CallbackInfo ci) {
        if (witherskyru$drawingTranslated || source == null || ru.wither.skyru.TechnicalText.active() || TooltipTextFit.renderingPrepared()) return;
        if(!SkyRuClient.active())return;
        if(SkyRuClient.numericSequence(source))return;
        try(var context=ru.wither.skyru.RenderContext.open()){
        boolean hud = HudTextFit.currentPanel();
        boolean fit = SkyRuClient.hudTextFitActive() && HudTextFit.fixedWidth(hud, TooltipTextFit.currentPanel());
        var translated = SkyRuClient.sequence(source, hud);
        if (translated == source) return;
        GuiGraphicsExtractor graphics = (GuiGraphicsExtractor)(Object)this;
        float scale=1f;
        if(fit){
            int budget=HudTextFit.currentBudget(net.minecraft.client.gui.TextAlignment.LEFT,x,font.width(source),graphics.pose());
            int width=font.width(translated);
            if(!HudTextFit.canFit(budget,width)){translated=SkyRuClient.narrowHud(source,translated);width=font.width(translated);}
            if(!HudTextFit.canFit(budget,width))return;
            scale=HudTextFit.scale(budget,width);
        }
        witherskyru$drawingTranslated = true;
        boolean pushed = false;
        try {
            if (scale < 1f) {
                graphics.pose().pushMatrix();
                pushed = true;
                graphics.pose().translate(x, y);
                graphics.pose().scale(scale, scale);
                graphics.text(font, translated, 0, 0, color, shadow);
            } else {
                graphics.text(font, translated, x, y, color, shadow);
            }
        } finally {
            if (pushed) graphics.pose().popMatrix();
            witherskyru$drawingTranslated = false;
        }
        ci.cancel();
        }
    }

    @ModifyVariable(
        method="centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V",
        at=@At("HEAD"), argsOnly=true, ordinal=0, require=1)
    private net.minecraft.util.FormattedCharSequence witherskyru$centeredSequence(net.minecraft.util.FormattedCharSequence source) {
        return SkyRuClient.sequenceOutsideHud(source);
    }

    @ModifyVariable(
        method="centeredText(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V",
        at=@At("HEAD"), argsOnly=true, ordinal=0, require=1)
    private String witherskyru$centeredString(String source) {
        return SkyRuClient.textOutsideHud(source);
    }
    @ModifyVariable(
        method="text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
        at=@At("HEAD"), argsOnly=true, ordinal=0, require=1)
    private Component witherskyru$normalText(Component source) {
        return SkyRuClient.componentOutsideHud(source);
    }

    @ModifyVariable(
        method="text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",
        at=@At("HEAD"), argsOnly=true, ordinal=0, require=1)
    private String witherskyru$normalString(String source) {
        return SkyRuClient.textOutsideHud(source);
    }

    @ModifyVariable(
        method="centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V",
        at=@At("HEAD"), argsOnly=true, ordinal=0, require=1)
    private Component witherskyru$centered(Component source) {
        return SkyRuClient.componentOutsideHud(source);
    }

    @ModifyVariable(
        method="textWithWordWrap(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/FormattedText;IIII)V",
        at=@At("HEAD"), argsOnly=true, ordinal=0, require=1)
    private net.minecraft.network.chat.FormattedText witherskyru$wrapped(net.minecraft.network.chat.FormattedText source) {
        return source instanceof Component comp ? SkyRuClient.component(comp) : source;
    }

    @ModifyVariable(
        method="textWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V",
        at=@At("HEAD"), argsOnly=true, ordinal=0, require=1)
    private Component witherskyru$backdrop(Component source) { return SkyRuClient.component(source); }
}
