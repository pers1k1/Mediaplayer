package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.anim.FrameClock;
import com.persiki84.mediaplayer.color.Colors;
import com.persiki84.mediaplayer.config.IslandDial;
import com.persiki84.mediaplayer.config.IslandSettings;
import com.persiki84.mediaplayer.media.MediaGain;
import com.persiki84.mediaplayer.media.MediaWatch;
import com.persiki84.mediaplayer.render.Paint;
import net.minecraft.client.gui.GuiGraphics;

public final class IslandGlyph {
    public static final float PILL_WIDTH = 12.0f;
    public static final float PILL_HEIGHT = 10.5f;
    public static final float CARD_WIDTH = 16.0f;
    public static final float CARD_HEIGHT = 17.5f;

    private static final int BARS = MediaWatch.BANDS;
    private static final int STOPS = 4;
    private static final float GAP_SHARE = 0.69f;
    private static final float REST_SHARE = 0.078f;
    private static final float TICK_SECONDS = 1.0f / 15.0f;
    // WHY: подъём 40 мс и спад 60 мс с живого захвата Spotify оказались резковаты на слух: спад
    // WHY: смягчён на четверть, подъём на пятую часть, чтобы удар оставался быстрее затухания
    private static final float RISE_SECONDS = 0.048f;
    private static final float FALL_SECONDS = 0.075f;
    private static final float ONSET_RISE = 0.17f;
    private static final float ONSET_SPACING = 0.025f;
    private static final float[] BAND_DECIBELS = {-1.277f, -3.687f, 1.353f, -0.083f, -1.338f, -3.379f};
    private static final float LEVEL_POWER = 0.759f;
    private static final float LEVEL_FLOOR = 0.002f;
    // WHY: громкий кусок трека ставил полоски столбом в потолок: выше 0.6 высота сжимается
    // WHY: гиперболическим тангенсом и не доходит до края, полоски продолжают плясать
    private static final float SOFT_TOP = 0.6f;

    private static final float[] weighted = new float[BARS];
    private static final float[] start = new float[BARS];
    private static final float[] goal = new float[BARS];
    private static final float[] reach = new float[BARS];
    private static final float[] heard = new float[BARS];
    private static final float[] bandGain = new float[BARS];
    private static final int[] tints = new int[STOPS];
    private static final float floorPower = (float) Math.pow(LEVEL_FLOOR, LEVEL_POWER);

    private static float since;
    private static long stamp = -1L;

    static {
        for (int index = 0; index < BARS; index++) {
            bandGain[index] = (float) Math.pow(10.0, BAND_DECIBELS[index] / 20.0);
        }
    }

    private IslandGlyph() {}

    static float along(int column) {
        return (column * (1.0f + GAP_SHARE) + 0.5f) / (BARS + GAP_SHARE * (BARS - 1));
    }

    public static void draw(GuiGraphics graphics, float centerX, float centerY, float width, float height,
                            float fade, float blur) {
        if (fade <= 0.02f) return;

        float bar = width / (BARS + GAP_SHARE * (BARS - 1));
        float left = centerX - width / 2.0f;
        for (int index = 0; index < BARS; index++) {
            float tall = Math.max(bar, height * (REST_SHARE + (1.0f - REST_SHARE) * reach[index]));
            float spread = tall / (2.0f * height);
            Paint.ramp(graphics, left + index * bar * (1.0f + GAP_SHARE), centerY - tall / 2.0f, bar, tall,
                    bar / 2.0f, tinted(index, spread, fade), blur);
        }
    }

    private static int[] tinted(int index, float spread, float fade) {
        for (int stop = 0; stop < STOPS; stop++) {
            float share = 0.5f - spread + 2.0f * spread * stop / (STOPS - 1);
            tints[stop] = Colors.alpha(IslandTone.barAt(index, share), fade);
        }
        return tints;
    }

    // WHY: сетка 15 Гц держит шаг айфона, но резкий подъём (удар) такта не ждёт: сетка сдвигается
    // WHY: под удар, а на такте полоса едет от того места, где стоит, к свежему уровню
    public static void pulse(float energy) {
        long frame = FrameClock.frame();
        if (frame == stamp) return;

        stamp = frame;
        since += FrameClock.delta() * IslandSettings.dial(IslandDial.VISUALIZER_SPEED);
        listen(energy);
        if (since >= TICK_SECONDS || struck()) {
            since = since >= TICK_SECONDS ? since % TICK_SECONDS : 0.0f;
            retarget();
        }
        glide();
    }

    private static void listen(float energy) {
        float loudest = 0.0f;
        for (int index = 0; index < BARS; index++) {
            weighted[index] = MediaWatch.band(index) * bandGain[index];
            loudest = Math.max(loudest, weighted[index]);
        }
        float scale = MediaGain.scale(loudest) * IslandSettings.dial(IslandDial.VISUALIZER_GAIN);
        for (int index = 0; index < BARS; index++) {
            heard[index] = level(weighted[index] * scale) * energy;
        }
    }

    private static boolean struck() {
        if (since < ONSET_SPACING) return false;

        for (int index = 0; index < BARS; index++) {
            if (heard[index] - goal[index] > ONSET_RISE) return true;
        }
        return false;
    }

    private static void retarget() {
        for (int index = 0; index < BARS; index++) {
            start[index] = reach[index];
            goal[index] = heard[index];
        }
    }

    private static float level(float amplitude) {
        float lifted = (float) Math.pow(Math.max(0.0f, amplitude), LEVEL_POWER);
        float height = Math.max(0.0f, (lifted - floorPower) / (1.0f - floorPower));
        if (height <= SOFT_TOP) return height;

        float room = 1.0f - SOFT_TOP;
        return SOFT_TOP + room * (float) Math.tanh((height - SOFT_TOP) / room);
    }

    private static void glide() {
        float rise = IslandSettings.dial(IslandDial.VISUALIZER_ATTACK);
        for (int index = 0; index < BARS; index++) {
            float span = goal[index] > start[index] ? RISE_SECONDS / rise : FALL_SECONDS;
            reach[index] = start[index] + (goal[index] - start[index]) * Anim.smoothstep(0.0f, span, since);
        }
    }
}
