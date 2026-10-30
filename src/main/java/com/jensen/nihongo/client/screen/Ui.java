package com.jensen.nihongo.client.screen;

import com.jensen.nihongo.client.ClientConfig;
import com.jensen.nihongo.learning.Curriculum;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Small drawing helpers shared by the screens. */
final class Ui {
    private Ui() {}

    static final int WHITE = 0xFFFFFFFF;
    static final int GRAY = 0xFFAAAAAA;
    static final int DIM = 0xFF777777;
    static final int GOLD = 0xFFFFD866;
    static final int GREEN = 0xFF66E07A;
    static final int RED = 0xFFFF6B6B;
    static final int CYAN = 0xFF8AE6FF;
    static final int PINK = 0xFFFFA8D8;

    /** Draws text centred at cx, scaled (pixel fonts stay crisp at whole-number scales). */
    static void big(GuiGraphics g, Font font, String text, int cx, int y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, text, -font.width(text) / 2, 0, color, false);
        g.pose().popPose();
    }

    /** Largest scale (up to max) at which text fits in width. */
    static float fit(Font font, String text, float max, int width) {
        float s = Math.min(max, width / (float) Math.max(1, font.width(text)));
        return s >= 1 ? (float) Math.floor(s * 2) / 2f : s;
    }

    /** Word-wrapped, centred text. Returns the y below the last line. */
    static int wrap(GuiGraphics g, Font font, String text, int cx, int y, int width, int color) {
        if (text == null || text.isEmpty()) return y;
        List<FormattedCharSequence> lines = font.split(Component.literal(text), width);
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, cx - font.width(line) / 2, y, color, false);
            y += font.lineHeight + 1;
        }
        return y;
    }

    /** Word-wrapped, left-aligned text. Returns the y below the last line. */
    static int wrapLeft(GuiGraphics g, Font font, String text, int x, int y, int width, int color) {
        if (text == null || text.isEmpty()) return y;
        for (FormattedCharSequence line : font.split(Component.literal(text), width)) {
            g.drawString(font, line, x, y, color, false);
            y += font.lineHeight + 1;
        }
        return y;
    }

    static void panel(GuiGraphics g, int x0, int y0, int x1, int y1) {
        g.fill(x0, y0, x1, y1, 0xC0101020);
        g.renderOutline(x0, y0, x1 - x0, y1 - y0, 0xFF5B4B9B);
    }

    static ItemStack stack(String id) {
        if (id == null) return ItemStack.EMPTY;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(rl);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** Draws an item icon centred at cx, scaled up (dual coding: picture + word). */
    static void item(GuiGraphics g, String id, int cx, int y, float scale) {
        ItemStack stack = stack(id);
        if (stack.isEmpty()) return;
        g.pose().pushPose();
        g.pose().translate(cx - 8 * scale, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

    static void sound(boolean good) {
        try {
            if (!ClientConfig.FEEDBACK_SOUNDS.get()) return;
        } catch (IllegalStateException e) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play(good
                ? SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2F)
                : SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F));
    }

    /** Short display label for an item, e.g. in summaries. */
    static String label(Curriculum.Item item) {
        if (item instanceof Curriculum.KanaItem k) return k.isGroup() ? k.displayTitle() : k.kana + " " + k.romaji;
        if (item instanceof Curriculum.VocabItem v) return v.written() + " (" + v.kana + ") " + String.join(", ", v.meanings);
        if (item instanceof Curriculum.GrammarPoint gp) return gp.title;
        return item == null ? "?" : item.id;
    }
}
