import java.nio.file.Path;
import java.util.*;

/** Actual combined RRV/SkyRecipes classes and named ItemStacks. No client entrypoints or network. */
public final class TestSkyRecipesSearchBridge {
    private static void flag(Class<?> client, String name, boolean value) throws Exception {
        var field = client.getDeclaredField(name); field.setAccessible(true); field.set(null,value);
    }
    public static void run(ClassLoader loader, Path sandbox) throws Exception {
        Class.forName("net.minecraft.SharedConstants",true,loader).getMethod("tryDetectVersion").invoke(null);
        Class.forName("net.minecraft.server.Bootstrap",true,loader).getMethod("bootStrap").invoke(null);
        Class<?> stack = Class.forName("net.minecraft.world.item.ItemStack",true,loader);
        Class<?> itemLike = Class.forName("net.minecraft.world.level.ItemLike",true,loader);
        Class<?> items = Class.forName("net.minecraft.world.item.Items",true,loader);
        Class<?> component = Class.forName("net.minecraft.network.chat.Component",true,loader);
        Class<?> components = Class.forName("net.minecraft.core.component.DataComponents",true,loader);
        Class<?> componentType = Class.forName("net.minecraft.core.component.DataComponentType",true,loader);
        Class<?> itemType = Class.forName("net.minecraft.world.item.Item",true,loader);
        Class<?> componentMap = Class.forName("net.minecraft.core.component.DataComponentMap",true,loader);
        Class<?> holder = Class.forName("net.minecraft.core.Holder$Reference",true,loader);
        // Minimal fixture components, not a resource/world reload or a client launch.
        for(String fixtureItem:List.of("FISHING_ROD","DIAMOND_PICKAXE")) {
            Object value=items.getField(fixtureItem).get(null);
            Object reference=itemType.getMethod("builtInRegistryHolder").invoke(value);
            holder.getMethod("bindComponents",componentMap).invoke(reference,componentMap.getField("EMPTY").get(null));
        }
        var literal = component.getMethod("literal",String.class);
        var set = stack.getMethod("set",componentType,Object.class);
        Object customName = components.getField("CUSTOM_NAME").get(null);
        List<Object> stacks = new ArrayList<>();
        String[] names={"Fishing Rod","Rod of Legends","Diamond Pickaxe","Iron Pickaxe"};
        for(int i=0;i<names.length;i++) {
            Object instance = stack.getConstructor(itemLike).newInstance(items.getField(i<2?"FISHING_ROD":"DIAMOND_PICKAXE").get(null));
            set.invoke(instance,customName,literal.invoke(null,names[i])); stacks.add(instance);
        }
        Class<?> registry = Class.forName("com.github.kdgaming0.skyrecipes.core.registry.ItemRegistry",true,loader);
        Class<?> constants = Class.forName("com.github.kdgaming0.skyrecipes.core.registry.ConstantsRegistry",true,loader);
        Object itemRegistry = registry.getConstructor(Collection.class).newInstance(List.of());
        Object constantRegistry = constants.getConstructor(Map.class,Map.class,Set.class,Map.class)
            .newInstance(Map.of(),Map.of(),Set.of(),Map.of());
        Class<?> indexType = Class.forName("com.github.kdgaming0.skyrecipes.core.search.SkyblockSearchIndex",true,loader);
        Object index=indexType.getConstructor(List.class,registry,constants).newInstance(stacks,itemRegistry,constantRegistry);
        Class<?> plugin = Class.forName("com.github.kdgaming0.skyrecipes.rrv.plugin.SkyRecipesClientPlugin",true,loader);
        var searchIndex = plugin.getDeclaredField("searchIndex"); searchIndex.setAccessible(true); searchIndex.set(null,index);
        Class<?> filters=Class.forName("cc.cassian.rrv.common.overlay.itemlist.view.ItemFilters",true,loader);
        var filter=filters.getMethod("filter",String.class);
        for(String query:List.of("удочка","удо","УДОЧКА","Rod","кирка","кирка алмазная","\"алмазная кирка\"")) {
            List<?> results=(List<?>)filter.invoke(null,query);
            int expected=query.toLowerCase(Locale.ROOT).contains("алмаз")?1:2;
            if(results.size()!=expected) throw new AssertionError("Combined RRV + SkyRecipes search: " + query + " => " + results.size() + "; expected " + expected);
            System.out.println("PASS: combined RRV + SkyRecipes query " + query + " => " + results.size());
        }
        if(!((List<?>)filter.invoke(null,"несуществующийZX9921")).isEmpty())
            throw new AssertionError("Unknown combined query matched");
        // Controlled metadata fixture; selection must still be constrained by native SkyRecipes filters.
        var rarityField=indexType.getDeclaredField("rarityIndex"); rarityField.setAccessible(true);
        @SuppressWarnings("unchecked") Map<String,BitSet> rarities=(Map<String,BitSet>)rarityField.get(index);
        rarities.put("common",BitSet.valueOf(new long[]{1}));
        rarities.put("legendary",BitSet.valueOf(new long[]{2}));
        if(((List<?>)filter.invoke(null,"удочка rarity:common")).size()!=1)
            throw new AssertionError("Russian search lost native rarity filter");
        Class<?> category=Class.forName("com.github.kdgaming0.skyrecipes.core.model.SkyblockItemCategory",true,loader);
        var categoryField=indexType.getDeclaredField("categoryIndex"); categoryField.setAccessible(true);
        @SuppressWarnings("unchecked") Map<Object,BitSet> categories=(Map<Object,BitSet>)categoryField.get(index);
        Object fishing=category.getField("FISHING").get(null);
        Object tool=category.getField("TOOL").get(null);
        categories.put(fishing,BitSet.valueOf(new long[]{3})); categories.put(tool,BitSet.valueOf(new long[]{12}));
        var categoryFilter=indexType.getMethod("filter",String.class,category,String.class);
        if(((List<?>)categoryFilter.invoke(index,"удочка",fishing,null)).size()!=2
            || !((List<?>)categoryFilter.invoke(index,"удочка",tool,null)).isEmpty())
            throw new AssertionError("Russian search bypasses native category selection");
        System.out.println("PASS: combined search retains native rarity and category filters");
        Class<?> client=Class.forName("ru.wither.skyru.SkyRuClient",true,loader);
        var indexFilter=indexType.getMethod("filter",String.class);
        flag(client,"enabled",false);
        if(!((List<?>)indexFilter.invoke(index,"удочка")).isEmpty()) throw new AssertionError("Disabled Russian index still active");
        if(((List<?>)indexFilter.invoke(index,"Rod")).size()!=2) throw new AssertionError("Native English search regressed");
        flag(client,"enabled",true);
        if(((List<?>)indexFilter.invoke(index,"удочка")).size()!=2) throw new AssertionError("Reenabled index stale cache");
        for(int i=0;i<names.length;i++) {
            Object original=stack.getMethod("getHoverName").invoke(stacks.get(i));
            if(!names[i].equals(component.getMethod("getString").invoke(original))) throw new AssertionError("Original ItemStack mutated");
        }
        System.out.println("PASS: combined search lifecycle, unchanged ItemStacks and English input");
    }
}
