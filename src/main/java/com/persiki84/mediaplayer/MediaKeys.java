package com.persiki84.mediaplayer;

import com.mojang.blaze3d.platform.InputConstants;
import com.persiki84.mediaplayer.media.MediaControl;
import com.persiki84.mediaplayer.screen.SettingsScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class MediaKeys {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Mediaplayer.id("main"));

    private static KeyMapping settings;
    private static KeyMapping toggle;
    private static KeyMapping next;
    private static KeyMapping previous;

    private MediaKeys() {}

    public static void register() {
        settings = bind("settings", GLFW.GLFW_KEY_K);
        toggle = bind("toggle", InputConstants.UNKNOWN.getValue());
        next = bind("next", InputConstants.UNKNOWN.getValue());
        previous = bind("previous", InputConstants.UNKNOWN.getValue());
    }

    private static KeyMapping bind(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping("key.mediaplayer." + name, InputConstants.Type.KEYSYM,
                key, CATEGORY));
    }

    public static void tick(Minecraft minecraft) {
        while (settings.consumeClick()) {
            if (minecraft.screen == null) minecraft.setScreen(new SettingsScreen(null));
        }
        while (toggle.consumeClick()) MediaControl.togglePlay();
        while (next.consumeClick()) MediaControl.next();
        while (previous.consumeClick()) MediaControl.previous();
    }
}
