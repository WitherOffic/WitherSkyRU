package ru.wither.skyru;

import java.util.*;
import java.util.regex.*;

/** Closed current commission objectives. Not a general NPC/player-chat translator. */
final class CommissionInstructions {
    private CommissionInstructions() {}
    private static final String COUNT="([1-9][0-9]{0,5}|[1-9][0-9]{0,2}(?:,[0-9]{3}){1,2})";
    private static final Pattern SOURCE=Pattern.compile("^(Slay )"+COUNT+"( )(Glacite Walkers|Goblins|Automatons|Sludges|Team Treasurite Members|Yogs|Thysts|Maniacs)( in the Precursor Remnant| in the Jungle| in the Mithril Deposits| in the Goblin Holdout| in the Magma Fields| in the Glacite Mineshafts)?(\\.?)$");
    private static final Pattern TASK=Pattern.compile("^(Open|Loot|Discover|Obtain) "+COUNT+" (chests?|Frozen Corpses?|Mineshafts?|Suspicious Scrap)(\\.?)$");
    private static final Map<String,String[]> MOBS=Map.of(
        "Glacite Walkers",new String[]{"глацитового бродягу","глацитовых бродяг","глацитовых бродяг",""},
        "Goblins",new String[]{"гоблина","гоблинов","гоблинов"," in the Goblin Holdout"},
        "Automatons",new String[]{"автоматона","автоматонов","автоматонов"," in the Precursor Remnant"},
        "Sludges",new String[]{"существо «Слизь»","существа «Слизь»","существ «Слизь»"," in the Jungle"},
        "Team Treasurite Members",new String[]{"участника команды Treasurite","участников команды Treasurite","участников команды Treasurite"," in the Mithril Deposits"},
        "Yogs",new String[]{"йога","йогов","йогов"," in the Magma Fields"},
        "Thysts",new String[]{"тиста","тистов","тистов"," in the Jungle"},
        "Maniacs",new String[]{"маньяка","маньяков","маньяков"," in the Glacite Mineshafts"});
    private static final Map<String,String> AREAS=Map.of(
        " in the Precursor Remnant"," в Руинах предтеч", " in the Jungle"," в Джунглях",
        " in the Mithril Deposits"," в Мифриловых залежах", " in the Goblin Holdout"," в Убежище гоблинов",
        " in the Magma Fields"," в Магмовых полях", " in the Glacite Mineshafts"," в Глацитовых шахтах");
    private static Matcher match(String source){
        var m=SOURCE.matcher(source);if(!m.matches())return null;
        if(m.group(5)!=null&&!m.group(5).equals(MOBS.get(m.group(4))[3]))return null;
        return m;
    }
    private static Matcher task(String source){
        var m=TASK.matcher(source);if(!m.matches())return null;
        String term=m.group(3);boolean one=m.group(2).equals("1");
        boolean valid=switch(m.group(1)){
            case "Open" -> term.equals(one?"chest":"chests");
            case "Loot" -> term.equals(one?"Frozen Corpse":"Frozen Corpses");
            case "Discover" -> term.equals(one?"Mineshaft":"Mineshafts");
            default -> term.equals("Suspicious Scrap");
        };
        return valid?m:null;
    }
    static boolean supports(String source){return source!=null&&(match(source)!=null||task(source)!=null);}
    static List<String> keys(String source){var m=match(source);if(m!=null)return m.group(5)==null?List.of(source,"Slay",m.group(4)):List.of(source,"Slay",m.group(4),m.group(5).substring(8));m=task(source);return m==null?List.of(source):List.of(source,m.group(1),m.group(3));}
    private static String noun(Matcher m){long n=Long.parseLong(m.group(2).replace(",",""))%100;int form=n>=11&&n<=14?2:n%10==1?0:n%10>=2&&n%10<=4?1:2;return MOBS.get(m.group(4))[form];}
    private static String taskVerb(Matcher m){return switch(m.group(1)){case "Open"->"Откройте";case "Loot"->"Обыщите";case "Discover"->"Посетите";default->"Получите";};}
    private static String taskNoun(Matcher m){
        long n=Long.parseLong(m.group(2).replace(",",""))%100;int form=n>=11&&n<=14?2:n%10==1?0:n%10>=2&&n%10<=4?1:2;
        return switch(m.group(1)){
            case "Open" -> new String[]{"сундук","сундука","сундуков"}[form];
            case "Loot" -> new String[]{"замёрзшее тело","замёрзших тела","замёрзших тел"}[form];
            case "Discover" -> new String[]{"глацитовую шахту","глацитовые шахты","глацитовых шахт"}[form];
            default -> new String[]{"подозрительный обломок","подозрительных обломка","подозрительных обломков"}[form];
        };
    }
    static String translate(String source){var m=match(source);if(m!=null)return "Победите "+m.group(2)+" "+noun(m)+(m.group(5)==null?"":AREAS.get(m.group(5)))+".";m=task(source);return m==null?null:taskVerb(m)+" "+m.group(2)+" "+taskNoun(m)+".";}
    static List<OfflineDictionary.TitleEdit> edits(String source,String target){
        if(!Objects.equals(target,translate(source)))return List.of();
        var m=match(source);
        if(m==null){m=task(source);if(m==null)return List.of();return List.of(new OfflineDictionary.TitleEdit(m.start(1),m.end(1),taskVerb(m)),new OfflineDictionary.TitleEdit(m.start(3),m.end(3),taskNoun(m)+(m.group(4).isEmpty()?".":"")));}
        var result=new ArrayList<OfflineDictionary.TitleEdit>();
        result.add(new OfflineDictionary.TitleEdit(m.start(1),m.end(1),"Победите "));
        result.add(new OfflineDictionary.TitleEdit(m.start(4),m.end(4),noun(m)+(m.group(5)==null&&m.group(6).isEmpty()?".":"")));
        if(m.group(5)!=null)result.add(new OfflineDictionary.TitleEdit(m.start(5),m.end(5),AREAS.get(m.group(5))+(m.group(6).isEmpty()?".":"")));
        return List.copyOf(result);
    }
}
