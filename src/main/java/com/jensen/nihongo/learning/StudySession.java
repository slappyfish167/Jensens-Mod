package com.jensen.nihongo.learning;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A run of study steps: new items are taught, then immediately tested (retrieval practice);
 * cards that are still being learned come back a few questions later (expanding rehearsal).
 */
public final class StudySession {
    /** FIELD: a quick study break about something just met in the world. */
    public enum Mode { LESSON, REVIEW, SPEAKING, PLACEMENT, FIELD }

    public sealed interface Step permits Teach, Ask {}

    /** Introduces an item. {@code kanjiOnly} marks the later introduction of a known word's kanji. */
    public record Teach(Curriculum.Item item, boolean kanjiOnly) implements Step {}

    public record Ask(String cardId) implements Step {}

    private final LearningEngine engine;
    private final Mode mode;
    private final List<Step> queue;
    private final Random rng;
    private final PlacementTest placement;

    private Step current;
    private Question question;
    private AnswerChecker.Result result;
    private Srs.Grade pendingGrade;
    private boolean overridden;
    private int answered;
    private int correct;
    private final List<String> missed = new ArrayList<>();
    private final java.util.Set<String> speakingRetries = new java.util.HashSet<>();

    StudySession(LearningEngine engine, Mode mode, List<Step> steps, Random rng) {
        this(engine, mode, steps, rng, null);
    }

    StudySession(LearningEngine engine, Mode mode, List<Step> steps, Random rng, PlacementTest placement) {
        this.engine = engine;
        this.placement = placement;
        this.mode = mode;
        this.queue = new ArrayList<>(steps);
        this.rng = rng;
        pop();
    }

    public Mode mode() {
        return mode;
    }

    /** The placement test driving this session, or null for ordinary sessions. */
    public PlacementTest placement() {
        return placement;
    }

    public Step current() {
        return current;
    }

    public Question question() {
        return question;
    }

    public AnswerChecker.Result result() {
        return result;
    }

    public boolean isAnswered() {
        return result != null && result.verdict() != AnswerChecker.Verdict.RETRY;
    }

    public boolean finished() {
        return current == null;
    }

    public int remaining() {
        return queue.size() + (current == null ? 0 : 1);
    }

    public int answeredCount() {
        return answered;
    }

    public int correctCount() {
        return correct;
    }

    public List<String> missed() {
        return missed;
    }

    private void pop() {
        result = null;
        pendingGrade = null;
        overridden = false;
        question = null;
        if (queue.isEmpty()) {
            current = null;
            return;
        }
        current = queue.remove(0);
        if (current instanceof Teach t) {
            if (t.kanjiOnly()) engine.unlockKanji(t.item().id);
            else engine.introduce(t.item());
        } else if (current instanceof Ask a) {
            question = engine.buildQuestion(a.cardId(), mode == Mode.SPEAKING);
            if (question == null) pop();
        }
    }

    /** Grades a typed answer or the text of a chosen option / tile sequence. */
    public AnswerChecker.Result submit(String input) {
        if (question == null || isAnswered()) return result;
        AnswerChecker.Result r = switch (question.type) {
            case TYPE_MEANING -> AnswerChecker.checkMeaning(input, question.accepted);
            case CHOICE -> question.accepted.contains(input)
                    ? new AnswerChecker.Result(AnswerChecker.Verdict.CORRECT, "")
                    : new AnswerChecker.Result(AnswerChecker.Verdict.WRONG, "");
            case SPEAK -> AnswerChecker.checkSpoken(List.of(input), question.accepted);
            default -> AnswerChecker.checkJapanese(input, question.accepted);
        };
        return record(r);
    }

    /** Grades a speech-recognition result (top alternatives). */
    public AnswerChecker.Result submitSpoken(List<String> transcripts) {
        if (question == null || isAnswered()) return result;
        AnswerChecker.Result r = AnswerChecker.checkSpoken(transcripts, question.accepted);
        if (r.passed()) engine.recordSpoken(question.itemId);
        return record(r);
    }

    private AnswerChecker.Result record(AnswerChecker.Result r) {
        result = r;
        if (r.verdict() == AnswerChecker.Verdict.RETRY) return r;
        answered++;
        if (r.passed()) {
            correct++;
            pendingGrade = Srs.Grade.GOOD;
        } else {
            pendingGrade = Srs.Grade.AGAIN;
            if (!missed.contains(question.itemId)) missed.add(question.itemId);
        }
        return r;
    }

    /** "I was right": for typos the checker could not forgive. */
    public void overrideCorrect() {
        if (!isAnswered() || result.passed() || overridden) return;
        overridden = true;
        correct++;
        missed.remove(question.itemId);
        pendingGrade = Srs.Grade.HARD;
        result = new AnswerChecker.Result(AnswerChecker.Verdict.CLOSE, "Marked correct.");
    }

    /** "I don't know": counts as a wrong answer without having to guess. */
    public void giveUp() {
        if (question == null || isAnswered()) return;
        record(new AnswerChecker.Result(AnswerChecker.Verdict.WRONG, ""));
    }

    /** Lets the learner try the same question again without grading (used for speaking). */
    public void retry() {
        if (question == null) return;
        if (isAnswered()) {
            answered--;
            if (result.passed()) correct--;
        }
        result = null;
        pendingGrade = null;
    }

    /** Applies the grade for the current question (if any) and moves to the next step. */
    public void advance() {
        if (mode == Mode.PLACEMENT) {
            // Placement never touches the schedule; each answer only decides which questions come next.
            if (current instanceof Ask && pendingGrade != null) {
                queue.addAll(placement.record(question.itemId, pendingGrade != Srs.Grade.AGAIN));
                if (placement.done()) queue.clear();
            }
            pop();
            return;
        }
        if (current instanceof Ask a && pendingGrade != null && mode != Mode.SPEAKING) {
            boolean stillLearning = engine.applyGrade(a.cardId(), pendingGrade);
            if (stillLearning) {
                int base = pendingGrade == Srs.Grade.AGAIN ? 2 : 5;
                int pos = Math.min(queue.size(), base + rng.nextInt(3));
                queue.add(pos, new Ask(a.cardId()));
            }
        } else if (current instanceof Ask a && mode == Mode.SPEAKING && pendingGrade == Srs.Grade.AGAIN) {
            // Give one more go at the end of a speaking session.
            if (speakingRetries.add(a.cardId())) queue.add(new Ask(a.cardId()));
        }
        engine.changed();
        pop();
    }
}
