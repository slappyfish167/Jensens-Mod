package com.jensen.nihongo.client;

import com.jensen.nihongo.learning.Curriculum;
import com.jensen.nihongo.learning.LearningEngine;
import com.jensen.nihongo.learning.McIndex;

/** How a known word is shown in the world (tooltips and look-at labels). */
public final class Labels {
    private Labels() {}

    /** The best word the player has already studied for this thing, or null. */
    public static Curriculum.VocabItem bestKnown(McIndex.Type type, String id) {
        LearningEngine engine = Learner.engine();
        Curriculum.VocabItem best = null;
        double bestStrength = 0;
        for (Curriculum.VocabItem v : Learner.index().lookup(type, id)) {
            double s = engine.strength(v.id);
            if (s > bestStrength) {
                best = v;
                bestStrength = s;
            }
        }
        return best;
    }

    /** e.g. "木（き）" once the kanji has been learned, otherwise "き", plus romaji for weak words. */
    public static String format(Curriculum.VocabItem v) {
        LearningEngine engine = Learner.engine();
        boolean kanjiKnown = engine.progress().cards.containsKey(LearningEngine.cardId(v.id, "kanji"));
        String text = kanjiKnown ? v.kanji + "（" + v.kana + "）" : v.kana;
        ClientConfig.RomajiMode mode;
        try {
            mode = ClientConfig.LABEL_ROMAJI.get();
        } catch (IllegalStateException e) {
            mode = ClientConfig.RomajiMode.WEAK_WORDS;
        }
        boolean romaji = mode == ClientConfig.RomajiMode.ALWAYS
                || (mode == ClientConfig.RomajiMode.WEAK_WORDS && engine.strength(v.id) < 0.5);
        return romaji ? text + "  " + v.romaji() : text;
    }
}
