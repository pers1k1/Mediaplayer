package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.media.MediaTrack;

public final class IslandProgress {
    private static final TrackProgress HUD = new TrackProgress();

    private IslandProgress() {}

    public static void advance(MediaTrack track, float delta) {
        HUD.advance(track, delta);
    }

    public static void forget() {
        HUD.forget();
    }

    public static float value() {
        return HUD.value();
    }

    public static float surge() {
        return HUD.surge();
    }
}
