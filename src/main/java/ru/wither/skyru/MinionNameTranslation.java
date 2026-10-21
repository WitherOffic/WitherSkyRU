package ru.wither.skyru;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Closed 61 families observed in the local corpus. Not arbitrary player names. */
final class MinionNameTranslation {
    private static final Set<String> FAMILIES=Set.of("Acacia Minion","Birch Minion","Blaze Minion","Cactus Minion","Carrot Minion","Cave Spider Minion","Chicken Minion","Clay Minion","Coal Minion","Cobblestone Minion","Cocoa Beans Minion","Cow Minion","Creeper Minion","Dark Oak Minion","Diamond Minion","Emerald Minion","Enderman Minion","End Stone Minion","Fishing Minion","Flower Minion","Ghast Minion","Glowstone Minion","Gold Minion","Gravel Minion","Hard Stone Minion","Ice Minion","Inferno Minion","Iron Minion","Jungle Minion","Lapis Minion","Lily Pad Minion","Magma Cube Minion","Melon Minion","Mithril Minion","Mushroom Minion","Mycelium Minion","Nether Wart Minion","Oak Minion","Obsidian Minion","Pig Minion","Potato Minion","Pumpkin Minion","Quartz Minion","Rabbit Minion","Redstone Minion","Red Sand Minion","Revenant Minion","Sand Minion","Sheep Minion","Skeleton Minion","Slime Minion","Snow Minion","Spider Minion","Spruce Minion","Sugar Cane Minion","Sunflower Minion","Tarantula Minion","Vampire Minion","Voidling Minion","Wheat Minion","Zombie Minion");
    private static final Pattern SOURCE=Pattern.compile("^([A-Za-z ]+ Minion)( )([1-9]|1[0-2]|[IVX]{1,4})$");
    private static final Set<String> ROMAN=Set.of("I","II","III","IV","V","VI","VII","VIII","IX","X","XI","XII");
    static boolean supports(String source){
        if(source==null||source.length()>100||!source.contains(" Minion "))return false;
        var m=SOURCE.matcher(source);if(!m.matches()||!FAMILIES.contains(m.group(1)))return false;
        String tier=m.group(3);return tier.charAt(0)>='0'&&tier.charAt(0)<='9'||ROMAN.contains(tier);
    }
    static List<String> keys(String source){if(!supports(source))return List.of(source);var m=SOURCE.matcher(source);m.matches();return List.of(source,m.group(1),m.group(1)+" I");}
    static String translate(String source,Function<String,String> lookup){if(!supports(source))return null;var m=SOURCE.matcher(source);m.matches();String canonical=lookup.apply(m.group(1)+" I");if(canonical==null||!canonical.endsWith(" I"))return null;return canonical.substring(0,canonical.length()-2)+m.group(2)+m.group(3);}
    static List<OfflineDictionary.TitleEdit> edits(String source,String target,Function<String,String> lookup){
        if(!supports(source)||!Objects.equals(translate(source,lookup),target))return List.of();var m=SOURCE.matcher(source);m.matches();
        return List.of(new OfflineDictionary.TitleEdit(0,m.end(1),target.substring(0,target.length()-m.group(3).length()-1)));
    }
}
