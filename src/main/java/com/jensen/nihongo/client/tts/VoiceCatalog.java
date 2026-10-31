package com.jensen.nihongo.client.tts;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jensen.nihongo.NihongoMod;
import com.jensen.nihongo.client.ClientConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every Japanese voice the mod can speak with. Hearing the same word from different speakers is what lets
 * a learner recognise the word itself rather than one speaker's way of saying it, so the catalog gathers:
 * <ul>
 *   <li>the operating system's Japanese voices (Windows, macOS, espeak-ng on Linux);</li>
 *   <li>styled variants of them (slower and lower "narrator", higher "younger", faster "energetic"), where
 *       the engine can change rate or pitch, so even one installed voice gives some variety;</li>
 *   <li>speakers of a local VOICEVOX engine, if one is running (many natural, anime-style voices);</li>
 *   <li>custom commands from the config.</li>
 * </ul>
 * Discovery runs in the background; {@link #active()} is the list the learner has switched on, primary first.
 */
public final class VoiceCatalog {
    private VoiceCatalog() {}

    public enum Engine { ONECORE, SAPI, SAY, ESPEAK, SPD, VOICEVOX, CUSTOM }

    /** Delivery styles layered on a base voice: speaking-rate and pitch multipliers. */
    public enum Style {
        STANDARD("", 1.0, 1.0),
        NARRATOR("narrator: slow and calm", 0.82, 0.85),
        YOUNGER("younger: higher pitch", 1.06, 1.3),
        ENERGETIC("energetic: fast and bright", 1.25, 1.12);

        public final String label;
        public final double rate;
        public final double pitch;

        Style(String label, double rate, double pitch) {
            this.label = label;
            this.rate = rate;
            this.pitch = pitch;
        }
    }

    /**
     * One voice the learner can hear.
     *
     * @param voice  the engine's name for the base voice (for VOICEVOX, the style id; for CUSTOM, the command)
     * @param credit attribution to show when it speaks, if the voice's terms ask for one
     */
    public record Speaker(String id, String name, String role, Engine engine, String voice, Style style, String credit) {
        public String label() {
            return role.isEmpty() ? name : name + " - " + role;
        }
    }

    public enum State { SCANNING, READY }

    private static volatile List<Speaker> all = List.of();
    private static volatile State state = State.READY;
    private static volatile boolean scannedOnce;

    public static State state() {
        return state;
    }

    /** Every voice found, enabled or not. */
    public static List<Speaker> all() {
        if (!scannedOnce) refresh();
        return all;
    }

    public static Speaker byId(String id) {
        for (Speaker s : all) if (s.id().equals(id)) return s;
        return null;
    }

    /** Voices switched on, with the primary voice first. Empty if text-to-speech is off or no voice was found. */
    public static List<Speaker> active() {
        if (!Tts.enabled()) return List.of();
        List<Speaker> out = new ArrayList<>();
        List<? extends String> disabled = disabledIds();
        String primary = primaryId();
        for (Speaker s : all()) {
            if (disabled.contains(s.id())) continue;
            if (s.id().equals(primary)) out.add(0, s);
            else out.add(s);
        }
        return out;
    }

    public static Speaker primary() {
        List<Speaker> a = active();
        return a.isEmpty() ? null : a.get(0);
    }

    public static boolean isEnabled(Speaker s) {
        return !disabledIds().contains(s.id());
    }

    public static boolean isPrimary(Speaker s) {
        Speaker p = primary();
        return p != null && p.id().equals(s.id());
    }

    public static void setEnabled(Speaker s, boolean enabled) {
        List<String> disabled = new ArrayList<>(disabledIds());
        disabled.remove(s.id());
        if (!enabled) disabled.add(s.id());
        ClientConfig.DISABLED_VOICES.set(disabled);
        ClientConfig.DISABLED_VOICES.save();
    }

    public static void setPrimary(Speaker s) {
        ClientConfig.PRIMARY_VOICE.set(s.id());
        ClientConfig.PRIMARY_VOICE.save();
        if (!isEnabled(s)) setEnabled(s, true);
    }

    private static List<? extends String> disabledIds() {
        try {
            return ClientConfig.DISABLED_VOICES.get();
        } catch (IllegalStateException e) {
            return List.of();
        }
    }

    private static String primaryId() {
        try {
            return ClientConfig.PRIMARY_VOICE.get();
        } catch (IllegalStateException e) {
            return "";
        }
    }

    /** Looks for voices again in the background (e.g. after installing one or starting VOICEVOX). */
    public static synchronized void refresh() {
        if (state == State.SCANNING) return;
        scannedOnce = true;
        state = State.SCANNING;
        Thread t = new Thread(() -> {
            try {
                all = List.copyOf(scan());
                NihongoMod.LOGGER.info("Japanese voices found: {}", all.size());
            } catch (Throwable e) {
                NihongoMod.LOGGER.warn("Could not list Japanese voices", e);
            } finally {
                state = State.READY;
            }
        }, "Nihongo-Voices");
        t.setDaemon(true);
        t.start();
    }

    // ------------------------------------------------------------------ discovery

    private record Base(Engine engine, String voice, String name, String gender) {}

    private static List<Speaker> scan() {
        List<Speaker> out = new ArrayList<>();
        String custom = config(ClientConfig.TTS_COMMAND, "").trim();
        if (!custom.isEmpty()) {
            // A custom command replaces the system voices (as before multiple voices existed).
            out.add(new Speaker("custom", "Custom voice", "", Engine.CUSTOM, custom, Style.STANDARD, ""));
        } else {
            out.addAll(withStyles(systemVoices()));
        }
        List<? extends String> extra;
        try {
            extra = ClientConfig.EXTRA_VOICES.get();
        } catch (IllegalStateException e) {
            extra = List.of();
        }
        for (String line : extra) {
            int bar = line.indexOf('|');
            if (bar <= 0 || bar == line.length() - 1) continue;
            String name = line.substring(0, bar).trim();
            out.add(new Speaker("custom:" + name, name, "", Engine.CUSTOM, line.substring(bar + 1).trim(), Style.STANDARD, ""));
        }
        boolean voicevox;
        try {
            voicevox = ClientConfig.VOICEVOX.get();
        } catch (IllegalStateException e) {
            voicevox = false;
        }
        if (voicevox) out.addAll(voicevoxSpeakers());
        return out;
    }

    /** Standard delivery for every voice, plus styled variants of the first female and first male voice. */
    private static List<Speaker> withStyles(List<Base> bases) {
        List<Speaker> out = new ArrayList<>();
        for (Base b : bases) out.add(speaker(b, Style.STANDARD));
        List<Base> styled = new ArrayList<>();
        for (String gender : List.of("female", "male")) {
            for (Base b : bases) {
                if (b.gender().equals(gender)) {
                    styled.add(b);
                    break;
                }
            }
        }
        if (styled.isEmpty() && !bases.isEmpty()) styled.add(bases.get(0));
        for (Base b : styled) {
            boolean pitch = b.engine() == Engine.ONECORE || b.engine() == Engine.ESPEAK;
            boolean rate = pitch || b.engine() == Engine.SAPI || b.engine() == Engine.SAY;
            if (rate) out.add(speaker(b, Style.NARRATOR));
            if (pitch) out.add(speaker(b, Style.YOUNGER));
            if (rate) out.add(speaker(b, Style.ENERGETIC));
        }
        return out;
    }

    private static Speaker speaker(Base b, Style style) {
        String role = style == Style.STANDARD ? (b.gender().isEmpty() ? "" : "adult " + b.gender()) : style.label;
        String id = b.engine().name().toLowerCase(Locale.ROOT) + ":" + b.voice() + (style == Style.STANDARD ? "" : "/" + style.name().toLowerCase(Locale.ROOT));
        return new Speaker(id, b.name(), role, b.engine(), b.voice(), style, "");
    }

    private static List<Base> systemVoices() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return windowsVoices();
        if (os.contains("mac")) return macVoices();
        return linuxVoices();
    }

    private static List<Base> windowsVoices() {
        String script = "$ErrorActionPreference = 'SilentlyContinue';"
                + "try { Add-Type -AssemblyName System.Runtime.WindowsRuntime;"
                + "[void][Windows.Media.SpeechSynthesis.SpeechSynthesizer,Windows.Media.SpeechSynthesis,ContentType=WindowsRuntime];"
                + "[Windows.Media.SpeechSynthesis.SpeechSynthesizer]::AllVoices | Where-Object { $_.Language -like 'ja*' } |"
                + " ForEach-Object { 'onecore|' + $_.DisplayName + '|' + $_.Gender } } catch {};"
                + "try { Add-Type -AssemblyName System.Speech; $s = New-Object System.Speech.Synthesis.SpeechSynthesizer;"
                + "$s.GetInstalledVoices() | Where-Object { $_.VoiceInfo.Culture.Name -eq 'ja-JP' } |"
                + " ForEach-Object { 'sapi|' + $_.VoiceInfo.Name + '|' + $_.VoiceInfo.Gender } } catch {}";
        Map<String, Base> byName = new LinkedHashMap<>();
        for (String line : run(List.of("powershell", "-NoProfile", "-NonInteractive", "-Command", script))) {
            String[] p = line.trim().split("\\|");
            if (p.length < 3) continue;
            Engine engine = p[0].equals("onecore") ? Engine.ONECORE : Engine.SAPI;
            // "Microsoft Haruka Desktop" and "Microsoft Haruka" are the same speaker; keep the first (OneCore).
            String shortName = p[1].replace("Microsoft", "").replace("Desktop", "").replace("- Japanese", "").trim();
            byName.putIfAbsent(shortName, new Base(engine, p[1], shortName, gender(p[2])));
        }
        return new ArrayList<>(byName.values());
    }

    private static final Pattern SAY_LINE = Pattern.compile("^(.+?)\\s{2,}(ja[_-]JP)\\s");
    private static final List<String> MAC_MALE = List.of("Otoya", "Hattori");

    private static List<Base> macVoices() {
        List<Base> out = new ArrayList<>();
        for (String line : run(List.of("say", "-v", "?"))) {
            Matcher m = SAY_LINE.matcher(line);
            if (!m.find()) continue;
            String name = m.group(1).trim();
            String first = name.split("[ (]")[0];
            out.add(new Base(Engine.SAY, name, name, MAC_MALE.contains(first) ? "male" : "female"));
        }
        return out;
    }

    private static List<Base> linuxVoices() {
        List<Base> out = new ArrayList<>();
        if (!run(List.of("espeak-ng", "--version")).isEmpty()) {
            out.add(new Base(Engine.ESPEAK, "ja", "espeak-ng", "male"));
            out.add(new Base(Engine.ESPEAK, "ja+f3", "espeak-ng", "female"));
        } else if (!run(List.of("spd-say", "--version")).isEmpty()) {
            out.add(new Base(Engine.SPD, "ja", "Speech Dispatcher", ""));
        }
        return out;
    }

    private static String gender(String s) {
        String g = s.toLowerCase(Locale.ROOT);
        if (g.contains("female")) return "female";
        if (g.contains("male")) return "male";
        return "";
    }

    /** Runs a command and returns its output lines (empty if it is missing or fails). */
    private static List<String> run(List<String> cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String text = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            if (!p.waitFor(20, TimeUnit.SECONDS)) {
                p.destroy();
                return List.of();
            }
            if (p.exitValue() != 0) return List.of();
            List<String> lines = new ArrayList<>();
            for (String l : text.split("\\R")) if (!l.isBlank()) lines.add(l);
            return lines;
        } catch (Exception e) {
            return List.of();
        }
    }

    // ------------------------------------------------------------------ VOICEVOX

    /**
     * VOICEVOX characters to prefer, chosen to cover different kinds of speaker, with a short description.
     * Any others the engine has are used too, up to the configured maximum.
     */
    private static final Map<String, String> VOICEVOX_ROLES = new LinkedHashMap<>();

    static {
        VOICEVOX_ROLES.put("青山龍星", "deep adult male, narrator-like");
        VOICEVOX_ROLES.put("四国めたん", "young woman, anime style");
        VOICEVOX_ROLES.put("玄野武宏", "adult male");
        VOICEVOX_ROLES.put("ずんだもん", "energetic, anime mascot");
        VOICEVOX_ROLES.put("冥鳴ひまり", "calm woman");
        VOICEVOX_ROLES.put("白上虎太郎", "boy, energetic");
        VOICEVOX_ROLES.put("春日部つむぎ", "cheerful girl");
        VOICEVOX_ROLES.put("九州そら", "adult woman");
    }

    static String voicevoxUrl() {
        String url = config(ClientConfig.VOICEVOX_URL, "http://127.0.0.1:50021").trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
            .proxy(java.net.ProxySelector.of(null)).build();

    private static List<Speaker> voicevoxSpeakers() {
        List<Speaker> preferred = new ArrayList<>();
        List<Speaker> others = new ArrayList<>();
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(voicevoxUrl() + "/speakers")).timeout(Duration.ofSeconds(3)).GET().build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return List.of();
            for (JsonElement e : JsonParser.parseString(res.body()).getAsJsonArray()) {
                JsonObject sp = e.getAsJsonObject();
                String name = sp.get("name").getAsString();
                JsonObject style = null;
                for (JsonElement st : sp.getAsJsonArray("styles")) {
                    JsonObject o = st.getAsJsonObject();
                    if (style == null || o.get("name").getAsString().equals("ノーマル")) style = o;
                    if (o.get("name").getAsString().equals("ノーマル")) break;
                }
                if (style == null) continue;
                String id = style.get("id").getAsString();
                String role = VOICEVOX_ROLES.getOrDefault(name, "VOICEVOX");
                Speaker s = new Speaker("voicevox:" + id, name, role, Engine.VOICEVOX, id, Style.STANDARD, "VOICEVOX:" + name);
                (VOICEVOX_ROLES.containsKey(name) ? preferred : others).add(s);
            }
        } catch (Exception e) {
            // No engine running: that's the normal case.
            return List.of();
        }
        List<String> order = new ArrayList<>(VOICEVOX_ROLES.keySet());
        preferred.sort(java.util.Comparator.comparingInt(s -> order.indexOf(s.name())));
        int max;
        try {
            max = ClientConfig.VOICEVOX_MAX.get();
        } catch (IllegalStateException e) {
            max = 8;
        }
        List<Speaker> out = new ArrayList<>(preferred);
        out.addAll(others);
        return out.subList(0, Math.min(max, out.size()));
    }

    private static String config(net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<String> value, String fallback) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }
}
