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

    private static final int NO_VOICE = 3;
    private static final String NO_VOICE_HELP = "No Japanese voice found. Windows: Settings > Time & language > Speech > "
            + "Add voices > Japanese. macOS: install Kyoko. Linux: install espeak-ng.";

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
        boolean noVoice = false;
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
            // Voices added through Windows Settings are "OneCore" voices that System.Speech cannot see.
            String oneCore = "Add-Type -AssemblyName System.Runtime.WindowsRuntime;"
                    + "[void][Windows.Media.SpeechSynthesis.SpeechSynthesizer,Windows.Media.SpeechSynthesis,ContentType=WindowsRuntime];"
                    + "$s = New-Object Windows.Media.SpeechSynthesis.SpeechSynthesizer;"
                    + "$v = [Windows.Media.SpeechSynthesis.SpeechSynthesizer]::AllVoices | Where-Object { $_.Language -like 'ja*' } | Select-Object -First 1;"
                    + "if (-not $v) { exit 3 };"
                    + "$s.Voice = $v;"
                    + "$m = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' } | Select-Object -First 1;"
                    + "$t = $m.MakeGenericMethod([Windows.Media.SpeechSynthesis.SpeechSynthesisStream]).Invoke($null, @($s.SynthesizeTextToStreamAsync($env:NIHONGO_TTS_TEXT)));"
                    + "$t.Wait(-1) | Out-Null;"
                    + "$in = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead($t.Result);"
                    + "$ms = New-Object System.IO.MemoryStream; $in.CopyTo($ms); $ms.Position = 0;"
                    + "(New-Object System.Media.SoundPlayer $ms).PlaySync()";
            out.add(List.of("powershell", "-NoProfile", "-NonInteractive", "-Command", oneCore));
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
