package com.jensen.nihongo.learning;

import com.jensen.nihongo.learning.Srs.Grade;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SrsTest {
    @Test
    void newCardsGoThroughLearningSteps() {
        long now = 1_000_000L;
        Srs.CardState s = Srs.newCard(now);
        Srs.review(s, Grade.GOOD, now, null);
        assertFalse(s.inReview());
        assertEquals(now + 10 * Srs.MINUTE, s.due);
        Srs.review(s, Grade.GOOD, now, null);
        assertTrue(s.inReview());
        assertEquals(1.0, s.interval, 1e-9);
        assertEquals(now + Srs.DAY, s.due);
    }

    @Test
    void intervalsGrowWithEase() {
        long now = 0;
        Srs.CardState s = Srs.newCard(now);
        Srs.review(s, Grade.EASY, now, null);
        assertEquals(4.0, s.interval, 1e-9);
        Srs.review(s, Grade.GOOD, now, null);
        assertEquals(10.0, s.interval, 1e-9);
    }

    @Test
    void lapsesGoToRelearning() {
        long now = 0;
        Srs.CardState s = Srs.newCard(now);
        Srs.review(s, Grade.EASY, now, null);
        Srs.review(s, Grade.AGAIN, now, null);
        assertFalse(s.inReview());
        assertTrue(s.relearning);
        assertEquals(1, s.lapses);
        assertEquals(2.3, s.ease, 1e-9);
        Srs.review(s, Grade.GOOD, now, null);
        assertTrue(s.inReview());
        assertTrue(s.interval >= 1.0);
    }

    @Test
    void easeHasAFloor() {
        Srs.CardState s = Srs.newCard(0);
        Srs.review(s, Grade.EASY, 0, null);
        for (int i = 0; i < 20; i++) {
            Srs.review(s, Grade.AGAIN, 0, null);
            Srs.review(s, Grade.GOOD, 0, null);
        }
        assertEquals(1.3, s.ease, 1e-9);
    }
}
