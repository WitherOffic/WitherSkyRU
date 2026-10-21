package ru.wither.skyru;

import java.math.BigInteger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/** Closed numeric noun policy, mirrored by tools/translation_quality.py. */
public final class TranslationGrammar {
    private TranslationGrammar() {}
    private static final Pattern NUMBER = Pattern.compile("(?<![\\d.,])(\\d+(?:[ ,._]\\d+)*) (секунд(?:а|у|ы)?|монет(?:а|у|ы)?|предмет(?:ов|а)?|бит(?:ов|а)?|очк(?:о|а|ов)|блок(?:а|ов)?|уров(?:ень|ня|ней)|вредител(?:ь|я|ей)|раз(?:а)?|шахтёрск(?:ое задание|их задания|их заданий))(?![\\p{L}\\p{N}_])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static String numberCase(String prefix, String unit) {
        String[] clauses = prefix.toLowerCase(java.util.Locale.ROOT).replaceAll("[ +\\-]+$", "").split("[.!?;\\n]", -1);
        String clause = clauses[clauses.length - 1];
        if (unit.startsWith("монет")) {
            if (clause.matches("(?s).*(?:не хватает|недостаточно)\\s*$")) return "gen";
            if (clause.matches("(?s).*(?:получили|получаете|получите|получать|получает|получайте|нашли|найдено|заработали|потратили|потратьте|отдайте|даёт|дают|добавляет|стоит)\\s*$")) return "acc";
        } else if (unit.startsWith("секунд")) {
            if (clause.matches("(?s).*(?:в течение|не менее|не более|дольше|меньше|больше|до|после|около)\\s*$")) return "gen";
            if (clause.matches("(?s).*(?:на|за|через|каждую|каждые)\\s*$")) return "acc";
        } else if (unit.startsWith("блок") || unit.startsWith("уров") || unit.startsWith("очк")) {
            if (clause.matches("(?s).*(?:в радиусе|в пределах|не менее|не более|больше|меньше|до|около|от|из|с)\\s*$")) return "gen";
        }
        return "nom";
    }
    public static String polish(String value) {
        Matcher m = NUMBER.matcher(value);
        StringBuffer result = new StringBuffer();
        while (m.find()) {
            String raw = m.group(1), unit = m.group(2).toLowerCase(java.util.Locale.ROOT);
            String normalized = raw.replace(" ", "").replace("_", "");
            normalized = normalized.matches("\\d{1,3}(?:,\\d{3})+") ? normalized.replace(",", "") : normalized.replace(',', '.');
            boolean decimal = normalized.contains(".");
            int last = decimal ? 2 : new BigInteger(normalized).mod(BigInteger.valueOf(100)).intValue();
            int index = decimal ? 1 : last >= 11 && last <= 14 ? 2 : last % 10 == 1 ? 0 : last % 10 >= 2 && last % 10 <= 4 ? 1 : 2;
            String grammaticalCase = numberCase(value.substring(0, m.start()), unit);
            String[] forms;
            if (unit.startsWith("монет")) {
                forms = grammaticalCase.equals("gen") ? new String[]{"монеты", "монет", "монет"} :
                    grammaticalCase.equals("acc") ? new String[]{"монету", "монеты", "монет"} : new String[]{"монета", "монеты", "монет"};
            } else if (unit.startsWith("секунд")) {
                forms = grammaticalCase.equals("gen") ? new String[]{"секунды", "секунд", "секунд"} :
                    grammaticalCase.equals("acc") ? new String[]{"секунду", "секунды", "секунд"} : new String[]{"секунда", "секунды", "секунд"};
            } else if (unit.startsWith("предмет")) forms = new String[]{"предмет", "предмета", "предметов"};
            else if (unit.startsWith("бит")) forms = new String[]{"бит", "бита", "битов"};
            else if (unit.startsWith("очк")) forms = grammaticalCase.equals("gen") ? new String[]{"очка", "очков", "очков"} : new String[]{"очко", "очка", "очков"};
            else if (unit.startsWith("блок")) forms = grammaticalCase.equals("gen") ? new String[]{"блока", "блоков", "блоков"} : new String[]{"блок", "блока", "блоков"};
            else if (unit.startsWith("уров")) forms = grammaticalCase.equals("gen") ? new String[]{"уровня", "уровней", "уровней"} : new String[]{"уровень", "уровня", "уровней"};
            else if (unit.startsWith("вредител")) forms = new String[]{"вредитель", "вредителя", "вредителей"};
            else if (unit.startsWith("раз")) forms = new String[]{"раз", "раза", "раз"};
            else forms = new String[]{"шахтёрское задание", "шахтёрских задания", "шахтёрских заданий"};
            String word = decimal && unit.startsWith("монет") ? "монеты" : decimal && unit.startsWith("секунд") ? "секунды" :
                decimal && unit.startsWith("блок") ? "блока" : decimal && unit.startsWith("уров") ? "уровня" :
                decimal && unit.startsWith("очк") ? "очка" : forms[index];
            m.appendReplacement(result, Matcher.quoteReplacement(raw + " " + word));
        }
        m.appendTail(result);
        return result.toString();
    }
}
