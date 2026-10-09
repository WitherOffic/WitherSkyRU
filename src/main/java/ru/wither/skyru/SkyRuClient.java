package ru.wither.skyru;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import java.util.concurrent.atomic.AtomicLong;

/** Local-only SkyBlock translation, no requests, no changes to game packets. */
public final class SkyRuClient implements ClientModInitializer {
    private static volatile OfflineDictionary dictionary;
    private static volatile NpcDialogueTranslator npcDialogues;
    private static volatile ChatNotificationTranslator chatNotifications;
    private static volatile boolean translateNpcChat = true;
    private static volatile boolean translateGameChat = true;
    private static volatile boolean enabled = true;
    private static volatile boolean onlyHypixel = true;
    private static final AtomicLong lastChecked = new AtomicLong();
    private static volatile boolean onHypixel;
    private static volatile boolean fitHudText = true;
    private static volatile boolean compactHudLabels = true;
    private static volatile boolean fitTooltipText = true;

    @Override public void onInitializeClient() {
        try {
            Path config = FabricLoader.getInstance().getConfigDir().resolve("witherskyru");
            java.nio.file.Files.createDirectories(config);
            Path flags = config.resolve("settings.properties");
            java.util.Properties props = new java.util.Properties();
            if (java.nio.file.Files.exists(flags)) {
                try (var input = java.nio.file.Files.newInputStream(flags)) { props.load(input); }
            } else {
                props.setProperty("enabled","true");
                props.setProperty("onlyHypixel","true");
                props.setProperty("recordUnknown","true");
                props.setProperty("translateNpcChat","true");
                props.setProperty("translateGameChat","true");
                props.setProperty("fitHudText","true");
                props.setProperty("compactHudLabels","true");
                props.setProperty("fitTooltipText","true");
                try (var output=java.nio.file.Files.newOutputStream(flags)) { props.store(output,"WitherSkyRU - restart Minecraft after edits"); }
            }
            enabled = Boolean.parseBoolean(props.getProperty("enabled","true"));
            onlyHypixel = Boolean.parseBoolean(props.getProperty("onlyHypixel","true"));
            translateNpcChat = Boolean.parseBoolean(props.getProperty("translateNpcChat","true"));
            translateGameChat = Boolean.parseBoolean(props.getProperty("translateGameChat","true"));
            fitHudText = Boolean.parseBoolean(props.getProperty("fitHudText","true"));
            compactHudLabels = Boolean.parseBoolean(props.getProperty("compactHudLabels","true"));
            fitTooltipText = Boolean.parseBoolean(props.getProperty("fitTooltipText","true"));
            dictionary = new OfflineDictionary(config);
            dictionary.setRecordUnknown(Boolean.parseBoolean(props.getProperty("recordUnknown","true")));
            chatNotifications = new ChatNotificationTranslator(dictionary);
            System.out.println("[WitherSkyRU] Offline ready: "+dictionary.phraseCount()+" phrases, "+dictionary.patternCount()+" patterns. No AI or network used.");
            try {
                npcDialogues = new NpcDialogueTranslator();
                System.out.println("[WitherSkyRU] Exact NPC dialogue entries: " + npcDialogues.phraseCount());
            } catch (Exception npcError) {
                System.err.println("[WitherSkyRU] NPC translation disabled: " + npcError);
            }
        } catch (Exception ex) {
            System.err.println("[WitherSkyRU] Failed to load offline dictionary; mod disabled: "+ex);
            enabled = false;
        }
    }

    public static boolean active() {
        if (!enabled || dictionary == null) return false;
        if (!onlyHypixel) return true;
        long now=System.nanoTime();
        if (now-lastChecked.get()>2_000_000_000L && lastChecked.getAndSet(now)<now-2_000_000_000L) {
            try {
                Minecraft mc=Minecraft.getInstance();
                ServerData data=mc.getCurrentServer();
                String ip = data==null?"":data.ip.toLowerCase(java.util.Locale.ROOT);
                onHypixel = ip.equals("hypixel.net") || ip.endsWith(".hypixel.net");
            } catch(Throwable ignored) { onHypixel=false; }
        }
        return onHypixel;
    }

    public static String text(String source) {
        if (source==null || !active()) return source;
        return dictionary.translate(source);
    }

    public static boolean hudTextFitActive() {
        if (!fitHudText || !active()) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.level != null && mc.screen == null;
    }

    public static boolean tooltipTextFitActive() { return fitTooltipText && active(); }

    public static net.minecraft.client.multiplayer.chat.GuiMessage npcChat(net.minecraft.client.multiplayer.chat.GuiMessage message) {
        if (message == null || !active()
            || message.source() == net.minecraft.client.multiplayer.chat.GuiMessageSource.PLAYER
            || message.signature() != null) return message;
        try {
            Component translated = message.content();
            if (translateNpcChat && npcDialogues != null
                && message.source() == net.minecraft.client.multiplayer.chat.GuiMessageSource.SYSTEM_SERVER)
                translated = npcDialogues.translate(translated);
            if (translated == message.content() && translateGameChat && chatNotifications != null)
                translated = chatNotifications.translate(translated, message.source(), false);
            if (translated == message.content()) return message;
            return new net.minecraft.client.multiplayer.chat.GuiMessage(
                message.addedTime(), translated, message.signature(), message.source(), message.tag());
        } catch (Throwable ignored) { return message; }
    }

    public static Component component(Component source) {
        if (source==null || !active()) return source;
        // Never flatten styled Component trees: the Style carries clickEvent/hoverEvent,
        // and siblings carry distinct styling. Translate only literal leaves.
        try { return translateTree(source, 0); }
        catch (Throwable e) { return source; }
    }

    private static Component translateTree(Component original, int depth) {
        if (depth>18) return original;
        List<Component> siblings = original.getSiblings();
        String base = original.getContents() instanceof PlainTextContents plain ? plain.text() : null;
        String translated = base == null ? null : dictionary.translate(base);
        boolean edited = base != null && !base.equals(translated);
        List<Component> children = new ArrayList<>(siblings.size());
        for (Component sibling : siblings) {
            Component next=translateTree(sibling,depth+1);
            children.add(next);
            if (next!=sibling) edited=true;
        }
        if (!edited) return original;
        // Non-literal contents (e.g. vanilla translation keys) are preserved as-is.
        MutableComponent rebuilt=base == null ? original.plainCopy() : Component.literal(translated).setStyle(original.getStyle());
        for(Component sibling:children) rebuilt.append(sibling);
        return rebuilt;
    }

    /** HUD/scoreboards often reach Minecraft already split into styled glyph runs. */
    public static FormattedCharSequence sequence(FormattedCharSequence source) {
        return sequence(source, false);
    }

    public static FormattedCharSequence sequence(FormattedCharSequence source, boolean hudPanel) {
        if (source == null || !active()) return source;
        try {
            List<FormattedCharSequence> result = new ArrayList<>();
            StringBuilder text = new StringBuilder();
            Style[] style = {null};
            boolean[] changed = {false};
            Runnable flush = () -> {
                if (text.isEmpty()) return;
                String old = text.toString();
                String next = dictionary.translate(old);
                if (hudPanel && compactHudLabels) next = HudTextFit.compact(next);
                changed[0] |= !old.equals(next);
                result.add(FormattedCharSequence.forward(next, style[0]));
                text.setLength(0);
            };
            source.accept((index, currentStyle, codePoint) -> {
                if (!java.util.Objects.equals(style[0], currentStyle)) flush.run();
                style[0] = currentStyle;
                text.appendCodePoint(codePoint);
                return true;
            });
            flush.run();
            return changed[0] ? FormattedCharSequence.composite(result) : source;
        } catch (Throwable ignored) { return source; }
    }
}
