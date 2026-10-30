package com.jensen.nihongo.learning;

import com.jensen.nihongo.learning.Curriculum.GrammarPoint;
import com.jensen.nihongo.learning.Curriculum.Item;
import com.jensen.nihongo.learning.Curriculum.KanaItem;
import com.jensen.nihongo.learning.Curriculum.Lesson;
import com.jensen.nihongo.learning.Curriculum.VocabItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * An adaptive placement test for learners who already know some Japanese.
 * <p>
 * It walks the course in order and samples a few items from each lesson. A lesson is passed if at
 * most a third of its sampled answers are wrong; the test stops at the first lesson that is not
 * passed, so a true beginner sees only a couple of questions. Nothing is written to the learner's
 * progress until {@link #apply()} is called, so abandoning the test changes nothing.
 */
public final class PlacementTest {
    static final int SAMPLE = 3;
    static final int SAMPLE_LARGE = 4;
    /** Lessons with at least this many items get {@link #SAMPLE_LARGE} questions. */
    static final int LARGE_LESSON = 10;

    private final LearningEngine engine;
    private final Random rng;
    private final List<Lesson> lessons;
    private final int start;

    private int index;
    private int planned;
    private int asked;
    private int wrong;
    private boolean done;
    private int totalAsked;
    private int totalCorrect;
    private final Set<String> verified = new HashSet<>();

    PlacementTest(LearningEngine engine, Random rng) {
        this.engine = engine;
        this.rng = rng;
        this.lessons = engine.curriculum().lessons();
        Lesson current = engine.currentLesson();
        this.start = current == null ? lessons.size() : lessons.indexOf(current);
        this.index = start;
        this.done = start >= lessons.size();
    }

    /** Questions for the first lesson that needs testing (empty if there is nothing to test). */
    List<StudySession.Step> openingSteps() {
        return planFromCurrent();
    }

    /** Records an answer and returns any steps to queue next. Sets {@link #done()} when the test is over. */
    List<StudySession.Step> record(String itemId, boolean correct) {
        asked++;
        totalAsked++;
        if (correct) {
            totalCorrect++;
            verified.add(itemId);
        } else {
            wrong++;
        }
        if (wrong > planned / 3) {
            done = true;
            return List.of();
        }
        if (asked < planned) return List.of();
        index++;
        return planFromCurrent();
    }

    /** Plans the lesson at {@link #index}, auto-passing any lesson that has nothing to ask. */
    private List<StudySession.Step> planFromCurrent() {
        while (index < lessons.size()) {
            List<StudySession.Step> steps = plan(lessons.get(index));
            if (!steps.isEmpty()) {
                planned = steps.size();
                asked = 0;
                wrong = 0;
                return steps;
            }
            index++;
        }
        done = true;
        return List.of();
    }

    private List<StudySession.Step> plan(Lesson lesson) {
        List<Item> pool = engine.lessonItems(lesson);
        Collections.shuffle(pool, rng);
        int target = Math.min(pool.size(), lesson.items.size() >= LARGE_LESSON ? SAMPLE_LARGE : SAMPLE);
        List<StudySession.Step> steps = new ArrayList<>();
        for (Item item : pool) {
            if (steps.size() >= target) break;
            String card = LearningEngine.cardId(item.id, direction(item));
            if (engine.buildQuestion(card, false) != null) steps.add(new StudySession.Ask(card));
        }
        return steps;
    }

    /** Recognition-style direction for each kind of item: what the learner can read, not what they were drilled on. */
    private static String direction(Item item) {
        if (item instanceof KanaItem) return "read";
        if (item instanceof VocabItem) return "meaning";
        if (item instanceof GrammarPoint) return "ex";
        return item.directions().get(0);
    }

    public boolean done() {
        return done;
    }

    /** 1-based number of the lesson being tested. */
    public int lessonNumber() {
        return Math.min(index + 1, lessons.size());
    }

    public int lessonTotal() {
        return lessons.size();
    }

    public int lessonsPlaced() {
        return index - start;
    }

    /** The lesson the learner would start at after placement, or null if they know the whole course. */
    public Lesson firstUnplaced() {
        return index < lessons.size() ? lessons.get(index) : null;
    }

    public int totalAsked() {
        return totalAsked;
    }

    public int totalCorrect() {
        return totalCorrect;
    }

    /** Marks every passed lesson as learned. Returns the number of items placed. */
    public int apply() {
        return engine.placeLessons(start, index, verified);
    }
}
