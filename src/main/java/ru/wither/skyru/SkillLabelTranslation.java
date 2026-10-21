package ru.wither.skyru;

import java.util.*;
import java.util.regex.Pattern;

/** Whole known skill headings; level characters are never translated as prose. */
final class SkillLabelTranslation {
    private static final Map<String,String> LABELS=Map.ofEntries(
        Map.entry("Alchemy","Алхимия"),Map.entry("Carpentry","Плотницкое дело"),
        Map.entry("Combat","Бой"),Map.entry("Enchanting","Зачарование"),
        Map.entry("Farming","Фермерство"),Map.entry("Fishing","Рыбалка"),
        Map.entry("Foraging","Лесорубство"),Map.entry("Mining","Горное дело"),
        Map.entry("Runecrafting","Создание рун"),Map.entry("Social","Общение"),
        Map.entry("Taming","Приручение"),Map.entry("Hunting","Охота"),
        Map.entry("Catacombs","Катакомбы"),Map.entry("Dungeoneering","Прохождение подземелий"));
    private static final Pattern SOURCE=Pattern.compile("^(Alchemy|Carpentry|Combat|Enchanting|Farming|Fishing|Foraging|Mining|Runecrafting|Social|Taming|Hunting|Catacombs|Dungeoneering)( )([IVXLCDM]{1,12}|[0-9]{1,3})$");
    private static final Pattern HEADING=Pattern.compile("^([^A-Za-zА-Яа-яЁё\\n]{0,32})(Your Skills)([^A-Za-zА-Яа-яЁё\\n]{0,32})$");
    static List<String> keys(String source){var m=SOURCE.matcher(source);if(m.matches())return List.of(source,m.group(1));var h=HEADING.matcher(source);return h.matches()?List.of(source,"Your Skills"):List.of(source);}
    static String translate(String source){var m=SOURCE.matcher(source);if(m.matches())return LABELS.get(m.group(1))+m.group(2)+m.group(3);var h=HEADING.matcher(source);return h.matches()?h.group(1)+"Ваши навыки"+h.group(3):null;}
    static List<OfflineDictionary.TitleEdit> edits(String source,String target){
        if(!Objects.equals(translate(source),target))return List.of();
        var h=HEADING.matcher(source);if(h.matches())return List.of(new OfflineDictionary.TitleEdit(h.start(2),h.end(2),"Ваши навыки"));
        var m=SOURCE.matcher(source);if(!m.matches())return List.of();
        return List.of(new OfflineDictionary.TitleEdit(m.start(1),m.end(1),LABELS.get(m.group(1))));
    }
}
