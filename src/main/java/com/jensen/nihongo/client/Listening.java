package com.jensen.nihongo.client;

import com.jensen.nihongo.client.screen.StudyScreen;
import com.jensen.nihongo.client.tts.Tts;
import com.jensen.nihongo.client.tts.VoiceCatalog;
import com.jensen.nihongo.client.tts.VoiceCatalog.Speaker;
import com.jensen.nihongo.client.voice.VoiceService;
import com.jensen.nihongo.learning.Curriculum;
import com.jensen.nihongo.learning.LearningEngine;
import com.jensen.nihongo.learning.Question;
import com.jensen.nihongo.learning.StudySession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Japanese heard during play, in many voices. Every time a word is read aloud, a voice is chosen for it
 * (see {@link LearningEngine#pickVoice}) and the hearing is counted. Out in the world, known words are
 * sometimes said aloud as you meet the things they name, and now and then a short listening check says a
 * word or phrase you have heard several times and asks what it was. Listening has its own cards, so it is
 * tracked apart from reading.
 */
public final class Listening {
    private Listening() {}

    /** How long a listening check waits for an answer. */
    private static final long CHECK_SHOW_MS = 12_000L;
    /** The same word is not said again in the world for this long. */
    private static final long SAME_WORD_MS = 3 * 60_000L;

    /** A listening check that has been heard and waits for an answer; nothing is graded unless it is answered. */
    private static StudySession pending;
    private static long pendingUntil;
    private static long nextCheckAt = System.currentTimeMillis() + 3 * 60_000L;
    private static long nextAmbientAt;
    private static final Map<String, Long> recentAmbient = new HashMap<>();

    /** Whether there is a voice to listen to. */
    public static boolean available() {
        return !VoiceCatalog.active().isEmpty();
    }

    private static List<String> voiceIds() {
        List<String> ids = new ArrayList<>();
        for (Speaker s : VoiceCatalog.active()) ids.add(s.id());
        return ids;
    }

    /** The voice that should read this item now. */
    public static Speaker voiceFor(String itemId, boolean check) {
        String id = Learner.engine().pickVoice(itemId, voiceIds(), check);
        Speaker s = id == null ? null : VoiceCatalog.byId(id);
        return s != null ? s : VoiceCatalog.primary();
    }

    /** Reads an item's Japanese aloud in a voice chosen for it, and counts the hearing. Returns the voice used. */
    public static Speaker say(String itemId, String text) {
        if (text == null || text.isBlank() || !Tts.enabled()) return null;
        Speaker s = itemId == null ? VoiceCatalog.primary() : voiceFor(itemId, false);
        say(itemId, text, s, false);
        return s;
    }

    /** Reads an item aloud in a given voice (null: the system default), and counts the hearing. */
    public static void say(String itemId, String text, Speaker voice, boolean slow) {
        if (text == null || text.isBlank() || !Tts.enabled()) return;
        Tts.speak(text, voice, slow);
        if (itemId != null) Learner.engine().recordHeard(itemId, voice == null ? null : voice.id());
    }

    static void reset() {
        pending = null;
        recentAmbient.clear();
        nextCheckAt = System.currentTimeMillis() + 3 * 60_000L;
    }

    private static boolean checksEnabled() {
        try {
            return ClientConfig.LISTENING_CHECKS.get();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private static long checkIntervalMs() {
        try {
            return ClientConfig.LISTENING_CHECK_MINUTES.get() * 60_000L;
        } catch (IllegalStateException e) {
            return 5 * 60_000L;
        }
    }

    private static boolean ambientEnabled() {
        try {
            return ClientConfig.AMBIENT_SPEECH.get();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private static long ambientIntervalMs() {
        try {
            return ClientConfig.AMBIENT_SPEECH_SECONDS.get() * 1000L;
        } catch (IllegalStateException e) {
            return 45_000L;
        }
    }

    /** Whether a listening check is waiting for an answer (the HUD shows it). */
    public static boolean checkPending() {
        return pending != null;
    }

    // ------------------------------------------------------------------ in the world

    /** Called once a second in-world: may start a listening check when the player isn't busy. */
    static void tick(Minecraft mc) {
        long now = System.currentTimeMillis();
        if (pending != null && now > pendingUntil) pending = null;
        if (!checksEnabled() || pending != null || now < nextCheckAt) return;
        // Only when nothing else is going on: no screen, no study prompt, not fighting, not talking.
        if (mc.player == null || mc.screen != null || Encounters.pending() != null || VoiceService.isRecording()
                || mc.player.hurtTime > 0 || !available()) return;
        LearningEngine engine = Learner.engine();
        String card = engine.listeningCheck();
        if (card == null) {
            nextCheckAt = now + 60_000L;
            return;
        }
        StudySession session = engine.startListenCheck(card);
        Question q = session == null ? null : session.question();
        if (q == null || q.speak == null) {
            nextCheckAt = now + 60_000L;
            return;
        }
        Speaker voice = voiceFor(q.itemId, true);
        q.voice = voice == null ? null : voice.id();
        pending = session;
        pendingUntil = now + CHECK_SHOW_MS;
        nextCheckAt = now + checkIntervalMs();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 1.2F, 0.5F));
        say(q.itemId, q.speak, voice, false);
    }

    /**
     * Something whose word the learner knows was just met in the world: sometimes say the word, so the sound
     * is heard in context, in a rotating voice.
     */
    static void ambient(List<Curriculum.VocabItem> words) {
        if (!ambientEnabled() || pending != null || !available()) return;
        long now = System.currentTimeMillis();
        if (now < nextAmbientAt) return;
        LearningEngine engine = Learner.engine();
        for (Curriculum.VocabItem v : words) {
            if (!engine.isIntroduced(v.id)) continue;
            Long last = recentAmbient.get(v.id);
            if (last != null && now - last < SAME_WORD_MS) continue;
            recentAmbient.put(v.id, now);
            nextAmbientAt = now + ambientIntervalMs();
            say(v.id, v.kana);
            return;
        }
    }

    /** The field-study key while a listening check is up: opens it. Returns false if no check is pending. */
    static boolean onKey(Minecraft mc) {
        if (pending == null) return false;
        StudySession session = pending;
        pending = null;
        // It was just heard in the world, so the screen doesn't play it again unless asked to.
        mc.setScreen(new StudyScreen(null, session, true));
        return true;
    }
}
