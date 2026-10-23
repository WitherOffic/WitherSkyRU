package publicsmoke;

import java.nio.file.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/** Actual isolated client startup in A/B/C; no static translator or external mod class dependency. */
public final class PublicSmoke117 {
    private static boolean done;private static int frames,checks;
    private static final long start=System.nanoTime();
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;}
    public static void frame(){
        if(done)return;var mc=Minecraft.getInstance();
        try{
            if(System.nanoTime()-start>150_000_000_000L)throw new AssertionError("finite client smoke timeout");
            if(mc.gui.overlay()!=null||mc.gui.screen()==null||++frames<50)return;
            boolean translator=Boolean.getBoolean("witherskyru.public.smoke.translator");
            String independent=System.getProperty("witherskyru.public.smoke.collector","");
            var loader=FabricLoader.getInstance();check(loader.isModLoaded("witherskyru")==translator,"expected translator presence");
            if(!independent.isEmpty())check(loader.isModLoaded(independent),"independent mod loaded");
            if(translator){
                var type=Class.forName("ru.wither.skyru.SkyRuClient");var field=type.getDeclaredField("dictionary");field.setAccessible(true);
                Object dictionary=field.get(null);check(dictionary!=null,"actual Fabric translator initialization completed");
                check(dictionary.getClass().getMethod("translate",String.class).invoke(dictionary,"Click to view!").equals("Нажмите, чтобы посмотреть!"),"real client offline GUI dictionary");
                check(dictionary.getClass().getMethod("translate",String.class).invoke(dictionary,"I Heart Ice Cream").equals("Я люблю мороженое"),"prepared bulk117-007 preserved");
                try{Class.forName("ru.wither.skyru.CollectorCapture");throw new AssertionError("private integrated class present");}
                catch(ClassNotFoundException expected){checks++;}
                check(Thread.getAllStackTraces().keySet().stream().noneMatch(t->t.getName().startsWith("WitherSkyCollector")),"no integrated private workers");
                check(!Files.exists(Path.of("config/witherskyru/collector-status.json")),"no integrated diagnostic side effects");
            }
            finish(mc,true,"A/B/C startup and actual translator initialization; offline test identity, no game server");
        }catch(Throwable failure){failure.printStackTrace();finish(mc,false,failure.toString());}
    }
    private static void finish(Minecraft mc,boolean pass,String reason){done=true;
        try{Files.writeString(Path.of("public-smoke-result.txt"),(pass?"PASS":"FAIL")+"\nchecks="+checks+"\n"+reason+"\n");}catch(Exception ignored){}
        System.out.println("PUBLIC117_CLIENT "+(pass?"PASS":"FAIL")+" checks="+checks+" "+reason);mc.stop();
    }
}
