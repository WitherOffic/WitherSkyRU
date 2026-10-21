import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Isolates original-only chat frontend overhead, not native rendering/game FPS. */
public final class BenchmarkProtectedChat94 {
    private static volatile Object sink;
    private static final java.lang.invoke.MethodHandle DRAW;
    static{try{
        var callerLoader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(System.getProperty("witherskyru.fixture.path")).toUri().toURL()},BenchmarkProtectedChat94.class.getClassLoader()){
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
                if(!name.equals("net.minecraft.client.gui.components.ChatComponent"))return super.loadClass(name,resolve);
                synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}
            }
        };
        DRAW=java.lang.invoke.MethodHandles.publicLookup().findStatic(Class.forName("net.minecraft.client.gui.components.ChatComponent",true,callerLoader),"draw",java.lang.invoke.MethodType.methodType(FormattedCharSequence.class,FormattedCharSequence.class,boolean.class));
    }catch(Exception e){throw new ExceptionInInitializerError(e);}}
    private static FormattedCharSequence draw(FormattedCharSequence source,boolean scoped){try{return(FormattedCharSequence)DRAW.invokeExact(source,scoped);}catch(Throwable e){throw new AssertionError(e);}}
    private static void flag(String name,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,v);}
    private static double median(double[] x){var a=x.clone();Arrays.sort(a);return(a[1]+a[2])/2;}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[1]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        boolean scoped=Boolean.parseBoolean(args[2]);var out=new JsonObject();
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);if(!bean.isThreadCpuTimeEnabled())bean.setThreadCpuTimeEnabled(true);long thread=Thread.currentThread().threadId();
        for(String mode:List.of("protected_prose","protected_numeric","protected_fresh_actions")){
            var source=FormattedCharSequence.forward(mode.equals("protected_numeric")?"1,234/5,678 (+12%)":"[VIP] Player: Normal /command Не менять",Style.EMPTY);int[] visits={0};var original=source;FormattedCharSequence counted=s->{visits[0]++;return original.accept(s);};
            check(draw(counted,scoped)==counted,"original sequence identity");check(visits[0]==(scoped?0:1),"no translator traversal for protected scoped draw");
            for(int i=0;i<200000;i++)sink=draw(source,scoped);
            double[] wall=new double[4],cpu=new double[4],bytes=new double[4];int operations=100000;
            for(int r=0;r<4;r++){
                long memory=bean.getThreadAllocatedBytes(thread),startCpu=bean.getCurrentThreadCpuTime(),start=System.nanoTime();
                for(int i=0;i<operations;i++){
                    var input=mode.equals("protected_fresh_actions")?FormattedCharSequence.forward("[VIP] Player: Normal /current "+i,Style.EMPTY.withColor(i).withClickEvent(new ClickEvent.SuggestCommand("/current "+i)).withHoverEvent(new HoverEvent.ShowText(Component.literal("hint "+i)))):source;
                    var result=draw(input,scoped);if(result!=input)throw new AssertionError("protected content/action changed");sink=result;
                }
                wall[r]=(System.nanoTime()-start)/((double)operations);cpu[r]=(bean.getCurrentThreadCpuTime()-startCpu)/((double)operations);bytes[r]=(bean.getThreadAllocatedBytes(thread)-memory)/((double)operations);
            }
            var result=new JsonObject();result.addProperty("ns_per_operation",median(wall));result.addProperty("cpu_ns_per_operation",median(cpu));result.addProperty("bytes_per_operation",median(bytes));result.add("rounds_ns",new Gson().toJsonTree(wall));result.add("rounds_cpu_ns",new Gson().toJsonTree(cpu));result.add("rounds_bytes",new Gson().toJsonTree(bytes));out.add(mode,result);
        }
        check(!TechnicalText.active(),"no scope leaked");out.addProperty("scope","Benchmark-only exact ChatComponent caller shim;same original-only output. Before fresh caller walk,after explicit draw scope.200000 warmup/4x100000 calls;fresh-actions construction included equally. No native collector/font/GPU rendering,live game FPS or draw-frequency measurement. Shim never packaged in mod.");Files.writeString(Path.of(args[0]),new GsonBuilder().setPrettyPrinting().create().toJson(out));
    }
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
}
