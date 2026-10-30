package com.jensen.nihongo.client.screen;

import com.jensen.nihongo.client.Learner;
import com.jensen.nihongo.client.tts.Tts;
import com.jensen.nihongo.learning.Curriculum;
import com.jensen.nihongo.learning.LearningEngine;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Re-read the grammar points you've been taught. */
public class GrammarBookScreen extends Screen {
    private static final int PER_PAGE = 8;
    private final Screen parent;
    private final List<Curriculum.GrammarPoint> points = new ArrayList<>();
    private int page;
    private Curriculum.GrammarPoint selected;
    private int scroll;

    public GrammarBookScreen(Screen parent) {
        super(Component.literal("Grammar notes"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        points.clear();
        LearningEngine engine = Learner.engine();
        for (String id : engine.progress().introduced) {
            if (Learner.curriculum().item(id) instanceof Curriculum.GrammarPoint gp) points.add(gp);
        }
        if (selected == null && !points.isEmpty()) selected = points.get(0);
        int listW = Math.min(140, width / 3);
        int y = 30;
        int from = page * PER_PAGE;
        for (int i = from; i < points.size() && i < from + PER_PAGE; i++) {
            Curriculum.GrammarPoint gp = points.get(i);
            String label = gp.title.length() > 22 ? gp.title.substring(0, 21) + "…" : gp.title;
            Button b = addRenderableWidget(Button.builder(Component.literal(label), btn -> {
                selected = gp;
                scroll = 0;
                rebuildWidgets();
            }).bounds(8, y, listW, 18).build());
            b.active = gp != selected;
            y += 20;
        }
        if (points.size() > PER_PAGE) {
            Button prev = addRenderableWidget(Button.builder(Component.literal("▲"), b -> {
                page--;
                rebuildWidgets();
            }).bounds(8, y + 2, listW / 2 - 2, 18).build());
            prev.active = page > 0;
            Button next = addRenderableWidget(Button.builder(Component.literal("▼"), b -> {
                page++;
                rebuildWidgets();
            }).bounds(8 + listW / 2 + 2, y + 2, listW / 2 - 2, 18).build());
            next.active = (page + 1) * PER_PAGE < points.size();
        }
        if (selected != null && !selected.examples.isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal("Listen to examples"), b -> {
                StringBuilder sb = new StringBuilder();
                for (Curriculum.Example ex : selected.examples) sb.append(ex.jp).append(' ');
                Tts.speak(sb.toString());
            }).bounds(width / 2 - 104, height - 28, 100, 20).build()).active = Tts.enabled();
        }
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(width / 2 + 4, height - 28, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, "文法  Grammar notes", width / 2, 10, Ui.GOLD);
        if (selected == null) {
            Ui.wrap(g, font, "No grammar yet. Grammar starts once you can read hiragana and know some words.", width / 2, height / 2, width - 40, Ui.GRAY);
            return;
        }
        int listW = Math.min(140, width / 3);
        int x = listW + 20;
        int w = width - x - 10;
        g.enableScissor(x, 26, width, height - 34);
        int y = 30 - scroll;
        y = Ui.wrapLeft(g, font, selected.title, x, y, w, Ui.GOLD) + 2;
        y = Ui.wrapLeft(g, font, selected.summary, x, y, w, Ui.WHITE) + 6;
        for (String p : selected.explanation) y = Ui.wrapLeft(g, font, "• " + p, x, y, w, Ui.WHITE) + 3;
        y += 6;
        g.drawString(font, "Examples", x, y, Ui.CYAN, false);
        y += 12;
        for (Curriculum.Example ex : selected.examples) {
            y = Ui.wrapLeft(g, font, ex.jp, x + 6, y, w - 6, Ui.WHITE);
            y = Ui.wrapLeft(g, font, ex.en, x + 6, y, w - 6, Ui.GRAY) + 4;
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
