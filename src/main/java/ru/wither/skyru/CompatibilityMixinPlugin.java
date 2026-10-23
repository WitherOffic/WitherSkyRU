package ru.wither.skyru;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

/** Foreign internals are enabled only for an explicitly inspected version. */
public final class CompatibilityMixinPlugin implements IMixinConfigPlugin {
    public static boolean supportedSkyCubed(String version,String api,String overlayHash,String publisherHash){
        return "1.10.0".equals(version)&&"4.2.41".equals(api)
            &&"486147e525b113246e9806e6601b509966ea33868044e477602db34b15550906".equals(overlayHash)
            &&"4237b364e266db0e8f886303e8be5a6b095d3dfa5a128ba4cb43231caf014bd0".equals(publisherHash);
    }
    private static String classHash(net.fabricmc.loader.api.ModContainer mod,String path){
        try(var in=java.nio.file.Files.newInputStream(mod.findPath(path).orElseThrow())){
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));
        }catch(Exception unavailable){return "";}
    }
    public static boolean supportedRrvVersion(String version) {
        return "8.10.9+26.1.2".equals(version) || "8.11.1+26.2".equals(version);
    }

    public static boolean supportedSkyRecipesVersion(String version) { return "0.5.13".equals(version); }
    public static boolean supportedSkyblockerVersion(String version) { return "6.10.4+26.1.2".equals(version) || "6.11.0+26.2".equals(version); }
    public static boolean supportedOverhaulVersion(String version) { return "1.4.3+mc26.1".equals(version); }
    public static boolean supportedOverhaul151Version(String version) { return "1.5.1+mc26.1".equals(version) || "1.5.1+mc26.2".equals(version); }

    public static boolean supportedCustomScoreboardVersion(String version) { return "1.12.14-2".equals(version) || "1.13.2".equals(version); }

    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if(mixin.endsWith(".SkyCubedDialogueMixin")){
            var loader=FabricLoader.getInstance();var sky=loader.getModContainer("skycubed");var api=loader.getModContainer("skyblock-api");
            if(sky.isEmpty()||api.isEmpty())return false;
            return supportedSkyCubed(sky.get().getMetadata().getVersion().getFriendlyString(),api.get().getMetadata().getVersion().getFriendlyString(),
                classHash(sky.get(),"tech/thatgravyboat/skycubed/features/overlays/dialogue/DialogueOverlay.class"),
                classHash(api.get(),"tech/thatgravyboat/skyblockapi/impl/events/MiscEventHandler.class"));
        }
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
