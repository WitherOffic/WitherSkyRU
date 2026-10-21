package ru.wither.skyru;

import java.util.*;
import java.util.regex.Pattern;

/** Closed class-level and floor-meter headings; not lore, chat or user input. */
final class DungeonUiLabels {
    private static final Map<String,String> CLASSES=Map.of("Archer","Лучник","Berserk","Берсерк","Healer","Целитель","Mage","Маг","Tank","Танк");
    private static final Pattern CLASS=Pattern.compile("^(Archer|Berserk|Healer|Mage|Tank) Level ([1-9]|[1-4][0-9]|50)$");
    private static final Pattern PASS=Pattern.compile("^(Master Catacombs Pass|Catacombs Pass) (0|I|II|III|IV|V|VI|VII|VIII|IX|X)$");
    private static final Pattern METER=Pattern.compile("^(Catacombs)( \\([FM][1-7]\\))( RNG Meter)$");
    static List<String> keys(String source){var c=CLASS.matcher(source);if(c.matches())return List.of(source,c.group(1),c.group(1)+" Level");return METER.matcher(source).matches()?List.of(source,"Catacombs","RNG Meter"):List.of(source);}
    static String translate(String source){var c=CLASS.matcher(source);if(c.matches())return CLASSES.get(c.group(1))+": уровень "+c.group(2);var m=METER.matcher(source);return m.matches()?"Катакомбы"+m.group(2)+" — счётчик гарантированной добычи":null;}
    static List<OfflineDictionary.TitleEdit> edits(String source,String target){
        var pass=PASS.matcher(source);
        if(pass.matches()){
            String label=pass.group(1).startsWith("Master")?"Пропуск в мастер-режим Катакомб":"Пропуск в Катакомбы";
            if(Objects.equals(label+" "+pass.group(2),target))return List.of(new OfflineDictionary.TitleEdit(0,pass.start(2),label+" "));
        }
        if(!Objects.equals(translate(source),target))return List.of();var c=CLASS.matcher(source);if(c.matches())return List.of(new OfflineDictionary.TitleEdit(0,c.start(2),CLASSES.get(c.group(1))+": уровень "));
        var m=METER.matcher(source);return m.matches()?List.of(new OfflineDictionary.TitleEdit(m.start(1),m.end(1),"Катакомбы"),new OfflineDictionary.TitleEdit(m.start(3),m.end(3)," — счётчик гарантированной добычи")):List.of();
    }
}
