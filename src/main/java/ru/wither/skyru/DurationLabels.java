package ru.wither.skyru;

import java.util.List;
import java.util.regex.Pattern;

/** Complete reviewed cooldown/action-interval labels; never replaces a bare single letter. */
final class DurationLabels {
    private DurationLabels() {}
    private static final Pattern SOURCE=Pattern.compile("^(Cooldown|Time Between Actions)(: )([0-9]{1,6}(?:\\.[0-9]{1,6})?|[1-9][0-9]{0,2}(?:,[0-9]{3}){1,2}(?:\\.[0-9]{1,6})?)(s|m|h| hours| seconds)$");
    static boolean supports(String source){
        if(source==null)return false;
        var m=SOURCE.matcher(source);
        return m.matches()&&(m.group(1).equals("Cooldown")||m.group(4).equals("s")||m.group(4).equals(" seconds"));
    }
    static List<String> overrideKeys(String source){
        var m=SOURCE.matcher(source);
        String label=m.matches()?m.group(1):"Cooldown";
        return List.of(source,label,label+":");
    }
    static List<OfflineDictionary.TitleEdit> edits(String source,String target){
        if(!supports(source))return List.of();
        var m=SOURCE.matcher(source);if(!m.matches())return List.of();
        String label=m.group(1).equals("Cooldown")?"Перезарядка":"Интервал действий";
        String unit=switch(m.group(4)){case "s"," seconds"->"с";case "m"->"мин";default->"ч";};
        if(!(label+": "+m.group(3)+" "+unit).equals(target))return List.of();
        boolean spaced=m.group(4).startsWith(" ");
        return List.of(new OfflineDictionary.TitleEdit(m.start(1),m.end(1),label),
            new OfflineDictionary.TitleEdit(m.start(4)+(spaced?1:0),m.end(4),(spaced?"":" ")+unit));
    }
}
