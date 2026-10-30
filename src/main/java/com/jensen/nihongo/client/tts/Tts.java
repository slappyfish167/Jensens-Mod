package com.jensen.nihongo.client.tts;

import com.jensen.nihongo.NihongoMod;
import com.jensen.nihongo.client.ClientConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Speaks Japanese through the operating system's own speech synthesiser, so the mod needs no
 * bundled audio: Windows SAPI (picks an installed ja-JP voice), macOS `say -v Kyoko`, or
 * espeak-ng / spd-say on Linux. A custom command can be set in the config.
 */
public final class Tts {
    private Tts() {}

    private static volatile Process current;
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

    public static void speak(String japanese) {
        if (japanese == null || japanese.isBlank() || !enabled()) return;
        String text = japanese.replace("___", "").trim();
        Thread t = new Thread(() -> run(text), "Nihongo-TTS");
        t.setDaemon(true);
        t.start();
    }

    private static void run(String text) {
        Process previous = current;
        if (previous != null && previous.isAlive()) previous.destroy();
        for (List<String> cmd : commands(text)) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
                pb.environment().put("NIHONGO_TTS_TEXT", text);
                Process p = pb.start();
                current = p;
                p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
                if (p.waitFor(30, TimeUnit.SECONDS) && p.exitValue() == 0) {
                    lastError = "";
                    return;
                }
                lastError = "Speech command failed: " + cmd.get(0);
            } catch (Exception e) {
                lastError = "Speech unavailable: " + cmd.get(0) + " (" + e.getMessage() + ")";
            }
        }
        NihongoMod.LOGGER.debug("TTS failed: {}", lastError);
    }

    private static List<List<String>> commands(String text) {
        List<List<String>> out = new ArrayList<>();
        String custom = "";
        try {
            custom = ClientConfig.TTS_COMMAND.get().trim();
        } catch (IllegalStateException ignored) {
        }
        if (!custom.isEmpty()) {
            List<String> cmd = new ArrayList<>();
            for (String part : custom.split("\\s+")) cmd.add(part.equals("{text}") ? text : part);
            out.add(cmd);
            return out;
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            // The text is passed through an environment variable so it never needs escaping.
            String script = "Add-Type -AssemblyName System.Speech;"
                    + "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer;"
                    + "$v = $s.GetInstalledVoices() | Where-Object { $_.VoiceInfo.Culture.Name -eq 'ja-JP' } | Select-Object -First 1;"
                    + "if ($v) { $s.SelectVoice($v.VoiceInfo.Name) } else { exit 3 };"
                    + "$s.Rate = -2; $s.Speak($env:NIHONGO_TTS_TEXT)";
            out.add(List.of("powershell", "-NoProfile", "-NonInteractive", "-Command", script));
        } else if (os.contains("mac")) {
            out.add(List.of("say", "-v", "Kyoko", text));
            out.add(List.of("say", "-v", "Otoya", text));
        } else {
            out.add(List.of("espeak-ng", "-v", "ja", text));
            out.add(List.of("spd-say", "-w", "-l", "ja", text));
        }
        return out;
    }
}
