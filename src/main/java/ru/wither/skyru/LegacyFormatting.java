package ru.wither.skyru;

import java.util.regex.Pattern;

/** Most modern GUI text keeps colour in Style and has no legacy codes at all. */
final class LegacyFormatting {
    private static final Pattern CODES=Pattern.compile("(?i)§[0-9a-fk-or]");
    private LegacyFormatting() {}
    static String strip(String text){return text.indexOf('§')<0?text:CODES.matcher(text).replaceAll("");}
}
