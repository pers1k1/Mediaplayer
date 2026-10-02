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
    // WHY: владелец 03.10.2026: «всё окно размывалось при открытии и закрытии», элементы не убираются.
    // WHY: Остров размывается целиком одной кривой на весь ход, а обе раскладки перетекают внахлёст
    // WHY: под её пиком, поэтому пустого стекла между таблеткой и карточкой нет ни в один кадр
    private static final float BLUR_RISE = 0.12f;
    private static final float BLUR_FALL = 0.2f;
    private static final float OPEN_TAIL = 0.32f;
    private static final float CLOSE_SPAN = 0.36f;
    private static final float OPEN_SWAP_MIN = 0.02f;
    private static final float OPEN_SWAP_EARLY = 0.08f;
    private static final float OPEN_SWAP_LATE = 0.12f;
    private static final float CLOSE_SWAP_FROM = 0.04f;
    private static final float CLOSE_SWAP_TO = 0.22f;

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
    private float blurFrom;
    private float pillAlpha = 1.0f;
    private float cardAlpha;
    private float blur;

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
        blurFrom = blur;
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
        float swap = Anim.smoothstep(Math.max(OPEN_SWAP_MIN, lead - OPEN_SWAP_EARLY), lead + OPEN_SWAP_LATE, clock);
        pillAlpha = pillFrom * (1.0f - swap);
        cardAlpha = cardFrom + (1.0f - cardFrom) * swap;
        settle(lead + OPEN_TAIL);
    }

    private void closing() {
        float swap = Anim.smoothstep(CLOSE_SWAP_FROM, CLOSE_SWAP_TO, clock);
        cardAlpha = cardFrom * (1.0f - swap);
        pillAlpha = pillFrom + (1.0f - pillFrom) * swap;
        settle(CLOSE_SPAN);
    }

    private void settle(float span) {
        float fall = 1.0f - Anim.smoothstep(span - BLUR_FALL, span, clock);
        blur = Math.max(blurFrom * fall, Anim.smoothstep(0.0f, BLUR_RISE, clock) * fall);
        if (clock >= span) phase = Phase.REST;
    }

    private void rest() {
        pillAlpha = open ? 0.0f : 1.0f;
        cardAlpha = open ? 1.0f : 0.0f;
        blur = 0.0f;
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

    public float blur() {
        return Anim.clamp01(blur);
    }

    public float cardAlpha() {
        return Anim.clamp01(cardAlpha);
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
