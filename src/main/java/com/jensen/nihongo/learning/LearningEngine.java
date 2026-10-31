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
        /** Whether {@link #dailyNewLimit} applies at all. Off by default: learners set their own pace. */
        public boolean dailyLimitEnabled = false;
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
        /** Whether Japanese audio can be played, so listening cards can be asked. Set by the client. */
        public boolean listeningAvailable = false;
        /** Times an item must have been heard before an in-world listening check may ask about it. */
        public int listenUnlockHeard = 3;
        /** New listening cards started in one listening practice session. */
        public int maxNewListenPerSession = 10;
        /** Times an item is heard in the primary voice before other voices start reading it. */
        public int anchorExposures = 2;
        /** Chance (0..1) that a voice other than the primary one reads an item, once past the anchor exposures. */
        public double voiceVariety = 0.4;
    }

    public static final String SEP = "#";
    /** Direction of listening cards: understanding the item by ear, tracked apart from reading it. */
    public static final String LISTEN = "listen";

    /** Placement: review interval and due-date spread (days) for items skipped past vs. proven in the test. */
    static final double PLACED_INTERVAL = 3.0;
    static final double PLACED_SPREAD = 7.0;
    static final double PLACED_SURE_INTERVAL = 8.0;
    static final double PLACED_SURE_SPREAD = 14.0;

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
        for (Map.Entry<String, Srs.CardState> e : progress.cards.entrySet()) {
            if (e.getValue().isDue(now) && askable(e.getKey())) n++;
        }
        return n;
    }

    public long nextDueTime() {
        long best = Long.MAX_VALUE;
        for (Map.Entry<String, Srs.CardState> e : progress.cards.entrySet()) {
            if (askable(e.getKey())) best = Math.min(best, e.getValue().due);
        }
        return best;
    }

    /** Whether a card can be asked now: its item exists, and listening cards need audio. */
    private boolean askable(String cardId) {
        return curriculum.item(itemOf(cardId)) != null && (settings.listeningAvailable || !isListenCard(cardId));
    }

    /** New items still allowed today; unlimited when the daily limit is turned off. */
    public int newRemainingToday() {
        progress.rollDay(today());
        if (!settings.dailyLimitEnabled) return Integer.MAX_VALUE;
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

    /** True once every hiragana and katakana item has been introduced (the course teaches all kana before words). */
    public boolean kanaStageDone() {
        for (Item i : curriculum.items().values()) {
            if (i.kind() == Kind.KANA && !isIntroduced(i.id) && inSomeLesson(i.id)) return false;
        }
        return true;
    }

    /** Kanji are the last stage: they open once every word lesson has been started. */
    public boolean kanjiStageOpen() {
        for (Lesson l : curriculum.lessons()) {
            for (String id : l.items) {
                if (curriculum.item(id) instanceof VocabItem && !isIntroduced(id)) return false;
            }
        }
        return true;
    }

    private boolean inSomeLesson(String itemId) {
        for (Lesson l : curriculum.lessons()) if (l.items.contains(itemId)) return true;
        return false;
    }

    /** Words whose kanji can be taught now: the spoken word has been remembered for a few days. */
    public List<VocabItem> kanjiReady() {
        List<VocabItem> out = new ArrayList<>();
        if (!kanjiStageOpen()) return out;
        for (VocabItem v : introducedVocab()) {
            if (!v.hasKanjiCard() || progress.cards.containsKey(cardId(v.id, "kanji"))) continue;
            Srs.CardState reading = progress.cards.get(cardId(v.id, "reading"));
            if (reading != null && reading.inReview() && reading.interval >= settings.kanjiUnlockDays) out.add(v);
        }
        return out;
    }

    /** Why a kanji lesson can't start right now, or null if it can. */
    public String kanjiBlockReason() {
        if (!kanjiStageOpen()) return "Kanji come last: finish the hiragana, katakana and word lessons first.";
        if (newRemainingToday() <= 0) return "Daily limit of new items reached.";
        if (kanjiReady().isEmpty()) {
            return String.format("No kanji ready yet. A word's kanji unlocks once you've remembered the word for %d days.",
                    Math.round(settings.kanjiUnlockDays));
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
        if (currentLesson() == null) return "You've finished every lesson! Now learn the kanji and keep your reviews going.";
        if (newRemainingToday() <= 0) return "Daily limit of new items reached. Reviews and rest help memories set.";
        if (gateProgress() < settings.gate) {
            return String.format("Master the last lesson first (%d%% of its cards past the learning stage, need %d%%).",
                    Math.round(gateProgress() * 100), Math.round(settings.gate * 100));
        }
        return null;
    }

    /** {@code listening}: words and phrases understood by ear (listening cards past the learning stage). */
    public record Counts(int kanaLearned, int kanaTotal, int vocabLearned, int vocabTotal,
                         int grammarLearned, int grammarTotal, int kanjiLearned, int listening) {}

    public Counts counts() {
        int kl = 0, kt = 0, vl = 0, vt = 0, gl = 0, gt = 0, kj = 0, li = 0;
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
            Srs.CardState l = progress.cards.get(cardId(i.id, LISTEN));
            if (l != null && l.inReview()) li++;
        }
        return new Counts(kl, kt, vl, vt, gl, gt, kj, li);
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

    /**
     * Marks the items of lessons [from, to) as already known, without counting them as new items today.
     * Their cards start in review with short intervals, spread over the coming days, so any gap the
     * placement test missed is caught by ordinary reviews. Items answered correctly in the test get longer ones.
     */
    int placeLessons(int from, int to, Set<String> verified) {
        long now = now();
        int placed = 0;
        List<Lesson> lessons = curriculum.lessons();
        for (int i = from; i < to && i < lessons.size(); i++) {
            for (String id : lessons.get(i).items) {
                Item item = curriculum.item(id);
                if (item == null || !introducedSet.add(id)) continue;
                progress.introduced.add(id);
                boolean sure = verified.contains(id);
                double interval = sure ? PLACED_SURE_INTERVAL : PLACED_INTERVAL;
                double spread = sure ? PLACED_SURE_SPREAD : PLACED_SPREAD;
                for (String dir : item.directions()) {
                    Srs.CardState s = progress.cards.computeIfAbsent(cardId(id, dir), k -> Srs.newCard(now));
                    s.step = -1;
                    s.relearning = false;
                    s.interval = interval;
                    s.reps = Math.max(s.reps, 1);
                    s.last = now;
                    s.due = now + (long) (rng.nextDouble() * spread * Srs.DAY);
                }
                placed++;
            }
        }
        if (placed > 0) changed();
        return placed;
    }

    void unlockKanji(String itemId) {
        if (!progress.cards.containsKey(cardId(itemId, "kanji"))) {
            progress.cards.put(cardId(itemId, "kanji"), Srs.newCard(now()));
            progress.rollDay(today());
            progress.dailyNew++;
        }
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
            if (e.getValue().isDue(now) && askable(e.getKey())) due.add(e);
        }
        due.sort(Comparator.comparingLong(e -> e.getValue().due));
        List<StudySession.Step> steps = new ArrayList<>();
        for (int i = 0; i < due.size() && i < settings.maxReviewsPerSession; i++) {
            steps.add(new StudySession.Ask(due.get(i).getKey()));
        }
        // Interleave item types and directions rather than reviewing in blocks.
        Collections.shuffle(steps, rng);
        return steps.isEmpty() ? null : new StudySession(this, StudySession.Mode.REVIEW, steps, rng);
    }

    /** Teaches the kanji of words whose spoken form is already well known (the final stage of the course). */
    public StudySession startKanjiLesson() {
        if (kanjiBlockReason() != null) return null;
        List<VocabItem> ready = kanjiReady();
        int n = Math.min(Math.min(settings.maxKanjiPerSession, newRemainingToday()), ready.size());
        List<StudySession.Step> steps = new ArrayList<>();
        List<StudySession.Step> asks = new ArrayList<>();
        for (VocabItem v : ready.subList(0, n)) {
            steps.add(new StudySession.Teach(v, true));
            asks.add(new StudySession.Ask(cardId(v.id, "kanji")));
        }
        Collections.shuffle(asks, rng);
        steps.addAll(asks);
        return steps.isEmpty() ? null : new StudySession(this, StudySession.Mode.LESSON, steps, rng);
    }

    // ---------------------------------------------------------------- in-world encounters

    public enum EncounterKind { REVIEW, DISCOVER }

    /** Something met in the world (mined, collected, attacked, looked at) that the learner can study on the spot. */
    public record Encounter(EncounterKind kind, VocabItem word) {}

    /**
     * Picks what to study about a thing met in the world: a due review of its word first; otherwise, once the
     * kana are done, a word not learned yet, taught right there instead of waiting for its lesson.
     */
    public Encounter encounter(List<VocabItem> words) {
        long now = now();
        for (VocabItem v : words) {
            if (isIntroduced(v.id) && !dueCards(v, now).isEmpty()) return new Encounter(EncounterKind.REVIEW, v);
        }
        if (!kanaStageDone() || newRemainingToday() <= 0) return null;
        for (VocabItem v : words) {
            if (!isIntroduced(v.id) && inSomeLesson(v.id)) return new Encounter(EncounterKind.DISCOVER, v);
        }
        return null;
    }

    private List<String> dueCards(VocabItem v, long now) {
        List<String> out = new ArrayList<>();
        for (String dir : List.of("meaning", "reading", "kanji")) {
            Srs.CardState s = progress.cards.get(cardId(v.id, dir));
            if (s != null && s.isDue(now)) out.add(cardId(v.id, dir));
        }
        return out;
    }

    /** A short study session for an encounter, or null if it no longer applies. */
    public StudySession startEncounter(Encounter e) {
        List<StudySession.Step> steps = new ArrayList<>();
        if (e.kind() == EncounterKind.REVIEW) {
            for (String card : dueCards(e.word(), now())) steps.add(new StudySession.Ask(card));
            Collections.shuffle(steps, rng);
            return steps.isEmpty() ? null : new StudySession(this, StudySession.Mode.FIELD, steps, rng);
        }
        if (isIntroduced(e.word().id) || newRemainingToday() <= 0) return null;
        steps.add(new StudySession.Teach(e.word(), false));
        List<StudySession.Step> asks = new ArrayList<>();
        for (String dir : e.word().directions()) asks.add(new StudySession.Ask(cardId(e.word().id, dir)));
        Collections.shuffle(asks, rng);
        steps.addAll(asks);
        return new StudySession(this, StudySession.Mode.FIELD, steps, rng);
    }

    // ---------------------------------------------------------------- listening

    public static boolean isListenCard(String cardId) {
        return LISTEN.equals(directionOf(cardId));
    }

    /** Items with a listening card: words, and grammar points (through their example sentences). */
    public static boolean hasListening(Item item) {
        return item instanceof VocabItem || item instanceof GrammarPoint g && !g.examples.isEmpty();
    }

    /**
     * Known well enough by eye to be tested by ear, with no listening card yet: words whose reading has left
     * the learning steps, and learned grammar points.
     */
    private boolean listenEligible(Item item) {
        if (!hasListening(item) || !isIntroduced(item.id) || progress.cards.containsKey(cardId(item.id, LISTEN))) return false;
        if (item instanceof VocabItem) {
            Srs.CardState reading = progress.cards.get(cardId(item.id, "reading"));
            return reading != null && reading.inReview();
        }
        return isLearned(item.id);
    }

    public int heardCount(String itemId) {
        return progress.heard.getOrDefault(itemId, 0);
    }

    /** Items heard often enough to be asked about by ear, most heard first. */
    public List<Item> listenReady() {
        List<Item> out = new ArrayList<>();
        for (String id : progress.introduced) {
            Item item = curriculum.item(id);
            if (item != null && listenEligible(item) && heardCount(id) >= settings.listenUnlockHeard) out.add(item);
        }
        out.sort(Comparator.comparingInt((Item i) -> -heardCount(i.id)));
        return out;
    }

    public int listenDueCount() {
        if (!settings.listeningAvailable) return 0;
        long now = now();
        int n = 0;
        for (Map.Entry<String, Srs.CardState> e : progress.cards.entrySet()) {
            if (isListenCard(e.getKey()) && e.getValue().isDue(now) && askable(e.getKey())) n++;
        }
        return n;
    }

    private void unlockListening(String itemId) {
        progress.cards.computeIfAbsent(cardId(itemId, LISTEN), k -> Srs.newCard(now()));
        changed();
    }

    /** Counts one hearing of an item (in the given voice, if known). */
    public void recordHeard(String itemId, String voice) {
        if (itemId == null || curriculum.item(itemId) == null) return;
        progress.heard.merge(itemId, 1, Integer::sum);
        if (voice != null) progress.heardVoices.computeIfAbsent(itemId, k -> new java.util.HashMap<>()).merge(voice, 1, Integer::sum);
        changed();
    }

    void recordListening(String voice, boolean right) {
        Progress.Tally t = progress.listenByVoice.computeIfAbsent(voice, k -> new Progress.Tally());
        t.total++;
        if (right) t.right++;
    }

    /**
     * Chooses who reads an item aloud. The first few hearings use the primary voice ({@code voices.get(0)}), so
     * a new word starts from one clear model; after that other voices take over some of the time, favouring the
     * voices this item has been heard in least, then the voices the learner understands least. Listening checks
     * always rotate, so recognition is tested across speakers rather than in one familiar voice.
     */
    public String pickVoice(String itemId, List<String> voices, boolean check) {
        if (voices.isEmpty()) return null;
        String primary = voices.get(0);
        if (voices.size() == 1 || settings.voiceVariety <= 0) return primary;
        if (!check && (heardCount(itemId) < settings.anchorExposures || rng.nextDouble() >= settings.voiceVariety)) {
            return primary;
        }
        Map<String, Integer> byVoice = progress.heardVoices.getOrDefault(itemId, Map.of());
        List<String> pool = new ArrayList<>(check ? voices : voices.subList(1, voices.size()));
        Collections.shuffle(pool, rng);
        pool.sort(Comparator.comparingInt((String v) -> byVoice.getOrDefault(v, 0))
                .thenComparingDouble(v -> progress.listenByVoice.getOrDefault(v, new Progress.Tally()).accuracy()));
        return pool.get(0);
    }

    /**
     * The card for an occasional in-world listening check: a listening card that is due, or else a known word
     * or phrase the learner has now heard several times (its listening card starts here). Null if there is none.
     */
    public String listeningCheck() {
        if (!settings.listeningAvailable) return null;
        long now = now();
        String best = null;
        long bestDue = Long.MAX_VALUE;
        for (Map.Entry<String, Srs.CardState> e : progress.cards.entrySet()) {
            Srs.CardState s = e.getValue();
            if (isListenCard(e.getKey()) && s.isDue(now) && s.due < bestDue && askable(e.getKey())) {
                best = e.getKey();
                bestDue = s.due;
            }
        }
        if (best != null) return best;
        List<Item> ready = listenReady();
        if (ready.isEmpty()) return null;
        unlockListening(ready.get(0).id);
        return cardId(ready.get(0).id, LISTEN);
    }

    /** A one-question session for an in-world listening check. */
    public StudySession startListenCheck(String cardId) {
        if (!settings.listeningAvailable || !isListenCard(cardId) || !progress.cards.containsKey(cardId)) return null;
        StudySession s = new StudySession(this, StudySession.Mode.FIELD, List.of(new StudySession.Ask(cardId)), rng);
        s.repeat = false;
        return s;
    }

    /** Why listening practice can't start right now, or null if it can. */
    public String listeningBlockReason() {
        if (!settings.listeningAvailable) return "No Japanese voice found. Set one up in Voice & audio.";
        if (listenDueCount() > 0) return null;
        for (String id : progress.introduced) {
            Item item = curriculum.item(id);
            if (item != null && listenEligible(item)) return null;
        }
        return "Nothing to listen to yet. Words join listening practice once you know how to read them.";
    }

    /**
     * Listening practice: due listening cards, plus a few known words and phrases tested by ear for the first
     * time (most heard first). Nothing is shown in writing until it has been answered.
     */
    public StudySession startListening() {
        if (listeningBlockReason() != null) return null;
        long now = now();
        List<Map.Entry<String, Srs.CardState>> due = new ArrayList<>();
        for (Map.Entry<String, Srs.CardState> e : progress.cards.entrySet()) {
            if (isListenCard(e.getKey()) && e.getValue().isDue(now) && askable(e.getKey())) due.add(e);
        }
        due.sort(Comparator.comparingLong(e -> e.getValue().due));
        List<StudySession.Step> steps = new ArrayList<>();
        for (int i = 0; i < due.size() && i < settings.maxReviewsPerSession; i++) steps.add(new StudySession.Ask(due.get(i).getKey()));
        List<Item> fresh = new ArrayList<>();
        for (String id : progress.introduced) {
            Item item = curriculum.item(id);
            if (item != null && listenEligible(item)) fresh.add(item);
        }
        fresh.sort(Comparator.comparingInt((Item i) -> -heardCount(i.id)));
        for (int i = 0; i < fresh.size() && i < settings.maxNewListenPerSession; i++) {
            unlockListening(fresh.get(i).id);
            steps.add(new StudySession.Ask(cardId(fresh.get(i).id, LISTEN)));
        }
        Collections.shuffle(steps, rng);
        return steps.isEmpty() ? null : new StudySession(this, StudySession.Mode.LISTENING, steps, rng);
    }

    /** Adaptive placement test, or null if the course is already complete. Changes nothing until applied. */
    public PlacementTest startPlacement() {
        PlacementTest test = new PlacementTest(this, rng);
        return test.done() ? null : test;
    }

    /** The study session that runs a placement test. */
    public StudySession placementSession(PlacementTest test) {
        List<StudySession.Step> steps = test.openingSteps();
        return steps.isEmpty() ? null : new StudySession(this, StudySession.Mode.PLACEMENT, steps, rng, test);
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
        if (LISTEN.equals(dir)) return listenQuestion(cardId, item);
        if (item instanceof KanaItem k) return kanaQuestion(cardId, k, dir);
        if (item instanceof VocabItem v) return vocabQuestion(cardId, v, dir);
        if (item instanceof GrammarPoint g) return grammarQuestion(cardId, g);
        return null;
    }

    /** Audio-only questions: the challenge is recognising spoken Japanese, so nothing Japanese is shown first. */
    private Question listenQuestion(String cardId, Item item) {
        Question q;
        if (item instanceof VocabItem v) {
            List<String> distractors = v.meanings.isEmpty() ? List.of() : soundAlikeMeanings(v);
            if (distractors.size() >= 3 && rng.nextInt(10) < 7) {
                q = new Question(cardId, v.id, Question.Type.CHOICE);
                q.instruction = "Which word did you hear?";
                q.subPrompt = "Listen, then pick what it means";
                String right = v.meanings.get(0);
                List<String> opts = new ArrayList<>(distractors.subList(0, 3));
                opts.add(right);
                Collections.shuffle(opts, rng);
                q.options = opts;
                q.accepted.add(right);
                q.answer = right;
            } else {
                q = new Question(cardId, v.id, Question.Type.TYPE_JAPANESE);
                q.instruction = "What was just said? Type what you heard (romaji is fine)";
                q.accepted.add(v.kana);
                q.accepted.addAll(v.accept);
                q.answer = v.kana;
            }
            q.speak = v.kana;
            q.reading = v.kana + "  (" + v.romaji() + ")" + (v.kanji != null && !v.kanji.isEmpty() ? "   " + v.kanji : "");
            q.meaning = String.join(", ", v.meanings);
            q.icon = v.icon;
            q.detail = v.note;
        } else if (item instanceof GrammarPoint g && !g.examples.isEmpty()) {
            Curriculum.Example ex = g.examples.get(rng.nextInt(g.examples.size()));
            q = new Question(cardId, g.id, Question.Type.CHOICE);
            q.instruction = "What did this phrase mean?";
            q.subPrompt = "Listen, then pick the meaning";
            List<String> opts = new ArrayList<>(otherSentenceMeanings(g, ex.en));
            opts.add(ex.en);
            Collections.shuffle(opts, rng);
            q.options = opts;
            q.accepted.add(ex.en);
            q.answer = ex.en;
            q.speak = ex.jp;
            q.reading = ex.jp;
            q.detail = g.title + ": " + g.summary;
        } else {
            return null;
        }
        q.prompt = "♪";
        q.listening = true;
        q.speakInPrompt = true;
        return q;
    }

    /**
     * Meanings of three words that sound like this one (closest kana spelling), so the right answer can only
     * be picked by actually telling the sounds apart. Words the learner knows are preferred.
     */
    private List<String> soundAlikeMeanings(VocabItem v) {
        Set<String> own = new HashSet<>();
        for (String m : v.meanings) own.add(m.toLowerCase(java.util.Locale.ROOT));
        List<VocabItem> pool = new ArrayList<>();
        for (VocabItem o : introducedVocab()) if (usableDistractor(v, o, own)) pool.add(o);
        if (pool.size() < 3) {
            for (Item i : curriculum.items().values()) {
                if (i instanceof VocabItem o && !isIntroduced(o.id) && usableDistractor(v, o, own)) pool.add(o);
            }
        }
        Collections.shuffle(pool, rng);
        pool.sort(Comparator.comparingInt(o -> soundDistance(v.kana, o.kana)));
        List<VocabItem> near = new ArrayList<>(pool.subList(0, Math.min(6, pool.size())));
        Collections.shuffle(near, rng);
        Set<String> out = new LinkedHashSet<>();
        for (VocabItem o : near) {
            if (out.size() < 3 && !own.contains(o.meanings.get(0).toLowerCase(java.util.Locale.ROOT))) out.add(o.meanings.get(0));
        }
        return new ArrayList<>(out);
    }

    private static boolean usableDistractor(VocabItem v, VocabItem o, Set<String> ownMeanings) {
        // Homophones are left out: they can't be told apart by ear.
        return !o.id.equals(v.id) && !o.kana.equals(v.kana) && !o.meanings.isEmpty()
                && !ownMeanings.contains(o.meanings.get(0).toLowerCase(java.util.Locale.ROOT));
    }

    /** Edit distance between two kana spellings, plus a little for length, as a rough measure of sounding alike. */
    static int soundDistance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int sub = prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                cur[j] = Math.min(sub, Math.min(prev[j], cur[j - 1]) + 1);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()] * 3 + Math.abs(a.length() - b.length());
    }

    /** English meanings of up to three example sentences from other grammar points (learned ones first). */
    private List<String> otherSentenceMeanings(GrammarPoint g, String right) {
        List<String> known = new ArrayList<>();
        List<String> rest = new ArrayList<>();
        for (Item i : curriculum.items().values()) {
            if (!(i instanceof GrammarPoint o) || o.id.equals(g.id)) continue;
            for (Curriculum.Example ex : o.examples) {
                if (ex.en != null && !ex.en.isBlank() && !ex.en.equals(right)) (isIntroduced(o.id) ? known : rest).add(ex.en);
            }
        }
        Collections.shuffle(known, rng);
        Collections.shuffle(rest, rng);
        Set<String> out = new LinkedHashSet<>();
        for (String s : known) if (out.size() < 3) out.add(s);
        for (String s : rest) if (out.size() < 3) out.add(s);
        return new ArrayList<>(out);
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
