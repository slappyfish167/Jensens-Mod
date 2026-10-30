package com.jensen.nihongo.learning;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kana utilities: an IME-style romaji to hiragana converter (so players without a Japanese
 * keyboard can still answer in Japanese), hiragana/katakana conversion, Hepburn romanisation
 * for display, and normalisation used when comparing answers.
 */
public final class Kana {
    private Kana() {}

    private static final Map<String, String> ROMAJI = new HashMap<>();
    private static final Map<String, String> TO_ROMAJI = new LinkedHashMap<>();
    private static final int MAX_KEY = 4;

    private static void r(String romaji, String kana) {
        ROMAJI.put(romaji, kana);
    }

    private static void k(String kana, String romaji) {
        TO_ROMAJI.putIfAbsent(kana, romaji);
    }

    static {
        String[][] base = {
                {"a", "あ"}, {"i", "い"}, {"u", "う"}, {"e", "え"}, {"o", "お"},
                {"ka", "か"}, {"ki", "き"}, {"ku", "く"}, {"ke", "け"}, {"ko", "こ"},
                {"ga", "が"}, {"gi", "ぎ"}, {"gu", "ぐ"}, {"ge", "げ"}, {"go", "ご"},
                {"sa", "さ"}, {"shi", "し"}, {"su", "す"}, {"se", "せ"}, {"so", "そ"},
                {"za", "ざ"}, {"ji", "じ"}, {"zu", "ず"}, {"ze", "ぜ"}, {"zo", "ぞ"},
                {"ta", "た"}, {"chi", "ち"}, {"tsu", "つ"}, {"te", "て"}, {"to", "と"},
                {"da", "だ"}, {"di", "ぢ"}, {"du", "づ"}, {"de", "で"}, {"do", "ど"},
                {"na", "な"}, {"ni", "に"}, {"nu", "ぬ"}, {"ne", "ね"}, {"no", "の"},
                {"ha", "は"}, {"hi", "ひ"}, {"fu", "ふ"}, {"he", "へ"}, {"ho", "ほ"},
                {"ba", "ば"}, {"bi", "び"}, {"bu", "ぶ"}, {"be", "べ"}, {"bo", "ぼ"},
                {"pa", "ぱ"}, {"pi", "ぴ"}, {"pu", "ぷ"}, {"pe", "ぺ"}, {"po", "ぽ"},
                {"ma", "ま"}, {"mi", "み"}, {"mu", "む"}, {"me", "め"}, {"mo", "も"},
                {"ya", "や"}, {"yu", "ゆ"}, {"yo", "よ"},
                {"ra", "ら"}, {"ri", "り"}, {"ru", "る"}, {"re", "れ"}, {"ro", "ろ"},
                {"wa", "わ"}, {"wo", "を"},
        };
        for (String[] p : base) {
            r(p[0], p[1]);
            k(p[1], p[0]);
        }
        // Kunrei / alternative spellings
        r("si", "し"); r("ti", "ち"); r("tu", "つ"); r("hu", "ふ"); r("zi", "じ");
        r("la", "ら"); r("li", "り"); r("lu", "る"); r("le", "れ"); r("lo", "ろ");
        r("ca", "か"); r("cu", "く"); r("co", "こ");
        r("ye", "いぇ"); r("wi", "うぃ"); r("we", "うぇ");
        r("vu", "ゔ"); r("va", "ゔぁ"); r("vi", "ゔぃ"); r("ve", "ゔぇ"); r("vo", "ゔぉ");
        r("fa", "ふぁ"); r("fi", "ふぃ"); r("fe", "ふぇ"); r("fo", "ふぉ");
        r("she", "しぇ"); r("je", "じぇ"); r("che", "ちぇ");
        r("thi", "てぃ"); r("dhi", "でぃ"); r("twu", "とぅ"); r("dwu", "どぅ");
        r("tsa", "つぁ"); r("tsi", "つぃ"); r("tse", "つぇ"); r("tso", "つぉ");
        k("ゔ", "vu"); k("ふぁ", "fa"); k("ふぃ", "fi"); k("ふぇ", "fe"); k("ふぉ", "fo");
        k("うぃ", "wi"); k("うぇ", "we"); k("うぉ", "wo"); k("しぇ", "she"); k("じぇ", "je");
        k("ちぇ", "che"); k("てぃ", "ti"); k("でぃ", "di"); k("とぅ", "tu"); k("どぅ", "du");
        k("いぇ", "ye"); k("ゔぁ", "va"); k("ゔぃ", "vi"); k("ゔぇ", "ve"); k("ゔぉ", "vo");
        k("つぁ", "tsa");

        // Yoon (combination) sounds
        String[][] yoon = {
                {"ky", "き"}, {"gy", "ぎ"}, {"ny", "に"}, {"hy", "ひ"}, {"by", "び"},
                {"py", "ぴ"}, {"my", "み"}, {"ry", "り"}, {"ly", "り"}, {"dy", "ぢ"},
        };
        String[][] small = {{"a", "ゃ"}, {"u", "ゅ"}, {"o", "ょ"}};
        for (String[] y : yoon) {
            for (String[] s : small) {
                String kana = y[1] + s[1];
                r(y[0] + s[0], kana);
                if (!y[0].equals("ly")) k(kana, y[0] + s[0]);
            }
        }
        String[][] special = {
                {"sha", "しゃ"}, {"shu", "しゅ"}, {"sho", "しょ"},
                {"sya", "しゃ"}, {"syu", "しゅ"}, {"syo", "しょ"},
                {"ja", "じゃ"}, {"ju", "じゅ"}, {"jo", "じょ"},
                {"jya", "じゃ"}, {"jyu", "じゅ"}, {"jyo", "じょ"},
                {"zya", "じゃ"}, {"zyu", "じゅ"}, {"zyo", "じょ"},
                {"cha", "ちゃ"}, {"chu", "ちゅ"}, {"cho", "ちょ"},
                {"cya", "ちゃ"}, {"cyu", "ちゅ"}, {"cyo", "ちょ"},
                {"tya", "ちゃ"}, {"tyu", "ちゅ"}, {"tyo", "ちょ"},
        };
        for (String[] s : special) {
            r(s[0], s[1]);
            k(s[1], s[0]);
        }
        // Small kana, typed with an x prefix like most IMEs
        String[][] smallKana = {
                {"xa", "ぁ"}, {"xi", "ぃ"}, {"xu", "ぅ"}, {"xe", "ぇ"}, {"xo", "ぉ"},
                {"xya", "ゃ"}, {"xyu", "ゅ"}, {"xyo", "ょ"}, {"xtu", "っ"}, {"xtsu", "っ"},
                {"xwa", "ゎ"}, {"xka", "ゕ"}, {"xke", "ゖ"},
        };
        for (String[] s : smallKana) r(s[0], s[1]);
        k("ぁ", "a"); k("ぃ", "i"); k("ぅ", "u"); k("ぇ", "e"); k("ぉ", "o");
        k("ゃ", "ya"); k("ゅ", "yu"); k("ょ", "yo"); k("ゎ", "wa");
        k("ん", "n");
        r("-", "ー");
    }

    public static boolean isHiragana(char c) {
        return c >= 'ぁ' && c <= 'ゖ';
    }

    public static boolean isKatakana(char c) {
        return c >= 'ァ' && c <= 'ヺ';
    }

    public static boolean isKana(char c) {
        return isHiragana(c) || isKatakana(c) || c == 'ー';
    }

    public static boolean isKanji(char c) {
        return (c >= '一' && c <= '鿿') || (c >= '㐀' && c <= '䶿') || c == '々';
    }

    /** True if the string contains any kana or kanji. */
    public static boolean containsJapanese(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (isKana(c) || isKanji(c)) return true;
        }
        return false;
    }

    public static String toHiragana(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'ァ' && c <= 'ヶ') sb.append((char) (c - 0x60));
            else sb.append(c);
        }
        return sb.toString();
    }

    public static String toKatakana(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'ぁ' && c <= 'ゖ') sb.append((char) (c + 0x60));
            else sb.append(c);
        }
        return sb.toString();
    }

    private static boolean isVowel(char c) {
        return c == 'a' || c == 'i' || c == 'u' || c == 'e' || c == 'o';
    }

    /**
     * Converts typed romaji to hiragana, IME style. Japanese characters already present are kept.
     *
     * @param finish when true a trailing lone "n" becomes ん (used when checking an answer);
     *               when false it is left as "n" so live previews don't jump while typing.
     */
    public static String romajiToHiragana(String input, boolean finish) {
        String s = input.toLowerCase(java.util.Locale.ROOT);
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            // n / nn / n'
            if (c == 'n') {
                char next = i + 1 < s.length() ? s.charAt(i + 1) : 0;
                if (next == 'n') {
                    // "nn" -> ん, but "nna" means ん + な
                    char after = i + 2 < s.length() ? s.charAt(i + 2) : 0;
                    if (after != 0 && (isVowel(after) || after == 'y')) {
                        out.append('ん');
                        i += 1;
                    } else {
                        out.append('ん');
                        i += 2;
                    }
                    continue;
                }
                if (next == '\'') {
                    out.append('ん');
                    i += 2;
                    continue;
                }
                if (next == 0) {
                    out.append(finish ? "ん" : "n");
                    i++;
                    continue;
                }
                if (!isVowel(next) && next != 'y') {
                    out.append('ん');
                    i++;
                    continue;
                }
            }
            // Sokuon: doubled consonant -> っ (also "tch")
            if (i + 1 < s.length() && c >= 'a' && c <= 'z' && !isVowel(c) && c != 'n') {
                char next = s.charAt(i + 1);
                if (next == c || (c == 't' && next == 'c')) {
                    out.append('っ');
                    i++;
                    continue;
                }
            }
            boolean matched = false;
            for (int len = Math.min(MAX_KEY, s.length() - i); len >= 1; len--) {
                String key = s.substring(i, i + len);
                String kana = ROMAJI.get(key);
                if (kana != null) {
                    out.append(kana);
                    i += len;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** Hepburn romanisation of a kana string (hiragana or katakana), for display. */
    public static String toRomaji(String kanaInput) {
        String s = toHiragana(kanaInput);
        StringBuilder out = new StringBuilder();
        boolean doubleNext = false;
        for (int i = 0; i < s.length(); ) {
            char c = s.charAt(i);
            if (c == 'っ') {
                doubleNext = true;
                i++;
                continue;
            }
            if (c == 'ー') {
                char last = out.length() > 0 ? out.charAt(out.length() - 1) : 0;
                out.append(isVowel(last) ? last : '-');
                i++;
                continue;
            }
            if (c == 'ん') {
                char next = i + 1 < s.length() ? s.charAt(i + 1) : 0;
                out.append('n');
                String nextRomaji = next != 0 ? TO_ROMAJI.get(String.valueOf(next)) : null;
                if (nextRomaji != null && (isVowel(nextRomaji.charAt(0)) || nextRomaji.charAt(0) == 'y')) {
                    out.append('\'');
                }
                i++;
                continue;
            }
            String romaji = null;
            int used = 1;
            if (i + 1 < s.length()) {
                romaji = TO_ROMAJI.get(s.substring(i, i + 2));
                if (romaji != null) used = 2;
            }
            if (romaji == null) romaji = TO_ROMAJI.get(String.valueOf(c));
            if (romaji == null) {
                out.append(c);
                i++;
                doubleNext = false;
                continue;
            }
            if (doubleNext) {
                out.append(romaji.startsWith("ch") ? 't' : romaji.charAt(0));
                doubleNext = false;
            }
            out.append(romaji);
            i += used;
        }
        return out.toString();
    }

    private static char vowelOf(char kana) {
        String romaji = TO_ROMAJI.get(String.valueOf(kana));
        if (romaji == null || romaji.isEmpty()) return 0;
        char last = romaji.charAt(romaji.length() - 1);
        return isVowel(last) ? last : 0;
    }

    private static final String VOWEL_KANA = "あいうえお";

    /**
     * Normalises Japanese text for comparison: katakana becomes hiragana, the long vowel mark is
     * expanded to the vowel it lengthens, and whitespace/punctuation is removed.
     */
    public static String normalize(String s) {
        String h = toHiragana(s.trim());
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < h.length(); i++) {
            char c = h.charAt(i);
            if (Character.isWhitespace(c) || isPunctuation(c)) continue;
            if (c == 'ー' && out.length() > 0) {
                char v = vowelOf(out.charAt(out.length() - 1));
                if (v != 0) {
                    out.append(VOWEL_KANA.charAt("aiueo".indexOf(v)));
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    /**
     * A looser comparison form: particles written は/へ/を are compared by sound (わ/え/お).
     * Used so "konnichiwa" is accepted for こんにちは.
     */
    public static String phonetic(String normalized) {
        return normalized.replace('は', 'わ').replace('へ', 'え').replace('を', 'お');
    }

    public static boolean isPunctuation(char c) {
        return "。、．，！？!?.,・「」『』（）()〜~…:;：；\"'’".indexOf(c) >= 0 || c == '　';
    }
}
