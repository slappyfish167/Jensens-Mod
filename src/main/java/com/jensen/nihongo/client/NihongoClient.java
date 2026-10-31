package com.jensen.nihongo.client;

import com.jensen.nihongo.NihongoMod;
import com.jensen.nihongo.client.tts.VoiceCatalog;
import com.jensen.nihongo.client.voice.VoiceService;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = NihongoMod.MODID, dist = Dist.CLIENT)
public class NihongoClient {
    public NihongoClient(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(NihongoClient::onRegisterKeys);
        modEventBus.addListener(NihongoClient::onRegisterLayers);
        modEventBus.addListener(NihongoClient::onRegisterReloadListeners);
        modEventBus.addListener(NihongoClient::onClientSetup);

        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onTooltip);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onLoggingIn);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(Encounters::onAttack);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN_HUB);
        event.register(KeyBindings.SPEAK);
        event.register(KeyBindings.QUEST_HINT);
        event.register(KeyBindings.FIELD_STUDY);
    }

    private static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(NihongoMod.id("immersion"), ImmersionHud::render);
    }

    private static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new CurriculumLoader());
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(VoiceService::initFromDisk);
        event.enqueueWork(VoiceCatalog::refresh);
    }
}
