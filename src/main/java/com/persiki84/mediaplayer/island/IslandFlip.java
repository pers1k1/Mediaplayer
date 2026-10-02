package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.config.IslandDial;
import com.persiki84.mediaplayer.config.IslandSettings;

// WHY: обложка поворачивается как карточка: лицевая сторона это уходящий трек, обратная это
// WHY: пришедший, а сторона поворота показывает, листнул игрок вперёд или назад (IslandOrder)
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

    public static float angle() {
        return way * (float) Math.PI * Anim.smoothstep(0.0f, 1.0f, phase);
    }

    public static float haze() {
        return 1.0f - Math.abs((float) Math.cos(angle()));
    }

    public static void forget() {
        phase = 1.0f;
    }
}
