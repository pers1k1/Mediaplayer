package com.persiki84.mediaplayer.config;

public enum IslandDial {
    CARD_SECONDS("cardSeconds", 5.0f, 1.0f, 30.0f),
    VISUALIZER_GAIN("visualizerGain", 1.0f, 0.25f, 2.0f),
    VISUALIZER_SPEED("visualizerSpeed", 1.0f, 0.25f, 2.0f),
    VISUALIZER_ATTACK("visualizerAttack", 1.0f, 0.25f, 2.0f),
    VISUALIZER_LIGHT("visualizerLight", 1.0f, 0.25f, 2.0f),
    VISUALIZER_COLOR("visualizerColor", 1.0f, 0.25f, 2.0f),
    FLIP_SPEED("flipSpeed", 1.0f, 0.25f, 2.0f);

    private final String key;
    private final float fallback;
    private final float min;
    private final float max;

    IslandDial(String key, float fallback, float min, float max) {
        this.key = key;
        this.fallback = fallback;
        this.min = min;
        this.max = max;
    }

    public String key() {
        return key;
    }

    public float fallback() {
        return fallback;
    }

    public float min() {
        return min;
    }

    public float max() {
        return max;
    }

    public float clamp(float value) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }

    public String translationKey() {
        return "mediaplayer.settings.dial." + key;
    }

    public String hintKey() {
        return translationKey() + ".hint";
    }
}
