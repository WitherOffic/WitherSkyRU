package ru.wither.skyru.mixin;

import ru.wither.skyru.SkyRuClient;
import ru.wither.skyru.HudTextFit;
import ru.wither.skyru.TooltipTextFit;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Standard widget captions, only while connected to Hypixel. */
@Mixin(AbstractWidget.class)
public abstract class WidgetMixin {
    @Inject(method="getMessage()Lnet/minecraft/network/chat/Component;",at=@At("RETURN"),cancellable=true,require=1)
    private void witherskyru$widget(CallbackInfoReturnable<Component> cir) {
        if(!SkyRuClient.active())return;
        try(var context=ru.wither.skyru.RenderContext.open()){
        // HUD widgets measure this message before drawing. Keep their original metrics;
        // the final text/collector hook translates and fits only the visible copy.
        if (HudTextFit.currentPanel() || TooltipTextFit.currentPanel()) return;
        Component original=cir.getReturnValue();
        Component next=SkyRuClient.component(original);
        if(next!=original) cir.setReturnValue(next);
        }
    }
}
