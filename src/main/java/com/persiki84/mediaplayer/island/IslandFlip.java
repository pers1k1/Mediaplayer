package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.config.IslandDial;
import com.persiki84.mediaplayer.config.IslandSettings;

// WHY: обложка не гаснет и не появляется заново, а поворачивается: лицевая сторона это уходящий
// WHY: трек, обратная это пришедший, поэтому у переворота одна доля хода на обе стороны
public final class IslandFlip {
    private static final float TURN_SECONDS = 1.05f;

    private static float phase = 1.0f;
    private static float way = 1.0f;

    private IslandFlip() {}

    public static void begin(int direction) {
        way = direction < 0 ? -1.0f : 1.0f;
        phase = 0.0f;
    }

    public static void advance(float delta) {
        if (phase >= 1.0f) return;

        phase = Math.min(1.0f, phase + delta * IslandSettings.dial(IslandDial.FLIP_SPEED) / TURN_SECONDS);
    }

    public static boolean turning() {
        return phase < 1.0f;
    }

    public static boolean fresh() {
        return turned() >= 0.5f;
    }

    public static float squeeze() {
        return Math.abs((float) Math.cos(Math.PI * turned()));
    }

    public static float way() {
        return way;
    }

    public static void forget() {
        phase = 1.0f;
    }

    private static float turned() {
        return Anim.smoothstep(0.0f, 1.0f, phase);
    }
}
