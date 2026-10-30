package com.jensen.nihongo.client.screen;

import com.jensen.nihongo.client.Learner;
import com.jensen.nihongo.client.tts.Tts;
import com.jensen.nihongo.learning.Curriculum;
import com.jensen.nihongo.learning.LearningEngine;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** The gojūon table, coloured by what you know. Click a kana to hear it. */
public class KanaChartScreen extends Screen {
    private static final String[] CONSONANTS = {"", "k", "s", "t", "n", "h", "m", "y", "r", "w", "n"};
    private static final String[] VOWELS = {"a", "i", "u", "e", "o"};
    private static final int CELL_W = 30;
    private static final int CELL_H = 26;

    private final Screen parent;
    private boolean katakana;

    public KanaChartScreen(Screen parent) {
        super(Component.literal("Kana chart"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        addRenderableWidget(Button.builder(Component.literal(katakana ? "Show hiragana" : "Show katakana"), b -> {
            katakana = !katakana;
            rebuildWidgets();
        }).bounds(cx - 104, height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(cx + 4, height - 28, 100, 20).build());
    }

    private static String romaji(int col, int row) {
        String c = CONSONANTS[col];
        String v = VOWELS[row];
        if (col == 10) return row == 0 ? "n" : null;
        if (c.equals("y") && (v.equals("i") || v.equals("e"))) return null;
        if (c.equals("w")) return v.equals("a") ? "wa" : v.equals("o") ? "wo" : null;
        String r = c + v;
        return switch (r) {
            case "si" -> "shi";
            case "ti" -> "chi";
            case "tu" -> "tsu";
            case "hu" -> "fu";
            default -> r;
        };
    }

    private Curriculum.KanaItem cell(int col, int row) {
        String r = romaji(col, row);
        if (r == null) return null;
        return Learner.curriculum().item((katakana ? "k_" : "h_") + r) instanceof Curriculum.KanaItem k ? k : null;
    }

    private int left() {
        return width / 2 - CONSONANTS.length * CELL_W / 2;
    }

    private int top() {
        return 44;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        LearningEngine engine = Learner.engine();
        g.drawCenteredString(font, katakana ? "カタカナ  Katakana" : "ひらがな  Hiragana", width / 2, 12, Ui.GOLD);
        g.drawCenteredString(font, "white = learned   gold = learning   dark = not yet  •  click to hear", width / 2, 26, Ui.GRAY);
        Curriculum.KanaItem hovered = null;
        // Traditional order reads right to left, but learners from left-to-right scripts find this easier.
        for (int col = 0; col < CONSONANTS.length; col++) {
            for (int row = 0; row < VOWELS.length; row++) {
                Curriculum.KanaItem k = cell(col, row);
                if (k == null) continue;
                int x = left() + col * CELL_W;
                int y = top() + row * CELL_H;
                boolean learned = engine.isLearned(k.id);
                boolean seen = engine.isIntroduced(k.id);
                g.fill(x + 1, y + 1, x + CELL_W - 1, y + CELL_H - 1, learned ? 0x6040A060 : seen ? 0x60A08020 : 0x60202020);
                Ui.big(g, font, k.kana, x + CELL_W / 2, y + 3, 1.5f, learned ? Ui.WHITE : seen ? Ui.GOLD : Ui.DIM);
                g.drawCenteredString(font, seen ? k.romaji : "", x + CELL_W / 2, y + 16, Ui.GRAY);
                if (mouseX >= x && mouseX < x + CELL_W && mouseY >= y && mouseY < y + CELL_H) hovered = k;
            }
        }
        int noteY = top() + VOWELS.length * CELL_H + 8;
        Ui.wrap(g, font, "Plus ゛ (dakuten: k→g, s→z, t→d, h→b), ゜ (h→p), small ゃゅょ combos and small っ.",
                width / 2, noteY, Math.min(360, width - 20), Ui.GRAY);
        if (hovered != null) {
            List<FormattedCharSequence> tip = new ArrayList<>();
            tip.add(Component.literal(hovered.kana + "  " + hovered.romaji).getVisualOrderText());
            if (engine.isIntroduced(hovered.id)) tip.addAll(font.split(Component.literal(hovered.mnemonic), 200));
            else tip.add(Component.literal("Not learned yet").getVisualOrderText());
            g.renderTooltip(font, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int col = (int) Math.floor((mouseX - left()) / CELL_W);
        int row = (int) Math.floor((mouseY - top()) / CELL_H);
        if (col >= 0 && col < CONSONANTS.length && row >= 0 && row < VOWELS.length) {
            Curriculum.KanaItem k = cell(col, row);
            if (k != null) {
                Tts.speak(k.kana);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
