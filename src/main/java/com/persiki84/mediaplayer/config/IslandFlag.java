package com.persiki84.mediaplayer.config;

public enum IslandFlag {
    MEDIA("media"),
    CARD("card"),
    AVATAR("avatar"),
    NICK("nick"),
    FPS("fps"),
    PING("ping"),
    COVER("cover"),
    COVER_TINT("coverTint"),
    TITLE("title"),
    ARTIST("artist"),
    TIME("time"),
    BAR("bar"),
    VISUALIZER("visualizer");

    private final String key;

    IslandFlag(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public String translationKey() {
        return "mediaplayer.settings.flag." + key;
    }

    public String hintKey() {
        return translationKey() + ".hint";
    }
}
