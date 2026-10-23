package publicsmoke.mixin;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public abstract class PublicSmokeMixin117 {
    @Inject(method="tick()V",at=@At("RETURN"),require=1)
    private void publicSmoke$tick(CallbackInfo ci){publicsmoke.PublicSmoke117.frame();}
}
