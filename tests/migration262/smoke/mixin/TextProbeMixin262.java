package smoke.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import smoke.ClientSmoke262;

/** Observer only. Does not replace the production translation or graphics call. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class TextProbeMixin262 {
    @Inject(method="text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",at=@At("HEAD"),require=1)
    private void smoke$observe(Font font, FormattedCharSequence source, int x,int y,int color,boolean shadow,CallbackInfo ci){
        ClientSmoke262.observe(source,x,y);
    }
}
