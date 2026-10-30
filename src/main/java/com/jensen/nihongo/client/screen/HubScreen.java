package com.jensen.nihongo.client.screen;

import com.jensen.nihongo.client.Learner;
import com.jensen.nihongo.client.QuestTracker;
import com.jensen.nihongo.client.voice.VoiceService;
import com.jensen.nihongo.learning.Curriculum;
import com.jensen.nihongo.learning.LearningEngine;
import com.jensen.nihongo.learning.Quests;
import com.jensen.nihongo.learning.StudySession;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The study hub: what to do next, at a glance. */
public class HubScreen extends Screen {
    private LearningEngine engine;
    private String lessonReason;

    public HubScreen() {
        super(Component.literal("NihongoCraft"));
    }

    @Override
    protected void init() {
        engine = Learner.engine();
        int cx = width / 2;
        int y = Math.max(78, height / 2 - 52);
        int bw = 220;

        Curriculum.Lesson lesson = engine.currentLesson();
        lessonReason = engine.lessonBlockReason();
        String lessonLabel = lesson == null ? "Course complete!"
                : "Learn: " + lesson.title;
        Button learn = addRenderableWidget(Button.builder(Component.literal(lessonLabel), b -> startLesson())
                .bounds(cx - bw / 2, y, bw, 20).build());
        learn.active = lessonReason == null;
        if (lesson != null) {
            learn.setTooltip(Tooltip.create(Component.literal(lessonReason != null ? lessonReason
                    : (lesson.intro.isEmpty() ? lesson.title : lesson.intro))));
        }

        int due = engine.dueCount();
        Button review = addRenderableWidget(Button.builder(Component.literal("Review (" + due + " due)"), b -> startReview())
                .bounds(cx - bw / 2, y + 24, bw, 20).build());
        review.active = due > 0;

        if (VoiceService.isReady()) {
            Button speak = addRenderableWidget(Button.builder(Component.literal("Speaking practice"), b -> startSpeaking())
                    .bounds(cx - bw / 2, y + 48, bw, 20).build());
            speak.active = !engine.introducedVocab().isEmpty();
            speak.setTooltip(Tooltip.create(Component.literal("Say the words you know out loud. Production practice strengthens recall.")));
        } else {
            addRenderableWidget(Button.builder(Component.literal("Set up voice recognition"), b -> minecraft.setScreen(new VoiceSetupScreen(this)))
                    .bounds(cx - bw / 2, y + 48, bw, 20).build());
        }

        int half = bw / 2 - 2;
        addRenderableWidget(Button.builder(Component.literal("Kana chart"), b -> minecraft.setScreen(new KanaChartScreen(this)))
                .bounds(cx - bw / 2, y + 72, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Grammar notes"), b -> minecraft.setScreen(new GrammarBookScreen(this)))
                .bounds(cx + 2, y + 72, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Course map"), b -> minecraft.setScreen(new CourseScreen(this)))
                .bounds(cx - bw / 2, y + 96, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Voice & audio"), b -> minecraft.setScreen(new VoiceSetupScreen(this)))
                .bounds(cx + 2, y + 96, half, 20).build());
        if (QuestTracker.current() != null) {
            addRenderableWidget(Button.builder(Component.literal("Skip quest"), b -> {
                QuestTracker.skip();
                rebuildWidgets();
            }).bounds(cx - bw / 2, y + 120, half, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(cx + 2, y + 120, half, 20).build());
    }

    private void startLesson() {
        StudySession s = engine.startLesson();
        if (s != null) minecraft.setScreen(new StudyScreen(this, s));
    }

    private void startReview() {
        StudySession s = engine.startReview();
        if (s != null) minecraft.setScreen(new StudyScreen(this, s));
    }

    private void startSpeaking() {
        StudySession s = engine.startSpeaking(10);
        if (s != null) minecraft.setScreen(new StudyScreen(this, s));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int top = Math.max(78, height / 2 - 52);
        Ui.big(g, font, "日本語  NihongoCraft", cx, top - 70, 2f, Ui.GOLD);

        LearningEngine.Counts c = engine.counts();
        var p = engine.progress();
        String stats = String.format("Kana %d/%d   Words %d/%d   Grammar %d/%d   Kanji %d",
                c.kanaLearned(), c.kanaTotal(), c.vocabLearned(), c.vocabTotal(), c.grammarLearned(), c.grammarTotal(), c.kanjiLearned());
        g.drawCenteredString(font, stats, cx, top - 46, Ui.WHITE);
        int streak = p.currentStreak(engine.today());
        String today = String.format("Today: %d new (limit %d)  •  %d reviews  •  streak %d day%s",
                p.dailyNew, engine.settings().dailyNewLimit, p.dailyReviews, streak, streak == 1 ? "" : "s");
        g.drawCenteredString(font, today, cx, top - 34, Ui.GRAY);

        String hint;
        if (lessonReason != null && engine.dueCount() == 0) {
            long next = engine.nextDueTime();
            hint = lessonReason + (next == Long.MAX_VALUE ? "" : "  Next review " + until(next) + ".");
        } else if (lessonReason != null) {
            hint = "Do your reviews first - retrieval practice is where memories are built.";
        } else {
            hint = engine.dueCount() > 0 ? "Tip: reviews first, then new lessons." : "Ready for something new!";
        }
        Ui.wrap(g, font, hint, cx, top - 20, Math.min(360, width - 20), Ui.CYAN);

        Quests.Quest q = QuestTracker.current();
        if (q != null) {
            g.drawCenteredString(font, "クエスト: " + q.japanese() + " (" + QuestTracker.progress() + "/" + q.count() + ")", cx, top + 146, Ui.PINK);
        }
    }

    private String until(long time) {
        long ms = time - engine.now();
        if (ms <= 0) return "now";
        long min = ms / 60_000;
        if (min < 60) return "in " + Math.max(1, min) + " min";
        long h = min / 60;
        if (h < 48) return "in " + h + " h";
        return "in " + (h / 24) + " days";
    }

    @Override
    public void onClose() {
        Learner.saveIfDirty();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
