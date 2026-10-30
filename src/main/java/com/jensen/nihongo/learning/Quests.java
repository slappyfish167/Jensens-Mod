package com.jensen.nihongo.learning;

import com.jensen.nihongo.learning.Curriculum.VocabItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Collection quests written in Japanese. Understanding the request is the point: it turns words
 * from flashcards into meaningful, task-based input.
 */
public final class Quests {
    private Quests() {}

    public record Quest(String vocabId, String itemId, int count, String japanese, String romaji, String english) {}

    private static final String[] COUNTER_READINGS = {"", "いっこ", "にこ", "さんこ", "よんこ", "ごこ"};

    public static Quest generate(List<VocabItem> learned, Random rng, String avoidVocabId) {
        List<VocabItem> pool = new ArrayList<>();
        for (VocabItem v : learned) {
            if (v.collect != null && !v.collect.isEmpty() && !v.id.equals(avoidVocabId)) pool.add(v);
        }
        if (pool.isEmpty()) return null;
        VocabItem v = pool.get(rng.nextInt(pool.size()));
        int count = 1 + rng.nextInt(5);
        String jp = v.kana + " を " + COUNTER_READINGS[count] + " あつめて ください";
        String english = "Please collect " + count + " " + v.meanings.get(0) + (count > 1 ? " (x" + count + ")" : "");
        return new Quest(v.id, v.collect, count, jp, Kana.toRomaji(jp), english);
    }
}
