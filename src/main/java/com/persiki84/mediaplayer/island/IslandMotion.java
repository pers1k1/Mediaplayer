package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.anim.Spring;

// WHY: числа сняты покадрово с записи острова iPhone (60 fps, 3x): раскрытие это пружина
// WHY: response 0.51 c и damping 0.815 с перелётом 1.1-1.4 %, перед ним сжатие за 0.2 c до 0.815
// WHY: ширины и 0.94 высоты таблетки; сворачивание сразу пружиной 0.47 / 0.86. Содержимое каждого
// WHY: состояния стоит в своей раскладке, масштабируется с формой и проходит через размытие по своим
// WHY: окнам времени. Размер формы не клампится: отдача пружины и есть характер движения
public final class IslandMotion {
    private static final float SQUEEZE_SECONDS = 0.2f;
    private static final float SQUEEZE_WIDTH = 0.815f;
    private static final float SQUEEZE_HEIGHT = 0.94f;
    private static final float SQUEEZE_SKIP = 1.1f;
    private static final float SQUEEZE_RESPONSE = 0.3f;
    private static final float SQUEEZE_DAMPING = 1.0f;
    private static final float OPEN_RESPONSE = 0.51f;
    private static final float OPEN_DAMPING = 0.815f;
    private static final float CLOSE_RESPONSE = 0.47f;
    private static final float CLOSE_DAMPING = 0.86f;
    private static final float CONTENT_OUT = 0.12f;
    private static final float BLUR_IN = 0.03f;
    private static final float CARD_IN_FROM = 0.02f;
    private static final float CARD_IN_TO = 0.20f;
    private static final float CARD_SHARP_FROM = 0.05f;
    private static final float CARD_SHARP_TO = 0.30f;
    private static final float PILL_IN_FROM = 0.15f;
    private static final float PILL_IN_TO = 0.28f;
    private static final float PILL_SHARP_TO = 0.32f;

    private static final float SWELL_RESPONSE = 0.35f;
    private static final float SWELL_DAMPING = 0.6f;
    private static final float SWELL_HOLD = 0.12f;
    private static final float SWELL_WIDTH = 0.06f;
    private static final float SWELL_HEIGHT = 0.05f;

    // WHY: радиус угла у iPhone растёт с высотой: 20 pt у компакта высотой 40 pt и 47 pt у карточки
    // WHY: высотой 203 pt, то есть на 0.166 от прироста высоты, до полной капсулы у таблетки
    private static final float RADIUS_GROWTH = 0.166f;
    private static final float SCALE_LOW = 0.5f;
    private static final float SCALE_HIGH = 1.15f;

    private enum Phase { REST, OPEN, CLOSE }

    private final Spring width = new Spring(CLOSE_RESPONSE, CLOSE_DAMPING);
    private final Spring height = new Spring(CLOSE_RESPONSE, CLOSE_DAMPING);
    private final Spring swell = new Spring(SWELL_RESPONSE, SWELL_DAMPING, 0.0f);

    private Phase phase = Phase.REST;
    private boolean open;
    private float clock;
    private float lead;
    private float swellClock = SWELL_HOLD;
    private float pillFrom = 1.0f;
    private float cardFrom;
    private float pillAlpha = 1.0f;
    private float pillBlur;
    private float cardAlpha;
    private float cardBlur;

    public void advance(boolean wanted, IslandMeasure measure, float delta) {
        if (wanted != open) depart(wanted, measure);
        clock += delta;
        swellClock += delta;
        drive(measure, delta);
        swell.to(swellClock < SWELL_HOLD ? 1.0f : 0.0f, delta);
        if (phase == Phase.OPEN) opening();
        if (phase == Phase.CLOSE) closing();
        if (phase == Phase.REST) rest();
    }

    private void depart(boolean wanted, IslandMeasure measure) {
        pillFrom = pillAlpha;
        cardFrom = cardAlpha;
        open = wanted;
        phase = wanted ? Phase.OPEN : Phase.CLOSE;
        boolean compact = width.get() <= measure.pillWidth() * SQUEEZE_SKIP;
        lead = wanted && compact ? SQUEEZE_SECONDS : 0.0f;
        clock = 0.0f;
    }

    private void drive(IslandMeasure measure, float delta) {
        boolean squeezing = phase == Phase.OPEN && clock < lead;
        if (squeezing) {
            tune(SQUEEZE_RESPONSE, SQUEEZE_DAMPING);
            width.to(measure.pillWidth() * SQUEEZE_WIDTH, delta);
            height.to(IslandMeasure.PILL_HEIGHT * SQUEEZE_HEIGHT, delta);
            return;
        }
        if (open) {
            tune(OPEN_RESPONSE, OPEN_DAMPING);
        } else {
            tune(CLOSE_RESPONSE, CLOSE_DAMPING);
        }
        width.to(open ? measure.cardWidth() : measure.pillWidth(), delta);
        height.to(open ? IslandMeasure.CARD_HEIGHT : IslandMeasure.PILL_HEIGHT, delta);
    }

    private void tune(float response, float damping) {
        width.tune(response, damping);
        height.tune(response, damping);
    }

    private void opening() {
        pillAlpha = pillFrom * (1.0f - Anim.smoothstep(0.0f, CONTENT_OUT, clock));
        pillBlur = Anim.smoothstep(0.0f, CONTENT_OUT, clock);
        cardAlpha = Math.max(cardFrom, Anim.smoothstep(lead + CARD_IN_FROM, lead + CARD_IN_TO, clock));
        cardBlur = 1.0f - Anim.smoothstep(lead + CARD_SHARP_FROM, lead + CARD_SHARP_TO, clock);
        if (clock >= lead + CARD_SHARP_TO) phase = Phase.REST;
    }

    private void closing() {
        cardAlpha = cardFrom * (1.0f - Anim.smoothstep(0.0f, CONTENT_OUT, clock));
        cardBlur = Anim.smoothstep(0.0f, BLUR_IN, clock);
        pillAlpha = Math.max(pillFrom, Anim.smoothstep(PILL_IN_FROM, PILL_IN_TO, clock));
        pillBlur = 1.0f - Anim.smoothstep(PILL_IN_FROM, PILL_SHARP_TO, clock);
        if (clock >= PILL_SHARP_TO) phase = Phase.REST;
    }

    private void rest() {
        pillAlpha = open ? 0.0f : 1.0f;
        cardAlpha = open ? 1.0f : 0.0f;
        pillBlur = 0.0f;
        cardBlur = 0.0f;
    }

    public void snap(boolean wanted, IslandMeasure measure) {
        open = wanted;
        phase = Phase.REST;
        width.snap(wanted ? measure.cardWidth() : measure.pillWidth());
        height.snap(wanted ? IslandMeasure.CARD_HEIGHT : IslandMeasure.PILL_HEIGHT);
        rest();
    }

    public void pulse() {
        swellClock = 0.0f;
    }

    public boolean opened() {
        return open;
    }

    public float width() {
        return Math.max(1.0f, width.get() * (1.0f + SWELL_WIDTH * swell.get()));
    }

    public float height() {
        return Math.max(1.0f, height.get() * (1.0f + SWELL_HEIGHT * swell.get()));
    }

    public float pillAlpha() {
        return Anim.clamp01(pillAlpha);
    }

    public float pillBlur() {
        return Anim.clamp01(pillBlur);
    }

    public float cardAlpha() {
        return Anim.clamp01(cardAlpha);
    }

    public float cardBlur() {
        return Anim.clamp01(cardBlur);
    }

    public float pillScale(IslandMeasure measure) {
        return fit(width() / measure.pillWidth(), height() / IslandMeasure.PILL_HEIGHT);
    }

    public float cardScale(IslandMeasure measure) {
        return fit(width() / measure.cardWidth(), height() / IslandMeasure.CARD_HEIGHT);
    }

    private static float fit(float across, float down) {
        return Anim.clamp((float) Math.sqrt(Math.max(0.0f, across * down)), SCALE_LOW, SCALE_HIGH);
    }

    public float radius() {
        float tall = height();
        float grown = IslandMeasure.PILL_HEIGHT / 2.0f + (tall - IslandMeasure.PILL_HEIGHT) * RADIUS_GROWTH;
        return Math.min(tall / 2.0f, grown);
    }
}
