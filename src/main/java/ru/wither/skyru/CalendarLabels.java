package ru.wither.skyru;

import java.util.*;
import java.util.regex.Pattern;

/** Calendar display labels only; timestamps, counts and input are not rewritten. */
final class CalendarLabels {
    private static final Pattern ELECTION=Pattern.compile("^([0-9]{1,6})(st|nd|rd|th)( Election Over!| Election Booth Opens| New Year Celebration| Hoppity's Hunt| Spooky Festival| Season of Jerry)$");
    private static final Pattern COUNTDOWN=Pattern.compile("^(Starts in: |Ends in: )([0-9]{1,6}[dhms](?: [0-9]{1,6}[dhms]){0,3})$");
    private static final Pattern UNIT=Pattern.compile("[0-9]{1,6}([dhms])");
    private static final Pattern SKY_DATE=Pattern.compile("^(Early Spring|Spring|Late Spring|Early Summer|Summer|Late Summer|Early Autumn|Autumn|Late Autumn|Early Winter|Winter|Late Winter) ([1-9]|[12][0-9]|3[01])(st|nd|rd|th)$");
    static List<String> keys(String source){var m=ELECTION.matcher(source);if(m.matches())return List.of(source,m.group(3).strip());var c=COUNTDOWN.matcher(source);return c.matches()?List.of(source,c.group(1).strip()):List.of(source);}
    static String translate(String source){var edits=edits(source);if(edits.isEmpty())return null;var b=new StringBuilder();int at=0;for(var e:edits){b.append(source,at,e.start()).append(e.replacement());at=e.end();}return b.append(source,at,source.length()).toString();}
    static List<OfflineDictionary.TitleEdit> edits(String source){
        var date=SKY_DATE.matcher(source);if(date.matches()){
            String season=switch(date.group(1)){
                case "Early Spring"->"Начало весны";case "Spring"->"Весна";case "Late Spring"->"Конец весны";
                case "Early Summer"->"Начало лета";case "Summer"->"Лето";case "Late Summer"->"Конец лета";
                case "Early Autumn"->"Начало осени";case "Autumn"->"Осень";case "Late Autumn"->"Конец осени";
                case "Early Winter"->"Начало зимы";case "Winter"->"Зима";default->"Конец зимы";};
            return List.of(new OfflineDictionary.TitleEdit(date.start(1),date.end(1),season),
                new OfflineDictionary.TitleEdit(date.end(1),date.start(2),", "),
                new OfflineDictionary.TitleEdit(date.start(3),date.end(3),""));
        }
        var e=ELECTION.matcher(source);if(e.matches()){
            String event=switch(e.group(3)){case " Election Booth Opens"->" выборы: начало голосования";case " New Year Celebration"->" празднование Нового года";case " Hoppity's Hunt"->" охота Хоппити";case " Spooky Festival"->" Жуткий фестиваль";case " Season of Jerry"->" сезон Джерри";default->" выборы завершены!";};
            String suffix=switch(e.group(3)){case " Hoppity's Hunt"->"-я";case " Spooky Festival"," Season of Jerry"->"-й";default->"-е";};
            return List.of(new OfflineDictionary.TitleEdit(e.start(2),e.end(2),suffix),new OfflineDictionary.TitleEdit(e.start(3),e.end(3),event));
        }
        var c=COUNTDOWN.matcher(source);if(!c.matches())return List.of();
        var result=new ArrayList<OfflineDictionary.TitleEdit>();result.add(new OfflineDictionary.TitleEdit(0,c.end(1),c.group(1).startsWith("Starts")?"Начнётся через: ":"Завершится через: "));
        var u=UNIT.matcher(source);int order=-1;String units="dhms";
        while(u.find()){int next=units.indexOf(u.group(1));if(next<=order)return List.of();order=next;String ru=switch(u.group(1)){case "d"->" дн";case "h"->" ч";case "m"->" мин";default->" с";};result.add(new OfflineDictionary.TitleEdit(u.start(1),u.end(1),ru));}
        return List.copyOf(result);
    }
}
