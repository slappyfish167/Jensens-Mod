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
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The study hub: what to do next, at a glance. */
public class HubScreen extends Screen {
    private static final int ROW = 22;
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

        int half = bw / 2 - 2;
        int due = engine.dueCount();
        Button review = addRenderableWidget(Button.builder(Component.literal("Review (" + due + " due)"), b -> startReview())
                .bounds(cx - bw / 2, y + ROW, half, 20).build());
        review.active = due > 0;

        // Kanji are the last stage of the course, taught through words you already know.
        String kanjiReason = engine.kanjiBlockReason();
        int kanjiReady = engine.kanjiReady().size();
        Button kanji = addRenderableWidget(Button.builder(Component.literal(kanjiReady > 0 ? "Kanji (" + kanjiReady + " ready)" : "Kanji"),
                b -> startKanji()).bounds(cx + 2, y + ROW, half, 20).build());
        kanji.active = kanjiReason == null;
        kanji.setTooltip(Tooltip.create(Component.literal(kanjiReason != null ? kanjiReason
                : "Learn the kanji for words you already know.")));

        if (VoiceService.isReady()) {
            Button speak = addRenderableWidget(Button.builder(Component.literal("Speaking practice"), b -> startSpeaking())
                    .bounds(cx - bw / 2, y + ROW * 2, bw, 20).build());
            speak.active = !engine.introducedVocab().isEmpty();
            speak.setTooltip(Tooltip.create(Component.literal("Say the words you know out loud. Production practice strengthens recall.")));
        } else {
            addRenderableWidget(Button.builder(Component.literal("Set up voice recognition"), b -> minecraft.setScreen(new VoiceSetupScreen(this)))
                    .bounds(cx - bw / 2, y + ROW * 2, bw, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Kana chart"), b -> minecraft.setScreen(new KanaChartScreen(this)))
                .bounds(cx - bw / 2, y + ROW * 3, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Grammar notes"), b -> minecraft.setScreen(new GrammarBookScreen(this)))
                .bounds(cx + 2, y + ROW * 3, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Course map"), b -> minecraft.setScreen(new CourseScreen(this)))
                .bounds(cx - bw / 2, y + ROW * 4, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Voice & audio"), b -> minecraft.setScreen(new VoiceSetupScreen(this)))
                .bounds(cx + 2, y + ROW * 4, half, 20).build());

        boolean limited = engine.settings().dailyLimitEnabled;
        Button limit = addRenderableWidget(Button.builder(Component.literal("Daily limit: " + (limited ? engine.settings().dailyNewLimit : "off")), b -> {
            Learner.setDailyLimitEnabled(!limited);
            rebuildWidgets();
        }).bounds(cx - bw / 2, y + ROW * 5, half, 20).build());
        limit.setTooltip(Tooltip.create(Component.literal(limited
                ? "New items are limited to " + engine.settings().dailyNewLimit + " a day (change the number in the mod config). Click to turn the limit off."
                : "Learn as many new items a day as you like. Click to limit new items per day.")));
        Button reset = addRenderableWidget(Button.builder(Component.literal("Reset progress"), b -> confirmReset())
                .bounds(cx + 2, y + ROW * 5, half, 20).build());
        reset.setTooltip(Tooltip.create(Component.literal("Start the course over from the first hiragana.")));

        if (lesson != null) {
            Button placement = addRenderableWidget(Button.builder(Component.literal("Placement test"), b -> minecraft.setScreen(new PlacementScreen(this)))
                    .bounds(cx - bw / 2, y + ROW * 6, half, 20).build());
            placement.setTooltip(Tooltip.create(Component.literal("Already know some Japanese? Skip the lessons you can already do.")));
        }
        if (QuestTracker.current() != null) {
            addRenderableWidget(Button.builder(Component.literal("Skip quest"), b -> {
                QuestTracker.skip();
                rebuildWidgets();
            }).bounds(cx + 2, y + ROW * 6, half, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(cx - bw / 2, y + ROW * 7, bw, 20).build());
    }

    private void confirmReset() {
        minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) Learner.resetProgress();
            minecraft.setScreen(new HubScreen());
        }, Component.literal("Reset all Japanese progress?"),
                Component.literal("Every lesson, review and kanji goes back to the start. A backup of your current progress is kept in .minecraft/nihongo/.")));
    }

    private void startKanji() {
        StudySession s = engine.startKanjiLesson();
        if (s != null) minecraft.setScreen(new StudyScreen(this, s));
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
        String limit = engine.settings().dailyLimitEnabled ? " (limit " + engine.settings().dailyNewLimit + ")" : "";
        String today = String.format("Today: %d new%s  •  %d reviews  •  streak %d day%s",
                p.dailyNew, limit, p.dailyReviews, streak, streak == 1 ? "" : "s");
        g.drawCenteredString(font, today, cx, top - 34, Ui.GRAY);

        String hint;
        if (engine.progress().introduced.isEmpty() && engine.currentLesson() != null) {
            hint = "New here? Press Learn. Already know some Japanese? Take the placement test to skip ahead.";
        } else if (lessonReason != null && engine.dueCount() == 0) {
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
            g.drawCenteredString(font, "クエスト: " + q.japanese() + " (" + QuestTracker.progress() + "/" + q.count() + ")", cx, top + ROW * 8 + 4, Ui.PINK);
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
