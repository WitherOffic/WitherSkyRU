package ru.wither.skyru.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wither.skyru.SkyRuClient;

/** Optional, version-guarded search bridge. No dependency on RRV classes. */
@Pseudo
@Mixin(targets = "cc.cassian.rrv.common.overlay.itemlist.view.ItemFilters", remap = false)
public abstract class RrvSearchMixin {
    @Redirect(method = "defaultFilter(Ljava/lang/String;)Ljava/util/List;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getDisplayName()Lnet/minecraft/network/chat/Component;"),
        require = 1, remap = false)
    private static Component witherskyru$searchName(ItemStack stack, String query) {
        return SkyRuClient.searchName(stack.getDisplayName(), query);
    }

    @Inject(method = "getMatch(Ljava/lang/String;Lnet/minecraft/network/chat/Component;)Ljava/lang/Integer;",
        at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void witherskyru$tooltipSearch(String query, Component text, CallbackInfoReturnable<Integer> cir) {
        int match = SkyRuClient.russianSearchMatch(text, query);
        if (match != 0) cir.setReturnValue(match);
    }
}
