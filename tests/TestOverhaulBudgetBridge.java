import java.nio.file.*;
import java.lang.reflect.*;
import java.time.Instant;
import java.util.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;

/** Actual vendor bytecode with proxy draw/font services: no render/client/mod entrypoint. */
public class TestOverhaulBudgetBridge {
    private static int checks;
    private static void check(boolean b,String label){if(!b)throw new AssertionError(label);checks++;}
    private static void field(Object object,String name,Object value)throws Exception {
        var f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);
    }
    private static Object blank(Class<?> type)throws Exception {
        var unsafeType=Class.forName("sun.misc.Unsafe");var f=unsafeType.getDeclaredField("theUnsafe");f.setAccessible(true);
        return unsafeType.getMethod("allocateInstance",Class.class).invoke(f.get(null),type);
    }
    public static void main(String[] args)throws Exception {
        var knot=new Knot(EnvType.CLIENT);var loader=knot.init(new String[]{"--gameDir",Path.of(args[0]).toAbsolutePath().toString(),"--version","26.1.2"});
        var rendererType=Class.forName("me.jfenn.scoreboardoverhaul.common.hud.HudRenderer",true,loader);
        check(Arrays.stream(rendererType.getDeclaredMethods()).filter(m->m.getName().contains("witherskyru$")).count()==2,"both actual vendor handlers applied");
        var configType=Class.forName("me.jfenn.scoreboardoverhaul.common.config.ScoreboardConfig",true,loader);
        var config=configType.getConstructor().newInstance();configType.getMethod("setUpdateHighlight",boolean.class).invoke(config,false);
        configType.getMethod("setUpdateFlicker",boolean.class).invoke(config,false);
        var showType=Class.forName("me.jfenn.scoreboardoverhaul.common.config.ScoreboardConfig$ShowScores",true,loader);
        configType.getMethod("setShowScoreValues",showType).invoke(config,showType.getField("Never").get(null));
        var state=blank(Class.forName("me.jfenn.scoreboardoverhaul.common.hud.HudState",true,loader));field(state,"config",config);
        var renderer=blank(rendererType);field(renderer,"config",config);field(renderer,"state",state);
        var component=Class.forName("net.minecraft.network.chat.Component",true,loader);var literal=component.getMethod("literal",String.class);var getString=component.getMethod("getString");
        var client=Class.forName("ru.wither.skyru.SkyRuClient",true,loader);var dictionary=Class.forName("ru.wither.skyru.OfflineDictionary",true,loader);
        var db=dictionary.getConstructor(Path.class).newInstance(Path.of(args[0]).resolve("test-config"));dictionary.getMethod("setRecordUnknown",boolean.class).invoke(db,false);
        for(String key:List.of("dictionary","enabled","onlyHypixel")){var f=client.getDeclaredField(key);f.setAccessible(true);f.set(null,key.equals("dictionary")?db:key.equals("enabled"));}
        var fit=Class.forName("ru.wither.skyru.HudTextFit",true,loader);var align=Class.forName("net.minecraft.client.gui.TextAlignment",true,loader);var left=align.getField("LEFT").get(null);
        var budget=Class.forName("ru.wither.skyru.OverhaulTextBudget",true,loader).getMethod("available",align,int.class,int.class);
        var fontType=Class.forName("me.jfenn.scoreboardoverhaul.api.IFont",true,loader);
        var font=Proxy.newProxyInstance(loader,new Class<?>[]{fontType},(p,m,a)->switch(m.getName()){
            case "getTextWidth"->((String)getString.invoke(a[0])).length()*4;
            case "truncate"->literal.invoke(null,"Wait for Beth to revisit");
            default->throw new AssertionError("Unexpected font API: "+m);
        });
        var drawType=Class.forName("me.jfenn.scoreboardoverhaul.api.IDrawService",true,loader);
        var rows=new ArrayList<String>();var capacities=new ArrayList<Integer>();boolean[] throwing={false};
        var draw=Proxy.newProxyInstance(loader,new Class<?>[]{drawType},(p,m,a)->{
            if(m.getName().equals("getFont"))return font;
            if(m.getName().equals("drawText")){
                String original=(String)getString.invoke(a[0]);int old=original.length()*4;
                int capacity=(Integer)budget.invoke(null,left,(Integer)a[1],old);capacities.add(capacity);
                Object translated=client.getMethod("component",component).invoke(null,a[0]);rows.add((String)getString.invoke(translated));
                if(throwing[0]&&!original.isEmpty())throw new IllegalStateException("synthetic draw failure");
                return null;
            }
            throw new AssertionError("Unexpected draw API: "+m);
        });
        var scoreType=Class.forName("me.jfenn.scoreboardoverhaul.common.data.ScoreInfo",true,loader);
        var entry=rendererType.getDeclaredMethod("drawScoreEntry",drawType,scoreType,int.class,int.class,int.class);entry.setAccessible(true);
        for(String show:List.of("Never","Always"))for(int width:List.of(120,160,220))for(String[] pair:new String[][]{{"Normal","Обычный режим"},{"- Mining Fiesta","- Горнодобывающая фиеста"},{"Wait for Beth to revisit your Garden again","Дождитесь следующего визита Beth в ваш Сад"},{"Sawdust: 5,011 (+2,283)","Опилки: 5,011 (+2,283)"},{"SKYBLOCK","SKYBLOCK"}}){
            rows.clear();capacities.clear();Object source=literal.invoke(null,pair[0]);var score=scoreType.getConstructor(String.class,component,int.class,Instant.class).newInstance("original-id",source,7,Instant.EPOCH);
            configType.getMethod("setShowScoreValues",showType).invoke(config,showType.getField(show).get(null));field(state,"entries",Map.of("original-id",score));
            entry.invoke(renderer,draw,score,100,30,width);
            int scoreWidth=show.equals("Never")?0:4,space=width-12-scoreWidth;
            check(rows.size()==2,"native score and name calls remain");check(capacities.get(0)==scoreWidth,"numeric score call not given label budget");
            check(rows.getFirst().equals(show.equals("Never")?"":"7"),"numeric score unchanged");check(capacities.get(1)==space,"vendor textWidthMax minus actual score width");
            String expected=pair[0].length()*4>space?"Дождитесь визита Beth":pair[1];check(rows.getLast().equals(expected),"native clipped/full label: "+pair[0]+" => "+rows);
            check(getString.invoke(source).equals(pair[0]),"original source unmodified");check(scoreType.getMethod("getId").invoke(score).equals("original-id"),"ID unchanged");
            check((Integer)budget.invoke(null,left,104,20)==20,"draw scope removed");
            check((Boolean)fit.getMethod("canFit",int.class,int.class).invoke(null,space,expected.length()*4),"synthetic font translation fits actual space");
        }
        configType.getMethod("setShowScoreValues",showType).invoke(config,showType.getField("Never").get(null));
        throwing[0]=true;var failure=scoreType.getConstructor(String.class,component,int.class,Instant.class).newInstance("test",literal.invoke(null,"Normal"),0,Instant.EPOCH);
        try{entry.invoke(renderer,draw,failure,100,30,160);throw new AssertionError("exception missing");}catch(InvocationTargetException e){check(e.getCause() instanceof IllegalStateException,"vendor exception preserved");}
        check((Integer)budget.invoke(null,left,104,20)==20,"exception scope cleaned");
        System.out.println("PASS: actual Overhaul 1.4.3 budget bridge "+checks+" assertions; original truncation/IDs/brand, real row space, proxy font and exception cleanup; no live rendering");
    }
}
