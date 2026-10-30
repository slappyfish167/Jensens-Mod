package com.jensen.nihongo.learning;

import java.util.Random;

/**
 * SM-2 style spaced repetition scheduler with Anki-like learning steps.
 * New cards are rehearsed at short expanding gaps before graduating to day-based intervals,
 * then each successful recall multiplies the interval by the card's ease.
 */
public final class Srs {
    private Srs() {}

    public enum Grade { AGAIN, HARD, GOOD, EASY }

    public static final long MINUTE = 60_000L;
    public static final long DAY = 24 * 60 * MINUTE;

    static final long[] LEARNING_STEPS = {MINUTE, 10 * MINUTE};
    static final long[] RELEARNING_STEPS = {10 * MINUTE};
    static final double GRADUATING_INTERVAL = 1.0;
    static final double EASY_INTERVAL = 4.0;
    static final double MIN_EASE = 1.3;
    static final double MAX_INTERVAL = 365.0;

    /** Mutable per-card scheduling state, serialised into the progress file. */
    public static final class CardState {
        /** Index into the (re)learning steps, or -1 once the card is in review. */
        public int step = 0;
        public boolean relearning = false;
        public double ease = 2.5;
        /** Current review interval in days. */
        public double interval = 0;
        public long due = 0;
        public int reps = 0;
        public int lapses = 0;
        public long last = 0;

        public boolean inReview() {
            return step < 0;
        }

        public boolean isDue(long now) {
            return due <= now;
        }
    }

    public static CardState newCard(long now) {
        CardState s = new CardState();
        s.due = now;
        return s;
    }

    public static void review(CardState s, Grade grade, long now, Random rng) {
        s.reps++;
        s.last = now;
        if (!s.inReview()) {
            long[] steps = s.relearning ? RELEARNING_STEPS : LEARNING_STEPS;
            switch (grade) {
                case AGAIN -> {
                    s.step = 0;
                    s.due = now + steps[0];
                }
                case HARD -> s.due = now + steps[Math.min(s.step, steps.length - 1)];
                case GOOD -> {
                    s.step++;
                    if (s.step >= steps.length) {
                        graduate(s, s.relearning ? Math.max(1.0, s.interval) : GRADUATING_INTERVAL, now, rng);
                    } else {
                        s.due = now + steps[s.step];
                    }
                }
                case EASY -> graduate(s, s.relearning ? Math.max(1.0, s.interval) + 1 : EASY_INTERVAL, now, rng);
            }
            return;
        }
        switch (grade) {
            case AGAIN -> {
                s.lapses++;
                s.ease = Math.max(MIN_EASE, s.ease - 0.2);
                s.interval = Math.max(1.0, s.interval * 0.3);
                s.step = 0;
                s.relearning = true;
                s.due = now + RELEARNING_STEPS[0];
            }
            case HARD -> {
                s.ease = Math.max(MIN_EASE, s.ease - 0.15);
                schedule(s, Math.max(s.interval + 1, s.interval * 1.2), now, rng);
            }
            case GOOD -> schedule(s, Math.max(s.interval + 1, s.interval * s.ease), now, rng);
            case EASY -> {
                s.ease += 0.15;
                schedule(s, Math.max(s.interval + 1, s.interval * s.ease * 1.3), now, rng);
            }
        }
    }

    private static void graduate(CardState s, double interval, long now, Random rng) {
        s.step = -1;
        s.relearning = false;
        schedule(s, interval, now, rng);
    }

    private static void schedule(CardState s, double interval, long now, Random rng) {
        double days = Math.min(MAX_INTERVAL, interval);
        if (days >= 3 && rng != null) {
            // Small fuzz so cards learned together don't stay clumped together forever.
            days *= 0.95 + rng.nextDouble() * 0.10;
        }
        s.interval = days;
        s.due = now + (long) (days * DAY);
    }
}
