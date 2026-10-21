package ru.wither.skyru;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

/** Foreign internals are enabled only for an explicitly inspected version. */
public final class CompatibilityMixinPlugin implements IMixinConfigPlugin {
    public static boolean supportedRrvVersion(String version) {
        return "8.10.9+26.1.2".equals(version);
    }

    public static boolean supportedSkyRecipesVersion(String version) { return "0.5.13".equals(version); }
    public static boolean supportedSkyblockerVersion(String version) { return "6.10.4+26.1.2".equals(version); }
    public static boolean supportedOverhaulVersion(String version) { return "1.4.3+mc26.1".equals(version); }
    public static boolean supportedOverhaul151Version(String version) { return "1.5.1+mc26.1".equals(version); }

    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if(mixin.endsWith(".Overhaul151BudgetMixin"))return FabricLoader.getInstance().getModContainer("scoreboard-overhaul")
            .map(mod->supportedOverhaul151Version(mod.getMetadata().getVersion().getFriendlyString())).orElse(false);
        if(mixin.endsWith(".OverhaulBudgetMixin"))return FabricLoader.getInstance().getModContainer("scoreboard-overhaul")
            .map(mod->supportedOverhaulVersion(mod.getMetadata().getVersion().getFriendlyString())).orElse(false);
        if(mixin.endsWith(".SkyblockerSearchMixin"))return FabricLoader.getInstance().getModContainer("skyblocker")
            .map(mod->supportedSkyblockerVersion(mod.getMetadata().getVersion().getFriendlyString())).orElse(false);
        if (mixin.endsWith(".SkyRecipesSearchMixin")) {
            return FabricLoader.getInstance().getModContainer("skyrecipes")
                .map(mod -> supportedSkyRecipesVersion(mod.getMetadata().getVersion().getFriendlyString()))
                .orElse(false)
                && FabricLoader.getInstance().getModContainer("rrv")
                .map(mod -> supportedRrvVersion(mod.getMetadata().getVersion().getFriendlyString())).orElse(false);
        }
        if (!mixin.endsWith(".RrvSearchMixin")) return true;
        return FabricLoader.getInstance().getModContainer("rrv")
            .map(mod -> supportedRrvVersion(mod.getMetadata().getVersion().getFriendlyString()))
            .orElse(false);
    }
}
