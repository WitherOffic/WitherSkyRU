package ru.wither.skyru;

import java.util.*;

/** One inspection per text draw, never per frame/world; nested draws inspect their own callers. */
public final class RenderContext {
    public record Snapshot(boolean protectedText,boolean hud,boolean tooltip,boolean customScoreboard) {}
    private static final StackWalker WALKER=StackWalker.getInstance();
    private static final ThreadLocal<Snapshot> CURRENT=new ThreadLocal<>();
    // Only immutable class/method classification, never caller order, frame depth
    // or the resulting context. New draws still walk their own actual callers.
    // Drawing stays on the caller's thread. Class/method masks are pure metadata:
    // no need for two synchronized, access-ordered LRU updates per visited frame.
    // Bound each thread independently; no rendered objects or call contexts retained.
    private static final ThreadLocal<Map<String,Map<String,Integer>>> FRAME_FLAGS=
        ThreadLocal.withInitial(()->localCache(256));
    private static <K,V> Map<K,V> localCache(int limit){
        return new LinkedHashMap<K,V>(Math.min(limit,256),.75f,false){
            @Override protected boolean removeEldestEntry(Map.Entry<K,V> entry){return size()>limit;}
        };
    }
    private static final int PROTECTED=1,HUD=2,TOOLTIP=4,CUSTOM=8,HUD_BLOCKED=16,TOOLTIP_BLOCKED=32;
    private RenderContext() {}
    public static Snapshot current(){var saved=CURRENT.get();return saved==null?inspect():saved;}
    public static Scope open(){var previous=CURRENT.get();CURRENT.set(inspect());return new Scope(previous);}
    public static final class Scope implements AutoCloseable {
        private final Snapshot previous;private boolean closed;
        private Scope(Snapshot previous){this.previous=previous;}
        // Keep the empty thread-local slot, not a Snapshot. Removing it caused a
        // new ThreadLocalMap.Entry allocation on the very next text draw.
        @Override public void close(){if(closed)return;closed=true;CURRENT.set(previous);}
    }
    private static Snapshot inspect(){
        return WALKER.walk(frames->{var classification=FRAME_FLAGS.get();var iterator=frames.iterator();int flags=0,index=0;
            while(index<64&&iterator.hasNext()){var frame=iterator.next();flags=classify(flags,frame.getClassName(),frame.getMethodName(),index++<48,classification);}
            return snapshot(flags);
        });
    }
    /** Pure regression oracle, using the same 48 panel / 64 user-input frame bounds. */
    public static Snapshot classify(List<String> frames){
        var classification=FRAME_FLAGS.get();int flags=0;for(int i=0;i<Math.min(64,frames.size());i++){
            String frame=frames.get(i);int split=frame.lastIndexOf('#');
            flags=classify(flags,split<0?frame:frame.substring(0,split),split<0?"":frame.substring(split+1),i<48,classification);
        }return snapshot(flags);
    }
    private static Snapshot snapshot(int flags){return new Snapshot((flags&PROTECTED)!=0,(flags&HUD)!=0&&(flags&HUD_BLOCKED)==0,
        (flags&TOOLTIP)!=0&&(flags&TOOLTIP_BLOCKED)==0,(flags&CUSTOM)!=0);}
    private static int classify(int flags,String type,String method,boolean panels,Map<String,Map<String,Integer>> classification){
        var methods=classification.get(type);
        if(methods==null){methods=localCache(32);classification.put(type,methods);}
        Integer mask=methods.get(method);
        if(mask==null){mask=frameFlags(type,method);methods.put(method,mask);}
        return flags|(panels?mask:mask&PROTECTED);
    }
    private static int frameFlags(String type,String method){
        int flags=0;
        if(type.equals("net.minecraft.client.gui.components.EditBox")
            ||type.equals("net.minecraft.client.gui.components.MultiLineEditBox")
            ||type.equals("net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen")&&method.startsWith("extract")
            ||type.equals("net.minecraft.client.gui.components.ChatComponent")
            ||type.equals("net.minecraft.client.gui.components.PlayerTabOverlay")&&method.startsWith("getNameForDisplay")
            ||type.equals("net.minecraft.client.gui.components.DebugScreenOverlay")
            ||type.startsWith("net.minecraft.client.gui.components.debugchart."))flags|=PROTECTED;
        String lower=type.toLowerCase(Locale.ROOT),action=method.toLowerCase(Locale.ROOT);
        boolean chat=lower.endsWith("chatcomponent")||lower.contains(".chat.");
        boolean tooltip=lower.contains("tooltip")||action.contains("tooltip");
        if(chat)flags|=HUD_BLOCKED|TOOLTIP_BLOCKED;
        if(tooltip||lower.contains(".screens.")&&!type.equals("net.minecraft.client.gui.screens.ChatScreen"))flags|=HUD_BLOCKED;
        boolean custom=type.startsWith("me.owdding.customscoreboard.core.");
        if(custom)flags|=CUSTOM;
        if(custom||type.startsWith("de.hysky.skyblocker.skyblock.tabhud.")||type.startsWith("at.hannibal2.skyhanni.")
            ||(type.equals("net.minecraft.client.gui.Gui")||type.equals("net.minecraft.client.gui.Hud"))&&(action.contains("scoreboard")||action.contains("sidebar"))
            ||lower.contains("scoreboardoverhaul")||action.contains("scoreboardoverhaul"))flags|=HUD;
        if(tooltip&&(action.contains("extract")||action.contains("draw")||action.contains("render")))flags|=TOOLTIP;
        return flags;
    }
}
