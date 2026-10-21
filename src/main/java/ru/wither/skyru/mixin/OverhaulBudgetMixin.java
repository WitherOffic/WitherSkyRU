package ru.wither.skyru.mixin;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import ru.wither.skyru.OverhaulTextBudget;

/** Optional exact-version bridge: preserve vendor truncation and reserved score width. */
@Pseudo
@Mixin(targets="me.jfenn.scoreboardoverhaul.common.hud.HudRenderer",remap=false)
public abstract class OverhaulBudgetMixin {
    @Unique private int witherskyru$nameWidth;
    // Inspected 1.4.3 bytecode: local 11 = rowWidth - 12 - scoreTextWidth.
    @ModifyVariable(method="drawScoreEntry(Lme/jfenn/scoreboardoverhaul/api/IDrawService;Lme/jfenn/scoreboardoverhaul/common/data/ScoreInfo;III)V",
        at=@At("STORE"),index=11,require=1,remap=false)
    private int witherskyru$captureNameWidth(int width) { witherskyru$nameWidth=width;return width; }

    @Redirect(method="drawScoreEntry(Lme/jfenn/scoreboardoverhaul/api/IDrawService;Lme/jfenn/scoreboardoverhaul/common/data/ScoreInfo;III)V",
        at=@At(value="INVOKE",target="Lme/jfenn/scoreboardoverhaul/api/IDrawService;drawText(Lnet/minecraft/network/chat/Component;IIIZ)V",ordinal=1),
        require=1,remap=false)
    private void witherskyru$drawName(@Coerce Object service,Component text,int x,int y,int color,boolean shadow) {
        OverhaulTextBudget.draw(service,text,x,y,color,shadow,witherskyru$nameWidth);
    }
}
