package skycubedsmoke.mixin;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public abstract class SkyCubedSmokeMixin117 {
    @Inject(method="runTick(Z)V",at=@At("RETURN"),require=1)
    private void skycubedSmoke$frame(boolean advance,CallbackInfo ci){skycubedsmoke.SkyCubedSmoke117.frame();}
}
