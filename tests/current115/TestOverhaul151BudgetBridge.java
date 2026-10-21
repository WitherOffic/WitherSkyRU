import java.nio.file.*;
import java.lang.reflect.*;
import java.time.Instant;
import java.util.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;

/** Actual 1.5.1 bytecode, proxy font/draw only; no vendor/game entrypoints. */
public final class TestOverhaul151BudgetBridge {
    private static int checks;
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    private static void field(Object object,String name,Object value)throws Exception{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
    private static Object blank(Class<?> type)throws Exception{var u=Class.forName("sun.misc.Unsafe");var f=u.getDeclaredField("theUnsafe");f.setAccessible(true);return u.getMethod("allocateInstance",Class.class).invoke(f.get(null),type);}
    public static void main(String[] args)throws Exception{
        var knot=new Knot(EnvType.CLIENT);var loader=knot.init(new String[]{"--gameDir",Path.of(args[0]).toAbsolutePath().toString(),"--version","26.2"});
        var rendererType=Class.forName("me.jfenn.scoreboardoverhaul.common.hud.HudRenderer",true,loader);
        check(Arrays.stream(rendererType.getDeclaredMethods()).filter(m->m.getName().contains("witherskyru$")).count()==2,"exactly new handlers, not old API");
        var configType=Class.forName("me.jfenn.scoreboardoverhaul.common.config.ScoreboardConfig",true,loader);
        var config=configType.getConstructor().newInstance();configType.getMethod("setUpdateHighlight",boolean.class).invoke(config,false);configType.getMethod("setUpdateFlicker",boolean.class).invoke(config,false);
        var stateType=Class.forName("me.jfenn.scoreboardoverhaul.common.hud.HudState",true,loader);var state=blank(stateType);field(state,"config",config);
        var renderer=blank(rendererType);field(renderer,"config",config);field(renderer,"state",state);
        var component=Class.forName("net.minecraft.network.chat.Component",true,loader);var literal=component.getMethod("literal",String.class);var getString=component.getMethod("getString");
        var client=Class.forName("ru.wither.skyru.SkyRuClient",true,loader);var dictionary=Class.forName("ru.wither.skyru.OfflineDictionary",true,loader);
        var db=dictionary.getConstructor(Path.class).newInstance(Path.of(args[0]).resolve("test-config"));dictionary.getMethod("setRecordUnknown",boolean.class).invoke(db,false);
        for(String key:List.of("dictionary","enabled","onlyHypixel")){var f=client.getDeclaredField(key);f.setAccessible(true);f.set(null,key.equals("dictionary")?db:key.equals("enabled"));}
        var enabledField=client.getDeclaredField("enabled");enabledField.setAccessible(true);
        var align=Class.forName("net.minecraft.client.gui.TextAlignment",true,loader);var left=align.getField("LEFT").get(null);
        var budget=Class.forName("ru.wither.skyru.OverhaulTextBudget",true,loader).getMethod("available",align,int.class,int.class);
        var fontType=Class.forName("me.jfenn.scoreboardoverhaul.api.IFont",true,loader);
        var font=Proxy.newProxyInstance(loader,new Class<?>[]{fontType},(p,m,a)->switch(m.getName()){
            case "getTextWidth"->((String)getString.invoke(a[0])).length()*4;
            case "truncate"->{String original=(String)getString.invoke(a[0]);int limit=Math.max(0,(Integer)a[1]/4);yield literal.invoke(null,original.substring(0,Math.min(original.length(),limit)));}
            default->throw new AssertionError("Unexpected font API "+m);
        });
        var drawType=Class.forName("me.jfenn.scoreboardoverhaul.api.IDrawService",true,loader);
        var rows=new ArrayList<String>();var capacities=new ArrayList<Integer>();var inputs=new ArrayList<Object>();boolean[] throwing={false};
        var draw=Proxy.newProxyInstance(loader,new Class<?>[]{drawType},(p,m,a)->{
            if(m.getName().equals("getFont"))return font;
            if(m.getName().equals("drawText")){
                String original=(String)getString.invoke(a[0]);capacities.add((Integer)budget.invoke(null,left,(Integer)a[1],original.length()*4));inputs.add(a[0]);
                Object translated=client.getMethod("component",component).invoke(null,a[0]);rows.add((String)getString.invoke(translated));
                if(throwing[0]&&rows.size()==2)throw new IllegalStateException("synthetic draw failure");return null;
            }
            throw new AssertionError("Unexpected draw API "+m);
        });
        var scoreType=Class.forName("me.jfenn.scoreboardoverhaul.api.data.ScoreInfo",true,loader);
        var constructor=scoreType.getConstructor(String.class,component,int.class,component,component);
        var entry=rendererType.getDeclaredMethod("drawScoreEntry",drawType,scoreType,int.class,int.class,int.class);entry.setAccessible(true);
        var pairs=new String[][]{{"Normal","Обычный режим"},{"- Mining Fiesta","- Горнодобывающая фиеста"},
            {"Wait for Beth to revisit your Garden again","Дождитесь следующего визита Beth в ваш Сад"},
            {"Sawdust: 5,011 (+2,283)","Опилки: 5,011 (+2,283)"},{"Sawdust: 59,481","Опилки: 59,481"},
            {"   Cleanup: 52.1%","   Очистка: 52.1%"},{"SKYBLOCK","SKYBLOCK"}};
        for(boolean enabled:List.of(true,false))for(boolean expanded:List.of(false,true))for(boolean truncate:List.of(false,true))
        for(int width:List.of(120,160,220))for(var number:new String[][]{{"",""},{"7","7"},{"12.3k","12,345"}})for(var pair:pairs)for(int repeat=0;repeat<5;repeat++){
            rows.clear();capacities.clear();inputs.clear();enabledField.set(null,enabled);stateType.getMethod("setExpanded",boolean.class).invoke(state,expanded);
            configType.getMethod("setTruncateScoreValues",boolean.class).invoke(config,truncate);
            Object source=literal.invoke(null,pair[0]),compact=literal.invoke(null,number[0]),full=literal.invoke(null,number[1]);
            Object score=constructor.newInstance("original-id",source,12345,compact,full);scoreType.getMethod("setUpdatedAt",Instant.class).invoke(score,Instant.EPOCH);
            field(state,"entries",Map.of("original-id",score));entry.invoke(renderer,draw,score,100,30,width);
            String selected=!expanded&&truncate?number[0]:number[1];int scoreWidth=selected.length()*4,space=width-12-scoreWidth;
            check(rows.size()==2,"native two draws remain");check(rows.getFirst().equals(selected),"compact/expanded score unchanged");
            check(inputs.getFirst()==(!expanded&&truncate?compact:full),"original score component/styles/actions retained");
            check(capacities.getFirst()==scoreWidth,"numeric draw not given name budget");check(capacities.getLast()==space,"actual reserved score width");
            boolean clipped=pair[0].length()*4>space;
            String clippedText=pair[0].substring(0,Math.min(pair[0].length(),Math.max(0,space/4)));
            // Clipped prefixes may be unknown. This checks the bridge against the
            // existing dictionary policy, not invented translations of lost text.
            String clippedTarget=(String)dictionary.getMethod("translate",String.class).invoke(db,clippedText);
            String expected=enabled?(clipped?clippedTarget:pair[1]):(clipped?clippedText:pair[0]);
            check(rows.getLast().equals(expected),"full/clipped translated name "+rows);check(getString.invoke(source).equals(pair[0]),"original label unchanged");
            check(scoreType.getMethod("getId").invoke(score).equals("original-id"),"original ID");check((Integer)budget.invoke(null,left,104,20)==20,"scope removed after every repeat");
        }
        enabledField.set(null,true);stateType.getMethod("setExpanded",boolean.class).invoke(state,false);configType.getMethod("setTruncateScoreValues",boolean.class).invoke(config,true);
        rows.clear();throwing[0]=true;var failure=constructor.newInstance("test",literal.invoke(null,"Normal"),0,literal.invoke(null,""),literal.invoke(null,""));
        scoreType.getMethod("setUpdatedAt",Instant.class).invoke(failure,Instant.EPOCH);
        try{entry.invoke(renderer,draw,failure,100,30,160);throw new AssertionError("exception missing");}catch(InvocationTargetException e){check(e.getCause() instanceof IllegalStateException,"vendor exception preserved");}
        check((Integer)budget.invoke(null,left,104,20)==20,"exception cleanup");
        System.out.println("PASS: actual Overhaul1.5.1 "+checks+" assertions; compact/expanded score components, current name budget, clipping/brand/disabled/five repeats/exception cleanup; proxy font, not live rendering");
    }
}
