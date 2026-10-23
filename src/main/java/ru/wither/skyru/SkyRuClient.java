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

/** Offline SkyBlock translation, no telemetry or changes to game packets. */
public final class SkyRuClient implements ClientModInitializer {
    private static volatile OfflineDictionary dictionary;
    private static volatile RussianSearchAliases searchAliases;
    private static volatile SignSearchNames signNames;
    private static volatile NpcDialogueTranslator npcDialogues;
    private static volatile NpcDialogueTranslator gardenDialogues;
    private static volatile NpcDialogueTranslator questDialogues;
    private static volatile NpcDialogueTranslator storyDialogues;
    private static volatile NpcDialogueTranslator winterDialogues;
    private static volatile NpcDialogueTranslator questObjectives;
    private static volatile NpcDialogueTranslator dungeonDialogues;
    private static volatile NpcDialogueTranslator dungeonNotifications;
    // Screenshot-confirmed roles only. Never use the whole dictionary to classify entity names.
    private static final java.util.Set<String> NPC_WORLD_ROLES=java.util.Set.of(
        "Thaumaturgist","Maths Enjoyer","Enchantment Nerd","ENCHANTMENT GUIDE");
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
                try (var output=java.nio.file.Files.newOutputStream(flags)) { props.store(output,"SkyblockRPG_RU - restart Minecraft after edits"); }
            }
            enabled = Boolean.parseBoolean(props.getProperty("enabled","true"));
            onlyHypixel = Boolean.parseBoolean(props.getProperty("onlyHypixel","true"));
            translateNpcChat = Boolean.parseBoolean(props.getProperty("translateNpcChat","true"));
            translateGameChat = Boolean.parseBoolean(props.getProperty("translateGameChat","true"));
            fitHudText = Boolean.parseBoolean(props.getProperty("fitHudText","true"));
            compactHudLabels = Boolean.parseBoolean(props.getProperty("compactHudLabels","true"));
            fitTooltipText = Boolean.parseBoolean(props.getProperty("fitTooltipText","true"));
            TooltipFont.load(flags, props);
            dictionary = new OfflineDictionary(config);
            dictionary.setRecordUnknown(Boolean.parseBoolean(props.getProperty("recordUnknown","true")));
            chatNotifications = new ChatNotificationTranslator(dictionary);
            try { winterDialogues=NpcDialogueTranslator.winterDialogues(); }
            catch(Exception winterError){ System.err.println("[SkyblockRPG_RU] Winter dialogue resource unavailable: "+winterError); }
            System.out.println("[SkyblockRPG_RU] Offline ready: "+dictionary.phraseCount()+" phrases, "+dictionary.patternCount()+" patterns. Translation is offline; no telemetry.");
            try {
                npcDialogues = new NpcDialogueTranslator();
                System.out.println("[SkyblockRPG_RU] Exact NPC dialogue entries: " + npcDialogues.phraseCount());
            } catch (Exception npcError) {
                System.err.println("[SkyblockRPG_RU] NPC translation disabled: " + npcError);
            }
            try {
                gardenDialogues=NpcDialogueTranslator.gardenDialogues();
            } catch(Exception gardenError){
                System.err.println("[SkyblockRPG_RU] Garden dialogue resource unavailable: "+gardenError);
            }
            try {
                questDialogues=NpcDialogueTranslator.questDialogues();
                questObjectives=NpcDialogueTranslator.questObjectives();
            } catch(Exception questError){
                System.err.println("[SkyblockRPG_RU] Quest dialogue resource unavailable: "+questError);
            }
            try {
                storyDialogues=NpcDialogueTranslator.storyDialogues();
            } catch(Exception storyError){
                System.err.println("[SkyblockRPG_RU] Story dialogue resource unavailable: "+storyError);
            }
            try {
                dungeonDialogues=NpcDialogueTranslator.dungeonDialogues();
                dungeonNotifications=NpcDialogueTranslator.dungeonNotifications();
            } catch(Exception dungeonError){
                System.err.println("[SkyblockRPG_RU] Dungeon dialogue resource unavailable: "+dungeonError);
            }
        } catch (Exception ex) {
            System.err.println("[SkyblockRPG_RU] Failed to load offline dictionary; mod disabled: "+ex);
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
if (source==null || TechnicalText.active() || !active() || DisplayNoop.text(source) || UserInputText.rendering() || CollectionContributors.protectedRow(source) || PlayerNames.contains(source) || OriginalItemTitle.hoveredName(source)) {return source;}
        String translated=dictionary.translate(source);
        return translated;
    }

    /** Late-bound dictionary for optional search indexes; also honours enabled/server restrictions. */
    public static OfflineDictionary activeSearchDictionary() { return active() ? dictionary : null; }

    /** Skyblocker Bazaar/Auction suggestions: add aliases without rewriting typed or sent text. */
    public static boolean overlaySearchMatch(String original,String query){
        if(original==null||query==null||query.isBlank()||query.length()>100
            ||!RussianSearchAliases.russianQuery(query)||!active())return false;
        try{
            var aliases=searchAliases;
            if(aliases==null)searchAliases=aliases=new RussianSearchAliases(dictionary::translateForSearch);
            String searchable=aliases.name(original,query).toLowerCase(java.util.Locale.ROOT).replace('ё','е');
            String normalized=query.strip().toLowerCase(java.util.Locale.ROOT).replace('ё','е');
            return java.util.Arrays.stream(normalized.split("\\s+")).allMatch(searchable::contains);
        }catch(RuntimeException unsupported){return false;}
    }

    /** Only explicit sign-helper clicks use these suggestions; typing/chat are never rewritten. */
    public static List<String> signSearchNames(String query) {
        if (!active() || !RussianSearchAliases.russianQuery(query)) return List.of();
        try {
            var names = signNames;
            if (names == null) signNames = names = SignSearchNames.load(dictionary);
            return names.suggestions(query, 5);
        } catch (Exception unsupported) { return List.of(); }
    }

    /** RRV receives a temporary search component, never an edited item component. */
    public static Component searchName(Component source, String query) {
        if (source == null || !RussianSearchAliases.russianQuery(query) || !active()) return source;
        try {
            var aliases = searchAliases;
            if (aliases == null) searchAliases = aliases = new RussianSearchAliases(dictionary::translateForSearch);
            String original = source.getString();
            String searchable = aliases.name(original, query);
            return searchable.equals(original) ? source : Component.literal(searchable);
        } catch (RuntimeException ignored) { return source; }
    }

    /** RRV's side panel uses getListMatch/getMatch, not only defaultFilter. */
    public static int russianSearchMatch(Component source, String query) {
        if (source == null || !RussianSearchAliases.russianQuery(query) || !active()) return 0;
        String searchable = searchName(source, query).getString().toLowerCase(java.util.Locale.ROOT);
        String input = query.toLowerCase(java.util.Locale.ROOT);
        if (searchable.startsWith(input)) return 1;
        if (searchable.contains(input)) return 2;
        if (input.contains(" ") && java.util.Arrays.stream(input.split(" ")).allMatch(searchable::contains)) return 2;
        return 0;
    }

    public static boolean hudTextFitActive() {
        return fitHudText && hudDisplayActive();
    }

    /** Recognition/translation must not depend on the optional font-fit setting. */
    public static boolean hudDisplayActive() {
        if (!active()) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.level != null && (mc.gui.screen() == null || mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen);
    }

    public static boolean tooltipTextFitActive() { return fitTooltipText && active(); }
    public static boolean tooltipWrap() { return fitTooltipText; }
    static void setTooltipWrap(boolean wrap) { fitTooltipText = wrap; }

    public static List<EditorialContext.Edit> tooltipContextEdits(List<String> source) {
        return !active() || UserInputText.rendering() ? List.of() : dictionary.contextEdits(source);
    }
    static boolean tooltipPowerAllowed(List<String> source) {
        return active() && !UserInputText.rendering() && !dictionary.hasCustomOverride(source);
    }

    // The optional overlay reuses the existing NPC engine on an unsigned display copy.
    static Component overlayChat(Component original) {
        if(!overlayDialogueActive())return original;
        return npcChat(new net.minecraft.client.multiplayer.chat.GuiMessage(0,original,null,
            net.minecraft.client.multiplayer.chat.GuiMessageSource.SYSTEM_SERVER,null)).content();
    }
    static boolean overlayDialogueActive(){return active()&&translateNpcChat&&!UserInputText.rendering()&&!TechnicalText.active();}
    static Component overlayComponent(Component original){
        return overlayDialogueActive()?component(original,original.getString()):original;
    }
    public static net.minecraft.client.multiplayer.chat.GuiMessage npcChat(net.minecraft.client.multiplayer.chat.GuiMessage message) {
        if (message == null || !active()
            || message.source() == net.minecraft.client.multiplayer.chat.GuiMessageSource.PLAYER
            || message.signature() != null) return message;
        try {
            Component translated = message.content();
            // Closed providers inspect the same original message until one wins.
            // Share only this receipt's text parse, never a message/style cache.
            var input=message.source()==net.minecraft.client.multiplayer.chat.GuiMessageSource.SYSTEM_SERVER
                &&(translateNpcChat||translateGameChat)?new NpcDialogueTranslator.Input(translated):null;
            // Closed winter speakers only, on display-queue insertion/refresh, never per frame.
            // Cheap literal rejection before the existing exact dialogue/legacy parser.
            if(translateNpcChat&&winterDialogues!=null
                &&message.source()==net.minecraft.client.multiplayer.chat.GuiMessageSource.SYSTEM_SERVER){
                // The exact raw-name filter shares the receipt-local snapshot.
                String text=input.raw;
                if(text.contains("Frosty")||text.contains("St. Jerry"))translated=winterDialogues.translate(translated,dictionary,input);
            }
            if (translateNpcChat && translated==message.content() && npcDialogues != null
                && message.source() == net.minecraft.client.multiplayer.chat.GuiMessageSource.SYSTEM_SERVER)
                translated = npcDialogues.translate(translated,dictionary,input);
            if(translateNpcChat && translated==message.content() && dungeonDialogues!=null
                &&message.source()==net.minecraft.client.multiplayer.chat.GuiMessageSource.SYSTEM_SERVER)
                translated=dungeonDialogues.translate(translated,dictionary,input);
            if (translateNpcChat && translated == message.content() && chatNotifications != null)
                translated = chatNotifications.translateNpcRequest(translated, message.source(), false);
            if (translateNpcChat && translated == message.content() && gardenDialogues != null
                && message.source() == net.minecraft.client.multiplayer.chat.GuiMessageSource.SYSTEM_SERVER)
                translated = gardenDialogues.translate(translated,dictionary,input);
            if (translateNpcChat && translated == message.content() && questDialogues != null
                && message.source() == net.minecraft.client.multiplayer.chat.GuiMessageSource.SYSTEM_SERVER)
                translated = questDialogues.translate(translated,dictionary,input);
            if (translated == message.content() && translateGameChat && chatNotifications != null)
                translated = chatNotifications.translate(translated, message.source(), false);
            if (translateGameChat && translated == message.content() && questObjectives != null
                && message.source() == net.minecraft.client.multiplayer.chat.GuiMessageSource.SYSTEM_SERVER)
                translated = questObjectives.translate(translated,dictionary,input);
            if (translateNpcChat && translated == message.content() && storyDialogues != null
                && message.source() == net.minecraft.client.multiplayer.chat.GuiMessageSource.SYSTEM_SERVER)
                translated = storyDialogues.translate(translated,dictionary,input);
            if(translateGameChat &&translated==message.content()&&dungeonNotifications!=null
                &&message.source()==net.minecraft.client.multiplayer.chat.GuiMessageSource.SYSTEM_SERVER)
                translated=dungeonNotifications.translate(translated,dictionary,input);
            if (translated == message.content()) return message;
            return new net.minecraft.client.multiplayer.chat.GuiMessage(
                message.addedTime(), translated, message.signature(), message.source(), message.tag());
        } catch (Throwable ignored) { return message; }
    }

    public static Component component(Component source) {
        // World labels use the same safe renderer through minionNameTag(), never entity mutation.
        if(source==null || TechnicalText.active() || !active())return source;
        String visible=source.getString();
        if(DisplayNoop.text(visible)||UserInputText.rendering())return source;
        Component translated=component(source,visible);
        return translated;
    }
    private static Component component(Component source,String visible){
        if(CollectionContributors.protectedRow(visible) || PlayerNames.contains(visible) || OriginalItemTitle.hoveredName(visible)){return source;}
        // Closed item-title and authored requirement spans can join fragmented literals.
        // All other text, including non-literal translation keys, uses the original tree policy.
        try {
            Component title = StyledItemTitle.translate(source, dictionary, visible);
            return title != null ? title : translateTree(source, 0);
        }
        catch (Throwable e) { return source; }
    }

    /** One caller snapshot for the high-level GUI hook and its translation checks. */
    public static Component componentOutsideHud(Component source) {
        if(source==null||TechnicalText.active()||!active())return source;
        String visible=source.getString();if(DisplayNoop.text(visible))return source;
        try(var context=RenderContext.open()){
            Component translated=HudTextFit.currentPanel()||UserInputText.rendering()?source:component(source,visible);
            return translated;
        }
    }
    public static String textOutsideHud(String source) {
        if(source==null||TechnicalText.active()||!active())return source;
        if(DisplayNoop.text(source))return source;
        try(var context=RenderContext.open()){return HudTextFit.currentPanel()?source:text(source);}
    }
    public static FormattedCharSequence sequenceOutsideHud(FormattedCharSequence source) {
        if(source==null||TechnicalText.active()||!active())return source;
        if(numericSequence(source))return source;
        try(var context=RenderContext.open()){return HudTextFit.currentPanel()?source:sequence(source);}
    }

    /** Exactly the existing numeric no-op policy, before a caller-stack inspection. */
    public static boolean numericSequence(FormattedCharSequence source){
        try{return source!=null&&DisplayNoop.numbers(source);}catch(Throwable unsupported){return false;}
    }

    /** Render-state copy only; all player entities and unknown world labels remain original. */
    public static Component minionNameTag(Component source, boolean playerEntity) {
        if(playerEntity||source==null||!active())return source;
        String visible=LegacyFormatting.strip(source.getString());
        if(visible.equals("CLICK"))return NpcInteractionLabel.translate(source,dictionary);
        if(!NPC_WORLD_ROLES.contains(visible)&&!MinionNameTranslation.supports(visible))return source;
        return component(source);
    }

    private static Component translateTree(Component original, int depth) {
        if (depth>18) return original;
        List<Component> siblings = original.getSiblings();
        String base = original.getContents() instanceof PlainTextContents plain ? plain.text() : null;
        String translated = base == null ? null : dictionary.translate(base);
        boolean edited = base != null && !base.equals(translated);
        List<Component> children = null;
        for (int i=0;i<siblings.size();i++) {
            Component sibling=siblings.get(i);
            Component next=translateTree(sibling,depth+1);
            if (next!=sibling) {
                edited=true;
                if(children==null){children=new ArrayList<>(siblings.size());for(int j=0;j<i;j++)children.add(siblings.get(j));}
            }
            if(children!=null)children.add(next);
        }
        if (!edited) return original;
        // Non-literal contents (e.g. vanilla translation keys) are preserved as-is.
        MutableComponent rebuilt=base == null ? original.plainCopy() : Component.literal(translated).setStyle(original.getStyle());
        for(Component sibling:children==null?siblings:children) rebuilt.append(sibling);
        return rebuilt;
    }

    /** HUD/scoreboards often reach Minecraft already split into styled glyph runs. */
    public static FormattedCharSequence narrowHud(FormattedCharSequence original,FormattedCharSequence translated){
        if(!active()||UserInputText.rendering())return translated;
        // The override set is immutable. With none, no source traversal/key parsing can affect the decision.
        if(!dictionary.hasCustomOverrides())return HudTextFit.narrow(translated);
        var text=new StringBuilder();original.accept((i,s,p)->{text.appendCodePoint(p);return true;});
        var keys=new ArrayList<String>(ScreenLabels.keys(text.toString().strip()));keys.addAll(InterfaceDisplay.keys(text.toString().strip()));
        if(dictionary.hasCustomOverride(keys))return translated;
        return HudTextFit.narrow(translated);
    }
    public static FormattedCharSequence sequence(FormattedCharSequence source) {
        return sequence(source, false);
    }

    public static FormattedCharSequence sequence(FormattedCharSequence source, boolean hudPanel) {
        if (source == null || TechnicalText.active() || OriginalItemTitle.protectedSequence(source) || !active() || UserInputText.rendering()) return source;
        try {
            var current=StyledRuns.collect(source);String visible=current.text;
            if (DisplayNoop.text(visible) || CollectionContributors.protectedRow(visible) || PlayerNames.contains(visible) || OriginalItemTitle.hoveredName(visible)){return source;}
            var db=dictionary;boolean compact=hudPanel&&compactHudLabels;
            var prepared=PreparedDisplay.find(source,current,db,compact);if(prepared!=null){
                return prepared;
            }
            var title = StyledItemTitle.translate(source, db, current);
            if (title != null){PreparedDisplay.remember(source,title,current,db,compact);
                return title;
            }
            List<FormattedCharSequence> result = null;
            for(var run:current.runs){
                String old=visible.substring(run.start(),run.end());
                String next = PlayerNames.contains(old) ? old : db.translate(old);
                if (compact) next = HudTextFit.compact(next);
                if(!old.equals(next)&&result==null){result=new ArrayList<>();current.appendOriginal(result,0,run.start());}
                if(result!=null)result.add(FormattedCharSequence.forward(next,run.style()));
            }
            var translated=result!=null?FormattedCharSequence.composite(result):source;
            PreparedDisplay.remember(source,translated,current,db,compact);
            return translated;
        } catch (Throwable ignored) { return source; }
    }
}
