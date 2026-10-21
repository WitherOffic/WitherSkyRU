package ru.wither.skyru.mixin;

import ru.wither.skyru.HudTextFit;
import ru.wither.skyru.TooltipTextFit;
import ru.wither.skyru.SkyRuClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** StringWidget HUD rendering in 26.1.2 bypasses GuiGraphicsExtractor.text(). */
@Mixin(targets="net.minecraft.client.gui.GuiGraphicsExtractor$RenderingTextCollector")
public abstract class HudCollectorMixin {
    @Unique private boolean witherskyru$collectingTranslated;

    @Inject(method="accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V",
        at=@At("HEAD"), cancellable=true, require=1)
    private void witherskyru$fitCollectedText(TextAlignment alignment, int x, int y,
        ActiveTextCollector.Parameters parameters, FormattedCharSequence source, CallbackInfo ci) {
        if (witherskyru$collectingTranslated || source == null || ru.wither.skyru.TechnicalText.active() || TooltipTextFit.renderingPrepared()) return;
        if(!SkyRuClient.active())return;
        if(SkyRuClient.numericSequence(source))return;
        try(var context=ru.wither.skyru.RenderContext.open()){
        boolean hud = HudTextFit.currentPanel();
        boolean tooltip = TooltipTextFit.currentPanel();
        if (!hud && !tooltip) return;
        boolean fit = SkyRuClient.hudTextFitActive() && HudTextFit.fixedWidth(hud, tooltip);
        var translated = SkyRuClient.sequence(source, hud);
        if (translated == source) return;
        if(!fit){
            // Without fitting, retain native alignment/pose/width handling. No
            // extra Font.width pass or copied matrix is needed for a tooltip.
            witherskyru$collectingTranslated=true;
            try{((ActiveTextCollector)(Object)this).accept(alignment,x,y,parameters,translated);}
            finally{witherskyru$collectingTranslated=false;}
            ci.cancel();return;
        }
        var font = Minecraft.getInstance().font;
        int oldWidth = font.width(source), newWidth = font.width(translated);
        int budget = HudTextFit.currentBudget(alignment, x, oldWidth, parameters.pose());
        if (!HudTextFit.canFit(budget, newWidth)){translated=SkyRuClient.narrowHud(source,translated);newWidth=font.width(translated);}
        if (!HudTextFit.canFit(budget, newWidth)) return;
        float scale = HudTextFit.scale(budget, newWidth);
        int left = alignment.calculateLeft(x, (int)Math.ceil(newWidth * scale));
        // Copy the pose. Never scale the caller's matrix or replace hover/click styles.
        var pose = new Matrix3x2f(parameters.pose()).translate(left, y).scale(scale, scale);
        witherskyru$collectingTranslated = true;
        try {
            ((ActiveTextCollector)(Object)this).accept(TextAlignment.LEFT, 0, 0,
                parameters.withPose(pose), translated);
        } finally {
            witherskyru$collectingTranslated = false;
        }
        ci.cancel();
        }
    }
}
