import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.lang.reflect.Proxy;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.mojang.blaze3d.font.*;
import net.minecraft.client.gui.font.providers.*;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/** Loads the shipped TTF through Minecraft/FreeType without a window, GPU upload or game loop. */
public class TestInterFontAsset {
    private static int checks;
    private static void check(String label, boolean ok) { if (!ok) throw new AssertionError(label); checks++; }
    private static byte[] asset(String name) throws IOException {
        try (var input = TestInterFontAsset.class.getClassLoader().getResourceAsStream(name)) {
            if (input == null) throw new FileNotFoundException("Packaged resource missing: " + name);
            return input.readAllBytes();
        }
    }
    private static int weight(byte[] bytes) {
        var buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        int tables = Short.toUnsignedInt(buffer.getShort(4));
        for (int i = 0; i < tables; i++) {
            int row = 12 + 16 * i;
            if (buffer.getInt(row) == 0x4f532f32) return Short.toUnsignedInt(buffer.getShort(buffer.getInt(row + 8) + 4));
        }
        throw new AssertionError("OS/2 table missing");
    }
    public static void main(String[] args) throws Exception {
        byte[] ttf = asset("assets/witherskyru/font/inter_medium.ttf");
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(ttf));
        check("unmodified official Inter 4.1", hash.equals("97ad806f526e41546d46365bb3a393145f75b7b1568913db74549ad8b8dba872"));
        check("medium rather than heavy bold", weight(ttf) == 500);
        String license = new String(asset("META-INF/licenses/Inter-OFL-1.1.txt"), java.nio.charset.StandardCharsets.UTF_8);
        check("copyright bundled", license.contains("The Inter Project Authors"));
        check("font license bundled", license.contains("SIL OPEN FONT LICENSE Version 1.1") && license.contains("TERMINATION") && license.contains("DISCLAIMER"));
        JsonObject definition = JsonParser.parseString(new String(asset("assets/witherskyru/font/inter_medium.json"), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        var providers = definition.getAsJsonArray("providers");
        var first = GlyphProviderDefinition.MAP_CODEC.codec().parse(JsonOps.INSTANCE, providers.get(0)).getOrThrow();
        check("actual MC TTF codec", first instanceof TrueTypeGlyphProviderDefinition);
        var ttfDefinition = (TrueTypeGlyphProviderDefinition) first;
        check("oversampling moderate", ttfDefinition.oversample() == 3 && ttfDefinition.size() == 9);
        var fallback = GlyphProviderDefinition.MAP_CODEC.codec().parse(JsonOps.INSTANCE, providers.get(1)).getOrThrow();
        check("fallback to vanilla", fallback.unpack().right().orElseThrow().id().equals(Identifier.withDefaultNamespace("default")));
        Identifier expected = Identifier.fromNamespaceAndPath("witherskyru", "font/inter_medium.ttf");
        ResourceManager resources = (ResourceManager)Proxy.newProxyInstance(TestInterFontAsset.class.getClassLoader(), new Class[]{ResourceManager.class}, (proxy, method, values) -> {
            if (method.getName().equals("open")) {
                check("actual resource path resolved", expected.equals(values[0]));
                return new ByteArrayInputStream(ttf);
            }
            if (method.getName().equals("toString")) return "IsolatedInterResourceManager";
            throw new UnsupportedOperationException(method.toString());
        });
        String alphabet = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюяABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-.,:()/ %";
        float[] bounds = {Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY};
        try (var provider = first.unpack().left().orElseThrow().load(resources)) {
            check("loaded real MC FreeType provider", provider instanceof TrueTypeGlyphProvider);
            alphabet.codePoints().forEach(cp -> {
                check("Cyrillic/Latin glyph supported " + cp, provider.getSupportedGlyphs().contains(cp));
                var glyph = provider.getGlyph(cp);
                check("glyph available " + cp, glyph != null);
                check("finite advance " + cp, Float.isFinite(glyph.info().getAdvance()) && glyph.info().getAdvance() > 0);
                glyph.bake(new UnbakedGlyph.Stitcher() {
                    public BakedGlyph getMissing() { throw new AssertionError("Unexpected missing glyph"); }
                    public BakedGlyph stitch(GlyphInfo info, GlyphBitmap bitmap) {
                        check("oversampling on native bitmap", bitmap.getOversample() == 3);
                        bounds[0] = Math.min(bounds[0], bitmap.getTop()); bounds[1] = Math.max(bounds[1], bitmap.getBottom());
                        // Intentionally no bitmap.upload, GPU texture, or BakedGlyph allocation.
                        return null;
                    }
                });
            });
            check("empty unknown handled", provider.getGlyph(0x10ffff) == null);
        }
        check("glyphs fit normal row height " + Arrays.toString(bounds), bounds[0] >= -1 && bounds[1] <= 10);
        var styled = ru.wither.skyru.TooltipFont.apply(net.minecraft.util.FormattedCharSequence.forward("Ё Й Ц", net.minecraft.network.chat.Style.EMPTY), ru.wither.skyru.TooltipFont.Choice.INTER_MEDIUM);
        int spacing = ru.wither.skyru.TooltipFont.minimumRowHeight(List.of(styled));
        check("native bounds plus shadow fit spacing", spacing >= bounds[1] - bounds[0] + 1);
        var oldRow = new net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip(styled);
        var prepared = new ru.wither.skyru.TooltipTextFit.PreparedText(oldRow, List.of(styled, styled));
        var dummyFont = new net.minecraft.client.gui.Font(null);
        check("background height updated before drawing", prepared.getHeight(dummyFont) == spacing * 2);
        var awt = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, new ByteArrayInputStream(ttf));
        check("family name", awt.getFontName(Locale.ROOT).equals("Inter Medium"));
        check("all Russian glyphs display", awt.canDisplayUpTo(alphabet) == -1);
        Path output = Path.of(args[0]); Files.createDirectories(output);
        var image = new java.awt.image.BufferedImage(820, 220, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(new java.awt.Color(22, 23, 27)); graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setFont(awt.deriveFont(24f)); graphics.setColor(new java.awt.Color(215, 218, 224));
            graphics.drawString("Inter Medium · 500", 28, 45);
            graphics.setFont(awt.deriveFont(27f)); graphics.setColor(new java.awt.Color(106, 224, 213));
            graphics.drawString("Алмазная кирка · Здоровье +120", 28, 94);
            graphics.setColor(new java.awt.Color(222, 224, 230));
            graphics.drawString("Ёжик, защита, редкость · COMMON / EPIC", 28, 142);
            graphics.setFont(awt.deriveFont(16f)); graphics.setColor(new java.awt.Color(160, 164, 175));
            graphics.drawString("Образец TTF, не скриншот Minecraft", 28, 191);
        } finally { graphics.dispose(); }
        javax.imageio.ImageIO.write(image, "png", output.resolve("inter-medium-preview.png").toFile());
        System.out.println("PASS: Inter TTF asset " + checks + " assertions; native bounds=" + Arrays.toString(bounds));
    }
}
