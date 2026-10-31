package com.jensen.nihongo.client;

import com.jensen.nihongo.client.screen.StudyScreen;
import com.jensen.nihongo.learning.Curriculum;
import com.jensen.nihongo.learning.LearningEngine;
import com.jensen.nihongo.learning.McIndex;
import com.jensen.nihongo.learning.StudySession;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Study woven into play: when you collect, mine, attack or stare at something, and its word is due
 * for review (or, once the kana are learned, not known yet), a small prompt offers a question about
 * it right then. Recalling a word while holding the thing it names is about as strong a cue as it gets.
 */
public final class Encounters {
    private Encounters() {}

    /** How long a prompt stays on screen. */
    private static final long SHOW_MS = 15_000L;
    /** The same word is not offered again for this long, so one stack of logs doesn't nag. */
    private static final long SAME_WORD_MS = 5 * 60_000L;
    /** Ticks you have to keep looking at something before it counts as an encounter. */
    private static final int LOOK_TICKS = 40;

    private static LearningEngine.Encounter pending;
    private static String pendingIcon;
    private static long pendingUntil;
    private static long nextAllowedAt;
    private static final Map<String, Long> recent = new HashMap<>();
    private static Map<Item, Integer> lastCounts;
    private static String lookKey = "";
    private static int lookTicks;

    public static LearningEngine.Encounter pending() {
        return pending;
    }

    public static String pendingIcon() {
        return pendingIcon;
    }

    static void reset() {
        pending = null;
        lastCounts = null;
        recent.clear();
        lookKey = "";
        lookTicks = 0;
        nextAllowedAt = System.currentTimeMillis() + 30_000L;
    }

    private static boolean enabled() {
        try {
            return ClientConfig.FIELD_PROMPTS.get();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private static long cooldownMs() {
        try {
            return ClientConfig.FIELD_PROMPT_SECONDS.get() * 1000L;
        } catch (IllegalStateException e) {
            return 30_000L;
        }
    }

    // ------------------------------------------------------------------ sources

    static void tick(Minecraft mc, int ticks) {
        if (mc.player == null) return;
        if (pending != null && System.currentTimeMillis() > pendingUntil) pending = null;
        // Things met are still tracked with prompts off, so known words can be said aloud (see Listening).
        if (!enabled()) pending = null;
        if (mc.screen == null) trackLook(mc);
        if (ticks % 20 == 0) trackInventory(mc);
    }

    /** New items in the inventory: mined blocks, picked-up drops, crafted things. */
    private static void trackInventory(Minecraft mc) {
        Map<Item, Integer> counts = new HashMap<>();
        for (ItemStack stack : mc.player.getInventory().items) {
            if (!stack.isEmpty()) counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        Map<Item, Integer> before = lastCounts;
        lastCounts = counts;
        if (before == null) return;
        for (Map.Entry<Item, Integer> e : counts.entrySet()) {
            if (e.getValue() > before.getOrDefault(e.getKey(), 0)) {
                String id = BuiltInRegistries.ITEM.getKey(e.getKey()).toString();
                if (offer(McIndex.Type.ITEM, id, id, false)) return;
            }
        }
    }

    private static void trackLook(Minecraft mc) {
        Target t = target(mc);
        String key = t == null ? "" : t.type + "|" + t.id;
        if (!key.equals(lookKey)) {
            lookKey = key;
            lookTicks = 0;
            return;
        }
        if (t != null && ++lookTicks == LOOK_TICKS) offer(t.type, t.id, null, false);
    }

    static void onAttack(AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide()) return;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getTarget().getType()).toString();
        offer(McIndex.Type.ENTITY, id, null, false);
    }

    private record Target(McIndex.Type type, String id) {}

    private static Target target(Minecraft mc) {
        Entity entity = mc.crosshairPickEntity;
        HitResult hit = mc.hitResult;
        if (entity != null) {
            return new Target(McIndex.Type.ENTITY, BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        }
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK && mc.level != null) {
            return new Target(McIndex.Type.BLOCK, BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(bhr.getBlockPos()).getBlock()).toString());
        }
        return null;
    }

    /** Shows a prompt for this thing if there is something to study about it. Returns true if one was shown. */
    private static boolean offer(McIndex.Type type, String id, String icon, boolean force) {
        List<Curriculum.VocabItem> words = Learner.index().lookup(type, id);
        if (words.isEmpty()) return false;
        if (!force && Listening.checkPending()) return false;
        LearningEngine.Encounter e = Learner.engine().encounter(words);
        // A known word that isn't up for review is just said aloud now and then (saying a due word would give the answer away).
        if (!force && (e == null || e.kind() == LearningEngine.EncounterKind.DISCOVER)) Listening.ambient(words);
        if (!force && (!enabled() || pending != null)) return false;
        long now = System.currentTimeMillis();
        if (!force && now < nextAllowedAt) return false;
        if (e == null) return false;
        Long last = recent.get(e.word().id);
        if (!force && last != null && now - last < SAME_WORD_MS) return false;
        recent.put(e.word().id, now);
        pending = e;
        pendingIcon = icon != null ? icon : e.word().icon;
        pendingUntil = now + SHOW_MS;
        nextAllowedAt = now + cooldownMs();
        if (!force) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.5F, 0.6F));
        return true;
    }

    // ------------------------------------------------------------------ answering

    /** The field-study key: answers the current prompt, or asks about whatever you're looking at. */
    static void onKey(Minecraft mc) {
        if (mc.player == null || mc.screen != null) return;
        if (Listening.onKey(mc)) return;
        if (pending == null) {
            Target t = target(mc);
            if (t == null || !offer(t.type, t.id, null, true)) {
                mc.player.displayClientMessage(Component.literal("Nothing to study here right now. Look at a block or mob, or press J for lessons.")
                        .withStyle(ChatFormatting.GRAY), true);
                return;
            }
        }
        LearningEngine.Encounter e = pending;
        pending = null;
        StudySession session = Learner.engine().startEncounter(e);
        if (session != null) mc.setScreen(new StudyScreen(null, session));
    }
}
