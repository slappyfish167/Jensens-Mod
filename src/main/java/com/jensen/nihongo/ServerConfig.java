package com.jensen.nihongo;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Per-world settings for rewards and spells (only matters on the server / singleplayer host). */
public final class ServerConfig {
    private ServerConfig() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SPELLS_ENABLED = BUILDER
            .comment("Allow spoken 'kotodama' spells (say a learned Japanese word to get a short potion effect).")
            .define("spellsEnabled", true);

    public static final ModConfigSpec.IntValue SPELL_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown per spell, in seconds.")
            .defineInRange("spellCooldownSeconds", 90, 0, 3600);

    public static final ModConfigSpec.IntValue XP_PER_CORRECT = BUILDER
            .comment("Experience points per correct answer in a study session.")
            .defineInRange("xpPerCorrectAnswer", 1, 0, 100);

    public static final ModConfigSpec.IntValue MAX_XP_PER_SESSION = BUILDER
            .comment("Maximum experience points from one study session.")
            .defineInRange("maxXpPerSession", 60, 0, 10000);

    public static final ModConfigSpec.IntValue QUEST_XP = BUILDER
            .comment("Experience points for completing a Japanese collection quest.")
            .defineInRange("questXp", 15, 0, 10000);

    public static final ModConfigSpec.IntValue QUEST_EMERALDS = BUILDER
            .comment("Emeralds given for completing a Japanese collection quest.")
            .defineInRange("questEmeralds", 1, 0, 64);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
