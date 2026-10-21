package ru.wither.skyru;

import java.util.*;

/** Dictionary-instance caches are bounded and contain no mutable render objects. */
final class BoundedCache {
    private BoundedCache() {}
    static <K,V> Map<K,V> create(int limit){
        return Collections.synchronizedMap(new LinkedHashMap<K,V>(Math.min(limit,256),.75f,true){
            @Override protected boolean removeEldestEntry(Map.Entry<K,V> entry){return size()>limit;}
        });
    }
}
