package com.jensen.nihongo.client.screen;

import com.jensen.nihongo.client.Learner;
import com.jensen.nihongo.client.Listening;
import com.jensen.nihongo.client.tts.Tts;
import com.jensen.nihongo.client.tts.VoiceCatalog;
import com.jensen.nihongo.client.voice.VoiceService;
import com.jensen.nihongo.learning.AnswerChecker;
import com.jensen.nihongo.learning.Curriculum;
import com.jensen.nihongo.learning.Kana;
import com.jensen.nihongo.learning.Question;
import com.jensen.nihongo.learning.StudySession;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runs a study session: teaching cards for new items, then questions with immediate feedback.
 */
public class StudyScreen extends Screen {
    private final Screen parent;
    private final StudySession session;
    private final boolean placement;

    private EditBox input;
    private String typed = "";
    private final List<Integer> picked = new ArrayList<>();
    private boolean hint;
    private String status = "";
    private List<String> heard = List.of();
    private boolean rewarded;
    private int scroll;
    private boolean wasRecording;
    /** The listening question whose audio has already been played automatically. */
    private Question autoPlayed;

    public StudyScreen(Screen parent, StudySession session) {
        this(parent, session, false);
    }

    /** @param alreadyHeard the first question's audio was just played (an in-world listening check) */
    public StudyScreen(Screen parent, StudySession session, boolean alreadyHeard) {
        super(Component.literal("Study"));
        this.parent = parent;
        this.session = session;
        this.placement = session.mode() == StudySession.Mode.PLACEMENT;
        if (alreadyHeard) autoPlayed = session.question();
    }

    /** Bottom row for buttons; placement tests reserve a line below it for "I don't know". */
    private int bottomY() {
        return height - 28 - (placement && !session.isAnswered() ? 24 : 0);
    }

    private Question question() {
        return session.question();
    }

    // ------------------------------------------------------------------ widgets

    @Override
    protected void init() {
        input = null;
        int cx = width / 2;
        int bottom = bottomY();

        if (session.finished()) {
            finishOnce();
            addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(cx - 50, bottom, 100, 20).build());
            return;
        }

        if (session.current() instanceof StudySession.Teach teach) {
            String speak = teach.kanjiOnly() ? null : teach.item().speakText();
            if (teach.item() instanceof Curriculum.GrammarPoint gp && !gp.examples.isEmpty()) speak = gp.examples.get(0).jp;
            if (teach.kanjiOnly() && teach.item() instanceof Curriculum.VocabItem v) speak = v.kana;
            String toSpeak = speak;
            String itemId = teach.item().id;
            Button listen = addRenderableWidget(Button.builder(Component.literal("Listen"), b -> Listening.say(itemId, toSpeak))
                    .bounds(cx - 104, bottom, 100, 20).build());
            listen.active = Tts.enabled() && toSpeak != null;
            addRenderableWidget(Button.builder(Component.literal("Got it  →"), b -> next()).bounds(cx + 4, bottom, 100, 20).build());
            return;
        }

        Question q = question();
        if (q == null) return;
        int inputY = inputY();
        if (q.listening && !session.isAnswered() && autoPlayed != q) {
            autoPlayed = q;
            playListening(q, false);
        }

        if (placement && !session.isAnswered()) {
            addRenderableWidget(Button.builder(Component.literal("I don't know"), b -> {
                session.giveUp();
                afterAnswer(session.result());
            }).bounds(cx - 50, height - 28, 100, 20).build());
        }

        if (session.isAnswered()) {
            List<Button> row = new ArrayList<>();
            if (q.type == Question.Type.SPEAK && !session.result().passed()) {
                row.add(Button.builder(Component.literal("Try again"), b -> {
                    session.retry();
                    heard = List.of();
                    rebuildWidgets();
                }).width(90).build());
            } else if (!session.result().passed() && (q.type == Question.Type.TYPE_JAPANESE || q.type == Question.Type.TYPE_MEANING)) {
                row.add(Button.builder(Component.literal("I was right"), b -> {
                    session.overrideCorrect();
                    rebuildWidgets();
                }).width(90).build());
            }
            if (Tts.enabled() && q.speak != null) row.add(Button.builder(Component.literal("Listen"), b -> speakAnswer(q)).width(70).build());
            row.add(Button.builder(Component.literal("Continue (Enter)"), b -> next()).width(110).build());
            layoutRow(row, bottom);
            return;
        }

        switch (q.type) {
            case TYPE_JAPANESE, TYPE_MEANING -> {
                input = new EditBox(font, cx - 110, inputY, 220, 20, Component.literal("Answer"));
                input.setMaxLength(80);
                input.setValue(typed);
                input.setResponder(s -> typed = s);
                input.setHint(Component.literal(q.type == Question.Type.TYPE_MEANING ? "English..." : "romaji or kana...").withColor(0xFF777777));
                addRenderableWidget(input);
                setInitialFocus(input);
                List<Button> row = new ArrayList<>();
                if (q.listening) row.addAll(replayButtons(q));
                else if (q.speakInPrompt && Tts.enabled()) row.add(Button.builder(Component.literal("Listen"), b -> Listening.say(q.itemId, q.speak)).width(70).build());
                if (q.allowVoice && VoiceService.isReady()) row.add(micButton(q));
                row.add(Button.builder(Component.literal("Check (Enter)"), b -> checkTyped()).width(100).build());
                layoutRow(row, inputY + 40);
            }
            case CHOICE -> {
                int bw = Math.min(160, (width - 24) / 2);
                for (int i = 0; i < q.options.size(); i++) {
                    int idx = i;
                    int x = i % 2 == 0 ? cx - bw - 2 : cx + 2;
                    int y = inputY + (i / 2) * 24;
                    addRenderableWidget(Button.builder(Component.literal((i + 1) + ".  " + q.options.get(i)), b -> choose(idx))
                            .bounds(x, y, bw, 20).build());
                }
                if (q.listening) {
                    layoutRow(replayButtons(q), bottom);
                } else if (q.speak != null && Tts.enabled() && q.promptJapanese && q.subPrompt.isEmpty()) {
                    addRenderableWidget(Button.builder(Component.literal("Listen"), b -> Listening.say(q.itemId, q.prompt)).bounds(cx - 35, bottom, 70, 20).build());
                }
            }
            case ORDER -> {
                int x = cx - 150;
                int y = inputY + 20;
                for (int i = 0; i < q.tiles.size(); i++) {
                    String tile = q.tiles.get(i);
                    int w = font.width(tile) + 14;
                    if (x + w > cx + 150) {
                        x = cx - 150;
                        y += 24;
                    }
                    int idx = i;
                    Button tb = addRenderableWidget(Button.builder(Component.literal(tile), b -> {
                        picked.add(idx);
                        rebuildWidgets();
                    }).bounds(x, y, w, 20).build());
                    tb.active = !picked.contains(i);
                    x += w + 4;
                }
                List<Button> row = new ArrayList<>();
                row.add(Button.builder(Component.literal("Undo"), b -> {
                    if (!picked.isEmpty()) picked.remove(picked.size() - 1);
                    rebuildWidgets();
                }).width(60).build());
                row.add(Button.builder(Component.literal("Check (Enter)"), b -> checkOrder()).width(100).build());
                layoutRow(row, bottom);
            }
            case SPEAK -> {
                List<Button> row = new ArrayList<>();
                row.add(Button.builder(Component.literal(hint ? "Hint shown" : "Hint"), b -> {
                    hint = true;
                    rebuildWidgets();
                }).width(70).build());
                if (VoiceService.isReady()) row.add(micButton(q));
                row.add(Button.builder(Component.literal("Skip"), b -> {
                    session.submit("skip");
                    rebuildWidgets();
                }).width(60).build());
                layoutRow(row, inputY + 10);
            }
        }
    }

    /** Replay at normal speed, or slowly (a right answer after a slow replay counts as "hard"). */
    private List<Button> replayButtons(Question q) {
        List<Button> row = new ArrayList<>();
        row.add(Button.builder(Component.literal("♪ Replay"), b -> playListening(q, false)).width(70).build());
        row.add(Button.builder(Component.literal("Slower"), b -> {
            session.usedSlowReplay();
            playListening(q, true);
        }).width(60).build());
        return row;
    }

    /** Plays a listening question in its voice (picking one the first time). */
    private void playListening(Question q, boolean slow) {
        if (q.speak == null) return;
        if (q.voice == null) {
            VoiceCatalog.Speaker s = Listening.voiceFor(q.itemId, true);
            q.voice = s == null ? null : s.id();
        }
        Listening.say(q.itemId, q.speak, q.voice == null ? null : VoiceCatalog.byId(q.voice), slow);
    }

    /** After answering: the same voice again for listening questions, otherwise whichever voice is due. */
    private void speakAnswer(Question q) {
        if (q.listening) playListening(q, false);
        else Listening.say(q.itemId, q.speak);
    }

    private Button micButton(Question q) {
        boolean rec = VoiceService.isRecording();
        return Button.builder(Component.literal(rec ? "■ Stop" : "● Speak"), b -> toggleMic(q)).width(80).build();
    }

    private void layoutRow(List<Button> row, int y) {
        int total = 0;
        for (Button b : row) total += b.getWidth() + 4;
        int x = width / 2 - (total - 4) / 2;
        for (Button b : row) {
            b.setPosition(x, y);
            addRenderableWidget(b);
            x += b.getWidth() + 4;
        }
    }

    private int inputY() {
        return Math.max(118, height / 2 + 10);
    }

    // ------------------------------------------------------------------ actions

    private void next() {
        session.advance();
        if (placement && session.finished()) {
            minecraft.setScreen(new PlacementScreen(parent, session.placement()));
            return;
        }
        // In-world prompts drop straight back into the game instead of showing a summary.
        if (session.mode() == StudySession.Mode.FIELD && session.finished()) {
            onClose();
            return;
        }
        typed = "";
        picked.clear();
        hint = false;
        status = "";
        heard = List.of();
        scroll = 0;
        VoiceService.stopRecording();
        rebuildWidgets();
    }

    private void afterAnswer(AnswerChecker.Result r) {
        if (r.verdict() == AnswerChecker.Verdict.RETRY) {
            status = r.note();
            return;
        }
        status = "";
        Ui.sound(r.passed());
        Question q = question();
        // Hearing the word right after recalling it ties sound to meaning.
        if (q != null && q.speak != null && (q.type != Question.Type.CHOICE || q.listening)) speakAnswer(q);
        rebuildWidgets();
    }

    private void checkTyped() {
        if (input == null) return;
        afterAnswer(session.submit(input.getValue()));
    }

    private void choose(int idx) {
        Question q = question();
        if (q == null || idx >= q.options.size()) return;
        afterAnswer(session.submit(q.options.get(idx)));
    }

    private void checkOrder() {
        Question q = question();
        if (q == null) return;
        if (picked.isEmpty()) {
            status = "Click the tiles in order first.";
            return;
        }
        List<String> parts = new ArrayList<>();
        for (int i : picked) parts.add(q.tiles.get(i));
        afterAnswer(session.submit(String.join(" ", parts)));
    }

    private void toggleMic(Question q) {
        if (VoiceService.isRecording()) {
            VoiceService.stopRecording();
            return;
        }
        status = "Listening... speak now, then press Stop.";
        VoiceService.startRecording(expectedWords(q), results -> {
            if (question() != q || session.isAnswered()) return;
            heard = results;
            AnswerChecker.Result r = session.submitSpoken(results);
            if (r.verdict() == AnswerChecker.Verdict.RETRY) {
                status = r.note();
                rebuildWidgets();
            } else {
                afterAnswer(r);
            }
        });
        rebuildWidgets();
    }

    /** Expected answers plus some other known words, so the constrained recogniser can also hear mistakes. */
    private List<String> expectedWords(Question q) {
        List<String> words = new ArrayList<>(q.accepted);
        List<Curriculum.VocabItem> others = new ArrayList<>(Learner.engine().introducedVocab());
        Collections.shuffle(others);
        for (int i = 0; i < others.size() && i < 15; i++) words.add(others.get(i).kana);
        return words;
    }

    private void finishOnce() {
        if (rewarded || placement) return;
        rewarded = true;
        if (session.mode() == StudySession.Mode.FIELD) Learner.rewardField(session.correctCount());
        else Learner.rewardStudy(session.correctCount());
        Learner.saveIfDirty();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (session.finished()) onClose();
            else if (session.current() instanceof StudySession.Teach) next();
            else if (session.isAnswered()) next();
            else if (question() != null && question().type == Question.Type.ORDER) checkOrder();
            else if (input != null) checkTyped();
            return true;
        }
        Question q = question();
        if (q != null && !session.isAnswered() && q.type == Question.Type.CHOICE
                && keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_4) {
            choose(keyCode - GLFW.GLFW_KEY_1);
            return true;
        }
        if (q != null && !session.isAnswered() && q.type == Question.Type.ORDER && keyCode == GLFW.GLFW_KEY_BACKSPACE && !picked.isEmpty()) {
            picked.remove(picked.size() - 1);
            rebuildWidgets();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) (scrollY * 12));
        return true;
    }

    @Override
    public void onClose() {
        VoiceService.stopRecording();
        if (session.finished()) finishOnce();
        Learner.saveIfDirty();
        minecraft.setScreen(parent instanceof HubScreen ? new HubScreen() : parent);
    }

    @Override
    public void tick() {
        // Refresh the mic button label when recording starts or stops by itself.
        boolean rec = VoiceService.isRecording();
        if (rec != wasRecording) {
            wasRecording = rec;
            if (!rec && status.startsWith("Listening")) status = "";
            rebuildWidgets();
        }
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = width / 2;
        if (session.finished()) {
            renderSummary(g, cx);
            return;
        }
        String mode = switch (session.mode()) {
            case LESSON -> "Lesson";
            case REVIEW -> "Review";
            case SPEAKING -> "Speaking practice";
            case PLACEMENT -> "Placement test";
            case FIELD -> question() != null && question().listening ? "Listening check" : "Field study";
            case LISTENING -> "Listening practice";
        };
        String left = placement ? "lesson " + session.placement().lessonNumber() + "/" + session.placement().lessonTotal()
                : session.remaining() + " left";
        String header = mode + "   •   " + left + "   •   ✔ " + session.correctCount() + "/" + session.answeredCount();
        g.drawCenteredString(font, header, cx, 8, Ui.GRAY);
        if (!Tts.lastError().isEmpty()) Ui.wrap(g, font, Tts.lastError(), cx, 20, width - 40, Ui.RED);

        if (session.current() instanceof StudySession.Teach teach) {
            g.enableScissor(0, 20, width, height - 32);
            renderTeach(g, teach, cx, 26 - scroll);
            g.disableScissor();
            return;
        }
        Question q = question();
        if (q != null) renderQuestion(g, q, cx);
    }

    private void renderTeach(GuiGraphics g, StudySession.Teach teach, int cx, int y) {
        int wrapW = Math.min(340, width - 30);
        Curriculum.Item item = teach.item();
        if (teach.kanjiOnly() && item instanceof Curriculum.VocabItem v) {
            g.drawCenteredString(font, "New kanji for a word you already know!", cx, y, Ui.CYAN);
            Ui.big(g, font, v.kanji, cx, y + 18, 5f, Ui.WHITE);
            Ui.big(g, font, v.kana + "  (" + v.romaji() + ")", cx, y + 70, 1.5f, Ui.GOLD);
            g.drawCenteredString(font, String.join(", ", v.meanings), cx, y + 88, Ui.WHITE);
            Ui.wrap(g, font, v.note, cx, y + 104, wrapW, Ui.GRAY);
            return;
        }
        if (item instanceof Curriculum.KanaItem k) {
            g.drawCenteredString(font, "New " + k.script, cx, y, Ui.CYAN);
            if (k.isGroup()) {
                Ui.big(g, font, k.displayTitle(), cx, y + 16, 1.5f, Ui.GOLD);
                int row = y + 36;
                StringBuilder line = new StringBuilder();
                int count = 0;
                for (List<String> m : k.group) {
                    line.append(m.get(0)).append(' ').append(m.get(1)).append("    ");
                    if (++count % 5 == 0) {
                        Ui.big(g, font, line.toString().trim(), cx, row, 1.5f, Ui.WHITE);
                        row += 18;
                        line.setLength(0);
                    }
                }
                if (!line.isEmpty()) {
                    Ui.big(g, font, line.toString().trim(), cx, row, 1.5f, Ui.WHITE);
                    row += 18;
                }
                Ui.wrap(g, font, k.mnemonic, cx, row + 8, wrapW, Ui.GRAY);
            } else {
                Ui.big(g, font, k.kana, cx, y + 16, 6f, Ui.WHITE);
                Ui.big(g, font, k.romaji, cx, y + 76, 2f, Ui.GOLD);
                int ny = Ui.wrap(g, font, k.mnemonic, cx, y + 100, wrapW, Ui.WHITE);
                if (k.example != null && !k.example.isEmpty()) g.drawCenteredString(font, "e.g. " + k.example, cx, ny + 6, Ui.GRAY);
            }
            return;
        }
        if (item instanceof Curriculum.VocabItem v) {
            g.drawCenteredString(font, "New " + v.pos, cx, y, Ui.CYAN);
            int top = y + 14;
            if (v.icon != null) {
                Ui.item(g, v.icon, cx, top, 2f);
                top += 36;
            }
            Ui.big(g, font, v.kana, cx, top, Ui.fit(font, v.kana, 4f, width - 40), Ui.WHITE);
            top += 40;
            String sub = v.romaji() + (v.kanji != null ? "     written: " + v.kanji : "");
            g.drawCenteredString(font, sub, cx, top, Ui.GOLD);
            Ui.big(g, font, String.join(", ", v.meanings), cx, top + 14, 1.5f, Ui.WHITE);
            int ny = Ui.wrap(g, font, v.note, cx, top + 34, wrapW, Ui.GRAY);
            if (v.example != null) {
                g.drawCenteredString(font, v.example.jp, cx, ny + 6, Ui.CYAN);
                g.drawCenteredString(font, v.example.en, cx, ny + 18, Ui.GRAY);
            }
            return;
        }
        if (item instanceof Curriculum.GrammarPoint gp) {
            g.drawCenteredString(font, "New grammar", cx, y, Ui.CYAN);
            Ui.big(g, font, gp.title, cx, y + 14, Ui.fit(font, gp.title, 1.5f, width - 30), Ui.GOLD);
            int ny = Ui.wrap(g, font, gp.summary, cx, y + 34, wrapW, Ui.WHITE) + 6;
            int left = cx - wrapW / 2;
            for (String para : gp.explanation) ny = Ui.wrapLeft(g, font, "• " + para, left, ny, wrapW, Ui.WHITE) + 3;
            ny += 4;
            g.drawString(font, "Examples:", left, ny, Ui.CYAN, false);
            ny += 12;
            for (Curriculum.Example ex : gp.examples) {
                g.drawString(font, ex.jp, left + 6, ny, Ui.WHITE, false);
                g.drawString(font, ex.en, left + 6, ny + 10, Ui.GRAY, false);
                ny += 24;
            }
            if (ny > height - 32) g.drawCenteredString(font, "(scroll for more)", cx, height - 42 + scroll, Ui.DIM);
        }
    }

    private void renderQuestion(GuiGraphics g, Question q, int cx) {
        int y = 22;
        g.drawCenteredString(font, q.instruction, cx, y, Ui.CYAN);
        y += 14;
        boolean showIcon = q.icon != null && (q.iconInPrompt || session.isAnswered());
        if (showIcon) {
            Ui.item(g, q.icon, cx, y, 2f);
            y += 36;
        }
        String promptText = q.type == Question.Type.SPEAK && hint ? q.prompt + "   →  " + q.answer : q.prompt;
        if (q.promptJapanese) {
            float scale = Ui.fit(font, promptText, promptText.length() <= 2 ? 5f : 3f, width - 30);
            Ui.big(g, font, promptText, cx, y, scale, Ui.WHITE);
            y += (int) (font.lineHeight * scale) + 6;
        } else {
            float scale = Ui.fit(font, promptText, 2f, width - 30);
            if (scale < 1f) {
                y = Ui.wrap(g, font, promptText, cx, y, width - 30, Ui.WHITE) + 4;
            } else {
                Ui.big(g, font, promptText, cx, y, scale, Ui.WHITE);
                y += (int) (font.lineHeight * scale) + 6;
            }
        }
        if (!q.subPrompt.isEmpty()) {
            g.drawCenteredString(font, q.subPrompt, cx, y, Ui.GRAY);
        }

        int inputY = inputY();
        if (!session.isAnswered()) {
            if (input != null && q.type == Question.Type.TYPE_JAPANESE && !typed.isBlank()) {
                String preview = Kana.romajiToHiragana(typed, false);
                if (!preview.equals(typed)) g.drawCenteredString(font, "→ " + preview, cx, inputY + 24, Ui.GOLD);
            }
            if (q.type == Question.Type.ORDER) {
                StringBuilder sb = new StringBuilder();
                for (int i : picked) sb.append(q.tiles.get(i)).append(' ');
                String built = sb.isEmpty() ? "(click the tiles in order)" : sb.toString().trim();
                g.drawCenteredString(font, built, cx, inputY + 4, sb.isEmpty() ? Ui.DIM : Ui.GOLD);
            }
            if (VoiceService.isRecording()) {
                int bar = (int) (VoiceService.level() * 120);
                g.fill(cx - 60, inputY - 8, cx - 60 + bar, inputY - 5, 0xFFFF80FF);
            }
            if (!status.isEmpty()) g.drawCenteredString(font, status, cx, bottomY() - 16, Ui.PINK);
            return;
        }

        AnswerChecker.Result r = session.result();
        int fy = inputY - 10;
        Ui.panel(g, cx - Math.min(180, width / 2 - 8), fy - 4, cx + Math.min(180, width / 2 - 8), height - 34);
        boolean ok = r.passed();
        Ui.big(g, font, ok ? "✔  せいかい! Correct" : "✘  ざんねん - not quite", cx, fy + 2, 1.5f, ok ? Ui.GREEN : Ui.RED);
        fy += 20;
        int wrapW = Math.min(340, width - 30);
        if (!r.note().isEmpty()) fy = Ui.wrap(g, font, r.note(), cx, fy, wrapW, Ui.GOLD);
        if (!heard.isEmpty()) fy = Ui.wrap(g, font, "Heard: " + String.join(" / ", heard.subList(0, Math.min(3, heard.size()))), cx, fy, wrapW, Ui.GRAY);
        fy = Ui.wrap(g, font, "Answer: " + q.answer, cx, fy, wrapW, Ui.WHITE);
        if (!q.reading.isEmpty()) fy = Ui.wrap(g, font, q.reading, cx, fy, wrapW, Ui.GOLD);
        if (!q.meaning.isEmpty() && !q.meaning.equals(q.answer)) fy = Ui.wrap(g, font, q.meaning, cx, fy, wrapW, Ui.WHITE);
        fy = Ui.wrap(g, font, q.detail, cx, fy + 2, wrapW, Ui.GRAY);
        VoiceCatalog.Speaker voice = q.listening && q.voice != null ? VoiceCatalog.byId(q.voice) : null;
        if (voice != null) {
            String credit = voice.credit().isEmpty() ? "" : "   (" + voice.credit() + ")";
            Ui.wrap(g, font, "Voice: " + voice.label() + credit, cx, fy + 2, wrapW, Ui.DIM);
        }
    }

    private void renderSummary(GuiGraphics g, int cx) {
        Ui.big(g, font, "おつかれさま!", cx, 20, 3f, Ui.GOLD);
        g.drawCenteredString(font, "Session complete - good work.", cx, 52, Ui.WHITE);
        int answered = session.answeredCount();
        int pct = answered == 0 ? 100 : Math.round(100f * session.correctCount() / answered);
        g.drawCenteredString(font, session.correctCount() + " / " + answered + " correct (" + pct + "%)", cx, 68, Ui.GREEN);
        int y = 88;
        if (!session.missed().isEmpty()) {
            g.drawCenteredString(font, "Worth another look (they'll come back soon):", cx, y, Ui.CYAN);
            y += 12;
            for (String id : session.missed()) {
                if (y > height - 50) break;
                g.drawCenteredString(font, Ui.label(Learner.curriculum().item(id)), cx, y, Ui.WHITE);
                y += 11;
            }
        } else {
            g.drawCenteredString(font, "Perfect run!", cx, y, Ui.CYAN);
        }
        g.drawCenteredString(font, "Sleep helps memories stick - come back tomorrow for reviews.", cx, height - 44, Ui.GRAY);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
