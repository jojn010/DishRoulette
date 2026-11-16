package com.example.dishroulette;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure parsing for English/Arabic steps and durations; never changes the displayed recipe.
 */
final class CookingInstructions {
    private CookingInstructions() {
    }

    static List<String> steps(RecipeCatalog.Recipe recipe) {
        List<String> result = new ArrayList<>();
        for (String s : recipe.method.split("\\r?\\n\\s*\\r?\\n|\\r?\\n(?=[\\t ]*\\p{Nd}+[.)]\\s)")) {
            String clean = s.trim().replaceFirst("^\\p{Nd}+[.)]\\s*", "");
            if (!clean.isEmpty())
                result.add(clean);
        }
        return result;
    }

    private static final String DURATION_NUMBER = "[0-9٠-٩۰-۹]+(?:[.٫][0-9٠-٩۰-۹]+)?";
    private static final String DURATION_RANGE =
        "(" + DURATION_NUMBER + ")(?:\\s*(?:[-–]|إلى|to)\\s*(" + DURATION_NUMBER + "))?";
    private static final String MINUTE_UNIT = "minutes?|mins?|دقيقة|دقائق|دقيقتين|دقيقتان";
    private static final Pattern ARABIC_VOWEL_MARKS = Pattern.compile("[\u064B-\u0652\u0670]");
    private static final Pattern DURATION = Pattern.compile("(?i)(?<![\\p{L}\\p{N}.,٫])(?:"
        + DURATION_RANGE + "\\s*(hours?|hrs?|ساعة|ساعات|ساعتين|ساعتان|" + MINUTE_UNIT + ")"
        + "|(ساعة|ساعتين|ساعتان|دقيقتين|دقيقتان))(?![\\p{L}])");
    private static final Pattern ADJACENT_MINUTES = Pattern.compile("(?i)^(?:\\s+(?:and\\s+)?|\\s*و\\s*)(?:"
        + DURATION_RANGE + "\\s*(?:" + MINUTE_UNIT + ")|(دقيقتين|دقيقتان))(?![\\p{L}])");

    static int suggestedMinutes(String step) {
        // Match vocalized and unvocalized durations without changing the displayed step.
        String durationText = ARABIC_VOWEL_MARKS.matcher(step).replaceAll("");
        // Keep the first duration; only adjacent minutes extend an hour duration.
        Matcher m = DURATION.matcher(durationText);
        if (!m.find())
            return 0;
        String unit = m.group(3) == null ? m.group(4) : m.group(3);
        boolean hours = unit.toLowerCase(Locale.ROOT).startsWith("h") || unit.startsWith("سا");
        double amount = m.group(1) == null ? (unit.startsWith("ساعت") || unit.startsWith("دقيقت") ? 2 : 1)
            : durationNumber(m.group(2) == null ? m.group(1) : m.group(2));
        double minutes = amount * (hours ? 60 : 1);
        if (hours) {
            Matcher extra = ADJACENT_MINUTES.matcher(durationText.substring(m.end()));
            if (extra.find())
                minutes += extra.group(1) == null ? 2 : durationNumber(
                    extra.group(2) == null ? extra.group(1) : extra.group(2));
        }
        return Math.max(1, (int) Math.min(240, Math.ceil(minutes)));
    }

    private static double durationNumber(String value) {
        StringBuilder normalized = new StringBuilder();
        for (char c : value.toCharArray())
            normalized.append(c == '.' || c == '٫' ? '.' : (char) ('0' + Character.digit(c, 10)));
        return Double.parseDouble(normalized.toString());
    }
}
