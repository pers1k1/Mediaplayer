package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.config.IslandFlag;
import com.persiki84.mediaplayer.config.IslandSettings;
import com.persiki84.mediaplayer.render.Ink;
import com.persiki84.mediaplayer.render.Weight;
import net.minecraft.client.gui.Font;

public final class IslandMeasure {
    public static final float PILL_HEIGHT = 19.0f;
    public static final float CARD_HEIGHT = 47.0f;
    static final float PAD = 5.0f;
    static final float FACE = 15.0f;
    static final float ART = 30.0f;
    static final float GAP = 6.0f;
    static final float WAVE_GAP = 6.0f;
    static final float STAT_INSET = 8.0f;
    static final float CAPSULE_GAP = 4.0f;
    static final float CAPSULE_PAD = 6.0f;
    static final float PILL_TIME_GAP = 4.0f;

    static final float NICK_SCALE = 1.0f;
    static final float TITLE_PILL_SCALE = 0.9f;
    static final float TITLE_CARD_SCALE = 1.0f;
    static final float ARTIST_SCALE = 0.84f;
    static final float TIME_SCALE = 0.7f;

    private static final float PILL_BAR_MIN = 32.0f;
    // WHY: цифры FPS, пинга и таймера меняют ширину строки на пару единиц (99 -> 100, 9:59 -> 10:00),
    // WHY: и остров прыгал бы шире и уже вместе с ними: ширина держит свой максимум и отпускает его,
    // WHY: только когда строка стала короче больше чем на запас
    private static final float HOLD_SLACK = 10.0f;
    // WHY: остров тянется за названием, но верхний предел держит его компактным: всё, что длиннее
    // WHY: слота, уезжает бегущей строкой
    private static final float TITLE_MIN = 42.0f;
    private static final float TITLE_MAX = 82.0f;
    private static final float CARD_TEXT_MIN = 96.0f;
    private static final float CARD_TEXT_MAX = 122.0f;

    private float media;
    private float blind;
    private float face;
    private float stats;
    private float timingPeak;
    private float waveSlot;
    private float cardWaveSlot;
    private float pillWidth;
    private float cardWidth;

    void measure(Font font, IslandTitles titles, float statsWidth) {
        media = IslandModel.media();
        blind = IslandModel.blind() * media;
        face = IslandSettings.on(IslandFlag.AVATAR) || IslandSettings.on(IslandFlag.COVER) ? FACE : 0.0f;
        stats = held(stats, statsWidth);
        boolean visualized = IslandSettings.on(IslandFlag.VISUALIZER);
        waveSlot = visualized ? IslandGlyph.PILL_WIDTH + WAVE_GAP : 0.0f;
        cardWaveSlot = visualized ? IslandGlyph.CARD_WIDTH + WAVE_GAP : 0.0f;
        timingPeak = held(timingPeak, titles.pillRow.line().width(font, Ink.fit(TIME_SCALE)));
        pillWidth = Anim.lerp(idleWidth(font), mediaWidth(font, titles), media);
        float cardText = Math.max(titles.title.line().width(font, Ink.fit(TITLE_CARD_SCALE)),
                titles.artist.line().width(font, Ink.fit(ARTIST_SCALE)));
        cardWidth = PAD + ART + GAP + Anim.clamp(cardText, CARD_TEXT_MIN, CARD_TEXT_MAX) + cardWaveSlot + PAD;
    }

    private float idleWidth(Font font) {
        float nick = IslandSettings.on(IslandFlag.NICK)
                ? Ink.width(font, IslandModel.nick(), Weight.SEMIBOLD, Ink.fit(NICK_SCALE)) : 0.0f;
        return PAD + face + GAP + nick + (stats > 0.0f ? STAT_INSET + stats : 0.0f) + PAD;
    }

    private float mediaWidth(Font font, IslandTitles titles) {
        float titleWidth = titles.title.line().width(font, Ink.fit(TITLE_PILL_SCALE));
        float title = PAD + face + GAP + Anim.clamp(titleWidth, TITLE_MIN, TITLE_MAX)
                + waveSlot + PAD;
        float timed = PAD + face + GAP + timingPeak + (titles.untimed() ? 0.0f : PILL_TIME_GAP + PILL_BAR_MIN) + PAD;
        return Math.max(title, timed * (1.0f - blind));
    }

    private static float held(float peak, float measured) {
        return measured > peak || measured < peak - HOLD_SLACK ? measured : peak;
    }

    public float media() {
        return media;
    }

    public float blind() {
        return blind;
    }

    public float face() {
        return face;
    }

    public float art() {
        return face > 0.0f ? ART : 0.0f;
    }

    public float timingPeak() {
        return timingPeak;
    }

    public float stats() {
        return stats;
    }

    public float waveSlot() {
        return waveSlot;
    }

    public float cardWaveSlot() {
        return cardWaveSlot;
    }

    public float pillWidth() {
        return pillWidth;
    }

    public float cardWidth() {
        return cardWidth;
    }

    // WHY: счётчик можно убрать на время музыки: тогда он гаснет и уходит вместе с её появлением, а
    // WHY: без музыки по-прежнему стоит в острове рядом с ником
    public float statsShare() {
        return IslandSettings.on(IslandFlag.STATS_WITH_MEDIA) ? 1.0f : 1.0f - media;
    }

    public float capsule() {
        return stats > 0.0f ? CAPSULE_PAD * 2.0f + stats : 0.0f;
    }
}
