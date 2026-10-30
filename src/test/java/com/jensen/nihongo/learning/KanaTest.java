package com.jensen.nihongo.learning;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KanaTest {
    private static String h(String romaji) {
        return Kana.romajiToHiragana(romaji, true);
    }

    @Test
    void convertsBasicRomaji() {
        assertEquals("こんにちわ", h("konnichiwa"));
        assertEquals("おんな", h("onna"));
        assertEquals("しんぶん", h("shinbun"));
        assertEquals("かんい", h("kan'i"));
        assertEquals("ほん", h("hon"));
        assertEquals("ほん", h("honn"));
        assertEquals("し", h("si"));
        assertEquals("つち", h("tsuchi"));
        assertEquals("ふね", h("hune"));
    }

    @Test
    void handlesDoubleConsonantsAndCombos() {
        assertEquals("きって", h("kitte"));
        assertEquals("まっちゃ", h("matcha"));
        assertEquals("いっしょ", h("issho"));
        assertEquals("きょう", h("kyou"));
        assertEquals("じゃがいも", h("jagaimo"));
        assertEquals("けーき", h("ke-ki"));
    }

    @Test
    void trailingNOnlyBecomesKanaWhenFinished() {
        assertEquals("ほn", Kana.romajiToHiragana("hon", false));
        assertEquals("ほん", Kana.romajiToHiragana("hon", true));
    }

    @Test
    void passesJapaneseThrough() {
        assertEquals("ねこ", h("ねこ"));
        assertEquals("木", h("木"));
    }

    @Test
    void romanises() {
        assertEquals("kitte", Kana.toRomaji("きって"));
        assertEquals("shinbun", Kana.toRomaji("しんぶん"));
        assertEquals("kyou", Kana.toRomaji("きょう"));
        assertEquals("keeki", Kana.toRomaji("ケーキ"));
        assertEquals("kan'i", Kana.toRomaji("かんい"));
        assertEquals("matcha", Kana.toRomaji("まっちゃ"));
    }

    @Test
    void normalisesForComparison() {
        assertEquals(Kana.normalize("けえき"), Kana.normalize("ケーキ"));
        assertEquals("ねこ", Kana.normalize(" ネコ。"));
        assertTrue(Kana.containsJapanese("abcあ"));
        assertFalse(Kana.containsJapanese("abc"));
    }
}
