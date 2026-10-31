package com.jensen.nihongo.client.screen;

import com.jensen.nihongo.client.tts.Tts;
import com.jensen.nihongo.client.tts.VoiceCatalog;
import com.jensen.nihongo.client.voice.VoiceService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Download the speech model, test the microphone and test text-to-speech. */
public class VoiceSetupScreen extends Screen {
    private final Screen parent;
    private VoiceService.State lastState;
    private String testResult = "";
    private boolean lastRecording;

    public VoiceSetupScreen(Screen parent) {
        super(Component.literal("Voice & audio"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 + 10;
        VoiceService.State state = VoiceService.state();
        lastState = state;
        lastRecording = VoiceService.isRecording();

        Button download = addRenderableWidget(Button.builder(Component.literal(
                        state == VoiceService.State.READY ? "Speech model installed" : "Download Japanese speech model (~50 MB)"),
                b -> {
                    VoiceService.download();
                    rebuildWidgets();
                }).bounds(cx - 120, y, 240, 20).build());
        download.active = state == VoiceService.State.NO_MODEL || state == VoiceService.State.ERROR;

        Button test = addRenderableWidget(Button.builder(Component.literal(VoiceService.isRecording() ? "■ Stop" : "Test microphone (say こんにちは)"),
                b -> testMic()).bounds(cx - 120, y + 24, 240, 20).build());
        test.active = state == VoiceService.State.READY;

        Button tts = addRenderableWidget(Button.builder(Component.literal("Test speech: こんにちは"), b -> Tts.speak("こんにちは、 にほんご を べんきょう しましょう。"))
                .bounds(cx - 120, y + 48, 240, 20).build());
        tts.active = Tts.enabled();

        int voices = VoiceCatalog.active().size();
        addRenderableWidget(Button.builder(Component.literal("Voices (" + voices + " on)..."), b -> minecraft.setScreen(new VoicesScreen(this)))
                .bounds(cx - 120, y + 72, 240, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(cx - 50, height - 28, 100, 20).build());
    }

    private void testMic() {
        if (VoiceService.isRecording()) {
            VoiceService.stopRecording();
            return;
        }
        testResult = "Listening... say こんにちは, then press Stop.";
        VoiceService.startRecording(List.of("こんにちは", "ありがとう", "さようなら"), results -> {
            testResult = results.isEmpty() ? "Heard nothing. Check your microphone and its permissions." : "Heard: " + String.join(" / ", results);
            rebuildWidgets();
        });
        rebuildWidgets();
    }

    @Override
    public void tick() {
        if (VoiceService.state() != lastState || VoiceService.isRecording() != lastRecording) rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int w = Math.min(380, width - 20);
        g.drawCenteredString(font, "Voice & audio", cx, 12, Ui.GOLD);
        int y = Ui.wrap(g, font, "Speaking practice and spells use Vosk, an offline speech recogniser: your voice never leaves your computer. "
                + "It needs a one-time download of a Japanese model.", cx, 28, w, Ui.WHITE) + 4;
        String state = switch (VoiceService.state()) {
            case DISABLED -> "Voice recognition is turned off in the config.";
            case NO_MODEL -> "Speech model not installed.";
            case DOWNLOADING -> String.format("Downloading... %d%%", Math.round(VoiceService.downloadProgress() * 100));
            case LOADING -> "Loading the speech model...";
            case READY -> "Ready! Hold V in-world to speak a spell; use the Speak buttons while studying.";
            case ERROR -> VoiceService.status();
        };
        y = Ui.wrap(g, font, state, cx, y, w, VoiceService.state() == VoiceService.State.ERROR ? Ui.RED : Ui.CYAN) + 2;
        if (VoiceService.state() == VoiceService.State.DOWNLOADING) {
            int bw = 200;
            g.fill(cx - bw / 2, y, cx + bw / 2, y + 4, 0xFF303030);
            g.fill(cx - bw / 2, y, cx - bw / 2 + (int) (bw * VoiceService.downloadProgress()), y + 4, 0xFF66E07A);
            y += 8;
        }
        if (!testResult.isEmpty()) y = Ui.wrap(g, font, testResult, cx, y, w, Ui.GOLD) + 2;
        if (VoiceService.isRecording()) {
            int bar = (int) (VoiceService.level() * 200);
            g.fill(cx - 100, y, cx - 100 + bar, y + 3, 0xFFFF80FF);
        }
        int below = height / 2 + 106;
        Ui.wrap(g, font, "Listening uses your system's Japanese voices, in several styles. Windows: Settings > Time & language > Speech > "
                + "add Japanese. macOS: Kyoko and Otoya. Linux: espeak-ng. For many natural voices, run VOICEVOX (free).", cx, below, w, Ui.GRAY);
        if (!Tts.lastError().isEmpty()) Ui.wrap(g, font, Tts.lastError(), cx, below + 32, w, Ui.RED);
    }

    @Override
    public void onClose() {
        VoiceService.stopRecording();
        minecraft.setScreen(parent instanceof HubScreen ? new HubScreen() : parent);
    }
}
