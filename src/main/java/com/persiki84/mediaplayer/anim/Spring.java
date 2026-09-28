package com.persiki84.mediaplayer.anim;

public final class Spring {
    private static final float SUBSTEP = 1.0f / 240.0f;
    private static final int MAX_SUBSTEPS = 32;
    private static final float REST_DISTANCE = 0.0004f;
    private static final float REST_SPEED = 0.0025f;

    private float stiffness;
    private float damping;
    private float value;
    private float velocity;
    private boolean primed;

    public Spring(float response, float dampingRatio) {
        tune(response, dampingRatio);
    }

    public Spring(float response, float dampingRatio, float initial) {
        this(response, dampingRatio);
        snap(initial);
    }

    // WHY: response и dampingRatio это параметры пружины SwiftUI: период незатухающего колебания
    // WHY: и доля критического демпфирования, k = (2п / response)^2, c = 2 * ratio * 2п / response
    public void tune(float response, float dampingRatio) {
        float frequency = (float) (Math.PI * 2.0) / Math.max(0.02f, response);
        stiffness = frequency * frequency;
        damping = 2.0f * dampingRatio * frequency;
    }

    public float to(float target, float delta) {
        if (!primed) {
            snap(target);
            return value;
        }
        if (delta <= 0.0f) return value;

        int steps = Math.min(MAX_SUBSTEPS, Math.max(1, (int) Math.ceil(delta / SUBSTEP)));
        float step = delta / steps;
        for (int index = 0; index < steps; index++) {
            velocity += ((target - value) * stiffness - velocity * damping) * step;
            value += velocity * step;
        }
        settle(target);
        return value;
    }

    private void settle(float target) {
        if (Math.abs(target - value) >= REST_DISTANCE || Math.abs(velocity) >= REST_SPEED) return;

        value = target;
        velocity = 0.0f;
    }

    public void kick(float impulse) {
        velocity += impulse;
    }

    public float get() {
        return value;
    }

    public boolean resting(float target) {
        return primed && value == target && velocity == 0.0f;
    }

    public void snap(float target) {
        value = target;
        velocity = 0.0f;
        primed = true;
    }
}
