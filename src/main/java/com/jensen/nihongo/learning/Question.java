package com.jensen.nihongo.learning;

import java.util.ArrayList;
import java.util.List;

/** One prompt shown to the learner, built from a card when it comes up. */
public final class Question {
    public enum Type {
        /** Answer typed in Japanese (romaji is converted to kana automatically). */
        TYPE_JAPANESE,
        /** Answer typed in English. */
        TYPE_MEANING,
        /** Pick one of several options. */
        CHOICE,
        /** Put word tiles in the right order. */
        ORDER,
        /** Say the answer out loud (voice recognition). */
        SPEAK
    }

    public final String cardId;
    public final String itemId;
    public final Type type;

    public String prompt = "";
    /** Render the prompt large, as Japanese text. */
    public boolean promptJapanese;
    public String subPrompt = "";
    public String instruction = "";
    public List<String> accepted = new ArrayList<>();
    public List<String> options = new ArrayList<>();
    public List<String> tiles = new ArrayList<>();

    /** Shown after answering. */
    public String answer = "";
    public String reading = "";
    public String meaning = "";
    public String detail = "";

    /** Minecraft item id to draw as a picture (dual coding). */
    public String icon;
    public boolean iconInPrompt;
    /** Japanese text that can be spoken by text-to-speech. */
    public String speak;
    /** Whether listening is allowed before answering (it is a listening prompt, not a giveaway). */
    public boolean speakInPrompt;
    /** Whether the learner may answer by voice instead of typing. */
    public boolean allowVoice;
    /** A listening question: the Japanese is only heard, never shown, until it is answered. */
    public boolean listening;
    /** Id of the voice that reads this question aloud (chosen by the client), or null. */
    public String voice;

    public Question(String cardId, String itemId, Type type) {
        this.cardId = cardId;
        this.itemId = itemId;
        this.type = type;
    }
}
