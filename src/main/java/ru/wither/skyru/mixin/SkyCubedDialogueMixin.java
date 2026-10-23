package ru.wither.skyru.mixin;

import ru.wither.skyru.SkyCubedDialogueBridge;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

/** Pinned bytecode only. Work after matching, never mutate the received event or NPC ID. */
@Pseudo
@Mixin(targets="tech.thatgravyboat.skycubed.features.overlays.dialogue.DialogueOverlay",remap=false)
public abstract class SkyCubedDialogueMixin {
    @ModifyArgs(method="onChatReceived$lambda$0",at=@At(value="INVOKE",
        target="Lkotlin/TuplesKt;to(Ljava/lang/Object;Ljava/lang/Object;)Lkotlin/Pair;"),require=1,remap=false)
    private static void witherskyru$dialogueCopy(Args args){
        args.set(1,SkyCubedDialogueBridge.dialogue(args.get(0),args.get(1)));
    }
    @ModifyExpressionValue(method="onChatReceived$lambda$1",at=@At(value="INVOKE",
        target="Ltech/thatgravyboat/skyblockapi/utils/regex/component/Destructured;component1()Lnet/minecraft/network/chat/Component;"),require=1,remap=false)
    private static Component witherskyru$optionCopies(Component group){return SkyCubedDialogueBridge.options(group);}

    @ModifyArg(method="createMainDisplay",at=@At(value="INVOKE",ordinal=0,
        target="Lme/owdding/lib/displays/Displays;component$default(Lme/owdding/lib/displays/Displays;Lnet/minecraft/network/chat/Component;ILkotlin/jvm/functions/Function0;ZILjava/lang/Object;)Lme/owdding/lib/displays/Display;"),index=1,require=1,remap=false)
    private Component witherskyru$displayName(Component name){return SkyCubedDialogueBridge.name(name);}
}
