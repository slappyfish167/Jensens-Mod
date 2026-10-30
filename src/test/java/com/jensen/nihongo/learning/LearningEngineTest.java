package com.jensen.nihongo.learning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningEngineTest {
    long now;
    Progress progress;
    LearningEngine engine;

    @BeforeEach
    void setUp() throws IOException {
        now = 1_700_000_000_000L;
        progress = new Progress();
        engine = new LearningEngine(CurriculumTest.load(), progress, () -> now, new Random(42),
                new LearningEngine.Settings(), ZoneOffset.UTC);
    }

    /** Answers every question in the session correctly. */
    static int answerAll(StudySession s) {
        int asked = 0;
        while (!s.finished()) {
            Question q = s.question();
            if (q != null) {
                asked++;
                String answer = switch (q.type) {
                    case CHOICE -> q.accepted.get(0);
                    case ORDER -> q.answer;
                    default -> q.accepted.get(0);
                };
                assertTrue(s.submit(answer).passed(), q.cardId + " answer " + answer);
            }
            s.advance();
        }
        return asked;
    }

    @Test
    void firstLessonTeachesThenTests() {
        StudySession s = engine.startLesson();
        assertNotNull(s);
        assertInstanceOf(StudySession.Teach.class, s.current());
        int asked = answerAll(s);
        // 5 kana x 2 directions x 2 learning steps
        assertEquals(20, asked);
        for (String id : List.of("h_a", "h_i", "h_u", "h_e", "h_o")) assertTrue(engine.isLearned(id), id);
        assertEquals(5, progress.dailyNew);
        assertEquals(1, progress.streak);
    }

    @Test
    void wrongAnswersComeBack() {
        StudySession s = engine.startLesson();
        while (s.question() == null) s.advance();
        String card = s.question().cardId;
        assertFalse(s.submit("zzz").passed());
        s.advance();
        boolean seenAgain = false;
        while (!s.finished()) {
            if (s.question() != null && s.question().cardId.equals(card)) seenAgain = true;
            if (s.question() != null) s.submit(s.question().accepted.get(0));
            s.advance();
        }
        assertTrue(seenAgain);
    }

    @Test
    void dailyLimitAndGateBlockNewLessons() {
        engine.settings().dailyNewLimit = 5;
        answerAll(engine.startLesson());
        assertNotNull(engine.lessonBlockReason());
        assertNull(engine.startLesson());
        now += Srs.DAY;
        assertNull(engine.lessonBlockReason());
    }

    @Test
    void reviewsComeDueAndKanjiUnlockLater() {
        engine.settings().dailyNewLimit = 100;
        // Finish the first three lessons (two kana rows and the first words).
        for (int i = 0; i < 3; i++) answerAll(engine.startLesson());
        assertTrue(engine.isLearned("v_ki"));
        assertEquals(0, engine.dueCount());
        now += Srs.DAY + 1;
        assertTrue(engine.dueCount() > 0);
        answerAll(engine.startReview());
        assertEquals(0, engine.dueCount());

        // After a few successful days the kanji 木 is introduced through the known word き.
        for (int day = 0; day < 4; day++) {
            now += 4 * Srs.DAY;
            StudySession review = engine.startReview();
            if (review != null) answerAll(review);
        }
        assertTrue(progress.cards.containsKey("v_ki#kanji"));
    }

    @Test
    void progressRoundTripsThroughJson() {
        answerAll(engine.startLesson());
        java.io.StringWriter w = new java.io.StringWriter();
        progress.write(w);
        Progress back = Progress.read(new java.io.StringReader(w.toString()));
        assertEquals(progress.introduced, back.introduced);
        assertEquals(progress.cards.keySet(), back.cards.keySet());
        assertEquals(progress.cards.get("h_a#read").interval, back.cards.get("h_a#read").interval);
    }

    @Test
    void questsUseLearnedCollectibleWords() {
        engine.settings().dailyNewLimit = 100;
        for (int i = 0; i < 3; i++) answerAll(engine.startLesson());
        Quests.Quest q = Quests.generate(engine.learnedVocab(), new Random(1), null);
        assertNotNull(q);
        assertTrue(q.japanese().contains("あつめて"));
        assertTrue(q.count() >= 1 && q.count() <= 5);
    }
}
