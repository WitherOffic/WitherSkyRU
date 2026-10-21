package ru.wither.skyru;

import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** Closed reviewed lore paragraphs, not a global translation of power names. */
final class PowerStoneTranslation {
    private record Entry(String before,String prefix,String name,String suffix) {
        String text(){return prefix+name+suffix+".";}
    }
    private static Entry adjective(String nominative,String accusative){return new Entry(nominative+" сила.","",accusative," силу");}
    private static Entry quoted(String name){return new Entry("силу «"+name+"».","силу «",name,"»");}
    private static final Map<String,Entry> ENTRIES=Map.ofEntries(
        Map.entry("Forceful",adjective("Напористая","Напористую")),
        Map.entry("Bloody",quoted("Кровавая")),
        Map.entry("Bubba",new Entry("Сила Bubba.","силу ","Bubba","")),
        Map.entry("Crumbly",adjective("Рассыпчатая","Рассыпчатую")),
        Map.entry("Shaded",quoted("Теневая")),
        Map.entry("Sanguisuge",adjective("Кровососущая","Кровососущую")),
        Map.entry("Bizarre",quoted("Причудливая")),
        Map.entry("Sighted",quoted("Зрячая")),
        Map.entry("Adept",quoted("Искусная")),
        Map.entry("Itchy",adjective("Зудящая","Зудящую")),
        Map.entry("Frozen",adjective("Замороженная","Замороженную")),
        Map.entry("Slender",quoted("Стройная")),
        Map.entry("Demonic",adjective("Демоническая","Демоническую")),
        Map.entry("Silky",quoted("Шёлковая")),
        Map.entry("Hurtful",adjective("Болезненная","Болезненную")),
        Map.entry("Strong",quoted("Сильная")),
        Map.entry("Mythical",adjective("Мифическая","Мифическую")),
        Map.entry("Pleasant",adjective("Приятная","Приятную")),
        Map.entry("Sweet",adjective("Сладкая","Сладкую")),
        Map.entry("Scorching",quoted("Обжигающая")),
        Map.entry("Buttery",adjective("Масляная","Масляную")),
        Map.entry("Healthy",adjective("Здоровая","Здоровую")));
    private static String name(String source){return source.endsWith(" power.")?source.substring(0,source.length()-7):"";}
    static List<EditorialContext.Edit> edits(int row,String source){
        Entry entry=ENTRIES.get(name(source));
        return entry==null || entry.before().equals(entry.text())?List.of():List.of(new EditorialContext.Edit(row,entry.before(),entry.text(),true));
    }
    private static Entry reviewed(List<String> source,int row){
        if(row<2 || !source.get(row-2).equals("Combine x9 of this item at the")
            || !source.get(row-1).equals("Thaumaturgist in the Hub to unlock the"))return null;
        String name=name(source.get(row));Entry entry=ENTRIES.get(name);
        if(entry==null || !SkyRuClient.tooltipPowerAllowed(List.of(source.get(row-2),source.get(row-1),source.get(row),name,"power","power.")))return null;
        return entry;
    }
    /** A changing style inside a semantic word is preserved as original, never flattened. */
    static FormattedCharSequence render(FormattedCharSequence original,List<String> source,int row){
        Entry entry=reviewed(source,row);if(entry==null)return null;
        String expected=source.get(row), name=name(expected);
        StringBuilder actual=new StringBuilder();List<Style> styles=new ArrayList<>();
        original.accept((i,s,cp)->{actual.appendCodePoint(cp);for(int n=0;n<Character.charCount(cp);n++)styles.add(s);return true;});
        if(!actual.toString().equals(expected))return original;
        Style term=styles.getFirst(),noun=styles.get(name.length()+1),period=styles.getLast();
        for(int i=0;i<name.length();i++)if(!styles.get(i).equals(term))return original;
        for(int i=name.length()+1;i<expected.length()-1;i++)if(!styles.get(i).equals(noun))return original;
        return FormattedCharSequence.composite(List.of(FormattedCharSequence.forward(entry.prefix(),noun),
            FormattedCharSequence.forward(entry.name(),term),FormattedCharSequence.forward(entry.suffix(),noun),FormattedCharSequence.forward(".",period)));
    }
    static Component render(Component original,List<String> source,int row){
        if(reviewed(source,row)==null)return null;
        List<FormattedCharSequence> pieces=new ArrayList<>();
        StringDecomposer.iterateFormatted(original,Style.EMPTY,(i,s,cp)->{pieces.add(FormattedCharSequence.forward(new String(Character.toChars(cp)),s));return true;});
        var next=render(FormattedCharSequence.composite(pieces),source,row);
        MutableComponent out=Component.empty();
        next.accept((i,s,cp)->{out.append(Component.literal(new String(Character.toChars(cp))).setStyle(s));return true;});
        return out;
    }
}
