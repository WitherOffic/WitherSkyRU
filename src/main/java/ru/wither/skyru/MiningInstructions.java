package ru.wither.skyru;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Reviewed complete commission instructions; numbers and materials have separate styles. */
final class MiningInstructions {
    private MiningInstructions() {}
    private static final Pattern SOURCE = Pattern.compile("^(Mine |Collect |Defeat |Kill )([0-9][0-9,._ ]{0,17})( )(Mithril|Titanium|Hard Stone|Goblins|Ice Walkers)( in the Dwarven Mines)?(\\.?)$");
    private static final Map<String,String> MATERIALS = Map.of("Mithril","ед. мифрила","Titanium","ед. титана","Hard Stone","ед. твёрдого камня","Goblins","гоблинов","Ice Walkers","ледяных ходоков");
    static boolean supports(String source) { return CommissionInstructions.supports(source) || source != null && (source.startsWith("Mine ") || source.startsWith("Collect ") || source.startsWith("Kill ") || source.startsWith("Defeat ")) && SOURCE.matcher(source).matches(); }
    static List<String> overrideKeys(String source) {
        if(CommissionInstructions.supports(source))return CommissionInstructions.keys(source);
        var m=SOURCE.matcher(source);
        return m.matches()?List.of(source,m.group(1).trim(),m.group(4),"Dwarven Mines"):List.of(source);
    }
    static List<OfflineDictionary.TitleEdit> edits(String source, String target) {
        if(CommissionInstructions.supports(source))return CommissionInstructions.edits(source,target);
        var m=SOURCE.matcher(source);if(!m.matches())return List.of();
        boolean mob=m.group(4).equals("Goblins")||m.group(4).equals("Ice Walkers");
        if(mob!=(m.group(1).equals("Defeat ")||m.group(1).equals("Kill ")))return List.of();
        String verb=switch(m.group(1)){case "Mine "->"Добудьте ";case "Collect "->"Соберите ";case "Defeat "->"Победите ";default->"Убейте ";};
        String material=MATERIALS.get(m.group(4));
        String location=m.group(5)==null?"":" в Дварфских шахтах";
        String expected=verb+m.group(2)+" "+material+location+".";
        if(!expected.equals(target))return List.of();
        var result=new java.util.ArrayList<OfflineDictionary.TitleEdit>();
        result.add(new OfflineDictionary.TitleEdit(m.start(1),m.end(1),verb));
        boolean addPeriod=m.group(6).isEmpty();
        result.add(new OfflineDictionary.TitleEdit(m.start(4),m.end(4),material+(addPeriod&&m.group(5)==null?".":"")));
        if(m.group(5)!=null)result.add(new OfflineDictionary.TitleEdit(m.start(5),m.end(5),location+(addPeriod?".":"")));
        return List.copyOf(result);
    }
}
