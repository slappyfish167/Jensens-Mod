package com.jensen.nihongo.client;

import com.jensen.nihongo.Spells;
import com.jensen.nihongo.client.screen.HubScreen;
import com.jensen.nihongo.client.voice.VoiceService;
import com.jensen.nihongo.learning.AnswerChecker;
import com.jensen.nihongo.learning.Curriculum;
import com.jensen.nihongo.learning.LearningEngine;
import com.jensen.nihongo.learning.McIndex;
import com.jensen.nihongo.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;

public final class ClientEvents {
    private ClientEvents() {}

    private static boolean speakWasDown;
    private static int ticks;

    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        while (KeyBindings.OPEN_HUB.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new HubScreen());
        }
        while (KeyBindings.QUEST_HINT.consumeClick()) {
            QuestTracker.toggleHint();
        }
        while (KeyBindings.FIELD_STUDY.consumeClick()) {
            Encounters.onKey(mc);
        }
        if (mc.player != null && mc.screen == null) {
            handlePushToTalk(mc);
        } else if (speakWasDown) {
            speakWasDown = false;
            VoiceService.stopRecording();
        }
        if (mc.player != null && ticks % 20 == 0) QuestTracker.tick(mc);
        if (mc.player != null) Encounters.tick(mc, ticks);
        if (ticks % 200 == 0) Learner.saveIfDirty();
    }

    static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        LearningEngine engine = Learner.engine();
        int due = engine.dueCount();
        Component msg;
        if (engine.progress().introduced.isEmpty()) {
            msg = Component.literal("ようこそ! Press J (or craft a Japanese Textbook: book + red dye) to start learning Japanese. "
                    + "Once you know your kana, things you mine, collect or fight will offer quick questions: press K to answer.");
        } else if (due > 0) {
            msg = Component.literal("おかえり! You have " + due + " Japanese reviews waiting - press J.");
        } else {
            return;
        }
        event.getPlayer().displayClientMessage(msg.copy().withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Learner.saveIfDirty();
        QuestTracker.reset();
        Encounters.reset();
    }

    // ------------------------------------------------------------------ spells

    private static void handlePushToTalk(Minecraft mc) {
        boolean down = KeyBindings.SPEAK.isDown();
        if (down && !speakWasDown) {
            if (!spellsEnabled()) {
                mc.player.displayClientMessage(Component.literal("Voice spells are turned off in the NihongoCraft config."), true);
            } else if (!VoiceService.isReady()) {
                mc.player.displayClientMessage(Component.literal("Set up voice recognition first (press J > Voice & audio)."), true);
            } else {
                List<Spells.Spell> known = knownSpells();
                if (known.isEmpty()) {
                    mc.player.displayClientMessage(Component.literal("Learn words like ひかり, みず or はやい to unlock spoken spells."), true);
                } else {
                    VoiceService.startRecording(spellWords(known), ClientEvents::onSpellHeard);
                }
            }
        } else if (!down && speakWasDown) {
            VoiceService.stopRecording();
        }
        speakWasDown = down;
    }

    private static boolean spellsEnabled() {
        try {
            return ClientConfig.SPELLS.get();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private static List<Spells.Spell> knownSpells() {
        LearningEngine engine = Learner.engine();
        List<Spells.Spell> out = new ArrayList<>();
        for (Spells.Spell s : Spells.ALL) {
            if (engine.isLearned(s.vocabId())) out.add(s);
        }
        return out;
    }

    private static List<String> spellWords(List<Spells.Spell> spells) {
        List<String> words = new ArrayList<>();
        for (Spells.Spell s : spells) {
            if (Learner.curriculum().item(s.vocabId()) instanceof Curriculum.VocabItem v) words.addAll(v.acceptedReadings());
        }
        return words;
    }

    private static void onSpellHeard(List<String> transcripts) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        for (Spells.Spell s : knownSpells()) {
            if (!(Learner.curriculum().item(s.vocabId()) instanceof Curriculum.VocabItem v)) continue;
            if (AnswerChecker.checkSpoken(transcripts, v.acceptedReadings()).passed()) {
                if (!Learner.sendToServer(new ModNetwork.CastSpell(s.id()))) {
                    mc.player.displayClientMessage(Component.literal("「" + v.kana + "」 - this server doesn't have NihongoCraft, so spells can't take effect."), true);
                } else {
                    Learner.engine().progress().spellsCast++;
                    Learner.markDirty();
                }
                return;
            }
        }
        String heard = transcripts.isEmpty() ? "..." : transcripts.get(0);
        mc.player.displayClientMessage(Component.literal("Heard: " + heard + " - no spell.").withStyle(ChatFormatting.GRAY), true);
    }

    // ------------------------------------------------------------------ tooltips

    static void onTooltip(ItemTooltipEvent event) {
        if (event.getEntity() == null || event.getItemStack().isEmpty()) return;
        try {
            if (!ClientConfig.TOOLTIPS.get()) return;
        } catch (IllegalStateException e) {
            return;
        }
        String id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()).toString();
        Curriculum.VocabItem v = Labels.bestKnown(McIndex.Type.ITEM, id);
        if (v == null) return;
        List<Component> tip = event.getToolTip();
        int at = Math.min(1, tip.size());
        tip.add(at, Component.literal(Labels.format(v)).withStyle(ChatFormatting.GOLD));
    }
}
