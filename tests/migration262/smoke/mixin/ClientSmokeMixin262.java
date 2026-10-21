package smoke.mixin;

import smoke.ClientSmoke262;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Test-only driver; exact real end-of-frame hook, absent from production JAR. */
@Mixin(Minecraft.class)
public abstract class ClientSmokeMixin262 {
    @Inject(method="runTick(Z)V",at=@At("RETURN"),require=1)
    private void smoke$frame(boolean advance, CallbackInfo ci){ClientSmoke262.frame();}
}
