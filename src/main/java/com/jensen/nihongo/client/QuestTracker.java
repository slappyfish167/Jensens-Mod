package com.jensen.nihongo.client;

import com.jensen.nihongo.learning.Quests;
import com.jensen.nihongo.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Random;

/** Tracks the current Japanese collection quest. */
public final class QuestTracker {
    private QuestTracker() {}

    private static final long EXPIRE_MS = 20 * 60_000L;
    private static final Random RNG = new Random();

    private static Quests.Quest quest;
    private static Item item;
    private static int baseline;
    private static int progress;
    private static long startedAt;
    private static long nextQuestAt = System.currentTimeMillis() + 60_000L;
    private static boolean hint;

    public static Quests.Quest current() {
        return quest;
    }

    public static int progress() {
        return progress;
    }

    public static boolean hintShown() {
        return hint;
    }

    static void toggleHint() {
        if (quest != null) hint = !hint;
    }

    public static void skip() {
        quest = null;
        nextQuestAt = System.currentTimeMillis() + intervalMs();
    }

    static void reset() {
        quest = null;
        nextQuestAt = System.currentTimeMillis() + 60_000L;
    }

    private static boolean enabled() {
        try {
            return ClientConfig.QUESTS.get();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private static long intervalMs() {
        try {
            return ClientConfig.QUEST_INTERVAL_MINUTES.get() * 60_000L;
        } catch (IllegalStateException e) {
            return 6 * 60_000L;
        }
    }

    static void tick(Minecraft mc) {
        if (!enabled() || mc.player == null) {
            quest = null;
            return;
        }
        long now = System.currentTimeMillis();
        if (quest == null) {
            if (now < nextQuestAt) return;
            nextQuestAt = now + intervalMs();
            Quests.Quest q = Quests.generate(Learner.engine().learnedVocab(), RNG, null);
            if (q == null) return;
            Item target = BuiltInRegistries.ITEM.get(ResourceLocation.parse(q.itemId()));
            if (target == Items.AIR) return;
            quest = q;
            item = target;
            baseline = mc.player.getInventory().countItem(item);
            progress = 0;
            startedAt = now;
            hint = false;
            mc.player.displayClientMessage(Component.literal("クエスト: " + q.japanese()).withStyle(ChatFormatting.AQUA), false);
            mc.player.displayClientMessage(Component.literal("(New Japanese quest! Press H for a hint.)").withStyle(ChatFormatting.GRAY), false);
            return;
        }
        if (now - startedAt > EXPIRE_MS) {
            quest = null;
            return;
        }
        int count = mc.player.getInventory().countItem(item);
        // If items were used up, move the baseline down so progress can't go negative.
        if (count < baseline + progress) baseline = Math.max(0, count - progress);
        progress = Math.max(0, count - baseline);
        if (progress >= quest.count()) complete(mc);
    }

    private static void complete(Minecraft mc) {
        Quests.Quest done = quest;
        quest = null;
        nextQuestAt = System.currentTimeMillis() + intervalMs();
        Learner.engine().progress().questsCompleted++;
        Learner.markDirty();
        boolean rewarded = Learner.sendToServer(new ModNetwork.Reward(ModNetwork.Reward.QUEST, 1));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0F));
        mc.player.displayClientMessage(Component.literal("クエスト クリア!  よく できました! (Well done!) - " + done.english()
                + (rewarded ? "" : " (no reward: this server doesn't have NihongoCraft)")).withStyle(ChatFormatting.GREEN), false);
    }
}
