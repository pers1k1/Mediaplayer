package com.persiki84.mediaplayer.config;

public enum IslandFlag {
    MEDIA("media"),
    CARD("card"),
    AVATAR("avatar"),
    NICK("nick"),
    FPS("fps"),
    PING("ping"),
    STATS_WITH_MEDIA("statsWithMedia"),
    COVER("cover"),
    COVER_TINT("coverTint"),
    TITLE("title"),
    ARTIST("artist"),
    LYRICS("lyrics"),
    SPOTIFY_BRIDGE("spotifyBridge"),
    TIME("time"),
    BAR("bar"),
    VISUALIZER("visualizer"),
    MOD_FONT("modFont");

    private final String key;

    IslandFlag(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public String translationKey() {
        return "glassmediaplayer.settings.flag." + key;
    }

    public String hintKey() {
        return translationKey() + ".hint";
    }
}
