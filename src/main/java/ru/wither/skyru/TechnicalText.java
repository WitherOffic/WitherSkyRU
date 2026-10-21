package ru.wither.skyru;

/** Exact original-only F3/chat line draws, never a global "F3/chat visible" state. */
public final class TechnicalText {
    private static final ThreadLocal<Integer> DEPTH=new ThreadLocal<>();
    private TechnicalText() {}
    public static boolean active(){return DEPTH.get()!=null;}
    public static Scope open(){Integer previous=DEPTH.get();DEPTH.set(previous==null?1:previous+1);return new Scope(previous);}
    public static final class Scope implements AutoCloseable {
        private final Integer previous;private boolean closed;
        private Scope(Integer previous){this.previous=previous;}
        // Null still means inactive; preserve the slot to avoid per-F3-line
        // ThreadLocalMap.Entry churn. No text or drawing object is retained.
        @Override public void close(){if(closed)return;closed=true;DEPTH.set(previous);}
    }
}
