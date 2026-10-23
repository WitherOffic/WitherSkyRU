package ru.wither.skyru;

import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.util.StringDecomposer;

/** Exact, source-backed dialogue translations. No player chat, guesses or I/O at render time. */
public final class NpcDialogueTranslator {
    private static final Pattern LEGACY_COLORS = Pattern.compile("(?i)§[0-9a-fk-or]");
    private record Anchor(String source, String target, boolean wholeWord,int sourceOccurrence,int targetOccurrence,int sourceOffset,int targetOffset) {}
    private record Entry(String target, int prefixLength, List<Anchor> anchors, int suffixLength,List<Anchor> alternateAnchors,List<String> overrideKeys,String targetBody) {}
    private record StyleCorrection(String target,List<Anchor> anchors) {}
    private record Resolved(String source,String key,String baseKey,Entry entry) {}
    private record Span(int start, int end, String text, Style style) {}
    private record Region(int start, int end, Style style,int copyStart,int copyEnd) {}
    private final Map<String, Entry> entries;
    private final boolean allowCounters;
    private final boolean allowObjectivePadding;
    private static final Pattern COUNTER=Pattern.compile(" (\\((?:x|×)[0-9]{1,6}\\))$");

    /** One receipt-local text snapshot shared by closed providers. No styles/actions retained. */
    static final class Input {
        final String raw,source;final int timestamp;
        private int counterStart=-2;
        Input(Component original){
            raw=original.getString();source=raw.indexOf('§')>=0?LEGACY_COLORS.matcher(raw).replaceAll(""):raw;
            timestamp=timestampLength(source);
        }
        int counterStart(){
            if(counterStart==-2){var match=COUNTER.matcher(source);counterStart=match.find()?match.start():-1;}
            return counterStart;
        }
    }

    public NpcDialogueTranslator() throws IOException {
        this("/data/witherskyru/npc-dialogues.json",java.util.Set.of("NPC"));
    }
    public static NpcDialogueTranslator gardenDialogues() throws IOException {
        return new NpcDialogueTranslator("/data/witherskyru/garden-dialogues.json",java.util.Set.of("NPC"),true);
    }
    public static NpcDialogueTranslator questDialogues() throws IOException {
        return new NpcDialogueTranslator("/data/witherskyru/quest-dialogues.json",java.util.Set.of("NPC"),true);
    }
    public static NpcDialogueTranslator storyDialogues() throws IOException {
        // Merge authored additions once at startup; no new lookup/render pass per chat message.
        return new NpcDialogueTranslator(new String[]{"/data/witherskyru/story-dialogues.json",
            "/data/witherskyru/hub-dialogues.json","/data/witherskyru/npc-continuations70.json",
            "/data/witherskyru/npc-expansion74.json","/data/witherskyru/npc-expansion75.json",
            "/data/witherskyru/npc-expansion76.json","/data/witherskyru/npc-expansion77.json",
            "/data/witherskyru/npc-expansion78.json","/data/witherskyru/npc-expansion79.json","/data/witherskyru/npc-expansion80.json","/data/witherskyru/npc-expansion81.json","/data/witherskyru/npc-expansion82.json","/data/witherskyru/npc-expansion83.json","/data/witherskyru/npc-expansion84.json","/data/witherskyru/npc-expansion85.json","/data/witherskyru/npc-expansion86.json","/data/witherskyru/npc-expansion87.json","/data/witherskyru/npc-expansion88.json","/data/witherskyru/npc-raw-repairs88.json",
            "/data/witherskyru/npc-expansion89.json","/data/witherskyru/npc-extras89.json","/data/witherskyru/npc-expansion90.json","/data/witherskyru/npc-expansion91.json","/data/witherskyru/npc-expansion92.json","/data/witherskyru/npc-expansion93.json","/data/witherskyru/npc-expansion99.json","/data/witherskyru/npc-expansion100.json","/data/witherskyru/npc-expansion101.json","/data/witherskyru/npc-expansion102.json","/data/witherskyru/npc-current102.json","/data/witherskyru/npc-expansion106.json","/data/witherskyru/npc-expansion111.json","/data/witherskyru/npc-expansion112.json","/data/witherskyru/npc-literals113.json","/data/witherskyru/npc-recovery115.json","/data/witherskyru/npc-reviewed117.json"},java.util.Set.of("NPC","SECURITY"),true,false);
    }
    public static NpcDialogueTranslator winterDialogues() throws IOException {
        return new NpcDialogueTranslator("/data/witherskyru/winter-dialogues.json",java.util.Set.of("NPC"),true);
    }
    public static NpcDialogueTranslator questObjectives() throws IOException {
        return new NpcDialogueTranslator(new String[]{"/data/witherskyru/quest-objectives.json","/data/witherskyru/system-expansion107.json","/data/witherskyru/skins-expansion109.json","/data/witherskyru/game-expansion110.json","/data/witherskyru/system-expansion111.json","/data/witherskyru/system-expansion112.json","/data/witherskyru/eggs-expansion114.json"},java.util.Set.of("SYSTEM"),true,true);
    }
    /** Separate closed resources: NPC toggle and game-notification toggle remain independent. */
    public static NpcDialogueTranslator dungeonDialogues() throws IOException {
        // Add authored boss literals to the existing provider once at startup.
        return new NpcDialogueTranslator(new String[]{"/data/witherskyru/dungeon-dialogues.json",
            "/data/witherskyru/dungeon-expansion98.json","/data/witherskyru/boss-expansion108.json"},java.util.Set.of("NPC","STATUE","BOSS"),false,false);
    }
    public static NpcDialogueTranslator dungeonNotifications() throws IOException {
        return new NpcDialogueTranslator("/data/witherskyru/dungeon-notifications.json",java.util.Set.of("SYSTEM"));
    }
    private NpcDialogueTranslator(String resource,java.util.Set<String> tags) throws IOException {
        this(resource,tags,false);
    }
    private NpcDialogueTranslator(String resource,java.util.Set<String> tags,boolean allowCounters) throws IOException {
        this(resource,tags,allowCounters,false);
    }
    private NpcDialogueTranslator(String resource,java.util.Set<String> tags,boolean allowCounters,boolean allowObjectivePadding) throws IOException {
        this(new String[]{resource},tags,allowCounters,allowObjectivePadding);
    }
    private NpcDialogueTranslator(String[] resources,java.util.Set<String> tags,boolean allowCounters,boolean allowObjectivePadding) throws IOException {
        this.allowCounters=allowCounters;
        this.allowObjectivePadding=allowObjectivePadding;
        Map<String, Entry> loaded = new HashMap<>();
        for(String resource:resources){
        var stream = NpcDialogueTranslator.class.getResourceAsStream(resource);
        if (stream == null) throw new IOException("Missing NPC dialogue resource");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            for (var row : root.getAsJsonArray("records")) {
                var record = row.getAsJsonObject();
                String source = record.get("source").getAsString();
                String target = record.get("translation").getAsString();
                String tag=record.has("tag")?record.get("tag").getAsString():"NPC";
                String speaker=record.get("speaker").getAsString();
                if(!tags.contains(tag)||tag.equals("SYSTEM")&&!speaker.isEmpty())
                    throw new IOException("Invalid closed dialogue tag");
                String prefix = tag.equals("SYSTEM")?"":"["+tag+"] " + speaker + ": ";
                if (!source.startsWith(prefix) || !target.startsWith(prefix))
                    throw new IOException("NPC name/prefix must be retained");
                if (source.startsWith("✆ ", prefix.length()) && target.startsWith("✆ ", prefix.length()))
                    prefix += "✆ ";
                if (source.length() > 4096 || target.length() > 4096 || source.equals(target))
                    throw new IOException("Invalid NPC translation");
                String suffix = record.has("preserve_suffix") ? record.get("preserve_suffix").getAsString() : "";
                // Only explicitly recorded counters; never infer a new message or counter value.
                if (!suffix.isEmpty() && (!suffix.matches(" \\(x[0-9]{1,8}\\)")
                    || !source.endsWith(suffix) || !target.endsWith(suffix)
                    || source.length() - suffix.length() <= prefix.length()
                    || target.length() - suffix.length() <= prefix.length()))
                    throw new IOException("Invalid retained NPC counter");
                List<Anchor> anchors = new ArrayList<>();
                if (record.has("style_anchors")) {
                    for (var raw : record.getAsJsonArray("style_anchors")) {
                        var anchor = raw.getAsJsonObject();
                        String original = anchor.get("source").getAsString();
                        String translated = anchor.get("target").getAsString();
                        boolean wholeWord=anchor.has("whole_word")&&anchor.get("whole_word").getAsBoolean();
                        int sourceOccurrence=occurrence(anchor,"source_occurrence"),targetOccurrence=occurrence(anchor,"target_occurrence");
                        int from=anchorPosition(source,original,prefix.length(),wholeWord,sourceOccurrence,source.length()-suffix.length());
                        int to=anchorPosition(target,translated,prefix.length(),wholeWord,targetOccurrence,target.length()-suffix.length());
                        if (original.isEmpty() || translated.isEmpty()
                            || from < 0 || to < 0)
                            throw new IOException("Ambiguous NPC style anchor");
                        anchors.add(new Anchor(original,translated,wholeWord,sourceOccurrence,targetOccurrence,from-prefix.length(),to-prefix.length()));
                    }
                }
                validateAnchorRanges(source,prefix.length(),source.length()-suffix.length(),anchors,true);
                validateAnchorRanges(target,prefix.length(),target.length()-suffix.length(),anchors,false);
                // Closed corrections are read once at startup, never during chat/render.
                // Keep the original plan: old layouts and semantic overrides must work.
                var correction=StyleCorrections.ENTRIES.get(source);
                List<Anchor> alternate=correction==null?List.of():correction.anchors();
                if(correction!=null){
                    if(!correction.target().equals(target))throw new IOException("Style correction changed a dialogue target");
                    validateAnchorRanges(source,prefix.length(),source.length()-suffix.length(),alternate,true);
                    validateAnchorRanges(target,prefix.length(),target.length()-suffix.length(),alternate,false);
                }
                // Immutable source/semantic keys, not a cached override verdict or
                // a cached styled message. No per-message list/iterator building.
                var overrideKeys=new java.util.LinkedHashSet<String>();overrideKeys.add(source);
                for(var anchor:anchors)overrideKeys.add(anchor.source());
                for(var anchor:alternate)overrideKeys.add(anchor.source());
                String targetBody=target.substring(prefix.length(),target.length()-suffix.length());
                if (loaded.putIfAbsent(source, new Entry(target, prefix.length(), List.copyOf(anchors), suffix.length(),alternate,List.copyOf(overrideKeys),targetBody)) != null)
                    throw new IOException("Duplicate NPC source");
            }
        } catch (RuntimeException e) {
            throw new IOException("Invalid NPC dialogue JSON", e);
        }
        }
        entries = Map.copyOf(loaded);
    }

    public int phraseCount() { return entries.size(); }

    private static final class StyleCorrections {
        private static final Map<String,StyleCorrection> ENTRIES=load();
        private static Map<String,StyleCorrection> load(){
            try{var result=new HashMap<String,StyleCorrection>();for(String path:new String[]{"/data/witherskyru/npc-styles78.json","/data/witherskyru/npc-styles83.json","/data/witherskyru/npc-styles104.json"})try(var stream=NpcDialogueTranslator.class.getResourceAsStream(path)){
                if(stream==null)throw new IOException("Missing reviewed NPC style corrections");
                // Merge closed tables once; no resource iteration during chat/render.
                try(var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
                    for(var raw:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("records")){
                        var record=raw.getAsJsonObject();String source=record.get("source").getAsString(),target=record.get("translation").getAsString();
                        int prefix=source.indexOf(": ")+2;
                        if(!source.startsWith("[NPC] ")||prefix<8||!target.startsWith(source.substring(0,prefix))
                            ||source.length()>4096||target.length()>4096)throw new IOException("Invalid closed style correction");
                        if(source.startsWith("✆ ",prefix)&&target.startsWith("✆ ",prefix))prefix+=2;
                        var anchors=new ArrayList<Anchor>();
                        for(var item:record.getAsJsonArray("style_anchors")){
                            var a=item.getAsJsonObject();String from=a.get("source").getAsString(),to=a.get("target").getAsString();
                            boolean whole=a.has("whole_word")&&a.get("whole_word").getAsBoolean();
                            int x=occurrence(a,"source_occurrence"),y=occurrence(a,"target_occurrence");
                            int sourceAt=anchorPosition(source,from,prefix,whole,x,source.length());
                            int targetAt=anchorPosition(target,to,prefix,whole,y,target.length());
                            if(from.isEmpty()||to.isEmpty()||sourceAt<0||targetAt<0)throw new IOException("Invalid reviewed style anchor");
                            anchors.add(new Anchor(from,to,whole,x,y,sourceAt-prefix,targetAt-prefix));
                        }
                        validateAnchorRanges(source,prefix,source.length(),anchors,true);
                        validateAnchorRanges(target,prefix,target.length(),anchors,false);
                        if(result.putIfAbsent(source,new StyleCorrection(target,List.copyOf(anchors)))!=null)
                            throw new IOException("Duplicate reviewed style source");
                    }
                }
                }return Map.copyOf(result);
            }catch(IOException e){throw new java.io.UncheckedIOException(e);}
        }
    }

    /** Closed Dungeon resources yield to both full-message and semantic-term overrides. */
    public Component translate(Component original, OfflineDictionary dictionary) {
        if (original == null) return null;
        return translate(original,dictionary,new Input(original));
    }
    Component translate(Component original,OfflineDictionary dictionary,Input input){
        if(original==null)return null;
        var resolved=resolve(input);if(resolved==null)return original;
        if(dictionary.hasCustomOverride(resolved.entry().overrideKeys()))return original;
        // Only a live repeat suffix introduces keys which cannot be prepared at
        // startup. Full-counter and stripped-base user overrides still win.
        if(resolved.baseKey()!=null&&dictionary.hasCustomOverride(List.of(resolved.key(),resolved.baseKey())))return original;
        return render(original,resolved.source(),resolved.entry());
    }

    /** Unknown messages, non-literal trees and unsupported formatting remain the original object. */
    public Component translate(Component original) {
        if (original == null) return null;
        var resolved=resolve(new Input(original));return resolved==null?original:render(original,resolved.source(),resolved.entry());
    }
    /** One exact lookup/legacy parse per provider call. Never retains current messages. */
    private Resolved resolve(Input input){
        String source=input.source;
        int timeLength=input.timestamp;
        if(allowObjectivePadding)while(timeLength<source.length()&&source.charAt(timeLength)==' ')timeLength++;
        String key=timeLength==0?source:source.substring(timeLength),baseKey=null;
        Entry entry = entries.get(key);
        if(allowCounters){
            int counter=input.counterStart();
            if(counter>=0){
                baseKey=source.substring(timeLength,counter);Entry base=entry==null?entries.get(baseKey):null;
                if(base!=null){String suffix=source.substring(counter);entry=new Entry(base.target()+suffix,base.prefixLength(),base.anchors(),suffix.length(),base.alternateAnchors(),base.overrideKeys(),base.targetBody());}
            }
        }
        if (entry != null && timeLength > 0)
            entry = new Entry(source.substring(0, timeLength) + entry.target(), entry.prefixLength() + timeLength, entry.anchors(), entry.suffixLength(),entry.alternateAnchors(),entry.overrideKeys(),entry.targetBody());
        return entry==null?null:new Resolved(source,key,baseKey,entry);
    }
    private static int timestampLength(String s){
        if(s.length()<11||s.charAt(0)!='['||s.charAt(3)!=':'||s.charAt(6)!=':'||s.charAt(9)!=']'||s.charAt(10)!=' ')return 0;
        for(int i=1;i<9;i++){if(i==3||i==6)continue;char c=s.charAt(i);if(c<'0'||c>'9')return 0;}return 11;
    }
    private Component render(Component original,String source,Entry entry){
        if (!literalTree(original, 0, new int[] {0})) return original;

        List<Span> spans = new ArrayList<>();
        int[] offset = {0};
        StringBuilder run = new StringBuilder(), visible = new StringBuilder();
        Style[] currentStyle = {null};
        Runnable flush = () -> {
            if (!run.isEmpty()) {
                spans.add(new Span(offset[0] - run.length(), offset[0], run.toString(), currentStyle[0]));
                run.setLength(0);
            }
        };
        // The game's own parser resolves § formatting and inherited Component styles.
        // Logical text order is retained; we do not use a visual/bidi string as a lookup key.
        StringDecomposer.iterateFormatted(original, Style.EMPTY, (index, style, codePoint) -> {
            if (!java.util.Objects.equals(currentStyle[0], style)) flush.run();
            currentStyle[0] = style;
            run.appendCodePoint(codePoint);
            visible.appendCodePoint(codePoint);
            offset[0] += Character.charCount(codePoint);
            return true;
        });
        flush.run();
        if (!visible.toString().equals(source)) return original;
        // Every body character has exactly the same current Style, including
        // actions/font/shadow. Semantic redistribution is unnecessary here.
        // Only the immutable authored text is reused; styles/counters are fresh.
        int bodyEnd=source.length()-entry.suffixLength();
        Style uniform=uniformStyle(spans,entry.prefixLength(),bodyEnd);
        if(uniform!=null){
            var fast=Component.empty();appendSource(fast,spans,0,entry.prefixLength());
            append(fast,entry.targetBody(),uniform);appendSource(fast,spans,bodyEnd,source.length());
            return fast;
        }
        var result=renderPlan(original,source,entry,spans,entry.anchors());
        if(result!=original||entry.alternateAnchors().isEmpty())return result;
        // A finite published alternate layout, not a heuristic style guess.
        // Current runs/actions are collected only once and are never cached.
        return renderPlan(original,source,entry,spans,entry.alternateAnchors());
    }
    private Component renderPlan(Component original,String source,Entry entry,List<Span> spans,List<Anchor> anchors){
        int sourceBodyEnd = source.length() - entry.suffixLength();
        int targetBodyEnd = entry.target().length() - entry.suffixLength();
        List<Region> sourceAnchors = new ArrayList<>();
        List<Region> targetAnchors = new ArrayList<>();
        for (Anchor anchor : anchors) {
            // Authored locations are immutable; only live timestamp/padding moves.
            // Current styles/actions still come from this message's fresh spans.
            int from = entry.prefixLength()+anchor.sourceOffset();
            int to = entry.prefixLength()+anchor.targetOffset();
            boolean copied=anchor.source().equals(anchor.target());
            Style style = uniformStyle(spans, from, from + anchor.source().length());
            if (style == null&&!copied) return original;
            sourceAnchors.add(new Region(from, from + anchor.source().length(), style,copied?from:-1,copied?from+anchor.source().length():-1));
            targetAnchors.add(new Region(to, to + anchor.target().length(), style,copied?from:-1,copied?from+anchor.source().length():-1));
        }
        // A highlighted first word (e.g. "Chips") is not the base prose style.
        Style bodyStyle = null;
        for (Span span : spans) {
            for (int position = Math.max(span.start(), entry.prefixLength()); position < Math.min(span.end(), sourceBodyEnd); position++) {
                if (Character.isWhitespace(source.charAt(position))) continue;
                boolean anchored=false;
                for(Region region:sourceAnchors)if(position>=region.start()&&position<region.end()){anchored=true;break;}
                if (!anchored) {
                    bodyStyle = span.style(); break;
                }
            }
            if (bodyStyle != null) break;
        }
        if (bodyStyle == null) bodyStyle = styleAt(spans, entry.prefixLength());
        if (bodyStyle == null) return original;
        targetAnchors.sort(java.util.Comparator.comparingInt(Region::start));
        int previousEnd = entry.prefixLength();
        for (Region region : targetAnchors) {
            if (region.start() < previousEnd) return original;
            previousEnd = region.end();
        }
        // Never arbitrarily assign a click/hover event or coloured word to a different meaning.
        // Only a uniform body and explicitly authored semantic anchors are supported initially.
        Style comparableBody=null;
        for (Span span : spans) {
            int start = Math.max(entry.prefixLength(), span.start());
            int end = Math.min(span.end(), sourceBodyEnd);
            // Formatting often includes a separator after a highlighted term.
            // Whitespace has no translated semantic meaning; do not reject its anchor.
            while (start < end && Character.isWhitespace(source.charAt(start))) start++;
            while (end > start && Character.isWhitespace(source.charAt(end - 1))) end--;
            if (start >= end || span.style()==bodyStyle || span.style().equals(bodyStyle)) continue;
            // The game's parser merges adjacent equal-style pieces. A merged span
            // may cover several explicit anchors (e.g. [, CLICK, ]). Every visible
            // character must still have an authored semantic mapping with that
            // exact Style; never accept unrelated highlighted prose or actions.
            boolean fullyAnchored=true;
            for(int position=start;position<end;position++){
                if(Character.isWhitespace(source.charAt(position)))continue;
                boolean anchored=false;
                for(Region region:sourceAnchors){
                    if(position>=region.start()&&position<region.end()&&(region.copyStart()>=0||span.style().equals(region.style()))){anchored=true;break;}
                }
                if(!anchored){fullyAnchored=false;break;}
            }
            // Anchored highlights already carry exact current styles. Normalize
            // implicit/explicit legacy flags only for unanchored base prose,
            // once for the body instead of rebuilding it for every span.
            if(fullyAnchored)continue;
            if(comparableBody==null)comparableBody=bodyAppearance(bodyStyle);
            if(!bodyAppearance(span.style()).equals(comparableBody))return original;
        }

        MutableComponent result = Component.empty();
        appendSource(result, spans, 0, entry.prefixLength());
        int cursor = entry.prefixLength();
        for (Region region : targetAnchors) {
            append(result, entry.target().substring(cursor, region.start()), bodyStyle);
            if(region.copyStart()>=0)appendSource(result,spans,region.copyStart(),region.copyEnd());
            else append(result, entry.target().substring(region.start(), region.end()), region.style());
            cursor = region.end();
        }
        append(result, entry.target().substring(cursor, targetBodyEnd), bodyStyle);
        appendSource(result, spans, sourceBodyEnd, source.length());
        return result;
    }

    private static boolean literalTree(Component component, int depth, int[] count) {
        if (depth > 18 || ++count[0] > 128 || !(component.getContents() instanceof PlainTextContents)) return false;
        for (Component child : component.getSiblings())
            if (!literalTree(child, depth + 1, count)) return false;
        return true;
    }

    private static void validateAnchorRanges(String text,int start,int bodyEnd,List<Anchor> anchors,boolean source)throws IOException{
        var ranges=new ArrayList<int[]>();for(var anchor:anchors){String word=source?anchor.source():anchor.target();int at=anchorPosition(text,word,start,anchor.wholeWord(),source?anchor.sourceOccurrence():anchor.targetOccurrence(),bodyEnd);ranges.add(new int[]{at,at+word.length()});}ranges.sort(java.util.Comparator.comparingInt(a->a[0]));int end=start;for(var range:ranges){if(range[0]<end)throw new IOException("Overlapping NPC style anchors");end=range[1];}
    }
    private static int occurrence(com.google.gson.JsonObject anchor,String key)throws IOException{
        if(!anchor.has(key))return 0;var value=anchor.get(key);if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IOException("Invalid anchor occurrence");int n=value.getAsInt();if(n<1||n>16||!value.getAsString().equals(Integer.toString(n)))throw new IOException("Invalid anchor occurrence");return n;
    }
    private static int anchorPosition(String text,String fragment,int start,boolean wholeWord,int occurrence,int bodyEnd){
        int result=-1,seen=0;
        for(int at=text.indexOf(fragment,start);at>=0;at=text.indexOf(fragment,at+1)){
            int end=at+fragment.length();
            if(end>bodyEnd)break;
            if(wholeWord&&((at>0&&wordCharacter(text.charAt(at-1)))
                ||(end<text.length()&&wordCharacter(text.charAt(end)))))continue;
            if(occurrence>0){if(++seen==occurrence)return at;continue;}
            if(result>=0)return -1;
            result=at;
        }
        return occurrence>0?-1:result;
    }
    private static boolean wordCharacter(char c){return Character.isLetterOrDigit(c)||c=='_';}

    private static Style styleAt(List<Span> spans, int position) {
        for (Span span : spans) if (span.start() <= position && span.end() > position) return span.style();
        return null;
    }

    /** Legacy colour resets encode false flags explicitly; implicit false is identical.
     * Keep fonts, shadow and click/hover events strict, never move different actions. */
    private static Style bodyAppearance(Style style) {
        return style.withColor(style.getColor()==null?0xffffff:style.getColor().getValue())
            .withBold(style.isBold()).withItalic(style.isItalic()).withUnderlined(style.isUnderlined())
            .withStrikethrough(style.isStrikethrough()).withObfuscated(style.isObfuscated());
    }

    private static Style uniformStyle(List<Span> spans, int start, int end) {
        Style result = null;
        int covered = 0;
        for (Span span : spans) {
            int overlap = Math.min(end, span.end()) - Math.max(start, span.start());
            if (overlap <= 0) continue;
            if (result != null && !result.equals(span.style())) return null;
            result = span.style();
            covered += overlap;
        }
        return covered == end - start ? result : null;
    }

    private static void appendSource(MutableComponent result, List<Span> spans, int start, int end) {
        for (Span span : spans) {
            int from = Math.max(start, span.start()), to = Math.min(end, span.end());
            if (from < to) append(result, span.text().substring(from - span.start(), to - span.start()), span.style());
        }
    }

    private static void append(MutableComponent result, String text, Style style) {
        if (!text.isEmpty()) result.append(Component.literal(text).setStyle(style));
    }
}
