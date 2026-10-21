package ru.wither.skyru;

import java.util.*;
import java.util.regex.*;

/** Closed captured acquisition/requirement bullets; no arbitrary names or rule changes. */
final class EnchantBookFooter{
    private static final String SOURCE="(Enchantment Table|Experiments|Bazaar|Catacombs|Dungeons|Dungeon|Fateful Stinger)";
    private static final String ROMAN="[IVXLCDM]{1,12}(?:-[IVXLCDM]{1,12})?";
    private static final Pattern BULLET=Pattern.compile("^( ?- )"+SOURCE+"(?: \\(("+ROMAN+")\\))?$");
    private static final Pattern OLD=Pattern.compile("^("+ROMAN+"): "+SOURCE+"(?:, "+SOURCE+")?(,)?$");
    private static final Pattern LEVEL=Pattern.compile("^( ?- )(Enchanting Level) ([1-9][0-9]{0,2})$");
    private static final List<String> ROMAN_LEVELS=List.of("I","II","III","IV","V","VI","VII");
    private static final Pattern TITLE=Pattern.compile("^(Chance|Execute|Prosecute|Titan Killer|Thunderbolt|Thunderlord|Cleave|Impaling|Venomous) (I|II|III|IV|V|VI|VII)$");
    private static final Pattern APPLIED=Pattern.compile("^( ?- )(Sword|Longsword|Gauntlet|Bow|Fishing Weapon|Fishing Rod|Execute|Prosecute|Giant Killer|Thunderlord|Thunderbolt)$");
    private static final Map<String,String> APPLIED_NAMES=Map.ofEntries(Map.entry("Sword","Меч"),Map.entry("Longsword","Длинный меч"),Map.entry("Gauntlet","Рукавица"),Map.entry("Bow","Лук"),Map.entry("Fishing Weapon","Рыболовное оружие"),Map.entry("Fishing Rod","Удочка"),Map.entry("Execute","Казнь"),Map.entry("Prosecute","Приговор"),Map.entry("Giant Killer","Убийца великанов"),Map.entry("Thunderlord","Повелитель грома"),Map.entry("Thunderbolt","Разряд молнии"));
    private static final Map<String,String> NAMES=Map.of("Chance","Шанс","Execute","Казнь","Prosecute","Приговор","Titan Killer","Убийца титанов","Thunderbolt","Разряд молнии","Thunderlord","Повелитель грома","Cleave","Рассечение","Impaling","Пронзатель","Venomous","Ядовитость");
    private static final Map<String,Integer> MAX_LEVELS=Map.of("Chance",5,"Execute",6,"Prosecute",6,"Titan Killer",7,"Thunderbolt",7,"Thunderlord",7,"Cleave",6,"Impaling",5,"Venomous",7);
    private static final Map<String,String> SOURCES=Map.of("Enchantment Table","Стол зачарований","Experiments","Эксперименты","Bazaar","Базар","Catacombs","Катакомбы","Dungeon","Подземелье","Dungeons","Подземелья","Fateful Stinger","Судьбоносное жало");
    static boolean supports(String s){return s!=null&&s.length()<=120&&(s.startsWith("- ")||s.startsWith(" - ")||s.startsWith("Chance ")||s.startsWith("Execute ")||s.startsWith("Prosecute ")||s.startsWith("Titan Killer ")||s.startsWith("Thunderbolt ")||s.startsWith("Thunderlord ")||s.startsWith("Cleave ")||s.startsWith("Impaling ")||s.startsWith("Venomous ")||s.endsWith("Enchantment Table")||s.endsWith("Enchantment Table,")||s.endsWith("Experiments")||s.endsWith("Experiments,")||s.endsWith("Bazaar")||s.endsWith("Bazaar,")||s.endsWith("Catacombs")||s.endsWith("Catacombs,")||s.endsWith("Dungeon")||s.endsWith("Dungeon,")||s.endsWith("Dungeons")||s.endsWith("Dungeons,")||s.endsWith("Fateful Stinger")||s.endsWith("Fateful Stinger,"));}
    static List<String> keys(String s){var m=TITLE.matcher(s);if(m.matches())return List.of(s,m.group(1));m=LEVEL.matcher(s);if(m.matches())return List.of(s,"Enchanting Level","Enchanting");m=APPLIED.matcher(s);if(m.matches())return List.of(s,m.group(2));m=BULLET.matcher(s);if(m.matches())return List.of(s,m.group(2));m=OLD.matcher(s);return m.matches()?m.group(3)==null?List.of(s,m.group(2)):List.of(s,m.group(2),m.group(3)):List.of(s);}
    static List<OfflineDictionary.TitleEdit> edits(String s){
        if(!supports(s))return List.of();var m=TITLE.matcher(s);if(m.matches()){
            int level=ROMAN_LEVELS.indexOf(m.group(2))+1,max=MAX_LEVELS.get(m.group(1));
            if(level>max)return List.of();return List.of(new OfflineDictionary.TitleEdit(m.start(1),m.end(1),NAMES.get(m.group(1))));
        }
        m=LEVEL.matcher(s);if(m.matches())return List.of(new OfflineDictionary.TitleEdit(m.start(2),m.end(2),"Зачарование: уровень"));
        m=APPLIED.matcher(s);if(m.matches())return List.of(new OfflineDictionary.TitleEdit(m.start(2),m.end(2),APPLIED_NAMES.get(m.group(2))));
        m=BULLET.matcher(s);if(m.matches()){if(m.group(3)!=null&&!validRange(m.group(3)))return List.of();}
        else{m=OLD.matcher(s);if(!m.matches()||!validRange(m.group(1)))return List.of();if(m.group(3)!=null)return List.of(new OfflineDictionary.TitleEdit(m.start(2),m.end(2),SOURCES.get(m.group(2))),new OfflineDictionary.TitleEdit(m.start(3),m.end(3),SOURCES.get(m.group(3))));}
        return List.of(new OfflineDictionary.TitleEdit(m.start(2),m.end(2),SOURCES.get(m.group(2))));
    }
    private static boolean validRange(String s){int dash=s.indexOf('-');if(dash<0)return ROMAN_LEVELS.contains(s);int a=ROMAN_LEVELS.indexOf(s.substring(0,dash)),b=ROMAN_LEVELS.indexOf(s.substring(dash+1));return a>=0&&b>=a;}
    static String translate(String s){var e=edits(s);if(e.isEmpty())return null;var out=new StringBuilder(s);for(int i=e.size()-1;i>=0;i--){var p=e.get(i);out.replace(p.start(),p.end(),p.replacement());}return out.toString();}
}
