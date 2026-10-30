package com.jensen.nihongo.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    private KeyBindings() {}

    public static final String CATEGORY = "key.categories.nihongo";

    public static final KeyMapping OPEN_HUB = new KeyMapping("key.nihongo.open_hub",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY);

    /** Hold to talk: in-world it listens for spells. */
    public static final KeyMapping SPEAK = new KeyMapping("key.nihongo.speak",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);

    public static final KeyMapping QUEST_HINT = new KeyMapping("key.nihongo.quest_hint",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);
}
