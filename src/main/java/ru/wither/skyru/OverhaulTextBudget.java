package ru.wither.skyru;

import java.lang.reflect.*;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.TextAlignment;

/** A single vendor draw call's local bounds; no persistent HUD or scoreboard data. */
public final class OverhaulTextBudget {
    private record Bounds(int left,int width) {}
    private static final ThreadLocal<Bounds> CURRENT=new ThreadLocal<>();
    private static final ClassValue<Method> DRAW=new ClassValue<>() {
        protected Method computeValue(Class<?> type) {
            try { return type.getMethod("drawText",Component.class,int.class,int.class,int.class,boolean.class); }
            catch(NoSuchMethodException e) { throw new IllegalStateException("Inspected Overhaul draw API changed",e); }
        }
    };
    private OverhaulTextBudget() {}
    public static int available(TextAlignment alignment,int x,int originalWidth) {
        var b=CURRENT.get();
        return b==null?originalWidth:HudTextFit.panelBudget(b.left(),b.width(),x,alignment,originalWidth);
    }
    /** Finally restores nested scopes, including when the vendor draw throws. */
    public static void within(int left,int width,Runnable draw) {
        var old=CURRENT.get();
        if(width>0&&width<=16384)CURRENT.set(new Bounds(left,width));else CURRENT.set(null);
        try { draw.run(); }
        finally { CURRENT.set(old); }
    }
    public static void draw(Object service,Component text,int x,int y,int color,boolean shadow,int width) {
        within(x,width,()->{
            try { DRAW.get(service.getClass()).invoke(service,text,x,y,color,shadow); }
            catch(InvocationTargetException e) {
                if(e.getCause() instanceof RuntimeException runtime)throw runtime;
                if(e.getCause() instanceof Error error)throw error;
                throw new IllegalStateException(e.getCause());
            } catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
        });
    }
}
