package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.config.IslandDial;
import com.persiki84.mediaplayer.config.IslandSettings;

// WHY: обложка не разворачивается ребром, где она на миг становится полоской и читается как
// WHY: пропавшая, а перетекает на месте: старая размывается и гаснет ровно настолько, насколько
// WHY: проявилась новая
public final class IslandCoverSwap {
    private static final float SWAP_SECONDS = 0.6f;

    private static float phase = 1.0f;

    private IslandCoverSwap() {}

    public static void begin() {
        phase = 0.0f;
    }

    public static void advance(float delta) {
        if (phase >= 1.0f) return;

        phase = Math.min(1.0f, phase + delta * IslandSettings.dial(IslandDial.FLIP_SPEED) / SWAP_SECONDS);
    }

    public static boolean swapping() {
        return phase < 1.0f;
    }

    public static float share() {
        return Anim.smoothstep(0.0f, 1.0f, phase);
    }

    public static float haze() {
        float share = share();
        return 4.0f * share * (1.0f - share);
    }

    public static void forget() {
        phase = 1.0f;
    }
}
