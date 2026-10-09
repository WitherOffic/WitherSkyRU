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
        if (witherskyru$collectingTranslated || source == null) return;
        boolean hud = HudTextFit.currentPanel();
        if (!hud && !TooltipTextFit.currentPanel()) return;
        var translated = SkyRuClient.sequence(source, hud);
        if (translated == source) return;
        var font = Minecraft.getInstance().font;
        int oldWidth = font.width(source), newWidth = font.width(translated);
        float scale = HudTextFit.scale(oldWidth, newWidth);
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
