package com.persiki84.mediaplayer.color;

import com.persiki84.mediaplayer.anim.Anim;

public final class ColorWash {
    // WHY: мелкий шаг это протяжка ползунка в палитре, и переезд по дуге тона превратил бы её
    // WHY: в запаздывание за курсором; переезжает только настоящий скачок - пресет, сброс, пакет тем.
    // WHY: скачок меряется от показанного цвета, а не от прошлой цели: применение настроек
    // WHY: назначает цвет несколько раз за вызов, и промежуточное значение съело бы переезд
    private static final float JUMP_FLOOR = 0.05f;
    private static final float BLOOM = 0.32f;

    private final float seconds;

    private int from;
    private int target;
    private int shown;
    private float phase = 1.0f;
    private boolean primed;

    public ColorWash(float seconds) {
        this.seconds = Math.max(0.01f, seconds);
    }

    // WHY: заводской цвет ставится без взвода, иначе первое чтение конфига дало бы переезд
    // WHY: с белого на акцент игрока прямо на загрузочном экране
    public ColorWash(float seconds, int initial) {
        this(seconds);
        snap(initial);
    }

    public void aim(int argb) {
        if (argb == target) return;

        if (!primed || Oklab.distance(shown, argb) < JUMP_FLOOR) {
            primed = true;
            snap(argb);
            return;
        }
        from = shown;
        target = argb;
        phase = 0.0f;
    }

    public void advance(float delta) {
        if (phase >= 1.0f) return;

        phase = Math.min(1.0f, phase + delta / seconds);
        shown = Oklab.sweep(from, target, Anim.smoothstep(0.0f, 1.0f, phase), BLOOM);
    }

    public void snap(int argb) {
        from = argb;
        target = argb;
        shown = argb;
        phase = 1.0f;
    }

    public void take(ColorWash older) {
        from = older.from;
        target = older.target;
        shown = older.shown;
        phase = older.phase;
        primed = older.primed;
    }

    public int get() {
        return shown;
    }
}
