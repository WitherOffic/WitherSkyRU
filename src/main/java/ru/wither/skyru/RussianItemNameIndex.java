package ru.wither.skyru;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Immutable translated-name index, owned by one recipe index. Never rewrites its items or queries. */
public final class RussianItemNameIndex {
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]{2,}");
    private final NavigableMap<String, BitSet> tokens;
    private final String[] names;
    private final Map<String, BitSet> cache = Collections.synchronizedMap(new LinkedHashMap<>(128,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, BitSet> entry) { return size() > 512; }
    });

    public RussianItemNameIndex(String[] originals, Function<String,String> translate) {
        names = new String[originals.length];
        TreeMap<String,BitSet> index = new TreeMap<>();
        for(int i=0;i<originals.length;i++) {
            String original=originals[i];
            if(original==null) { names[i]=""; continue; }
            String translated=translate.apply(original);
            if(translated==null || translated.equals(original)) { names[i]=""; continue; }
            names[i]=normalize(translated);
            var matcher=TOKEN.matcher(names[i]);
            while(matcher.find()) index.computeIfAbsent(matcher.group(),ignored -> new BitSet()).set(i);
        }
        tokens=Collections.unmodifiableNavigableMap(index);
    }

    public static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace('ё','е');
    }

    /** Prefix lookup mirrors SkyRecipes token semantics; returned sets are never shared with callers. */
    public BitSet prefix(String query) {
        String prefix=normalize(query);
        BitSet saved=cache.get(prefix);
        if(saved!=null) return (BitSet)saved.clone();
        BitSet result=new BitSet();
        if(prefix.length()>=2) {
            for(var entry:tokens.tailMap(prefix,true).entrySet()) {
                if(!entry.getKey().startsWith(prefix)) break;
                result.or(entry.getValue());
            }
        }
        cache.put(prefix,(BitSet)result.clone());
        return result;
    }

    public boolean phrase(int item, String phrase) {
        return item>=0 && item<names.length && names[item].contains(normalize(phrase));
    }
}
