package com.persiki84.mediaplayer.render;

public enum Weight {
    REGULAR("inter"),
    SEMIBOLD("inter_semibold");

    private final String face;

    Weight(String face) {
        this.face = face;
    }

    String face() {
        return face;
    }
}
