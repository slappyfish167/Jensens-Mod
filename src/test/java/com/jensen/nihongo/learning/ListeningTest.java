package com.jensen.nihongo.learning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListeningTest {
    long now;
    Progress progress;
    LearningEngine engine;

    @BeforeEach
    void setUp() throws IOException {
        now = 1_700_000_000_000L;
        progress = new Progress();
        engine = new LearningEngine(CurriculumTest.load(), progress, () -> now, new Random(7),
                new LearningEngine.Settings(), ZoneOffset.UTC);
        engine.settings().listeningAvailable = true;
    }

    void learnUntil(String itemId) {
        while (!engine.isIntroduced(itemId)) {
            StudySession s = engine.startLesson();
            assertNotNull(s, engine.lessonBlockReason());
            LearningEngineTest.answerAll(s);
        }
    }

    void hear(String itemId, String voice, int times) {
        for (int i = 0; i < times; i++) engine.recordHeard(itemId, voice);
    }

    @Test
    void listeningIsTrackedApartFromReading() {
        learnUntil("v_ki");
        assertTrue(engine.isLearned("v_ki"));
        // Known by eye, but not heard yet: nothing to check by ear.
        assertNull(engine.listeningCheck());

        hear("v_ki", "a", engine.settings().listenUnlockHeard);
        String card = engine.listeningCheck();
        assertEquals("v_ki#listen", card);
        assertTrue(progress.cards.containsKey(card));

        // A new listening card doesn't make the word any less learned for reading.
        assertTrue(engine.isLearned("v_ki"));
        assertEquals(0, engine.counts().listening());

        StudySession s = engine.startListenCheck(card);
        assertFalse(s.submit("zzz").passed());
        s.advance();
        assertTrue(engine.isLearned("v_ki"), "failing by ear must not reset the reading cards");
    }

    @Test
    void listeningCardsNeedAudio() {
        learnUntil("v_ki");
        hear("v_ki", null, 5);
        String card = engine.listeningCheck();
        assertNotNull(card);
        int withAudio = engine.dueCount();

        engine.settings().listeningAvailable = false;
        assertNull(engine.listeningCheck());
        assertEquals(withAudio - 1, engine.dueCount());
        assertNull(engine.startListenCheck(card));
        assertNotNull(engine.listeningBlockReason());
        if (engine.dueCount() == 0) assertNull(engine.startReview());
        else assertFalse(engine.startReview().question().listening);
    }

    @Test
    void listeningQuestionsDontShowTheJapanese() {
        learnUntil("v_ki");
        hear("v_ki", null, 3);
        String card = engine.listeningCheck();
        Curriculum.VocabItem ki = (Curriculum.VocabItem) engine.curriculum().item("v_ki");
        Set<Question.Type> types = new HashSet<>();
        for (int i = 0; i < 40; i++) {
            Question q = engine.startListenCheck(card).question();
            types.add(q.type);
            assertTrue(q.listening);
            assertFalse(q.promptJapanese);
            assertFalse(q.prompt.contains(ki.kana));
            assertFalse(q.subPrompt.contains(ki.kana));
            assertEquals(ki.kana, q.speak);
            if (q.type == Question.Type.CHOICE) {
                assertEquals(4, q.options.size());
                assertTrue(q.options.containsAll(q.accepted));
                assertEquals(4, new HashSet<>(q.options).size());
                for (String o : q.options) assertFalse(o.contains(ki.kana), "options are meanings, not Japanese");
            } else {
                assertEquals(Question.Type.TYPE_JAPANESE, q.type);
                assertTrue(q.accepted.contains(ki.kana));
            }
        }
        // Both "which word did you hear?" and "what was just said?" come up.
        assertEquals(Set.of(Question.Type.CHOICE, Question.Type.TYPE_JAPANESE), types);
    }

    @Test
    void phrasesAreCheckedByMeaning() {
        String grammar = null;
        while (grammar == null) {
            StudySession s = engine.startLesson();
            assertNotNull(s, engine.lessonBlockReason());
            LearningEngineTest.answerAll(s);
            for (String id : progress.introduced) {
                if (engine.curriculum().item(id) instanceof Curriculum.GrammarPoint && engine.isLearned(id)) grammar = id;
            }
        }
        // New listening cards start with what has been heard most.
        hear(grammar, null, 2);
        StudySession s = engine.startListening();
        assertNotNull(s);
        boolean sawPhrase = false;
        while (!s.finished()) {
            Question q = s.question();
            if (q != null && q.itemId.equals(grammar)) {
                sawPhrase = true;
                assertEquals(Question.Type.CHOICE, q.type);
                assertTrue(Kana.containsJapanese(q.speak));
                assertTrue(q.options.size() >= 2);
            }
            if (q != null) assertTrue(s.submit(q.accepted.get(0)).passed());
            s.advance();
        }
        assertTrue(sawPhrase);
    }

    @Test
    void listeningCheckIsOneQuestion() {
        learnUntil("v_ki");
        hear("v_ki", null, 3);
        StudySession s = engine.startListenCheck(engine.listeningCheck());
        assertEquals(1, s.remaining());
        s.giveUp();
        s.advance();
        assertTrue(s.finished(), "a missed check comes back later, not straight away");
    }

    @Test
    void listeningPracticeBuildsListeningSkill() {
        learnUntil("v_ki");
        assertNull(engine.listeningBlockReason());
        StudySession s = engine.startListening();
        assertNotNull(s);
        assertEquals(StudySession.Mode.LISTENING, s.mode());
        LearningEngineTest.answerAll(s);
        assertTrue(engine.counts().listening() > 0);
        assertTrue(engine.counts().listening() <= engine.settings().maxNewListenPerSession);
    }

    @Test
    void resultsAreTalliedPerVoice() {
        learnUntil("v_ki");
        hear("v_ki", null, 3);
        StudySession s = engine.startListenCheck(engine.listeningCheck());
        s.question().voice = "ichiro";
        s.submit(s.question().accepted.get(0));
        s.advance();
        Progress.Tally t = progress.listenByVoice.get("ichiro");
        assertEquals(1, t.total);
        assertEquals(1, t.right);
    }

    @Test
    void slowReplayCountsAsHard() {
        learnUntil("v_ki");
        hear("v_ki", null, 3);
        String card = engine.listeningCheck();
        StudySession s = engine.startListenCheck(card);
        s.usedSlowReplay();
        assertTrue(s.submit(s.question().accepted.get(0)).passed());
        s.advance();
        // "Hard" keeps a new card on its first learning step; "good" would have moved it on.
        assertEquals(0, progress.cards.get(card).step);
    }

    @Test
    void primaryVoiceFirstThenOthers() {
        List<String> voices = List.of("primary", "b", "c");
        engine.settings().voiceVariety = 1.0;
        assertEquals("primary", engine.pickVoice("v_ki", voices, false), "a new word starts in one clear voice");
        hear("v_ki", "primary", engine.settings().anchorExposures);

        String second = engine.pickVoice("v_ki", voices, false);
        assertNotEquals("primary", second);
        hear("v_ki", second, 1);
        String third = engine.pickVoice("v_ki", voices, false);
        assertNotEquals(second, third, "the voice heard least for this word goes next");
        assertNotEquals("primary", third);

        // Checks rotate through every voice, the primary included.
        hear("v_ki", third, 5);
        hear("v_ki", second, 5);
        assertEquals("primary", engine.pickVoice("v_ki", voices, true));

        engine.settings().voiceVariety = 0;
        for (int i = 0; i < 10; i++) assertEquals("primary", engine.pickVoice("v_ki", voices, true));
        assertNull(engine.pickVoice("v_ki", List.of(), false));
    }

    @Test
    void checksFavourVoicesThatAreHarderToFollow() {
        List<String> voices = List.of("primary", "easy", "hard");
        Progress.Tally easy = new Progress.Tally();
        easy.right = 9;
        easy.total = 10;
        Progress.Tally hard = new Progress.Tally();
        hard.right = 2;
        hard.total = 10;
        Progress.Tally primary = new Progress.Tally();
        primary.right = 10;
        primary.total = 10;
        progress.listenByVoice.put("easy", easy);
        progress.listenByVoice.put("hard", hard);
        progress.listenByVoice.put("primary", primary);
        for (int i = 0; i < 10; i++) assertEquals("hard", engine.pickVoice("v_ki", voices, true));
    }

    @Test
    void soundAlikeWordsAreCloser() {
        assertEquals(0, LearningEngine.soundDistance("はし", "はし"));
        assertTrue(LearningEngine.soundDistance("はし", "はな") < LearningEngine.soundDistance("はし", "ゾンビ"));
        assertTrue(LearningEngine.soundDistance("き", "ki") > 0);
    }

    @Test
    void listeningProgressRoundTripsThroughJson() {
        learnUntil("v_ki");
        hear("v_ki", "a", 3);
        progress.listenByVoice.computeIfAbsent("a", k -> new Progress.Tally()).total = 2;
        java.io.StringWriter w = new java.io.StringWriter();
        progress.write(w);
        Progress back = Progress.read(new java.io.StringReader(w.toString()));
        assertEquals(3, back.heard.get("v_ki"));
        assertEquals(3, back.heardVoices.get("v_ki").get("a"));
        assertEquals(2, back.listenByVoice.get("a").total);
        // Old progress files without these fields still load.
        Progress old = Progress.read(new java.io.StringReader("{\"version\":1}"));
        assertNotNull(old.heard);
        assertNotNull(old.listenByVoice);
    }
}
