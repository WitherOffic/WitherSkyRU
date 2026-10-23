package ru.wither.skyru;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Properties;
import java.util.List;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

/** Display-only fonts. Built-in licensed Inter asset; no item mutation or system font loading. */
public final class TooltipFont {
    public enum Choice {
        ORIGINAL("Текущий"), DEFAULT("Minecraft"), UNIFORM("Unicode"), INTER_MEDIUM("Inter Medium");
        public final String label;
        Choice(String label) { this.label = label; }
        public Choice next() { return values()[(ordinal() + 1) % values().length]; }
        public static Choice parse(String name) {
            try { return valueOf(name.toUpperCase(Locale.ROOT)); }
            catch (RuntimeException ex) { return ORIGINAL; }
        }
    }
    private static volatile Choice choice = Choice.ORIGINAL;
    private static Path settingsFile;
    private static final FontDescription UNIFORM = new FontDescription.Resource(Identifier.withDefaultNamespace("uniform"));
    private static final FontDescription INTER_MEDIUM = new FontDescription.Resource(Identifier.fromNamespaceAndPath("witherskyru", "inter_medium"));
    private TooltipFont() {}
    public static Choice choice() { return choice; }
    public static void load(Path file, Properties properties) {
        settingsFile = file;
        choice = Choice.parse(properties.getProperty("tooltipFont", "original"));
    }
    public static boolean active() { return choice != Choice.ORIGINAL && SkyRuClient.active(); }

    /** Save only our keys; preserve other settings. Publish live state only after successful save. */
    public static synchronized void save(Choice selected, boolean wrap) throws IOException {
        if (settingsFile == null) throw new IOException("Настройки ещё не загружены");
        Properties properties = new Properties();
        if (Files.exists(settingsFile)) {
            try (var input = Files.newInputStream(settingsFile)) { properties.load(input); }
        }
        properties.setProperty("tooltipFont", selected.name().toLowerCase(Locale.ROOT));
        properties.setProperty("fitTooltipText", Boolean.toString(wrap));
        Path temporary = Files.createTempFile(settingsFile.toAbsolutePath().getParent(), "witherskyru-font-", ".tmp");
        try {
            try (var output = Files.newOutputStream(temporary)) { properties.store(output, "SkyblockRPG_RU settings"); }
            try { Files.move(temporary, settingsFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, settingsFile, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
        choice = selected;
        SkyRuClient.setTooltipWrap(wrap);
    }

    public static FormattedCharSequence apply(FormattedCharSequence source) { return apply(source, choice); }
    /** Snapshot spacing from actual styled glyphs, not a global setting at draw time. */
    public static int minimumRowHeight(List<FormattedCharSequence> lines) {
        for (var line : lines) {
            boolean[] inter = {false};
            line.accept((index, style, point) -> {
                inter[0] = style.getFont().equals(INTER_MEDIUM);
                return !inter[0];
            });
            if (inter[0]) return 12;
        }
        return 0;
    }
    public static FormattedCharSequence apply(FormattedCharSequence source, Choice selected) {
        if (source == null || selected == Choice.ORIGINAL) return source;
        FontDescription target = switch (selected) {
            case UNIFORM -> UNIFORM;
            case INTER_MEDIUM -> INTER_MEDIUM;
            default -> FontDescription.DEFAULT;
        };
        // Lazy glyph copy preserves indexes, styling, and early termination. Explicit custom fonts
        // and icon glyphs stay intact; normal default/uniform/our Inter runs may switch.
        return sink -> source.accept((index, style, point) -> sink.accept(index, fontStyle(style, point, target), point));
    }
    private static Style fontStyle(Style style, int point, FontDescription target) {
        var existing = style.getFont();
        boolean ordinary = point >= 32 && point <= 126 || point >= 0x00a0 && point <= 0x024f
            || point >= 0x0400 && point <= 0x052f;
        if (!ordinary || !(existing.equals(FontDescription.DEFAULT) || existing.equals(UNIFORM) || existing.equals(INTER_MEDIUM))) return style;
        return existing.equals(target) ? style : style.withFont(target);
    }
}
