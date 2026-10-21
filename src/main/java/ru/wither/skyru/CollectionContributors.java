package ru.wither.skyru;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Co-op roster rows contain identities, even when a contributor is currently offline. */
final class CollectionContributors {
    private static final Pattern ROW=Pattern.compile("^(?:\\[[A-Za-z0-9+]{1,12}] )?[A-Za-z0-9_]{1,16}: [0-9]{1,12}(?:,[0-9]{3})*$");
    private static final Map<String,Long> visible=new ConcurrentHashMap<>();
    static Set<Integer> rows(List<String> source){
        source=source.stream().map(LegacyFormatting::strip).toList();
        var result=new HashSet<Integer>();
        for(int i=0;i<source.size();i++)if(source.get(i).equals("Co-op Contributions:")||source.get(i).equals("Вклады участников совместного профиля:"))
            for(int j=i+1;j<source.size()&&j<=i+16&&ROW.matcher(source.get(j)).matches();j++)result.add(j);
        return Set.copyOf(result);
    }
    static void remember(List<String> source){
        long now=System.nanoTime();visible.entrySet().removeIf(e->e.getValue()<now);
        if(visible.size()>256)visible.clear();for(int i:rows(source))visible.put(LegacyFormatting.strip(source.get(i)),now+15_000_000_000L);
    }
    static boolean protectedRow(String text){
        if(text==null||visible.isEmpty())return false;Long until=visible.get(LegacyFormatting.strip(text));
        return until!=null&&until>=System.nanoTime();
    }
}
