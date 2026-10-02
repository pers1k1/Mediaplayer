package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.anim.Spring;

// WHY: числа сняты покадрово с записи острова iPhone (60 fps, 3x): раскрытие это пружина
// WHY: response 0.51 c и damping 0.815 с перелётом 1.1-1.4 %, перед ним сжатие за 0.2 c до 0.815
// WHY: ширины и 0.94 высоты таблетки; сворачивание сразу пружиной 0.47 / 0.86. Размер формы не
// WHY: клампится: отдача пружины и есть характер движения
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
    // WHY: владелец 03.10.2026: окно не размывается, размывается только содержимое внутри, пока
    // WHY: общие части летят из таблетки в карточку. Кривая набирается за 0.12 c и снимается за
    // WHY: последние 0.2 c хода, к концу хода содержимое снова резкое
    private static final float BLUR_RISE = 0.12f;
    private static final float BLUR_FALL = 0.2f;
    private static final float OPEN_TAIL = 0.32f;
    private static final float CLOSE_SPAN = 0.36f;

    private static final float SWELL_RESPONSE = 0.35f;
    private static final float SWELL_DAMPING = 0.6f;
    private static final float SWELL_HOLD = 0.12f;
    private static final float SWELL_WIDTH = 0.06f;
    private static final float SWELL_HEIGHT = 0.05f;

    // WHY: радиус угла у iPhone растёт с высотой: 20 pt у компакта высотой 40 pt и 47 pt у карточки
    // WHY: высотой 203 pt, то есть на 0.166 от прироста высоты, до полной капсулы у таблетки
    private static final float RADIUS_GROWTH = 0.166f;

    private final Spring width = new Spring(CLOSE_RESPONSE, CLOSE_DAMPING);
    private final Spring height = new Spring(CLOSE_RESPONSE, CLOSE_DAMPING);
    private final Spring swell = new Spring(SWELL_RESPONSE, SWELL_DAMPING, 0.0f);

    private boolean moving;
    private boolean open;
    private float clock;
    private float lead;
    private float span;
    private float swellClock = SWELL_HOLD;
    private float blurFrom;
    private float blur;

    public void advance(boolean wanted, IslandMeasure measure, float delta) {
        if (wanted != open) depart(wanted, measure);
        clock += delta;
        swellClock += delta;
        drive(measure, delta);
        swell.to(swellClock < SWELL_HOLD ? 1.0f : 0.0f, delta);
        blur = moving ? travelBlur() : 0.0f;
        if (clock >= span) moving = false;
    }

    private void depart(boolean wanted, IslandMeasure measure) {
        blurFrom = blur;
        open = wanted;
        moving = true;
        boolean compact = width.get() <= measure.pillWidth() * SQUEEZE_SKIP;
        lead = wanted && compact ? SQUEEZE_SECONDS : 0.0f;
        span = wanted ? lead + OPEN_TAIL : CLOSE_SPAN;
        clock = 0.0f;
    }

    private void drive(IslandMeasure measure, float delta) {
        if (open && clock < lead) {
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

    private float travelBlur() {
        float fall = 1.0f - Anim.smoothstep(span - BLUR_FALL, span, clock);
        return Math.max(blurFrom, Anim.smoothstep(0.0f, BLUR_RISE, clock)) * fall;
    }

    public void snap(boolean wanted, IslandMeasure measure) {
        open = wanted;
        moving = false;
        blur = 0.0f;
        width.snap(wanted ? measure.cardWidth() : measure.pillWidth());
        height.snap(wanted ? IslandMeasure.CARD_HEIGHT : IslandMeasure.PILL_HEIGHT);
    }

    public void pulse() {
        swellClock = 0.0f;
    }

    public boolean opened() {
        return open;
    }

    public boolean resting() {
        return !moving && swellClock >= SWELL_HOLD && swell.resting(0.0f);
    }

    // WHY: доля полёта общих частей берётся из высоты формы без толчка: части едут вместе со стеклом,
    // WHY: а перелёт пружины за карточку не уносит их за её раскладку
    public float flight() {
        float travel = IslandMeasure.CARD_HEIGHT - IslandMeasure.PILL_HEIGHT;
        return Anim.clamp01((height.get() - IslandMeasure.PILL_HEIGHT) / travel);
    }

    // WHY: остров у края экрана прижат к нему, и центр считается по ширине: с толчком в ширине толчок
    // WHY: сдвигал весь остров вбок, и содержимое ехало на несколько пикселей
    public float steadyWidth() {
        return Math.max(1.0f, width.get());
    }

    public float width() {
        return Math.max(1.0f, width.get() * (1.0f + SWELL_WIDTH * swell.get()));
    }

    public float height() {
        return Math.max(1.0f, height.get() * (1.0f + SWELL_HEIGHT * swell.get()));
    }

    public float blur() {
        return Anim.clamp01(blur);
    }

    public float radius() {
        float tall = height();
        float grown = IslandMeasure.PILL_HEIGHT / 2.0f + (tall - IslandMeasure.PILL_HEIGHT) * RADIUS_GROWTH;
        return Math.min(tall / 2.0f, grown);
    }
}
