package ru.wither.skyru.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wither.skyru.SkyRuClient;

@Mixin(EntityRenderer.class)
public abstract class MinionNameTagMixin {
    @Inject(method="extractRenderState",at=@At("RETURN"))
    private void witherskyru$minionNameTag(Entity entity,EntityRenderState state,float partialTick,CallbackInfo ci){
        state.nameTag=SkyRuClient.minionNameTag(state.nameTag,entity instanceof Player);
    }
}
