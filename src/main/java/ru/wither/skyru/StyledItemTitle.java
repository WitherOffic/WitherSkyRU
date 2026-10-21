package ru.wither.skyru;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;

/** Recognised titles and authored requirement spans keep their exact effective Style. */
final class StyledItemTitle {
    private StyledItemTitle() {}
    static FormattedCharSequence translate(FormattedCharSequence source, OfflineDictionary db) {
        return translate(source,db,StyledRuns.collect(source,400));
    }
    /** Reuse current glyph runs, not a cached object or a second source traversal. */
    static FormattedCharSequence translate(FormattedCharSequence source, OfflineDictionary db, StyledRuns current) {
        String text=current.text;
        if (text.length() > 400) return null;
        var edits = db.displayEdits(text);
        if (edits.isEmpty()) return null;
        var reordered = db.reorderedItemTitle(text);
        if (reordered != null) {
            for (var part : reordered.parts()) if (!copied(text,part)&&!current.uniform(part.start(), part.end())) return source;
            List<FormattedCharSequence> moved = new ArrayList<>();
            current.appendOriginal(moved,0,reordered.start());
            for (var part : reordered.parts()){
                if(copied(text,part))current.appendOriginal(moved,part.start(),part.end());
                else moved.add(FormattedCharSequence.forward(part.replacement(), current.styleAt(part.start())));
            }
            current.appendOriginal(moved,reordered.end(),text.length());
            return FormattedCharSequence.composite(moved);
        }
        for (var edit : edits) {
            if(!current.uniform(edit.start(),edit.end()))
                return db.preserveRequirementOnStyleFailure(text) ? source : null;
        }
        List<FormattedCharSequence> result = new ArrayList<>();
        int offset = 0;
        for (var edit : edits) {
            current.appendOriginal(result,offset,edit.start());
            result.add(FormattedCharSequence.forward(edit.replacement(), current.styleAt(edit.start())));
            offset = edit.end();
        }
        current.appendOriginal(result,offset,text.length());
        return FormattedCharSequence.composite(result);
    }
    private static boolean copied(String text,OfflineDictionary.TitleEdit part){
        return part.end()-part.start()==part.replacement().length()&&text.regionMatches(part.start(),part.replacement(),0,part.replacement().length());
    }
    static Component translate(Component source, OfflineDictionary db) {
        return translate(source,db,source.getString());
    }
    static Component translate(Component source, OfflineDictionary db, String visible) {
        if (!literalOnly(source, 0)) return null;
        if (db.displayEdits(LegacyFormatting.strip(visible)).isEmpty()) return null;
        // Visit current effective styles directly, rather than allocating a String,
        // lambda and composite entry for every glyph of a title on every draw.
        FormattedCharSequence formatted = sink -> StringDecomposer.iterateFormatted(source, Style.EMPTY, sink);
        var next = translate(formatted, db);
        if (next == null) return null;
        if (next == formatted) return source;
        List<Component> runs = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        Style[] style = {null};
        Runnable flush = () -> {
            if (!text.isEmpty()) { runs.add(Component.literal(text.toString()).setStyle(style[0])); text.setLength(0); }
        };
        next.accept((i, s, cp) -> {
            if (!s.equals(style[0])) flush.run();
            style[0] = s; text.appendCodePoint(cp); return true;
        });
        flush.run();
        if (runs.size() == 1) return runs.getFirst();
        MutableComponent rebuilt = Component.empty().setStyle(source.getStyle());
        for (Component run : runs) rebuilt.append(run);
        return rebuilt;
    }
    private static boolean literalOnly(Component source, int depth) {
        if (depth > 18 || !(source.getContents() instanceof PlainTextContents)) return false;
        for (Component child : source.getSiblings()) if (!literalOnly(child, depth + 1)) return false;
        return true;
    }
}
