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
    private static final Set<String> RARITY_TOKENS = Set.of("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "DIVINE", "SPECIAL", "VERY SPECIAL", "SUPREME", "ULTIMATE");

    private final Map<String, String> exact;
    private final Map<String, String> folded;
    private final List<Rule> rules;
    private final Map<String,String[]> grammar;
    private final Map<String,String[]> reforgeForms;
    private final Map<String,String[]> itemGenders;
    private final Map<String,String> cache = Collections.synchronizedMap(new java.util.LinkedHashMap<>(4096, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String,String> entry) { return size() > 4096; }
    });
    private final Path unknownFile;
    private final Set<String> misses = Collections.synchronizedSet(new HashSet<>());
    private final ExecutorService unknownWriter = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "WitherSkyRU-Unknown-Writer");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean recordUnknown = true;

    private record Rule(Pattern pattern, String replacement) {}

    public OfflineDictionary(Path overridesDir) throws IOException {
        this.unknownFile = overridesDir.resolve("unknown.tsv");
        Map<String,String> dictionary = new HashMap<>();
        for (String resource : List.of("phrases.tsv", "vanilla-names.tsv", "lexicon-names.tsv", "curated.tsv", "lore.tsv", "continuation.tsv", "enchantments.tsv", "names.tsv", "pet-lore.tsv", "catalogue.tsv", "corrections.tsv", "quality-fixes.tsv")) {
            try (InputStream stream = OfflineDictionary.class.getResourceAsStream("/data/witherskyru/" + resource)) {
                if (stream == null) throw new IOException("Missing embedded dictionary: " + resource);
                try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    parsePairs(reader, dictionary, true);
                }
            }
        }
        Path overrides = overridesDir.resolve("overrides.tsv");
        if (Files.exists(overrides)) {
            try (BufferedReader reader = Files.newBufferedReader(overrides, StandardCharsets.UTF_8)) {
                parsePairs(reader, dictionary, true);
            }
        }
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
        this.itemGenders = readMetadata("item-genders.tsv", 2);
        List<Rule> compiled = new ArrayList<>();
        for (String resource : List.of("editorial-patterns.tsv", "pet-lore-patterns.tsv", "continuation-patterns.tsv", "expanded-patterns.tsv", "canonical-patterns.tsv")) {
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
                    compiled.add(new Rule(Pattern.compile(pair[0], Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), pair[1]));
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
    public void setRecordUnknown(boolean enabled) { recordUnknown = enabled; }

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
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public String translate(String input) {
        if (input == null || input.isEmpty()) return input;
        if (input.indexOf('\n') >= 0) {
            String[] lines = input.split("\\n", -1);
            boolean changed = false;
            for (int i=0;i<lines.length;i++) {
                String next = translateLine(lines[i]);
                if (!next.equals(lines[i])) changed = true;
                lines[i] = next;
            }
            return changed ? String.join("\n",lines) : input;
        }
        return translateLine(input);
    }

    private String translateLine(String line) {
        String saved = cache.get(line);
        if (saved != null) return saved;
        String converted = convertLine(line);
        cache.put(line, converted);
        return converted;
    }

    private String lookup(String value) {
        return lookup(value, 0);
    }

    private String lookup(String value, int depth) {
        if (!value.isEmpty() && Character.isUpperCase(value.charAt(0)) && RARITY_TOKENS.contains(value.toUpperCase(Locale.ROOT))) return value;
        String result = exact.get(value);
        if (result == null) result = folded.get(fold(value));
        if (result != null || depth > 2) return result;
        for (String tier : List.of("VERY SPECIAL", "UNCOMMON", "COMMON", "LEGENDARY", "MYTHIC", "DIVINE", "SPECIAL", "SUPREME", "ULTIMATE", "RARE", "EPIC")) {
            if (value.startsWith(tier + " ") && !value.startsWith("RARE DROP")) {
                String translated = lookup(value.substring(tier.length() + 1), depth + 1);
                if (translated != null) return tier + " " + translated;
            }
        }
        int space = value.indexOf(' ');
        if (space > 0) {
            String[] modifier = reforgeForms.get(fold(value.substring(0, space)));
            String base = value.substring(space + 1);
            String[] gender = itemGenders.get(fold(base));
            if (modifier != null && gender != null) {
                String translated = lookup(base, depth + 1);
                int form = switch(gender[0]) { case "f" -> 1; case "n" -> 2; case "p" -> 3; default -> 0; };
                if (translated != null) return modifier[form] + " " + translated;
            }
        }
        return null;
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
        String converted = lookup(core);
        // A complete, anchored template is more specific than a decorative label.
        // Otherwise "7331 HOTM Exp" uses the bare label and bypasses its number form.
        if (converted == null) {
            for (Rule rule : rules) {
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
        if (!recordUnknown || value.length()<4 || value.length()>400 || !value.matches("(?s).*[A-Za-z].*")) return;
        if (!misses.add(value) || misses.size()>15000) return;
        unknownWriter.execute(() -> {
            try {
                Files.createDirectories(unknownFile.getParent());
                Files.writeString(unknownFile, value.replace('\t',' ').replace('\r',' ').replace('\n',' ') + "\n", StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);
            } catch (IOException ignored) { }
        });
    }
}
