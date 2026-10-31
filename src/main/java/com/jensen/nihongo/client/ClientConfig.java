package com.jensen.nihongo.client;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class ClientConfig {
    private ClientConfig() {}

    public enum RomajiMode { NEVER, WEAK_WORDS, ALWAYS }

    /** How often voices other than the primary one read words aloud. */
    public enum VoiceVariety {
        ONE_VOICE(0.0), SOMETIMES(0.4), OFTEN(0.75);

        public final double chance;

        VoiceVariety(double chance) {
            this.chance = chance;
        }
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("study");
    }

    public static final ModConfigSpec.BooleanValue DAILY_LIMIT_ENABLED = BUILDER
            .comment("Limit how many new items you can learn per day (off = learn as much as you like).")
            .define("dailyLimitEnabled", false);

    public static final ModConfigSpec.IntValue DAILY_NEW_LIMIT = BUILDER
            .comment("New items (kana, words, grammar points, kanji) introduced per day, when the daily limit is on.")
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

    public static final ModConfigSpec.BooleanValue FIELD_PROMPTS = BUILDER
            .comment("When you mine, collect, attack or look at something whose word is due for review (or not learned yet),",
                    "offer a quick question about it right there. Press the field-study key (K) to answer.")
            .define("fieldPrompts", true);

    public static final ModConfigSpec.IntValue FIELD_PROMPT_SECONDS = BUILDER
            .comment("Minimum seconds between field-study prompts.")
            .defineInRange("fieldPromptCooldownSeconds", 30, 5, 3600);

    public static final ModConfigSpec.BooleanValue LISTENING_CHECKS = BUILDER
            .comment("Now and then, say a word or phrase you have heard several times and ask what it was (press K to answer).",
                    "Listening is tracked separately from reading.")
            .define("listeningChecks", true);

    public static final ModConfigSpec.IntValue LISTENING_CHECK_MINUTES = BUILDER
            .comment("Minimum minutes between in-world listening checks.")
            .defineInRange("listeningCheckMinutes", 5, 1, 240);

    public static final ModConfigSpec.BooleanValue AMBIENT_SPEECH = BUILDER
            .comment("Say the Japanese word for things you know when you meet them in the world (mining, collecting, looking).")
            .define("ambientSpeech", true);

    public static final ModConfigSpec.IntValue AMBIENT_SPEECH_SECONDS = BUILDER
            .comment("Minimum seconds between spoken words in the world.")
            .defineInRange("ambientSpeechSeconds", 45, 5, 3600);

    static {
        BUILDER.pop().push("audio");
    }

    public static final ModConfigSpec.BooleanValue TTS = BUILDER
            .comment("Read Japanese aloud with your operating system's speech synthesiser (needs a Japanese voice installed).")
            .define("textToSpeech", true);

    public static final ModConfigSpec.ConfigValue<String> TTS_COMMAND = BUILDER
            .comment("Optional custom text-to-speech command. Use {text} where the Japanese text goes, e.g. 'espeak-ng -v ja {text}'.")
            .define("ttsCommand", "");

    public static final ModConfigSpec.EnumValue<VoiceVariety> VOICE_VARIETY = BUILDER
            .comment("How often other voices read words instead of your primary voice: ONE_VOICE, SOMETIMES or OFTEN.",
                    "Hearing many speakers helps you recognise the word itself, not one speaker's voice.")
            .defineEnum("voiceVariety", VoiceVariety.SOMETIMES);

    public static final ModConfigSpec.ConfigValue<String> PRIMARY_VOICE = BUILDER
            .comment("Id of the voice that reads new words first (set it from Voice & audio > Voices).")
            .define("primaryVoice", "");

    public static final ModConfigSpec.ConfigValue<List<? extends String>> DISABLED_VOICES = BUILDER
            .comment("Ids of voices that are switched off.")
            .defineListAllowEmpty("disabledVoices", List.of(), () -> "", o -> o instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_VOICES = BUILDER
            .comment("Extra voices as 'Name|command', with {text} where the Japanese goes, e.g. 'Piper|my-piper-script {text}'.")
            .defineListAllowEmpty("extraVoices", List.of(), () -> "", o -> o instanceof String);

    public static final ModConfigSpec.BooleanValue VOICEVOX = BUILDER
            .comment("Use the speakers of a VOICEVOX engine running on this computer, if there is one (free, many natural voices).")
            .define("voicevox", true);

    public static final ModConfigSpec.ConfigValue<String> VOICEVOX_URL = BUILDER
            .comment("Address of the VOICEVOX engine (or a compatible one).")
            .define("voicevoxUrl", "http://127.0.0.1:50021");

    public static final ModConfigSpec.IntValue VOICEVOX_MAX = BUILDER
            .comment("Most VOICEVOX speakers to use.")
            .defineInRange("voicevoxMaxSpeakers", 8, 1, 50);

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
