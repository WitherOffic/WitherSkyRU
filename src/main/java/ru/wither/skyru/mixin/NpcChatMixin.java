package ru.wither.skyru.mixin;

import ru.wither.skyru.SkyRuClient;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Whitelisted NPC/notification display queue only; original history/log text stays intact. */
@Mixin(ChatComponent.class)
public abstract class NpcChatMixin {
    @ModifyVariable(
        method="addMessageToDisplayQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V",
        at=@At("HEAD"), argsOnly=true, ordinal=0, require=1)
    private GuiMessage witherskyru$npcDisplayMessage(GuiMessage original) {
        return SkyRuClient.npcChat(original);
    }
}
