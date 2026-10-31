package com.jensen.nihongo.client.screen;

import com.jensen.nihongo.client.ClientConfig;
import com.jensen.nihongo.client.Learner;
import com.jensen.nihongo.client.tts.Tts;
import com.jensen.nihongo.client.tts.VoiceCatalog;
import com.jensen.nihongo.client.tts.VoiceCatalog.Speaker;
import com.jensen.nihongo.learning.Progress;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The voices that read Japanese aloud: test each one, switch voices on or off, choose the primary voice
 * (which reads new words first) and how often the others take over. Shows how well you understand each.
 */
public class VoicesScreen extends Screen {
    private static final int ROW = 22;
    private static final String SAMPLE = "こんにちは。 きょう は いい てんき です ね。";

    private final Screen parent;
    private int scroll;
    private VoiceCatalog.State lastState;
    private int lastCount;

    public VoicesScreen(Screen parent) {
        super(Component.literal("Voices"));
        this.parent = parent;
    }

    private int listTop() {
        return 54;
    }

    private int visibleRows() {
        return Math.max(1, (height - 60 - listTop()) / ROW);
    }

    @Override
    protected void init() {
        lastState = VoiceCatalog.state();
        List<Speaker> all = VoiceCatalog.all();
        lastCount = all.size();
        scroll = Math.max(0, Math.min(scroll, all.size() - visibleRows()));
        int right = width / 2 + Math.min(190, width / 2 - 8);
        for (int i = 0; i < visibleRows() && scroll + i < all.size(); i++) {
            Speaker s = all.get(scroll + i);
            int y = listTop() + i * ROW;
            boolean on = VoiceCatalog.isEnabled(s);
            boolean primary = VoiceCatalog.isPrimary(s);
            Button star = addRenderableWidget(Button.builder(Component.literal(primary ? "★" : "☆"), b -> {
                VoiceCatalog.setPrimary(s);
                rebuildWidgets();
            }).bounds(right - 20, y, 20, 20).build());
            star.setTooltip(Tooltip.create(Component.literal(primary ? "Primary voice: reads new words first."
                    : "Make this the primary voice (it reads new words first).")));
            Button toggle = addRenderableWidget(Button.builder(Component.literal(on ? "On" : "Off"), b -> {
                VoiceCatalog.setEnabled(s, !on);
                rebuildWidgets();
            }).bounds(right - 62, y, 40, 20).build());
            toggle.active = !primary;
            addRenderableWidget(Button.builder(Component.literal("▶"), b -> Tts.speak(SAMPLE, s, false))
                    .bounds(right - 84, y, 20, 20).build());
        }

        int cx = width / 2;
        int bottom = height - 28;
        ClientConfig.VoiceVariety variety = variety();
        Button v = addRenderableWidget(Button.builder(Component.literal("Variety: " + switch (variety) {
            case ONE_VOICE -> "one voice";
            case SOMETIMES -> "sometimes";
            case OFTEN -> "often";
        }), b -> {
            ClientConfig.VoiceVariety next = ClientConfig.VoiceVariety.values()[(variety.ordinal() + 1) % ClientConfig.VoiceVariety.values().length];
            ClientConfig.VOICE_VARIETY.set(next);
            ClientConfig.VOICE_VARIETY.save();
            rebuildWidgets();
        }).bounds(cx - 154, bottom, 120, 20).build());
        v.setTooltip(Tooltip.create(Component.literal("How often voices other than the primary one read words aloud. "
                + "Listening checks always rotate voices, so you learn the word, not one speaker.")));
        Button rescan = addRenderableWidget(Button.builder(Component.literal("Find voices again"), b -> {
            VoiceCatalog.refresh();
            rebuildWidgets();
        }).bounds(cx - 30, bottom, 110, 20).build());
        rescan.active = VoiceCatalog.state() == VoiceCatalog.State.READY;
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(cx + 84, bottom, 70, 20).build());
    }

    private static ClientConfig.VoiceVariety variety() {
        try {
            return ClientConfig.VOICE_VARIETY.get();
        } catch (IllegalStateException e) {
            return ClientConfig.VoiceVariety.SOMETIMES;
        }
    }

    @Override
    public void tick() {
        if (VoiceCatalog.state() != lastState || VoiceCatalog.all().size() != lastCount) rebuildWidgets();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, VoiceCatalog.all().size() - visibleRows());
        int next = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
        if (next != scroll) {
            scroll = next;
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int w = Math.min(380, width - 20);
        g.drawCenteredString(font, "Voices", cx, 10, Ui.GOLD);
        Ui.wrap(g, font, "Words are read by different speakers so you learn to recognise them in any voice. "
                + "★ marks the primary voice, which reads new words first.", cx, 24, w, Ui.GRAY);

        List<Speaker> all = VoiceCatalog.all();
        int left = cx - Math.min(190, width / 2 - 8);
        int labelW = Math.min(380, width - 16) - 92;
        if (all.isEmpty()) {
            String msg = VoiceCatalog.state() == VoiceCatalog.State.SCANNING ? "Looking for Japanese voices..."
                    : "No Japanese voices found. Windows: Settings > Time & language > Speech > add Japanese voices. "
                    + "macOS: add Kyoko and Otoya. Linux: install espeak-ng. Or run VOICEVOX for many natural voices.";
            Ui.wrap(g, font, msg, cx, listTop() + 6, w, VoiceCatalog.state() == VoiceCatalog.State.SCANNING ? Ui.CYAN : Ui.RED);
            return;
        }
        Progress p = Learner.engine().progress();
        for (int i = 0; i < visibleRows() && scroll + i < all.size(); i++) {
            Speaker s = all.get(scroll + i);
            int y = listTop() + i * ROW;
            int color = VoiceCatalog.isEnabled(s) ? Ui.WHITE : Ui.DIM;
            g.drawString(font, font.plainSubstrByWidth(s.label(), labelW), left, y + 2, color, false);
            Progress.Tally t = p.listenByVoice.get(s.id());
            String sub = t == null || t.total == 0 ? "not heard in a listening check yet"
                    : "understood " + t.right + "/" + t.total + " (" + Math.round(100.0 * t.right / t.total) + "%)";
            if (!s.credit().isEmpty()) sub = s.credit() + "   " + sub;
            g.drawString(font, font.plainSubstrByWidth(sub, labelW), left, y + 12, Ui.DIM, false);
        }
        if (all.size() > visibleRows()) {
            g.drawCenteredString(font, "(scroll for more: " + all.size() + " voices)", cx, height - 42, Ui.DIM);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
