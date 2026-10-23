package ru.wither.skyru.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wither.skyru.SkyRuSettingsScreen;

/** Small header button; never replaces vanilla options or requires Mod Menu/Fabric API. */
@Mixin(OptionsScreen.class)
public abstract class OptionsMenuMixin extends Screen {
    @Unique private Button witherskyru$settings;
    protected OptionsMenuMixin() { super(Component.empty()); }
    @Inject(method = "init()V", at = @At("RETURN"), require = 1)
    private void witherskyru$addSettings(CallbackInfo ci) {
        witherskyru$settings = addRenderableWidget(Button.builder(Component.literal("SkyblockRPG_RU"),
            button -> minecraft.gui.setScreen(new SkyRuSettingsScreen((Screen)(Object)this)))
            .bounds(Math.max(4, width - 134), 4, 130, 20).build());
    }
    @Inject(method = "repositionElements()V", at = @At("RETURN"), require = 1)
    private void witherskyru$moveSettings(CallbackInfo ci) {
        if (witherskyru$settings != null) witherskyru$settings.setX(Math.max(4, width - 134));
    }
}
