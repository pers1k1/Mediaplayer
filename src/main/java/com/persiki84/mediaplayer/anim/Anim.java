package com.persiki84.mediaplayer.anim;

public final class Anim {
    private static final float SETTLED = 0.0008f;

    private Anim() {}

    public static float approach(float current, float target, float speed, float delta) {
        if (delta <= 0.0f) return current;

        float next = target + (current - target) * (float) Math.exp(-speed * delta);
        return Math.abs(target - next) < SETTLED ? target : next;
    }

    public static float easeOut(float weight) {
        float inverse = 1.0f - clamp01(weight);
        return 1.0f - inverse * inverse * inverse;
    }

    // WHY: стандартная кривая easeOutBack (Penner): c1 = 1.70158 даёт перелёт на 10 %, c3 = c1 + 1
    public static float easeOutBack(float weight) {
        float shifted = clamp01(weight) - 1.0f;
        return 1.0f + 2.70158f * shifted * shifted * shifted + 1.70158f * shifted * shifted;
    }

    public static float smoothstep(float from, float to, float value) {
        if (to - from <= 0.0f) return value < to ? 0.0f : 1.0f;

        float share = clamp01((value - from) / (to - from));
        return share * share * (3.0f - 2.0f * share);
    }

    public static float lerp(float from, float to, float weight) {
        return from + (to - from) * weight;
    }

    public static float clamp(float value, float low, float high) {
        return Math.max(low, Math.min(high, value));
    }

    public static float clamp01(float value) {
        return clamp(value, 0.0f, 1.0f);
    }
}
