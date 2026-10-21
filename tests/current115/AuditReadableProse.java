import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import ru.wither.skyru.*;

/** Candidate screening only, not automatic edits or a quality/completeness certificate. */
public class AuditReadableProse {
    private static final Pattern RUSSIAN = Pattern.compile("[А-Яа-яЁё]");
    private static final Pattern CONNECTOR = Pattern.compile("\\b(?:like|them|into|which|while|unless|because|either|or|and|to|of|the|your|with)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPEATED = Pattern.compile("(?iu)\\b(или|и|в|на|для|от|с|до|из|что|чтобы)\\s+\\1\\b");
    private static final Set<String> seen = new HashSet<>();
    private static final JsonArray candidates = new JsonArray();
    private static void review(OfflineDictionary db, String source, String origin) {
        String target = db.translateForSearch(source);
        boolean repeated = REPEATED.matcher(target).find();
        boolean mixed = RUSSIAN.matcher(target).find() && CONNECTOR.matcher(target).find();
        if ((!mixed && !repeated) || !seen.add(source)) return;
        JsonObject row = new JsonObject(); row.addProperty("source", source); row.addProperty("translation", target);
        row.addProperty("origin", origin); row.addProperty("mixed_connector_candidate", mixed);
        row.addProperty("repeated_connector_candidate", repeated); candidates.add(row);
    }
    public static void main(String[] args) throws Exception {
        Path project = Path.of(args[0]), output = Path.of(args[1]);
        var db = new OfflineDictionary(output.getParent().resolve("isolated-readability-config")); db.setRecordUnknown(false);
        int corpus = 0, paragraphs = 0;
        for (String line : Files.readAllLines(project.resolve("translation-source/corpus.jsonl"))) {
            review(db, JsonParser.parseString(line).getAsJsonObject().get("source").getAsString(), "frozen corpus"); corpus++;
        }
        var items = JsonParser.parseString(Files.readString(project.resolve("translation-source/item-context.json"))).getAsJsonArray();
        for (var value : items) {
            var item = value.getAsJsonObject(); List<String> rows = new ArrayList<>();
            for (var line : item.getAsJsonArray("lore")) rows.add(line.getAsString().replaceAll("(?i)§[0-9a-fk-or]", ""));
            if (!rows.isEmpty()) { review(db, String.join("\n", rows), "item-context:" + item.get("id").getAsString()); paragraphs++; }
        }
        JsonObject result = new JsonObject(); result.addProperty("version", args[2]);
        result.addProperty("corpus_sources_checked", corpus); result.addProperty("item_lore_blocks_checked", paragraphs);
        result.addProperty("candidate_sources_unique", candidates.size()); result.add("candidates", candidates);
        result.addProperty("automatic_changes", false); result.addProperty("all_candidates_confirmed_errors", false);
        result.addProperty("full_manual_proofread", false); result.addProperty("live_server_complete", false);
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(result) + "\n");
        System.out.println("PROSE SCREEN: corpus=" + corpus + " lore blocks=" + paragraphs + " distinct candidates=" + candidates.size());
    }
}
