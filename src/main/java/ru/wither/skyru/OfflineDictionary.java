package ru.wither.skyru;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic, network-free dictionary. Unknown strings are never guessed. */
public final class OfflineDictionary {
    private static final Pattern TRIM = Pattern.compile("(?s)^(\\s*)(.*?)(\\s*)$");
    private static final Pattern SAFE = Pattern.compile("^[\\d\\s.,+%():/\\-×*±❤✦☆★✧☘⚔❈☠✿♣♦⛏]+$");
    private static final Pattern URL = Pattern.compile("(?i)^(?:https?://\\S+|/\\S+|[0-9a-f]{8}-[0-9a-f-]{27,})$");
    private static final Pattern COLORS = Pattern.compile("(?i)§[0-9a-fk-or]");
    private static final Pattern COLON = Pattern.compile("^([^:\\n]{2,60}): (.+)$");
    private static final Pattern CAPTURE_REFERENCE = Pattern.compile("\\$(?:(\\d+)|\\{(gen|acc):(\\d+)\\})");
    private static final Pattern DECORATED = Pattern.compile("^([^A-Za-zА-Яа-яЁё]*)([A-Za-z][A-Za-z '’-]*)([^A-Za-zА-Яа-яЁё]*)$");
    private static final Pattern ENCHANTMENT_LEVEL = Pattern.compile("^(.+?)( (?:[IVXLCDM]+|[0-9]+)(?:→(?:[IVXLCDM]+|[0-9]+))?)?$");
    private static final Set<String> RARITY_TOKENS = Set.of("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "DIVINE", "SPECIAL", "VERY SPECIAL", "SUPREME", "ULTIMATE");

    private final Map<String, String> exact;
    private final Set<String> customOverrides;
    private final Map<String, String> folded;
    private final List<Rule> rules;
    private final Map<String,String[]> grammar;
    private final Map<String,String[]> reforgeForms;
    private final List<String> reforgePrefixes;
    private final Set<String> suffixReforges;
    private final Map<String,String[]> itemGenders;
    private final Set<String> enchantmentNames;
    private final Map<String,String> cache = Collections.synchronizedMap(new java.util.LinkedHashMap<>(4096, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String,String> entry) { return size() > 4096; }
    });
    private final Map<String,List<TitleEdit>> titleCache = Collections.synchronizedMap(new java.util.LinkedHashMap<>(4096, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String,List<TitleEdit>> entry) { return size() > 4096; }
    });
    private final Map<String,String> quietCache=BoundedCache.create(4096);
    private final Map<String,List<TitleEdit>> displayCache=BoundedCache.create(4096);
    private final Map<String,java.util.Optional<ReorderedTitle>> reorderedCache=BoundedCache.create(4096);
    private final Map<String,Boolean> styleFailureCache=BoundedCache.create(4096);
    private final Map<Integer,List<Rule>> ruleBuckets=BoundedCache.create(128);
    private final Path unknownFile;
    private final Set<String> misses = Collections.synchronizedSet(new HashSet<>());
    private final java.util.concurrent.ArrayBlockingQueue<String> unknownQueue=new java.util.concurrent.ArrayBlockingQueue<>(256);
    private final java.util.concurrent.atomic.AtomicBoolean unknownScheduled=new java.util.concurrent.atomic.AtomicBoolean();
    private final java.util.concurrent.atomic.AtomicLong unknownDropped=new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.ThreadPoolExecutor unknownWriter = new java.util.concurrent.ThreadPoolExecutor(1,1,0L,java.util.concurrent.TimeUnit.MILLISECONDS,new java.util.concurrent.ArrayBlockingQueue<>(1),r -> {
        Thread t = new Thread(r, "WitherSkyRU-Unknown-Writer");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean recordUnknown = true;
    private final ThreadLocal<Boolean> quietSearch = ThreadLocal.withInitial(() -> false);

    private record Rule(Pattern pattern, String replacement,int initial) {}
    private static final Pattern[] INITIALS=new Pattern[26];
    static {for(int i=0;i<26;i++)INITIALS[i]=Pattern.compile(String.valueOf((char)('a'+i)),Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);}

    public OfflineDictionary(Path overridesDir) throws IOException {
        BulkLore117.initialize();
        this.unknownFile = overridesDir.resolve("unknown.tsv");
        Map<String,String> dictionary = new HashMap<>();
        for (String resource : List.of("phrases.tsv", "vanilla-names.tsv", "lexicon-names.tsv", "curated.tsv", "lore.tsv", "continuation.tsv", "enchantments.tsv", "names.tsv", "pet-lore.tsv", "catalogue.tsv", "corrections.tsv", "quality-fixes.tsv", "proofread.tsv", "quest-interface.tsv")) {
            try (InputStream stream = OfflineDictionary.class.getResourceAsStream("/data/witherskyru/" + resource)) {
                if (stream == null) throw new IOException("Missing embedded dictionary: " + resource);
                try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    parsePairs(reader, dictionary, true);
                }
            }
        }
        Path overrides = overridesDir.resolve("overrides.tsv");
        Map<String,String> custom = new HashMap<>();
        if (Files.exists(overrides)) {
            try (BufferedReader reader = Files.newBufferedReader(overrides, StandardCharsets.UTF_8)) {
                parsePairs(reader, custom, true);
            }
        }
        dictionary.putAll(custom);
        this.customOverrides = custom.keySet().stream().map(OfflineDictionary::fold).collect(java.util.stream.Collectors.toUnmodifiableSet());
        this.exact = Map.copyOf(dictionary);
        Map<String,String> insensitive = new HashMap<>();
        // When capitalisation changes meaning/style (e.g. COMMON versus common),
        // never use case-insensitive fallback for that ambiguous key.
        Set<String> ambiguous = new HashSet<>();
        for (var entry : new java.util.TreeMap<>(dictionary).entrySet()) {
            String key = fold(entry.getKey());
            if (insensitive.containsKey(key) && !insensitive.get(key).equals(entry.getValue())) ambiguous.add(key);
            else insensitive.putIfAbsent(key, entry.getValue());
        }
        for (String key : ambiguous) insensitive.remove(key);
        this.folded = Map.copyOf(insensitive);
        Map<String,String[]> forms = new HashMap<>();
        try (InputStream stream = OfflineDictionary.class.getResourceAsStream("/data/witherskyru/grammar.tsv")) {
            if (stream == null) throw new IOException("Missing embedded grammar");
            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] fields = line.split("\\t");
                    if (fields.length != 3) throw new IOException("Invalid grammar: " + line);
                    forms.put(fold(fields[0]), new String[] {fields[1], fields[2]});
                }
            }
        }
        this.grammar = Map.copyOf(forms);
        this.reforgeForms = readMetadata("reforges.tsv", 5);
        this.reforgePrefixes = reforgeForms.keySet().stream().sorted(java.util.Comparator.comparingInt(String::length).reversed().thenComparing(String::compareTo)).toList();
        this.suffixReforges = Set.copyOf(readMetadata("reforge-placement.tsv", 2).keySet());
        Map<String,String[]> genders = new HashMap<>(readMetadata("item-genders.tsv", 2));
        genders.putAll(readMetadata("item-gender-overrides.tsv", 2));
        this.itemGenders = Collections.unmodifiableMap(genders);
        this.enchantmentNames = Set.copyOf(readMetadata("enchantments.tsv", 2).keySet());
        List<Rule> compiled = new ArrayList<>();
        for (String resource : List.of("proofread-patterns.tsv", "editorial-patterns.tsv", "pet-lore-patterns.tsv", "continuation-patterns.tsv", "expanded-patterns.tsv", "canonical-patterns.tsv")) {
        try (InputStream stream = OfflineDictionary.class.getResourceAsStream("/data/witherskyru/" + resource)) {
            if (stream == null) throw new IOException("Missing embedded template dictionary");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                int lineNo = 0;
                while ((line = reader.readLine()) != null) {
                    lineNo++;
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] pair = line.split("\\t", 2);
                    if (pair.length != 2 || pair[0].isEmpty() || pair[1].isEmpty()) throw new IOException("Bad pattern line "+lineNo);
                    var pattern=Pattern.compile(pair[0], Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                    compiled.add(new Rule(pattern,pair[1],mandatoryInitial(pattern)));
                }
            }
        }
        }
        this.rules = List.copyOf(compiled);
    }

    private static void parsePairs(BufferedReader reader, Map<String,String> result, boolean allowOverrides) throws IOException {
        String line;
        int number = 0;
        while ((line = reader.readLine()) != null) {
            number++;
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] pair = line.split("\\t", 2);
            if (pair.length != 2 || pair[0].isBlank() || pair[1].isBlank()) throw new IOException("Invalid phrase entry at line " + number);
            String key = pair[0].trim();
            String value = pair[1].trim();
            if (!allowOverrides && result.containsKey(key) && !result.get(key).equals(value)) throw new IOException("Conflicting entry: " + key);
            result.put(key, value);
        }
    }

    public int phraseCount() { return exact.size(); }
    public int patternCount() { return rules.size(); }
    /** Only a proven mandatory first literal; complex/optional/alternative roots remain fallback. */
    private static int mandatoryInitial(Pattern pattern){
        String regex=pattern.pattern();int at=regex.startsWith("(?i)")?4:0;
        if((pattern.flags()&(Pattern.LITERAL|Pattern.CANON_EQ|Pattern.COMMENTS))!=0||regex.contains("\\Q")||regex.contains("\\E")||regex.contains("#")
            ||at+1>=regex.length()||regex.charAt(at)!='^')return 0;
        char first=regex.charAt(++at);if(!(first>='a'&&first<='z'||first>='A'&&first<='Z'))return 0;
        if(at+1<regex.length()&&"?*{".indexOf(regex.charAt(at+1))>=0)return 0;
        int depth=0;boolean characterClass=false;
        for(int i=0;i<regex.length();i++){
            char c=regex.charAt(i);if(c=='\\'){i++;continue;}
            if(c=='['){if(characterClass)return 0;characterClass=true;continue;}
            if(c==']'&&characterClass){characterClass=false;continue;}
            if(characterClass)continue;
            if(c=='(')depth++;else if(c==')')depth--;else if(c=='|'&&depth==0)return 0;
        }
        return Character.toLowerCase(first);
    }
    private List<Rule> candidateRules(String source){
        if(source.isEmpty())return rules;
        int point=source.codePointAt(0);var saved=ruleBuckets.get(point);if(saved!=null)return saved;
        boolean[] initials=new boolean[26];
        if(point>='a'&&point<='z')initials[point-'a']=true;
        else if(point>='A'&&point<='Z')initials[point-'A']=true;
        else if(point>127){String first=new String(Character.toChars(point));for(int i=0;i<26;i++)initials[i]=INITIALS[i].matcher(first).matches();}
        var selected=new ArrayList<Rule>();
        // Original order is essential: a specific rule may precede a generic fallback.
        for(var rule:rules)if(rule.initial()==0||initials[rule.initial()-'a'])selected.add(rule);
        var result=List.copyOf(selected);ruleBuckets.put(point,result);return result;
    }
    public void setRecordUnknown(boolean enabled) { recordUnknown = enabled; }

    /** Searching the entire item index must not populate the observed-screen backlog. */
    public String translateForSearch(String source) {
        boolean before = quietSearch.get();
        quietSearch.set(true);
        try { return translate(source); }
        finally { if (before) quietSearch.set(true); else quietSearch.remove(); }
    }

    private static Map<String,String[]> readMetadata(String filename, int columns) throws IOException {
        Map<String,String[]> result = new java.util.LinkedHashMap<>();
        try (InputStream stream = OfflineDictionary.class.getResourceAsStream("/data/witherskyru/" + filename)) {
            if (stream == null) throw new IOException("Missing metadata: " + filename);
            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] fields = line.split("\\t");
                    if (fields.length != columns) throw new IOException("Invalid metadata in " + filename);
                    result.put(fold(fields[0]), java.util.Arrays.copyOfRange(fields, 1, fields.length));
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static String fold(String value) {
        String trimmed=value.trim();boolean collapse=false;char previous=0;
        for(int i=0;i<trimmed.length();i++){
            char c=trimmed.charAt(i);
            if(c=='\t'||c=='\n'||c=='\r'||c=='\f'||c=='\u000b'||c==' '&&previous==' '){collapse=true;break;}
            previous=c;
        }
        return (collapse?WHITESPACE.matcher(trimmed).replaceAll(" "):trimmed).toLowerCase(Locale.ROOT);
    }
    private static final Pattern WHITESPACE=Pattern.compile("\\s+");

    public String translate(String input) {
        if (input == null || input.isEmpty() || DisplayNoop.numbers(input)) return input;
        if (input.indexOf('\n') >= 0) {
            String[] lines = input.split("\\n", -1);
            List<String> original = List.of(lines.clone());
            var contributors=CollectionContributors.rows(original);
            boolean changed = false;
            for (int i=0;i<lines.length;i++) {
                String next = contributors.contains(i)?lines[i]:translateLine(lines[i]);
                if (!next.equals(lines[i])) changed = true;
                lines[i] = next;
            }
            var contextual = EditorialContext.apply(contextEdits(original), List.of(lines));
            contextual=BoosterUpgradeLore.plain(original,contextual,keys->!hasCustomOverride(keys));
            return changed || !contextual.equals(original) ? String.join("\n",contextual) : input;
        }
        return translateLine(input);
    }

    /** User-authored overrides are final, including in neighbouring-line contexts. */
    public List<EditorialContext.Edit> contextEdits(List<String> source) {
        if (hasCustomOverride(source)) return List.of();
        return EditorialContext.edits(source);
    }
    boolean hasCustomOverride(List<String> source) {
        if(customOverrides.isEmpty())return false;
        for(String key:source)if(customOverrides.contains(fold(key)))return true;
        return false;
    }
    /** Immutable per-dictionary flag; avoid constructing candidate keys when none can match. */
    boolean hasCustomOverrides(){return !customOverrides.isEmpty();}

    private String translateLine(String line) {
        if (quietSearch.get()) {
            String saved=cache.get(line);if(saved!=null)return saved;
            saved=quietCache.get(line);if(saved!=null)return saved;
            String converted=convertLine(line);quietCache.put(line,converted);return converted;
        }
        String saved = cache.get(line);
        if (saved != null) return saved;
        // Quiet mode changes only unknown recording. With no ASCII letters, every
        // possible source substring fails that recorder's original ASCII condition.
        saved=quietCache.get(line);
        if(saved!=null&&!hasAsciiLetters(line)){cache.put(line,saved);return saved;}
        String converted = convertLine(line);
        cache.put(line, converted);
        return converted;
    }

    private String lookup(String value) {
        return lookup(value, 0);
    }

    /** Closed item-title spans, not a general prose/word substitution API. */
    public record TitleEdit(int start, int end, String replacement) {}
    public record ReorderedTitle(int start, int end, List<TitleEdit> parts) {}

    /** Closed suffix modifiers move with their own Style, never with the base item's actions. */
    ReorderedTitle reorderedItemTitle(String source) {
        var saved=reorderedCache.get(source);if(saved!=null)return saved.orElse(null);
        var result=findReorderedItemTitle(source);reorderedCache.put(source,java.util.Optional.ofNullable(result));return result;
    }
    private ReorderedTitle findReorderedItemTitle(String source) {
        if(DisplayDates.supports(source)&&!hasCustomOverride(DisplayDates.keys(source))){
            var date=DisplayDates.plan(source);if(date!=null)return date;
        }
        var edits = itemTitleEdits(source);
        // This compound has a primary red Sin / purple seeker Scythe title.
        // Map authored semantic pieces, rather than flattening arbitrary coloured names.
        if (!customOverrides.contains(fold(source))) {
            boolean reviewed = false, overridden = false;
            var parts = new ArrayList<TitleEdit>();
            int previous = edits.isEmpty() ? 0 : edits.getFirst().start();
            for (var part : edits) {
                String value = source.substring(part.start(), part.end());
                overridden |= customOverrides.contains(fold(value));
                if (part.start() > previous)
                    parts.add(new TitleEdit(previous, part.start(), source.substring(previous, part.start())));
                if (value.equals("Sinseeker Scythe") && part.replacement().equals("Коса искателя грехов")) {
                    parts.add(new TitleEdit(part.start()+3, part.end(), "Коса искателя "));
                    parts.add(new TitleEdit(part.start(), part.start()+3, "грехов"));
                    reviewed = true;
                } else parts.add(part);
                previous = part.end();
            }
            if (reviewed && !overridden)
                return new ReorderedTitle(edits.getFirst().start(), edits.getLast().end(), List.copyOf(parts));
        }
        if (edits.size() != 1) return null;
        var edit = edits.getFirst();
        String value = source.substring(edit.start(), edit.end());
        if (customOverrides.contains(fold(value))) return null;
        for (String prefix : reforgePrefixes) {
            if (!suffixReforges.contains(prefix) || value.length() <= prefix.length()
                || value.charAt(prefix.length()) != ' ' || !value.regionMatches(true, 0, prefix, 0, prefix.length())) continue;
            String base = value.substring(prefix.length() + 1);
            String[] gender = itemGenders.get(fold(base));
            if (gender == null) continue;
            String target = lookup(base);
            int form = switch(gender[0]) { case "f" -> 1; case "n" -> 2; case "p" -> 3; default -> 0; };
            String modifier = reforgeForms.get(prefix)[form];
            if (target == null || !edit.replacement().equals(target + " " + modifier)) continue;
            int gap = edit.start() + prefix.length();
            return new ReorderedTitle(edit.start(), edit.end(), List.of(
                new TitleEdit(gap + 1, edit.end(), target),
                new TitleEdit(gap, gap + 1, " "),
                new TitleEdit(edit.start(), gap, modifier)));
        }
        return null;
    }
    /** Reviewed display spans reuse the style-preserving renderer, not an item-name override. */
    List<TitleEdit> displayEdits(String source) {
        if(source==null||source.length()>400||source.indexOf('\n')>=0)return List.of();
        var saved=displayCache.get(source);if(saved!=null)return saved;
        var result=List.copyOf(findDisplayEdits(source));displayCache.put(source,result);return result;
    }
    private List<TitleEdit> findDisplayEdits(String source) {
        boolean noOverrides=customOverrides.isEmpty();
        var bulk=BulkLore117.find(source);
        if(bulk!=null&&(noOverrides||!hasCustomOverride(bulk.plan().keys()))){
            var edits=bulk.edits(translateForSearch(source));if(!edits.isEmpty())return edits;
        }
        if(EnchantBookFooter.supports(source)){
            var book=EnchantBookFooter.edits(source);if(!book.isEmpty()){
                // A whole-caption override must not fall through to separately
                // translated child literals. Mixed styles safely keep the source.
                if(!noOverrides&&hasCustomOverride(List.of(source)))return List.of(new TitleEdit(0,source.length(),translateForSearch(source)));
                if((noOverrides||!hasCustomOverride(EnchantBookFooter.keys(source)))&&java.util.Objects.equals(EnchantBookFooter.translate(source),translateForSearch(source)))return book;
            }
        }
        if(ExperimentDisplay.supports(source)&&(noOverrides||!hasCustomOverride(ExperimentDisplay.keys(source)))){
            var exp=ExperimentDisplay.edits(source);if(!exp.isEmpty()&&java.util.Objects.equals(ExperimentDisplay.translate(source),translateForSearch(source)))return exp;
        }
        if(DisplayDates.supports(source)&&(noOverrides||!hasCustomOverride(DisplayDates.keys(source)))){
            var date=DisplayDates.translate(source);if(date!=null&&java.util.Objects.equals(date,translateForSearch(source)))return List.of(new TitleEdit(0,source.length(),date));
        }
        if(noOverrides||!hasCustomOverride(InterfaceDisplay.keys(source))){var ui=InterfaceDisplay.edits(source,this::lookup);if(!ui.isEmpty()&&java.util.Objects.equals(InterfaceDisplay.applyEdits(source,ui),translateForSearch(source)))return ui;}
        if(noOverrides||!hasCustomOverride(ScreenLabels.keys(source))){var labels=ScreenLabels.edits(source,translateForSearch(source));if(!labels.isEmpty())return labels;}
        if(noOverrides||!hasCustomOverride(DungeonUiLabels.keys(source))){var ui=DungeonUiLabels.edits(source,translateForSearch(source));if(!ui.isEmpty())return ui;}
        if(noOverrides||!hasCustomOverride(MinionNameTranslation.keys(source))){var minion=MinionNameTranslation.edits(source,translateForSearch(source),this::lookup);if(!minion.isEmpty())return minion;}
        if(noOverrides||!hasCustomOverride(CalendarLabels.keys(source))){var calendar=CalendarLabels.edits(source);if(!calendar.isEmpty()&&java.util.Objects.equals(CalendarLabels.translate(source),translateForSearch(source)))return calendar;}
        if (noOverrides||!hasCustomOverride(SkillLabelTranslation.keys(source))) {
            var skill=SkillLabelTranslation.edits(source,translateForSearch(source));
            if(!skill.isEmpty())return skill;
        }
        if (DurationLabels.supports(source) && (noOverrides||!hasCustomOverride(DurationLabels.overrideKeys(source)))) {
            var duration=DurationLabels.edits(source,translateForSearch(source));
            if(!duration.isEmpty())return duration;
        }
        if (MiningInstructions.supports(source) && source.length() <= 400 && (noOverrides||!hasCustomOverride(MiningInstructions.overrideKeys(source)))) {
            var instructions = MiningInstructions.edits(source, translateForSearch(source));
            if (!instructions.isEmpty()) return instructions;
        }
        if (source != null && source.startsWith("❣ Requires ") && source.length() <= 400 && (noOverrides||!hasCustomOverride(RequirementTranslation.overrideKeys(source)))) {
            var requirement = RequirementTranslation.edits(source, exact.get(source));
            if (!requirement.isEmpty()) return requirement;
        }
        return itemTitleEdits(source);
    }
    boolean preserveRequirementOnStyleFailure(String source) {
        var saved=styleFailureCache.get(source);if(saved!=null)return saved;
        boolean result=findStyleFailurePolicy(source);styleFailureCache.put(source,result);return result;
    }
    private boolean findStyleFailurePolicy(String source) {
        boolean noOverrides=customOverrides.isEmpty();
        var bulk=BulkLore117.find(source);
        if(bulk!=null&&(noOverrides||!hasCustomOverride(bulk.plan().keys())))return true;
        return EnchantBookFooter.supports(source)&&!EnchantBookFooter.edits(source).isEmpty()
            || ExperimentDisplay.supports(source)&&(noOverrides||!hasCustomOverride(ExperimentDisplay.keys(source)))&&!ExperimentDisplay.edits(source).isEmpty()
            || DisplayDates.supports(source)&&(noOverrides||!hasCustomOverride(DisplayDates.keys(source)))&&DisplayDates.plan(source)!=null
            || (noOverrides||!hasCustomOverride(InterfaceDisplay.keys(source)))&&!InterfaceDisplay.edits(source,this::lookup).isEmpty()
            || (noOverrides||!hasCustomOverride(ScreenLabels.keys(source)))&&!ScreenLabels.edits(source,translateForSearch(source)).isEmpty()
            || MinionNameTranslation.supports(source)&&(noOverrides||!hasCustomOverride(MinionNameTranslation.keys(source)))
            || (noOverrides||!hasCustomOverride(DungeonUiLabels.keys(source)))&&!DungeonUiLabels.edits(source,translateForSearch(source)).isEmpty()
            || (noOverrides||!hasCustomOverride(CalendarLabels.keys(source))) && !CalendarLabels.edits(source).isEmpty()
            || (noOverrides||!hasCustomOverride(SkillLabelTranslation.keys(source)))
            && !SkillLabelTranslation.edits(source,translateForSearch(source)).isEmpty()
            || DurationLabels.supports(source) && (noOverrides||!hasCustomOverride(DurationLabels.overrideKeys(source)))
            && !DurationLabels.edits(source,translateForSearch(source)).isEmpty()
            || source.startsWith("❣ Requires ") && (noOverrides||!hasCustomOverride(RequirementTranslation.overrideKeys(source)))
            && !RequirementTranslation.edits(source, exact.get(source)).isEmpty()
            || MiningInstructions.supports(source) && (noOverrides||!hasCustomOverride(MiningInstructions.overrideKeys(source)))
            && !MiningInstructions.edits(source, translateForSearch(source)).isEmpty();
    }
    public List<TitleEdit> itemTitleEdits(String source) {
        if (source == null || source.length() > 400 || source.indexOf('\n') >= 0) return List.of();
        var cached = titleCache.get(source);
        if (cached != null) return cached;
        var result = findItemTitleEdits(source);
        titleCache.put(source, result);
        return result;
    }
    private List<TitleEdit> findItemTitleEdits(String source) {
        int start = 0, end = source.length();
        // Prefer the longest known name: model numbers belong to the item, not its decoration.
        while (start < end && !Character.isLetter(source.codePointAt(start)))
            start += Character.charCount(source.codePointAt(start));
        for (int stop = end; stop > start;) {
            String candidate = source.substring(start, stop).stripTrailing();
            List<TitleEdit> edits = bareItemTitleEdits(candidate, start);
            if (!edits.isEmpty()) return edits;
            int cp = source.codePointBefore(stop);
            if (Character.isLetter(cp)) break;
            stop -= Character.charCount(cp);
        }
        return List.of();
    }

    private List<TitleEdit> bareItemTitleEdits(String value, int offset) {
        if (itemGenders.containsKey(fold(value))) {
            String target = lookup(value);
            if (target != null && !target.equals(value)) return List.of(new TitleEdit(offset, offset + value.length(), target));
        }
        for (String rarity : RARITY_TOKENS) {
            if (value.startsWith(rarity + " ")) {
                var rest = bareItemTitleEdits(value.substring(rarity.length() + 1), offset + rarity.length() + 1);
                if (!rest.isEmpty()) {
                    if (customOverrides.contains(fold(value)))
                        return List.of(new TitleEdit(offset, offset + value.length(), lookup(value)));
                    return rest;
                }
            }
        }
        for (String prefix : reforgePrefixes) {
            if (value.length() <= prefix.length() || value.charAt(prefix.length()) != ' '
                || !value.regionMatches(true, 0, prefix, 0, prefix.length())) continue;
            String base = value.substring(prefix.length() + 1);
            String[] gender = itemGenders.get(fold(base));
            if (gender == null) continue;
            String target = lookup(base);
            if (target == null || target.equals(base)) continue;
            String full = lookup(value);
            if (full == null || full.equals(value)) continue;
            int form = switch(gender[0]) { case "f" -> 1; case "n" -> 2; case "p" -> 3; default -> 0; };
            String modifier = reforgeForms.get(prefix)[form];
            // Full-name overrides and suffix reforges cannot be split into adjective/base spans.
            if (!full.equals(modifier + " " + target) || suffixReforges.contains(prefix))
                return List.of(new TitleEdit(offset, offset + value.length(), full));
            return List.of(new TitleEdit(offset, offset + prefix.length(), modifier),
                new TitleEdit(offset + prefix.length() + 1, offset + value.length(), target));
        }
        return List.of();
    }

    private String translateItemTitle(String value) {
        List<TitleEdit> edits = itemTitleEdits(value);
        if (edits.isEmpty()) return null;
        StringBuilder result = new StringBuilder();
        int from = 0;
        for (TitleEdit edit : edits) {
            result.append(value, from, edit.start()).append(edit.replacement());
            from = edit.end();
        }
        return result.append(value, from, value.length()).toString();
    }

    private String lookup(String value, int depth) {
        if (!value.isEmpty() && Character.isUpperCase(value.charAt(0)) && RARITY_TOKENS.contains(value.toUpperCase(Locale.ROOT))) return value;
        String result = exact.get(value);
        // Title-case Epic/Legendary can be reforges on a known item. The folded
        // category "EPIC BOW" must not mask "Epic Bow". Exact/user overrides still win.
        if (result == null && depth <= 2 && !customOverrides.contains(fold(value))
            && (value.startsWith("Epic ") || value.startsWith("Legendary "))) {
            int space = value.indexOf(' ');
            String[] gender = itemGenders.get(fold(value.substring(space + 1)));
            if (gender != null) {
                String base = lookup(value.substring(space + 1), depth + 1);
                if (base != null) {
                    int form = switch(gender[0]) { case "f" -> 1; case "n" -> 2; case "p" -> 3; default -> 0; };
                    return reforgeForms.get(fold(value.substring(0, space)))[form] + " " + base;
                }
            }
        }
        if (result == null) result = folded.get(fold(value));
        if (result != null || depth > 2) return result;
        for (String tier : List.of("VERY SPECIAL", "UNCOMMON", "COMMON", "LEGENDARY", "MYTHIC", "DIVINE", "SPECIAL", "SUPREME", "ULTIMATE", "RARE", "EPIC")) {
            if (value.startsWith(tier + " ") && !value.startsWith("RARE DROP")) {
                String translated = lookup(value.substring(tier.length() + 1), depth + 1);
                if (translated != null) return tier + " " + translated;
            }
        }
        // A title-case Epic/Legendary prefix on a known item is a reforge;
        // uppercase EPIC/LEGENDARY above is a rarity and must remain English.
        for (String prefix : reforgePrefixes) {
            if (value.length() <= prefix.length() || value.charAt(prefix.length()) != ' '
                || !value.regionMatches(true, 0, prefix, 0, prefix.length())) continue;
            String[] modifier = reforgeForms.get(prefix);
            String base = value.substring(prefix.length() + 1);
            String[] gender = itemGenders.get(fold(base));
            if (modifier != null && gender != null) {
                String translated = lookup(base, depth + 1);
                int form = switch(gender[0]) { case "f" -> 1; case "n" -> 2; case "p" -> 3; default -> 0; };
                if (translated != null) return suffixReforges.contains(prefix)
                    ? translated + " " + modifier[form] : modifier[form] + " " + translated;
            }
        }
        return null;
    }

    /** Closed enchantment vocabulary only, not arbitrary comma-separated prose. */
    private String translateEnchantments(String value) {
        String marker = value.startsWith("› ") ? "› " : "";
        String body = value.substring(marker.length());
        String[] parts = body.split("(?<=,)", -1);
        StringBuilder out = new StringBuilder(marker);
        boolean changed = false;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            boolean comma = part.endsWith(",");
            String content = comma ? part.substring(0, part.length() - 1) : part;
            Matcher trim = TRIM.matcher(content);
            if (!trim.matches()) return null;
            String member = trim.group(2);
            if (member.isEmpty() && i == parts.length - 1 && i > 0) {
                out.append(content); // Native lore may end with a comma.
                continue;
            }
            Matcher level = ENCHANTMENT_LEVEL.matcher(member);
            if (!level.matches() || !enchantmentNames.contains(fold(level.group(1)))) return null;
            String translated = lookup(level.group(1));
            if (translated == null) return null;
            out.append(trim.group(1)).append(translated);
            if (level.group(2) != null) out.append(level.group(2));
            out.append(trim.group(3));
            if (comma) out.append(',');
            changed |= !translated.equals(level.group(1));
        }
        return changed ? out.toString() : null;
    }

    private String argument(String value) {
        if (value == null) return "";
        String result = lookup(value);
        return result == null ? value : result;
    }

    private String renderTemplate(Matcher source, String template) {
        Matcher references = CAPTURE_REFERENCE.matcher(template);
        StringBuffer result = new StringBuffer();
        while (references.find()) {
            String caseName = references.group(2);
            String value = source.group(Integer.parseInt(caseName == null ? references.group(1) : references.group(3)));
            String[] forms = value == null ? null : grammar.get(fold(value));
            String next = caseName != null && forms != null ? forms[caseName.equals("gen") ? 0 : 1] : argument(value);
            references.appendReplacement(result, Matcher.quoteReplacement(next));
        }
        references.appendTail(result);
        return TranslationGrammar.polish(result.toString());
    }

    private String convertLine(String line) {
        Matcher split = TRIM.matcher(line);
        if (!split.matches()) return line;
        String leading = split.group(1);
        String core = split.group(2);
        String ending = split.group(3);
        if (core.isBlank()) return line;
        // Preserve text-style codes without trying to translate their bytes.
        StringBuilder prefixCodes = new StringBuilder();
        while (core.length() >= 2 && core.charAt(0)=='§' && COLORS.matcher(core.substring(0,2)).matches()) {
            prefixCodes.append(core,0,2);
            core = core.substring(2);
        }
        if (core.isBlank() || SAFE.matcher(core).matches() || URL.matcher(core).matches()) return line;
        // Translate each legacy-format run independently: no color code is discarded
        // or guessed at a new position after Russian text changes length.
        Matcher color = COLORS.matcher(core);
        if (color.find()) {
            StringBuilder runs = new StringBuilder();
            int offset = 0;
            do {
                runs.append(translateLine(core.substring(offset, color.start())));
                runs.append(color.group());
                offset = color.end();
            } while (color.find());
            runs.append(translateLine(core.substring(offset)));
            return leading + prefixCodes + runs + ending;
        }
        if(ScreenLabels.finalText(core))return line;
        boolean noOverrides=customOverrides.isEmpty();
        String converted = lookup(core);
        if(EnchantBookFooter.supports(core)&&(noOverrides||!hasCustomOverride(EnchantBookFooter.keys(core)))){var book=EnchantBookFooter.translate(core);if(book!=null)converted=book;}
        if(ExperimentDisplay.supports(core)&&(noOverrides||!hasCustomOverride(ExperimentDisplay.keys(core)))){var exp=ExperimentDisplay.translate(core);if(exp!=null)converted=exp;}
        if(DisplayDates.supports(core)&&(noOverrides||!hasCustomOverride(DisplayDates.keys(core)))){var date=DisplayDates.translate(core);if(date!=null)converted=date;}
        if(converted==null&&(noOverrides||!hasCustomOverride(ScreenLabels.keys(core))))converted=ScreenLabels.wrappedTranslation(core);
        if(InterfaceDisplay.cropMilestoneShape(core)&&(noOverrides||!hasCustomOverride(List.of(core))))
            converted=InterfaceDisplay.translate(core,this::lookup);
        if(converted==null&&CommissionInstructions.supports(core)&&(noOverrides||!hasCustomOverride(CommissionInstructions.keys(core))))converted=CommissionInstructions.translate(core);
        if(converted==null&&(noOverrides||InterfaceDisplay.hudShape(core)||!hasCustomOverride(InterfaceDisplay.keys(core))))converted=InterfaceDisplay.translate(core,this::lookup);
        if(converted==null&&(noOverrides||!hasCustomOverride(DungeonUiLabels.keys(core))))converted=DungeonUiLabels.translate(core);
        if (converted == null && (noOverrides||!hasCustomOverride(MinionNameTranslation.keys(core)))) converted=MinionNameTranslation.translate(core,this::lookup);
        if (converted == null && (noOverrides||!hasCustomOverride(CalendarLabels.keys(core)))) converted=CalendarLabels.translate(core);
        if (converted == null && (noOverrides||!hasCustomOverride(SkillLabelTranslation.keys(core)))) converted=SkillLabelTranslation.translate(core);
        if (converted == null) converted = translateItemTitle(core);
        if (converted == null) converted = translateEnchantments(core);
        // A complete, anchored template is more specific than a decorative label.
        // Otherwise "7331 HOTM Exp" uses the bare label and bypasses its number form.
        if (converted == null) {
            for (Rule rule : candidateRules(core)) {
                Matcher matcher = rule.pattern.matcher(core);
                if (matcher.matches()) {
                    try {
                        converted = renderTemplate(matcher, rule.replacement);
                    } catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {
                        // Malformed external template: keep the safe fallback.
                    }
                    if (converted != null) break;
                }
            }
        }
        if (converted == null) {
            Matcher decorated = DECORATED.matcher(core);
            if (decorated.matches()) {
                String middle = lookup(decorated.group(2).trim());
                String originalMiddle = decorated.group(2);
                String suffixSpace = originalMiddle.substring(originalMiddle.stripTrailing().length());
                String suffix = decorated.group(3);
                if (suffix.matches("[.,!?]") && middle != null && middle.endsWith(suffix)) suffix = "";
                if (middle != null) converted = TranslationGrammar.polish(decorated.group(1) + middle + suffixSpace + suffix);
            }
        }
        if (converted == null) {
            Matcher colon = COLON.matcher(core);
            if (colon.matches()) {
                String left = colon.group(1), right = colon.group(2);
                String a = argument(left);
                String b = argument(right);
                if (!a.equals(left) || !b.equals(right)) converted = a + ": " + b;
            }
        }
        if (converted == null) {
            logUnknown(core);
            return line;
        }
        return leading + prefixCodes + converted + ending;
    }

    private void logUnknown(String value) {
        if (!recordUnknown || quietSearch.get() || value.length()<4 || value.length()>400 || !hasAsciiLetters(value)) return;
        // Check before insertion: the former post-add check still retained every
        // new dynamic string after the nominal15000 limit was reached.
        synchronized(misses){if(misses.size()>=15000||!misses.add(value))return;}
        if(unknownQueue.offer(value))scheduleUnknownWrite();else unknownDropped.incrementAndGet();
    }
    private void scheduleUnknownWrite(){
        if(unknownScheduled.compareAndSet(false,true))unknownWriter.execute(this::drainUnknown);
    }
    /** Bounded best-effort diagnostics. Never blocks rendering on disk or queue capacity. */
    private void drainUnknown(){
        try{
            var batch=new ArrayList<String>(64);
            while(!unknownQueue.isEmpty()){
                unknownQueue.drainTo(batch,64);if(batch.isEmpty())break;
                var text=new StringBuilder();for(String value:batch)text.append(value.replace('\t',' ').replace('\r',' ').replace('\n',' ')).append('\n');
                try{
                    Files.createDirectories(unknownFile.getParent());
                    Files.writeString(unknownFile,text,StandardCharsets.UTF_8,java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);
                }catch(IOException ignored){}
                batch.clear();
            }
        }finally{
            unknownScheduled.set(false);
            if(!unknownQueue.isEmpty())scheduleUnknownWrite();
        }
    }
    private static boolean hasAsciiLetters(String value){
        for(int i=0;i<value.length();i++){char c=value.charAt(i);if(c>='A'&&c<='Z'||c>='a'&&c<='z')return true;}
        return false;
    }
}
