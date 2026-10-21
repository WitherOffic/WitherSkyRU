package ru.wither.skyru;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/** Explicit local name suggestions. Never sends, types, or guesses an English query. */
public final class SignSearchNames {
    private record Name(String english, String russian) {}
    private final List<Name> names;
    public SignSearchNames(Collection<String> originals, Function<String,String> translate) {
        List<Name> entries = new ArrayList<>();
        for (String english : new TreeSet<>(originals)) {
            if (english == null || english.isBlank() || english.length() > 100
                || english.indexOf('\n') >= 0 || english.startsWith("/")) continue;
            String russian = normalize(translate.apply(english));
            if (RussianSearchAliases.russianQuery(russian)) entries.add(new Name(english, russian));
        }
        names = List.copyOf(entries);
    }
    public static SignSearchNames load(OfflineDictionary dictionary) throws IOException {
        InputStream input = SignSearchNames.class.getResourceAsStream("/data/witherskyru/search-item-names.txt");
        if (input == null) throw new IOException("Missing original item-name snapshot");
        try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return new SignSearchNames(reader.lines().toList(), dictionary::translateForSearch);
        }
    }
    private static String normalize(String text) {
        return text == null ? "" : text.strip().toLowerCase(Locale.ROOT).replace('ё', 'е').replaceAll("\\s+", " ");
    }
    public List<String> suggestions(String query, int limit) {
        String normalized = normalize(query);
        if (limit < 1 || !RussianSearchAliases.russianQuery(normalized) || normalized.length() > 100
            || !normalized.matches("[\\p{L}\\p{N} '’()™.\\-]+")) return List.of();
        String[] words = normalized.split(" ");
        LinkedHashSet<String> exact = new LinkedHashSet<>(), partial = new LinkedHashSet<>();
        for (Name name : names) {
            if (name.russian.equals(normalized)) exact.add(name.english);
            else if (Arrays.stream(words).allMatch(name.russian::contains)) partial.add(name.english);
        }
        // A specific full translated name wins; ambiguity is displayed, never resolved silently.
        return (exact.isEmpty() ? partial : exact).stream().limit(Math.min(limit, 5)).toList();
    }
}
