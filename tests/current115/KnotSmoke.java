import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.knot.Knot;

/** Applies the real mixin transformer without starting the game or connecting to a server. */
public final class KnotSmoke {
    public static void main(String[] args) throws Exception {
        Path gameDir = Path.of(args[0]).toAbsolutePath();
        Path config = gameDir.resolve("config/witherskyru");
        Files.createDirectories(config);
        Files.writeString(config.resolve("settings.properties"),
            "enabled=true\nonlyHypixel=false\nrecordUnknown=false\n", StandardCharsets.UTF_8);
        Knot knot = new Knot(EnvType.CLIENT);
        ClassLoader target = knot.init(new String[] {"--gameDir", gameDir.toString(), "--version", "26.1.2"});
        if (!FabricLoader.getInstance().isModLoaded("witherskyru")) {
            throw new AssertionError("WitherSkyRU was not discovered");
        }
        System.out.println("Mixin phase: " + org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().getPhase());
        System.out.println("Registered mixin configurations: " + org.spongepowered.asm.mixin.Mixins.getConfigs());
        knot.addToClassPath(Path.of(args[1]).toAbsolutePath());
        for (String name : new String[] {
            "net.minecraft.client.gui.GuiGraphicsExtractor",
            "net.minecraft.client.gui.components.AbstractWidget"
        }) {
            Class<?> loaded = Class.forName(name, false, target);
            long injected = java.util.Arrays.stream(loaded.getDeclaredMethods())
                .filter(method -> method.getName().contains("witherskyru$")).count();
            long expected = name.endsWith("GuiGraphicsExtractor") ? 7 : 1;
            if (injected != expected) throw new AssertionError("Expected " + expected + " injected methods in " + name + ", got " + injected);
            byte[] transformed = knot.getClassByteArray(name, true);
            // The exported classes are inspected separately to confirm all injections were applied.
            if (transformed == null) throw new AssertionError("No bytecode for " + name);
            System.out.println("PASS: transformed " + name);
        }
        try {
            Class.forName("TestOffline", true, target).getMethod("main", String[].class)
                .invoke(null, (Object) new String[0]);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new AssertionError("Runtime test failed", e.getCause());
        }
        System.out.println("PASS: real Fabric " + FabricLoader.getInstance().getModContainer("fabricloader")
            .orElseThrow().getMetadata().getVersion().getFriendlyString() + " / Minecraft 26.1.2 smoke test");
    }
}
