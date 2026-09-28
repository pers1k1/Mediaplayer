package com.persiki84.mediaplayer.render;

import com.persiki84.mediaplayer.anim.Anim;
import net.minecraft.util.Util;

public final class Marquee {
    private static final float UNITS_PER_SECOND = 9.0f;
    private static final float DWELL_SECONDS = 1.8f;
    private static final float MIN_TRAVEL = 0.5f;
    private static final long FORGET_MS = 400L;
    private static final int SLOTS = 32;

    private static final int[] keys = new int[SLOTS];
    private static final long[] started = new long[SLOTS];
    private static final long[] seen = new long[SLOTS];

    private Marquee() {}

    public static float shift(String text, float travel) {
        if (travel <= MIN_TRAVEL) return 0.0f;

        long now = Util.getMillis();
        float seconds = (now - startOf(text.hashCode(), now)) / 1000.0f;
        float run = travel / UNITS_PER_SECOND;
        float phase = seconds % ((DWELL_SECONDS + run) * 2.0f);
        if (phase < DWELL_SECONDS) return 0.0f;
        if (phase < DWELL_SECONDS + run) return travel * Anim.smoothstep(0.0f, run, phase - DWELL_SECONDS);
        if (phase < DWELL_SECONDS * 2.0f + run) return travel;
        return travel * (1.0f - Anim.smoothstep(0.0f, run, phase - DWELL_SECONDS * 2.0f - run));
    }

    // WHY: время строки идёт от её появления, а не от общих часов: иначе название, пришедшее
    // WHY: посреди хода, появлялось уже уехавшим, и начало слова игрок не видел вовсе
    private static long startOf(int key, long now) {
        int stale = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            if (keys[slot] == key && seen[slot] != 0L && now - seen[slot] <= FORGET_MS) {
                seen[slot] = now;
                return started[slot];
            }
            if (seen[slot] < seen[stale]) stale = slot;
        }
        keys[stale] = key;
        started[stale] = now;
        seen[stale] = now;
        return now;
    }
}
