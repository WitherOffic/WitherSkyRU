import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import ru.wither.skyru.*;

/** Public distribution boundary and retained local unknown writer. No private service or token. */
public final class TestPublicDistribution117 {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static void absent(String type)throws Exception {
        try { Class.forName(type,false,SkyRuClient.class.getClassLoader());throw new AssertionError("Private class remains: "+type); }
        catch(ClassNotFoundException expected){checks++;}
    }
    public static void main(String[] args)throws Exception {
        for(String type:List.of("CollectorCapture","CollectorConsent","CollectorConsentScreen","CollectorEnrollment",
                "CollectorEnvironment","CollectorOfflineSpool","CollectorSourcePolicy","CollectorTelemetry"))
            absent("ru.wither.skyru."+type);
        absent("ru.wither.skyru.mixin.CollectorLifecycleMixin");
        check(Arrays.stream(RenderContext.Snapshot.class.getRecordComponents()).map(c->c.getName()).toList()
            .equals(List.of("protectedText","hud","tooltip","customScoreboard")),"only translation render context remains");
        check(Arrays.stream(OfflineDictionary.class.getDeclaredMethods()).noneMatch(m->m.getName().equals("collectorReason")),"no private worker API");
        Path root=Path.of(args[0]);Files.createDirectories(root);
        var db=new OfflineDictionary(root);db.setRecordUnknown(true);
        check(db.translate("Click to view!").equals("Нажмите, чтобы посмотреть!"),"offline GUI translation without any other mod");
        check(db.patternCount()>1000,"dynamic templates retained");
        String unknown="Unrecognized public distribution fixture ZXQ117";
        check(db.translate(unknown).equals(unknown),"unknown text unchanged");
        // Bounded wait for the existing asynchronous local writer, never a networking test.
        Path log=root.resolve("unknown.tsv");long until=System.nanoTime()+3_000_000_000L;
        while(System.nanoTime()<until&&(!Files.exists(log)||!Files.readString(log,StandardCharsets.UTF_8).contains(unknown)))Thread.sleep(20);
        check(Files.exists(log)&&Files.readString(log,StandardCharsets.UTF_8).contains(unknown),"local unknown.tsv still works");
        check(Thread.getAllStackTraces().keySet().stream().noneMatch(t->t.getName().startsWith("WitherSkyCollector")),"no private lifecycle/HTTP workers");
        System.out.println("PASS public distribution117: "+checks+" assertions; private classes/API absent, translation and bounded local unknown logging retained");
    }
}
