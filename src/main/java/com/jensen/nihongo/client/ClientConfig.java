package com.jensen.nihongo.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    private ClientConfig() {}

    public enum RomajiMode { NEVER, WEAK_WORDS, ALWAYS }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("study");
    }

    public static final ModConfigSpec.IntValue DAILY_NEW_LIMIT = BUILDER
            .comment("New items (kana, words, grammar points) introduced per day. Small daily doses beat cramming.")
            .defineInRange("dailyNewLimit", 15, 1, 200);

    public static final ModConfigSpec.IntValue MAX_REVIEWS = BUILDER
            .comment("Maximum reviews in one session.")
            .defineInRange("maxReviewsPerSession", 60, 5, 1000);

    public static final ModConfigSpec.IntValue GATE_PERCENT = BUILDER
            .comment("Percent of the previous lesson's cards that must be past the learning stage before the next lesson unlocks.")
            .defineInRange("unlockThresholdPercent", 80, 0, 100);

    public static final ModConfigSpec.BooleanValue FEEDBACK_SOUNDS = BUILDER
            .comment("Play a sound for right and wrong answers.")
            .define("feedbackSounds", true);

    static {
        BUILDER.pop().push("immersion");
    }

    public static final ModConfigSpec.BooleanValue LOOK_LABELS = BUILDER
            .comment("Show the Japanese word for the block or mob you are looking at (only words you have studied).")
            .define("lookAtLabels", true);

    public static final ModConfigSpec.BooleanValue TOOLTIPS = BUILDER
            .comment("Add the Japanese word to item tooltips (only words you have studied).")
            .define("itemTooltips", true);

    public static final ModConfigSpec.EnumValue<RomajiMode> LABEL_ROMAJI = BUILDER
            .comment("Show romaji under labels: NEVER, WEAK_WORDS (only words you don't know well yet) or ALWAYS.")
            .defineEnum("labelRomaji", RomajiMode.WEAK_WORDS);

    public static final ModConfigSpec.BooleanValue QUESTS = BUILDER
            .comment("Give collection quests written in Japanese, using words you have learned.")
            .define("quests", true);

    public static final ModConfigSpec.IntValue QUEST_INTERVAL_MINUTES = BUILDER
            .comment("Minutes between quests.")
            .defineInRange("questIntervalMinutes", 6, 1, 240);

    static {
        BUILDER.pop().push("audio");
    }

    public static final ModConfigSpec.BooleanValue TTS = BUILDER
            .comment("Read Japanese aloud with your operating system's speech synthesiser (needs a Japanese voice installed).")
            .define("textToSpeech", true);

    public static final ModConfigSpec.ConfigValue<String> TTS_COMMAND = BUILDER
            .comment("Optional custom text-to-speech command. Use {text} where the Japanese text goes, e.g. 'espeak-ng -v ja {text}'.")
            .define("ttsCommand", "");

    public static final ModConfigSpec.BooleanValue VOICE = BUILDER
            .comment("Enable offline voice recognition (Vosk) for speaking practice and spells.")
            .define("voiceRecognition", true);

    public static final ModConfigSpec.ConfigValue<String> VOICE_MODEL_URL = BUILDER
            .comment("Download URL of the Vosk Japanese model (zip).")
            .define("voiceModelUrl", "https://alphacephei.com/vosk/models/vosk-model-small-ja-0.22.zip");

    public static final ModConfigSpec.BooleanValue SPELLS = BUILDER
            .comment("Hold the speak key in-world and say a learned word to cast a kotodama spell.")
            .define("voiceSpells", true);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
