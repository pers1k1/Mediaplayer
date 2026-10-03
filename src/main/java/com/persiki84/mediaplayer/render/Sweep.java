package com.persiki84.mediaplayer.render;

public interface Sweep {
    Sweep NONE = index -> 1.0f;

    float lit(int index);

    default float head() {
        return -1.0f;
    }

    default int accent(int base) {
        return base;
    }

    default float held(int index) {
        return 0.0f;
    }
}
