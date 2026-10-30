package com.jensen.nihongo.client.screen;

import com.jensen.nihongo.client.Learner;
import com.jensen.nihongo.learning.Curriculum;
import com.jensen.nihongo.learning.LearningEngine;
import com.jensen.nihongo.learning.PlacementTest;
import com.jensen.nihongo.learning.StudySession;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Explains the placement test before it starts, and shows where it placed the learner afterwards. */
public class PlacementScreen extends Screen {
    private final Screen parent;
    private final PlacementTest result;

    /** Intro shown before the test. */
    public PlacementScreen(Screen parent) {
        this(parent, null);
    }

    /** Result shown after a finished test. */
    public PlacementScreen(Screen parent, PlacementTest result) {
        super(Component.literal("Placement test"));
        this.parent = parent;
        this.result = result;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int bottom = height - 28;
        if (result == null) {
            addRenderableWidget(Button.builder(Component.literal("Start the test"), b -> start())
                    .bounds(cx - 104, bottom, 100, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                    .bounds(cx + 4, bottom, 100, 20).build());
        } else if (result.lessonsPlaced() == 0) {
            addRenderableWidget(Button.builder(Component.literal("Start from lesson 1"), b -> onClose())
                    .bounds(cx - 60, bottom, 120, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("Skip ahead"), b -> accept())
                    .bounds(cx - 104, bottom, 100, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Don't skip"), b -> onClose())
                    .bounds(cx + 4, bottom, 100, 20).build());
        }
    }

    private void start() {
        LearningEngine engine = Learner.engine();
        PlacementTest test = engine.startPlacement();
        StudySession session = test == null ? null : engine.placementSession(test);
        if (session != null) minecraft.setScreen(new StudyScreen(parent, session));
        else onClose();
    }

    private void accept() {
        result.apply();
        Learner.saveIfDirty();
        minecraft.setScreen(new HubScreen());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int w = Math.min(340, width - 30);
        Ui.big(g, font, "れべるチェック", cx, 20, 3f, Ui.GOLD);
        g.drawCenteredString(font, "Placement test", cx, 52, Ui.WHITE);
        int y = 76;
        if (result == null) {
            y = Ui.wrap(g, font, "Already know some Japanese? Answer a few questions from each lesson, in course order, "
                    + "and the mod will skip you past everything you can already do.", cx, y, w, Ui.WHITE) + 8;
            y = Ui.wrap(g, font, "It stops at the first lesson that gives you trouble, so it's short if you're new. "
                    + "Press \"I don't know\" instead of guessing; that's what it is for.", cx, y, w, Ui.GRAY) + 8;
            Ui.wrap(g, font, "Nothing changes until you confirm at the end, and skipped items still come back as reviews "
                    + "over the next days to catch any gaps.", cx, y, w, Ui.GRAY);
            return;
        }
        g.drawCenteredString(font, result.totalCorrect() + " / " + result.totalAsked() + " correct", cx, y, Ui.GREEN);
        y += 18;
        Curriculum.Lesson next = result.firstUnplaced();
        if (result.lessonsPlaced() == 0) {
            Ui.wrap(g, font, "No problem - everyone starts somewhere. The course begins with the very first hiragana, "
                    + "and you can retake this test any time.", cx, y, w, Ui.WHITE);
            return;
        }
        y = Ui.wrap(g, font, result.lessonsPlaced() + " lesson" + (result.lessonsPlaced() == 1 ? "" : "s")
                + " of " + result.lessonTotal() + " can be skipped.", cx, y, w, Ui.WHITE) + 6;
        y = Ui.wrap(g, font, next == null ? "You already know the whole course! Reviews will keep it fresh."
                : "You'd continue at: " + next.title, cx, y, w, Ui.GOLD) + 8;
        Ui.wrap(g, font, "Skipped items are scheduled as reviews over the coming days; anything you've forgotten will "
                + "show up there and get relearned.", cx, y, w, Ui.GRAY);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent instanceof HubScreen ? new HubScreen() : parent);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
