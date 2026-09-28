package com.persiki84.mediaplayer.color;

import com.persiki84.mediaplayer.anim.Anim;

public final class Colors {
    private Colors() {}

    public static int alpha(int color, float share) {
        int alpha = (int) (((color >>> 24) & 0xFF) * Anim.clamp01(share));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static int withAlpha(int color, float value) {
        int alpha = (int) (Anim.clamp01(value) * 255.0f);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static int mix(int from, int to, float ratio) {
        float share = Anim.clamp01(ratio);
        int alpha = channel(from >>> 24, to >>> 24, share);
        int red = channel((from >> 16) & 0xFF, (to >> 16) & 0xFF, share);
        int green = channel((from >> 8) & 0xFF, (to >> 8) & 0xFF, share);
        int blue = channel(from & 0xFF, to & 0xFF, share);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static int lighten(int color, float amount) {
        float share = Anim.clamp01(amount);
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        red = (int) (red + (255 - red) * share);
        green = (int) (green + (255 - green) * share);
        blue = (int) (blue + (255 - blue) * share);
        return (color & 0xFF000000) | (red << 16) | (green << 8) | blue;
    }

    public static float opacity(int color) {
        return ((color >>> 24) & 0xFF) / 255.0f;
    }

    private static int channel(int from, int to, float share) {
        return (int) (from + (to - from) * share) & 0xFF;
    }
}
