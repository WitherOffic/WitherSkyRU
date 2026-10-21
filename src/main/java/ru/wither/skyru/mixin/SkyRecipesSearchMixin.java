package ru.wither.skyru.mixin;

import java.util.BitSet;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wither.skyru.*;

/** Optional bridge for the inspected SkyRecipes 0.5.13 index that bypasses RRV's native filter. */
@Pseudo
@Mixin(targets="com.github.kdgaming0.skyrecipes.core.search.SkyblockSearchIndex",remap=false)
public abstract class SkyRecipesSearchMixin {
    @Shadow @Final private String[] displayNames;
    @Shadow @Final private String[] searchText;
    @Unique private volatile RussianItemNameIndex witherskyru$nameIndex;
    @Unique private OfflineDictionary witherskyru$indexDictionary;

    @Unique private RussianItemNameIndex witherskyru$index(String query) {
        if(!RussianSearchAliases.russianQuery(query)) return null;
        OfflineDictionary dictionary=SkyRuClient.activeSearchDictionary();
        if(dictionary==null) return null;
        // Build only when a Russian query is actually used, after the client dictionary is ready.
        synchronized(this) {
            if(witherskyru$nameIndex==null || witherskyru$indexDictionary!=dictionary) {
                witherskyru$nameIndex=new RussianItemNameIndex(displayNames,dictionary::translateForSearch);
                witherskyru$indexDictionary=dictionary;
            }
            return witherskyru$nameIndex;
        }
    }

    // Hook outside the native cache: disabling/re-enabling translation cannot leave cached Russian matches.
    @Inject(method={"resolveKeyword(Ljava/lang/String;)Ljava/util/BitSet;",
        "resolveKeywordInName(Ljava/lang/String;)Ljava/util/BitSet;"},at=@At("RETURN"),cancellable=true,require=2,remap=false)
    private void witherskyru$russianKeywords(String token, CallbackInfoReturnable<BitSet> cir) {
        RussianItemNameIndex index=witherskyru$index(token);
        if(index==null) return;
        BitSet result=(BitSet)cir.getReturnValue().clone();
        result.or(index.prefix(token));
        cir.setReturnValue(result);
    }

    // Keep the candidate mask produced by native categories/stat/rarity filters and AND/OR/NOT logic.
    @Inject(method="retainPhraseMatches(Ljava/util/BitSet;Ljava/lang/String;)V",at=@At("HEAD"),cancellable=true,require=1,remap=false)
    private void witherskyru$russianPhrase(BitSet candidates, String phrase, CallbackInfo ci) {
        RussianItemNameIndex index=witherskyru$index(phrase);
        if(index==null) return;
        for(int i=candidates.nextSetBit(0);i>=0;i=candidates.nextSetBit(i+1)) {
            String original=searchText[i];
            if((original==null || !original.contains(phrase)) && !index.phrase(i,phrase)) candidates.clear(i);
        }
        ci.cancel();
    }
}
