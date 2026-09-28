package com.persiki84.mediaplayer.anim;

public final class FrameClock {
    private static final float MAX_DELTA = 0.1f;

    private static long lastNanos;
    private static long frame;
    private static float delta;

    private FrameClock() {}

    public static void advance() {
        frame++;
        long now = System.nanoTime();
        delta = lastNanos == 0L ? 0.0f : Math.min((now - lastNanos) / 1_000_000_000.0f, MAX_DELTA);
        lastNanos = now;
    }

    public static long frame() {
        return frame;
    }

    public static float delta() {
        return delta;
    }
}
