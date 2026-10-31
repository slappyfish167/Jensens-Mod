package com.jensen.nihongo.learning;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A learner's progress. Stored per Minecraft account (not per world), so what you learn follows
 * you between singleplayer worlds and servers.
 */
public final class Progress {
    public int version = 1;
    /** Card id ("itemId#direction") to scheduling state. */
    public Map<String, Srs.CardState> cards = new LinkedHashMap<>();
    /** Item ids in the order they were introduced. */
    public List<String> introduced = new ArrayList<>();

    public String dailyDate = "";
    public int dailyNew = 0;
    public int dailyReviews = 0;
    public int dailyCorrect = 0;

    public String lastStudyDate = "";
    public int streak = 0;
    public int bestStreak = 0;

    public long totalReviews = 0;
    public long totalCorrect = 0;
    public int questsCompleted = 0;
    public int spellsCast = 0;
    /** How many times each item has been spoken correctly. */
    public Map<String, Integer> spoken = new HashMap<>();
    /** How many times each item has been heard aloud, in any voice. */
    public Map<String, Integer> heard = new HashMap<>();
    /** Item id to voice id to how many times the item was heard in that voice. */
    public Map<String, Map<String, Integer>> heardVoices = new HashMap<>();
    /** Listening results per voice, so voices that are harder to follow can be shown and practised more. */
    public Map<String, Tally> listenByVoice = new HashMap<>();

    /** Right answers out of all answers. */
    public static final class Tally {
        public int right;
        public int total;

        /** Smoothed accuracy, so a voice heard once doesn't count as 0% or 100%. */
        public double accuracy() {
            return (right + 1.0) / (total + 2.0);
        }
    }

    /** Resets the per-day counters when the date changes and maintains the study streak. */
    public void rollDay(LocalDate today) {
        String t = today.toString();
        if (!t.equals(dailyDate)) {
            dailyDate = t;
            dailyNew = 0;
            dailyReviews = 0;
            dailyCorrect = 0;
        }
    }

    public void markStudied(LocalDate today) {
        String t = today.toString();
        if (t.equals(lastStudyDate)) return;
        if (today.minusDays(1).toString().equals(lastStudyDate)) streak++;
        else streak = 1;
        bestStreak = Math.max(bestStreak, streak);
        lastStudyDate = t;
    }

    /** Streak as displayed: it only counts if you studied today or yesterday. */
    public int currentStreak(LocalDate today) {
        if (today.toString().equals(lastStudyDate) || today.minusDays(1).toString().equals(lastStudyDate)) {
            return streak;
        }
        return 0;
    }

    private void fillDefaults() {
        if (cards == null) cards = new LinkedHashMap<>();
        if (introduced == null) introduced = new ArrayList<>();
        if (spoken == null) spoken = new HashMap<>();
        if (heard == null) heard = new HashMap<>();
        if (heardVoices == null) heardVoices = new HashMap<>();
        if (listenByVoice == null) listenByVoice = new HashMap<>();
        if (dailyDate == null) dailyDate = "";
        if (lastStudyDate == null) lastStudyDate = "";
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Progress read(Reader reader) {
        Progress p = GSON.fromJson(reader, Progress.class);
        if (p == null) p = new Progress();
        p.fillDefaults();
        return p;
    }

    public void write(Writer writer) {
        GSON.toJson(this, writer);
    }

    public static Progress load(Path file) {
        if (!Files.exists(file)) return new Progress();
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return read(r);
        } catch (Exception e) {
            // Keep the broken file for manual recovery rather than silently overwriting it.
            try {
                Files.copy(file, file.resolveSibling(file.getFileName() + ".corrupt"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
            }
            return new Progress();
        }
    }

    public void save(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            write(w);
        }
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
