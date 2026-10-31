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

    /** Runs lessons until the given item has been introduced. */
    void learnUntil(String itemId) {
        while (!engine.isIntroduced(itemId)) {
            StudySession s = engine.startLesson();
            assertNotNull(s, engine.lessonBlockReason());
            answerAll(s);
        }
    }

    /** Does every due review (sessions are capped, so possibly several). */
    void reviewAll() {
        StudySession s;
        while ((s = engine.startReview()) != null) answerAll(s);
    }

    @Test
    void dailyLimitIsOffByDefault() {
        assertFalse(engine.settings().dailyLimitEnabled);
        for (int i = 0; i < 4; i++) answerAll(engine.startLesson());
        assertTrue(progress.dailyNew > engine.settings().dailyNewLimit);
        assertNull(engine.lessonBlockReason());
    }

    @Test
    void dailyLimitAndGateBlockNewLessons() {
        engine.settings().dailyLimitEnabled = true;
        engine.settings().dailyNewLimit = 5;
        answerAll(engine.startLesson());
        assertNotNull(engine.lessonBlockReason());
        assertNull(engine.startLesson());
        now += Srs.DAY;
        assertNull(engine.lessonBlockReason());
    }

    @Test
    void courseTeachesHiraganaThenKatakanaThenWords() {
        List<Curriculum.Lesson> lessons = engine.curriculum().lessons();
        int stage = 0;
        for (Curriculum.Lesson l : lessons) {
            int s = switch (l.stage) {
                case "hiragana" -> 0;
                case "katakana" -> 1;
                default -> 2;
            };
            assertTrue(s >= stage, l.id + " comes too late or too early");
            stage = s;
        }
        assertEquals("hiragana", lessons.get(0).stage);
        assertEquals(2, stage);
    }

    @Test
    void reviewsComeDue() {
        learnUntil("v_ki");
        assertTrue(engine.kanaStageDone());
        assertTrue(engine.isLearned("v_ki"));
        assertEquals(0, engine.dueCount());
        now += Srs.DAY + 1;
        assertTrue(engine.dueCount() > 0);
        reviewAll();
        assertEquals(0, engine.dueCount());
    }

    @Test
    void kanjiComeLastThroughKnownWords() {
        learnUntil("v_ki");
        assertFalse(engine.kanjiStageOpen());
        assertNotNull(engine.kanjiBlockReason());
        assertNull(engine.startKanjiLesson());

        StudySession lesson;
        while ((lesson = engine.startLesson()) != null) answerAll(lesson);
        assertNull(engine.currentLesson());
        assertTrue(engine.kanjiStageOpen());
        // The words are known, but not yet for long enough.
        assertTrue(engine.kanjiReady().isEmpty());

        for (int day = 0; day < 4 && engine.kanjiReady().isEmpty(); day++) {
            now += 4 * Srs.DAY;
            reviewAll();
        }
        assertFalse(engine.kanjiReady().isEmpty());
        // Reviews never sneak kanji in; they have their own lesson.
        assertFalse(progress.cards.containsKey("v_ki#kanji"));
        int before = engine.kanjiReady().size();
        StudySession kanji = engine.startKanjiLesson();
        assertNotNull(kanji);
        assertInstanceOf(StudySession.Teach.class, kanji.current());
        answerAll(kanji);
        assertEquals(before - Math.min(before, engine.settings().maxKanjiPerSession), engine.kanjiReady().size());
        assertTrue(engine.counts().kanjiLearned() > 0);
    }

    @Test
    void encountersReviewDueWordsAndTeachNewOnes() {
        Curriculum.VocabItem tree = (Curriculum.VocabItem) engine.curriculum().item("v_ki");
        // Nothing to do in the world until the kana are learned.
        assertNull(engine.encounter(List.of(tree)));

        learnUntil("k_extended");
        assertTrue(engine.kanaStageDone());
        LearningEngine.Encounter discover = engine.encounter(List.of(tree));
        assertNotNull(discover);
        assertEquals(LearningEngine.EncounterKind.DISCOVER, discover.kind());
        StudySession s = engine.startEncounter(discover);
        assertEquals(StudySession.Mode.FIELD, s.mode());
        assertInstanceOf(StudySession.Teach.class, s.current());
        answerAll(s);
        assertTrue(engine.isLearned("v_ki"));
        // Learned and not due: nothing to ask.
        assertNull(engine.encounter(List.of(tree)));

        now += Srs.DAY + 1;
        LearningEngine.Encounter review = engine.encounter(List.of(tree));
        assertEquals(LearningEngine.EncounterKind.REVIEW, review.kind());
        assertTrue(answerAll(engine.startEncounter(review)) >= 2);
        assertNull(engine.encounter(List.of(tree)));
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
        learnUntil("v_ki");
        Quests.Quest q = Quests.generate(engine.learnedVocab(), new Random(1), null);
        assertNotNull(q);
        assertTrue(q.japanese().contains("あつめて"));
        assertTrue(q.count() >= 1 && q.count() <= 5);
    }
}
