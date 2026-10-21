package ru.wither.skyru;

import java.util.*;
import java.util.regex.*;

/** Closed observed rows and source-backed caption shapes, never minigame actions or item mutation. */
final class ExperimentDisplay{
    private static final String NUM="(?:[0-9]{1,9}|[0-9]{1,3}(?:,[0-9]{3}){1,3})(?:\\.[0-9]{1,4})?[kMB]?";
    private static final String VALUE=NUM+"(?:-"+NUM+")?";
    private static final Pattern XP=Pattern.compile("^("+VALUE+") (Enchanting Exp|Опыт зачарования|опыта зачарования)(?: \\((Stakes|Pairs|сложность|пары)\\))?$");
    private static final Pattern LEVELS=Pattern.compile("^([0-9]{1,6}) (XP Levels)$");
    private static final Pattern BOTTLES=Pattern.compile("^([0-9]{1,6}-[0-9]{1,6}) ((?:Grand |Titanic |Colossal )?Experience Bottles)$");
    private static final Pattern BOOKS=Pattern.compile("^([IVXLCDM]{1,12}-[IVXLCDM]{1,12}) (Enchanted Books)$");
    private static final Pattern PAGE=Pattern.compile("^\\(([1-9])/([1-9])\\) (Experimentation Table RNG)$");
    private static final Pattern TITLE=Pattern.compile("^(Superpairs|Chronomatron|Ultrasequencer) \\((Beginner|High|Grand|Supreme|Transcendent|Metaphysical)\\)$");
    private static final Pattern REWARDS=Pattern.compile("^(Superpairs|Chronomatron|Ultrasequencer) (Rewards)$");
    private static final Pattern STAKES=Pattern.compile("^(Stakes:|Сложность:) (Beginner|High|Grand|Supreme|Transcendent|Metaphysical)$");
    private static final Pattern COUNTER=Pattern.compile("^(Remaining Clicks:|Round:|Clicks:|Grid Size:) ([0-9]{1,6}(?:/[1-9][0-9]{0,5})?)$");
    private static final Pattern XP_COUNTER=Pattern.compile("^(Experimental XP:|Stored Experimental XP:) ("+NUM+")$");
    private static final Pattern TIMER=Pattern.compile("^(Timer:) ([0-9]{1,6})(s)$");
    static final Map<String,String> GAMES=Map.of("Superpairs","Суперпары","Chronomatron","Хрономатрон","Ultrasequencer","Ультрасеквенсор");
    private static final Map<String,String> TIERS=Map.of("Beginner","Начальный","High","Продвинутый","Grand","Большой","Supreme","Высший","Transcendent","Трансцендентный","Metaphysical","Метафизический");
    private static final Map<String,String> LABELS=Map.of("Remaining Clicks:","Осталось ходов:","Round:","Раунд:","Experimental XP:","Опыт экспериментов:","Stored Experimental XP:","Накопленный опыт экспериментов:","Clicks:","Ходы:","Grid Size:","Размер поля:");
    static boolean supports(String s){
        if(s==null||s.isEmpty()||s.length()>160)return false;char c=s.charAt(0);
        if(c>='0'&&c<='9')return s.contains("Enchanting Exp")||s.contains("зачарования")||s.endsWith("XP Levels")||s.endsWith("Experience Bottles");
        if(c=='(')return s.endsWith("Experimentation Table RNG");
        if(s.endsWith("Enchanted Books"))return true;
        return s.startsWith("Superpairs ")||s.startsWith("Chronomatron ")||s.startsWith("Ultrasequencer ")||s.startsWith("Stakes: ")||s.startsWith("Сложность: ")||s.startsWith("Timer: ")||s.startsWith("Remaining Clicks: ")||s.startsWith("Round: ")||s.startsWith("Experimental XP: ")||s.startsWith("Stored Experimental XP: ")||s.startsWith("Clicks: ")||s.startsWith("Grid Size: ");
    }
    static List<String> keys(String s){
        if(!supports(s))return List.of(s);var out=new ArrayList<String>();out.add(s);
        if(s.contains("зачарования"))out.add("Enchanting Exp");
        for(String key:List.of("Enchanting Exp","XP Levels","Experience Bottles","Grand Experience Bottles","Titanic Experience Bottles","Colossal Experience Bottles","Enchanted Books","Experimentation Table RNG","Stakes","Pairs","Rewards","Stakes:","Timer:"))if(s.contains(key))out.add(key);
        for(var key:GAMES.keySet())if(s.startsWith(key+" "))out.add(key);
        for(var key:TIERS.keySet())if(s.contains(key))out.add(key);
        for(var key:LABELS.keySet())if(s.startsWith(key+" "))out.add(key);
        return List.copyOf(out);
    }
    private static OfflineDictionary.TitleEdit edit(Matcher m,int group,String target){return new OfflineDictionary.TitleEdit(m.start(group),m.end(group),target);}
    static List<OfflineDictionary.TitleEdit> edits(String s){
        if(!supports(s))return List.of();Matcher m;char c=s.charAt(0);
        if(c>='0'&&c<='9'){
            if((s.contains("Enchanting Exp")||s.contains("зачарования"))&&(m=XP.matcher(s)).matches()){var out=new ArrayList<OfflineDictionary.TitleEdit>();out.add(edit(m,2,"опыта зачарования"));if(m.group(3)!=null)out.add(edit(m,3,m.group(3).equals("Stakes")||m.group(3).equals("сложность")?"сложность":"пары"));return List.copyOf(out);}
            if(s.endsWith("XP Levels")&&(m=LEVELS.matcher(s)).matches())return List.of(edit(m,2,"уровней опыта"));
            if(s.endsWith("Experience Bottles")&&(m=BOTTLES.matcher(s)).matches())return List.of(edit(m,2,switch(m.group(2)){case "Grand Experience Bottles"->"больших пузырьков опыта";case "Titanic Experience Bottles"->"титанических пузырьков опыта";case "Colossal Experience Bottles"->"колоссальных пузырьков опыта";default->"пузырьков опыта";}));
            return List.of();
        }
        if(s.endsWith("Enchanted Books")&&(m=BOOKS.matcher(s)).matches())return List.of(edit(m,2,"зачарованных книг"));
        if(c=='('&&(m=PAGE.matcher(s)).matches()&&Integer.parseInt(m.group(1))<=Integer.parseInt(m.group(2)))return List.of(edit(m,3,"Случайная добыча стола экспериментов"));
        if((m=TITLE.matcher(s)).matches())return List.of(edit(m,1,GAMES.get(m.group(1))),edit(m,2,TIERS.get(m.group(2))));
        if((m=REWARDS.matcher(s)).matches())return List.of(edit(m,1,GAMES.get(m.group(1))),edit(m,2,"— награды"));
        if((m=STAKES.matcher(s)).matches())return List.of(edit(m,1,"Сложность:"),edit(m,2,TIERS.get(m.group(2))));
        if((m=COUNTER.matcher(s)).matches())return List.of(edit(m,1,LABELS.get(m.group(1))));
        if((m=XP_COUNTER.matcher(s)).matches())return List.of(edit(m,1,LABELS.get(m.group(1))));
        if(s.startsWith("Timer: ")&&(m=TIMER.matcher(s)).matches())return List.of(edit(m,1,"Таймер:"),edit(m,3," с"));
        return List.of();
    }
    static String translate(String s){var edits=edits(s);if(edits.isEmpty())return null;var out=new StringBuilder();int at=0;for(var e:edits){out.append(s,at,e.start()).append(e.replacement());at=e.end();}return out.append(s,at,s.length()).toString();}
}
