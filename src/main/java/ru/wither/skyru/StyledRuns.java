package ru.wither.skyru;

import java.util.*;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

/** Fresh per-call snapshot. Never stored in a cache or retained across draws. */
final class StyledRuns {
    record Run(int start,int end,Style style) {}
    final String text;
    final List<Run> runs;
    private StyledRuns(String text,List<Run> runs){this.text=text;this.runs=runs;}
    static StyledRuns collect(FormattedCharSequence source){return collect(source,Integer.MAX_VALUE);}
    static StyledRuns collect(FormattedCharSequence source,int limit){
        var builder=new Builder(limit);source.accept(builder);return builder.finish();
    }
    private static final class Builder implements FormattedCharSink {
        private final int limit;private final StringBuilder text=new StringBuilder();private List<Run> runs;
        private Style current;private int start;
        Builder(int limit){this.limit=limit;}
        @Override public boolean accept(int index,Style style,int point){
            if(text.length()>limit)return false;
            if(!Objects.equals(current,style)){
                if(text.length()>start){if(runs==null)runs=new ArrayList<>();runs.add(new Run(start,text.length(),current));}
                current=style;start=text.length();
            }
            text.appendCodePoint(point);return true;
        }
        StyledRuns finish(){
            if(runs==null)return new StyledRuns(text.toString(),text.isEmpty()?List.of():List.of(new Run(0,text.length(),current)));
            if(text.length()>start)runs.add(new Run(start,text.length(),current));
            return new StyledRuns(text.toString(),List.copyOf(runs));
        }
    }
    Style styleAt(int index){
        for(var run:runs)if(index>=run.start()&&index<run.end())return run.style();
        throw new IndexOutOfBoundsException(index);
    }
    boolean uniform(int start,int end){
        Style first=styleAt(start);
        for(var run:runs)if(run.start()<end&&run.end()>start&&!Objects.equals(first,run.style()))return false;
        return true;
    }
    void appendOriginal(List<FormattedCharSequence> result,int start,int end){
        for(var run:runs){
            int from=Math.max(start,run.start()),to=Math.min(end,run.end());
            if(from<to)result.add(FormattedCharSequence.forward(text.substring(from,to),run.style()));
        }
    }
}
