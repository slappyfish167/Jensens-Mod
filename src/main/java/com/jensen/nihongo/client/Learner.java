package com.jensen.nihongo.client;

import com.jensen.nihongo.NihongoMod;
import com.jensen.nihongo.learning.Curriculum;
import com.jensen.nihongo.learning.LearningEngine;
import com.jensen.nihongo.learning.McIndex;
import com.jensen.nihongo.learning.Progress;
import com.jensen.nihongo.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Random;
import java.util.UUID;

/** Client-side owner of the course, the learner's progress and the scheduling engine. */
public final class Learner {
    private Learner() {}

    private static Curriculum curriculum = new Curriculum();
    private static McIndex index = new McIndex(curriculum);
    private static Progress progress;
    private static Path progressFile;
    private static LearningEngine engine;
    private static boolean dirty;

    public static Path dataDir() {
        return FMLPaths.GAMEDIR.get().resolve("nihongo");
    }

    static void onCurriculumLoaded(Curriculum loaded) {
        curriculum = loaded;
        index = new McIndex(loaded);
        engine = null;
    }

    public static Curriculum curriculum() {
        return curriculum;
    }

    public static McIndex index() {
        return index;
    }

    public static LearningEngine engine() {
        if (progress == null) {
            UUID id = Minecraft.getInstance().getUser().getProfileId();
            progressFile = dataDir().resolve("progress-" + (id == null ? "local" : id.toString()) + ".json");
            progress = Progress.load(progressFile);
        }
        if (engine == null) {
            engine = new LearningEngine(curriculum, progress, System::currentTimeMillis, new Random(),
                    new LearningEngine.Settings(), ZoneId.systemDefault());
            engine.setOnChange(() -> dirty = true);
        }
        applySettings();
        return engine;
    }

    /** Copies the client config into the engine (called whenever the engine is fetched). */
    private static void applySettings() {
        LearningEngine.Settings s = engine.settings();
        try {
            s.dailyNewLimit = ClientConfig.DAILY_NEW_LIMIT.get();
            s.maxReviewsPerSession = ClientConfig.MAX_REVIEWS.get();
            s.gate = ClientConfig.GATE_PERCENT.get() / 100.0;
        } catch (IllegalStateException notLoadedYet) {
            // Config not loaded yet; keep defaults.
        }
    }

    public static void markDirty() {
        dirty = true;
    }

    public static void saveIfDirty() {
        if (!dirty || progress == null) return;
        dirty = false;
        try {
            progress.save(progressFile);
        } catch (Exception e) {
            NihongoMod.LOGGER.error("Could not save Japanese progress to {}", progressFile, e);
        }
    }

    /** Sends a payload if the server has this mod; silently does nothing otherwise. */
    public static boolean sendToServer(CustomPacketPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null || !connection.hasChannel(payload)) return false;
        try {
            PacketDistributor.sendToServer(payload);
            return true;
        } catch (RuntimeException e) {
            NihongoMod.LOGGER.debug("Could not send {}", payload.type().id(), e);
            return false;
        }
    }

    public static void rewardStudy(int correct) {
        if (correct > 0) sendToServer(new ModNetwork.Reward(ModNetwork.Reward.STUDY, correct));
    }
}
