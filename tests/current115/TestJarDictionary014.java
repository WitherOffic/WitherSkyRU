import java.nio.file.Path;
import ru.wither.skyru.OfflineDictionary;

/** Standalone dictionary check. Does not start Minecraft or initialize Fabric. */
public class TestJarDictionary014 {
    public static void main(String[] args) throws Exception {
        OfflineDictionary db = new OfflineDictionary(Path.of(args[0]));
        db.setRecordUnknown(false);
        // Current editorial catalogue; frozen 0.14.2 tests remain in its source ZIP.
        var expected = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(Path.of(args[1]).resolve("coverage.json"))).getAsJsonObject();
        if (db.phraseCount() != expected.get("runtime_dictionary_entries").getAsInt() || db.patternCount() != expected.get("runtime_templates").getAsInt())
            throw new AssertionError("Dictionary count mismatch: phrases=" + db.phraseCount() + "; templates=" + db.patternCount());
        check(db, "Dwarven Mines", "Дварфские шахты");
        check(db, "Your Bags", "Ваши сумки");
        check(db, "Purse: 245,449", "Кошелёк: 245,449");
        check(db, "https://hypixel.net/skyblock", "https://hypixel.net/skyblock");
        check(db, "/warp hub", "/warp hub");
        check(db, "The totally unknown item ZX-9921 must not be invented.",
                  "The totally unknown item ZX-9921 must not be invented.");
        System.out.println("PASS: 7 checks; phrases=" + db.phraseCount() + "; templates=" + db.patternCount());
    }
    private static void check(OfflineDictionary db, String input, String expected) {
        String actual = db.translate(input);
        if (!actual.equals(expected)) throw new AssertionError(input + ": " + actual);
    }
}
