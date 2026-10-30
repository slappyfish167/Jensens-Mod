package com.jensen.nihongo.learning;

import java.util.List;
import java.util.Locale;

/**
 * Grades typed and spoken answers.
 * Readings must be exact (after normalisation), as a wrong kana is a different word.
 * English meanings tolerate small typos, since a spelling slip says nothing about memory.
 */
public final class AnswerChecker {
    private AnswerChecker() {}

    public enum Verdict {
        CORRECT,
        /** Correct, but with a typo. */
        CLOSE,
        WRONG,
        /** Answered in the wrong script/language; the player gets another try. */
        RETRY
    }

    public record Result(Verdict verdict, String note) {
        public boolean passed() {
            return verdict == Verdict.CORRECT || verdict == Verdict.CLOSE;
        }
    }

    /** Checks a Japanese answer typed as romaji or kana against the accepted forms. */
    public static Result checkJapanese(String input, List<String> accepted) {
        String typed = Kana.normalize(Kana.romajiToHiragana(input.trim(), true));
        if (typed.isEmpty()) return new Result(Verdict.RETRY, "Type an answer first.");
        for (String a : accepted) {
            String target = Kana.normalize(Kana.romajiToHiragana(a, true));
            if (typed.equals(target)) return new Result(Verdict.CORRECT, "");
            if (target.length() >= 3 && Kana.phonetic(typed).equals(Kana.phonetic(target))) {
                return new Result(Verdict.CORRECT, "Spelling note: the particle is written " + particleHint(target) + ".");
            }
        }
        boolean hasLatin = false;
        for (int i = 0; i < typed.length(); i++) {
            char c = typed.charAt(i);
            if (c >= 'a' && c <= 'z') hasLatin = true;
        }
        if (hasLatin) {
            return new Result(Verdict.WRONG, "Some letters didn't form kana: " + typed);
        }
        return new Result(Verdict.WRONG, "");
    }

    private static String particleHint(String target) {
        if (target.contains("は")) return "は (pronounced wa)";
        if (target.contains("を")) return "を (pronounced o)";
        if (target.contains("へ")) return "へ (pronounced e)";
        return target;
    }

    /** Checks an English meaning. */
    public static Result checkMeaning(String input, List<String> meanings) {
        String typed = cleanEnglish(input);
        if (typed.isEmpty()) return new Result(Verdict.RETRY, "Type an answer first.");
        if (Kana.containsJapanese(input)) {
            return new Result(Verdict.RETRY, "This one wants the English meaning.");
        }
        Verdict best = Verdict.WRONG;
        for (String m : meanings) {
            String target = cleanEnglish(m);
            String noParens = cleanEnglish(m.replaceAll("\\(.*?\\)", ""));
            if (typed.equals(target) || typed.equals(noParens)) return new Result(Verdict.CORRECT, "");
            int tolerance = noParens.length() <= 3 ? 0 : noParens.length() <= 6 ? 1 : 2;
            if (levenshtein(typed, noParens) <= tolerance || levenshtein(typed, target) <= tolerance) {
                best = Verdict.CLOSE;
            }
        }
        if (best == Verdict.CLOSE) return new Result(Verdict.CLOSE, "Close enough - watch the spelling.");
        return new Result(Verdict.WRONG, "");
    }

    static String cleanEnglish(String s) {
        String t = s.toLowerCase(Locale.ROOT).trim()
                .replaceAll("[^a-z0-9 '()-]", " ")
                .replaceAll("\\s+", " ").trim();
        for (String prefix : new String[]{"to ", "a ", "an ", "the "}) {
            if (t.startsWith(prefix)) t = t.substring(prefix.length());
        }
        return t.trim();
    }

    public static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }

    /**
     * Grades a speech-recognition transcript. Recognisers output words in kanji or kana and may
     * add filler words, so a match anywhere in any alternative transcript counts.
     */
    public static Result checkSpoken(List<String> transcripts, List<String> accepted) {
        if (transcripts.isEmpty() || transcripts.stream().allMatch(t -> Kana.normalize(t).isEmpty())) {
            return new Result(Verdict.RETRY, "I didn't hear anything. Try again a little louder.");
        }
        for (String t : transcripts) {
            String heard = Kana.normalize(t);
            if (heard.isEmpty()) continue;
            for (String a : accepted) {
                String target = Kana.normalize(a);
                if (target.isEmpty()) continue;
                if (heard.equals(target) || heard.contains(target)
                        || Kana.phonetic(heard).contains(Kana.phonetic(target))) {
                    return new Result(Verdict.CORRECT, "");
                }
            }
        }
        return new Result(Verdict.WRONG, "Heard: " + transcripts.get(0));
    }
}
