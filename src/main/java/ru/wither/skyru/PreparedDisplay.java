package ru.wither.skyru;

import java.util.Arrays;
import java.util.List;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

/** Reusable glyph plans only. Never retains a source sequence, Style, Component or action. */
final class PreparedDisplay {
    static final int SLOTS=128,MAX_TEXT=256,MAX_RUNS=16,MAX_OUTPUT=512,MAX_GLYPHS=16384;
    @FunctionalInterface interface Renderer {
        FormattedCharSequence render(FormattedCharSequence source,StyledRuns current,OfflineDictionary db,boolean compact);
    }
    private static final ThreadLocal<Cache> CACHE=ThreadLocal.withInitial(Cache::new);
    private PreparedDisplay(){}
    /** Callback oracle; production keeps the ordinary miss renderer in its original method. */
    static FormattedCharSequence render(FormattedCharSequence source,StyledRuns current,OfflineDictionary db,boolean compact,Renderer renderer){
        var prepared=find(source,current,db,compact);if(prepared!=null)return prepared;
        var result=renderer.render(source,current,db,compact);remember(source,result,current,db,compact);return result;
    }
    static FormattedCharSequence find(FormattedCharSequence source,StyledRuns current,OfflineDictionary db,boolean compact){
        if(current.text.length()>MAX_TEXT||current.runs.size()>MAX_RUNS||current.runs.isEmpty())return null;
        var cache=CACHE.get();if(cache.busy)return null;
        var roster=PlayerNames.cacheToken();
        if(cache.owner.get()!=db||cache.roster.get()!=roster){cache.clear();cache.owner=new java.lang.ref.WeakReference<>(db);cache.roster=new java.lang.ref.WeakReference<>(roster);}
        cache.busy=true;
        try{
            cache.pending=null;
            int textHash=current.text.hashCode()*31+(compact?1:0),hash=layoutHash(current,textHash,cache.groups);
            int slot=(textHash^(textHash>>>16))&(SLOTS-1);var entry=cache.entries[slot];
            boolean matched=entry!=null&&entry.matches(current,compact,cache.groups);
            if(matched&&entry.plan!=null)return entry.plan.bind(source,current.runs);
            if(!matched){
                // First observations store only a primitive signature, not a
                // key/object or translated glyphs. Signature collisions can
                // admit early, never hit: full text/layout is checked above.
                long signature=((long)hash<<32)|((long)current.text.length()<<8)|current.runs.size();
                if(cache.seen[slot]==signature){cache.pending=new Entry(current,compact,cache.groups);cache.pendingSlot=slot;}
                cache.seen[slot]=signature;
            }
            return null;
        }finally{cache.busy=false;}
    }
    static void remember(FormattedCharSequence source,FormattedCharSequence result,StyledRuns current,OfflineDictionary db,boolean compact){
        if(current.text.length()>MAX_TEXT||current.runs.size()>MAX_RUNS||current.runs.isEmpty())return;
        var cache=CACHE.get();var pending=cache.pending;
        if(cache.busy||pending==null||cache.owner.get()!=db||cache.roster.get()!=PlayerNames.cacheToken()||!pending.text.equals(current.text))return;
        cache.busy=true;
        try{
            layoutHash(current,0,cache.groups);if(!pending.matches(current,compact,cache.groups))return;
            Plan plan;try{plan=compile(source,result,current);}catch(Throwable unsupported){plan=null;}
            int slot=cache.pendingSlot;var old=cache.entries[slot];if(old!=null&&old.plan!=null)cache.glyphs-=old.plan.size();
            if(plan!=null&&cache.glyphs+plan.size()>MAX_GLYPHS)cache.clear();
            pending.plan=plan;cache.entries[slot]=pending;if(plan!=null)cache.glyphs+=plan.size();
        }finally{cache.pending=null;cache.busy=false;}
    }
    private static int layoutHash(StyledRuns current,int hash,int[] groups){
        for(int i=0;i<current.runs.size();i++){
            int group=i;Style style=current.runs.get(i).style();
            // Output glyphs use an original run's exact Style reference. Track
            // aliases, not deep equality of mutable hover component contents.
            for(int j=0;j<i;j++)if(style==current.runs.get(j).style()){group=groups[j];break;}
            groups[i]=group;hash=(hash*31+current.runs.get(i).end())*31+group;
        }return hash;
    }
    private static final class Cache {
        java.lang.ref.WeakReference<OfflineDictionary> owner=new java.lang.ref.WeakReference<>(null);
        java.lang.ref.WeakReference<Object> roster=new java.lang.ref.WeakReference<>(null);
        boolean busy;int glyphs,pendingSlot;Entry pending;
        final Entry[] entries=new Entry[SLOTS];final int[] groups=new int[MAX_RUNS];final long[] seen=new long[SLOTS];
        void clear(){Arrays.fill(entries,null);Arrays.fill(seen,0);glyphs=0;pending=null;}
    }
    private static final class Entry {
        final String text;final boolean compact;final int[] layout;Plan plan;
        Entry(StyledRuns current,boolean compact,int[] groups){
            text=current.text;this.compact=compact;layout=new int[current.runs.size()*2];
            for(int i=0;i<current.runs.size();i++){layout[2*i]=current.runs.get(i).end();layout[2*i+1]=groups[i];}
        }
        boolean matches(StyledRuns current,boolean compact,int[] groups){
            if(this.compact!=compact||layout.length!=current.runs.size()*2||!text.equals(current.text))return false;
            for(int i=0;i<current.runs.size();i++)if(layout[2*i]!=current.runs.get(i).end()||layout[2*i+1]!=groups[i])return false;
            return true;
        }
    }
    private static final Plan UNCHANGED=new Plan(null,null,null);
    private record Plan(int[] indices,int[] points,int[] runs){
        int size(){return points==null?0:points.length;}
        FormattedCharSequence bind(FormattedCharSequence source,List<StyledRuns.Run> styles){
            if(this==UNCHANGED)return source;
            if(styles.size()==1)return new UniformBound(this,styles.getFirst().style());
            var current=new Style[styles.size()];for(int i=0;i<current.length;i++)current[i]=styles.get(i).style();
            return new Bound(this,current);
        }
    }
    private record UniformBound(Plan plan,Style style) implements FormattedCharSequence {
        @Override public boolean accept(FormattedCharSink sink){
            for(int i=0;i<plan.points.length;i++)if(!sink.accept(plan.indices[i],style,plan.points[i]))return false;
            return true;
        }
    }
    private record Bound(Plan plan,Style[] styles) implements FormattedCharSequence {
        @Override public boolean accept(FormattedCharSink sink){
            for(int i=0;i<plan.points.length;i++)if(!sink.accept(plan.indices[i],styles[plan.runs[i]],plan.points[i]))return false;
            return true;
        }
    }
    private static Plan compile(FormattedCharSequence source,FormattedCharSequence result,StyledRuns current){
        if(result==source)return UNCHANGED;
        var builder=new Compiler(current);boolean complete=result.accept(builder);
        if(!complete||builder.unsupported)return null;
        return new Plan(Arrays.copyOf(builder.indices,builder.size),Arrays.copyOf(builder.points,builder.size),Arrays.copyOf(builder.runs,builder.size));
    }
    private static final class Compiler implements FormattedCharSink {
        final StyledRuns current;int size;boolean unsupported;
        int[] indices=new int[32],points=new int[32],runs=new int[32];
        Compiler(StyledRuns current){this.current=current;}
        @Override public boolean accept(int index,Style style,int point){
            if(size>=MAX_OUTPUT){unsupported=true;return false;}
            int run=-1;for(int i=0;i<current.runs.size();i++)if(style==current.runs.get(i).style()){run=i;break;}
            if(run<0){unsupported=true;return false;}
            if(size==points.length){int length=Math.min(MAX_OUTPUT,size*2);indices=Arrays.copyOf(indices,length);points=Arrays.copyOf(points,length);runs=Arrays.copyOf(runs,length);}
            indices[size]=index;points[size]=point;runs[size++]=run;return true;
        }
    }
}
