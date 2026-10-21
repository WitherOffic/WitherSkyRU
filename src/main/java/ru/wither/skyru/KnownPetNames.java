package ru.wither.skyru;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/** Closed pet identities from the shipped item-name corpus; skins are not pets. */
final class KnownPetNames {
    private static final Map<String,String> TITLES=load();
    private KnownPetNames() {}
    private static Map<String,String> load(){
        var names=new TreeMap<String,String>();
        try(var stream=KnownPetNames.class.getResourceAsStream("/data/witherskyru/search-item-names.txt")){
            if(stream==null)return Map.of();
            try(var reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))){
                String line;while((line=reader.readLine())!=null){
                    if(line.startsWith("[Lvl {LVL}] "))names.put(line.substring(12),line);
                    else if(line.startsWith("[Lvl 100] "))names.put(line.substring(10),line);
                }
            }
        }catch(IOException unavailable){return Map.of();}
        return Map.copyOf(names);
    }
    static boolean contains(String name){return TITLES.containsKey(name);}
    static List<String> keys(String name){return contains(name)?List.of(name,TITLES.get(name)):List.of(name);}
    static String translation(String name,Function<String,String> lookup){
        String key=TITLES.get(name);if(key==null)return null;
        String target=lookup.apply(key);if(target==null||target.equals(key))return null;
        String prefix=key.startsWith("[Lvl {LVL}]")?"[Ур. {LVL}] ":"[Ур. 100] ";
        return target.startsWith(prefix)&&target.length()>prefix.length()?target.substring(prefix.length()):null;
    }
}
