package com.jensen.nihongo.client;

import com.jensen.nihongo.client.screen.HubScreen;
import net.minecraft.client.Minecraft;

/** Entry points called from common code; only ever invoked on the client. */
public final class ClientHooks {
    private ClientHooks() {}

    public static void openHub() {
        Minecraft.getInstance().setScreen(new HubScreen());
    }
}
