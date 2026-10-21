import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import ru.wither.skyru.OfflineDictionary;

/** Core dictionary only; emits line-index flags, never a private-text dump. */
public class AuditDisplayCurrent {
    public static void main(String[] args) throws Exception {
        var db = new OfflineDictionary(Path.of(args[1]));
        db.setRecordUnknown(false);
        var lines = Files.readAllLines(Path.of(args[0]), StandardCharsets.UTF_8);
        StringBuilder out = new StringBuilder();
        int changed = 0;
        for (String line : lines) {
            boolean translated = !db.translate(line).equals(line);
            out.append(translated ? '1' : '0');
            if (translated) changed++;
        }
        Files.writeString(Path.of(args[2]), out, StandardCharsets.UTF_8);
        System.out.println("PASS: current dictionary audit; physical lines="+lines.size()+"; changed="+changed+"; entries="+db.phraseCount()+"; templates="+db.patternCount());
    }
}
