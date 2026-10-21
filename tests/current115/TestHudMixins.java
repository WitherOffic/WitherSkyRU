import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;

/** Transform classes only. Never runs Minecraft.main(), renders, or connects. */
public class TestHudMixins {
    public static void main(String[] args) throws Exception {
        Knot knot = new Knot(EnvType.CLIENT);
        ClassLoader loader = knot.init(new String[]{"--gameDir", Path.of(args[0]).toAbsolutePath().toString(), "--version", "26.2"});
        String[] targets = {"net.minecraft.client.gui.GuiGraphicsExtractor",
            "net.minecraft.client.gui.components.AbstractWidget",
            "net.minecraft.client.gui.GuiGraphicsExtractor$RenderingTextCollector",
            "net.minecraft.client.gui.components.ChatComponent",
            "net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip",
            "net.minecraft.client.gui.screens.inventory.AbstractContainerScreen",
            "net.minecraft.client.gui.screens.options.OptionsScreen",
            "net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen",
            "net.minecraft.client.renderer.entity.EntityRenderer",
            "net.minecraft.client.gui.components.DebugScreenOverlay",
            "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess",
            "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess",
            "net.minecraft.client.gui.components.ChatComponent$ClickableTextOnlyGraphicsAccess"};
        // OptionsScreen: two handlers plus the generated button callback.
        int[] expected = {12, 1, 1, 1, 1, 1, 3, 5, 1, 1, 1, 1, 1};
        for (int i = 0; i < targets.length; i++) {
            Class<?> type = Class.forName(targets[i], false, loader);
            long actual = java.util.Arrays.stream(type.getDeclaredMethods())
                .filter(m -> m.getName().contains("witherskyru$")).count();
            if (actual != expected[i]) throw new AssertionError(targets[i] + ": " + actual + " != " + expected[i]);
            System.out.println("PASS: transformed " + targets[i] + " (" + actual + " handlers)");
        }
        Class<?> style = Class.forName("net.minecraft.network.chat.Style", true, loader);
        Class<?> sequence = Class.forName("net.minecraft.util.FormattedCharSequence", true, loader);
        Object text = sequence.getMethod("forward", String.class, style)
            .invoke(null, "Click to TP to it!", style.getField("EMPTY").get(null));
        Class<?> tooltip = Class.forName(targets[4], true, loader);
        Object row = tooltip.getConstructor(sequence).newInstance(text);
        Class<?> accessor = Class.forName("ru.wither.skyru.mixin.TextTooltipAccessor", false, loader);
        if (!accessor.isInstance(row)) throw new AssertionError("Tooltip accessor interface missing");
        if (accessor.getMethod("witherskyru$text").invoke(row) != text)
            throw new AssertionError("Tooltip accessor changed the original sequence");
        System.out.println("PASS: real transformed tooltip accessor returns the original sequence");
        Class<?> technical=Class.forName("ru.wither.skyru.TechnicalText",true,loader);
        Class<?> debug=Class.forName(targets[9],false,loader);
        var debugHandler=java.util.Arrays.stream(debug.getDeclaredMethods()).filter(m->m.getName().contains("witherskyru$debugText")).findFirst().orElseThrow();
        debugHandler.setAccessible(true);
        try{debugHandler.invoke(null,null,null,"Normal",0,0,-1,false);throw new AssertionError("Expected null graphics fixture failure");}
        catch(java.lang.reflect.InvocationTargetException failure){if(!(failure.getCause() instanceof NullPointerException))throw failure;}
        if((Boolean)technical.getMethod("active").invoke(null))throw new AssertionError("Debug scope leaked on exception");
        System.out.println("PASS: transformed DebugOverlay draw handler exception cleans technical scope; not live rendering");
        Class<?> collector=Class.forName("net.minecraft.client.gui.ActiveTextCollector",false,loader);
        Class<?> alignment=Class.forName("net.minecraft.client.gui.TextAlignment",false,loader);Object left=alignment.getField("LEFT").get(null);
        for(int i=10;i<13;i++){
            var draw=java.util.Arrays.stream(Class.forName(targets[i],false,loader).getDeclaredMethods()).filter(m->m.getName().contains("witherskyru$original")).findFirst().orElseThrow();draw.setAccessible(true);
            final int[] forwarded={0};Object output=java.lang.reflect.Proxy.newProxyInstance(loader,new Class<?>[]{collector},(p,m,a)->{
                if(!m.getName().equals("accept")||!(Boolean)technical.getMethod("active").invoke(null))throw new AssertionError("Chat draw not protected");
                if(a[0]!=left||!a[1].equals(17)||!a[2].equals(29)||a[a.length-1]!=text)throw new AssertionError("Chat draw arguments changed");forwarded[0]++;return null;});
            Object[] call=draw.getParameterCount()==6?new Object[]{output,left,17,29,null,text}:new Object[]{output,left,17,29,text};
            draw.invoke(null,call);if(forwarded[0]!=1||(Boolean)technical.getMethod("active").invoke(null))throw new AssertionError("Chat draw forwarding/cleanup");
            try(var outer=(AutoCloseable)technical.getMethod("open").invoke(null)){draw.invoke(null,call);if(!(Boolean)technical.getMethod("active").invoke(null))throw new AssertionError("Outer scope lost");}
            call[0]=null;try{draw.invoke(null,call);throw new AssertionError("Expected null collector fixture failure");}catch(java.lang.reflect.InvocationTargetException failure){if(!(failure.getCause() instanceof NullPointerException))throw failure;}
            if((Boolean)technical.getMethod("active").invoke(null))throw new AssertionError("Chat draw scope leaked on exception");
            System.out.println("PASS: actual transformed chat draw forwards original args/actions, nested restoration and exception cleanup: "+targets[i]);
        }
        if (args.length > 1 && args[1].equals("rrv")) {
            Class<?> filters = Class.forName("cc.cassian.rrv.common.overlay.itemlist.view.ItemFilters", false, loader);
            long count = java.util.Arrays.stream(filters.getDeclaredMethods())
                .filter(m -> m.getName().contains("witherskyru$searchName") || m.getName().contains("witherskyru$tooltipSearch")).count();
            if (count != 2) throw new AssertionError("RRV search handlers missing: " + count);
            System.out.println("PASS: real RRV 8.11.1+26.2 search class transformed (2 handlers)");
            Class<?> client = Class.forName("ru.wither.skyru.SkyRuClient", true, loader);
            Class<?> dictionary = Class.forName("ru.wither.skyru.OfflineDictionary", true, loader);
            Object db = dictionary.getConstructor(Path.class).newInstance(Path.of(args[0]).resolve("test-search-config"));
            dictionary.getMethod("setRecordUnknown", boolean.class).invoke(db, false);
            for (String flag : new String[]{"dictionary", "enabled", "onlyHypixel"}) {
                var field = client.getDeclaredField(flag); field.setAccessible(true);
                field.set(null, flag.equals("dictionary") ? db : flag.equals("enabled"));
            }
            Class<?> component = Class.forName("net.minecraft.network.chat.Component", true, loader);
            var literal = component.getMethod("literal", String.class);
            var match = filters.getDeclaredMethod("getListMatch", java.util.List.class, String.class); match.setAccessible(true);
            Object pickaxe = literal.invoke(null, "Diamond Pickaxe");
            java.util.List<Object> rows = java.util.List.of(pickaxe);
            for (String query : new String[]{"кирка", "кир", "кирка алмазная", "Pickaxe"}) {
                int result = (Integer)match.invoke(null, rows, query);
                if (result == 0) throw new AssertionError("Real RRV search failed: " + query);
                System.out.println("PASS: actual RRV getListMatch query " + query + " => " + result);
            }
            Object lowerPickaxe = literal.invoke(null, "diamond pickaxe");
            if ((Integer)match.invoke(null, java.util.List.of(lowerPickaxe), "pickaxe") == 0)
                throw new AssertionError("Native RRV lowercase English search regressed");
            if ((Integer)match.invoke(null, rows, "несуществующий ZX-9921") != 0)
                throw new AssertionError("RRV unknown query matched");
            if (!component.getMethod("getString").invoke(pickaxe).equals("Diamond Pickaxe"))
                throw new AssertionError("RRV original component mutated");
        }
        if (args.length > 2 && args[2].equals("skyrecipes"))
            TestSkyRecipesSearchBridge.run(loader, Path.of(args[0]));
    }
}
