package com.jensen.nihongo.client;

import com.jensen.nihongo.client.voice.VoiceService;
import com.jensen.nihongo.learning.Curriculum;
import com.jensen.nihongo.learning.McIndex;
import com.jensen.nihongo.learning.Quests;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * In-world overlay: the Japanese name of whatever you're looking at (only for words you have
 * studied, so the world gradually "turns Japanese" as you learn), the active quest, and a
 * listening indicator for voice spells.
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
