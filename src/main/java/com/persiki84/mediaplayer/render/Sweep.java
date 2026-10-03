package com.persiki84.mediaplayer.render;

public interface Sweep {
    Sweep NONE = index -> 1.0f;

    float lit(int index);
}
