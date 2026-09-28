package com.persiki84.mediaplayer.config;

import java.util.EnumMap;
import java.util.Map;

public final class IslandSettings {
    private static final Map<IslandFlag, Boolean> flags = new EnumMap<>(IslandFlag.class);
    private static final Map<IslandDial, Float> dials = new EnumMap<>(IslandDial.class);
    private static IslandPlacement placement = IslandPlacement.DEFAULT;

    static {
        reset();
    }

    private IslandSettings() {}

    public static boolean on(IslandFlag flag) {
        return flags.get(flag);
    }

    public static void set(IslandFlag flag, boolean value) {
        flags.put(flag, value);
        SettingsStore.changed();
    }

    public static float dial(IslandDial dial) {
        return dials.get(dial);
    }

    public static void set(IslandDial dial, float value) {
        dials.put(dial, dial.clamp(value));
        SettingsStore.changed();
    }

    public static IslandPlacement placement() {
        return placement;
    }

    public static void place(IslandPlacement next) {
        placement = next;
        SettingsStore.changed();
    }

    static void reset() {
        for (IslandFlag flag : IslandFlag.values()) flags.put(flag, true);
        for (IslandDial dial : IslandDial.values()) dials.put(dial, dial.fallback());
        placement = IslandPlacement.DEFAULT;
    }

    static void load(IslandFlag flag, boolean value) {
        flags.put(flag, value);
    }

    static void load(IslandDial dial, float value) {
        dials.put(dial, dial.clamp(value));
    }

    static void load(IslandPlacement value) {
        placement = value;
    }
}
