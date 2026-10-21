package ru.wither.skyru;

import java.time.*;
import java.util.*;
import java.util.regex.*;

/** Only the observed Received date shape. No creation clocks, time zones or date guesses. */
final class DisplayDates{
    private record Month(int number,String english,String nominative,String genitive){}
    private static final List<Month> MONTHS=List.of(
        new Month(1,"January","Январь","января"),new Month(2,"February","Февраль","февраля"),
        new Month(3,"March","Март","марта"),new Month(4,"April","Апрель","апреля"),
        new Month(5,"May","Май","мая"),new Month(6,"June","Июнь","июня"),
        new Month(7,"July","Июль","июля"),new Month(8,"August","Август","августа"),
        new Month(9,"September","Сентябрь","сентября"),new Month(10,"October","Октябрь","октября"),
        new Month(11,"November","Ноябрь","ноября"),new Month(12,"December","Декабрь","декабря"));
    private static final Map<String,Month> BY_MONTH=index();
    private static Map<String,Month> index(){var out=new TreeMap<String,Month>();for(var m:MONTHS)for(String s:List.of(m.english,m.nominative,m.genitive))out.put(s.toLowerCase(Locale.ROOT),m);return Map.copyOf(out);}
    private static final Pattern DATE=Pattern.compile("^(Received|Получено):? ?("+String.join("|",new TreeSet<>(BY_MONTH.keySet()))+") ([1-9]|[12][0-9]|3[01]), ([1-9][0-9]{3})$",Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
    static boolean supports(String text){return text!=null&&text.length()<=96&&(text.startsWith("Received")||text.startsWith("Получено"));}
    private static Matcher match(String text){
        if(!supports(text))return null;var m=DATE.matcher(text);if(!m.matches())return null;
        var month=BY_MONTH.get(m.group(2).toLowerCase(Locale.ROOT));
        try{LocalDate.of(Integer.parseInt(m.group(4)),month.number,Integer.parseInt(m.group(3)));}catch(DateTimeException invalid){return null;}return m;
    }
    static List<String> keys(String text){var m=match(text);if(m==null)return List.of(text);var month=BY_MONTH.get(m.group(2).toLowerCase(Locale.ROOT));return List.of(text,"Received","Получено",month.english,month.nominative,month.genitive);}
    static OfflineDictionary.ReorderedTitle plan(String text){
        var m=match(text);if(m==null)return null;var month=BY_MONTH.get(m.group(2).toLowerCase(Locale.ROOT));
        return new OfflineDictionary.ReorderedTitle(0,text.length(),List.of(
            new OfflineDictionary.TitleEdit(0,m.end(1),"Получено: "),
            new OfflineDictionary.TitleEdit(m.start(3),m.end(3),m.group(3)),
            new OfflineDictionary.TitleEdit(m.end(2),m.start(3)," "),
            new OfflineDictionary.TitleEdit(m.start(2),m.end(2),month.genitive),
            new OfflineDictionary.TitleEdit(m.start(4)-1,m.start(4)," "),
            new OfflineDictionary.TitleEdit(m.start(4),m.end(4),m.group(4))));
    }
    static String translate(String text){var plan=plan(text);if(plan==null)return null;var out=new StringBuilder();for(var p:plan.parts())out.append(p.replacement());return out.toString();}
}
