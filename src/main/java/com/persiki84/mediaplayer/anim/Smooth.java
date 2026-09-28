package com.persiki84.mediaplayer.anim;

public final class Smooth {
    private final float speed;
    private float value;

    public Smooth(float initial, float speed) {
        this.value = initial;
        this.speed = speed;
    }

    public float to(float target, float delta) {
        value = Anim.approach(value, target, speed, delta);
        return value;
    }

    public float to(float target, float customSpeed, float delta) {
        value = Anim.approach(value, target, customSpeed, delta);
        return value;
    }

    public float get() {
        return value;
    }

    public void snap(float target) {
        value = target;
    }
}
