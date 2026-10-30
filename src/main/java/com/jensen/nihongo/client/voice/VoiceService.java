package com.jensen.nihongo.client.voice;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jensen.nihongo.NihongoMod;
import com.jensen.nihongo.client.ClientConfig;
import com.jensen.nihongo.client.Learner;
import net.minecraft.client.Minecraft;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Offline Japanese speech recognition with Vosk. Nothing leaves the computer: the model is
 * downloaded once and recognition runs locally.
 *
 * Each recording is decoded twice in parallel: once constrained to a small list of expected words
 * (much more forgiving of a learner's accent) and once unconstrained (so a wrong word is still
 * heard as a wrong word).
 */
public final class VoiceService {
    private VoiceService() {}

    public enum State { DISABLED, NO_MODEL, DOWNLOADING, LOADING, READY, ERROR }

    private static final float SAMPLE_RATE = 16000f;
    private static final long MAX_RECORD_MS = 7000;

    private static volatile State state = State.NO_MODEL;
    private static volatile String status = "";
    private static volatile double downloadProgress;
    private static volatile boolean recording;
    private static volatile float level;
    private static Model model;

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Nihongo-Voice");
        t.setDaemon(true);
        return t;
    });

    public static State state() {
        if (!enabled()) return State.DISABLED;
        return state;
    }

    public static String status() {
        return status;
    }

    public static double downloadProgress() {
        return downloadProgress;
    }

    public static boolean isReady() {
        return state() == State.READY;
    }

    public static boolean isRecording() {
        return recording;
    }

    /** Microphone input level 0..1, for the UI meter. */
    public static float level() {
        return level;
    }

    private static boolean enabled() {
        try {
            return ClientConfig.VOICE.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }

    private static String modelUrl() {
        try {
            return ClientConfig.VOICE_MODEL_URL.get();
        } catch (IllegalStateException e) {
            return "https://alphacephei.com/vosk/models/vosk-model-small-ja-0.22.zip";
        }
    }

    static Path voiceDir() {
        return Learner.dataDir().resolve("voice");
    }

    /** The model folder: the zip's file name without ".zip". */
    static Path modelDir() {
        String url = modelUrl();
        String name = url.substring(url.lastIndexOf('/') + 1).replace(".zip", "");
        return voiceDir().resolve(name);
    }

    private static boolean modelPresent() {
        Path dir = modelDir();
        if (!Files.isDirectory(dir)) return false;
        try (Stream<Path> files = Files.list(dir)) {
            return files.findAny().isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    /** Called at startup: loads the model in the background if it was downloaded before. */
    public static void initFromDisk() {
        if (enabled() && modelPresent()) load();
    }

    public static void download() {
        if (state == State.DOWNLOADING || state == State.LOADING) return;
        state = State.DOWNLOADING;
        downloadProgress = 0;
        status = "Downloading Japanese speech model...";
        WORKER.submit(() -> {
            try {
                ModelDownloader.download(modelUrl(), voiceDir(), p -> downloadProgress = p);
                loadNow();
            } catch (Exception e) {
                fail("Download failed: " + e.getMessage(), e);
            }
        });
    }

    private static void load() {
        state = State.LOADING;
        status = "Loading speech model...";
        WORKER.submit(VoiceService::loadNow);
    }

    private static void loadNow() {
        state = State.LOADING;
        try {
            LibVosk.setLogLevel(LogLevel.WARNINGS);
            if (model != null) model.close();
            model = new Model(modelDir().toString());
            state = State.READY;
            status = "Voice recognition ready.";
        } catch (Throwable t) {
            // Includes UnsatisfiedLinkError on platforms without a native library (e.g. ARM Linux).
            fail("Could not start voice recognition: " + t, t);
        }
    }

    private static void fail(String message, Throwable t) {
        state = State.ERROR;
        status = message;
        NihongoMod.LOGGER.error(message, t);
    }

    /**
     * Starts listening. Results arrive on the main thread as a list of transcripts, best first.
     *
     * @param expected words to listen for (may be empty for free recognition)
     */
    public static void startRecording(List<String> expected, Consumer<List<String>> onResult) {
        if (!isReady() || recording) return;
        recording = true;
        level = 0;
        List<String> grammar = new ArrayList<>(new LinkedHashSet<>(expected));
        WORKER.submit(() -> {
            List<String> transcripts = new ArrayList<>();
            try {
                transcripts = record(grammar);
            } catch (Throwable t) {
                NihongoMod.LOGGER.error("Voice recording failed", t);
                status = "Microphone error: " + t.getMessage();
            } finally {
                recording = false;
                level = 0;
            }
            List<String> result = transcripts;
            Minecraft.getInstance().execute(() -> onResult.accept(result));
        });
    }

    public static void stopRecording() {
        recording = false;
    }

    private static List<String> record(List<String> grammar) throws Exception {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) throw new IllegalStateException("No microphone found");
        List<String> out = new ArrayList<>();
        Recognizer constrained = null;
        try (TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
             Recognizer free = new Recognizer(model, SAMPLE_RATE)) {
            free.setMaxAlternatives(3);
            if (!grammar.isEmpty()) {
                List<String> g = new ArrayList<>(grammar);
                g.add("[unk]");
                constrained = new Recognizer(model, SAMPLE_RATE, new Gson().toJson(g));
                constrained.setMaxAlternatives(3);
            }
            line.open(format);
            line.start();
            byte[] buf = new byte[3200];
            long start = System.currentTimeMillis();
            List<String> partials = new ArrayList<>();
            while (recording && System.currentTimeMillis() - start < MAX_RECORD_MS) {
                int n = line.read(buf, 0, buf.length);
                if (n <= 0) continue;
                level = rms(buf, n);
                if (constrained != null && constrained.acceptWaveForm(buf, n)) partials.addAll(texts(constrained.getResult()));
                if (free.acceptWaveForm(buf, n)) partials.addAll(texts(free.getResult()));
            }
            line.stop();
            if (constrained != null) out.addAll(texts(constrained.getFinalResult()));
            out.addAll(texts(free.getFinalResult()));
            out.addAll(partials);
        } finally {
            if (constrained != null) constrained.close();
        }
        out.removeIf(s -> s.isBlank() || s.equals("[unk]"));
        return out;
    }

    private static float rms(byte[] buf, int n) {
        double sum = 0;
        int samples = n / 2;
        for (int i = 0; i + 1 < n; i += 2) {
            short s = (short) ((buf[i] & 0xff) | (buf[i + 1] << 8));
            sum += (double) s * s;
        }
        double r = Math.sqrt(sum / Math.max(1, samples)) / 32768.0;
        return (float) Math.min(1.0, r * 4);
    }

    /** Extracts transcript strings from a Vosk result (with or without alternatives). */
    static List<String> texts(String json) {
        List<String> out = new ArrayList<>();
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            if (o.has("alternatives")) {
                for (JsonElement e : o.getAsJsonArray("alternatives")) {
                    out.add(e.getAsJsonObject().get("text").getAsString().replace("[unk]", "").trim());
                }
            } else if (o.has("text")) {
                out.add(o.get("text").getAsString().replace("[unk]", "").trim());
            }
        } catch (Exception ignored) {
        }
        return out;
    }
}
