package ru.wither.skyru;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Closed requirement spans; invoked only when an authored exact pair agrees. */
final class RequirementTranslation {
    private RequirementTranslation() {}
    private static final Map<String,String> LABELS=Map.of(
        "Catacombs","Катакомбы","Combat","Бой","Farming","Фермерство",
        "Fishing","Рыбалка","Foraging","Лесорубство","Hunting","Охота",
        "Mining","Горное дело","Social","Общение");
    private static final Pattern SOURCE=Pattern.compile("^❣ (Requires )(Catacombs|Combat|Farming|Fishing|Foraging|Hunting|Mining|Social)( Skill)( [0-9]+\\.)$");
    private static final Map<String,String> COLLECTION_LABELS=Map.of(
        "Cactus","Кактус","Coal","Уголь","Gold Ingot","Золотой слиток","Hard Stone","Твёрдый камень",
        "Honeycomb","Медовые соты","Ice","Лёд","Lotus","Лотос","Sand","Песок","Slimeball","Слизь","Umber","Умбра");
    private static final Pattern COLLECTION=Pattern.compile("^❣ (Requires )(Cactus|Coal|Gold Ingot|Hard Stone|Honeycomb|Ice|Lotus|Sand|Slimeball|Umber)( Collection)( [IVXLCDM]+\\.)$");
    static List<String> overrideKeys(String source) {
        var m=COLLECTION.matcher(source);
        return m.matches()?List.of(source,m.group(2),"Requires","Collection"):List.of(source);
    }
    static List<OfflineDictionary.TitleEdit> edits(String source,String authored) {
        var m=SOURCE.matcher(source);boolean collection=false;
        if(!m.matches()){m=COLLECTION.matcher(source);if(!m.matches())return List.of();collection=true;}
        String label=(collection?COLLECTION_LABELS:LABELS).get(m.group(2));
        String prefix=collection?"Требуется уровень коллекции ":"Требуется уровень навыка ";
        String expected="❣ "+prefix+"«"+label+"»"+m.group(4);
        if(!expected.equals(authored))return List.of();
        return List.of(new OfflineDictionary.TitleEdit(m.start(1),m.end(1),prefix),
            new OfflineDictionary.TitleEdit(m.start(2),m.end(2),"«"+label+"»"),
            new OfflineDictionary.TitleEdit(m.start(3),m.end(3),""));
    }
}
