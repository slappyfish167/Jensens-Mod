package com.jensen.nihongo.learning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementTestTest {
    long now;
    Progress progress;
    LearningEngine engine;
    Curriculum curriculum;

    @BeforeEach
    void setUp() throws IOException {
        now = 1_700_000_000_000L;
        progress = new Progress();
        curriculum = CurriculumTest.load();
        engine = new LearningEngine(curriculum, progress, () -> now, new Random(7),
                new LearningEngine.Settings(), ZoneOffset.UTC);
    }

    int lessonOf(String itemId) {
        List<Curriculum.Lesson> lessons = curriculum.lessons();
        for (int i = 0; i < lessons.size(); i++) if (lessons.get(i).items.contains(itemId)) return i;
        throw new IllegalArgumentException(itemId);
    }

    /** Runs the whole test, answering correctly only for items in the first {@code knownLessons} lessons. */
    PlacementTest run(int knownLessons) {
        PlacementTest test = engine.startPlacement();
        StudySession s = engine.placementSession(test);
        assertNotNull(s);
        while (!s.finished()) {
            Question q = s.question();
            if (lessonOf(q.itemId) < knownLessons) {
                String answer = q.type == Question.Type.ORDER ? q.answer : q.accepted.get(0);
                assertTrue(s.submit(answer).passed(), q.cardId);
            } else {
                s.giveUp();
            }
            s.advance();
        }
        assertTrue(test.done());
        return test;
    }

    @Test
    void beginnerStopsAtOnceAndIsPlacedAtTheStart() {
        PlacementTest test = run(0);
        assertEquals(0, test.lessonsPlaced());
        assertTrue(test.totalAsked() <= 3);
        assertEquals(0, test.apply());
        assertEquals("hira_a", engine.currentLesson().id);
    }

    @Test
    void placesLearnerAfterTheLessonsTheyKnow() {
        int known = 8;
        PlacementTest test = run(known);
        assertEquals(known, test.lessonsPlaced());
        assertEquals(curriculum.lessons().get(known).id, test.firstUnplaced().id);

        int placed = test.apply();
        assertTrue(placed > 0);
        assertEquals(curriculum.lessons().get(known).id, engine.currentLesson().id);
        for (int i = 0; i < known; i++) {
            for (String id : curriculum.lessons().get(i).items) assertTrue(engine.isLearned(id), id);
        }
        assertFalse(engine.isIntroduced(curriculum.lessons().get(known).items.get(0)));
    }

    @Test
    void placementDoesNotCountAsNewItemsOrBlockTheNextLesson() {
        run(8).apply();
        assertEquals(0, progress.dailyNew);
        assertNull(engine.lessonBlockReason());
        assertNotNull(engine.startLesson());
    }

    @Test
    void placedItemsReturnAsSpreadOutReviews() {
        run(8).apply();
        long limit = now + 14 * Srs.DAY;
        for (Srs.CardState c : progress.cards.values()) {
            assertTrue(c.inReview());
            assertTrue(c.due >= now && c.due <= limit);
        }
        now += 15 * Srs.DAY;
        assertEquals(progress.cards.size(), engine.dueCount());
    }

    @Test
    void abandoningTheTestChangesNothing() {
        StudySession s = engine.placementSession(engine.startPlacement());
        s.submit("x");
        s.advance();
        assertTrue(progress.introduced.isEmpty());
        assertTrue(progress.cards.isEmpty());
    }

    @Test
    void expertIsPlacedPastTheWholeCourse() {
        PlacementTest test = run(curriculum.lessons().size());
        assertNull(test.firstUnplaced());
        test.apply();
        assertNull(engine.currentLesson());
    }

    @Test
    void retakingStartsFromTheCurrentLesson() {
        run(5).apply();
        PlacementTest second = engine.startPlacement();
        assertEquals(curriculum.lessons().get(5).id, engine.currentLesson().id);
        StudySession s = engine.placementSession(second);
        assertTrue(lessonOf(s.question().itemId) >= 5);
    }

    @Test
    void finishedCourseHasNothingToPlace() {
        run(curriculum.lessons().size()).apply();
        assertNull(engine.startPlacement());
    }
}
