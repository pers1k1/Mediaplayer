package com.persiki84.mediaplayer;

import com.persiki84.mediaplayer.config.IslandFlag;
import com.persiki84.mediaplayer.config.IslandSettings;
import com.persiki84.mediaplayer.config.SettingsStore;
import com.persiki84.mediaplayer.edit.ChatDrag;
import com.persiki84.mediaplayer.island.IslandHud;
import com.persiki84.mediaplayer.island.IslandModel;
import com.persiki84.mediaplayer.lyrics.SpotifyBridge;
import com.persiki84.mediaplayer.media.MediaBridge;
import com.persiki84.mediaplayer.media.MediaWatch;
import com.persiki84.mediaplayer.render.GlassBackdrop;
import com.persiki84.mediaplayer.render.GlassPipelines;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;

public final class MediaplayerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SettingsStore.load();
        GlassPipelines.load();
        MediaKeys.register();
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Mediaplayer.id("island"), IslandHud::render);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> ChatDrag.attach(screen));
        ClientTickEvents.END_CLIENT_TICK.register(MediaplayerClient::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(MediaplayerClient::stop);
    }

    private static boolean inWorld;

    private static void tick(Minecraft minecraft) {
        boolean playing = minecraft.player != null;
        if (inWorld && !playing) IslandModel.forget();
        inWorld = playing;
        MediaWatch.want(playing && IslandSettings.on(IslandFlag.MEDIA));
        MediaWatch.tick();
        SpotifyBridge.want(IslandSettings.on(IslandFlag.LYRICS) && IslandSettings.on(IslandFlag.SPOTIFY_BRIDGE));
        if (playing) IslandModel.pollStats();
        MediaKeys.tick(minecraft);
        SettingsStore.tick();
    }

    private static void stop(Minecraft minecraft) {
        SettingsStore.flush();
        MediaBridge.stop();
        SpotifyBridge.stop();
        GlassBackdrop.release();
    }
}
