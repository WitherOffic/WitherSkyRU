package ru.wither.skyru.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wither.skyru.SkyRuClient;

/** Supplement the native predicate only; input, suggestions, IDs and submission stay native. */
@Pseudo
@Mixin(targets="de.hysky.skyblocker.skyblock.searchoverlay.SearchOverManager",remap=false)
public abstract class SkyblockerSearchMixin {
    @Shadow protected static String search;
    @Inject(method="lambda$updateSuggestions$0(Ljava/lang/String;)Z",at=@At("RETURN"),
        cancellable=true,require=1,remap=false)
    private static void witherskyru$russianOverlayName(String original,CallbackInfoReturnable<Boolean> cir){
        if(!cir.getReturnValueZ()&&SkyRuClient.overlaySearchMatch(original,search))cir.setReturnValue(true);
    }
}
