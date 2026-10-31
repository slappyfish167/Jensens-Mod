package com.jensen.nihongo.client.tts;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jensen.nihongo.NihongoMod;
import com.jensen.nihongo.client.ClientConfig;
import com.jensen.nihongo.client.tts.VoiceCatalog.Engine;
import com.jensen.nihongo.client.tts.VoiceCatalog.Speaker;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Speaks Japanese, so the mod needs no bundled audio: through the operating system's own speech
 * synthesiser (Windows OneCore/SAPI voices, macOS `say`, espeak-ng / spd-say on Linux), a local
 * VOICEVOX engine, or a custom command from the config. {@link VoiceCatalog} lists the voices;
 * {@link #speak(String, Speaker, boolean)} reads text in one of them.
 */
public final class Tts {
    private Tts() {}

    private static final int NO_VOICE = 3;
    private static final String NO_VOICE_HELP = "No Japanese voice found. Windows: Settings > Time & language > Speech > "
            + "Add voices > Japanese. macOS: install Kyoko. Linux: install espeak-ng.";
    /** Slow replays play at this fraction of the voice's usual speed. */
    private static final double SLOW = 0.7;

    private static volatile Process current;
    private static volatile Clip currentClip;
    private static volatile String lastError = "";

    public static String lastError() {
        return lastError;
    }

    public static boolean enabled() {
        try {
            return ClientConfig.TTS.get();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /** Speaks in the primary voice. */
    public static void speak(String japanese) {
        speak(japanese, VoiceCatalog.primary(), false);
    }

    /** Speaks in the given voice (or the system default if null), optionally slowed down. */
    public static void speak(String japanese, Speaker speaker, boolean slow) {
        if (japanese == null || japanese.isBlank() || !enabled()) return;
        String text = japanese.replace("___", "").trim();
        Thread t = new Thread(() -> run(text, speaker, slow), "Nihongo-TTS");
        t.setDaemon(true);
        t.start();
    }

    private static void stopCurrent() {
        Process previous = current;
        if (previous != null && previous.isAlive()) previous.destroy();
        Clip clip = currentClip;
        if (clip != null) clip.stop();
    }

    private static void run(String text, Speaker speaker, boolean slow) {
        stopCurrent();
        if (speaker != null && speaker.engine() == Engine.VOICEVOX) {
            try {
                voicevox(text, speaker, slow);
                lastError = "";
            } catch (Exception e) {
                lastError = "VOICEVOX is not answering (" + e.getMessage() + "). Start it, or switch that voice off.";
                NihongoMod.LOGGER.warn("TTS failed: {}", lastError);
            }
            return;
        }
        boolean noVoice = false;
        for (List<String> cmd : commands(text, speaker, slow)) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
                pb.environment().put("NIHONGO_TTS_TEXT", text);
                if (speaker != null) {
                    double rate = speaker.style().rate * (slow ? SLOW : 1.0);
                    pb.environment().put("NIHONGO_TTS_VOICE", speaker.voice());
                    pb.environment().put("NIHONGO_TTS_RATE", speaker.engine() == Engine.SAPI
                            ? Integer.toString(sapiRate(rate)) : String.format(Locale.ROOT, "%.2f", 0.9 * rate));
                    pb.environment().put("NIHONGO_TTS_PITCH", String.format(Locale.ROOT, "%.2f", Math.min(2.0, speaker.style().pitch)));
                }
                Process p = pb.start();
                current = p;
                p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
                if (p.waitFor(30, TimeUnit.SECONDS) && p.exitValue() == 0) {
                    lastError = "";
                    return;
                }
                if (p.isAlive()) p.destroy();
                else if (p.exitValue() == NO_VOICE) noVoice = true;
                lastError = "Speech command failed: " + cmd.get(0);
            } catch (Exception e) {
                lastError = "Speech unavailable: " + cmd.get(0) + " (" + e.getMessage() + ")";
            }
        }
        if (noVoice) lastError = NO_VOICE_HELP;
        NihongoMod.LOGGER.warn("TTS failed: {}", lastError);
    }

    /** System.Speech rate: -10..10, where the mod's normal pace is -2 (a little slower than default). */
    private static int sapiRate(double rate) {
        return (int) Math.max(-10, Math.min(10, Math.round(-2 + (rate - 1.0) * 10)));
    }

    // The text, voice, rate and pitch are passed through environment variables so they never need escaping.
    private static final String SAPI = "Add-Type -AssemblyName System.Speech;"
            + "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer;"
            + "$all = $s.GetInstalledVoices() | Where-Object { $_.VoiceInfo.Culture.Name -eq 'ja-JP' };"
            + "$v = $all | Where-Object { $_.VoiceInfo.Name -eq $env:NIHONGO_TTS_VOICE } | Select-Object -First 1;"
            + "if (-not $v) { $v = $all | Select-Object -First 1 };"
            + "if ($v) { $s.SelectVoice($v.VoiceInfo.Name) } else { exit 3 };"
            + "$s.Rate = -2; if ($env:NIHONGO_TTS_RATE) { $s.Rate = [int]$env:NIHONGO_TTS_RATE };"
            + "$s.Speak($env:NIHONGO_TTS_TEXT)";

    // Voices added through Windows Settings are "OneCore" voices that System.Speech cannot see.
    private static final String ONECORE = "Add-Type -AssemblyName System.Runtime.WindowsRuntime;"
            + "[void][Windows.Media.SpeechSynthesis.SpeechSynthesizer,Windows.Media.SpeechSynthesis,ContentType=WindowsRuntime];"
            + "$s = New-Object Windows.Media.SpeechSynthesis.SpeechSynthesizer;"
            + "$all = [Windows.Media.SpeechSynthesis.SpeechSynthesizer]::AllVoices | Where-Object { $_.Language -like 'ja*' };"
            + "$v = $all | Where-Object { $_.DisplayName -eq $env:NIHONGO_TTS_VOICE } | Select-Object -First 1;"
            + "if (-not $v) { $v = $all | Select-Object -First 1 };"
            + "if (-not $v) { exit 3 };"
            + "$s.Voice = $v;"
            + "try { if ($env:NIHONGO_TTS_RATE) { $s.Options.SpeakingRate = [double]$env:NIHONGO_TTS_RATE };"
            + " if ($env:NIHONGO_TTS_PITCH) { $s.Options.AudioPitch = [double]$env:NIHONGO_TTS_PITCH } } catch {};"
            + "$m = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' } | Select-Object -First 1;"
            + "$t = $m.MakeGenericMethod([Windows.Media.SpeechSynthesis.SpeechSynthesisStream]).Invoke($null, @($s.SynthesizeTextToStreamAsync($env:NIHONGO_TTS_TEXT)));"
            + "$t.Wait(-1) | Out-Null;"
            + "$in = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead($t.Result);"
            + "$ms = New-Object System.IO.MemoryStream; $in.CopyTo($ms); $ms.Position = 0;"
            + "(New-Object System.Media.SoundPlayer $ms).PlaySync()";

    private static List<String> powershell(String script) {
        return List.of("powershell", "-NoProfile", "-NonInteractive", "-Command", script);
    }

    /** Commands to try in order. With no speaker, falls back to whatever Japanese voice the system has. */
    private static List<List<String>> commands(String text, Speaker speaker, boolean slow) {
        List<List<String>> out = new ArrayList<>();
        double rate = (speaker == null ? 1.0 : speaker.style().rate) * (slow ? SLOW : 1.0);
        double pitch = speaker == null ? 1.0 : speaker.style().pitch;
        if (speaker != null) {
            switch (speaker.engine()) {
                case ONECORE -> out.add(powershell(ONECORE));
                case SAPI -> out.add(powershell(SAPI));
                case SAY -> out.add(List.of("say", "-v", speaker.voice(), "-r", Long.toString(Math.round(175 * rate)), text));
                case ESPEAK -> out.add(List.of("espeak-ng", "-v", speaker.voice(), "-s", Long.toString(Math.round(150 * rate)),
                        "-p", Long.toString(Math.max(0, Math.min(99, Math.round(50 * pitch)))), text));
                case SPD -> out.add(List.of("spd-say", "-w", "-l", "ja", "-r", Long.toString(Math.round((rate - 1) * 100)), text));
                case CUSTOM -> out.add(custom(speaker.voice(), text));
                default -> {
                }
            }
            return out;
        }
        String custom = "";
        try {
            custom = ClientConfig.TTS_COMMAND.get().trim();
        } catch (IllegalStateException ignored) {
        }
        if (!custom.isEmpty()) {
            out.add(custom(custom, text));
            return out;
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            out.add(powershell(SAPI));
            out.add(powershell(ONECORE));
        } else if (os.contains("mac")) {
            out.add(List.of("say", "-v", "Kyoko", text));
            out.add(List.of("say", "-v", "Otoya", text));
        } else {
            out.add(List.of("espeak-ng", "-v", "ja", text));
            out.add(List.of("spd-say", "-w", "-l", "ja", text));
        }
        return out;
    }

    private static List<String> custom(String command, String text) {
        List<String> cmd = new ArrayList<>();
        for (String part : command.split("\\s+")) cmd.add(part.equals("{text}") ? text : part);
        return cmd;
    }

    /** VOICEVOX: build an audio query for the text, adjust its speed, synthesise a WAV and play it. */
    private static void voicevox(String text, Speaker speaker, boolean slow) throws Exception {
        String base = VoiceCatalog.voicevoxUrl();
        String id = URLEncoder.encode(speaker.voice(), StandardCharsets.UTF_8);
        HttpRequest query = HttpRequest.newBuilder(URI.create(base + "/audio_query?speaker=" + id
                        + "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8)))
                .timeout(Duration.ofSeconds(10)).POST(HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> q = VoiceCatalog.HTTP.send(query, HttpResponse.BodyHandlers.ofString());
        if (q.statusCode() != 200) throw new IllegalStateException("HTTP " + q.statusCode());
        JsonObject body = JsonParser.parseString(q.body()).getAsJsonObject();
        body.addProperty("speedScale", slow ? SLOW : 0.95);
        HttpRequest synth = HttpRequest.newBuilder(URI.create(base + "/synthesis?speaker=" + id))
                .timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        HttpResponse<byte[]> wav = VoiceCatalog.HTTP.send(synth, HttpResponse.BodyHandlers.ofByteArray());
        if (wav.statusCode() != 200) throw new IllegalStateException("HTTP " + wav.statusCode());
        play(wav.body());
    }

    private static void play(byte[] wav) throws Exception {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(new ByteArrayInputStream(wav)))) {
            Clip clip = AudioSystem.getClip();
            CountDownLatch done = new CountDownLatch(1);
            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP) done.countDown();
            });
            clip.open(in);
            currentClip = clip;
            clip.start();
            done.await(30, TimeUnit.SECONDS);
            clip.close();
            if (currentClip == clip) currentClip = null;
        }
    }
}
