package com.jensen.nihongo.learning;

import com.jensen.nihongo.learning.Curriculum.GrammarPoint;
import com.jensen.nihongo.learning.Curriculum.Item;
import com.jensen.nihongo.learning.Curriculum.KanaItem;
import com.jensen.nihongo.learning.Curriculum.Kind;
import com.jensen.nihongo.learning.Curriculum.Lesson;
import com.jensen.nihongo.learning.Curriculum.VocabItem;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * Decides what to study next and builds questions. Pure Java, so it can be unit-tested
 * without Minecraft.
 */
public final class LearningEngine {

    public static final class Settings {
        /** New items per day. Small daily batches keep cognitive load and review piles manageable. */
        public int dailyNewLimit = 15;
        /** Fraction of the recent lessons' cards that must be past the learning steps to unlock more. */
        public double gate = 0.8;
        public int maxReviewsPerSession = 60;
        /** Items taught before testing them (keeps working memory load low). */
        public int batchSize = 5;
        /** Days a word's reading must have been remembered before its kanji is introduced. */
        public double kanjiUnlockDays = 3.0;
        public int maxKanjiPerSession = 5;
    }

    public static final String SEP = "#";

    private final Curriculum curriculum;
    private final Progress progress;
    private final LongSupplier clock;
    private final Random rng;
    private final Settings settings;
    private final ZoneId zone;
    private final Set<String> introducedSet = new HashSet<>();
    private Runnable onChange = () -> {};

    public LearningEngine(Curriculum curriculum, Progress progress, LongSupplier clock, Random rng, Settings settings, ZoneId zone) {
        this.curriculum = curriculum;
        this.progress = progress;
        this.clock = clock;
        this.rng = rng;
        this.settings = settings;
        this.zone = zone;
        introducedSet.addAll(progress.introduced);
        progress.rollDay(today());
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    void changed() {
        onChange.run();
    }

    public Curriculum curriculum() {
        return curriculum;
    }

    public Progress progress() {
        return progress;
    }

    public Settings settings() {
        return settings;
    }

    public long now() {
        return clock.getAsLong();
    }

    public LocalDate today() {
        return Instant.ofEpochMilli(now()).atZone(zone).toLocalDate();
    }

    public static String cardId(String itemId, String direction) {
        return itemId + SEP + direction;
    }

    public static String itemOf(String cardId) {
        int i = cardId.indexOf(SEP);
        return i < 0 ? cardId : cardId.substring(0, i);
    }

    public static String directionOf(String cardId) {
        int i = cardId.indexOf(SEP);
        return i < 0 ? "" : cardId.substring(i + 1);
    }

    // ---------------------------------------------------------------- state queries

    public boolean isIntroduced(String itemId) {
        return introducedSet.contains(itemId);
    }

    /** An item is learned once every one of its cards has left the learning steps. */
    public boolean isLearned(String itemId) {
        Item item = curriculum.item(itemId);
        if (item == null || !isIntroduced(itemId)) return false;
        for (String dir : item.directions()) {
            Srs.CardState s = progress.cards.get(cardId(itemId, dir));
            if (s == null || !s.inReview()) return false;
        }
        return true;
    }

    /** 0..1 memory strength estimate used for immersion features. */
    public double strength(String itemId) {
        Item item = curriculum.item(itemId);
        if (item == null || !isIntroduced(itemId)) return 0;
        double total = 0;
        int n = 0;
        for (String dir : item.directions()) {
            Srs.CardState s = progress.cards.get(cardId(itemId, dir));
            n++;
            if (s != null && s.inReview()) total += Math.min(1.0, 0.4 + s.interval / 30.0);
            else if (s != null) total += 0.2;
        }
        return n == 0 ? 0 : total / n;
    }

    public int dueCount() {
        long now = now();
        int n = 0;
        for (Srs.CardState s : progress.cards.values()) if (s.isDue(now)) n++;
        return n;
    }

    public long nextDueTime() {
        long best = Long.MAX_VALUE;
        for (Srs.CardState s : progress.cards.values()) best = Math.min(best, s.due);
        return best;
    }

    public int newRemainingToday() {
        progress.rollDay(today());
        return Math.max(0, settings.dailyNewLimit - progress.dailyNew);
    }

    /** First lesson that still has items to introduce, or null when the course is complete. */
    public Lesson currentLesson() {
        for (Lesson l : curriculum.lessons()) {
            for (String id : l.items) {
                if (!isIntroduced(id) && curriculum.item(id) != null) return l;
            }
        }
        return null;
    }

    public int lessonIndex(Lesson lesson) {
        return curriculum.lessons().indexOf(lesson);
    }

    /** Fraction of the cards from the recent lessons that have graduated out of learning. */
    public double gateProgress() {
        Lesson cur = currentLesson();
        List<Lesson> all = curriculum.lessons();
        int idx = cur == null ? all.size() : all.indexOf(cur);
        Set<String> recent = new LinkedHashSet<>();
        if (idx > 0) recent.addAll(all.get(idx - 1).items);
        if (cur != null) recent.addAll(cur.items);
        int total = 0;
        int done = 0;
        for (String id : recent) {
            if (!isIntroduced(id)) continue;
            Item item = curriculum.item(id);
            if (item == null) continue;
            for (String dir : item.directions()) {
                Srs.CardState s = progress.cards.get(cardId(id, dir));
                if (s == null) continue;
                total++;
                if (s.inReview()) done++;
            }
        }
        return total == 0 ? 1.0 : (double) done / total;
    }

    /** Why a new lesson can't start right now, or null if it can. */
    public String lessonBlockReason() {
        if (currentLesson() == null) return "You've finished every lesson - keep your reviews going!";
        if (newRemainingToday() <= 0) return "Daily limit of new items reached. Reviews and rest help memories set.";
        if (gateProgress() < settings.gate) {
            return String.format("Master the last lesson first (%d%% of its cards past the learning stage, need %d%%).",
                    Math.round(gateProgress() * 100), Math.round(settings.gate * 100));
        }
        return null;
    }

    public record Counts(int kanaLearned, int kanaTotal, int vocabLearned, int vocabTotal,
                         int grammarLearned, int grammarTotal, int kanjiLearned) {}

    public Counts counts() {
        int kl = 0, kt = 0, vl = 0, vt = 0, gl = 0, gt = 0, kj = 0;
        for (Item i : curriculum.items().values()) {
            boolean learned = isLearned(i.id);
            switch (i.kind()) {
                case KANA -> { kt++; if (learned) kl++; }
                case VOCAB -> {
                    vt++;
                    if (learned) vl++;
                    Srs.CardState k = progress.cards.get(cardId(i.id, "kanji"));
                    if (k != null && k.inReview()) kj++;
                }
                case GRAMMAR -> { gt++; if (learned) gl++; }
            }
        }
        return new Counts(kl, kt, vl, vt, gl, gt, kj);
    }

    public List<VocabItem> learnedVocab() {
        List<VocabItem> out = new ArrayList<>();
        for (String id : progress.introduced) {
            if (curriculum.item(id) instanceof VocabItem v && isLearned(id)) out.add(v);
        }
        return out;
    }

    public List<VocabItem> introducedVocab() {
        List<VocabItem> out = new ArrayList<>();
        for (String id : progress.introduced) {
            if (curriculum.item(id) instanceof VocabItem v) out.add(v);
        }
        return out;
    }

    // ---------------------------------------------------------------- mutations

    void introduce(Item item) {
        if (introducedSet.add(item.id)) {
            progress.introduced.add(item.id);
            progress.rollDay(today());
            progress.dailyNew++;
        }
        long now = now();
        for (String dir : item.directions()) {
            progress.cards.computeIfAbsent(cardId(item.id, dir), k -> Srs.newCard(now));
        }
        changed();
    }

    void unlockKanji(String itemId) {
        progress.cards.computeIfAbsent(cardId(itemId, "kanji"), k -> Srs.newCard(now()));
        changed();
    }

    /** Returns true if the card is still in its (re)learning steps and should be repeated soon. */
    boolean applyGrade(String cardId, Srs.Grade grade) {
        Srs.CardState s = progress.cards.get(cardId);
        if (s == null) return false;
        long now = now();
        Srs.review(s, grade, now, rng);
        progress.rollDay(today());
        progress.dailyReviews++;
        progress.totalReviews++;
        if (grade != Srs.Grade.AGAIN) {
            progress.dailyCorrect++;
            progress.totalCorrect++;
        }
        progress.markStudied(today());
        return !s.inReview();
    }

    void recordSpoken(String itemId) {
        progress.spoken.merge(itemId, 1, Integer::sum);
        progress.markStudied(today());
    }

    // ---------------------------------------------------------------- sessions

    public StudySession startLesson() {
        if (lessonBlockReason() != null) return null;
        Lesson lesson = currentLesson();
        List<Item> fresh = new ArrayList<>();
        int budget = newRemainingToday();
        for (String id : lesson.items) {
            Item item = curriculum.item(id);
            if (item != null && !isIntroduced(id) && fresh.size() < budget) fresh.add(item);
        }
        // Grammar points are taught one at a time; they carry a lot of new information.
        int batch = fresh.stream().anyMatch(i -> i.kind() == Kind.GRAMMAR) ? 1 : settings.batchSize;
        List<StudySession.Step> steps = new ArrayList<>();
        for (int start = 0; start < fresh.size(); start += batch) {
            List<Item> group = fresh.subList(start, Math.min(fresh.size(), start + batch));
            List<StudySession.Step> asks = new ArrayList<>();
            for (Item item : group) {
                steps.add(new StudySession.Teach(item, false));
                for (String dir : item.directions()) asks.add(new StudySession.Ask(cardId(item.id, dir)));
            }
            Collections.shuffle(asks, rng);
            steps.addAll(asks);
        }
        return steps.isEmpty() ? null : new StudySession(this, StudySession.Mode.LESSON, steps, rng);
    }

    public StudySession startReview() {
        long now = now();
        List<Map.Entry<String, Srs.CardState>> due = new ArrayList<>();
        for (Map.Entry<String, Srs.CardState> e : progress.cards.entrySet()) {
            if (e.getValue().isDue(now) && curriculum.item(itemOf(e.getKey())) != null) due.add(e);
        }
        due.sort(Comparator.comparingLong(e -> e.getValue().due));
        List<StudySession.Step> steps = new ArrayList<>();
        for (int i = 0; i < due.size() && i < settings.maxReviewsPerSession; i++) {
            steps.add(new StudySession.Ask(due.get(i).getKey()));
        }
        // Interleave item types and directions rather than reviewing in blocks.
        Collections.shuffle(steps, rng);

        // Kanji are introduced only for words whose spoken form is already well known.
        int kanji = 0;
        for (VocabItem v : introducedVocab()) {
            if (kanji >= settings.maxKanjiPerSession) break;
            if (!v.hasKanjiCard() || progress.cards.containsKey(cardId(v.id, "kanji"))) continue;
            Srs.CardState reading = progress.cards.get(cardId(v.id, "reading"));
            if (reading != null && reading.inReview() && reading.interval >= settings.kanjiUnlockDays) {
                steps.add(new StudySession.Teach(v, true));
                steps.add(new StudySession.Ask(cardId(v.id, "kanji")));
                kanji++;
            }
        }
        return steps.isEmpty() ? null : new StudySession(this, StudySession.Mode.REVIEW, steps, rng);
    }

    /** Production practice: say words you know. Not scheduled; pure extra output practice. */
    public StudySession startSpeaking(int count) {
        List<VocabItem> pool = introducedVocab();
        if (pool.isEmpty()) return null;
        // Favour words spoken least often.
        pool.sort(Comparator.comparingInt(v -> progress.spoken.getOrDefault(v.id, 0) * 4 + rng.nextInt(4)));
        List<StudySession.Step> steps = new ArrayList<>();
        for (int i = 0; i < pool.size() && i < count; i++) {
            steps.add(new StudySession.Ask(cardId(pool.get(i).id, "speak")));
        }
        Collections.shuffle(steps, rng);
        return new StudySession(this, StudySession.Mode.SPEAKING, steps, rng);
    }

    /** Re-studies one lesson's items without affecting the schedule (no grades applied). */
    public List<Item> lessonItems(Lesson lesson) {
        List<Item> out = new ArrayList<>();
        for (String id : lesson.items) {
            Item i = curriculum.item(id);
            if (i != null) out.add(i);
        }
        return out;
    }

    // ---------------------------------------------------------------- questions

    Question buildQuestion(String cardId, boolean speaking) {
        Item item = curriculum.item(itemOf(cardId));
        String dir = directionOf(cardId);
        if (item == null) return null;
        if (item instanceof KanaItem k) return kanaQuestion(cardId, k, dir);
        if (item instanceof VocabItem v) return vocabQuestion(cardId, v, dir);
        if (item instanceof GrammarPoint g) return grammarQuestion(cardId, g);
        return null;
    }

    private Question kanaQuestion(String cardId, KanaItem k, String dir) {
        if (k.isGroup()) {
            List<String> pick = k.group.get(rng.nextInt(k.group.size()));
            Question q = new Question(cardId, k.id, Question.Type.TYPE_JAPANESE);
            q.prompt = pick.get(0);
            q.promptJapanese = true;
            q.instruction = "Type the reading in romaji";
            q.accepted.add(pick.get(0));
            q.accepted.add(pick.get(1));
            q.answer = pick.get(1);
            q.reading = pick.get(0) + "  (" + pick.get(1) + ")";
            q.detail = k.mnemonic;
            q.speak = pick.get(0);
            return q;
        }
        if (dir.equals("pick")) {
            Question q = new Question(cardId, k.id, Question.Type.CHOICE);
            q.prompt = k.romaji;
            q.subPrompt = "Which " + k.script + " makes this sound?";
            q.instruction = "Choose the matching " + k.script;
            q.accepted.add(k.kana);
            q.options = kanaOptions(k);
            q.answer = k.kana;
            q.reading = k.kana + "  (" + k.romaji + ")";
            q.detail = k.mnemonic;
            q.speak = k.kana;
            return q;
        }
        Question q = new Question(cardId, k.id, Question.Type.TYPE_JAPANESE);
        q.prompt = k.kana;
        q.promptJapanese = true;
        q.instruction = "Type the reading in romaji";
        q.accepted.add(k.kana);
        q.accepted.addAll(k.alt);
        q.answer = k.romaji;
        q.reading = k.kana + "  (" + k.romaji + ")";
        q.detail = k.mnemonic;
        q.speak = k.kana;
        return q;
    }

    /** Correct kana plus three distractors, preferring look-alikes the learner has already met. */
    private List<String> kanaOptions(KanaItem k) {
        List<String> opts = new ArrayList<>();
        opts.add(k.kana);
        List<String> similar = new ArrayList<>(curriculum.confusablesFor(k.kana));
        Collections.shuffle(similar, rng);
        for (String s : similar) if (opts.size() < 4 && !opts.contains(s)) opts.add(s);
        List<String> pool = new ArrayList<>();
        List<String> fallback = new ArrayList<>();
        for (Item i : curriculum.items().values()) {
            if (i instanceof KanaItem other && !other.isGroup() && other.script.equals(k.script) && !other.kana.equals(k.kana)) {
                (isIntroduced(other.id) ? pool : fallback).add(other.kana);
            }
        }
        Collections.shuffle(pool, rng);
        Collections.shuffle(fallback, rng);
        for (String s : pool) if (opts.size() < 4 && !opts.contains(s)) opts.add(s);
        for (String s : fallback) if (opts.size() < 4 && !opts.contains(s)) opts.add(s);
        Collections.shuffle(opts, rng);
        return opts;
    }

    private Question vocabQuestion(String cardId, VocabItem v, String dir) {
        String meaning = String.join(", ", v.meanings);
        String reading = v.kana + "  (" + v.romaji() + ")";
        boolean kanjiKnown = progress.cards.containsKey(cardId(v.id, "kanji"));
        Question q;
        switch (dir) {
            case "meaning" -> {
                q = new Question(cardId, v.id, Question.Type.TYPE_MEANING);
                q.prompt = v.kana;
                q.promptJapanese = true;
                if (kanjiKnown) q.subPrompt = v.kanji;
                if (v.hint != null) q.subPrompt = (q.subPrompt + "  (" + v.hint + ")").trim();
                q.instruction = "What does it mean? (English)";
                q.accepted.addAll(v.meanings);
                q.answer = meaning;
                q.speakInPrompt = true;
            }
            case "kanji" -> {
                q = new Question(cardId, v.id, Question.Type.TYPE_JAPANESE);
                q.prompt = v.kanji;
                q.promptJapanese = true;
                q.instruction = "Kanji! Type its reading (you already know this word)";
                q.accepted.add(v.kana);
                q.accepted.addAll(v.accept);
                q.answer = v.kana;
                q.allowVoice = true;
            }
            case "speak" -> {
                q = new Question(cardId, v.id, Question.Type.SPEAK);
                q.prompt = meaning;
                q.instruction = "Say it in Japanese";
                q.accepted.addAll(v.acceptedReadings());
                q.answer = v.written();
                q.iconInPrompt = true;
                q.allowVoice = true;
            }
            default -> {
                q = new Question(cardId, v.id, Question.Type.TYPE_JAPANESE);
                q.prompt = meaning;
                q.instruction = "Say or type it in Japanese (romaji is fine)";
                q.accepted.addAll(v.acceptedReadings());
                q.answer = v.written();
                q.iconInPrompt = true;
                q.allowVoice = true;
            }
        }
        q.reading = reading + (v.kanji != null && !v.kanji.isEmpty() ? "   " + v.kanji : "");
        q.meaning = meaning;
        q.detail = v.note;
        q.icon = v.icon;
        q.speak = v.kana;
        return q;
    }

    private Question grammarQuestion(String cardId, GrammarPoint g) {
        if (g.exercises.isEmpty()) return null;
        Curriculum.Exercise ex = g.exercises.get(rng.nextInt(g.exercises.size()));
        Question q;
        switch (ex.type) {
            case "order" -> {
                q = new Question(cardId, g.id, Question.Type.ORDER);
                q.prompt = ex.en;
                q.instruction = "Build the Japanese sentence";
                List<String> tiles = new ArrayList<>(ex.tiles);
                tiles.addAll(ex.extra);
                Collections.shuffle(tiles, rng);
                q.tiles = tiles;
                q.accepted.add(String.join(" ", ex.tiles));
                q.accepted.addAll(ex.accept);
                q.answer = String.join(" ", ex.tiles);
                q.speak = q.answer;
            }
            case "type" -> {
                q = new Question(cardId, g.id, Question.Type.TYPE_JAPANESE);
                q.prompt = ex.prompt;
                q.promptJapanese = true;
                q.subPrompt = ex.en;
                q.instruction = "Type the missing part (romaji is fine)";
                q.accepted.add(ex.answer);
                q.accepted.addAll(ex.accept);
                q.answer = ex.answer;
                q.speak = ex.prompt.replace("___", ex.answer);
            }
            default -> {
                q = new Question(cardId, g.id, Question.Type.CHOICE);
                q.prompt = ex.prompt;
                q.promptJapanese = Kana.containsJapanese(ex.prompt);
                q.subPrompt = ex.en;
                q.instruction = "Choose the best answer";
                List<String> opts = new ArrayList<>(ex.options);
                if (!opts.contains(ex.answer)) opts.add(ex.answer);
                Collections.shuffle(opts, rng);
                q.options = opts;
                q.accepted.add(ex.answer);
                q.answer = ex.answer;
                q.speak = Kana.containsJapanese(ex.prompt) ? ex.prompt.replace("___", ex.answer) : null;
            }
        }
        q.meaning = ex.en;
        q.detail = !ex.explain.isEmpty() ? ex.explain : g.title + ": " + g.summary;
        return q;
    }
}
