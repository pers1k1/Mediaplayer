package com.persiki84.mediaplayer.config;

import com.persiki84.mediaplayer.anim.Anim;

public record IslandPlacement(float centerShare, float topShare, float scale) {
    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 2.5f;
    public static final IslandPlacement DEFAULT = new IslandPlacement(0.5f, 0.012f, 1.0f);

    public IslandPlacement {
        centerShare = Float.isFinite(centerShare) ? Anim.clamp01(centerShare) : 0.5f;
        topShare = Float.isFinite(topShare) ? Anim.clamp01(topShare) : 0.012f;
        scale = Float.isFinite(scale) ? Anim.clamp(scale, MIN_SCALE, MAX_SCALE) : 1.0f;
    }

    public IslandPlacement moved(float center, float top) {
        return new IslandPlacement(Anim.clamp01(center), Anim.clamp01(top), scale);
    }

    public IslandPlacement scaled(float next) {
        return new IslandPlacement(centerShare, topShare, next);
    }
}
