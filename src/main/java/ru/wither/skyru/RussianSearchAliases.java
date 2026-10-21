package ru.wither.skyru;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Search-local strings only: never changes stacks, IDs, recipes or user queries. */
public final class RussianSearchAliases {
    private final Function<String, String> translate;
    private final Map<String, String> cache = Collections.synchronizedMap(new LinkedHashMap<>(1024, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, String> entry) {
            return size() > 16384;
        }
    });

    public RussianSearchAliases(Function<String, String> translate) { this.translate = translate; }

    public static boolean russianQuery(String query) {
        return query != null && query.codePoints().anyMatch(c ->
            Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC);
    }

    public String name(String original, String query) {
        if (original == null || !russianQuery(query)) return original;
        String saved = cache.get(original);
        if (saved != null) return saved;
        String translated = translate.apply(original);
        if (translated == null) translated = original;
        String aliases = original;
        if (!translated.equals(original)) aliases += "\n" + translated;
        String withoutYo = translated.replace('ё', 'е').replace('Ё', 'Е');
        if (!withoutYo.equals(translated)) aliases += "\n" + withoutYo;
        cache.put(original, aliases);
        return aliases;
    }
}
