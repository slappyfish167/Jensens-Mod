package com.jensen.nihongo.client.screen;

import com.jensen.nihongo.client.Learner;
import com.jensen.nihongo.learning.Curriculum;
import com.jensen.nihongo.learning.LearningEngine;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The whole course at a glance: where you are and what's coming. */
public class CourseScreen extends Screen {
    private final Screen parent;
    private int scroll;

    public CourseScreen(Screen parent) {
        super(Component.literal("Course map"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(width / 2 - 50, height - 28, 100, 20).build());
        // Start scrolled so the current lesson is visible.
        LearningEngine engine = Learner.engine();
        Curriculum.Lesson cur = engine.currentLesson();
        int idx = cur == null ? engine.curriculum().lessons().size() : engine.lessonIndex(cur);
        scroll = Math.max(0, idx * 12 - (height - 80) / 2);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        LearningEngine engine = Learner.engine();
        g.drawCenteredString(font, "Course map", width / 2, 10, Ui.GOLD);
        g.drawCenteredString(font, "Hiragana → words → katakana → grammar, each step using only what came before.", width / 2, 22, Ui.GRAY);
        Curriculum.Lesson current = engine.currentLesson();
        int x = Math.max(10, width / 2 - 170);
        g.enableScissor(0, 36, width, height - 34);
        int y = 40 - scroll;
        for (Curriculum.Lesson l : engine.curriculum().lessons()) {
            int done = 0;
            int learned = 0;
            for (String id : l.items) {
                if (engine.isIntroduced(id)) done++;
                if (engine.isLearned(id)) learned++;
            }
            String mark;
            int color;
            if (done == l.items.size() && learned == l.items.size()) {
                mark = "✔";
                color = Ui.GREEN;
            } else if (l == current || done > 0) {
                mark = "▶";
                color = Ui.GOLD;
            } else {
                mark = "·";
                color = Ui.DIM;
            }
            g.drawString(font, mark + "  " + l.title, x, y, color, false);
            String count = learned + "/" + l.items.size();
            g.drawString(font, count, x + 330 - font.width(count), y, color, false);
            y += 12;
        }
        g.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) (scrollY * 12));
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
