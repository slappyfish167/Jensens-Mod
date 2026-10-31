package com.jensen.nihongo.client;

import com.jensen.nihongo.client.voice.VoiceService;
import com.jensen.nihongo.learning.Curriculum;
import com.jensen.nihongo.learning.LearningEngine;
import com.jensen.nihongo.learning.McIndex;
import com.jensen.nihongo.learning.Quests;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * In-world overlay: the Japanese name of whatever you're looking at (only for words you have
 * studied, so the world gradually "turns Japanese" as you learn), the active quest, in-world
 * study prompts, and a listening indicator for voice spells.
 */
public final class ImmersionHud {
    private ImmersionHud() {}

    private static String cachedTarget = "";
    private static String cachedLabel;

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        Font font = mc.font;
        int w = g.guiWidth();
        int h = g.guiHeight();

        String label = lookAtLabel(mc);
        if (label != null) {
            int tw = font.width(label);
            int x = (w - tw) / 2;
            int y = h / 2 + 14;
            g.fill(x - 4, y - 3, x + tw + 4, y + font.lineHeight + 2, 0x90000000);
            g.drawString(font, label, x, y, 0xFFFFE08A, false);
        }

        Quests.Quest quest = QuestTracker.current();
        if (quest != null) {
            String line = "クエスト  " + quest.japanese() + "  (" + QuestTracker.progress() + "/" + quest.count() + ")";
            int x = 6;
            int y = 6;
            int boxW = font.width(line) + 8;
            int lines = QuestTracker.hintShown() ? 3 : 1;
            g.fill(x - 3, y - 3, x - 3 + Math.max(boxW, QuestTracker.hintShown() ? font.width(quest.english()) + 8 : 0), y + lines * 11 + 1, 0x90000000);
            g.drawString(font, line, x, y, 0xFF8AE6FF, false);
            if (QuestTracker.hintShown()) {
                g.drawString(font, quest.romaji(), x, y + 11, 0xFFAAAAAA, false);
                g.drawString(font, quest.english(), x, y + 22, 0xFFAAAAAA, false);
            }
        }

        renderEncounter(g, font, w, h);

        if (VoiceService.isRecording()) {
            String text = "♪ きいて います... (listening)";
            int tw = font.width(text);
            int x = (w - tw) / 2;
            int y = h - 72;
            g.fill(x - 4, y - 3, x + tw + 4, y + 12, 0xA0300050);
            g.drawString(font, text, x, y, 0xFFFFB0FF, false);
            int bar = (int) (VoiceService.level() * (tw + 8));
            g.fill(x - 4, y + 12, x - 4 + bar, y + 14, 0xFFFF80FF);
        }
    }

    /** The in-world study prompt: a card at the right edge of the screen. */
    private static void renderEncounter(GuiGraphics g, Font font, int w, int h) {
        LearningEngine.Encounter e = Encounters.pending();
        if (e == null) return;
        String key = KeyBindings.FIELD_STUDY.getTranslatedKeyMessage().getString();
        boolean review = e.kind() == LearningEngine.EncounterKind.REVIEW;
        String title = review ? "ふくしゅう!  Review" : "あたらしい ことば!  New word";
        String line = review ? "What is this called in Japanese?" : "Learn the Japanese for \"" + e.word().meanings.get(0) + "\"";
        String action = "Press [" + key + "] to " + (review ? "answer" : "learn it");
        int textW = Math.max(font.width(title), Math.max(font.width(line), font.width(action)));
        int boxW = textW + 30;
        int x = w - boxW - 6;
        int y = h / 2 - 40;
        g.fill(x, y, x + boxW, y + 40, 0xB0101020);
        g.renderOutline(x, y, boxW, 40, review ? 0xFF8AE6FF : 0xFFFFD866);
        ItemStack icon = iconStack(Encounters.pendingIcon());
        if (!icon.isEmpty()) g.renderItem(icon, x + 5, y + 12);
        int tx = x + 25;
        g.drawString(font, title, tx, y + 4, review ? 0xFF8AE6FF : 0xFFFFD866, false);
        g.drawString(font, line, tx, y + 16, 0xFFFFFFFF, false);
        g.drawString(font, action, tx, y + 28, 0xFFAAAAAA, false);
    }

    private static ItemStack iconStack(String id) {
        ResourceLocation rl = id == null ? null : ResourceLocation.tryParse(id);
        if (rl == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(rl);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static String lookAtLabel(Minecraft mc) {
        try {
            if (!ClientConfig.LOOK_LABELS.get()) return null;
        } catch (IllegalStateException e) {
            return null;
        }
        String key;
        McIndex.Type type;
        Entity entity = mc.crosshairPickEntity;
        HitResult hit = mc.hitResult;
        if (entity != null) {
            type = McIndex.Type.ENTITY;
            key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        } else if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK && mc.level != null) {
            type = McIndex.Type.BLOCK;
            key = BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(bhr.getBlockPos()).getBlock()).toString();
        } else {
            return null;
        }
        // Labels only change when the target changes, or occasionally as words are learned.
        String cacheKey = type + key + (mc.level == null ? 0 : mc.level.getGameTime() / 40);
        if (!cacheKey.equals(cachedTarget)) {
            cachedTarget = cacheKey;
            Curriculum.VocabItem v = Labels.bestKnown(type, key);
            cachedLabel = v == null ? null : Labels.format(v);
        }
        return cachedLabel;
    }
}
